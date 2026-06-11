import Sidebar from "./pages/Sidebar/Sidebar.tsx";
import MainContent from "./pages/MainContent/MainContent.tsx";
import "./App.css";
import { useState } from "react";
import MainHeader from "@pages/header/MainHeader.tsx";

function App() {
    const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
    return (
        <div className="main-page">
            <Sidebar
                isMobileOpen={isMobileMenuOpen}
                closeMobile={() => setIsMobileMenuOpen(false)}
            />
            <div className="main-content">
                <MainHeader openMobile={() => setIsMobileMenuOpen(true)} />
                <MainContent />
            </div>
        </div>
    );
}

export default App;
