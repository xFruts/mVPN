import { create } from "zustand";
import { immer } from "zustand/middleware/immer";
import type { PromocodesGet } from "@/types/promocodes.ts";
import { PromocodeService } from "@api/services/promocodeService.ts";
import type { GetPromocodeData } from "@/types/general.ts";

interface PromocodesState {
    promocodes: PromocodesGet[];
    totalElements: number;
    totalPages: number;
    filters: GetPromocodeData;
    setParams: (params: Partial<GetPromocodeData>) => void;
    activeCodes: number;
    allUsages: number;
    fetchPromocodes: (params?: Partial<GetPromocodeData>) => Promise<void>;
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
        activeCodes: 0,
        allUsages: 0,
        setParams: (newParams) => {
            set((state) => {
                const updatedFilters = { ...state.filters, ...newParams };

                if (newParams.page === undefined) {
                    updatedFilters.page = 0;
                }

                Object.keys(newParams).forEach((key) => {
                    const k = key as keyof GetPromocodeData;
                    if (
                        updatedFilters[k] === "" ||
                        updatedFilters[k] === null ||
                        updatedFilters[k] === undefined
                    ) {
                        delete updatedFilters[k];
                    }
                });

                state.filters = updatedFilters;
            });

            get().fetchPromocodes();
        },
        fetchPromocodes: async (newParams) => {
            set({ isLoading: true });
            const currentFilters = { ...get().filters, ...newParams };

            try {
                const data =
                    await PromocodeService.getPromocodes(currentFilters);
                const content = data.content;

                const stats = content.reduce(
                    (acc, promocode) => {
                        acc.allCodes += 1;
                        if (promocode.status === "ACTIVE") {
                            acc.activeCodes += 1;
                        }
                        acc.allUsages += promocode.usage;
                        return acc;
                    },
                    { allCodes: 0, activeCodes: 0, allUsages: 0 },
                );

                set({
                    promocodes: content,
                    totalElements: data.totalElements,
                    totalPages: data.totalPages,
                    activeCodes: stats.activeCodes,
                    allUsages: stats.allUsages,
                    isLoading: false,
                    isInitialized: true,
                });
            } catch (error) {
                console.error(error);
                set({
                    isLoading: false,
                    isInitialized: true
                });
            }
        },
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
