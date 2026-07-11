import styles from "./Configuration.module.css"
import { X, Copy, Check } from "lucide-react";
import useUsersStore from "@store/useUsersStore.ts";
import { useEffect, useRef, useState } from "react";
import { UserService } from "@api/services/userService.ts";

export default function Configuration({ userId }: { userId: number }) {
    const { setConfiguration } = useUsersStore();
    const [copied, setCopied] = useState<string>("");
    const [code, setCode] = useState<string>("");
    const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
    const [isLoading, setIsLoading] = useState<boolean>(false);

    const handleCopy = async (name: "code" | "url") => {
        try {
            const textToCopy =
                name === "url" && code
                    ? `https://api.vpn.maxow.ru/v1/config/${code}`
                    : code;
            await navigator.clipboard.writeText(textToCopy);
            if (timeoutRef.current) {
                clearTimeout(timeoutRef.current);
            }
            setCopied(name);
            timeoutRef.current = setTimeout(() => {
                setCopied("");
                timeoutRef.current = null;
            }, 1000);
        } catch (err) {
            console.error("Ошибка при копировании: ", err);
        }
    };

    useEffect(() => {
        const fetchCode = async () => {
            try {
                setIsLoading(true);
                const data = await UserService.getCodeUser(userId);
                setCode(data);
            } catch (error) {
                console.error(error);
            }
            setIsLoading(false);
        };
        fetchCode();
    }, [userId]);

    return (
        <div
            className={styles.overlay}
            onClick={() => {
                setConfiguration(null);
            }}
        >
            <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
                <div className={styles.header}>
                    <span>Конфигурация</span>
                    <div
                        onClick={() => {
                            setConfiguration(null);
                        }}
                        className={styles.cancel}
                    >
                        <X size={20} />
                    </div>
                </div>
                {isLoading ? (
                    <div
                        className={"loadingText"}
                        style={{ padding: "25px 0" }}
                    >
                        Загрузка конфигурации...
                    </div>
                ) : (
                    <div className={styles.content}>
                        <div className={styles.desc}>
                            <span className={styles.descHeader}>
                                ПОЛУЧИТЬ КОНФИГУРАЦИЮ ПО КОДУ ВЕРИФИКАЦИИ
                            </span>
                            <span className={styles.descText}>
                                Используйте этот код или ссылку для получения
                                настроек VPN подключения.
                            </span>
                        </div>
                        <div className={styles.input}>
                            <span className={styles.inputHeader}>
                                КОД ВЕРИФИКАЦИИ
                            </span>
                            <div className={styles.inputContent}>
                                <input
                                    type={"text"}
                                    readOnly
                                    value={code || ""}
                                />
                                <div
                                    className={styles.copy}
                                    onClick={() => handleCopy("code")}
                                >
                                    <div
                                        className={`${styles.iconContainer} ${copied === "code" ? styles.showCheck : ""}`}
                                    >
                                        <Copy
                                            className={styles.copyIcon}
                                            size={16}
                                        />
                                        <Check
                                            className={styles.checkIcon}
                                            size={16}
                                        />
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div className={styles.input}>
                            <span className={styles.inputHeader}>
                                ССЫЛКА НА КОНФИГУРАЦИЮ
                            </span>
                            <div className={styles.inputContent}>
                                <input
                                    className={styles.url}
                                    type={"text"}
                                    readOnly
                                    value={
                                        code
                                            ? `https://api.vpn.maxow.ru/v1/config/${code}`
                                            : ""
                                    }
                                />
                                <div
                                    className={styles.copy}
                                    onClick={() => handleCopy("url")}
                                >
                                    <div
                                        className={`${styles.iconContainer} ${copied === "url" ? styles.showCheck : ""}`}
                                    >
                                        <Copy
                                            className={styles.copyIcon}
                                            size={16}
                                        />
                                        <Check
                                            className={styles.checkIcon}
                                            size={16}
                                        />
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                )}
                <div className={styles.footer}>
                    <button
                        className={`buttonCreate ${styles.close}`}
                        onClick={() => {
                            setConfiguration(null);
                        }}
                    >
                        Закрыть
                    </button>
                </div>
            </div>
        </div>
    );
}