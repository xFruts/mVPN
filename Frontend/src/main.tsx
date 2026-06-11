import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router";
import App from "./App.tsx";
import Keycloak from "keycloak-js";
import { setTokenGetter } from "@/api";
import { KEYCLOAK_CONFIG } from "./constant";
import { type AdminData, UserProvider } from "@/AdminContext.tsx";

const keycloak = new Keycloak(KEYCLOAK_CONFIG);


keycloak
    .init({
        onLoad: "login-required",
    })
    .then((authenticated) => {
        if (authenticated) {
            setTokenGetter(() => keycloak.token);
            const adminData: AdminData = {
                preferred_username:
                    keycloak.tokenParsed?.preferred_username || "",
            };

            createRoot(document.getElementById("root")!).render(
                <UserProvider value={adminData}>
                    <BrowserRouter>
                        <StrictMode>
                            <App />
                        </StrictMode>
                    </BrowserRouter>
                </UserProvider>,
            );
        }
    });
