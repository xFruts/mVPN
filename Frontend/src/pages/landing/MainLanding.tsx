import { useAdmin } from "@/AdminContext";
import { useEffect } from "react";
import { useNavigate } from "react-router";
import { Background } from "@pages/landing/component/Background/Background";
import Header from "./component/Header/Header.tsx";
import styles from "./MainLanding.module.css";
import Main from "./component/Main/Main.tsx";
import Technology from "@pages/landing/component/Technology/Technology.tsx";
import Advantages from "@pages/landing/component/Advantages/Advantages.tsx";
import Footer from "@pages/landing/component/Footer/Footer.tsx";
import { UserProvider } from "./UserProvider.tsx"
import { useUserMode} from "./UserContext.ts";
import { motion, AnimatePresence } from "framer-motion";
import SubscribeUser from "@pages/landing/component/Subscribe/SubscribeUser.tsx";
import SubscribeBusiness from "@pages/landing/component/Subscribe/SubscribeBusiness.tsx";

function MainLandingContent() {
    const { authenticated } = useAdmin();
    const navigate = useNavigate();
    const { isUserMode } = useUserMode();

    useEffect(() => {
        if (authenticated) {
            navigate("/payment");
        }
    }, [authenticated, navigate]);

    if (authenticated) return <div />;

    return (
        <div className={styles.landing}>
            <Background />
            <AnimatePresence mode="wait">
                {isUserMode ? (
                    <motion.div
                        key="user-content"
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        transition={{ duration: 0.3 }}
                    >
                        <Header mode={true} />
                        <Main mode={true} />
                        <Technology />
                        <Advantages mode={true} />
                        <SubscribeBusiness />
                    </motion.div>
                ) : (
                    <motion.div
                        key="business-content"
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        transition={{ duration: 0.3 }}
                    >
                        <Header mode={false} />
                        <Main mode={false} />
                        <Technology />
                        <Advantages mode={false} />
                        <SubscribeUser />
                    </motion.div>
                )}
            </AnimatePresence>

            <Footer />
        </div>
    );
}

export default function MainLanding() {
    return (
        <UserProvider>
            <MainLandingContent />
        </UserProvider>
    );
}
