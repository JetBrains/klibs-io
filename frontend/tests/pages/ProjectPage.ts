import { expect, Locator, Page, BrowserContext } from '@playwright/test';

export class ProjectPage {
  readonly page: Page;
  readonly readmeTabContent: Locator;
  readonly packagesTab: Locator;
  readonly packageIdText: Locator;
  readonly homepageLink: Locator;
  readonly githubRepoLink: Locator;
  readonly githubPagesLink: Locator;

  constructor(page: Page) {
    this.page = page;
    this.readmeTabContent = page.getByTestId('readme-tab').first();
    this.packagesTab = page.getByRole('tab', { name: 'Packages' }).first();
    this.packageIdText = page.getByText('Package id').first();
    this.homepageLink = page.getByRole('link', { name: 'Homepage' });
    this.githubRepoLink = page.getByRole('link', { name: 'GitHub repository' });
    this.githubPagesLink = page.getByRole('link', { name: 'GitHub pages' });
  }

  async open(org: string, repo: string) {
    await this.page.goto(`/project/${org}/${repo}`);
    await this.acceptCookiesIfPresent();
  }

  async acceptCookiesIfPresent() {
    if (process.env.PROD) {
      await this.page.waitForSelector('button.ch2-btn.ch2-btn-primary');
      await this.page.click('button.ch2-btn.ch2-btn-primary');
    }
  }

  async projectReadmeIsNotEmpty() {
    await expect(this.readmeTabContent).toBeVisible();
    await expect(this.readmeTabContent).not.toBeEmpty();
  }


  async openPackagesTab() {
    await expect(this.packagesTab).toBeVisible();
    await this.packagesTab.click();
  }

  async expectPackageIdVisible() {
    await expect(this.packageIdText).toBeVisible();
  }

  async clickHomepageAndGetNewPageUrl(context: BrowserContext): Promise<string> {
    return await this.clickLinkAndGetNewPageUrl(this.homepageLink, context);
  }

  async clickGitHubRepositoryAndGetNewPageUrl(context: BrowserContext): Promise<string> {
    return await this.clickLinkAndGetNewPageUrl(this.githubRepoLink, context);
  }

  async clickGitHubPagesAndGetNewPageUrl(context: BrowserContext): Promise<string> {
    return await this.clickLinkAndGetNewPageUrl(this.githubPagesLink, context);
  }

  private async clickLinkAndGetNewPageUrl(link: Locator, context: BrowserContext): Promise<string> {
    await expect(link).toBeVisible();
    const newPagePromise = context.waitForEvent('page');
    await link.click();
    const newPage = await newPagePromise;
    await newPage.waitForLoadState();
    return newPage.url();
  }
}
