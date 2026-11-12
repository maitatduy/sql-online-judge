import { type APIRequestContext, expect, type Page } from "@playwright/test";

const API_URL = process.env.E2E_API_URL ?? "http://localhost:8080";
const MAILPIT_URL = process.env.E2E_MAILPIT_URL ?? "http://localhost:8025";
const VERIFICATION_CODE = /là: (\d{6})/;

export interface Learner {
    email: string;
    password: string;
}

interface MailpitSearch {
    messages: { Snippet: string }[];
}

export async function registerVerifiedLearner(request: APIRequestContext): Promise<Learner> {
    const suffix = `${Date.now()}${Math.floor(Math.random() * 1000)}`;
    const learner = { email: `e2e.${suffix}@sqljudge.test`, password: "Hoc-SQL-2026-tot!" };

    const registered = await request.post(`${API_URL}/api/auth/register`, {
        data: { username: `e2e_${suffix}`, acceptedTerms: true, ...learner },
    });
    expect(registered.status()).toBe(201);

    const code = await verificationCodeOf(request, learner.email);
    const confirmed = await request.post(`${API_URL}/api/auth/email-verification/confirm`, {
        data: { email: learner.email, code },
    });
    expect(confirmed.status()).toBe(200);
    return learner;
}

export async function logIn(page: Page, learner: Learner, nextPath: string): Promise<void> {
    await page.goto(`/login?next=${encodeURIComponent(nextPath)}`);
    await page.getByLabel("Email").fill(learner.email);
    await page.getByLabel("Mật khẩu", { exact: true }).fill(learner.password);
    await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
    await expect(page).toHaveURL(nextPath, { timeout: 30_000 });
}

async function verificationCodeOf(request: APIRequestContext, email: string): Promise<string> {
    let code: string | undefined;
    await expect
        .poll(async () => {
            const response = await request.get(`${MAILPIT_URL}/api/v1/search`, {
                params: { query: `to:${email}` },
            });
            const search = (await response.json()) as MailpitSearch;
            code = search.messages
                .map((message) => VERIFICATION_CODE.exec(message.Snippet)?.[1])
                .find((match) => match !== undefined);
            return code;
        })
        .toBeDefined();
    return code as string;
}
