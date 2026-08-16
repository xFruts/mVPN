import styles from "./AddPaying.module.css"
import { usePayingStore } from "@store/usePayingStore.ts";
import { X } from "lucide-react"
import { z } from "zod";
import Autocomplete from "@pages/shared/AutoComplete/AutoComplete.tsx";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import type { PayingsPost } from "@/types/paying.ts";
import { PayingService } from "@api/services/payingService.ts";

const schema = z.object({
    paidUntilDate: z.string(),
    payerFullName: z.string(),
    paidAmount: z.number(),
    currency: z.string(),
    userComment: z.string(),
    adminComment: z.string(),
});

type FormData = z.infer<typeof schema>;

const defaultValues: FormData = {
    paidUntilDate: new Date(new Date().setMonth(new Date().getMonth() + 1))
        .toISOString()
        .split('T')[0],
    payerFullName: "",
    paidAmount: 75,
    currency: "RUB",
    userComment: "",
    adminComment: "",
};

export default function AddPaying() {
    const { setIsOpen, userId, fetchPaying, fetchStats } = usePayingStore();
    const { register, handleSubmit } = useForm<FormData>({
        resolver: zodResolver(schema),
        defaultValues,
    });

    const onSubmit = async (data: FormData) => {
        try {
            const fullData: PayingsPost = { ...data, userId };
            await PayingService.createPaying(fullData)
            void fetchPaying({})
            void fetchStats();
            setIsOpen(false);
        } catch (error) {
            console.error(error);
        }
    };

    return (
        <div
            className={"overlay"}
            onClick={() => {
                setIsOpen(false);
            }}
        >
            <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
                <div className={styles.modalHeader}>
                    <span>Новая заявка на проверку</span>
                    <div
                        className={styles.close}
                        onClick={() => {
                            setIsOpen(false);
                        }}
                    >
                        <X size={20} />
                    </div>
                </div>
                <form
                    id={"create-paying-form"}
                    onSubmit={handleSubmit(onSubmit)}
                >
                    <div className={styles.modalContent}>
                        <div className={"input"}>
                            <span className={styles.inputHeader}>
                                ПОЛЬЗОВАТЕЛЬ
                            </span>
                            <Autocomplete />
                        </div>
                        <div className={"input"}>
                            <span className={styles.inputHeader}>
                                СУММА (₽)
                            </span>
                            <input
                                type="text"
                                {...register("paidAmount", {
                                    valueAsNumber: true,
                                })}
                            />
                        </div>
                        <div className={"input"}>
                            <span className={styles.inputHeader}>
                                ОПЛАЧЕНО ДО (СРОК НОВОЙ ПОДПИСКИ)
                            </span>
                            <input type="date" {...register("paidUntilDate")} />
                        </div>
                        <div className={"input"}>
                            <span className={styles.inputHeader}>
                                ФИО ПЛАТЕЛЬЩИКА
                            </span>
                            <input type="text" {...register("payerFullName")} />
                        </div>
                        <div className={"input"}>
                            <span className={styles.inputHeader}>
                                КОММЕНТАРИЙ ЮЗЕРА (ЧЕК, ДЕТАЛИ)
                            </span>
                            <textarea
                                className={`${styles.textarea} scroll`}
                                {...register("userComment")}
                            />
                        </div>
                    </div>
                </form>
                <div className={styles.modalFooter}>
                    <button className={`buttonCancel ${styles.button}`}>
                        Отмена
                    </button>
                    <button
                        className={`buttonCreate ${styles.button}`}
                        form={"create-paying-form"}
                    >
                        Создать
                    </button>
                </div>
            </div>
        </div>
    );
}