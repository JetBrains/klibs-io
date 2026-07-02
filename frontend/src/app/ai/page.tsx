"use client"

import React from "react";
import cn from "classnames";

import Container from "@/app/ui/container";
import {Button} from '@rescui/button';

import {CopyIcon} from '@rescui/icons'

import styles from "./styles.module.css";

import {textCn} from "@rescui/typography";
import {CodeHighlight} from "@/app/ui/code-highlight/code-highlight";


const AGENTS_CODE_SAMPLE = `## Kotlin Multiplatform library selection

When adding or recommending Kotlin Multiplatform dependencies, 
query the klibs.io MCP (https://api.klibs.io/mcp) before choosing a library.

Use it to verify dependency metadata:

- supported targets,
- maven coordinate,
- latest versions or latest stable versions,
- license,
- maintenance/activity signals,
- comparable alternatives
- etc.`

export default function Organization() {

    const handleCopy = () => {
        navigator.clipboard.writeText(AGENTS_CODE_SAMPLE);
    };

    return (
        <Container mode={"container"} className={styles.pageContainer}>
            <Container mode={"wrapper"} split>

                <Container mode={"wrapper"} cardColumn>

                    <section className={styles.section} id={"header"}>
                        <h1 className={textCn('rs-h1')}>AI development with klibs.io</h1>

                        <p className={textCn('rs-subtitle-2')}>
                            Use AI tools and agents to discover, integrate, and maintain
                            Kotlin Multiplatform libraries.
                        </p>
                    </section>

                    <section className={styles.section} id={"mcp"}>
                        <h2 className={cn(textCn('rs-h2'), styles.sectionHeading)}>MCP</h2>

                        <p className={textCn('rs-text-1', {hardness: 'hard'})}>
                            MCP enables AI agents to interact with klibs.io through
                            structured endpoints instead of parsing web pages. This allows AI tools to retrieve accurate
                            and up-to-date library data, such as versions and metadata, in a reliable and predictable
                            way.
                        </p>

                        <p className={textCn('rs-text-1', {hardness: 'hard'})}>
                            <a className={textCn('rs-link', {mode: 'classic'})}>Add klibs.io MCP to your agent</a>
                        </p>

                    </section>


                    <section className={styles.section} id={"skill"}>

                        <h2 className={cn(textCn('rs-h2'), styles.sectionHeading)}>Skill</h2>

                        <p className={textCn('rs-text-1', {hardness: 'hard'})}>Skills are reusable, task-specific
                            instructions that extend AI tools with domain knowledge and specialized workflows.</p>
                        <p className={textCn('rs-text-1', {hardness: 'hard'})}>
                            By using the skill, AI tools can make more accurate library recommendations and dependency
                            decisions based on verified KMP metadata rather than outdated training data.
                        </p>
                        <p className={textCn('rs-text-1', {hardness: 'hard'})}>The klibs.io Skill equips AI agents with
                            expert knowledge in Kotlin Multiplatform libraries.
                            It helps them:
                        </p>

                        <ul className={cn(textCn('rs-ul'), textCn('rs-text-1', {hardness: 'hard'}))}>
                            <li>discover and compare KMP libraries</li>
                            <li>recommend libraries for specific use case</li>
                            <li>verify platform support</li>
                            <li>retrieve up-to-date dependency coordinates and stable versions directly from klibs.io
                            </li>
                        </ul>

                        <p className={textCn('rs-text-1', {hardness: 'hard'})}>
                            <a className={textCn('rs-link', {mode: 'classic'})}>Instruction how to use klibs.io
                                skill</a>
                        </p>

                    </section>

                    <section className={styles.section} id="agents">

                        <h2 className={cn(textCn('rs-h2'), styles.sectionHeading)}>AGENTS.md recommendation</h2>

                        <p className={textCn('rs-text-1', {hardness: 'hard'})}>
                            To ensure your AI tools use up-to-date Kotlin Multiplatform library information, add the
                            following instruction to your <span>AGENTS.md</span>:
                        </p>

                        <div className={styles.codeWrapper}>
                            <CodeHighlight className={styles.codeBlock} code={AGENTS_CODE_SAMPLE} language="markdown"/>
                            <Button
                                icon={<CopyIcon/>}
                                mode={'outline'}
                                onClick={handleCopy}
                                className={styles.codeCopyButton}
                            >
                                Copy
                            </Button>
                        </div>


                        <p className={textCn('rs-text-1', {hardness: 'hard'})}>
                            This helps the agent consistently use verified KMP library data instead of relying solely on
                            training data or general web search.
                        </p>

                    </section>
                </Container>

                <Container mode={"wrapper"} smallColumn>
                    sidebar
                </Container>
            </Container>
        </Container>
    )
}
