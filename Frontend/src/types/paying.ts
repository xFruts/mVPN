import  { PAYING_STATUS } from "@/constant.ts";

export interface PayingsGet {
    id: number;
    userId: number;
    paidUntilDate: string;
    payerFullName: string;
    paidAmount: number;
    status: (typeof PAYING_STATUS)[number];
    userComment: string;
    adminComment: string;
    createdAt: string;
}

export interface PayingsPost {
    paidUntilDate: string;
    userId: number;
    payerFullName: string;
    paidAmount: number;
    currency: string;
    userComment: string;
    adminComment: string;
}

export interface PayingsSettingsGet {
    id: number;
    billingMonth: string;
    expectedAmount: number;
    bankName: string;
    requisites: string;
    createdBy: string;
    createdAt: string;
}

export interface PayingsSettingsPost {
    billingMonth: string;
    expectedAmount: number;
    bankName: string;
    requisites: string;
}

export interface PayingStats {
    pendingCount: number;
    pendingAmount: number;
}