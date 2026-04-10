import type { UseFormReturn } from 'react-hook-form';
import { z } from 'zod';
import { schema } from "./SettingSchema.tsx";
import { BANDWIDTH, LOGGING } from "@/constant.ts";
import styles from "@pages/vpnServers/components/SettingsVpnServers/SettingVpnServers.module.css";

type FormData = z.infer<typeof schema>;

interface BasicInfoProps {
    form: UseFormReturn<FormData>;
}

export default function SettingMain({ form }: BasicInfoProps) {
    const { register, formState: { errors } } = form;
    return (
        <>
            <div className={styles.settingBody}>
                <div className={styles.settingBodyInput}>
                    <span>Максимум пользователей</span>
                    <input
                        type={"number"}
                        className={styles.editServerInput}
                        {...register('maxUsers')}
                    />
                    {errors.maxUsers && (
                        <span style={{ color: "red" }}>
                                            {errors.maxUsers.message}
                                        </span>
                    )}
                </div>
                <div className={styles.settingBodyInput}>
                    <span>Пропускная способность</span>
                    <div className={styles.editServerInput}>
                        <select
                            {...register('bandwidth')}
                        >
                            {Object.entries(BANDWIDTH).map(([, value]) => (
                                <option value={value}>{value}</option>
                            ))}
                        </select>
                        {errors.bandwidth && (
                            <span style={{ color: "red" }}>
                                            {errors.bandwidth.message}
                                        </span>
                        )}
                    </div>
                </div>
                <div className={styles.settingBodyInput}>
                    <span>Уровень логирования</span>
                    <div className={styles.editServerInput}>
                        <select
                            {...register('logging')}
                        >
                            {Object.entries(LOGGING).map(([, value]) => (
                                <option value={value}>{value}</option>
                            ))}
                        </select>
                        {errors.logging && (
                            <span style={{ color: "red" }}>
                                            {errors.logging.message}
                                        </span>
                        )}
                    </div>
                </div>
                <div className={styles.settingBodyInput}></div>
            </div>
            <div className={styles.settingCheckbox}>
                <div className={styles.settingCheckboxItem}>
                    <input type={"checkbox"} {...register('automaticReboot')}/>
                    <div className={styles.settingCheckboxText}>
                        <span style={{ color: "black" }}>
                            Автоматическая перезагрузка
                        </span>
                        <span>
                            Перезагружать сервер при критических ошибках
                        </span>
                    </div>
                </div>
            </div>
        </>
    );
}
