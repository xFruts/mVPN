import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { NavLink } from "react-router";
import { useState } from "react";
import {
    Lightbulb,
    MoveLeft,
    TriangleAlert,
    Plus,
    Eye,
    EyeOff,
} from "lucide-react";
import styles from "./AddVpnServers.module.css";
import { SERVER_STATUS, SERVER_LOCATION } from "@/constant.ts";

const ipv4Regex = /^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;

const schema = z.object({
    title: z.string().min(1).max(100),
    location: z.string().min(1).max(100),
    IP: z.string().regex(ipv4Regex, { message: "Неправильный IPv4 адрес" }),
    port: z.string().regex(/^\d+$/).refine(val => {
        const num = parseInt(val, 10);
        return num >= 1 && num <= 65535;
    }, { message: "Порт должен быть от 1 до 65535" }),
    maxUsers: z.string().min(1).max(5, { message: "Количество должно быть меньше 100000"}),
    status: z.enum(SERVER_STATUS),
    login: z.string()
        .min(1, "Login is required")
        .max(50, "Login must be at most 50 characters")
        .trim(),
    password: z.string().min(8).max(100),
});

type FormData = z.infer<typeof schema>;

export function AddVpnServers() {
    const [visiblePassword, setVisiblePassword] = useState(false);

    const {
        register,
        handleSubmit,
        formState: { errors },
    } = useForm<FormData>({
        resolver: zodResolver(schema),
        defaultValues: {
            title: "",
            location: "",
            IP: "",
            port: "51820",
            maxUsers: "100",
            status: "ONLINE",
            login: "",
            password: ""
        },
    });

    const onSubmit = (data: FormData) => {
        console.log(data);
    };

    return (
        <div className={styles.addServer}>
            <div className={styles.addServerHeader}>
                <div className={styles.addServerHeaderTitle}>
                    <span style={{ fontSize: "23px" }}>
                        Добавление VPN сервер
                    </span>
                    <span style={{ color: "gray" }}>
                        Настройка нового VPN сервера в сети
                    </span>
                </div>
                <NavLink to={"/servers"} end className={styles.addServerHeaderBack}>
                    <MoveLeft size={13} />
                    <span>Назад к списку</span>
                </NavLink>
            </div>
            <div className={styles.addServerContent}>
                <form onSubmit={handleSubmit(onSubmit)} noValidate>
                    <div className={styles.addServerInfo}>
                        <div className={styles.addServerInfoHeader}>
                            <span>Основная информация</span>
                        </div>
                        <div className={styles.addServerInfoBody}>
                            <div className={styles.addServerInfoBodyItem}>
                                <span>
                                    Название сервера{" "}
                                    <span style={{ color: "red" }}>*</span>
                                </span>
                                <input
                                    type="text"
                                    className={styles.addServerInput}
                                    placeholder="Helsinki-01"
                                    {...register('title')}
                                />
                                {errors.title && (
                                    <span style={{ color: "red" }}>
                                        {errors.title.message}
                                    </span>
                                )}
                            </div>
                            <div className={styles.addServerInfoBodyItem}>
                                <span>
                                    Локация <span style={{ color: "red" }}>*</span>
                                </span>
                                <div className={styles.addServerInput}>
                                    <select {...register('location')}>
                                        <option value="">Выберите локацию</option>
                                        {Object.entries(SERVER_LOCATION).map(([typeKey, config]) => (
                                            <option key={typeKey} value={config.codes}>
                                                {config.codes} {config.location}
                                            </option>
                                        ))}
                                    </select>
                                    {errors.location && (
                                        <span style={{ color: "red" }}>
                                            {errors.location.message}
                                        </span>
                                    )}
                                </div>
                            </div>
                            <div className={styles.addServerInfoBodyItem}>
                                <span>
                                    IP адрес <span style={{ color: "red" }}>*</span>
                                </span>
                                <input
                                    type="text"
                                    className={styles.addServerInput}
                                    placeholder="192.168.1.100"
                                    {...register('IP')}
                                />
                                {errors.IP && (
                                    <span style={{ color: "red" }}>
                                        {errors.IP.message}
                                    </span>
                                )}
                            </div>
                            <div className={styles.addServerInfoBodyItem}>
                                <span>Порт</span>
                                <input
                                    type="text"
                                    className={styles.addServerInput}
                                    {...register('port')}
                                />
                                {errors.port && (
                                    <span style={{ color: "red" }}>
                                        {errors.port.message}
                                    </span>
                                )}
                            </div>
                            <div className={styles.addServerInfoBodyItem}>
                                <span>Максимум пользователей</span>
                                <input
                                    type="number"
                                    className={styles.addServerInput}
                                    {...register('maxUsers')}
                                    min="1"
                                    max="100000"
                                    step="1"
                                />
                                {errors.maxUsers && (
                                    <span style={{ color: "red" }}>
                                        {errors.maxUsers.message}
                                    </span>
                                )}
                            </div>
                            <div className={styles.addServerInfoBodyItem}>
                                <span>Статус</span>
                                <div className={styles.addServerInput}>
                                    <select {...register('status')}>
                                        <option value="">Выберите статус</option>
                                        {Object.entries(SERVER_STATUS).map(([, status]) => (
                                            <option value={status}>{status}</option>
                                        ))}
                                    </select>
                                    {errors.status && (
                                        <span style={{ color: "red" }}>
                                            {errors.status.message}
                                        </span>
                                    )}
                                </div>
                            </div>
                        </div>
                    </div>
                    <div className={styles.addServerAccess}>
                        <div className={styles.addServerAccessContent}>
                            <div className={styles.addServerAccessHeader}>
                                <span>Данные для доступа к серверу</span>
                            </div>
                            <div className={styles.addServerAccessBody}>
                                <div className={styles.addServerAccessBodyItem}>
                                    <span>
                                        Логин{" "}
                                        <span style={{ color: "red" }}>*</span>
                                    </span>
                                    <input
                                        type="text"
                                        className={styles.addServerInput}
                                        placeholder="admin"
                                        {...register('login')}
                                    />
                                    {errors.login && (
                                        <span style={{ color: "red" }}>
                                            {errors.login.message}
                                        </span>
                                    )}
                                </div>
                                <div className={styles.addServerAccessBodyItem}>
                                    <span>
                                        Пароль{" "}
                                        <span style={{ color: "red" }}>*</span>
                                    </span>
                                    <div className={styles.addServerInput}>
                                        <input
                                            type={visiblePassword ? "text" : "password"}
                                            placeholder="Введите пароль"
                                            {...register('password')}
                                        />
                                        {errors.password && (
                                            <span style={{ color: "red" }}>
                                                {errors.password.message}
                                            </span>
                                        )}
                                        {visiblePassword ? (
                                            <Eye
                                                size={15}
                                                onClick={() => setVisiblePassword(false)}
                                            />
                                        ) : (
                                            <EyeOff
                                                size={15}
                                                onClick={() => setVisiblePassword(true)}
                                            />
                                        )}
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div className={styles.addServerAccessSafety}>
                            <div>
                                <TriangleAlert size={20} />
                                <span>Важно</span>
                            </div>
                            <span>
                                Эти данные будут использоваться для подключения к
                                серверу и управления конфигурациями. Убедитесь, что
                                пароль достаточно сложный и не используется в других
                                системах.
                            </span>
                        </div>
                    </div>
                    <div className={styles.addServerButtonRecommendations}>
                        <div className={styles.addServerButton}>
                            <button className={"buttonCreate"}>
                                <Plus size={20} />
                                Добавить сервер
                            </button>
                            <button className={"buttonCancel"}>Отмена</button>
                        </div>
                        <div className={styles.addServerRecommendations}>
                            <div className={styles.addServerRecommendationsHeader}>
                                <Lightbulb size={20} />
                                <span>Рекомендации</span>
                            </div>
                            <ul className={styles.addServerRecommendationsContent}>
                                <li>Убедитесь, что IP адрес доступен из интернета</li>
                                <li>Проверьте открытость указанного порта</li>
                                <li>Используйте понятные названия для серверов</li>
                                <li>Сохраните данные доступа в безопасном месте</li>
                            </ul>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    );
}