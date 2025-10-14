import {NavLink, Route, Routes} from "react-router";
import TextMasseges from "./component/TextMessages.tsx";
import PaymentDetails from "./component/ PaymentDetails.tsx";

export default function MainSendMessages(){
    return(
        <>
            <div className="send-header">
                <span>Добавить пользователся</span>
                <span>Массовая рассылка и уведомления пользователям</span>
                <div>
                    <NavLink to={"/"} className={"text-messages"}>
                        <span>Текстовые сообщения</span>
                    </NavLink>
                    <NavLink to={"/paymentDetails"} className={"payment-details"}>
                        <span>Данные об оплате</span>
                    </NavLink>
                </div>
            </div>
            <Routes>
                <Route path="/" Component={TextMasseges} />
                <Route path="/paymentDetails" Component={PaymentDetails} />
            </Routes>
        </>
    );
}