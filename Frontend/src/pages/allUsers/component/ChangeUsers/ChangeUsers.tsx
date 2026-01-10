import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useState } from "react";
import { MoveLeft, Save, TriangleAlert } from "lucide-react";
import { NavLink, useLocation } from "react-router";
import styles from "./ChangeUsers.module.css";
import ReactDOM from "react-dom";
import Countries from "../Countries/Countries.tsx";
import {
    USER_ROLES,
    ROLE_CONFIG,
    USER_TYPES,
    TYPE_CONFIG,
} from "@/constant.ts";

const schema = z.object({
    firstName: z.string(),
    lastName: z.string(),
    telegramId: z.string().optional(),
    role: z.enum(USER_ROLES),
    type: z.enum(USER_TYPES),
    dateFinish: z.string().optional(),
});

type FormData = z.infer<typeof schema>;
type UserRole = z.infer<typeof schema>["role"];
type UserType = z.infer<typeof schema>["type"];

export default function ChangeUsers() {
    const location = useLocation();
    const user = location.state?.props;
    const [selectedUser, setSelectedUser] = useState<null | {
        id: number;
        country: string;
    }>(null);

    const {
        register,
        watch,
        handleSubmit,
        setValue,
        formState: { errors },
    } = useForm<FormData>({
        resolver: zodResolver(schema),
        defaultValues: {
            firstName: user.firstname,
            lastName: user.lastname,
            telegramId: user.telegramId,
            role: user.role,
            type: user.type,
            dateFinish: user.dateFinish,
        },
    });

    const onSubmit = (data: FormData) => {
        console.log(data);
    };

    const role = watch("role");
    const type = watch("type");

    const handleOpen = (userId: number, country: string) => {
        setSelectedUser({ id: userId, country });
    };

    const handleClose = () => setSelectedUser(null);

    const addDaysToDate = (days: number) => {
        const baseDateStr =
            watch("dateFinish") || new Date().toISOString().split("T")[0];
        const baseDate = new Date(baseDateStr);
        baseDate.setDate(baseDate.getDate() + days);
        const newDate = baseDate.toISOString().split("T")[0];
        setValue("dateFinish", newDate);
    };

    const resetDate = () => {
        setValue("dateFinish", user.dateFinish);
    };

    return (
        <div className={styles.changeUsers}>
            <div className={styles.changeUsersHeader}>
                <div className={styles.changeUsersHeaderTitle}>
                    <span style={{ fontSize: "23px" }}>
                        Редактирование пользователя
                    </span>
                    <span style={{ color: "gray" }}>
                        Изменение данных пользователя #{user.id}
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
                            onSubmit={handleSubmit(onSubmit)}
                        >
                            <div className={styles.changeUsersPersonNameSurname}>
                                <div className={styles.changeUsersPersonName}>
                                    <span>
                                        Имя{" "}
                                        <span style={{ color: "red" }}>*</span>
                                    </span>
                                    <input
                                        type="text"
                                        className={styles.changeUsersPersonInput}
                                        placeholder="Введите имя"
                                        {...register("firstName")}
                                    />
                                    {errors.firstName && (
                                        <span style={{ color: "red" }}>
                                            {errors.firstName.message}
                                        </span>
                                    )}
                                </div>
                                <div className={styles.changeUsersPersonSurname}>
                                    <span>
                                        Фамилия{" "}
                                        <span style={{ color: "red" }}>*</span>
                                    </span>
                                    <input
                                        type="text"
                                        className={styles.changeUsersPersonInput}
                                        placeholder="Введите фамилию"
                                        {...register("lastName")}
                                    />
                                    {errors.lastName && (
                                        <span style={{ color: "red" }}>
                                            {errors.lastName.message}
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
                                    {...register("telegramId")}
                                />
                                {errors.telegramId && (
                                    <span style={{ color: "red" }}>
                                        {errors.telegramId.message}
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
                    <div className={styles.changeUsersType}>
                        <div className={styles.changeUsersTypeHeader}>
                            Тип подписки
                        </div>
                        <div className={styles.changeUsersTypeContent}>
                            {Object.entries(TYPE_CONFIG).map(
                                ([typeKey, config]) => {
                                    const IconComponent = config.icon;
                                    return (
                                        <div
                                            key={typeKey}
                                            className={`${styles.changeUsersTypeSpecific} ${
                                                type === typeKey ? styles.roleActive : ""
                                            }`}
                                            onClick={() =>
                                                setValue("type", typeKey as UserType)
                                            }
                                        >
                                            <span>
                                                <IconComponent size={20} />
                                            </span>
                                            <span>{typeKey as UserType}</span>
                                            <span
                                                className={`${styles.price} ${styles[config.priceClass]}`}
                                            >
                                                {config.description}
                                            </span>
                                            <div className={styles.changeUsersTypeSpecificInfo}>
                                                <span>{config.features[0]}</span>
                                                <span>{config.features[1]}</span>
                                            </div>
                                        </div>
                                    );
                                },
                            )}
                        </div>
                        <div className={styles.changeUsersDate}>
                            <div className={styles.changeUsersDateHeader}>
                                <span>Срок действия</span>
                            </div>
                            <input
                                className={styles.changeUsersDateInput}
                                type="date"
                                {...register("dateFinish")}
                            />
                            <div className={styles.changeUsersDateFast}>
                                <span style={{ fontSize: "13px", color: "gray" }}>
                                    Быстрое продление
                                </span>
                                <div className={styles.changeUsersDateFastChange}>
                                    <div
                                        className={`${styles.changeUsersDateFastChangeDetail} ${styles.dynamic}`}
                                        onClick={() => addDaysToDate(1)}
                                    >
                                        +1 день
                                    </div>
                                    <div
                                        className={`${styles.changeUsersDateFastChangeDetail} ${styles.basic}`}
                                        onClick={() => addDaysToDate(7)}
                                    >
                                        +7 день
                                    </div>
                                    <div
                                        className={`${styles.changeUsersDateFastChangeDetail} ${styles.special}`}
                                        onClick={() => addDaysToDate(30)}
                                    >
                                        +30 день
                                    </div>
                                    <div
                                        className={`${styles.changeUsersDateFastChangeDetail} ${styles.none}`}
                                        onClick={() => resetDate()}
                                    >
                                        Сброс даты
                                    </div>
                                </div>
                            </div>
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
                            <span>#{user.id}</span>
                        </div>
                        <div className={styles.changeUsersSearchDetails}>
                            <span style={{ color: "gray", fontSize: "15px" }}>
                                Дата создания
                            </span>
                            <span>{user.dateStart}</span>
                        </div>
                        <div className={styles.changeUsersSearchDetails}>
                            <span style={{ color: "gray", fontSize: "15px" }}>
                                Страны
                            </span>
                            <div className={"countries"}>
                                {user.countries.map((country: string) => (
                                    <span
                                        key={`${user.id}-${country}`}
                                        onClick={() => handleOpen(user.id, country)}
                                        style={{ cursor: "pointer" }}
                                    >
                                        {country}
                                    </span>
                                ))}
                            </div>
                        </div>
                    </div>
                    <div className={styles.changeUsersInfotype}>
                        <div className={styles.changeUsersInfotypeHeader}>
                            <span>Информация о подписке</span>
                        </div>
                        <div className={styles.changeUsersInfotypeContent}>
                            <span style={{ color: "gray" }}>Подписка</span>
                            <span>{type}</span>
                            {type === "VIP" ? (
                                <div style={{ color: "gray" }}>
                                    <span>365 дней</span>
                                    <span>Безлимит</span>
                                    <span>2999 ₽</span>
                                </div>
                            ) : type === "BASIC" ? (
                                <div style={{ color: "gray" }}>
                                    <span>30 дней</span>
                                    <span>300 ГБ</span>
                                    <span>299 ₽</span>
                                </div>
                            ) : type === "TRIAL" ? (
                                <div style={{ color: "gray" }}>
                                    <span>7 дней</span>
                                    <span>50 ГБ</span>
                                    <span>Бесплатно</span>
                                </div>
                            ) : (
                                <div style={{ color: "gray" }}>
                                    <span>-</span>
                                    <span>-</span>
                                    <span>-</span>
                                </div>
                            )}
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
                <button className={"buttonCancel"}>Отмена</button>
            </div>

            {selectedUser &&
                ReactDOM.createPortal(
                    <Countries
                        userId={selectedUser.id}
                        specificCountry={selectedUser.country}
                        firstName={user.firstname}
                        lastName={user.lastname}
                        countries={user.countries}
                        onClose={handleClose}
                    />,
                    document.body,
                )}
        </div>
    );
}