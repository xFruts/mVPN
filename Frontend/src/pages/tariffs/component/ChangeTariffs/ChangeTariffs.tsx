import { z } from "zod";
import styles from "./ChangeTariffs.module.css"
import { X } from "lucide-react"
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect, useState } from "react";
import {TariffService} from "@api/services/tariffService.ts";
import {ServerService} from "@api/services/serverService.ts";
import type { ServersAllGet } from "@/types/server.ts";


const schema = z.object({
    name: z.string(),
    maxDevices: z.number().gt(0, "Значение должно быть больше 1"),
    trafficLimitGb: z.number().gt(0, "Значение должно быть больше 1"),
    durationOfDays: z.number().gt(0, "Значение должно быть больше 1"),
    serverIds: z.array(z.number()).nonempty("Выберите хотя бы один сервер"),
});

export type FormData = z.infer<typeof schema>;

const defaultValues: Partial<FormData> = {
    name: "",
    maxDevices: 1,
    trafficLimitGb: 1,
    durationOfDays: 30,
    serverIds: [],
};

export default function ChangeTariffs({ id, onClose }: { id: number; onClose: () => void }) {
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [locations, setLocations] = useState<ServersAllGet[]>([]);
    const {
        register,
        reset,
        watch,
        getValues,
        setValue,
        handleSubmit,
        formState: { errors },
    } = useForm<FormData>({
        resolver: zodResolver(schema),
        mode: "onSubmit",
    });

    useEffect(() => {
        if (id !== -1) {
            setIsLoading(true);
            TariffService.getTariffById(id)
                .then((data) => {
                    reset(data);
                })
                .finally(() => {
                    setIsLoading(false);
                });
        } else {
            reset(defaultValues);
            setIsLoading(false);
        }
        const fetchServers = async () => {
            try {
                setIsLoading(true);
                const data = await ServerService.getAllServers();
                setLocations(data);
            } catch (error) {
                console.error("Ошибка при загрузке серверов:", error);
            }
            setIsLoading(false);
        };
        fetchServers();
    }, [id, reset, watch]);

    const handleChechbox = (id: number) => {
        const currentIds = getValues("serverIds") || [];

        if (currentIds.includes(id)) {
            const newIds = currentIds.filter((currentId) => currentId !== id);
            setValue("serverIds", newIds, {
                shouldValidate: true,
                shouldDirty: true,
            });
        } else {
            const newIds = [...currentIds, id];
            setValue("serverIds", newIds, {
                shouldValidate: true,
                shouldDirty: true,
            });
        }
    }

    const onSubmit = async (data: FormData) => {
        if (id === -1) {
            await TariffService.createTariff(data);
        } else {
            await TariffService.updateTariff(id, data);
        }
        onClose();
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

    const Flag = ({ code }: { code: string }) => (
        <img
            src={`https://purecatamphetamine.github.io/country-flag-icons/3x2/${code.toUpperCase()}.svg`}
            width="24"
            alt={code}
            style={{
                display: "inline-block",
                verticalAlign: "middle",
                borderRadius: "5px",
            }}
        />
    );

    return (
        <div className="overlay" onClick={onClose}>
            <div
                className={styles.tariffsModal}
                onClick={(e) => e.stopPropagation()}
            >
                <div className={styles.header}>
                    <span className={styles.headerText}>
                        {id === -1
                            ? "Создать новый тариф"
                            : "Редактировать тариф"}
                    </span>
                    <div className={styles.X} onClick={onClose}>
                        <X size={24} />
                    </div>
                </div>
                {isLoading ? (
                    <div
                        className={"loadingText"}
                        style={{ marginBottom: "30px", paddingTop: "30px" }}
                    >
                        Загрузка тарифа...
                    </div>
                ) : (
                    <>
                        <form
                            id={"change-tariff-form"}
                            onSubmit={handleSubmit(onSubmit)}
                            className={`${styles.content} scroll`}
                        >
                            <div className={styles.contentInput}>
                                <div className={styles.input}>
                                    <span className={styles.inputHeader}>
                                        Название тарифа
                                    </span>
                                    <input
                                        type={"text"}
                                        {...register("name")}
                                    />
                                    {ErrorMsg("name")}
                                </div>
                                <div className={styles.two}>
                                    <div className={styles.input}>
                                        <span className={styles.inputHeader}>
                                            Лимит устройств
                                        </span>
                                        <input
                                            type={"text"}
                                            {...register("maxDevices", {
                                                valueAsNumber: true,
                                            })}
                                        />
                                        {ErrorMsg("maxDevices")}
                                    </div>
                                    <div className={styles.input}>
                                        <span className={styles.inputHeader}>
                                            Срок действия (дней)
                                        </span>
                                        <input
                                            type={"text"}
                                            {...register("durationOfDays", {
                                                valueAsNumber: true,
                                            })}
                                        />
                                        {ErrorMsg("durationOfDays")}
                                    </div>
                                </div>
                                <div className={styles.input}>
                                    <span className={styles.inputHeader}>
                                        Лимит трафика (GB)
                                    </span>
                                    <input
                                        type={"text"}
                                        {...register("trafficLimitGb", {
                                            valueAsNumber: true,
                                        })}
                                    />
                                    {ErrorMsg("trafficLimitGb")}
                                </div>
                            </div>
                            <div className={styles.contentCheck}>
                                <span className={styles.checkHeader}>
                                    Доступные локации серверов
                                </span>
                                <div className={`${styles.servers} scroll`}>
                                    {locations.map((location) => (
                                        <div
                                            className={styles.check}
                                            key={location.id}
                                            onClick={() => {
                                                handleChechbox(location.id);
                                            }}
                                        >
                                            <input
                                                type={"checkbox"}
                                                className={styles.checkbox}
                                                checked={
                                                    watch(
                                                        "serverIds",
                                                    )?.includes(location.id) ||
                                                    false
                                                }
                                                readOnly
                                            />
                                            <span
                                                className={styles.checkboxText}
                                            >
                                                <Flag code={location.location}/>
                                                {location.name}
                                            </span>
                                        </div>
                                    ))}
                                </div>
                                {ErrorMsg("serverIds")}
                            </div>
                        </form>
                        <div className={styles.footer}>
                            <button className={styles.cancel} onClick={onClose}>
                                Отмена
                            </button>
                            <button
                                className={styles.add}
                                form={"change-tariff-form"}
                            >
                                Сохранить
                            </button>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
}