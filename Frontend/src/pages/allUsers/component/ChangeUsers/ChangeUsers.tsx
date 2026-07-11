import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useEffect, useState } from "react";
import { ArrowLeft } from "lucide-react";
import { NavLink, useNavigate, useParams } from "react-router";
import styles from "./ChangeUsers.module.css";
import { USER_ROLES, ROLE_CONFIG, USER_STATUS } from "@/constant.ts";
import { UserService } from "@api/services/userService.ts";
import type { UserUpdate } from "@/types/user.ts";
import UserTariffs from "@pages/allUsers/component/UserTariffs/UserTariffs.tsx";

const schema = z.object({
    fullName: z.string().min(1, "Ник обязателен"),
    userTelegramId: z.number().nullish(),
    role: z.enum(USER_ROLES),
    tariffName: z.string().nullish(),
    subscriptionStatus: z.enum(USER_STATUS).nullish(),
    startDate: z.string().nullish(),
    subscriptionEndDate: z.string().nullish(),
});

type FormData = z.infer<typeof schema>;
type UserRole = z.infer<typeof schema>["role"];

const defaultValues: FormData = {
    fullName: "",
    role: "REGULAR",
    userTelegramId: null,
    subscriptionStatus: "CANCELLED",
};

export default function ChangeUsers() {
    const navigate = useNavigate();
    const { id } = useParams();
    const isEditMode = Boolean(id);
    const userId = Number(id);
    const [isLoading, setIsLoading] = useState<boolean>(false)

    const {
        register,
        watch,
        handleSubmit,
        reset,
        setValue,
        formState: { errors },
    } = useForm<FormData>({
        resolver: zodResolver(schema),
        defaultValues,
    });

    useEffect(() => {
        if (isEditMode) {
            setIsLoading(true);
            UserService.getUserById(userId)
                .then((data) => {
                    reset(data);
                })
                .catch((err) =>
                    console.error("Ошибка загрузки пользователя", err),
                )
                .finally(() => {
                    setIsLoading(false);
                });
        } else {
            reset(defaultValues);
            setIsLoading(false);
        }
    }, [isEditMode, userId, reset]);

    const onSubmit = async (data: FormData) => {
        try {
            const userData = {
                ...data,
                userTelegramId:
                    data.userTelegramId && !isNaN(data.userTelegramId)
                        ? data.userTelegramId
                        : null,
            };

            if (isEditMode) {
                await UserService.updateUser(userId, userData as UserUpdate);
            } else {
                await UserService.createUser(userData);
            }

            navigate("/users");
        } catch (error) {
            console.error("Ошибка при сохранении:", error);
        }
    };

    const role = watch("role");
    const tgId = watch("userTelegramId");

    return (
        <div className={styles.container}>
            {isLoading ? (
                <div className={"loadingText"}>Загрузка пользователя...</div>
            ) : (
                <>
                    <div className={styles.header}>
                        <div className={styles.titleStack}>
                            <span className={styles.title}>
                                {isEditMode
                                    ? "Редактирование пользователя"
                                    : "Добавление пользователя"}
                            </span>
                            <span className={styles.subtitle}>
                                {isEditMode
                                    ? `Изменение данных пользователя #${id}`
                                    : "Создание нового пользователя VPN сервиса"}
                            </span>
                        </div>
                        <NavLink to={"/users"} end className={styles.backLink}>
                            <ArrowLeft size={16} />
                            <span>Назад к списку</span>
                        </NavLink>
                    </div>

                    <div className={styles.content}>
                        <div className={styles.leftCol}>
                            <div className={styles.card}>
                                <div className={styles.cardHeader}>
                                    <span>Персональная информация</span>
                                </div>
                                <form
                                    id="change-user-form"
                                    onSubmit={handleSubmit(onSubmit, (errors) =>
                                        console.log(
                                            "Ошибки валидации:",
                                            errors,
                                        ),
                                    )}
                                >
                                    <div className={styles.row}>
                                        <div className={styles.field}>
                                            <span>
                                                НИК{" "}
                                                <span
                                                    style={{
                                                        color: "#06b6d4",
                                                        fontWeight: "normal",
                                                    }}
                                                >
                                                    *
                                                </span>
                                            </span>
                                            <input
                                                type="text"
                                                className={styles.input}
                                                placeholder="Введите ник"
                                                {...register("fullName")}
                                            />
                                            {errors.fullName && (
                                                <span className={styles.error}>
                                                    {errors.fullName.message}
                                                </span>
                                            )}
                                        </div>
                                    </div>
                                    <div
                                        className={styles.field}
                                        style={{ marginTop: "15px" }}
                                    >
                                        <span>Telegram ID (опционально)</span>
                                        <input
                                            type="number"
                                            className={styles.input}
                                            placeholder="Введите Telegram ID"
                                            {...register("userTelegramId", {
                                                valueAsNumber: true,
                                            })}
                                        />
                                        {errors.userTelegramId && (
                                            <span className={styles.error}>
                                                {errors.userTelegramId.message}
                                            </span>
                                        )}
                                        <span className={styles.hint}>
                                            Если указан, пользователь сможет
                                            управлять подпиской через бота
                                        </span>
                                    </div>
                                </form>
                            </div>

                            <div className={styles.card}>
                                <div className={styles.cardHeader}>
                                    <span>Роль пользователя</span>
                                </div>
                                <div className={styles.roleGrid}>
                                    {Object.entries(ROLE_CONFIG).map(
                                        ([typeKey, config]) => {
                                            const IconComponent = config.icon;
                                            return (
                                                <div
                                                    key={typeKey}
                                                    className={`${styles.roleItem} ${role === typeKey ? styles.roleActive : ""}`}
                                                    onClick={() =>
                                                        setValue(
                                                            "role",
                                                            typeKey as UserRole,
                                                        )
                                                    }
                                                >
                                                    <IconComponent
                                                        size={24}
                                                        className={
                                                            styles.roleIcon
                                                        }
                                                    />
                                                    <span
                                                        className={
                                                            styles.roleName
                                                        }
                                                    >
                                                        {typeKey}
                                                    </span>
                                                    <span
                                                        className={
                                                            styles.roleDesc
                                                        }
                                                    >
                                                        {config.description}
                                                    </span>
                                                </div>
                                            );
                                        },
                                    )}
                                </div>
                            </div>
                        </div>

                        <div className={styles.rightCol}>
                            <div className={styles.card}>
                                <div className={styles.cardHeader}>
                                    <span>Информация о пользователе</span>
                                </div>
                                <div className={styles.previewItem}>
                                    <span className={styles.previewLabel}>
                                        ПОЛЬЗОВАТЕЛЬ
                                    </span>
                                    <span className={styles.previewInput}>
                                        {watch("fullName") || "Ник"}
                                    </span>
                                </div>
                                <div className={styles.previewItem}>
                                    <span className={styles.previewLabel}>
                                        РОЛЬ
                                    </span>
                                    <span
                                        className={`${styles.badge} ${styles[watch("role").toLowerCase()]}`}
                                    >
                                        {role}
                                    </span>
                                </div>
                                {isEditMode && (
                                    <div className={styles.previewItem}>
                                        <span className={styles.previewLabel}>
                                            ДАТА СОЗДАНИЯ
                                        </span>
                                        <span className={styles.previewInput}>
                                            {watch("startDate")
                                                ?.toString()
                                                .slice(0, 10)}
                                        </span>
                                    </div>
                                )}
                                {Number.isFinite(tgId) && (
                                    <div className={styles.previewItem}>
                                        <span className={styles.previewLabel}>
                                            TELEGRAM ID
                                        </span>
                                        <span className={styles.previewInput}>
                                            {tgId}
                                        </span>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>

                    <div className={styles.actions}>
                        <button
                            form={"change-user-form"}
                            className={"buttonCreate"}
                        >
                            {isEditMode
                                ? "Сохранить изменения"
                                : "Создать пользователя"}
                        </button>
                        <button
                            type="button"
                            className={"buttonCancel"}
                            onClick={() => navigate("/users")}
                        >
                            Отмена
                        </button>
                    </div>
                    {isEditMode && (
                        <div className={styles.history}>
                            <div className={styles.leftCol}>
                                <div className={styles.card}>
                                    <UserTariffs />
                                </div>
                            </div>
                            <div className={styles.rightCol}></div>
                        </div>
                    )}
                </>
            )}
        </div>
    );
}
