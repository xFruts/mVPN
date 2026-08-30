import { createContext, useContext } from "react";

export interface UserContextType {
    isUserMode: boolean;
    setIsUserMode: (value: boolean) => void;
}

export const UserContext = createContext<UserContextType | undefined>(
    undefined,
);

export const useUserMode = () => {
    const context = useContext(UserContext);
    if (context === undefined) {
        throw new Error("useUserMode must be used within a UserProvider");
    }
    return context;
};
