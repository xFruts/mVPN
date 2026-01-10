export interface User {
    id: number;
    firstname: string;
    lastname: string;
    telegramId: string;
    role: "ADMIN" | "SPECIAL" | "BASIC";
    type: "PREMIUM" | "TRIAL" | "BASIC" | "NONE";
    status: "ACTIVE" | "EXPIRED" | "CANCELLED" | "NONE";
    countries: string[];
    dateStart: string;
    dateFinish: string;
}
