import { z } from "zod";
import { AUTH_TYPE, SERVER_STATUS, SUBSCRIPTION_FORMAT } from "@/constant.ts";

export const schema = z.object({
    name: z.string(),
    location: z.string().length(2, "Код локации должен состоять из 2 букв"),
    countryEmoji: z.string(),
    ip: z.ipv4(),
    status: z.enum(SERVER_STATUS),
    maxUsers: z.number().min(0),
    maxTraffic: z.number().min(0),
    login: z.string().optional(),
    password: z.string().optional(),
    sshAuthType: z.enum(AUTH_TYPE),
    sshPrivateKeyObjectKey: z.string().optional(),
    xuiLogin: z.string(),
    xuiPassword: z.string(),
    xuiAuthToken: z.string().optional(),
    subscriptionFormat: z.enum(SUBSCRIPTION_FORMAT),
    port: z.number().min(0).max(65535),
    webBasePath: z.string(),
});

export type ServerFormData = z.infer<typeof schema>;

export type AuthType = ServerFormData["sshAuthType"];
export type StatusServer = ServerFormData["status"];
export type SubscriptionFormat = ServerFormData["subscriptionFormat"];