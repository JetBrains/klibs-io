import {PackageOverview} from "@/app/types";
import TargetsTable from "@/app/ui/targets-table";

interface TargetsTableProps {
    projectPackage: PackageOverview;
}

export default function TargetsTablePopup({projectPackage} : TargetsTableProps) {
    return <TargetsTable projectPackage={projectPackage}/>
}
