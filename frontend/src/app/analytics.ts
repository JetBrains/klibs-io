export enum GAEvent {
  REPORT_AN_ISSUE_CLICK = "report-an-issue-click",
  FAQ_CLICK = "faq-click",
  LOGO_CLICK = "logo-click",
  SLACK_CLICK = "slack-click",

  FILTER_PLATFORM_CHANGE = "filter-platform-change",
  SEARCH_MODE_TRIGGER_CHANGE = "search-mode-trigger-change",
  SEARCH_MODE_DROPDOWN_CLICK = "search-mode-dropdown-click",
  FILTER_DROPDOWN_CLICK = "filter-dropdown-click",

  CATEGORY_ALL_CLICK = "category-all-click",
  CATEGORY_TAG_CLICK = "category-tag-click",
  CATEGORY_TAG_SEE_ALL = "category-tag-see-all",
  CATEGORY_TAG_DROPDOWN_CLICK = "category-tag-dropdown-click",

  PACKAGE_PAGE_LINK_CLICK = "package-page-link-click",
  PACKAGE_VERSION_LINK_CLICK = "package-version-link-click",

  PROJECT_INFO_LINK_CLICK = "project-info-link-click",
  PROJECT_README_TAB_CLICK = "project-readme-tab-click",
  PROJECT_PACKAGES_TAB_CLICK = "project-packages-tab-click",
  PROJECT_PACKAGE_CLICK = "project-package-click",

  PACKAGE_CONTEXT_MENU_BUTTON_CLICK = "package-context-menu-button-click",
  PACKAGE_CONTEXT_MENU_DETAILS_CLICK = "package-context-menu-details-click",
  PACKAGE_CONTEXT_MENU_KOTLIN_SNIPPET_CLICK = "package-context-menu-kotlin-snippet-click",
  PACKAGE_CONTEXT_MENU_GROOVY_SNIPPET_CLICK = "package-context-menu-groovy-snippet-click",

  PACKAGE_IMPORT_TAB_CLICK = "package-import-tab-click",
  PACKAGE_COPY_SNIPPET = "package-copy-snippet",

  KOTLIN_ECOSYSTEM_DROPDOWN_CLICK = "kotlin-ecosystem-dropdown-click",

  PACKAGE_CARD_CLICK = "package-card-click",
  PROJECT_CARD_CLICK = "project-card-click",

  SEARCH_KEYBOARD_TRIGGER = "search-keyboard-trigger",
}

type NoParams = Record<string, never>;

export type GAEventParams = {
  [GAEvent.REPORT_AN_ISSUE_CLICK]: NoParams;
  [GAEvent.FAQ_CLICK]: NoParams;
  [GAEvent.LOGO_CLICK]: NoParams;
  [GAEvent.SLACK_CLICK]: NoParams;

  [GAEvent.FILTER_PLATFORM_CHANGE]: { eventCategory: string };
  [GAEvent.SEARCH_MODE_TRIGGER_CHANGE]: { eventCategory: string };
  [GAEvent.SEARCH_MODE_DROPDOWN_CLICK]: NoParams;
  [GAEvent.FILTER_DROPDOWN_CLICK]: NoParams;

  [GAEvent.CATEGORY_ALL_CLICK]: NoParams;
  [GAEvent.CATEGORY_TAG_CLICK]: { eventCategory: string };
  [GAEvent.CATEGORY_TAG_SEE_ALL]: NoParams;
  [GAEvent.CATEGORY_TAG_DROPDOWN_CLICK]: { eventCategory: string };

  [GAEvent.PACKAGE_PAGE_LINK_CLICK]: { eventCategory: string; eventLabel: string };
  [GAEvent.PACKAGE_VERSION_LINK_CLICK]: { eventCategory: string | null; eventLabel: string };

  [GAEvent.PROJECT_INFO_LINK_CLICK]: { eventCategory: string; eventLabel: string };
  [GAEvent.PROJECT_README_TAB_CLICK]: { eventCategory: string };
  [GAEvent.PROJECT_PACKAGES_TAB_CLICK]: { eventCategory: string };
  [GAEvent.PROJECT_PACKAGE_CLICK]: { eventCategory: string; eventLabel: string };

  [GAEvent.PACKAGE_CONTEXT_MENU_BUTTON_CLICK]: NoParams;
  [GAEvent.PACKAGE_CONTEXT_MENU_DETAILS_CLICK]: NoParams;
  [GAEvent.PACKAGE_CONTEXT_MENU_KOTLIN_SNIPPET_CLICK]: NoParams;
  [GAEvent.PACKAGE_CONTEXT_MENU_GROOVY_SNIPPET_CLICK]: NoParams;

  [GAEvent.PACKAGE_IMPORT_TAB_CLICK]: { eventCategory: string };
  [GAEvent.PACKAGE_COPY_SNIPPET]: NoParams;

  [GAEvent.KOTLIN_ECOSYSTEM_DROPDOWN_CLICK]: NoParams;

  [GAEvent.PACKAGE_CARD_CLICK]: { eventCategory: string };
  [GAEvent.PROJECT_CARD_CLICK]: { eventCategory: string };

  [GAEvent.SEARCH_KEYBOARD_TRIGGER]: NoParams;
};

export function trackEvent<E extends GAEvent>(event: E, params: GAEventParams[E]) {
  if (typeof window !== "undefined" && window.dataLayer) {
    window.dataLayer.push({
      event: "GAEvent",
      eventAction: event,
      ...(params || {}),
    });
  }
}

