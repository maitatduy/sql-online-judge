import type { ReactNode } from "react";

import type { BreadcrumbItem } from "@/components/shared/Breadcrumb";
import { ROUTES } from "@/lib/routes";

import type {
    RunAccepted,
    StartRunRequest,
    SubmissionAccepted,
    SubmitSolutionRequest,
} from "./types";

export interface WorkspaceExtraTab {
    value: string;
    label: string;
    icon: ReactNode;
    content: ReactNode;
}

export const PRACTICE_PARENT: BreadcrumbItem = { label: "Luyện tập", href: ROUTES.problems };

export interface WorkspaceTarget {
    storageKey: string;
    heading: string;
    parent: BreadcrumbItem;
    loginPath: string;
    historyProblemId: string | null;
    statementMeta?: ReactNode;
    extraTabs?: WorkspaceExtraTab[];
    startRun: (request: StartRunRequest) => Promise<RunAccepted>;
    submit: (request: SubmitSolutionRequest, idempotencyKey: string) => Promise<SubmissionAccepted>;
}
