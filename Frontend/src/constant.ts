import {
    User,
    Star,
    Crown,
    UserSearch,
    Link,
    WalletCards,
    Server,
    Settings2,
    ChartColumnIncreasing,
    Shield,
    Users,
    LayoutGrid,
    Wallet,
    SlidersHorizontal,
} from "lucide-react";

export const KEYCLOAK_CONFIG = {
    url: import.meta.env.VITE_KEYCLOAK_URL,
    realm: import.meta.env.VITE_KEYCLOAK_REALM,
    clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID,
};

export const API_BASE_URL = import.meta.env.VITE_API_URL;

export const FRONTEND_URL = import.meta.env.VITE_FRONTEND_URL;

export const USER_ROLES = ["REGULAR", "SPECIAL", "ADMIN"] as const;

export const ROLE_CONFIG = {
    REGULAR: {
        description: "Базовые возможности",
        icon: User,
    },
    SPECIAL: {
        description: "Семейный доступ",
        icon: Star,
    },
    ADMIN: {
        description: "Полный доступ",
        icon: Crown,
    },
} as const;

export const USER_STATUS = ["ACTIVE", "EXPIRED", "CANCELLED"] as const;

export const SERVER_STATUS = ["ACTIVE", "INACTIVE", "MAINTENANCE"] as const;

export const PROMOCODE_STATUS = ["ACTIVE", "EXPIRED", "USED"] as const;

export const PAYING_STATUS = ["PENDING", "APPROVED", "REJECTED"] as const;

export const SERVER_LOCATION = {
    FI: {
        codes: "FI",
        location: "Финляндия",
    },
    US: {
        codes: "US",
        location: "США",
    },
    RU: {
        codes: "RU",
        location: "Россия",
    },
    NL: {
        codes: "NL",
        location: "Нидерланды",
    },
    DE: {
        codes: "DE",
        location: "Германия",
    },
    SG: {
        codes: "SG",
        location: "Сингапур",
    },
    JP: {
        codes: "JP",
        location: "Япония",
    },
} as const;

export const BANDWIDTH = ["100 Mbps", "500 Mbps", "1 Gbps", "10Gbps"] as const;

export const LOGGING = ["Debug", "Info", "Warning", "Error"] as const;

export const BANK = [
    "Сбербанк",
    " ВТБ",
    "Тинькофф",
    "Альфа-Банк",
    "Райффайзен",
    "Газпромбанк",
] as const;

export const SAMPLES = [
    {
        name: "Выберите шаблон...",
        description: "",
    },
    {
        name: "Maintenance",
        description:
            "Внимание! Плановое техническое обслуживание серверов [НАЗВАНИЕ СЕРВЕРА] [ДАТА] с [ВРЕМЯ НАЧАЛА] до [ВРЕМЯ ОКОНЧАНИЯ]. Возможны кратковременные перебои в работе.",
    },
    {
        name: "Payment-reminder",
        description:
            "Напоминаем, что срок действия вашей подписки истекает [ДАТА]. Пожалуйста, продлите ее, чтобы избежать перерывов в доступе.",
    },
    {
        name: "New-server",
        description:
            "Отличные новости! Мы добавили новый сервер в [ЛОКАЦИЯ], чтобы сделать ваше подключение еще быстрее и стабильнее.",
    },
];

export const AUTH_TYPE = ["PASSWORD", "KEY"];

export const SUBSCRIPTION_FORMAT = ["VLESS", "JSON"];

export const UserCards = [
    {
        icon: UserSearch,
        header: "Обработка пользователей",
        desc: "Полный контроль над клиентской базой. Мониторинг сессий, управление сроками действия и профилями.",
    },
    {
        icon: Link,
        header: "Единая подписка",
        desc: "Объединение множества серверов и локаций в одну подписку. Удобно для вас и ваших клиентов.",
    },
    {
        icon: Crown,
        header: "Система тарифов",
        desc: "Возможность создания нескольких тарифов с различными лимитами по времени, трафику и доступным локациям.",
    },
    {
        icon: WalletCards,
        header: "График оплат",
        desc: "Удобный контроль финансов для администратора. Учет заявок на оплату, модерация и наглядная статистика.",
    },
    {
        icon: Server,
        header: "Мульти-серверность",
        desc: "Легкое масштабирование инфраструктуры. Добавляйте и управляйте неограниченным числом нод из единого центра.",
    },
    {
        icon: Settings2,
        header: "Синхронизация X-UI",
        desc: "Глубокая интеграция с X-UI панелями. Автоматическое создание, блокировка и обновление клиентов на серверах.",
    },
    {
        icon: ChartColumnIncreasing,
        header: "Детальная аналитика",
        desc: "Наглядные графики потребления трафика, роста активной аудитории и мониторинг нагрузки на каждый узел.",
    },
    {
        icon: Shield,
        header: "Безопасность",
        desc: "Защищенный доступ к панели управления, надежное хранение конфигураций и защита от несанкционированного доступа.",
    },
];

export const BusinessCards = [
    {
        icon: Shield,
        header: "Обход всех блокировок",
        desc: "Наша технология маскировки трафика позволяет обходить даже самые сложные файрволы (DPI) и получать доступ к любым сайтам.",
    },
    {
        icon: Server,
        header: "Высокая скорость (до 1 Гбит/с)",
        desc: "Смотрите видео в 4K, скачивайте файлы и играйте в онлайн-игры без задержек. Наши серверы не режут скорость.",
    },
    {
        icon: Link,
        header: "Умная маршрутизация",
        desc: "Настройте белые списки: VPN будет включаться автоматически только для заблокированных сервисов и сайтов.",
    },
    {
        icon: Users,
        header: "Безлимитный доступ",
        desc: "Никаких скрытых лимитов на трафик. Пользуйтесь интернетом столько, сколько нужно, на всех ваших устройствах.",
    },
    {
        icon: LayoutGrid,
        header: "Политика No-Logs",
        desc: "Мы не храним историю ваших посещений, логи подключений или DNS-запросы. Ваша активность остается приватной.",
    },
    {
        icon: Crown,
        header: "Простая настройка",
        desc: "Подключение в пару кликов на iOS, Android, Windows и macOS. Интуитивно понятный клиент для всех.",
    },
    {
        icon: Wallet,
        header: "Выгодные тарифы",
        desc: "Премиальное качество соединения по доступной цене от 125 ₽ в месяц. Оплачивайте удобно любой картой.",
    },
    {
        icon: SlidersHorizontal,
        header: "Круглосуточная поддержка",
        desc: "Наши специалисты готовы помочь вам с настройкой и ответить на любые вопросы в режиме 24/7.",
    },
];