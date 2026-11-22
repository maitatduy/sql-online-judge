import type { ReactNode } from "react";

import type {
    RunAccepted,
    StartRunRequest,
    SubmissionAccepted,
    SubmitSolutionRequest,
} from "./types";

export interface WorkspaceTarget {
    storageKey: string;
    heading: string;
    loginPath: string;
    historyProblemId: string | null;
    statementMeta?: ReactNode;
    startRun: (request: StartRunRequest) => Promise<RunAccepted>;
    submit: (request: SubmitSolutionRequest, idempotencyKey: string) => Promise<SubmissionAccepted>;
}
