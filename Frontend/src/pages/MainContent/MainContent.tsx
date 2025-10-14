import {Route, Routes} from "react-router";
import MainAllUsers from "../allUsers/MainAllUsers.tsx";
import MainSendMessages from "../sendMessages/MainSendMessages.tsx";
import {Bell, CircleUserRound, ChevronDown} from "lucide-react"
import "./MainContent.css"
import MainVpnServers from "../vpnServers/MainVpnServers.tsx";
import MainPromotional from "../promotional/MainPromotional.tsx";
import MainAnalytics from "../analytics/MainAnalytics.tsx";
import MainSettings from "../settings/MainSettings.tsx";
import MainControlPanel from "../controlPanel/MainControlPanel.tsx";
import AddUsers from "../allUsers/component/AddUsers/AddUsers.tsx";
import ChangeUsers from "../allUsers/component/ChangeUsers/ChangeUsers.tsx";


export default function MainContent() {
    return (
        <div>
            <div className="header">
                <div className="bell">
                    <Bell size={30}/>
                </div>
                <div className="account">
                    <CircleUserRound size={30}/>
                    <p>admin</p>
                    <ChevronDown size={30}/>
                </div>
            </div>
            <Routes>
                <Route path="/" Component={MainControlPanel} />
                <Route path="/allUsers" Component={MainAllUsers} />
                <Route path="/sendMessages" Component={MainSendMessages} />
                <Route path="/vpnServers" Component={MainVpnServers} />
                <Route path="/promotional" Component={MainPromotional} />
                <Route path="/analytics" Component={MainAnalytics} />
                <Route path="/settings" Component={MainSettings} />
                <Route path="/addUsers" Component={AddUsers} />
                <Route path="/changeUsers" Component={ChangeUsers} />
            </Routes>
        </div>
    );
}