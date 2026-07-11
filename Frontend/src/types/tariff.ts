export interface TariffsGet {
    id: number;
    name: string;
    maxDevices: number;
    trafficLimitGb: number;
    durationOfDays: number;
    serverLocation: { id: number; location: string }[];
}