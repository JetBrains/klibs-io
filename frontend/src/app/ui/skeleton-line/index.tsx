interface SkeletonProps {
	size?: string;
	height?: string;
}

export default function SkeletonLine({ size = '100%', height = '1rem' }: SkeletonProps) {
	const skeletonStyle = {
		width: size,
		height: height,
		backgroundColor: '#e0e0e0',
		borderRadius: '4px',
		animation: 'pulse 1.5s infinite',
		display: 'block'
	};

	return <span style={skeletonStyle}></span>;
};

