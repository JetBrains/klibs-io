const FILL = { position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'contain' } as const;

// next/image needs Next's image loader; tests render the plain <img> it produces for the same source.
export default function NextImage({ alt, className, fill, height, src, width }: {
    alt: string;
    className?: string;
    fill?: boolean;
    height?: number;
    src: string | { src: string };
    width?: number;
}) {
    return (
        // eslint-disable-next-line @next/next/no-img-element -- this is the next/image test double
        <img
            alt={alt}
            className={className}
            height={height}
            src={typeof src === 'string' ? src : src.src}
            style={fill ? FILL : undefined}
            width={width}
        />
    );
}
