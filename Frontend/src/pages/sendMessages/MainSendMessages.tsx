import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import PaymentDetails from "./component/PaymentDetails.tsx";
import styles from './MainSendMessages.module.css';
import {useState} from "react";
import {BANK, USER_ROLES } from '@/constant.ts'
import { Check, SendHorizontal } from 'lucide-react';
import TextMessages from "./component/TextMessages.tsx";

export const schema = z.object({
    message: z.string().max(1000),
    price: z.string(),
    phone: z.string().regex(/^\+7\d{10}$/, {
        message: "Неверный формат",
    }).max(12),
    bank: z.enum(BANK)
})

type FormData = z.infer<typeof schema>;

export default function MainSendMessages() {
    const [state, setState] = useState("text")
    const [target, setTarget] = useState<string>("all");
    const [plan, setPlan] = useState(false);

    const form = useForm<FormData>({
        resolver: zodResolver(schema),
        defaultValues: {
            message: "",
            price: "",
            phone: "",
            bank: "Сбербанк"
        },
    });

    const onSubmit = (data: FormData) => {
        console.log(data);
    };

    const handleTargetChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setTarget(e.target.value);
    };

    const handlePlanChange = () => {
        setPlan(!plan);
    }

    return (
        <div className={styles.send}>
            <div className={styles.sendHeader}>
                <span className={styles.sendHeaderTitle}>Отправка сообщений</span>
                <span className={styles.sendHeaderSubtitle}>Массовая рассылка и уведомления пользователям</span>
                <div className={styles.sendChoice}>
                    <div
                        className={`${styles.textMessages} ${state === "text" ? styles.active : ''}`}
                        onClick={() => setState("text")}
                    >
                        <span>Текстовые сообщения</span>
                    </div>
                    <div
                        className={`${styles.paymentDetails} ${state === "payment" ? styles.active : ''}`}
                        onClick={() => setState("payment")}
                    >
                        <span>Данные об оплате</span>
                    </div>
                </div>
            </div>
            <div>
                <form onSubmit={form.handleSubmit(onSubmit)} className={styles.sendContent}>
                    <div className={styles.sendMany} style={{flex: "2"}}>
                        <div className={styles.sendSample}></div>
                        <div className={styles.sendMain}>
                            {state === "text" ? <TextMessages form={form}/> : <PaymentDetails form={form}/>}
                        </div>
                        <div className={styles.sendRecipients}>
                            <span>Получатели</span>
                            <div className={styles.Recipients}>
                                <label>
                                    <input
                                        type="radio"
                                        name="audience"
                                        value="all"
                                        checked={target === "all"}
                                        onChange={handleTargetChange}
                                    />
                                    <span>Все пользователи</span>
                                </label>

                                <label>
                                    <input
                                        type="radio"
                                        name="audience"
                                        value="role"
                                        checked={target === "role"}
                                        onChange={handleTargetChange}
                                    />
                                    <span>По роли</span>
                                </label>

                                <label>
                                    <input
                                        type="radio"
                                        name="audience"
                                        value="ids"
                                        checked={target === "ids"}
                                        onChange={handleTargetChange}
                                    />
                                    <span>Конкретные ID</span>
                                </label>
                            </div>
                            {target === "role" ? (
                                <select className={styles.roleRecipients}>
                                    {USER_ROLES.map(role => (
                                        <option key={role}>{role}</option>
                                    ))}
                                </select>
                            ) : target === "ids" ? (
                                <input className={styles.roleRecipients} type="text" placeholder={"Введите все ID через запятую"}/>
                            ) : null}
                        </div>
                        <div className={styles.sendButton}>
                            {plan ? (
                                <input type={"datetime-local"}/>
                            ) : (<div></div>)}
                            <div className={styles.sendButtonItem}>
                                <button onClick={handlePlanChange}>{plan ? "Отменить" : "Запланировать"}</button>
                                <button style={{backgroundColor: "#4880ff", color: "white"}}><SendHorizontal />Отправить сообщение</button>
                            </div>
                        </div>
                    </div>
                    <div className={styles.sendMany} style={{flex: "1"}}>
                        <div className={styles.sendViewing}>
                            <span style={{textAlign: "center"}}>Предпросмотр Telegram</span>
                            <div className={styles.sendViewingMain}>
                                <span>Кому: <strong>Все пользователи</strong> (~1234 получателя)</span>
                                <div className={styles.sendViewingMessage}>
                                    <div className={styles.sendViewingText}>
                                        {state === "payment" ? (
                                            <>
                                                <p>Данные для оплаты:</p>
                                                <p>Сумма: {form.watch("price")}₽</p>
                                                <p>Телефон: {form.watch("phone")}</p>
                                                <p>Банк: {form.watch("bank")}</p>
                                            </>
                                        ) : <p>{ form.watch("message") || "Ваше сообщение здесь..."}</p>}
                                    </div>
                                    <div className={styles.sendViewingDate}>
                                        <span>14:30</span>
                                        <Check size={15}/>
                                        <Check size={15}/>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div className={styles.sendStats}>
                            <span>Статистика</span>
                            <div><span>Отправлено сегодня</span><span>12</span></div>
                            <div><span>За неделю</span><span>89</span></div>
                            <div><span>Доставлено</span><span style={{color: "green"}}>98.5%</span></div>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    );
}
