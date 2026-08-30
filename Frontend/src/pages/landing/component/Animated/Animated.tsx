import { useEffect, useRef } from "react";
import "./Animated.css";

const PETALS = 48;

const INNER_RADIUS = 32;
const PETAL_LENGTH = 70;
const LENGTH_WAVE = 23;

const ROTATION_SPEED = 0.00001;

const MORPH_SPEED = 0.00025;

interface Particle {
    x: number;
    y: number;
    radius: number;
    alpha: number;
    phase: number;
    speed: number;
}

export default function AnimatedFlower() {
    const canvasRef = useRef<HTMLCanvasElement | null>(null);

    useEffect(() => {
        const el = canvasRef.current;
        if (!el) return;
        const context = el.getContext("2d");
        if (!context) return;

        const canvas: HTMLCanvasElement = el;
        const ctx: CanvasRenderingContext2D = context;

        let width = 0;
        let height = 0;
        let dpr = 1;
        let animationFrameId: number;

        const VISUAL_SCALE = 0.7;

        const particles: Particle[] = Array.from({ length: 34 }, () => ({
            x: Math.random(),
            y: Math.random(),
            radius: Math.random() * 1.1 + 0.25,
            alpha: Math.random() * 0.35 + 0.08,
            phase: Math.random() * Math.PI * 2,
            speed: Math.random() * 0.0008 + 0.00025,
        }));

        function resize() {
            const parent = canvas.parentElement;
            if (!parent) return;

            const rect = parent.getBoundingClientRect();
            width = rect.width;
            height = rect.height;
            dpr = Math.min(window.devicePixelRatio || 1, 2);

            canvas.width = width * dpr;
            canvas.height = height * dpr;

            ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        }

        function drawPetal(
            angle: number,
            length: number,
            pWidth: number,
            bend: number,
            alpha: number,
        ) {
            const inner = INNER_RADIUS;
            const outer = inner + length;
            const x0 = Math.cos(angle) * inner;
            const y0 = Math.sin(angle) * inner;
            const x1 = Math.cos(angle) * outer;
            const y1 = Math.sin(angle) * outer;
            const nx = -Math.sin(angle);
            const ny = Math.cos(angle);
            const mx = Math.cos(angle) * (inner + length * 0.4);
            const my = Math.sin(angle) * (inner + length * 0.4);

            ctx.beginPath();
            ctx.moveTo(x0 + nx * pWidth * 0.1, y0 + ny * pWidth * 0.1);
            ctx.quadraticCurveTo(
                mx + nx * pWidth + nx * bend,
                my + ny * pWidth + ny * bend,
                x1,
                y1,
            );
            ctx.quadraticCurveTo(
                x1 + nx * pWidth * 0.05,
                y1 + ny * pWidth * 0.05,
                x1,
                y1,
            );
            ctx.quadraticCurveTo(
                mx - nx * pWidth * 0.82 + nx * bend,
                my - ny * pWidth * 0.82 + ny * bend,
                x0 - nx * pWidth * 0.05,
                y0 - ny * pWidth * 0.05,
            );
            ctx.closePath();

            const g = ctx.createLinearGradient(x0, y0, x1, y1);
            g.addColorStop(0, `rgba(17, 111, 125, ${alpha * 0.65})`);
            g.addColorStop(0.28, `rgba(20, 181, 197, ${alpha})`);
            g.addColorStop(0.72, `rgba(38, 193, 208, ${alpha * 0.92})`);
            g.addColorStop(1, `rgba(38, 164, 180, ${alpha * 0.5})`);
            ctx.fillStyle = g;
            ctx.fill();
        }

        function drawFlower(time: number) {
            ctx.save();
            ctx.translate(width / 2, height / 2);

            const minSide = Math.min(width, height);
            const baseSize = 250;
            const currentScale = (minSide / baseSize) * VISUAL_SCALE;
            ctx.scale(currentScale, currentScale);

            ctx.shadowColor = "rgba(17, 190, 208, 0.22)";
            ctx.shadowBlur = 14;

            const rotation = time * ROTATION_SPEED;
            for (let i = 0; i < PETALS; i++) {
                const bAngle = (i / PETALS) * Math.PI * 2;
                const angle = bAngle + rotation;
                const wave = Math.sin(bAngle * 3 - time * MORPH_SPEED * 4);
                const wave2 = Math.sin(bAngle * 5 + time * MORPH_SPEED * 2.3);
                const length = PETAL_LENGTH + wave * LENGTH_WAVE + wave2 * 8;
                const pWidth = 8.8 + Math.sin(bAngle * 2 + time * 0.001) * 1.1;
                const bend = 8 + wave2 * 5;
                const alpha = 0.78 + Math.sin(bAngle * 2.7) * 0.08;
                drawPetal(angle, length, pWidth, bend, alpha);
            }

            ctx.shadowBlur = 0;
            const glow = ctx.createRadialGradient(
                0,
                0,
                INNER_RADIUS * 0.6,
                0,
                0,
                145,
            );
            glow.addColorStop(0, "rgba(8, 133, 151, 0.15)");
            glow.addColorStop(1, "rgba(0, 0, 0, 0)");
            ctx.fillStyle = glow;
            ctx.beginPath();
            ctx.arc(0, 0, 145, 0, Math.PI * 2);
            ctx.fill();

            const center = ctx.createRadialGradient(
                0,
                0,
                2,
                0,
                0,
                INNER_RADIUS + 7,
            );
            center.addColorStop(0, "rgba(1, 13, 17, 0.98)");
            center.addColorStop(1, "rgba(2, 38, 44, 0.85)");
            ctx.fillStyle = center;
            ctx.beginPath();
            ctx.arc(0, 0, INNER_RADIUS + 2, 0, Math.PI * 2);
            ctx.fill();

            ctx.restore();
        }

        function animate(time: number) {
            if (width > 0 && height > 0) {
                ctx.clearRect(0, 0, width, height);
                // Частицы
                for (const p of particles) {
                    const pulse =
                        0.55 + Math.sin(time * p.speed * 1000 + p.phase) * 0.45;
                    ctx.beginPath();
                    ctx.arc(
                        p.x * width,
                        p.y * height,
                        p.radius,
                        0,
                        Math.PI * 2,
                    );
                    ctx.fillStyle = `rgba(33, 190, 211, ${p.alpha * pulse})`;
                    ctx.fill();
                }
                drawFlower(time);
            }
            animationFrameId = requestAnimationFrame(animate);
        }

        const ro = new ResizeObserver(() => resize());
        if (canvas.parentElement) ro.observe(canvas.parentElement);

        resize();
        animationFrameId = requestAnimationFrame(animate);

        return () => {
            ro.disconnect();
            cancelAnimationFrame(animationFrameId);
        };
    }, []);

    return (
        <div className="flower-scene">
            <canvas ref={canvasRef} className="flower-canvas" />

            <div className="flower-vignette" />
        </div>
    );
}
