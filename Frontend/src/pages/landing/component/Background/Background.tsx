import { useEffect, useMemo, useRef } from "react";
import { tsParticles } from "@tsparticles/engine";
import { type ISourceOptions, type Container } from "@tsparticles/engine";
import { loadSlim } from "@tsparticles/slim";
import styles from "./Background.module.css";

export const Background = () => {
    const containerRef = useRef<Container | undefined>(undefined);
    const isEngineReady = useRef(false);

    const options: ISourceOptions = useMemo(
        () => ({
            fullScreen: { enable: false },
            fpsLimit: 120,
            particles: {
                number: {
                    value: 150,
                    density: { enable: true, area: 800 },
                },
                color: {
                    value: "#0ea5e9",
                },
                shape: { type: "circle" },
                opacity: {
                    value: { min: 0.3, max: 1 },
                    animation: {
                        enable: true,
                        speed: 1,
                        minimumValue: 0.1,
                        sync: false,
                    },
                },
                size: {
                    value: { min: 1, max: 2.5 },
                },
                move: {
                    enable: true,
                    speed: 0.3,
                    direction: "bottom",
                    random: false,
                    straight: true,
                    outModes: {
                        default: "out",
                    },
                },
            },
            detectRetina: true,
        }),
        [],
    );

    useEffect(() => {
        const init = async () => {
            if (!isEngineReady.current) {
                await loadSlim(tsParticles);
                isEngineReady.current = true;
            }

            if (containerRef.current) {
                containerRef.current.destroy();
            }

            try {
                containerRef.current = await tsParticles.load({
                    id: "tsparticles",
                    options: options,
                });
            } catch (e) {
                console.error("Particles load error", e);
            }
        };

        void init();

        return () => {
            if (containerRef.current) {
                containerRef.current.destroy();
                containerRef.current = undefined;
            }
        };
    }, [options]);

    return (
        <div className={styles.backgroundContainer}>
            <div className={styles.glow} />
            <div id="tsparticles" className={styles.particlesCanvas} />
        </div>
    );
};
