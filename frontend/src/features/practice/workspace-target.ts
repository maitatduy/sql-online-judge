import type { ReactNode } from "react";

import type {
    RunAccepted,
    StartRunRequest,
    SubmissionAccepted,
    SubmitSolutionRequest,
} from "./types";

export interface WorkspaceExtraTab {
    value: string;
    label: string;
    content: ReactNode;
}

export interface WorkspaceTarget {
    storageKey: string;
    heading: string;
    loginPath: string;
    historyProblemId: string | null;
    statementMeta?: ReactNode;
    extraTabs?: WorkspaceExtraTab[];
    startRun: (request: StartRunRequest) => Promise<RunAccepted>;
    submit: (request: SubmitSolutionRequest, idempotencyKey: string) => Promise<SubmissionAccepted>;
}
