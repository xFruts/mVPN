import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {MoveLeft, UserPlus, Lightbulb} from "lucide-react";
import {NavLink, useNavigate} from "react-router";
import styles from "./AddUsers.module.css";
import {
    USER_ROLES,
    ROLE_CONFIG
} from "@/constant.ts";
import { UserService } from "@api/services/userService.ts";

const schema = z.object({
    fullName: z.string(),
    userTelegramId: z.number().optional(),
    role: z.enum(USER_ROLES),
});

type FormData = z.infer<typeof schema>;
type UserRole = z.infer<typeof schema>["role"];

export default function AddUsers() {
    const navigate = useNavigate();
    const {
        register,
        watch,
        handleSubmit,
        setValue,
        formState: { errors },
    } = useForm<FormData>({
        resolver: zodResolver(schema),
        values: {
            fullName: "",
            userTelegramId: 0,
            role: "REGULAR",
        },
    });

    const onSubmit = async () => {
        try {
            const createUser = {
                fullName: watch("fullName"),
                userTelegramId: watch("userTelegramId") || 0,
                role: watch("role")
            }
            await UserService.createUser(createUser);
            navigate("/users");
        }
        catch (error) {
            console.error(error);
        }
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
                                        Ник <span style={{ color: "red" }}>*</span>
                                    </span>
                                    <input
                                        type="text"
                                        className={styles.addusersPersonInput}
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
                            <div className={styles.addusersPersonTelegram}>
                                <span>Telegram ID (опционально)</span>
                                <input
                                    type="text"
                                    className={styles.addusersPersonInput}
                                    placeholder="Введите ID"
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
                </div>
                <div className={styles.addusersSearchInfo}>
                    <div className={styles.addusersSearch}>
                        <div className={styles.addusersSearchHeader}>
                            <span>Предварительный просмотр</span>
                        </div>
                        <div className={styles.addusersSearchDetails}>
                            <span style={{ color: "gray", fontSize: "15px" }}>Пользователь</span>
                            <span>
                                {watch("fullName") === "" ? "Ник" : watch("fullName")}{" "}
                            </span>
                        </div>
                        <div className={styles.addusersSearchDetails}>
                            <span style={{ color: "gray", fontSize: "15px" }}>Роль</span>
                            <span className={`${styles.userRole} ${styles[watch("role").toLowerCase()]}`}>
                                {watch("role")}
                            </span>
                        </div>
                        {watch("userTelegramId") && (
                            <div className={styles.addusersSearchDetails}>
                                <span style={{ color: "gray", fontSize: "15px" }}>Telegram ID</span>
                                <span>{watch("userTelegramId")}</span>
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
                <button className={"buttonCancel"} onClick={() => {navigate("/users");}}>Отмена</button>
            </div>
        </div>
    );
}