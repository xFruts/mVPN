import type {TariffsGet} from "./tariff.ts";

export type PromocodeStatus = "ACTIVE" | "EXPIRED" | "USED"

export interface PromocodesGet {
    id: number;
    code: string;
    usage: number;
    usageLimit: number;
    tariff: TariffsGet;
    status: PromocodeStatus;
    expirationDate: string;
}

export interface PromocodePut {
    usageLimit: number;
    validDays: number;
    tariff_id: number;
}