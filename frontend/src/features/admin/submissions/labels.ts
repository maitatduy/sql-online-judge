import type { AdminSubmission } from "./types";

export function problemLabel(submission: Pick<AdminSubmission, "problemNumber" | "problemTitle">) {
    const title = submission.problemTitle ?? "Bài không rõ tên";
    return submission.problemNumber ? `${submission.problemNumber}. ${title}` : title;
}
