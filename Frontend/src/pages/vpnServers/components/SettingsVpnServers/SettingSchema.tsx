import { z } from "zod";
import { BANDWIDTH, LOGGING } from "@/constant.ts";

const ipv4Regex = /^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;

export const schema = z.object({
    maxUsers: z.string().min(1).max(5, { message: "Количество должно быть меньше 100000"}),
    bandwidth: z.enum(BANDWIDTH),
    logging: z.enum(LOGGING),
    automaticReboot: z.boolean(),
    port: z.string().regex(/^\d+$/).refine(val => {
        const num = parseInt(val, 10);
        return num >= 1 && num <= 65535;
    }, { message: "Порт должен быть от 1 до 65535" }),
    keepAlive: z.string().refine(val => {
        const num = parseInt(val, 10);
        return num >= 1 && num <= 65535;
    }, { message: "Количество секунд должно быть > 0" }),
    dnsSettings: z.object({
        primary: z.string().regex(ipv4Regex, { message: "Неправильный IPv4 адрес" }).optional(),
        secondary: z.string().regex(ipv4Regex, { message: "Неправильный IPv4 адрес" }).optional(),
    }),
    allowedIPs: z.string().refine(val => {
        const parts = val.split('/');
        if (parts.length !== 2) return false;

        const [ip, maskStr] = parts;

        const mask = parseInt(maskStr, 10);
        if (isNaN(mask) || mask < 0 || mask > 32) return false;
        if (maskStr !== mask.toString()) return false;

        return ipv4Regex.test(ip);
    }, { message: "Неверный формат CIDR (например, 192.168.1.0/24)" }),
    chaCha: z.boolean(),
    AES: z.boolean(),
    blockingTraffic: z.boolean(),
    speedLimit: z.boolean(),
    monitoring: z.boolean(),
    backups: z.boolean(),
});