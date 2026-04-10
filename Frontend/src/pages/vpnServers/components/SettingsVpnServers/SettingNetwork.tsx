import type { UseFormReturn } from 'react-hook-form';
import { z } from 'zod';
import { schema } from "./SettingSchema.tsx";
import styles from "@pages/vpnServers/components/SettingsVpnServers/SettingVpnServers.module.css";

type FormData = z.infer<typeof schema>;

interface BasicInfoProps {
    form: UseFormReturn<FormData>;
}

export default function SettingNetwork({ form }: BasicInfoProps) {
    const { register, formState: { errors } } = form;
    return (
        <>
            <div className={styles.settingBody}>
                <div className={styles.settingBodyInput}>
                    <span>Порт сервера</span>
                    <input
                        type={"text"}
                        className={styles.editServerInput}
                        {...register('port')}
                    />
                    {errors.port && (
                        <span style={{ color: "red" }}>
                            {errors.port.message}
                        </span>
                    )}
                </div>
                <div className={styles.settingBodyInput}>
                    <span>Keep Alive (секунды)</span>
                    <input
                        type={"text"}
                        className={styles.editServerInput}
                        {...register('keepAlive')}
                    />
                    {errors.keepAlive && (
                        <span style={{ color: "red" }}>
                             {errors.keepAlive.message}
                        </span>
                    )}
                </div>
                <div className={styles.settingBodyInput}>
                    <span>Первичный DNS</span>
                    <input
                        type={"text"}
                        className={styles.editServerInput}
                        {...register('dnsSettings.primary')}
                    />
                    {errors.dnsSettings?.primary?.message && (
                        <span style={{ color: "red" }}>
                            {errors.dnsSettings.primary.message}
                        </span>
                    )}
                </div>
                <div className={styles.settingBodyInput}>
                    <span>Вторичный DNS</span>
                    <input
                        type={"text"}
                        className={styles.editServerInput}
                        {...register('dnsSettings.secondary')}
                    />
                    {errors.dnsSettings?.secondary?.message && (
                        <span style={{ color: "red" }}>
                            {errors.dnsSettings.secondary.message}
                        </span>
                    )}
                </div>
                <div className={styles.settingBodyInput}>
                    <span>Разрешенные IP адреса</span>
                    <input
                        type={"text"}
                        className={styles.editServerInput}
                        {...register('allowedIPs')}
                    />
                    {errors.allowedIPs && (
                        <span style={{ color: "red" }}>
                            {errors.allowedIPs.message}
                        </span>
                    )}
                </div>
            </div>
        </>
    );
}
