"use client"

import React from "react";
import {OwnerOrganization, ProjectSearchResults} from "@/app/types";
import Image from "next/image";
import ProjectCard from "@/app/ui/project-card";
import Container from "@/app/ui/container";

interface OrganizationPageContentProps {
    initialOrganization: OwnerOrganization;
    initialProjects: ProjectSearchResults[];
}

export default function Organization({initialOrganization, initialProjects}: OrganizationPageContentProps) {
    const organization = initialOrganization;
    const projects = initialProjects;

    return (
        <Container mode={"container"} className="padding-top-medium padding-bottom-large">
            <Container mode={"wrapper"} split>

                {/*Id section*/}
                <Container mode={"wrapper"} smallColumn>
                    <div className="row align-items-center">
                        <div className="col-md-12 col-3">
                            <div style={{maxWidth: '200px'}}>
                                <Image src={organization.avatarUrl}
                                       className="rounded img-fluid"
                                       alt={`${organization.name} logo`}
                                       height={200}
                                       width={200}
                                />
                            </div>
                        </div>

                        <div className="col-md-12 col-9 pt-md-3 pt-0">
                            <h1 className="h2 mb-1 bolder" data-testid="organization-name">
                                {organization.name}
                            </h1>
                        </div>
                    </div>

                    {organization.description &&
                        <div className="row pt-md-1 pt-3" data-testid="organization-description">
                            <p>{organization.description}</p>
                        </div>
                    }

                    <div className="row pt-2" data-testid="organization-links">
                        <ul className="list-unstyled">
                            {organization.homepage &&
                                <li>
                                    <a className="link-dark d-flex align-items-center gap-2 link-underline link-underline-opacity-0 link-underline-opacity-50-hover"
                                       target="_blank"
                                       data-testid="organization-homepage-link"
                                       href={organization.homepage}>
                                        <i className="bi bi-box-arrow-up-right"></i>
                                        <span className="text">{organization.homepage}</span>
                                    </a>
                                </li>
                            }
                            {organization.twitterHandle &&
                                <li>
                                    <a
                                        className="link-dark d-flex align-items-center gap-2 link-underline link-underline-opacity-0 link-underline-opacity-50-hover"
                                        target="_blank"
                                        data-testid="organization-twitter-link"
                                        href={`https://x.com/${organization.twitterHandle}`}>
                                        <i className="bi bi-twitter-x"></i>
                                        <span className="text">{organization.twitterHandle}</span>
                                    </a>
                                </li>
                            }
                            {organization.login &&
                                <li>
                                    <a className="link-dark d-flex align-items-center gap-2 link-underline link-underline-opacity-0 link-underline-opacity-50-hover"
                                       target="_blank"
                                       data-testid="organization-github-login"
                                       href={`https://github.com/${organization.login}`}>
                                        <i className="bi bi-github"></i>
                                        <span className="text">{organization.login}</span>
                                    </a>
                                </li>
                            }
                            {organization.email &&
                                <li>
                                    <a className="link-dark d-flex align-items-center gap-2 link-underline link-underline-opacity-0 link-underline-opacity-50-hover"
                                       target="_blank"
                                       data-testid="organization-email-link"
                                       href={`mailto:${organization.email}`}>
                                        <i className="bi bi-envelope"></i>
                                        <span className="text">{organization.email}</span>
                                    </a>
                                </li>
                            }
                        </ul>
                    </div>
                </Container>

                {/*Projects section*/}
                <Container mode={"wrapper"} cardColumn dataTestId="organization-projects">
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
