"use client"

import React from "react";
import {OwnerAuthor, ProjectSearchResults} from "@/app/types";
import Image from "next/image";
import ProjectCard from "@/app/ui/project-card";
import Container from "@/app/ui/container";

interface AuthorPageContentProps {
    initialAuthor: OwnerAuthor;
    initialProjects: ProjectSearchResults[];
}

export default function Author({initialAuthor, initialProjects}: AuthorPageContentProps) {
    const author = initialAuthor;
    const projects = initialProjects;

    return (
        <Container mode={"container"} className="padding-top-medium padding-bottom-large">
            <Container mode={"wrapper"} split>

                {/*Id section*/}
                <Container mode={"wrapper"} smallColumn>
                    <div className="row align-items-center">
                        <div className="col-md-12 col-3">
                            <div style={{maxWidth: '200px'}}>
                                <Image src={author.avatarUrl}
                                       width={200}
                                       height={200}
                                       className="rounded img-fluid"
                                       alt={`${author.name} avatar`}
                                />
                            </div>
                        </div>

                        <div className="col-md-12 col-9 pt-md-3 pt-0">
                            <h1 className="h3 mb-0" data-testid="author-name">
                                {author.name}
                            </h1>
                            <h5 className="fw-lighter" data-testid="author-login">{author.login}</h5>
                        </div>
                    </div>

                    {author.description &&
                        <div className="row pt-md-1 pt-3" data-testid="author-description">
                            <p>{author.description}</p>
                        </div>
                    }

                    <div className="row pt-2">
                        <ul className="list-unstyled">
                            <li data-testid="author-followers">
                                <span className="d-flex align-items-center gap-2">
                                    <i className="bi bi-people"></i>
                                    <span className="text">
                                        {author.followers} followers
                                    </span>
                                </span>
                            </li>

                            {author.company &&
                                <li data-testid="author-company">
                                    <span className="d-flex align-items-center gap-2">
                                        <i className="bi bi-building"></i>
                                        <span className="text">{author.company}</span>
                                    </span>
                                </li>
                            }

                            {author.location &&
                                <li data-testid="author-location">
                                    <span className="d-flex align-items-center gap-2">
                                        <i className="bi bi-geo"></i>
                                        <span className="text">{author.location}</span>
                                    </span>
                                </li>
                            }
                        </ul>
                    </div>

                    <div className="row pt-2">
                        <ul className="list-unstyled">
                            {author.homepage &&
                                <li>
                                    <a
                                        className="link-dark d-flex align-items-center gap-2 link-underline link-underline-opacity-0 link-underline-opacity-50-hover"
                                        target="_blank"
                                        data-testid="author-homepage-link"
                                        href={author.homepage}>
                                        <i className="bi bi-box-arrow-up-right"></i>
                                        <span className="text">{author.homepage}</span>
                                    </a>
                                </li>
                            }
                            {author.twitterHandle &&
                                <li>
                                    <a
                                        className="link-dark d-flex align-items-center gap-2 link-underline link-underline-opacity-0 link-underline-opacity-50-hover"
                                        target="_blank"
                                        data-testid="author-twitter-link"
                                        href={`https://x.com/${author.twitterHandle}`}>
                                        <i className="bi bi-twitter-x"></i>
                                        <span className="text">{author.twitterHandle}</span>
                                    </a>
                                </li>
                            }

                            {author.login &&
                                <li>
                                    <a
                                        className="link-dark d-flex align-items-center gap-2 link-underline link-underline-opacity-0 link-underline-opacity-50-hover"
                                        target="_blank"
                                        data-testid="author-github-login"
                                        href={`https://github.com/${author.login}`}>
                                        <i className="bi bi-github"></i>
                                        <span className="text">{author.login}</span>
                                    </a>
                                </li>
                            }
                        </ul>
                    </div>
                </Container>

                {/*Projects section*/}
                <Container mode={"wrapper"} cardColumn dataTestId="author-projects">
                    {projects && projects.length ? (
                        projects.map(project =>
                            <ProjectCard featuredProject={project} key={project.id}/>
                        )
                    ) : (
                        <p>No projects</p>
                    )}
                </Container>
            </Container>
        </Container>
    )
}
