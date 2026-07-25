import type {
    AuthType,
    StatusServer,
    SubscriptionFormat,
} from "@pages/vpnServers/components/ChangeVpnServers/ChangeSchema.tsx";

export interface Servers {
    id: number;
    name: string;
    location: string;
    countryEmoji: string;
    ip: string;
    status: StatusServer;
    load: number;
    usage: number;
    maxUsers: number;
    ping: string;
    uptime: number;
}

export interface ServerPut {
    name: string;
    location: string;
    countryEmoji: string;
    ip: string;
    status: StatusServer;
    maxUsers: number;
    maxTraffic: number;
    login: string;
    password: string;
    sshAuthType: AuthType;
    sshPrivateKeyObjectKey: string;
    xuiLogin: string;
    xuiPassword: string;
    xuiAuthToken: string;
    subscriptionFormat: SubscriptionFormat;
    port: number;
    webBasePath: string;
}

export interface ServerGet {
    name: string;
    location: string;
    countryEmoji: string;
    ip: string;
    status: StatusServer;
    maxUsers: number;
    maxTraffic: number;
    port: number;
    sshAuthType: AuthType;
    subscriptionFormat: SubscriptionFormat;
}

export interface ServersAllGet {
    id: number;
    name: string;
    location: string;
}