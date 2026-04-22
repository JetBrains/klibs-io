import { Fragment, ReactNode, useMemo } from "react";
import Link from "next/link";
import cn from 'classnames';
import { isFeaturedProject, isGrantWinner, kFormatter } from "@/app/types";
import { getPlatformName, getProjectLink, ProjectSearchResults, sortedPlatforms } from "@/app/types";
import { cardCn } from '@rescui/card';
import { textCn } from '@rescui/typography';
import { StarIcon, ReadIcon, WinIcon, RocketIcon } from '@rescui/icons';
import PlatformTag from "@/app/ui/platform-tag";

import styles from './styles.module.css';
import { trackEvent, GAEvent } from "@/app/analytics";

interface ProjectCardProps {
	featuredProject?: ProjectSearchResults;
	className?: string;
	search?: string;
}

function SearchTextWrap({ search, children }: { search?: ProjectCardProps['search'], children?: string | null }) {
	return useMemo(() => {
		if (!search || !children) return children;

		const chunks: ReactNode[] = [];
		let text = children;
		const query = search.toLowerCase();

		do {
			const i = text.toLowerCase().indexOf(query);

			if (i === -1) {
				text && chunks.push(<Fragment key={i}>{text}</Fragment>);
				break
			}

			const item = text.substring(0, i);
			const suffixIndex = i + query.length;

			chunks.push(<Fragment key={i + '-' + text.length}>{item}<span
				className={styles.highlight}>{text.substring(i, suffixIndex)}</span></Fragment>);
			text = text.substring(suffixIndex);
		} while (true);

		if (chunks.length === 1) return chunks[0];
		return <>{chunks}</>
	}, [search, children]);
}

export default function ProjectCard({ featuredProject, className, search }: ProjectCardProps) {
	const projectLink = featuredProject ? getProjectLink(featuredProject) : null;

	const hasTags = featuredProject && featuredProject.tags && featuredProject.tags.length > 0;
	const tagsContent = featuredProject && featuredProject.tags && featuredProject.tags.map((tag, i) => <>
		{i !== 0 ? ', ' : null}
		<SearchTextWrap key={tag} search={search}>{`#${tag}`}</SearchTextWrap>
	</>);

	const showGrantWinner = featuredProject && isGrantWinner(featuredProject);
	const showFeaturedProject = featuredProject && isFeaturedProject(featuredProject);

	return (
		// Solution for <Link> href error: https://stackoverflow.com/questions/66821351/nextjs-error-message-failed-prop-type-the-prop-href-expects-a-string-or-o
		<Link
			className={cn(cardCn({
				isClickable: true, paddings: 16,
			}), styles.card, className, {
				[styles.featuredProject]: showFeaturedProject,
				[styles.grantProject]: showGrantWinner
			})}
			href={String(projectLink)}
			title={featuredProject && featuredProject.name}
			onClick={() => trackEvent(GAEvent.PROJECT_CARD_CLICK, { eventCategory: `${featuredProject?.name}` })}
		>
			{featuredProject &&
				<>

					{/*Card content*/}

					<div className={styles.bodyWrapper}>
						{/*Heading row*/}
						<div className={styles.headingWrapper}>

							{/*Title & author*/}
							<div className={styles.headingLeftSideWrapper}>

								{/*Title*/}
								<h4 className={cn(textCn('rs-h4'), styles.title)}>
									<SearchTextWrap search={search}>{featuredProject.name}</SearchTextWrap>
								</h4>

								{/*Author */}
								<div className={styles.headingAuthorWrapper}>
									<p className={cn(textCn('rs-text-3', { hardness: 'hard' }), styles.authorLabel)}>
										by {featuredProject.ownerLogin}
									</p>

									{(showGrantWinner || showFeaturedProject) &&
										<div className={styles.featuredIconWrapper}>
											{showGrantWinner &&
												<div className={styles.featuredWrapper}>
													<WinIcon size={'m'} className={styles.featuredIcon} />
													<span className={cn('rs-h5', styles.featuredTitle)}>Kotlin grant winner</span>
												</div>
											}

											{showFeaturedProject && !showGrantWinner &&
												<div className={styles.featuredWrapper}>
													<RocketIcon size={'m'} className={styles.featuredIcon} />
													<span className={cn('rs-h5', styles.featuredTitle)}>Featured</span>
												</div>
											}
										</div>
									}
								</div>
							</div>

							{/*GH stars*/}
							<div className={styles.headingRightSideWrapper}>

								<StarIcon size={'s'} />

								{/*GH star value*/}
								<p className={cn(textCn('rs-text-1', { hardness: 'hard' }))}>
									{kFormatter(featuredProject.scmStars)}
								</p>
							</div>
						</div>

						{/*Description*/}
						{/*To-do: Truncation for description*/}
						<p className={cn(textCn('rs-text-3', { hardness: 'hard' }), styles.cardDescription)}>
							<SearchTextWrap search={search}>{featuredProject.description}</SearchTextWrap>
						</p>

						{/*Tags*/}
						{hasTags && <div className={styles.tagsRow}>
							<p className={cn(textCn('rs-text-3'))}
								title={'#' + featuredProject.tags?.join(', #')}>
								{tagsContent}
							</p>
						</div>}
					</div>

					{/*Footer section*/}
					<div className={styles.footerWrapper}>

						{/*Platforms*/}
						<div className={styles.footerPlatforms}>
							{sortedPlatforms(featuredProject.platforms).map(platform => (
								<PlatformTag key={platform} className={styles.platformTag}>
									{getPlatformName(platform)}
								</PlatformTag>
							))}
						</div>

						{/*License*/}
						<div className={styles.footerRow}>
							<ReadIcon size={'m'} className={styles.licenseIcon} />
							<p className={cn(textCn('rs-text-3'))}>
								{featuredProject.licenseName || 'Unknown license'}
							</p>
						</div>
					</div>
				</>
			}
		</Link>
	);
}
