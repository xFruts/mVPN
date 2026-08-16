import { create } from "zustand";

interface ErrorState {
    criticalError: number | null;
    setCriticalError: (status: number | null) => void;
}

export const useErrorStore = create<ErrorState>((set) => ({
    criticalError: null,
    setCriticalError: (status) => set({ criticalError: status === 500 || status === 0 ? status : null }),
}));
