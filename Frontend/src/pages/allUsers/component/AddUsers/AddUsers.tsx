import { useState } from "react";
import { MoveLeft, UserPlus, User, Star, Crown, Lightbulb, X, Ticket, Package, Gem } from 'lucide-react';
import {NavLink} from "react-router";
import './AddUsers.css'

export default function AddUsers() {

    const [role, setRole] = useState("basic")
    const [type, setType] = useState("none")
    const [formData, setFormData] = useState({
        firstName: '',
        lastName: '',
        telegramId: ''
    });

    const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = event.target;

        setFormData(prevFormData => ({
            ...prevFormData,
            [name]: value
        }));
    };

    const getFutureDate = (days: number) => {
        const d = new Date();
        d.setDate(d.getDate() + days);
        return d.toLocaleDateString();
    };

    return(
        <div className={"addusers"}>
            <div className="addusers-header">
                <div className="addusers-header-title">
                    <span style={{fontSize: "23px"}}>Добавление пользователя</span>
                    <span style={{color: "gray"}}>Создание нового пользователя VPN сервиса</span>
                </div>
                <NavLink to={'/allusers'} className="addusers-header-back">
                    <MoveLeft size={13}/>
                    <span>Назад к списку</span>
                </NavLink>
            </div>
            <div className={"addusers-content"}>
                <div className={"addusers-person-role-type"}>
                    <div className={"addusers-person"}>
                        <div className={"addusers-person-header"}>
                            <span>Персональная информация</span>
                        </div>
                        <div className={"addusers-person-name-surname"}>
                            <div className={"addusers-person-name"}>
                                <span>Имя <span style={{ color: "red"}}>*</span></span>
                                <input
                                    type="text"
                                    className="addusers-person-input"
                                    placeholder="Введите имя"
                                    name="firstName"
                                    value={formData.firstName}
                                    onChange={handleChange}/>
                            </div>
                            <div className={"addusers-person-surname"}>
                                <span>Фамилия <span style={{ color: "red"}}>*</span></span>
                                <input
                                    type="text"
                                    className="addusers-person-input"
                                    placeholder="Введите фамилию"
                                    name="lastName"
                                    value={formData.lastName}
                                    onChange={handleChange}/>
                            </div>
                        </div>
                        <div className={"addusers-person-telegram"}>
                            <span>Telegram ID (опционально)</span>
                            <input
                                type="text"
                                className="addusers-person-input"
                                placeholder="Введите имя"
                                name="telegramId"
                                value={formData.telegramId}
                                onChange={handleChange}/>
                            <span style={{color: "gray"}}>Если указан, пользователь сможет управлять подпиской через Telegram бота</span>
                        </div>
                    </div>
                    <div className={"addusers-role"}>
                        <div className={"addusers-role-header"}>
                            <span>Роль пользователя</span>
                        </div>
                        <div className={"addusers-role-content"}>
                            <div className={`addusers-role-specific ${role === "basic" ? `roleActive` : ``}`} onClick={() => setRole("basic")}>
                                <span><User size={20}/></span>
                                <span style={{fontSize: "15px", fontWeight: "bold"}}>BASIC</span>
                                <span style={{color: "gray"}}>Базовые возможности</span>
                            </div>
                            <div className={`addusers-role-specific ${role === "special" ? `roleActive` : ``}`} onClick={() => setRole("special")}>
                                <span><Star size={20}/></span>
                                <span style={{fontSize: "15px", fontWeight: "bold"}}>SPECIAL</span>
                                <span style={{color: "gray"}}>Семейный доступ</span>
                            </div>
                            <div className={`addusers-role-specific ${role === "admin" ? `roleActive` : ``}`} onClick={() => setRole("admin")}>
                                <span><Crown size={20}/></span>
                                <span style={{fontSize: "15px", fontWeight: "bold"}}>ADMIN</span>
                                <span style={{color: "gray"}}>Полный доступ</span>
                            </div>
                        </div>
                    </div>
                    <div className={"addusers-type"}>
                        <div className={"addusers-type-header"}>
                            Тип подписки
                        </div>
                        <div className={"addusers-type-content"}>
                            <div className={`addusers-type-specific ${type === "none" ? `roleActive` : ``}`} onClick={() => setType("none")}>
                                <span><X size={20}/></span>
                                <span>NONE</span>
                                <span className={"price none"}>Без подписки</span>
                                <div className={"addusers-type-specific-info"}>
                                    <span>-</span>
                                    <span>-</span>
                                </div>
                            </div>
                            <div className={`addusers-type-specific ${type === "trial" ? `roleActive` : ``}`} onClick={() => setType("trial")}>
                                <span><Ticket size={20}/></span>
                                <span>TRIAL</span>
                                <span className={"price trial"}>Бесплатно</span>
                                <div className={"addusers-type-specific-info"}>
                                    <span>7 дней</span>
                                    <span>50 ГБ</span>
                                </div>
                            </div>
                            <div className={`addusers-type-specific ${type === "basic" ? `roleActive` : ``}`} onClick={() => setType("basic")}>
                                <span><Package size={20}/></span>
                                <span>BASIC</span>
                                <span className={"price basic"}>299 ₽</span>
                                <div className={"addusers-type-specific-info"}>
                                    <span>30 дней</span>
                                    <span>300 ГБ</span>
                                </div>
                            </div>
                            <div className={`addusers-type-specific ${type === "vip" ? `roleActive` : ``}`} onClick={() => setType("vip")}>
                                <span><Gem size={20}/></span>
                                <span>VIP</span>
                                <span className={"price vip"}>2999 ₽</span>
                                <div className={"addusers-type-specific-info"}>
                                    <span>365 дней</span>
                                    <span>Безлимит</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div className={"addusers-search-info"}>
                    <div className={"addusers-search"}>
                        <div className={"addusers-search-header"}>
                            <span>Предварительный просмотр</span>
                        </div>
                        <div className={"addusers-search-details"}>
                            <span style={{color: "gray", fontSize: "15px"}}>Пользователь</span>
                            <span>{formData.firstName === "" ? "Имя" : formData.firstName} {formData.lastName === "" ? "Фамилия" : formData.lastName}</span>
                        </div>
                        <div className={"addusers-search-details"}>
                            <span style={{color: "gray", fontSize: "15px"}}>Роль</span>
                            <span className={`userRole ${role}`}>{role}</span>
                        </div>
                        <div className={"addusers-search-details"}>
                            <span style={{color: "gray", fontSize: "15px"}}>Подписка</span>
                            <span className={`userRole ${type}`}>{type}</span>
                        </div>
                        <div className={"addusers-search-date"} style={{color: "gray", fontSize: "15px"}}>
                            <span>
                                <span>До:</span>
                                {
                                    type === "trial" ? getFutureDate(7) :
                                    type === "basic" ? getFutureDate(30) :
                                    type === "vip" ? getFutureDate(365) :
                                    "-"
                                }
                            </span>
                        </div>
                        {formData.telegramId && (
                            <div className={"addusers-search-details"}>
                                <span style={{color: "gray", fontSize: "15px"}}>Telegram ID</span>
                                <span>{formData.telegramId}</span>
                            </div>
                        )}
                    </div>
                    <div className={"addusers-info"}>
                        <div className={"addusers-info-header"}>
                            <Lightbulb size={20}/>
                            <span>Информация</span>
                        </div>
                        <ul className={"addusers-info-content"}>
                            <li>Пользователь получит уведомление по Telegram</li>
                            <li>Подписка активируется автоматически</li>
                            <li>Конфигурации будут сгенерированы</li>
                        </ul>
                    </div>
                </div>
            </div>
            <div className={"addusers-create"}>
                <button className={"button-create"}><UserPlus size={20}/> Создать пользователя</button>
                <button className={"button-cancel"}>Отмена</button>
            </div>
        </div>
    )
}