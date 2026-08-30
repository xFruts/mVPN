import { type ReactNode, useEffect, useState } from "react";
import Keycloak from "keycloak-js";
import { setTokenGetter } from "@/api";
import { KEYCLOAK_CONFIG } from "./constant";
import { type AdminData, UserProvider } from "@/AdminContext.tsx";

const keycloak = new Keycloak(KEYCLOAK_CONFIG);
let isInitCalled = false;

export const AuthProvider = ({ children }: { children: ReactNode }) => {
    const [adminData, setAdminData] = useState<AdminData>({
        preferred_username: "",
        authenticated: false,
        login: () => keycloak.login(),
        logout: () => keycloak.logout(),
    });
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (isInitCalled) return;
        isInitCalled = true;
        keycloak
            .init({
                onLoad: "check-sso",
                silentCheckSsoRedirectUri:
                    window.location.origin + "/silent-check-sso.html",
            })
            .then((authenticated) => {
                if (authenticated) {
                    setTokenGetter(async () => {
                        try {
                            await keycloak.updateToken(30);
                            return keycloak.token;
                        } catch (error) {
                            console.error("Failed to refresh token", error);
                            await keycloak.login();
                        }
                    });

                    setAdminData({
                        preferred_username:
                            keycloak.tokenParsed?.preferred_username || "",
                        authenticated: true,
                        login: () => keycloak.login(),
                        logout: () => keycloak.logout(),
                    });
                } else {
                    setAdminData({
                        preferred_username: "",
                        authenticated: false,
                        login: () => keycloak.login(),
                        logout: () => keycloak.logout(),
                    });
                }
            })
            .catch((err) => {
                console.error("Keycloak init error", err);
            })
            .finally(() => {
                setLoading(false);
            });
    }, []);

    if (loading) return <div></div>;

    return <UserProvider value={adminData}>{children}</UserProvider>;
};
