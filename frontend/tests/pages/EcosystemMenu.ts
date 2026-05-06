import { expect, Locator, Page } from '@playwright/test';
import { MainPage } from './MainPage';

export class EcosystemMenu {
  readonly page: Page;
  readonly mainPage: MainPage;
  readonly menuButton: Locator;
  readonly mainLinks: Locator;
  readonly links: Locator;

  constructor(page: Page) {
    this.page = page;
    this.mainPage = new MainPage(page);
    this.menuButton = page.getByTestId('ecosystem-menu-button');
    this.mainLinks = page.getByTestId('ecosystem-menu-main-links');
    this.links = page.getByTestId('ecosystem-menu-links');
  }

  async open() {
    await this.mainPage.open();
    await expect(this.menuButton).toBeVisible();
    await this.menuButton.click();
  }

  async clickMainLink(name: string) {
    const link = this.mainLinks.getByRole('link', { name });
    await expect(link).toBeVisible();
    await link.click();
  }

  async clickLink(name: string) {
    const link = this.links.getByRole('link', { name });
    await expect(link).toBeVisible();
    await link.click();
  }
}
