import {NavLink} from "react-router";
import { useState } from "react";
import {Lightbulb, MoveLeft, TriangleAlert, Plus, Eye, EyeOff} from "lucide-react";
import './AddVpnServers.css'

export function AddVpnServers() {
    const [title, setTitle] = useState("")
    const [location, setLocation] = useState("")
    const [IP, setIP] = useState("")
    const [port, setPort] = useState("51820")
    const [maxUsers, setMaxUsers] = useState("100")
    const [status, setStatus] = useState("")
    const [login, setLogin] = useState("")
    const [password, setPassword] = useState("")
    const [visiblePassword, setVisiblePassword] = useState(false)
    return (
        <div className={"addServer"}>
            <div className={"addServer-header"}>
                <div className="addServer-header-title">
                    <span style={{fontSize: "23px"}}>Добавление VPN сервер</span>
                    <span style={{color: "gray"}}>Настройка нового VPN сервера в сети</span>
                </div>
                <NavLink to={'/servers'} end className="addServer-header-back">
                    <MoveLeft size={13}/>
                    <span>Назад к списку</span>
                </NavLink>
            </div>
            <div className={"addServer-content"}>
                <div className={"addServer-info"}>
                    <div className={"addServer-info-header"}>
                        <span>Основная информация</span>
                    </div>
                    <div className={"addServer-info-body"}>
                        <div className={"addServer-info-body-item"}>
                            <span>Название сервера <span style={{color: "red"}}>*</span></span>
                            <input
                                type="text"
                                className="addServer-input"
                                placeholder="Helsinki-01"
                                value={title}
                                onChange={(e) => {
                                    setTitle(e.target.value)
                                }}
                                name="title"
                            />
                        </div>
                        <div className={"addServer-info-body-item"}>
                            <span>Локация <span style={{color: "red"}}>*</span></span>
                            <div className="addServer-input">
                                <select
                                    value={location}
                                    onChange={(e) => {
                                        setLocation(e.target.value);
                                        e.target.blur();
                                    }}>
                                    <option value={"allStatus"}>Выберите локацию</option>
                                    <option value={"active"}>FI Финляндия</option>
                                    <option value={"expired"}>US США</option>
                                    <option value={"canceled"}>RU Россия</option>
                                    <option value={"canceled"}>NL Нидерланды</option>
                                    <option value={"canceled"}>DE Германия</option>
                                    <option value={"canceled"}>SG Сингапур</option>
                                    <option value={"canceled"}>JP Япония</option>
                                </select>
                            </div>
                        </div>
                        <div className={"addServer-info-body-item"}>
                            <span>IP адрес <span style={{color: "red"}}>*</span></span>
                            <input
                                type="text"
                                className="addServer-input"
                                placeholder="192.168.1.100"
                                value={IP}
                                onChange={(e) => {
                                    setIP(e.target.value)
                                }}
                                name="IP"
                            />
                        </div>
                        <div className={"addServer-info-body-item"}>
                            <span>Порт</span>
                            <input
                                type="text"
                                className="addServer-input"
                                value={port}
                                onChange={(e) => {
                                    setPort(e.target.value)
                                }}
                                name="port"
                            />
                        </div>
                        <div className={"addServer-info-body-item"}>
                            <span>Максимум пользователей</span>
                            <input
                                type="number"
                                className="addServer-input"
                                value={maxUsers}
                                onChange={(e) => {
                                    setMaxUsers(e.target.value)
                                }}
                                name="maxUsers"
                                min="1"
                                max="1000"
                                step="1"
                            />
                        </div>
                        <div className={"addServer-info-body-item"}>
                            <span>Статус</span>
                            <div className="addServer-input">
                                <select
                                    value={status}
                                    onChange={(e) => {
                                        setStatus(e.target.value);
                                        e.target.blur();
                                    }}>
                                    <option value={"allStatus"}>ONLINE</option>
                                    <option value={"active"}>MAINTENANCE</option>
                                    <option value={"expired"}>OFFLINE</option>
                                </select>
                            </div>

                        </div>
                    </div>
                </div>
                <div className={"addServer-access"}>
                    <div className={"addServer-access-content"}>
                        <div className={"addServer-access-header"}>
                            <span>Данные для доступа к серверу</span>
                        </div>
                        <div className={"addServer-access-body"}>
                            <div className={"addServer-access-body-item"}>
                                <span>Логин <span style={{color: "red"}}>*</span></span>
                                <input
                                    type="text"
                                    className="addServer-input"
                                    placeholder="admin"
                                    value={login}
                                    onChange={(e) => {
                                        setLogin(e.target.value)
                                    }}
                                    name="login"
                                />
                            </div>
                            <div className={"addServer-access-body-item"}>
                                <span>Пароль <span style={{color: "red"}}>*</span></span>
                                <div className="addServer-input">
                                    <input
                                        type={visiblePassword ? "text" : "password"}
                                        placeholder="Введите пароль"
                                        value={password}
                                        onChange={(e) => {
                                            setPassword(e.target.value)
                                        }}
                                        name="password"
                                    />
                                    {visiblePassword ? (
                                        <Eye size={15} onClick={() => setVisiblePassword(false)}/>
                                    ) : (
                                        <EyeOff size={15} onClick={() => setVisiblePassword(true)}/>
                                    )}
                                </div>

                            </div>
                        </div>
                    </div>
                    <div className={"addServer-access-safety"}>
                        <div>
                            <TriangleAlert size={20}/>
                            <span>Важно</span>
                        </div>
                        <span>Эти данные будут использоваться для подключения к серверу и управления конфигурациями. Убедитесь, что пароль достаточно сложный и не используется в других системах.</span>
                    </div>
                </div>
                <div className={"addServer-button-recommendations"}>
                    <div className={"addServer-button"}>
                        <button className={"button-create"}><Plus size={20}/>Добавить сервер</button>
                        <button className={"button-cancel"}>Отмена</button>
                    </div>
                    <div className={"addServer-recommendations"}>
                        <div className={"addServer-recommendations-header"}>
                            <Lightbulb size={20}/>
                            <span>Рекомендации</span>
                        </div>
                        <ul className={"addServer-recommendations-content"}>
                            <li>Убедитесь, что IP адрес доступен из интернета</li>
                            <li>Проверьте открытость указанного порта</li>
                            <li>Используйте понятные названия для серверов</li>
                            <li>Сохраните данные доступа в безопасном месте</li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    )
}