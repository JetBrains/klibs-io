# klibs.io frontend

This is a Next.js project for building the frontend of https://klibs.io.

## Table of Contents
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
  - [Install Dependencies](#1-install-dependencies)
  - [Set Environment Variables](#set-environment-variables)
  - [Running the Development Server](#3-running-the-development-server)
  - [Running the Application with Docker](#4-running-the-application-with-docker)
- [End-to-End Testing with Playwright](#end-to-end-testing-with-playwright)
- [Running Linter and Formatter (Optional)](#6-running-linter-and-formatter-optional)

## Prerequisites
You need Node.js to run the project.
You can download it from the [official website](https://nodejs.org/).
Check if Node.js is installed by running:

  ```bash
  node -v
  ```

## Getting Started

### 1. Install Dependencies

Install all the required packages by running:

```bash
npm install
```

### 2. Set Environment Variables

This project uses environment variables to configure API URLs and other settings.
Follow these steps to set up your environment:

1. **Create an `.env.local` file** in the root of the project by copying the `.env.example` file:

   ```bash
   cp .env.example .env.local

2. Update the .env.local file with your environment-specific values:
    ```bash
    NEXT_PUBLIC_API_URL=https://your-api.com/api
    ```

### 3. Running the Development Server

To start the development server, use the following command:

```bash
npm run dev
```

This will start the Next.js development server, and you can view the application by visiting `http://localhost:3000` in your browser.

### 4. Running the Application with Docker

To run the application in a production environment using Docker, follow the steps below.

#### Prerequisites
1. Docker must be installed on your system. You can download it from the official website.

#### Set Environment Variables
Create .env.production based on .env.example.

#### Build the Docker Image
```bash
docker build -t klibs.io .
```

#### Run the Docker Container

```bash
docker run -p 3000:3000 klibs.io
```

## End-to-End Testing with Playwright

This project is configured with Playwright for E2E tests.

Scripts:
- Install browsers and dependencies (first run or CI):
  ```bash
  npm run pw:install
  ```
- Run all tests in headless mode:
  ```bash
  npm run test:e2e
  ```
- Run tests in UI mode:
  ```bash
  npm run test:e2e:ui
  ```
- Open the last HTML report:
  ```bash
  npm run test:e2e:report
  ```

Notes:
- Tests live in the `tests/` folder and are configured via `playwright.config.ts`.
- Artifacts are written to `test-results/` and the HTML report to `playwright-report/` (both are git-ignored).
- The current tests target public URLs (e.g., klibs.io, demo.playwright.dev). If you want to run against a local server, set `use.baseURL` or enable `webServer` in `playwright.config.ts`.

## 6. Running Linter and Formatter (Optional)

To check the code with ESLint:

```bash
npm run lint
```

To format the code with Prettier (if configured):

```bash
npm run format
```
