import type {Metadata} from "next";
import "./globals.css";

import Navbar from "@/app/ui/navbar";
import React from "react";
import { GoogleTagManager } from "@next/third-parties/google";

import 'bootstrap/dist/css/bootstrap.css';
import 'bootstrap-icons/font/bootstrap-icons.css';
import '@rescui/typography/lib/font-jb-sans-auto.css';
import { getProjectsCount } from "@/app/api";

export async function generateMetadata(): Promise<Metadata> {
    const projectsCount = await getProjectsCount();

    return {
        metadataBase: new URL(process.env.NEXT_PUBLIC_METADATA_URL ?? ''),
        title: {
            default: `Klibs.io — Search ${projectsCount}+ KMP projects`,
            template: '%s | Klibs.io'
        },
        description: 'AI-powered search engine for Kotlin Multiplatform projects. Explore the best KMP packages, access metadata, and learn about the authors.',
        openGraph: {
            type: 'website',
            title: {
                default: `Klibs.io — Search ${projectsCount}+ KMP projects`,
                template: '%s | Klibs.io'
            },
            description: 'AI-powered search engine for Kotlin Multiplatform projects. Explore the best KMP packages, access metadata, and learn about the authors.',
            url: 'https://klibs.io'
        },
        twitter: {
            card: 'summary_large_image',
            title: {
                default: `Klibs.io — Search ${projectsCount}+ KMP projects`,
                template: '%s | Klibs.io'
            },
            description: 'AI-powered search engine for Kotlin Multiplatform projects. Explore the best KMP packages, access metadata, and learn about the authors.'
        },
    };
}

export default async function RootLayout({ children }: { children: React.ReactNode }) {
    // className="h-100"
    return (
        <html lang="en" data-bs-theme="light">
            <GoogleTagManager gtmId="GTM-5P98"/>

            <body className="d-flex flex-column">
                <Navbar />
                {children}
            </body>
        </html>
    );
}
