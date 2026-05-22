import type { Metadata } from "next";
import React from "react";

export const metadata: Metadata = {
    title: { absolute: "klibs.io insights" },
    robots: { index: false, follow: false },
};

export default function InsightsLayout({ children }: { children: React.ReactNode }) {
    return <>{children}</>;
}
