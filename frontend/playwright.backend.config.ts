import { defineConfig, devices } from "@playwright/test";

const FRONTEND_PORT = 3000;
const baseURL = `http://localhost:${FRONTEND_PORT}`;
const apiURL = process.env.E2E_API_URL ?? "http://localhost:8080";

export default defineConfig({
    testDir: "./e2e/backend",
    workers: 1,
    timeout: 60_000,
    expect: { timeout: 15_000 },
    reporter: process.env.CI ? [["github"], ["list"]] : "list",
    use: {
        baseURL,
        trace: "retain-on-failure",
        screenshot: "only-on-failure",
        ...devices["Desktop Chrome"],
        viewport: { width: 1440, height: 900 },
    },
    webServer: {
        command: `npm run dev -- --port ${FRONTEND_PORT}`,
        url: baseURL,
        reuseExistingServer: true,
        timeout: 300_000,
        env: {
            NEXT_PUBLIC_API_MOCKING: "disabled",
            NEXT_PUBLIC_API_URL: apiURL,
            NEXT_DIST_DIR: ".next-e2e-backend",
        },
    },
});
