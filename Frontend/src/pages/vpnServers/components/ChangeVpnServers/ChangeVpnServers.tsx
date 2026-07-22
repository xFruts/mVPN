import styles from "./ChangeVpnServers.module.css"
import { z } from "zod";
import { useEffect, useState } from "react";
import { Info, X } from "lucide-react";
import { schema } from "./ChangeSchema.tsx"
import useServersStore from "@store/useServersStore.ts";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ServerService } from "@api/services/serverService.ts";

type FormData = z.infer<typeof schema>;

const defaultValues: Partial<FormData> = {
    name: "",
    location: "",
    countryEmoji: "",
    ip: "",
    status: "ACTIVE",
    port: 2053,
    maxUsers: 100,
    maxTraffic: 500,
    sshAuthType: "PASSWORD",
    subscriptionFormat: "VLESS",
    login: "",
    password: "",
    xuiLogin: "",
    xuiPassword: "",
    webBasePath: "",
};

export default function ChangeVpnServers() {
    const { setIsChangeOpen, isOpen, fetchServers } = useServersStore();
    const [selectedFile, setSelectedFile] = useState<File | null>(null);
    const [isLoading, setIsLoading] = useState<boolean>(false);

    const fileName = selectedFile ? selectedFile.name : "Файл не выбран";
    const {
        register,
        handleSubmit,
        reset,
        watch,
        formState: { errors },
    } = useForm<FormData>({
        resolver: zodResolver(schema)
    });

    useEffect(() => {
        if (isOpen !== -1) {
            setIsLoading(true);
            ServerService.getServerById(isOpen).then((data) => {
                reset(data);
            }).finally(() => {setIsLoading(false)});
        } else {
            reset(defaultValues);
            setIsLoading(false);
        }
    }, [isOpen, reset]);

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (e.target.files && e.target.files.length > 0) {
            const file = e.target.files[0];
            const hasExtension = file.name.includes(".");
            if (hasExtension) {
                alert(
                    "Пожалуйста, выберите приватный ключ",
                );
                e.target.value = "";
                return;
            }
            setSelectedFile(file);
        }
    };

    const ErrorMsg = (field: keyof FormData) => {
        const error = errors[field];
        return error ? (
            <span
                style={{
                    color: "#ef4444",
                    fontSize: "12px",
                    marginTop: "4px",
                    display: "block",
                }}
            >
                {String(error.message)}
            </span>
        ) : null;
    };

    const clearEmptyStrings = (obj: Record<string, unknown>) => {
        return Object.fromEntries(
            Object.entries(obj).filter(([, value]) => value !== ""),
        );
    };

    const onSubmit = async (data: FormData) => {
        try {
            let sshKeyPath: string = "";
            if (selectedFile) {
                sshKeyPath = await ServerService.loadSshKey(selectedFile);
            }
            console.log(sshKeyPath);
            const serverPayload = {
                ...data,
                sshPrivateKeyObjectKey: sshKeyPath,
            };

            const cleanedData = clearEmptyStrings(serverPayload);
            console.log("ОЧИЩЕННЫЕ ДАННЫЕ:", cleanedData);
            if (isOpen === -1) {
                await ServerService.createServer(cleanedData);
            }
            else {
                await ServerService.updateServer(isOpen, cleanedData);
            }
            await fetchServers({});
            setIsChangeOpen(false);

        } catch (error) {
            console.error(error);
        }
    };

    console.log("ТЕКУЩИЙ isOpen:", isOpen);

    return (
        <div
            className={styles.modalOverlay}
            onClick={(e) => {
                if (e.target === e.currentTarget) {
                    setIsChangeOpen(false);
                }
            }}
        >
            <div
                className={styles.modalContent}
                onClick={(e) => e.stopPropagation()}
            >
                <div className={styles.header}>
                    {isOpen === -1 ? (
                        <span>Новый VPN узел</span>
                    ) : (
                        <span>Настройки сервера: {watch("name")}</span>
                    )}

                    <X
                        size={24}
                        className={styles.X}
                        onClick={() => setIsChangeOpen(false)}
                    />
                </div>
                {isLoading ? (
                    <div
                        className={"loadingText"}
                        style={{ marginBottom: "30px", paddingTop: "30px" }}
                    >
                        Загрузка сервера...
                    </div>
                ) : (
                    <>
                        <form
                            id="change-server-form"
                            onSubmit={handleSubmit(onSubmit)}
                        >
                            <div className={styles.content}>
                                <div className={styles.fullWidth}>
                                    <span>НАЗВАНИЕ СЕРВЕРА</span>
                                    <input
                                        type={"text"}
                                        placeholder={"Germany-VPN-01"}
                                        {...register("name")}
                                    />
                                    {ErrorMsg("name")}
                                </div>
                                <div>
                                    <span>ЛОКАЦИЯ (КОД)</span>
                                    <input
                                        type={"text"}
                                        placeholder={"DE"}
                                        {...register("location")}
                                    />
                                    {ErrorMsg("location")}
                                </div>
                                <div>
                                    <span>ЭМОДЗИ ФЛАГА</span>
                                    <input
                                        type={"text"}
                                        placeholder={"de"}
                                        {...register("countryEmoji")}
                                    />
                                    {ErrorMsg("countryEmoji")}
                                </div>
                                <div>
                                    <span>IP АДРЕС</span>
                                    <input
                                        type={"text"}
                                        placeholder={"192.168.1.10"}
                                        {...register("ip")}
                                    />
                                    {ErrorMsg("ip")}
                                </div>
                                <div>
                                    <span>ПОРТ X-UI</span>
                                    <input
                                        type={"number"}
                                        {...register("port", {
                                            valueAsNumber: true,
                                        })}
                                    />
                                    {ErrorMsg("port")}
                                </div>
                                <div>
                                    <span>ФОРМАТ ПОДПИСКИ</span>
                                    <select {...register("subscriptionFormat")}>
                                        <option value="VLESS">VLESS</option>
                                        <option value="JSON">JSON</option>
                                    </select>
                                    {ErrorMsg("subscriptionFormat")}
                                </div>
                                <div>
                                    <span>SSH ТИП АВТОРИЗАЦИИ</span>
                                    <select {...register("sshAuthType")}>
                                        <option value="PASSWORD">Пароль</option>
                                        <option value="KEY">
                                            Приватный ключ (SSH Key)
                                        </option>
                                    </select>
                                    {ErrorMsg("sshAuthType")}
                                </div>
                                <div>
                                    <span>SSH ЛОГИН</span>
                                    <input
                                        type={"text"}
                                        placeholder={"root"}
                                        {...register("login")}
                                    />
                                    {ErrorMsg("login")}
                                </div>
                                <div>
                                    {watch("sshAuthType") === "KEY" ? (
                                        <>
                                            <span>SSH ПРИВАТНЫЙ КЛЮЧ</span>
                                            <div
                                                className={
                                                    styles.fileInputWrapper
                                                }
                                            >
                                                <input
                                                    type="file"
                                                    id="ssh-file"
                                                    className={
                                                        styles.hiddenInput
                                                    }
                                                    onChange={handleFileChange}
                                                />

                                                <label
                                                    htmlFor="ssh-file"
                                                    className={
                                                        styles.fileButton
                                                    }
                                                >
                                                    Выберите
                                                    <p className={`PC`}>файл</p>
                                                </label>

                                                <span
                                                    className={styles.fileName}
                                                >
                                                    {fileName}
                                                </span>
                                            </div>
                                        </>
                                    ) : (
                                        <>
                                            <span>SSH ПАРОЛЬ</span>
                                            <input
                                                type={"password"}
                                                placeholder={"*******"}
                                                {...register("password")}
                                            />
                                            {ErrorMsg("password")}
                                        </>
                                    )}
                                </div>
                                <div
                                    className={`${styles.fullWidth} ${styles.panel}`}
                                >
                                    <div className={styles.panelHeader}>
                                        <Info size={16} />
                                        <span>X-UI ПАНЕЛЬ АВТОРИЗАЦИИ</span>
                                    </div>
                                    <div className={styles.panelContent}>
                                        <div>
                                            <span>X-UI LOGIN</span>
                                            <input
                                                type={"text"}
                                                placeholder={"admin"}
                                                {...register("xuiLogin")}
                                            />
                                            {ErrorMsg("xuiLogin")}
                                        </div>
                                        <div>
                                            <span>X-UI PASSWORD</span>
                                            <input
                                                type={"password"}
                                                placeholder={"admin"}
                                                {...register("xuiPassword")}
                                            />
                                            {ErrorMsg("xuiPassword")}
                                        </div>
                                        <div>
                                            <span>X-UI AUTH TOKEN</span>
                                            <input
                                                type={"text"}
                                                placeholder={
                                                    "Токен (опционально)"
                                                }
                                                {...register("xuiAuthToken")}
                                            />
                                            {ErrorMsg("xuiAuthToken")}
                                        </div>
                                        <div>
                                            <span>WEB BASE PATH</span>
                                            <input
                                                type={"text"}
                                                placeholder={"Kq8wcGUtqP5"}
                                                {...register("webBasePath")}
                                            />
                                            {ErrorMsg("webBasePath")}
                                        </div>
                                    </div>
                                </div>
                                <div>
                                    <span>ЛИМИТ ПОЛЬЗОВАТЕЛЕЙ</span>
                                    <input
                                        type={"number"}
                                        {...register("maxUsers", {
                                            valueAsNumber: true,
                                        })}
                                    />
                                    {ErrorMsg("maxUsers")}
                                </div>
                                <div>
                                    <span>ЛИМИТ ТРАФИКА (GB)</span>
                                    <input
                                        type={"number"}
                                        {...register("maxTraffic", {
                                            valueAsNumber: true,
                                        })}
                                    />
                                    {ErrorMsg("maxTraffic")}
                                </div>
                            </div>
                        </form>
                        <div className={styles.footer}>
                            <button
                                className={styles.cancel}
                                onClick={() => setIsChangeOpen(false)}
                            >
                                Отмена
                            </button>
                            <button
                                form={"change-server-form"}
                                className={styles.add}
                            >
                                {isOpen === -1 ? (
                                    <span>Добавить сервер</span>
                                ) : (
                                    <span>Сохранить</span>
                                )}
                            </button>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
}