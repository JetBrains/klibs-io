import Image from "next/image";

import kodeeWalking from '@/app/img/kodee/kodee-walking.gif';
import kodeeLoading from '@/app/img/kodee/kodee-loading.gif';
import kodeeFloating from '@/app/img/kodee/kodee-floating.gif';


const images = [
	kodeeWalking,
	kodeeLoading,
	kodeeFloating,
];

export default function KodeeSpinner() {
	const kodeeImgSrc = images[Math.floor(Math.random() * 3)];

	return (
		<Image
			src={kodeeImgSrc}
	        alt="Kodee spinning"
			width={64}
			height={64}
			unoptimized
		/>
	);
}