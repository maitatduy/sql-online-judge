export interface Stat {
    value: number | string;
    label: string;
}

export interface UpcomingContest {
    title: string;
    startLabel: string;
    problemCount: number;
    durationMinutes: number;
    note: string;
}

export interface TopRatedUser {
    username: string;
    rating: number;
}

export type RoadmapLevel = "FOUNDATION" | "ADVANCED";

export interface RoadmapStage {
    title: string;
    topics: string;
    level: RoadmapLevel;
}
