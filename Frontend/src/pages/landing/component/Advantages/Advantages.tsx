import styles from "./Advantages.module.css"
import { useScrollReveal } from "@pages/shared/useScrollReveal.tsx";
import { BusinessCards, UserCards } from "@/constant.ts";



export default function Advantages({ mode }: { mode?: boolean }) {
    const [ref, isVisible] = useScrollReveal();
    const Cards = mode ? UserCards : BusinessCards;
    return (
        <div
            ref={ref}
            id="advantages-section"
            className={`${styles.advantages} ${isVisible ? styles.visible : ""}`}
        >
            <div className={styles.header}>
                <span className={styles.headerText}>
                    Непревзойденная автоматизация, полный контроль над
                    пользователями и детальная аналитика — вот почему мы
                    лидируем на рынке панелей для управления VPN.
                </span>
            </div>
            <div className={styles.content}>
                {Cards.map((card) => {
                    const IconComponent = card.icon;
                    return (
                        <div className={styles.card}>
                            <div className={styles.icon}>
                                <IconComponent size={24} />
                            </div>
                            <span className={styles.cardHeader}>
                                {card.header}
                            </span>
                            <span className={styles.cardDesc}>
                                {card.desc}
                            </span>
                        </div>
                    );
                })}
            </div>
        </div>
    );
}