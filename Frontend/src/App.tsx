import { Routes, Route, Navigate } from "react-router";
import MainLanding from "@pages/landing/MainLanding.tsx";
import MainContent from "@pages/MainContent/MainContent.tsx";
import "./App.css"
import { useAdmin } from "@/AdminContext.tsx";


const App = () => {
    const { authenticated } = useAdmin();
    return (
        <Routes>
            <Route path="/" element={<MainLanding />} />
            <Route
                path="/*"
                element={authenticated ? <MainContent /> : <Navigate to="/" />}
            />
        </Routes>
    );
};

export default App;