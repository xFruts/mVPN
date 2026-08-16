import { z } from "zod";
import styles from "./AddPromocode.module.css"
import { Tag } from "lucide-react"
import { useEffect, useState } from "react";
import { TariffService } from "@api/services/tariffService.ts";
import { MySelect } from "@pages/shared/CustomSelect.tsx";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { PromocodeService } from "@api/services/promocodeService.ts";
import { usePromocodesStore } from "@store/usePromocodesStore.ts";

interface TariffName {
    id: number;
    name: string;
}

const schema = z.object({
    usageLimit: z.number().gt(0, "Значение должно быть больше 0"),
    validDays: z.number().gt(0, "Значение должно быть больше 0"),
    tariff_id: z.number("Выберите тариф")
});

export type FormData = z.infer<typeof schema>;

export default function AddPromocode() {
    const { fetchPromocodes, setIsInitialized } = usePromocodesStore();
    const [tariffList, setTariffList] = useState<TariffName[]>([]);
    const {
        register,
        control,
        handleSubmit,
        formState: { errors },
    } = useForm<FormData>({
        resolver: zodResolver(schema),
        defaultValues: {
            usageLimit: 100,
            validDays: 30,
        },
    });
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
                        name: tariff.name,
                    }),
                );

                setTariffList(formattedTariffs);
            } catch (error) {
                console.error("Ошибка при загрузке тарифов:", error);
            }
        };
        void fetchTariffs();
    }, []);

    const onSubmit = async (data: FormData) => {
        try {
            setIsInitialized(false);
            await PromocodeService.createPromocodes(data)
        }
        catch (error) {
            console.error(error);
        }
        void fetchPromocodes({});
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
        <form className={styles.addPromocode} onSubmit={handleSubmit(onSubmit)}>
            <div className={styles.addHeader}>
                <span>Создание промокода</span>
            </div>
            <div className={styles.addContent}>
                <div className="input">
                    <span className={styles.inputHeader}>
                        ЛИМИТ ИСПОЛЬЗОВАНИЙ
                    </span>
                    <input
                        type={"text"}
                        {...register("usageLimit", {
                            valueAsNumber: true,
                        })}
                        style={{ height: "22px" }}
                        className={styles.focus}
                    />
                    {ErrorMsg("usageLimit")}
                </div>
                <div className="input">
                    <span className={styles.inputHeader}>
                        СРОК ДЕЙСТВИЯ (ДНЕЙ)
                    </span>
                    <input
                        type={"text"}
                        {...register("validDays", {
                            valueAsNumber: true,
                        })}
                        style={{ height: "22px" }}
                        className={styles.focus}
                    />
                    {ErrorMsg("validDays")}
                </div>
                <div className="input">
                    <span className={styles.inputHeader}>ТАРИФ</span>
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
                                backgroundColor={"var(--input-bg)"}
                                borderColor={"var(--input-border)"}
                            />
                        )}
                    />
                    {ErrorMsg("tariff_id")}
                </div>
            </div>
            <div className={styles.addFooter}>
                <button className={styles.addButton}>
                    <Tag size={16} />
                    Создать промокод
                </button>
            </div>
        </form>
    );
}