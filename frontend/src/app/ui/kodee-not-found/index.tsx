"use client"

import Image from "next/image";

// Define an array of image paths or URLs
import kodeeFrightened from '@/app/img/kodee/kodee-frightened.png';
import kodeeFrustrated from '@/app/img/kodee/kodee-frustrated.png';
import kodeeLost from '@/app/img/kodee/kodee-lost.png';
import kodeeShocked from '@/app/img/kodee/kodee-shocked.png';
import kodeeSurprised from '@/app/img/kodee/kodee-surprised.png';
import kodeeBrokenHearted from '@/app/img/kodee/kodee-broken-hearted.png';


const images = [
    kodeeFrightened,
    kodeeFrustrated,
    kodeeLost,
    kodeeShocked,
    kodeeSurprised,
    kodeeBrokenHearted
];

export default function KodeeNotFound() {
    const kodeeImgSrc = images[Math.floor(Math.random() * 6)];

    return (
        <Image
            src={kodeeImgSrc}
            alt="Kodee lost"
            width={200}
            height={200}
        />
    );
}