import type {Metadata} from "next";
import ComparePageContent from './compare-page-content'
import {compareProjects} from "@/app/api";
import {MAX_COMPARE_ITEMS} from "@/app/constants";
import {parseCompareParams} from "@/app/helpers";

export const metadata: Metadata = {
    title: 'Compare projects',
};

type SearchParamsProps = {
    projects?: string | string[];
};

export default async function Page({searchParams}: { searchParams: SearchParamsProps }) {
    const references = parseCompareParams(searchParams.projects).slice(0, MAX_COMPARE_ITEMS);
    const projects = references.length > 0 ? await compareProjects(references) : [];

    return <ComparePageContent projects={projects}/>;
}
