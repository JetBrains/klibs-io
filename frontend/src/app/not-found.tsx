import KodeeNotFound from "@/app/ui/kodee-not-found";
import React from "react";


export default function NotFound() {
    return (
        <>
            <div className="container mb-4">

                <div className="px-4 py-5 my-5 text-center">
                    <KodeeNotFound/>

                    <h1 data-testid="not-found-page-message" className="pt-2 display-5 fw-bold text-body-emphasis">Page not found</h1>
                    <div className="col-lg-6 mx-auto">
                        <figure className="pt-5">
                            <blockquote className="blockquote">
                                <p>
                                    There were supposed to be ChatGPT-generated jokes about Kotlin here,
                                    but Seb thought they weren&apos;t funny enough
                                </p>
                            </blockquote>
                        </figure>
                    </div>
                </div>
            </div>
        </>
    )
}