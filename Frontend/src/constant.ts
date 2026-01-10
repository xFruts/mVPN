import { User, Star, Crown, X, Ticket, Package, Gem } from "lucide-react";

export const USER_ROLES = ["BASIC", "SPECIAL", "ADMIN"] as const;

export const ROLE_CONFIG = {
    BASIC: {
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

export const USER_TYPES = ["NONE", "TRIAL", "BASIC", "VIP"] as const;

export const TYPE_CONFIG = {
    NONE: {
        description: "Без подписки",
        priceClass: "none",
        icon: X,
        features: ["-", "-"],
    },
    TRIAL: {
        description: "Бесплатно",
        priceClass: "trial",
        icon: Ticket,
        features: ["7 дней", "50 ГБ"],
    },
    BASIC: {
        description: "299 ₽",
        priceClass: "basic",
        icon: Package,
        features: ["30 дней", "300 ГБ"],
    },
    VIP: {
        description: "2999 ₽",
        priceClass: "vip",
        icon: Gem,
        features: ["365 дней", "Безлимит"],
    },
} as const;

export const SERVER_STATUS = ['ONLINE', 'MAINTENANCE', 'OFFLINE'] as const;

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