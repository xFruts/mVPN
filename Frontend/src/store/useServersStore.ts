import { create } from "zustand";
import { immer } from "zustand/middleware/immer";
import type { Server } from "@typings/server";

interface ServersState {
    servers: Server[];
    filteredServers: Server[];
    sort: { field: string; direction: "up" | "down" | "" };
    selectedServer: { id: number } | null;
    isOpen: number;
    setIsOpen: (servers: number) => void;
    setSort: (field: string, direction: "up" | "down" | "") => void;
    setServers: (servers: Server[]) => void;
    setSelectedServer: (server: { id: number } | null) => void;
    setFilteredServers: (servers: Server[]) => void;
}

const useServersStore = create<ServersState>()(
    immer((set) => ({
        servers: [
            {
                id: 1,
                name: "server1",
                location: "Finland",
                ip: "94.183.190.23",
                status: "ONLINE",
                load: 67,
                usage: 20,
                maxUsers: 50,
                maxTraffic: 300,
                ping: 3,
                uptime: 66.67,
            },
            {
                id: 2,
                name: "server2",
                location: "Russia",
                ip: "94.183.190.23",
                status: "OFFLINE",
                load: 50,
                usage: 0,
                maxUsers: 100,
                maxTraffic: 1024,
                ping: 17,
                uptime: 50.0,
            },
            {
                id: 3,
                name: "server3",
                location: "USA",
                ip: "94.183.190.23",
                status: "MAINTENANCE",
                load: 90,
                usage: 100,
                maxUsers: 150,
                maxTraffic: 1024,
                ping: 57,
                uptime: 99.9,
            },
            {
                id: 4,
                name: "server4",
                location: "Germany",
                ip: "94.183.190.23",
                status: "ONLINE",
                load: 30,
                usage: 10,
                maxUsers: 25,
                maxTraffic: 500,
                ping: 103,
                uptime: 90.0,
            },
        ],
        filteredServers: [],
        sort: { field: "", direction: "" },
        selectedServer: null,
        isOpen: -1,

        setIsOpen: (id) => set({ isOpen: id }),
        setServers: (servers) => set({ servers }),
        setFilteredServers: (servers) => set({ filteredServers: servers }),
        setSort: (field, direction) => set({ sort: { field, direction } }),
        setSelectedServer: (server) => set({ selectedServer: server }),
    })),
);

export default useServersStore;
