import { vi } from 'vitest';

import { PackageSearchResults, ProjectSearchResults, TagsStats } from '@/app/types';

import { tagsStats } from './fixtures';

// Client components call these directly; every visual test answers them from fixtures.
export const getTagsStats = vi.fn(async (): Promise<TagsStats> => tagsStats());
export const searchProjects = vi.fn(async (): Promise<ProjectSearchResults[]> => []);
export const searchPackages = vi.fn(async (): Promise<PackageSearchResults[]> => []);
