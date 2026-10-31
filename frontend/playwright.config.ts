import { defineConfig, devices } from "@playwright/test";

const E2E_PORT = 3100;
const baseURL = `http://localhost:${E2E_PORT}`;

export default defineConfig({
    testDir: "./e2e",
    testIgnore: "backend/**",
    globalSetup: "./e2e/support/global-setup.ts",
    workers: 1,
    timeout: 60_000,
    expect: { timeout: 15_000 },
    reporter: process.env.CI ? [["github"], ["list"]] : "list",
    use: {
        baseURL,
        trace: "retain-on-failure",
        screenshot: "only-on-failure",
    },
    projects: [
        {
            name: "desktop",
            use: { ...devices["Desktop Chrome"], viewport: { width: 1440, height: 900 } },
        },
        {
            name: "mobile",
            use: { ...devices["Pixel 7"], viewport: { width: 390, height: 844 } },
        },
    ],
    webServer: {
        command: `npm run dev -- --port ${E2E_PORT}`,
        url: baseURL,
        reuseExistingServer: true,
        timeout: 300_000,
        env: { NEXT_PUBLIC_API_MOCKING: "enabled", NEXT_DIST_DIR: ".next-e2e" },
    },
});
