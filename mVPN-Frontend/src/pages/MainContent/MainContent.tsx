import {Route, Routes} from "react-router";
import MainAllUsers from "../allUsers/MainAllUsers.tsx";
import MainAddUsers from "../addUsers/MainAddUsers.tsx";


export default function MainContent() {
    return (
        <div>
            <Routes>
                <Route path="/allUsers" Component={MainAllUsers} />
                <Route path="/addUsers" Component={MainAddUsers} />
            </Routes>
        </div>
    );
}