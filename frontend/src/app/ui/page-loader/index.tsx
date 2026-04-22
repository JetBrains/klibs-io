import KodeeSpinner from "@/app/ui/kodee-spinner";

export default function PageLoader() {
    return (
        <div className="d-flex justify-content-center align-items-center vh-100">
            <div className="text-center">
                <KodeeSpinner />
            </div>
        </div>
    );
}