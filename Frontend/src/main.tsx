import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router";
import App from "./App.tsx";
import Keycloak from "keycloak-js";
import { setTokenGetter } from "@/api";
import { KEYCLOAK_CONFIG, FRONTEND_URL } from "./constant";

const keycloak = new Keycloak(KEYCLOAK_CONFIG);

keycloak
    .init({
        onLoad: "login-required",
        redirectUri: FRONTEND_URL,
    })
    .then((authenticated) => {
        if (authenticated) {
            setTokenGetter(() => keycloak.token);

            createRoot(document.getElementById("root")!).render(
                <BrowserRouter>
                    <StrictMode>
                        <App />
                    </StrictMode>
                </BrowserRouter>,
            );
        }
    });
