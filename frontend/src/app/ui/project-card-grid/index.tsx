import React from "react";
import Container from "@/app/ui/container";


export function ProjectCardGrid({ children }: { children: React.ReactNode }) {
    return (
        <Container mode="wrapper" cardGrid>
            {children}
        </Container>
    );
}