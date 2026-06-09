import { create } from "zustand";
import { immer } from "zustand/middleware/immer";
import type { Servers } from "@typings/server";
import type { GetData } from "@/types/general.ts";
import { ServerService } from "@api/services/serverService.ts";

interface ServersState {
    servers: Servers[];
    isLoading: boolean;
    filters: Partial<GetData>;
    fetchServers: (params?: Partial<GetData>) => Promise<void>;
    isChangeOpen: boolean;
    isOpen: number;
    setIsChangeOpen: () => void;
    setIsOpen: (servers: number) => void;
}

const useServersStore = create<ServersState>()(
    immer((set, get) => ({
        servers: [],
        isLoading: false,
        filters: {
            page: 0,
            size: 12,
            sort: "id",
        },
        fetchServers: async (newParams) => {
            set({ isLoading: true });
            const currentFilters = { ...get().filters, ...newParams };
            try {
                const data = await ServerService.getServers(currentFilters);
                set({
                    servers: data,
                    filters: currentFilters,
                    isLoading: false,
                });
            } catch (error) {
                set({ isLoading: false });
                console.error(error);
            }
        },
        isChangeOpen: false,
        isOpen: -1,
        setIsChangeOpen: () =>
            set((state) => {
                state.isChangeOpen = !state.isChangeOpen;
            }),
        setIsOpen: (id) => set({ isOpen: id }),
    })),
);

export default useServersStore;
