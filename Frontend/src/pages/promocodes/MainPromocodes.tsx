import styles from "./MainPromocodes.module.css"
import { usePromocodesStore } from "@store/usePromocodesStore.ts";
import AddPromocode from "@pages/promocodes/component/AddPromocode/AddPromocode.tsx";
import TablePromocodes from "@pages/promocodes/component/TablePromocodes/TablePromocodes.tsx";
import { useEffect } from "react";
import UpdateButton from "@pages/shared/update/update.tsx";
import type { GetPromocodeData } from "@/types/general.ts";

export default function MainPromocodes() {
    const { totalElements, activeCodes, allUsages, fetchPromocodes, isLoading, isInitialized } = usePromocodesStore();

    useEffect(() => {
        fetchPromocodes();
    }, [fetchPromocodes]);

    if (!isInitialized) {
        return <div className={"loadingText"}>Загрузка промокодов...</div>;
    }

    return (
        <div className={styles.promocode}>
            <div className={styles.promocodeHeader}>
                <span className={styles.headerText}>
                    Создание и управление промокодами для пользователей
                </span>
                <UpdateButton<GetPromocodeData>
                    isLoading={isLoading}
                    fetchData={fetchPromocodes}
                />
            </div>
            <div className={styles.promocodeContent}>
                <div className={styles.promocodeInfo}>
                    <div className={styles.info}>
                        <span className={styles.infoText}>ВСЕГО КОДОВ</span>
                        <span className={styles.allCodes}>{totalElements}</span>
                    </div>
                    <div className={styles.info}>
                        <span className={styles.infoText}>АКТИВНЫХ</span>
                        <span className={styles.activeCodes}>
                            {activeCodes}
                        </span>
                    </div>
                    <div className={styles.info}>
                        <span className={styles.infoText}>
                            ВСЕГО ИСПОЛЬЗОВАНИЙ
                        </span>
                        <span className={styles.allUsages}>{allUsages}</span>
                    </div>
                </div>
                <AddPromocode />
                <TablePromocodes />
            </div>
        </div>
    );
}
