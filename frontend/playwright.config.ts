import { defineConfig } from "@playwright/test";

export default defineConfig({
  retries: process.env.CI ? 2 : 0,
  use: {
    trace: "on-first-retry",
  },
  projects: [
    {
      name: "ui",
      testDir: "./e2e/ui",
      use: {
        baseURL: "http://127.0.0.1:4173",
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
    command: "npm run dev -- --host 127.0.0.1 --port 4173",
    port: 4173,
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
});
