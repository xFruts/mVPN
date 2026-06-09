import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {useEffect, useState} from "react";
import { MoveLeft, Save, TriangleAlert } from "lucide-react";
import {NavLink, useNavigate, useParams} from "react-router";
import styles from "./ChangeUsers.module.css";
import {
    USER_ROLES,
    ROLE_CONFIG,
    USER_STATUS
} from "@/constant.ts";
import { UserService } from "@api/services/userService.ts";
import type { UserGet, UserUpdate } from "@/types/user.ts";

const schema = z.object({
    fullName: z.string(),
    userTelegramId: z.number().optional(),
    role: z.enum(USER_ROLES),
    tariffName: z.string(),
    subscriptionStatus: z.enum(USER_STATUS),
    startDate: z.string().optional(),
    subscriptionEndDate: z.string(),
});

type FormData = z.infer<typeof schema>;
type UserRole = z.infer<typeof schema>["role"];

export default function ChangeUsers() {
    const navigate = useNavigate();
    const { id } = useParams();
    const userId = Number(id);
    const [user, setUser] = useState<UserGet>();

    const {
        register,
        watch,
        handleSubmit,
        setValue,
        formState: { errors },
    } = useForm<FormData>({
        resolver: zodResolver(schema),
        values: user ? {
            fullName: user.fullName,
            userTelegramId: user.userTelegramId || undefined,
            role: user.role,
            tariffName: user.tariffName,
            subscriptionStatus: user.subscriptionStatus,
            startDate: user.startDate,
            subscriptionEndDate: user.subscriptionEndDate,
        } : undefined
    });

    useEffect(() => {
        const load = async () => {
            const data = await UserService.getUserById(userId);
            setUser(data);
        };
        load();
    }, [userId]);

    if (!user) {
        return <div>Загрузка данных...</div>; // Или спиннер
    }

    const onSubmit = async () => {
        try {
            const updateUser : UserUpdate = {
                fullName: watch("fullName"),
                userTelegramId: watch("userTelegramId") || 0,
                role: watch("role"),
                subscriptionStatus: watch("subscriptionStatus"),
                subscriptionEndDate: watch("subscriptionEndDate"),

            }
            await UserService.updateUser(userId, updateUser)
            console.log(updateUser)
            navigate("/users");
        }
        catch (error) {
            console.log(error);
        }
    };

    const role = watch("role");

    return (
        <div className={styles.changeUsers}>
            <div className={styles.changeUsersHeader}>
                <div className={styles.changeUsersHeaderTitle}>
                    <span style={{ fontSize: "23px" }}>
                        Редактирование пользователя
                    </span>
                    <span style={{ color: "gray" }}>
                        Изменение данных пользователя #{id}
                    </span>
                </div>
                <NavLink to={"/users"} end className={styles.changeUsersHeaderBack}>
                    <MoveLeft size={13} />
                    <span>Назад к списку</span>
                </NavLink>
            </div>
            <div className={styles.changeUsersContent}>
                <div className={styles.changeUsersPersonRoleType}>
                    <div className={styles.changeUsersPerson}>
                        <div className={styles.changeUsersPersonHeader}>
                            <span>Персональная информация</span>
                        </div>
                        <form
                            id="change-user-form"
                            onSubmit={handleSubmit(onSubmit, (errors) => console.log("Ошибки валидации:", errors))}
                        >
                            <div className={styles.changeUsersPersonNameSurname}>
                                <div className={styles.changeUsersPersonName}>
                                    <span>
                                        Ник{" "}
                                        <span style={{ color: "red" }}>*</span>
                                    </span>
                                    <input
                                        type="text"
                                        className={styles.changeUsersPersonInput}
                                        placeholder="Введите ник"
                                        {...register("fullName")}
                                    />
                                    {errors.fullName && (
                                        <span style={{ color: "red" }}>
                                            {errors.fullName.message}
                                        </span>
                                    )}
                                </div>
                            </div>
                            <div className={styles.changeUsersPersonTelegram}>
                                <span>Telegram ID (опционально)</span>
                                <input
                                    type="text"
                                    className={styles.changeUsersPersonInput}
                                    placeholder="Введите имя"
                                    {...register("userTelegramId", { valueAsNumber: true })}
                                />
                                {errors.userTelegramId && (
                                    <span style={{ color: "red" }}>
                                        {errors.userTelegramId.message}
                                    </span>
                                )}
                                <span style={{ color: "gray" }}>
                                    Если указан, пользователь сможет управлять
                                    подпиской через Telegram бота
                                </span>
                            </div>
                        </form>
                    </div>
                    <div className={styles.changeUsersRole}>
                        <div className={styles.changeUsersRoleHeader}>
                            <span>Роль пользователя</span>
                        </div>
                        <div className={styles.changeUsersRoleContent}>
                            {Object.entries(ROLE_CONFIG).map(
                                ([typeKey, config]) => {
                                    console.log("Сравнение:", role, "с", typeKey);
                                    const IconComponent = config.icon;
                                    return (
                                        <div
                                            key={typeKey}
                                            className={`${styles.changeUsersRoleSpecific} ${
                                                role === typeKey ? styles.roleActive : ""
                                            }`}
                                            onClick={() =>
                                                setValue("role", typeKey as UserRole)
                                            }
                                        >
                                            <span>
                                                <IconComponent size={20} />
                                            </span>
                                            <span
                                                style={{
                                                    fontSize: "15px",
                                                    fontWeight: "bold",
                                                }}
                                            >
                                                {typeKey as UserRole}
                                            </span>
                                            <span style={{ color: "gray" }}>
                                                {config.description}
                                            </span>
                                        </div>
                                    );
                                },
                            )}
                        </div>
                    </div>
                </div>
                <div className={styles.changeUsersSearchInfotypeImportant}>
                    <div className={styles.changeUsersSearch}>
                        <div className={styles.changeUsersSearchHeader}>
                            <span>Информация о пользователе</span>
                        </div>
                        <div className={styles.changeUsersSearchDetails}>
                            <span style={{ color: "gray", fontSize: "15px" }}>
                                ID пользователя
                            </span>
                            <span>#{id}</span>
                        </div>
                        <div className={styles.changeUsersSearchDetails}>
                            <span style={{ color: "gray" }}>
                                Код верификации
                            </span>
                            <span>Будет сгенерирован</span>
                        </div>
                    </div>
                    <div className={styles.changeUsersImportant}>
                        <div className={styles.changeUsersImportantHeader}>
                            <TriangleAlert size={20} />
                            <span>Важно</span>
                        </div>
                        <ul className={styles.changeUsersImportantContent}>
                            <li>Изменения вступят в силу немедленно</li>
                            <li>Пользователь получит уведомление</li>
                            <li>
                                При смене типа подписки, конфигурации обновятся
                            </li>
                        </ul>
                    </div>
                </div>
            </div>
            <div className={styles.changeUsersCreate}>
                <button form={"change-user-form"} className={"buttonCreate"}>
                    <Save size={20} />
                    Сохранить изменения
                </button>
                <button className={"buttonCancel"} onClick={() => {navigate("/users");}}>Отмена</button>
            </div>
        </div>
    );
}