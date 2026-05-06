import KodeeNotFound from "@/app/ui/kodee-not-found";
import React from "react";


export function ContentFailed() {
    return (
        <div className="px-4 py-5 my-5 text-center">
            <KodeeNotFound/>

            <h1 className="pt-2 display-5 fw-bold text-body-emphasis">Boss, we have a problem!</h1>
            <div className="col-lg-6 mx-auto">
                <figure className="pt-5">
                    <blockquote className="blockquote">
                        <p>
                            We&apos;re on it, and we&apos;ll have it fixed in no time.
                        </p>
                    </blockquote>
                </figure>
            </div>
        </div>
    );
}