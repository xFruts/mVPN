import { z } from "zod";
import {BANK } from '@/constant.ts'

export const schema = z.object({
    message: z.string().max(1000),
    price: z.string(),
    phone: z.string().regex(/^\+7\d{10}$/, {
        message: "Неверный формат",
    }).max(12),
    bank: z.enum(BANK)
})