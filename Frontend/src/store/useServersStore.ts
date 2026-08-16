import { create } from "zustand";
import { immer } from "zustand/middleware/immer";
import type { Servers } from "@typings/server";
import type { GetServerData } from "@/types/general.ts";
import { ServerService } from "@api/services/serverService.ts";
import type { ApiErrorType } from "@api/types.ts";
import { createDataFetcher } from "@pages/shared/setParams.ts";

interface ServersState {
    servers: Servers[];
    totalElements: number;
    totalPages: number;
    isInitialized: boolean;
    isLoading: boolean;
    error: ApiErrorType | null;
    filters: GetServerData;
    fetchServers: (newParams: Partial<GetServerData>) => Promise<void>;
    isChangeOpen: boolean;
    isOpen: number;
    setIsChangeOpen: (value: boolean) => void;
    setIsOpen: (servers: number) => void;
}

const useServersStore = create<ServersState>()(
    immer((set, get) => ({
        servers: [],
        totalElements: 0,
        totalPages: 0,
        isInitialized: false,
        isLoading: false,
        error: null,
        filters: {
            page: 0,
            size: 12,
            sort: "id,asc",
        },
        fetchServers: createDataFetcher(
            ServerService.getServers,
            get,
            set,
            "servers",
        ),
        isChangeOpen: false,
        isOpen: -1,
        setIsChangeOpen: (value: boolean) =>
            set((state) => ({
                isChangeOpen:
                    typeof value === "boolean" ? value : !state.isChangeOpen,
            })),
        setIsOpen: (id) => set({ isOpen: id }),
    })),
);

export default useServersStore;
