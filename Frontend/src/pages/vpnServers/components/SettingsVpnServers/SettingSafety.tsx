import type { UseFormReturn } from 'react-hook-form';
import { z } from 'zod';
import { schema } from "./SettingVpnServers.tsx";
import { Lock } from "lucide-react";
import styles from "@pages/vpnServers/components/SettingsVpnServers/SettingVpnServers.module.css";

export type FormData = z.infer<typeof schema>;

interface BasicInfoProps {
    form: UseFormReturn<FormData>;
}

export default function SettingSafety({ form }: BasicInfoProps) {
    const { register, formState: { errors } } = form;
    return (
        <>
            <div className={styles.settingSafety}>
                <div className={styles.settingSafetyTitle}>
                    <Lock size={20} />
                    <span>Безопасность</span>
                </div>
                <span>
                    Настройки безопасности влияют на все подключения к серверу.
                    Изменяйте их осторожно.
                </span>
            </div>
            <div className={styles.settingCheckbox}>
                <div className={styles.settingCheckboxHeader}>
                    <span>Методы шифрования</span>
                </div>
                <div className={styles.settingCheckboxItem}>
                    <input type={"checkbox"} {...register('chaCha')}/>
                    <div className={styles.settingCheckboxText}>
                        <span>ChaCha20Poly1305</span>
                    </div>
                    {errors.chaCha && (
                        <span style={{ color: "red" }}>
                                            {errors.chaCha.message}
                                        </span>
                    )}
                </div>
                <div className={styles.settingCheckboxItem}>
                    <input type={"checkbox"} {...register('AES')}/>
                    <div className={styles.settingCheckboxText}>
                        <span>AES256-GCM</span>
                    </div>
                    {errors.AES && (
                        <span style={{ color: "red" }}>
                                            {errors.AES.message}
                                        </span>
                    )}
                </div>
            </div>
            <div className={styles.settingCheckbox}>
                <div className={styles.settingCheckboxHeader}>
                    <span>Дополнительные настройки</span>
                </div>
                <div className={styles.settingCheckboxItem}>
                    <input type={"checkbox"} {...register('blockingTraffic')}/>
                    <div className={styles.settingCheckboxText}>
                        <span>Блокировка подозрительного трафика</span>
                    </div>
                    {errors.blockingTraffic && (
                        <span style={{ color: "red" }}>
                                            {errors.blockingTraffic.message}
                                        </span>
                    )}
                </div>
                <div className={styles.settingCheckboxItem}>
                    <input type={"checkbox"} {...register('speedLimit')}/>
                    <div className={styles.settingCheckboxText}>
                        <span>Ограничение скорости подключения</span>
                    </div>
                    {errors.speedLimit && (
                        <span style={{ color: "red" }}>
                                            {errors.speedLimit.message}
                                        </span>
                    )}
                </div>
            </div>
        </>
    );
}
