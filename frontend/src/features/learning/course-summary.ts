const POINT_PREFIX = "- ";

export interface CourseSummaryParts {
    intro: string;
    points: string[];
}

export function courseSummaryPartsOf(summary: string): CourseSummaryParts {
    const lines = summary
        .split("\n")
        .map((line) => line.trim())
        .filter((line) => line.length > 0);
    return {
        intro: lines.filter((line) => !line.startsWith(POINT_PREFIX)).join(" "),
        points: lines
            .filter((line) => line.startsWith(POINT_PREFIX))
            .map((line) => line.slice(POINT_PREFIX.length)),
    };
}
