import type { UseFormReturn } from 'react-hook-form';
import { z } from 'zod';
import styles from '../MainSendMessages.module.css'
import { schema } from '../MainSendMessages.tsx'
import { BANK } from "@/constant.ts";

type FormData = z.infer<typeof schema>;

interface BasicInfoProps {
    form: UseFormReturn<FormData>;
}

export default function PaymentDetails({ form }: BasicInfoProps) {
    const { register, formState: { errors } } = form;
    return (
        <div className={styles.Payment}>
            <span>Данные об оплате</span>
            <div className={styles.PaymentDetails}>
                <div>
                    <span>Сумма (₽)</span>
                    <input
                        type="text"
                        placeholder="500"
                        {...register("price")}
                    />
                    {errors.price && (
                        <span style={{ color: "red" }}>
                                            {errors.price.message}
                                        </span>
                    )}
                </div>
                <div>
                    <span>Номер телефона</span>
                    <input
                        type="text"
                        placeholder="+79991234567"
                        {...register("phone")}
                    />
                    {errors.phone && (
                        <span style={{ color: "red" }}>
                                            {errors.phone.message}
                                        </span>
                    )}
                </div>
                <div>
                    <span>Название банка</span>
                    <select {...register("bank")}>
                        {BANK.map((bank) => (
                            <option value={bank}>{bank}</option>
                        ))}
                    </select>
                    {errors.bank && (
                        <span style={{ color: "red" }}>
                                            {errors.bank.message}
                                        </span>
                    )}
                </div>
            </div>
        </div>
    );
}
