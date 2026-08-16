import type { PayingsGet } from "@/types/paying.ts";
import type { GetPayingData } from "@/types/general.ts";
import { create } from "zustand";
import { immer } from "zustand/middleware/immer";
import { createDataFetcher } from "@pages/shared/setParams.ts";
import { PayingService } from "@api/services/payingService.ts";
import type { ApiErrorType } from "@api/types.ts";

interface PayingsState {
    payings: PayingsGet[];
    totalElements: number;
    totalPages: number;
    filters: GetPayingData;
    fetchPaying: (newParams: Partial<GetPayingData>) => Promise<void>;
    pendingCount: number;
    pendingAmount: number;
    fetchStats: () => Promise<void>;
    loadAllData: () => void;
    error: ApiErrorType | null;
    setError: (error: ApiErrorType) => void;
    isLoading: boolean;
    isInitialized: boolean;
    isOpen: boolean;
    setIsOpen: (param: boolean) => void;
    userId: number;
    setUserId: (userId: number) => void;
}

export const usePayingStore = create<PayingsState>()(
    immer((set, get) => ({
        payings: [],
        totalElements: 0,
        totalPages: 0,
        filters: {
            page: 0,
            size: 20,
            sort: "paidAmount,desc",
        },
        fetchPaying: createDataFetcher(
            PayingService.getPayings,
            get,
            set,
            "payings",
        ),
        pendingCount: 0,
        pendingAmount: 0,
        fetchStats: async () => {
            const data = await PayingService.statsPaying();
            set({
                pendingAmount: data.pendingAmount,
                pendingCount: data.pendingCount,
            });
        },
        loadAllData: async () => {
            set({ isLoading: true });
            await Promise.all([get().fetchPaying({}), get().fetchStats()]).finally(() => set({ isLoading: false, isInitialized: true }));
        },
        error: null,
        setError: (error: ApiErrorType) => {
            set({error: error});
        },
        isLoading: false,
        isInitialized: false,
        isOpen: false,
        setIsOpen: (param) => {
            set({ isOpen: param });
        },
        userId: 0,
        setUserId: (userId) => {
            set({ userId: userId });
        },
    })),
);