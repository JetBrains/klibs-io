import React, { useEffect, useRef, useCallback } from 'react';
import styles from './background-squares.module.css';

interface Square {
    baseX: number;
    baseY: number;
    x: number;
    y: number;
    size: number;
    angle: number;
    speed: number;
    radius: number;
    phase: number;
}

const getRandomInRange = (min: number, max: number) =>
    Math.random() * (max - min) + min;

const SQUARES_COUNT = { min: 12, max: 20 };

const SAFE_ZONE = 16;
const DESKTOP_WIDTH_RATIO = 0.7;
const COLOR_THRESHOLD = 72; // Squares above this Y are pink, below are white
const COLOR_PINK = 'rgba(253, 185, 215, 1)';
const COLOR_WHITE = 'rgba(255, 255, 255, 0.5)';
const SPEED = { min: 0.15, max: 0.35 };

interface BackgroundSquaresProps {
    debug?: boolean;
}

export const BackgroundSquares: React.FC<BackgroundSquaresProps> = ({ debug = false }) => {
    const canvasRef = useRef<HTMLCanvasElement>(null);
    const squaresRef = useRef<Square[]>([]);
    const animationFrameRef = useRef<number>(0);
    const lastTimeRef = useRef<number>(0);

    const generateSquares = useCallback((width: number, height: number): Square[] => {
        const count = Math.floor(getRandomInRange(SQUARES_COUNT.min, SQUARES_COUNT.max));

        const squares: Square[] = [];

        const movementRadius = getRandomInRange(8, 16);
        const minX = SAFE_ZONE + movementRadius;
        const maxX = width - SAFE_ZONE - movementRadius - 8; // 8 is max square size
        const minY = SAFE_ZONE + movementRadius;
        const maxY = height - SAFE_ZONE - movementRadius - 8;

        const isOverlapping = (x: number, y: number, size: number, radius: number): boolean => {
            const minDistance = SAFE_ZONE;
            for (const sq of squares) {
                // Account for movement radius of both squares
                const totalRadius = radius + sq.radius + minDistance + Math.max(size, sq.size);
                const dx = x - sq.baseX;
                const dy = y - sq.baseY;
                const distance = Math.sqrt(dx * dx + dy * dy);
                if (distance < totalRadius) {
                    return true;
                }
            }
            return false;
        };

        const maxAttempts = 50;

        for (let i = 0; i < count; i++) {
            const radius = getRandomInRange(8, 16);
            const size = Math.random() > 0.5 ? 4 : 8;

            let baseX: number;
            let baseY: number;
            let attempts = 0;

            do {
                baseX = Math.max(minX, Math.min(maxX, width - Math.pow(Math.random(), 0.7) * width));
                baseY = getRandomInRange(minY, maxY);
                attempts++;
            } while (isOverlapping(baseX, baseY, size, radius) && attempts < maxAttempts);

            // Only add if we found a non-overlapping position
            if (attempts < maxAttempts) {
                squares.push({
                    baseX,
                    baseY,
                    x: baseX,
                    y: baseY,
                    size,
                    angle: Math.random() * Math.PI * 2,
                    speed: getRandomInRange(SPEED.min, SPEED.max),
                    radius,
                    phase: Math.random() * Math.PI * 2,
                });
            }
        }

        return squares;
    }, []);

    const updateSquares = useCallback((deltaTime: number) => {
        squaresRef.current.forEach((square) => {
            square.angle += square.speed * deltaTime;

            // Lissajous-like movement for more organic feel
            square.x = square.baseX + Math.sin(square.angle) * square.radius;
            square.y = square.baseY + Math.cos(square.angle * 0.7 + square.phase) * square.radius;
        });
    }, []);

    const draw = useCallback((ctx: CanvasRenderingContext2D) => {
        const { width, height } = ctx.canvas;
        ctx.clearRect(0, 0, width, height);

        const isLargeDesktop = window.innerWidth >= 1440;
        squaresRef.current.forEach((square) => {
            const color = isLargeDesktop && square.y < COLOR_THRESHOLD ? COLOR_PINK : COLOR_WHITE;
            ctx.fillStyle = debug ? 'red' : color;
            ctx.fillRect(square.x, square.y, square.size, square.size);
        });
    }, [debug]);

    const animate = useCallback((timestamp: number) => {
        const canvas = canvasRef.current;
        if (!canvas) return;

        const ctx = canvas.getContext('2d');
        if (!ctx) return;

        const deltaTime = lastTimeRef.current ? (timestamp - lastTimeRef.current) / 1000 : 0;
        lastTimeRef.current = timestamp;

        updateSquares(deltaTime);
        draw(ctx);

        animationFrameRef.current = requestAnimationFrame(animate);
    }, [updateSquares, draw]);

    useEffect(() => {
        const canvas = canvasRef.current;
        if (!canvas) return;

        const updateCanvasSize = () => {
            const parent = canvas.parentElement;
            if (!parent) return;

            const rect = parent.getBoundingClientRect();
            const dpr = window.devicePixelRatio || 1;
            const isLargeDesktop = window.innerWidth >= 1440;

            const width = isLargeDesktop ? rect.width * DESKTOP_WIDTH_RATIO : rect.width;
            const height = isLargeDesktop ? 365 : rect.height; // 293 + 72 on large desktop

            canvas.width = width * dpr;
            canvas.height = height * dpr;
            canvas.style.width = `${width}px`;
            canvas.style.height = `${height}px`;

            const ctx = canvas.getContext('2d');
            if (ctx) {
                ctx.scale(dpr, dpr);
            }

            // Regenerate squares on resize
            squaresRef.current = generateSquares(width, height);
        };

        updateCanvasSize();
        window.addEventListener('resize', updateCanvasSize);

        // Start animation
        animationFrameRef.current = requestAnimationFrame(animate);

        return () => {
            window.removeEventListener('resize', updateCanvasSize);
            if (animationFrameRef.current) {
                cancelAnimationFrame(animationFrameRef.current);
            }
        };
    }, [generateSquares, animate]);

    return (
        <canvas
            ref={canvasRef}
            className={styles.squaresContainer}
        />
    );
};
