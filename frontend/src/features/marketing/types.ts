export interface LandingStat {
    label: string;
    value: number;
}

export interface LandingContest {
    id: string;
    slug: string;
    title: string;
    startAt: string;
    durationMinutes: number;
    isRated: boolean;
}

export interface LandingRatedUser {
    userId: string;
    username: string | null;
    displayName: string | null;
    rating: number;
}

export interface TotalCount {
    totalItems: number;
}

export interface LandingPage<T> {
    items: T[];
}
