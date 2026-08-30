import styles from "./Footer.module.css"
import Logo from "@/assets/images/Logo.tsx";
import { Mail, Phone, MessageCircle } from "lucide-react"

export default function Footer() {

    const scrollTo = (id: string) => {
        const element = document.getElementById(id);
        if (element) {
            element.scrollIntoView({ behavior: "smooth" });
        }
    };

    return (
        <div className={styles.footer}>
            <div className={styles.footerPanel}>
                <div className={styles.panelHeader}>
                    <div className={styles.logo}>
                        <Logo />
                        <span>mVPN</span>
                    </div>
                    <span className={styles.headerDesc}>
                        Мощная и универсальная панель, позволяющая с легкостью
                        настраивать и управлять вашими VPN-серверами и
                        клиентами.
                    </span>
                </div>
                <div className={styles.redirect}>
                    <span className={styles.redirectHeader}>Контакты</span>
                    <span className={styles.contact}>
                        <Mail size={16} color={"#06b6d4"} />
                        support@mvpn.ru
                    </span>
                    <span className={styles.contact}>
                        <Phone size={16} color={"#06b6d4"} />
                        +7 (495) 123-45-67
                    </span>
                    <span className={styles.contact}>
                        <MessageCircle size={16} color={"#06b6d4"} />
                        @mvpn_business
                    </span>
                </div>
                <div className={styles.redirect}>
                    <span className={styles.redirectHeader}>Навигация</span>
                    <span onClick={() => scrollTo("technology-section")}>
                        Технологии
                    </span>
                    <span onClick={() => scrollTo("advantages-section")}>
                        Преимущества
                    </span>
                    <span onClick={() => scrollTo("subscribe-section")}>
                        Тарифы
                    </span>
                </div>
            </div>
            <div className={styles.panelCopyright}>
                <span>Copyright © 2026 mVPN. Все права защищены.</span>
                <span>MAXOW TECHNOLOGIES LTD</span>
                <div className={styles.confidentiality}>
                    <span>Политика Конфиденциальности</span>
                    <span>Пользовательское Соглашение</span>
                </div>
            </div>
        </div>
    );
}