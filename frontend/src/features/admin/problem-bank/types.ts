import type { Page } from "@/lib/pagination";

import type { ReviewProblemSummary } from "../problem-reviews/types";

export interface BankTag {
    slug: string;
    name: string;
}

export interface BankProblem extends ReviewProblemSummary {
    authorUsername: string | null;
    tags: BankTag[];
    submissionCount: number;
    acceptedCount: number;
    acceptanceRate: number;
}

export type BankProblemPage = Page<BankProblem>;

export interface ProblemBankSummary {
    publicCount: number;
    contestOnlyCount: number;
    hiddenCount: number;
    lowAcceptanceCount: number;
}
