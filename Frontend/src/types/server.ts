export interface Server {
    id: number;
    name: string;
    location: string;
    ip: string;
    status: string;
    load: number;
    usage: number;
    maxUsers: number;
    maxTraffic: number;
    ping: number;
    uptime: number;
}
