import type { UseFormReturn } from 'react-hook-form';
import { z } from 'zod';
import styles from '../MainSendMessages.module.css'
import { schema } from './SchemaSendMessages.tsx'
import { SAMPLES } from "@/constant.ts";

type FormData = z.infer<typeof schema>;

interface BasicInfoProps {
    form: UseFormReturn<FormData>;
}

export default function TextMessages({ form }: BasicInfoProps) {
    const { register, formState: { errors } } = form;

    function handleSelectSample(e: React.ChangeEvent<HTMLSelectElement>): void {
        const selectedName = e.target.value;
        const sample = SAMPLES.find(s => s.name === selectedName) || SAMPLES[0];
        form.setValue("message", sample.description)
    }

    return (
        <div className={styles.TextMessages}>
            <span style={{fontSize: "14px"}}>Шаблоны</span>
            <select className={styles.Samples} onChange={handleSelectSample}>
                {SAMPLES.map((sample) => (
                    <option value={sample.name}>{sample.name}</option>
                ))}
            </select>
            <span style={{ fontSize: "14px"}}>Текст сообщения</span>
            <textarea
                rows={10}
                placeholder="Введите текст сообщения..."
                className={styles.textarea}
                {...register("message")}
            />
            {errors.message && (
                <span style={{ color: "red" }}>
                                            {errors.message.message}
                                        </span>
            )}
            <span style={{color: "gray", fontSize: "13px"}}>{form.watch("message").length}/1000 символов</span>
        </div>
    );
}
