import { User, Star, Crown } from "lucide-react";

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

export const SERVER_LOCATION = {
    FI: {
        codes: "FI",
        location: "Финляндия"
    },
    US: {
        codes: "US",
        location: "США"
    },
    RU: {
        codes: "RU",
        location: "Россия"
    },
    NL: {
        codes: "NL",
        location: "Нидерланды"
    },
    DE: {
        codes: "DE",
        location: "Германия"
    },
    SG: {
        codes: "SG",
        location: "Сингапур"
    },
    JP: {
        codes: "JP",
        location: "Япония"
    },
} as const;

export const BANDWIDTH = ["100 Mbps", "500 Mbps", "1 Gbps", "10Gbps"] as const

export const LOGGING = ["Debug", "Info", "Warning", "Error"] as const;

export const BANK = ["Сбербанк", " ВТБ", "Тинькофф", "Альфа-Банк", "Райффайзен", "Газпромбанк"] as const;

export const SAMPLES = [
    {
        name: "Выберите шаблон...",
        description: ""
    },
    {
        name: "Maintenance",
        description: "Внимание! Плановое техническое обслуживание серверов [НАЗВАНИЕ СЕРВЕРА] [ДАТА] с [ВРЕМЯ НАЧАЛА] до [ВРЕМЯ ОКОНЧАНИЯ]. Возможны кратковременные перебои в работе."
    },
    {
        name: "Payment-reminder",
        description: "Напоминаем, что срок действия вашей подписки истекает [ДАТА]. Пожалуйста, продлите ее, чтобы избежать перерывов в доступе."
    },
    {
        name: "New-server",
        description: "Отличные новости! Мы добавили новый сервер в [ЛОКАЦИЯ], чтобы сделать ваше подключение еще быстрее и стабильнее."
    }
]

export const AUTH_TYPE = ["PASSWORD", "KEY"]

export const SUBSCRIPTION_FORMAT = ["VLESS", "JSON"]