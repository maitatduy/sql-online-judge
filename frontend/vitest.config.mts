import { fileURLToPath } from "node:url";

import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

export default defineConfig({
    plugins: [react()],
    resolve: {
        alias: {
            "@": fileURLToPath(new URL("./src", import.meta.url)),
        },
    },
    test: {
        environment: "jsdom",
        pool: "vmThreads",
        maxWorkers: 2,
        testTimeout: 30000,
        setupFiles: ["./vitest.polyfills.ts", "./vitest.setup.ts"],
        include: ["src/**/*.test.{ts,tsx}"],
        coverage: {
            provider: "v8",
            include: ["src/lib/**", "src/features/*/hooks/**"],
            exclude: ["**/*.test.{ts,tsx}"],
            thresholds: {
                lines: 80,
            },
        },
    },
});
