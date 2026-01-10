import type { UseFormReturn } from 'react-hook-form';
import { z } from 'zod';
import { schema } from "./SettingVpnServers.tsx";
import styles from "@pages/vpnServers/components/SettingsVpnServers/SettingVpnServers.module.css";

export type FormData = z.infer<typeof schema>;

interface BasicInfoProps {
    form: UseFormReturn<FormData>;
}

export default function SettingMonitoring({ form }: BasicInfoProps) {
    const { register, formState: { errors } } = form;
    return (
        <>
            <div className={styles.settingCheckbox}>
                <div className={styles.settingCheckboxItem}>
                    <input type={"checkbox"} {...register('monitoring')}/>
                    <div className={styles.settingCheckboxText}>
                        <span style={{ color: "black" }}>
                            Включить мониторинг
                        </span>
                        <span>Собирать метрики производительности сервера</span>
                    </div>
                    {errors.monitoring && (
                        <span style={{ color: "red" }}>
                                            {errors.monitoring.message}
                                        </span>
                    )}
                </div>
                <div className={styles.settingCheckboxItem}>
                    <input type={"checkbox"} {...register('backups')}/>
                    <div className={styles.settingCheckboxText}>
                        <span style={{ color: "black" }}>
                            Автоматические резервные копии
                        </span>
                        <span>
                            Создавать резервные копии конфигураций каждые 24
                            часа
                        </span>
                    </div>
                    {errors.backups && (
                        <span style={{ color: "red" }}>
                                            {errors.backups.message}
                                        </span>
                    )}
                </div>
            </div>
            <div className={styles.settingMetrics}>
                <div className={styles.settingMetricsItem}>
                    <span className={styles.settingMetricsItemHeader}>
                        Использование CPU
                    </span>
                    <span className={styles.settingMetricsItemText}>23%</span>
                </div>
                <div className={styles.settingMetricsItem}>
                    <span className={styles.settingMetricsItemHeader}>
                        Использование RAM
                    </span>
                    <span className={styles.settingMetricsItemText}>45%</span>
                </div>
                <div className={styles.settingMetricsItem}>
                    <span className={styles.settingMetricsItemHeader}>
                        Использование диска
                    </span>
                    <span className={styles.settingMetricsItemText}>12%</span>
                </div>
            </div>
        </>
    );
}
