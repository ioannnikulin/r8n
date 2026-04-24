import { defineConfig } from "@playwright/test";

const uiUse = {
  baseURL: "http://127.0.0.1:4173",
};

export default defineConfig({
  retries: process.env.CI ? 2 : 0,
  use: {
    trace: "on-first-retry",
  },
  projects: [
    {
      name: "ui-chromium",
      testDir: "./e2e/ui",
      use: {
        ...uiUse,
        browserName: "chromium",
      },
    },
    {
      name: "ui-firefox",
      testDir: "./e2e/ui",
      use: {
        ...uiUse,
        browserName: "firefox",
      },
    },
    {
      name: "ui-webkit",
      testDir: "./e2e/ui",
      use: {
        ...uiUse,
        browserName: "webkit",
      },
    },
    {
      name: "api",
      testDir: "./e2e/api",
      use: {
        baseURL: "https://127.0.0.1:8080",
        ignoreHTTPSErrors: true,
      },
    },
  ],
  webServer: {
    command:
      "VITE_E2E_BYPASS_AUTH=true npm run dev -- --host 127.0.0.1 --port 4173",
    port: 4173,
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
});
