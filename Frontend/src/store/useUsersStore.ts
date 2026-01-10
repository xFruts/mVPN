import { create } from "zustand";
import { immer } from "zustand/middleware/immer";
import type { User } from "@typings/user";

interface UsersState {
    users: User[];
    filteredUsers: User[];
    sort: { field: string; direction: "up" | "down" | "" };
    selectedUser: { id: number; country: string } | null;
    apiOpen: boolean;
    isOpen: number;
    setUsers: (users: User[]) => void;
    setFilteredUsers: (users: User[]) => void;
    setSort: (field: string, direction: "up" | "down" | "") => void;
    setSelectedUser: (user: { id: number; country: string } | null) => void;
    setApiOpen: (open: boolean) => void;
    setIsOpen: (id: number) => void;
    toggleMenu: (id: number) => void;
    resetMenu: () => void;
    updateUserData: (id: number, updates: Partial<User>) => void;
}

const useUsersStore = create<UsersState>()(
    immer((set) => ({
        users: [
            {
                id: 1,
                firstname: "Алексей",
                lastname: "Петров",
                telegramId: "",
                role: "SPECIAL",
                type: "TRIAL",
                status: "CANCELLED",
                countries: ["FI", "US", "RU", "NL", "DE", "SG"],
                dateStart: "2021-02-11",
                dateFinish: "2021-05-02",
            },
            {
                id: 2,
                firstname: "Алексей",
                lastname: "Смирнов",
                telegramId: "@her",
                role: "BASIC",
                type: "BASIC",
                status: "EXPIRED",
                countries: ["RU", "FI"],
                dateStart: "2021-03-13",
                dateFinish: "2021-08-11",
            },
            {
                id: 3,
                firstname: "Максим",
                lastname: "Петров",
                telegramId: "",
                role: "ADMIN",
                type: "PREMIUM",
                status: "ACTIVE",
                countries: ["RU"],
                dateStart: "2021-01-06",
                dateFinish: "2021-05-02",
            },
            {
                id: 4,
                firstname: "Максим",
                lastname: "Петров",
                telegramId: "@pebb",
                role: "BASIC",
                type: "NONE",
                status: "NONE",
                countries: ["RU"],
                dateStart: "2021-03-20",
                dateFinish: "2021-05-02",
            },
        ],
        filteredUsers: [],
        sort: { field: "", direction: "" },
        selectedUser: null,
        apiOpen: false,
        isOpen: -1,

        setUsers: (users) => set({ users }),
        setFilteredUsers: (users) => set({ filteredUsers: users }),
        setSort: (field, direction) => set({ sort: { field, direction } }),
        setSelectedUser: (user) => set({ selectedUser: user }),
        setApiOpen: (open) => set({ apiOpen: open }),
        setIsOpen: (id) => set({ isOpen: id }),
        toggleMenu: (id) =>
            set((state) => ({ isOpen: state.isOpen === id ? -1 : id })),
        resetMenu: () =>
            set({ isOpen: -1, selectedUser: null, apiOpen: false }),
        updateUserData: (id, updates) =>
            set((state) => {
                const index = state.users.findIndex((u: User) => u.id === id);
                if (index !== -1) {
                    state.users[index] = { ...state.users[index], ...updates };
                }
            }),
    })),
);

export default useUsersStore;
