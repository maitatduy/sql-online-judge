import { AUTH_CONTENT } from "../content";
import type { AuthMode } from "../types";

import { SchemaIllustration } from "./SchemaIllustration";

interface AuthIntroProps {
    mode: AuthMode;
}

export function AuthIntro({ mode }: AuthIntroProps) {
    const content = AUTH_CONTENT[mode];

    return (
        <section
            aria-label="Giới thiệu"
            className="flex w-full max-w-[460px] min-w-0 flex-col gap-7 lg:max-w-[600px]"
        >
            <div>
                <p className="mb-3.5 font-mono text-[13px] font-semibold text-syntax-function">
                    -- {content.eyebrow}
                </p>
                <h1 className="text-[40px] leading-[1.04] font-extrabold tracking-[-0.035em] md:text-[56px]">
                    Viết. Chạy. <span className="block text-verdict-ac">Accepted.</span>
                </h1>
                <p className="mt-4.5 hidden max-w-[460px] text-[17px] leading-[1.6] text-muted-foreground md:block">
                    {content.lead}
                </p>
            </div>
            <SchemaIllustration sampleLearnerName={content.sampleLearnerName} />
        </section>
    );
}
