import styles from "./PayingSettings.module.css"
import { CreditCard, Info } from "lucide-react";
import { z } from "zod";
import {useForm} from "react-hook-form";
import {zodResolver} from "@hookform/resolvers/zod";
import { useEffect, useState } from "react";
import {PayingService} from "@api/services/payingService.ts";

const schema = z.object({
    id: z.number(),
    billingMonth: z.string(),
    expectedAmount: z.number(),
    bankName: z.string(),
    requisites: z.string(),
    createdBy: z.string(),
    createdAt: z.string(),
});

type FormData = z.infer<typeof schema>;

const defaultValues: FormData = {
    id: 0,
    billingMonth: new Date().toISOString().slice(0, 7),
    expectedAmount: 75,
    bankName: "Tinkoff",
    requisites: "+79991234567",
    createdBy: "admin",
    createdAt: "",
};

export default function PayingSettings() {
    const [isEdit, setIsEdit] = useState<boolean>(true);
    const {
        register,
        watch,
        reset,
        handleSubmit,
    } = useForm<FormData>({
        resolver: zodResolver(schema),
        defaultValues,
    });
    const billingMonthValue = watch("billingMonth");

    useEffect(() => {
        if (!billingMonthValue) return;

        PayingService.getSettings(billingMonthValue)
            .then((data) => {
                reset(data);
            })
            .catch((err) => {
                console.log(err);
                if (err.statusCode === 404) {
                    setIsEdit(false);
                } else {
                    console.error("Другая ошибка:", err);
                }
            });

    }, [billingMonthValue, reset]);

    const onSubmit = async (data: FormData) => {
        try {
            // eslint-disable-next-line @typescript-eslint/no-unused-vars
            const { id, createdBy, createdAt, ...postData } = data;
            if (isEdit) {
                await PayingService.updateSettings(postData, id);
            } else {
                await PayingService.createSettings(postData);
            }
        } catch (error) {
            console.error("Ошибка при сохранении:", error);
        }
    };

    return (
        <form
            onSubmit={handleSubmit(onSubmit, (errors) =>
                console.log("Ошибки валидации:", errors),
            )}
        >
            <div className={styles.settings}>
                <div className={styles.settingsCard}>
                    <div className={styles.settingsHeader}>
                        <CreditCard size={20} color={"#06b6d4"} />
                        <span>Настройки платежа (Месяц)</span>
                    </div>
                    <div className={styles.settingsContent}>
                        <div className={"input"}>
                            <span className={styles.inputHeader}>
                                РАСЧЁТНЫЙ МЕСЯЦ
                            </span>
                            <input
                                type={"month"}
                                className={styles.month}
                                {...register("billingMonth")}
                            />
                        </div>
                        <div className={"input"}>
                            <span className={styles.inputHeader}>
                                ОЖИДАЕМАЯ СУММА (₽)
                            </span>
                            <input
                                type={"text"}
                                {...register("expectedAmount", {
                                    valueAsNumber: true,
                                })}
                            />
                        </div>
                    </div>
                </div>
                <div className={styles.settingsCard}>
                    <div className={styles.settingsHeader}>
                        <Info size={20} color={"#06b6d4"} />
                        <span>Реквизиты для перевода</span>
                    </div>
                    <div className={styles.settingsContent}>
                        <span className={styles.settingsInfo}>
                            Эти данные показываются пользователю при попытке
                            оплатить подписку вручную.
                        </span>
                        <div className={"input"}>
                            <span className={styles.inputHeader}>
                                НАЗВАНИЕ БАНКА
                            </span>
                            <input type={"text"} {...register("bankName")} />
                        </div>
                        <div className={"input"}>
                            <span className={styles.inputHeader}>
                                РЕКВИЗИТЫ (ТЕЛЕФОН, КАРТА)
                            </span>
                            <input type={"text"} {...register("requisites")} />
                        </div>
                    </div>
                    <div className={styles.settingsFooter}>
                        <button className={`buttonCreate ${styles.button}`}>
                            Сохранить настройки
                        </button>
                    </div>
                </div>
            </div>
        </form>
    );
}
