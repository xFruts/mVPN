import styles from "./MainTariffs.module.css"
import {
    Calendar,
    Database,
    Plus,
    Users,
    SquarePen,
    Trash,
} from "lucide-react";
import { TariffService } from "@api/services/tariffService.ts";
import type { TariffsGet } from "@/types/tariff.ts";
import { useEffect, useState } from "react";
import ChangeTariffs from "@pages/tariffs/component/ChangeTariffs/ChangeTariffs.tsx";

export default function MainTariffs() {
    const [tariffs, setTariffs] = useState<TariffsGet[]>([])
    const [isLoading, setIsLoading] = useState<boolean>(false)
    const [id, setId] = useState<number>(-1)
    const [isOpen, setIsOpen] = useState<boolean>(false)

    const fetchTariffs = async () => {
        try {
            setIsLoading(true);
            const data = await TariffService.getTariffs().finally(() =>
                setIsLoading(false),
            );
            setTariffs(data as unknown as TariffsGet[]);
        } catch (e) {
            console.error(e);
        }
    }

    useEffect(() =>  {
        void fetchTariffs();
    },[])

    const handleDeleteTariff = async (id: number) => {
        try {
            await TariffService.deleteTariff(id);
        }
        catch (e) {
            console.error(e);
        }
        void fetchTariffs()
    }

    if (isLoading) {
        return <div className={"loadingText"}>Загрузка тарифов...</div>;
    }

    return (
        <div className={"component"}>
            <div className={"componentHeader"}>
                <span className={"componentText"}>
                    Управление тарифными планами
                </span>
                <div className={"addButton"}
                     onClick={() => {
                         setId(-1)
                         setIsOpen(true)
                     }}
                >
                    <Plus size={20} />
                    <span>
                        Создать <span className={"PC"}>тариф</span>
                    </span>
                </div>
            </div>
            <div className={styles.tariffsContent}>
                {tariffs.map((tariff) => {
                    return (
                        <div key={tariff.id} className={styles.tariff}>
                            <div className={styles.tariffHeader}>
                                <span className={styles.headerName}>
                                    {tariff.name}
                                </span>
                                <span className={styles.headerId}>
                                    ID: {tariff.id}
                                </span>
                            </div>
                            <div className={styles.tariffInfo}>
                                <div className={styles.info}>
                                    <Database
                                        size={20}
                                        className={styles.infoIcon}
                                    />
                                    <span className={styles.infoText}>
                                        Трафик:
                                    </span>
                                    <span className={styles.infoNumber}>
                                        {tariff.trafficLimitGb} GB
                                    </span>
                                </div>
                                <div className={styles.info}>
                                    <Users
                                        size={20}
                                        className={styles.infoIcon}
                                    />
                                    <span className={styles.infoText}>
                                        Устройств:
                                    </span>
                                    <span className={styles.infoNumber}>
                                        {tariff.maxDevices}
                                    </span>
                                </div>
                                <div className={styles.info}>
                                    <Calendar
                                        size={20}
                                        className={styles.infoIcon}
                                    />
                                    <span className={styles.infoText}>
                                        Длительность:
                                    </span>
                                    <span className={styles.infoNumber}>
                                        {tariff.durationOfDays} дней
                                    </span>
                                </div>
                            </div>
                            <div className={styles.tariffLocation}>
                                <span>ЛОКАЦИИ СЕРВЕРОВ</span>
                                <div className={styles.locations}>
                                    {tariff.serverLocation.map((location) => {
                                        return (
                                            <div
                                                key={location.id}
                                                className={styles.location}
                                            >
                                                {location.location}
                                            </div>
                                        );
                                    })}
                                </div>
                            </div>
                            <div className={styles.tariffActions}>
                                <div
                                    className={`${styles.action} ${styles.edit}`}
                                    onClick={()=> {
                                        setIsOpen(true)
                                        setId(tariff.id)
                                    }}
                                >
                                    <SquarePen size={16} />
                                </div>
                                <div
                                    className={`${styles.action} ${styles.delete}`}
                                    onClick={() => {void handleDeleteTariff(tariff.id);}}
                                >
                                    <Trash size={16} />
                                </div>
                            </div>
                        </div>
                    );
                })}
            </div>
            {isOpen && (
                <ChangeTariffs id={id} onClose={() => {
                    setIsOpen(false)
                    void fetchTariffs()}} />
            )}
        </div>
    );
}
