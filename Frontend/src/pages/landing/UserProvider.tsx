import React, { useState, useEffect, type ReactNode } from "react";
import { UserContext } from "./UserContext";

interface UserProviderProps {
    children: ReactNode;
}

export const UserProvider: React.FC<UserProviderProps> = ({ children }) => {
    const [isUserMode, setIsUserMode] = useState<boolean>(() => {
        return localStorage.getItem("userMode") === "true";
    });

    useEffect(() => {
        localStorage.setItem("userMode", isUserMode.toString());
    }, [isUserMode]);

    return (
        <UserContext.Provider value={{ isUserMode, setIsUserMode }}>
            {children}
        </UserContext.Provider>
    );
};
