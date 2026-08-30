/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, type ReactNode } from "react";

export interface AdminData {
    preferred_username?: string;
    authenticated?: boolean;
    login?: () => void;
    logout?: () => void;
}

const UserContext = createContext<AdminData | null>(null);

export const useAdmin = () => {
    const context = useContext(UserContext);
    if (!context) throw new Error("useUser must be used within a UserProvider");
    return context;
};

export const UserProvider = ({
    children,
    value,
}: {
    children: ReactNode;
    value: AdminData;
}) => {
    return (
        <UserContext.Provider value={value}>{children}</UserContext.Provider>
    );
};
