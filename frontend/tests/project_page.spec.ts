import { test, expect } from '@playwright/test';
import { ProjectPage } from './pages/ProjectPage';

test.describe('Project page (Arrow)', () => {
  let projectPage: ProjectPage;

  test.beforeEach(async ({ page }) => {
    projectPage = new ProjectPage(page);
    await projectPage.open('arrow-kt', 'arrow');
  });

  test('Project description for the "Arrow" is not empty', async ({ page }) => {
    await projectPage.projectReadmeIsNotEmpty()
  });

  test('Click on "Packages" opens the related tab', async ({ page }) => {
    await projectPage.openPackagesTab();
    await projectPage.expectPackageIdVisible();
  });

  test('Click on "Homepage" button opens the github page of the project', async ({ context }) => {
    const url = await projectPage.clickHomepageAndGetNewPageUrl(context);
    await expect(url).toContain('https://arrow-kt.io/');
  });

  test('Click on "GitHub repository" button opens the github repository of the project', async ({ context }) => {
    const url = await projectPage.clickGitHubRepositoryAndGetNewPageUrl(context);
    await expect(url).toContain('https://github.com/arrow-kt/arrow');
  });

  test('Click on "GitHub pages" button opens the github page of the project', async ({ context }) => {
    const url = await projectPage.clickGitHubPagesAndGetNewPageUrl(context);
    await expect(url).toContain('https://apidocs.arrow-kt.io/');
  });
});