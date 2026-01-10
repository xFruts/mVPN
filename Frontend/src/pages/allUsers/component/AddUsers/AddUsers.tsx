import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { MoveLeft, UserPlus, Lightbulb } from "lucide-react";
import { NavLink } from "react-router";
import styles from "./AddUsers.module.css";
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
});

type FormData = z.infer<typeof schema>;
type UserRole = z.infer<typeof schema>["role"];
type UserType = z.infer<typeof schema>["type"];

export default function AddUsers() {
    const {
        register,
        watch,
        handleSubmit,
        setValue,
        formState: { errors },
    } = useForm<FormData>({
        resolver: zodResolver(schema),
        defaultValues: {
            firstName: "",
            lastName: "",
            telegramId: undefined,
            role: "BASIC",
            type: "NONE",
        },
    });

    const onSubmit = (data: FormData) => {
        console.log(data);
    };

    const getFutureDate = (days: number) => {
        const d = new Date();
        d.setDate(d.getDate() + days);
        return d.toLocaleDateString();
    };

    return (
        <div className={styles.addusers}>
            <div className={styles.addusersHeader}>
                <div className={styles.addusersHeaderTitle}>
                    <span style={{ fontSize: "23px" }}>
                        Добавление пользователя
                    </span>
                    <span style={{ color: "gray" }}>
                        Создание нового пользователя VPN сервиса
                    </span>
                </div>
                <NavLink to={"/users"} end className={styles.addusersHeaderBack}>
                    <MoveLeft size={13} />
                    <span>Назад к списку</span>
                </NavLink>
            </div>
            <div className={styles.addusersContent}>
                <div className={styles.addusersPersonRoleType}>
                    <div className={styles.addusersPerson}>
                        <div className={styles.addusersPersonHeader}>
                            <span>Персональная информация</span>
                        </div>
                        <form id="add-user-form" onSubmit={handleSubmit(onSubmit)}>
                            <div className={styles.addusersPersonNameSurname}>
                                <div className={styles.addusersPersonName}>
                                    <span>
                                        Имя <span style={{ color: "red" }}>*</span>
                                    </span>
                                    <input
                                        type="text"
                                        className={styles.addusersPersonInput}
                                        placeholder="Введите имя"
                                        {...register("firstName")}
                                    />
                                    {errors.firstName && (
                                        <span style={{ color: "red" }}>
                                            {errors.firstName.message}
                                        </span>
                                    )}
                                </div>
                                <div className={styles.addusersPersonSurname}>
                                    <span>
                                        Фамилия <span style={{ color: "red" }}>*</span>
                                    </span>
                                    <input
                                        type="text"
                                        className={styles.addusersPersonInput}
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
                            <div className={styles.addusersPersonTelegram}>
                                <span>Telegram ID (опционально)</span>
                                <input
                                    type="text"
                                    className={styles.addusersPersonInput}
                                    placeholder="Введите ID"
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
                    <div className={styles.addusersRole}>
                        <div className={styles.addusersRoleHeader}>
                            <span>Роль пользователя</span>
                        </div>
                        <div className={styles.addusersRoleContent}>
                            {Object.entries(ROLE_CONFIG).map(([typeKey, config]) => {
                                const IconComponent = config.icon;
                                return (
                                    <div
                                        key={typeKey}
                                        className={`${styles.addusersRoleSpecific} ${
                                            watch("role") === typeKey ? styles.roleActive : ""
                                        }`}
                                        onClick={() => setValue("role", typeKey as UserRole)}
                                    >
                                        <span>
                                            <IconComponent size={20} />
                                        </span>
                                        <span style={{ fontSize: "15px", fontWeight: "bold" }}>
                                            {typeKey as UserRole}
                                        </span>
                                        <span style={{ color: "gray" }}>{config.description}</span>
                                    </div>
                                );
                            })}
                        </div>
                    </div>
                    <div className={styles.addusersType}>
                        <div className={styles.addusersTypeHeader}>Тип подписки</div>
                        <div className={styles.addusersTypeContent}>
                            {Object.entries(TYPE_CONFIG).map(([typeKey, config]) => {
                                const IconComponent = config.icon;
                                return (
                                    <div
                                        key={typeKey}
                                        className={`${styles.addusersTypeSpecific} ${
                                            watch("type") === typeKey ? styles.roleActive : ""
                                        }`}
                                        onClick={() => setValue("type", typeKey as UserType)}
                                    >
                                        <span>
                                            <IconComponent size={20} />
                                        </span>
                                        <span>{typeKey as UserType}</span>
                                        <span className={`${styles.price} ${styles[config.priceClass]}`}>
                                            {config.description}
                                        </span>
                                        <div className={styles.addusersTypeSpecificInfo}>
                                            <span>{config.features[0]}</span>
                                            <span>{config.features[1]}</span>
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    </div>
                </div>
                <div className={styles.addusersSearchInfo}>
                    <div className={styles.addusersSearch}>
                        <div className={styles.addusersSearchHeader}>
                            <span>Предварительный просмотр</span>
                        </div>
                        <div className={styles.addusersSearchDetails}>
                            <span style={{ color: "gray", fontSize: "15px" }}>Пользователь</span>
                            <span>
                                {watch("firstName") === "" ? "Имя" : watch("firstName")}{" "}
                                {watch("lastName") === "" ? "Фамилия" : watch("lastName")}
                            </span>
                        </div>
                        <div className={styles.addusersSearchDetails}>
                            <span style={{ color: "gray", fontSize: "15px" }}>Роль</span>
                            <span className={`${styles.userRole} ${styles[watch("role").toLowerCase()]}`}>
                                {watch("role")}
                            </span>
                        </div>
                        <div className={styles.addusersSearchDetails}>
                            <span style={{ color: "gray", fontSize: "15px" }}>Подписка</span>
                            <span className={`${styles.userRole} ${styles[watch("type").toLowerCase()]}`}>
                                {watch("type")}
                            </span>
                        </div>
                        <div className={styles.addusersSearchDate} style={{ color: "gray", fontSize: "15px" }}>
                            <span>
                                <span>До:</span>
                                {watch("type") === "TRIAL"
                                    ? getFutureDate(7)
                                    : watch("type") === "BASIC"
                                        ? getFutureDate(30)
                                        : watch("type") === "VIP"
                                            ? getFutureDate(365)
                                            : "-"}
                            </span>
                        </div>
                        {watch("telegramId") && (
                            <div className={styles.addusersSearchDetails}>
                                <span style={{ color: "gray", fontSize: "15px" }}>Telegram ID</span>
                                <span>{watch("telegramId")}</span>
                            </div>
                        )}
                    </div>
                    <div className={styles.addusersInfo}>
                        <div className={styles.addusersInfoHeader}>
                            <Lightbulb size={20} />
                            <span>Информация</span>
                        </div>
                        <ul className={styles.addusersInfoContent}>
                            <li>Пользователь получит уведомление по Telegram</li>
                            <li>Подписка активируется автоматически</li>
                            <li>Конфигурации будут сгенерированы</li>
                        </ul>
                    </div>
                </div>
            </div>
            <div className={styles.addusersCreate}>
                <button form="add-user-form" className={"buttonCreate"}>
                    <UserPlus size={20} /> Создать пользователя
                </button>
                <button className={"buttonCancel"}>Отмена</button>
            </div>
        </div>
    );
}