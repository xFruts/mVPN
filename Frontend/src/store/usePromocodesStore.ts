import { create } from "zustand";
import { immer } from "zustand/middleware/immer";
import type { PromocodesGet } from "@/types/promocodes.ts";
import { PromocodeService } from "@api/services/promocodeService.ts";
import type { GetPromocodeData } from "@/types/general.ts";
import { createDataFetcher } from "@pages/shared/setParams.ts";
import type { ApiErrorType } from "@api/types.ts";

interface PromocodesState {
    promocodes: PromocodesGet[];
    totalElements: number;
    totalPages: number;
    filters: GetPromocodeData;
    error: ApiErrorType | null;
    activeCodes: number;
    allUsages: number;
    fetchPromocodes: (newParams: Partial<GetPromocodeData>) => Promise<void>;
    isInitialized: boolean;
    setIsInitialized: (isInitialized: boolean) => void;
    isLoading: boolean;
    setIsLoading: (isLoading: boolean) => void;
}

export const usePromocodesStore = create<PromocodesState>()(
    immer((set, get) => ({
        promocodes: [],
        totalElements: 0,
        totalPages: 0,
        filters: {
            page: 0,
            size: 20,
            sort: "id,asc",
        },
        error: null,
        activeCodes: 0,
        allUsages: 0,
        fetchPromocodes: createDataFetcher(
            PromocodeService.getPromocodes,
            get,
            set,
            "promocodes",
        ),
        isInitialized: false,
        setIsInitialized(isInitialized: boolean) {
            set({ isInitialized: isInitialized });
        },
        isLoading: false,
        setIsLoading: (isLoading: boolean) => {
            set({ isLoading: isLoading });
        },
    })),
);
