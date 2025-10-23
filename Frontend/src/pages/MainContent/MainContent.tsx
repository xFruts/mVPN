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
import {AddVpnServers} from "../vpnServers/components/AddVpnServers/AddVpnServers.tsx";
import SettingsVpnServers from "../vpnServers/components/SettingsVpnServers/SettingVpnServers.tsx";
import ViewingLogs from "../vpnServers/components/ViewingLogs/ViewingLogs.tsx";


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
                <Route path="/users" Component={MainAllUsers} />
                <Route path="/sendMessages" Component={MainSendMessages} />
                <Route path="/servers" Component={MainVpnServers} />
                <Route path="/promotional" Component={MainPromotional} />
                <Route path="/analytics" Component={MainAnalytics} />
                <Route path="/settings" Component={MainSettings} />
                <Route path="/users/add" Component={AddUsers} />
                <Route path="/users/edit" Component={ChangeUsers} />
                <Route path="/servers/add" Component={AddVpnServers} />
                <Route path="/servers/edit" Component={SettingsVpnServers} />
                <Route path="/servers/log" Component={ViewingLogs} />
            </Routes>
        </div>
    );
}