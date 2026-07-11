import { useParams } from "react-router";
import { useCallback, useEffect, useState } from "react";
import styles from "./UserTariffs.module.css"
import { Plus, Database } from "lucide-react";
import { TariffService } from "@api/services/tariffService.ts";
import { USER_STATUS } from "@/constant.ts";
import { z } from "zod";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { MySelect } from "@/pages/shared/CustomSelect";
import {SubscriptionService} from "@api/services/subscriptionService.ts";
import type {SubscriptionsGet} from "@/types/subscription.ts";

interface TariffName {
    id: number;
    name: string;
}

const schema = z.object({
    startDate: z.string(),
    status: z.enum(USER_STATUS),
    tariff_id: z.number()
});

type FormData = z.infer<typeof schema>;

export default function UserTariffs() {
    const {
        register,
        control,
        handleSubmit,
        formState: { errors },
    } = useForm<FormData>({
        resolver: zodResolver(schema),
        defaultValues: {
            startDate: new Date().toISOString().split('T')[0],
            status: "ACTIVE",
        },
    });
    const { id } = useParams();
    const userId = Number(id);
    const [isAdd, setIsAdd] = useState<boolean>(false);
    const [tariffList, setTariffList] = useState<TariffName[]>([]);
    const [subscriptions, setSubscriptions] = useState<SubscriptionsGet[]>([]);
    const [isLoading, setIsLoading] = useState<boolean>(false);

    const fetchSubscription = useCallback(async () => {
        try {
            setIsLoading(true);
            const data = await SubscriptionService.getAllSubscriptions(userId);
            setSubscriptions(data);

        } catch (error) {
            console.error("Ошибка при загрузке подписок:", error);
        }
        setIsLoading(false);
    }, [userId]);

    useEffect(() => {
        const fetchTariffs = async () => {
            try {
                const data = await TariffService.getTariffs();
                const arrayToMap = Array.isArray(data)
                    ? data
                    : data?.content || [];

                const formattedTariffs: TariffName[] = arrayToMap.map(
                    (tariff) => ({
                        id: tariff.id,
                        name: `${tariff.name} (${tariff.durationOfDays} дней)`,
                    }),
                );

                setTariffList(formattedTariffs);
            } catch (error) {
                console.error("Ошибка при загрузке тарифов:", error);
            }
        };
        fetchTariffs();
        fetchSubscription();
    }, [userId, fetchSubscription])

    const handleAdd = async (data: FormData) => {
        try {
            const requestData = {
                ...data,
                startDate: new Date(data.startDate).toISOString(),
            };
            await SubscriptionService.addSubscription(requestData, userId);
            setIsAdd(!isAdd);
            await fetchSubscription();
        }
        catch (error) {
            console.error(error);
        }
    }
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

    return (
        <>
            <div className={styles.historyHeader}>
                <span className={styles.headerText}>История подписок</span>
                <div
                    className={`${styles.historyAddButton} ${isAdd ? `${styles.none}` : ""}`}
                    onClick={() => setIsAdd(true)}
                >
                    <Plus size={16} />
                    <span>Добавить</span>
                </div>
            </div>
            {isAdd && (
                <div className={styles.historyAdd}>
                    <span className={styles.addTitle}>Новая подписка</span>
                    <form
                        onSubmit={handleSubmit(handleAdd, (errors) =>
                            console.log("Ошибки валидации:", errors),
                        )}
                        className={styles.addForm}
                    >
                        <div className={styles.addInputs}>
                            <div className={styles.input}>
                                <span>ТАРИФ</span>
                                <Controller
                                    name="tariff_id"
                                    control={control}
                                    render={({ field }) => (
                                        <MySelect
                                            options={tariffList.map((t) => ({
                                                value: t.id,
                                                label: t.name,
                                            }))}
                                            value={field.value}
                                            onChange={field.onChange}
                                            placeholder="Выберите тариф"
                                        />
                                    )}
                                />
                                {ErrorMsg("tariff_id")}
                            </div>
                            <div className={styles.input}>
                                <span>СТАТУС</span>
                                <Controller
                                    name="status"
                                    control={control}
                                    render={({ field }) => (
                                        <MySelect
                                            options={USER_STATUS.map(
                                                (status) => ({
                                                    value: status,
                                                    label: status,
                                                }),
                                            )}
                                            value={field.value}
                                            onChange={field.onChange}
                                            placeholder="Выберите статус"
                                        />
                                    )}
                                />
                                {ErrorMsg("status")}
                            </div>
                            <div className={styles.input}>
                                <span>ДАТА НАЧАЛА</span>
                                <input
                                    type={"date"}
                                    {...register("startDate")}
                                />
                                {ErrorMsg("startDate")}
                            </div>
                        </div>
                        <div className={styles.addButton}>
                            <button className={"buttonCreate"}>
                                Сохранить
                            </button>
                            <button
                                type={"button"}
                                className={"buttonCancel"}
                                onClick={() => setIsAdd(false)}
                            >
                                Отмена
                            </button>
                        </div>
                    </form>
                </div>
            )}
            <div className={styles.historyContent}>
                {isLoading && (
                    <div className={"loadingText"} style={{ padding: 0 }}>
                        Загрузка подписок...
                    </div>
                )}
                {subscriptions.map((tariff, index) => (
                    <div className={styles.tariff} key={index}>
                        <div className={styles.tariffHeader}>
                            <div className={styles.dataBase}>
                                <Database size={20} />
                            </div>
                            <div className={styles.tariffNameDate}>
                                <div className={styles.tariffName}>
                                    <span>{tariff.name}</span>
                                </div>
                                <div className={styles.tariffDate}>
                                    <span>
                                        {tariff.startDate
                                            ?.toString()
                                            .slice(0, 10)}{" "}
                                        —{" "}
                                        {tariff.endDate
                                            ?.toString()
                                            .slice(0, 10)}
                                    </span>
                                </div>
                            </div>
                        </div>
                        <div className={styles.tariffStatus}>
                            <div
                                className={`${styles.role} ${
                                    tariff.status === "ACTIVE"
                                        ? styles.active
                                        : tariff.status === "EXPIRED"
                                          ? styles.expired
                                          : styles.regular
                                }`}
                            >
                                {tariff.status}
                            </div>
                        </div>
                    </div>
                ))}
            </div>
        </>
    );
}