import { request, type FullConfig } from "@playwright/test";

import { MOCK_LEARNER } from "./auth";

const WARM_UP_PATHS = ["/", "/login", `/u/${MOCK_LEARNER.username}`];
const WARM_UP_TIMEOUT_MS = 180_000;

export default async function warmUpDevServer(config: FullConfig): Promise<void> {
    const context = await request.newContext({ baseURL: config.projects[0]?.use.baseURL });
    try {
        for (const path of WARM_UP_PATHS) {
            await context.get(path, { timeout: WARM_UP_TIMEOUT_MS });
        }
    } finally {
        await context.dispose();
    }
}
