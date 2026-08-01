import styles from "./update.module.css";
import { RefreshCw } from "lucide-react";

interface UpdateProps<T> {
    isLoading: boolean;
    fetchData: (params: Partial<T>) => Promise<void>;
}

export default function UpdateButton<T>({
    isLoading,
    fetchData,
}: UpdateProps<T>) {
    const handleRefresh = () => {
        void fetchData({});
    };

    return (
        <button
            type="button"
            className={styles.updateButton}
            onClick={handleRefresh}
            disabled={isLoading}
        >
            <RefreshCw size={16} className={isLoading ? styles.spinning : ""} />
        </button>
    );
}
