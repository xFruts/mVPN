import Sidebar from "./pages/Sidebar/Sidebar.tsx";
import MainContent from "./pages/MainContent/MainContent.tsx";
import "./App.css";

function App() {
    return (
        <div className="main-page">
            <div className="sidebar">
                <Sidebar />
            </div>
            <div className="main-content">
                <MainContent />
            </div>
        </div>
    );
}

export default App;
