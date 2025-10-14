import { useState } from "react";
import { MoveLeft, Save, User, Star, Crown, TriangleAlert, X, Ticket, Package, Gem } from 'lucide-react';
import { NavLink, useLocation } from "react-router";
import './ChangeUsers.css'
import ReactDOM from "react-dom";
import Countries from "../Countries/Countries.tsx";

export default function ChangeUsers() {
    const location = useLocation();
    const user = location.state?.props;
    const [role, setRole] = useState(user.role)
    const [type, setType] = useState(user.type)
    const [date, setDate] = useState(user.dateFinish)
    const [selectedUser, setSelectedUser] = useState<null | { id: number; country: string}>(null);
    const [formData, setFormData] = useState({
        firstName: user.firstname,
        lastName: user.lastname,
        telegramId: user.telegramId,
    });

    const handleOpen = (userId: number, country: string) => {
        setSelectedUser({ id: userId, country });
    };

    const handleClose = () => setSelectedUser(null);

    const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = event.target;

        setFormData(prevFormData => ({
            ...prevFormData,
            [name]: value
        }));
    };

    const getFutureDate = (days: number) => {
        let d = new Date(date);
        d.setDate(d.getDate() + days);
        setDate(d.toISOString().slice(0, 10));
    };

    return(
        <div className={"changeUsers"}>
            <div className="changeUsers-header">
                <div className="changeUsers-header-title">
                    <span style={{fontSize: "23px"}}>Редактирование пользователя</span>
                    <span style={{color: "gray"}}>Изменение данных пользователя #{user.id}</span>
                </div>
                <NavLink to={'/allusers'} className="changeUsers-header-back">
                    <MoveLeft size={13}/>
                    <span>Назад к списку</span>
                </NavLink>
            </div>
            <div className={"changeUsers-content"}>
                <div className={"changeUsers-person-role-type"}>
                    <div className={"changeUsers-person"}>
                        <div className={"changeUsers-person-header"}>
                            <span>Персональная информация</span>
                        </div>
                        <div className={"changeUsers-person-name-surname"}>
                            <div className={"changeUsers-person-name"}>
                                <span>Имя <span style={{ color: "red"}}>*</span></span>
                                <input
                                    type="text"
                                    className="changeUsers-person-input"
                                    placeholder="Введите имя"
                                    name="firstName"
                                    value={formData.firstName}
                                    onChange={handleChange}/>
                            </div>
                            <div className={"changeUsers-person-surname"}>
                                <span>Фамилия <span style={{ color: "red"}}>*</span></span>
                                <input
                                    type="text"
                                    className="changeUsers-person-input"
                                    placeholder="Введите фамилию"
                                    name="lastName"
                                    value={formData.lastName}
                                    onChange={handleChange}/>
                            </div>
                        </div>
                        <div className={"changeUsers-person-telegram"}>
                            <span>Telegram ID (опционально)</span>
                            <input
                                type="text"
                                className="changeUsers-person-input"
                                placeholder="Введите имя"
                                name="telegramId"
                                value={formData.telegramId}
                                onChange={handleChange}/>
                            <span style={{color: "gray"}}>Если указан, пользователь сможет управлять подпиской через Telegram бота</span>
                        </div>
                    </div>
                    <div className={"changeUsers-role"}>
                        <div className={"changeUsers-role-header"}>
                            <span>Роль пользователя</span>
                        </div>
                        <div className={"changeUsers-role-content"}>
                            <div className={`changeUsers-role-specific ${role === "BASIC" ? `roleActive` : ``}`} onClick={() => setRole("BASIC")}>
                                <span><User size={20}/></span>
                                <span style={{fontSize: "15px", fontWeight: "bold"}}>BASIC</span>
                                <span style={{color: "gray"}}>Базовые возможности</span>
                            </div>
                            <div className={`changeUsers-role-specific ${role === "SPECIAL" ? `roleActive` : ``}`} onClick={() => setRole("SPECIAL")}>
                                <span><Star size={20}/></span>
                                <span style={{fontSize: "15px", fontWeight: "bold"}}>SPECIAL</span>
                                <span style={{color: "gray"}}>Семейный доступ</span>
                            </div>
                            <div className={`changeUsers-role-specific ${role === "ADMIN" ? `roleActive` : ``}`} onClick={() => setRole("ADMIN")}>
                                <span><Crown size={20}/></span>
                                <span style={{fontSize: "15px", fontWeight: "bold"}}>ADMIN</span>
                                <span style={{color: "gray"}}>Полный доступ</span>
                            </div>
                        </div>
                    </div>
                    <div className={"changeUsers-type"}>
                        <div className={"changeUsers-type-header"}>
                            Тип подписки
                        </div>
                        <div className={"changeUsers-type-content"}>
                            <div className={`changeUsers-type-specific ${type === "NONE" ? `roleActive` : ``}`} onClick={() => setType("NONE")}>
                                <span><X size={20}/></span>
                                <span>NONE</span>
                                <span className={"price none"}>Без подписки</span>
                                <div className={"changeUsers-type-specific-info"}>
                                    <span>-</span>
                                    <span>-</span>
                                </div>
                            </div>
                            <div className={`changeUsers-type-specific ${type === "TRIAL" ? `roleActive` : ``}`} onClick={() => setType("TRIAL")}>
                                <span><Ticket size={20}/></span>
                                <span>TRIAL</span>
                                <span className={"price trial"}>Бесплатно</span>
                                <div className={"changeUsers-type-specific-info"}>
                                    <span>7 дней</span>
                                    <span>50 ГБ</span>
                                </div>
                            </div>
                            <div className={`changeUsers-type-specific ${type === "BASIC" ? `roleActive` : ``}`} onClick={() => setType("BASIC")}>
                                <span><Package size={20}/></span>
                                <span>BASIC</span>
                                <span className={"price basic"}>299 ₽</span>
                                <div className={"changeUsers-type-specific-info"}>
                                    <span>30 дней</span>
                                    <span>300 ГБ</span>
                                </div>
                            </div>
                            <div className={`changeUsers-type-specific ${type === "PREMIUM" ? `roleActive` : ``}`} onClick={() => setType("PREMIUM")}>
                                <span><Gem size={20}/></span>
                                <span>PREMIUM</span>
                                <span className={"price special"}>2999 ₽</span>
                                <div className={"changeUsers-type-specific-info"}>
                                    <span>365 дней</span>
                                    <span>Безлимит</span>
                                </div>
                            </div>
                        </div>
                        <div className={"changeUsers-date"}>
                            <div className={"changeUsers-date-header"}>
                                <span>Срок действия</span>
                            </div>
                            <input
                                className={"changeUsers-date-input"}
                                type="date"
                                value={date}
                                onChange={(e) => setDate(e.target.value)}/>
                            <div className={"changeUsers-date-fast"}>
                                <span style = {{fontSize : "13px", color: "gray"}}>Быстрое продление</span>
                                <div className={"changeUsers-date-fast-change"}>
                                    <div className={"changeUsers-date-fast-change-detail dynamic"} onClick={() => getFutureDate(1)}>
                                        +1 день
                                    </div>
                                    <div className={"changeUsers-date-fast-change-detail basic"} onClick={() => getFutureDate(7)}>
                                        +7 день
                                    </div>
                                    <div className={"changeUsers-date-fast-change-detail special"} onClick={() => getFutureDate(30)}>
                                        +30 день
                                    </div>
                                    <div className={"changeUsers-date-fast-change-detail none"} onClick={() => setDate(user.dateFinish)}>
                                        Сброс даты
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div className={"changeUsers-search-infotype-important"}>
                    <div className={"changeUsers-search"}>
                        <div className={"changeUsers-search-header"}>
                            <span>Информация о пользователе</span>
                        </div>
                        <div className={"changeUsers-search-details"}>
                            <span style={{color: "gray", fontSize: "15px"}}>ID пользователя</span>
                            <span>#{user.id}</span>
                        </div>
                        <div className={"changeUsers-search-details"}>
                            <span style={{color: "gray", fontSize: "15px"}}>Дата создания</span>
                            <span>{user.dateStart}</span>
                        </div>
                        <div className={"changeUsers-search-details"}>
                            <span style={{color: "gray", fontSize: "15px"}}>Страны</span>
                            <div className="countries">
                                {user.countries.map((country : string) => (
                                    <span
                                        key={`${user.id}-${country}`}
                                        onClick={() => handleOpen(user.id, country)}
                                        style={{ cursor: "pointer" }}
                                        >{country}</span>
                                ))}
                            </div>
                        </div>
                    </div>
                    <div className={"changeUsers-infotype"}>
                        <div className={"changeUsers-infotype-header"}>
                            <span>Информация о подписке</span>
                        </div>
                        <div className={"changeUsers-infotype-content"}>
                            <span style = {{color: "gray"}}>Подписка</span>
                            <span>{type}</span>
                            {
                                type === "PREMIUM" ? (
                                    <div style = {{color: "gray"}}>
                                        <span>365 дней</span>
                                        <span>Безлимит</span>
                                        <span>2999 ₽</span>
                                    </div>
                                ) :
                                type === "BASIC" ? (
                                    <div style = {{color: "gray"}}>
                                        <span>30 дней</span>
                                        <span>300 ГБ</span>
                                        <span>299 ₽</span>
                                    </div>
                                ) :
                                type === "TRIAL" ? (
                                    <div style = {{color: "gray"}}>
                                        <span>7 дней</span>
                                        <span>50 ГБ</span>
                                        <span>Бесплатно</span>
                                    </div>
                                ) : (
                                    <div style = {{color: "gray"}}>
                                        <span>-</span>
                                        <span>-</span>
                                        <span>-</span>
                                    </div>
                                )
                            }
                        </div>
                    </div>
                    <div className={"changeUsers-important"}>
                        <div className={"changeUsers-important-header"}>
                            <TriangleAlert size={20}/>
                            <span>Важно</span>
                        </div>
                        <ul className={"changeUsers-important-content"}>
                            <li>Изменения вступят в силу немедленно</li>
                            <li>Пользователь получит уведомление</li>
                            <li> При смене типа подписки, конфигурации обновятся</li>
                        </ul>
                    </div>
                </div>
            </div>
            <div className={"changeUsers-create"}>
                <button className={"button-create"}><Save size={20}/>Сохранить изменения</button>
                <button className={"button-cancel"}>Отмена</button>
            </div>

            {selectedUser && (() => {
                return ReactDOM.createPortal(
                    <Countries
                        userId={selectedUser.id}
                        specificCountry={selectedUser.country}
                        firstName={user.firstname}
                        lastName={user.lastname}
                        countries={user.countries}
                        onClose={handleClose}
                    />,
                    document.body
                );
            })()}
        </div>
    )
}