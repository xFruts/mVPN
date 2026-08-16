import type { PageResponse } from "@/types/general.ts";
import ApiError from "@api/index.ts";
import type {ApiErrorType} from "@api/types.ts";
import { useErrorStore } from "@store/useErrorStore.ts";

export const createDataFetcher = <
    T_Filters extends { page: number },
    T_Entity,
    T_Store extends {
        filters: T_Filters;
        isLoading: boolean;
        isInitialized: boolean;
        totalElements: number;
        totalPages: number;
        error: ApiErrorType | null;
    },
>(
    serviceMethod: (params: T_Filters) => Promise<PageResponse<T_Entity>>,
    get: () => T_Store,
    set: (partial: Partial<T_Store>) => void,
    dataKey: keyof T_Store,
) => {
    return async (newParams: Partial<T_Filters>) => {
        set({ isLoading: true } as Partial<T_Store>);


        const updatedFilters: T_Filters = { ...get().filters, ...newParams };

        if (newParams.page === undefined) {
            updatedFilters.page = 0 as T_Filters["page"];
        }

        (Object.keys(newParams) as Array<keyof T_Filters>).forEach((key) => {
            const val = updatedFilters[key];
            if (val === "" || val === null || val === undefined) {
                delete updatedFilters[key];
            }
        });
        const errorStore = useErrorStore.getState();

        try {
            const data = await serviceMethod(updatedFilters);

            set({
                [dataKey]: data.content,
                totalElements: data.totalElements,
                totalPages: data.totalPages,
                filters: updatedFilters,
                isLoading: false,
                isInitialized: true,
            } as unknown as Partial<T_Store>);
            errorStore.setCriticalError(200);
        } catch (error) {
            if (error instanceof ApiError) {
                const apiError = error as ApiErrorType;
                console.log(apiError.statusCode);
                errorStore.setCriticalError(apiError.statusCode);
            } else {
                console.error("Unknown error:", error);
                errorStore.setCriticalError(0);
            }
            set({ isLoading: false, isInitialized: true, error: error } as Partial<T_Store>);
        }
    };
};
