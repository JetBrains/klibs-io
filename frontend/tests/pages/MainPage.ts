import { expect, Locator, Page } from '@playwright/test';

export class MainPage {
  readonly page: Page;
  readonly logoLink: Locator;
  readonly reportIssueLink: Locator;
  readonly faqLink: Locator;
  readonly slackLink: Locator;
  readonly ecosystemMenuButton: Locator;
  readonly kotlinEcosystemPopupItem: Locator;
  readonly createAndSubmitProjectLink: Locator;

  constructor(page: Page) {
    this.page = page;
    this.logoLink = page.getByRole('link', { name: 'Klibs.io wordmark' });
    this.reportIssueLink = page.getByRole('link', { name: 'Report an Issue' });
    this.faqLink = page.getByRole('link', { name: 'FAQ' });
    this.slackLink = page.getByTestId('slack-link');
    this.ecosystemMenuButton = page.getByTestId('ecosystem-menu-button');
    this.kotlinEcosystemPopupItem = page.getByText('Kotlin Playground');
    this.createAndSubmitProjectLink = page.getByTestId('create-and-submit-project-link');
  }

  async open() {
    await this.page.goto('/');
    await this.acceptCookiesIfPresent();
  }

  async acceptCookiesIfPresent() {
    if (process.env.PROD) {
      await this.page.waitForSelector('button.ch2-btn.ch2-btn-primary');
      await this.page.click('button.ch2-btn.ch2-btn-primary');
    }
  }

  async clickLogo() {
    await this.logoLink.click();
  }

  async clickReportIssue() {
    await this.reportIssueLink.click();
  }

  async getReportIssueHref() {
    return await this.reportIssueLink.getAttribute('href');
  }

  async clickFAQ() {
    await this.faqLink.click();
  }

  async clickSlack() {
    await this.slackLink.click();
  }

  async openEcosystemMenu() {
    await this.ecosystemMenuButton.click();
  }

  async clickCreateAndSubmitProject() {
    await this.createAndSubmitProjectLink.first().click();
  }
}
