import type { RoadmapStage, Stat, TopRatedUser, UpcomingContest } from "./types";

export const SAMPLE_STATS: Stat[] = [
    { value: 1806, label: "bài tập có máy chấm" },
    { value: 36, label: "người đang học" },
    { value: 100000, label: "bài nộp đã chấm" },
    { value: "Thứ 7 hằng tuần", label: "contest SQL Weekly" },
];

export const SAMPLE_UPCOMING_CONTESTS: UpcomingContest[] = [
    {
        title: "SQL Weekly #38",
        startLabel: "Thứ 7 · 19:30",
        problemCount: 4,
        durationMinutes: 90,
        note: "Rated",
    },
    {
        title: "Window Function Cup",
        startLabel: "CN · 09:00",
        problemCount: 5,
        durationMinutes: 120,
        note: "Rated",
    },
    {
        title: "Tân binh #12",
        startLabel: "T2 · 20:00",
        problemCount: 3,
        durationMinutes: 60,
        note: "Cho người mới",
    },
];

export const SAMPLE_TOP_RATED_USERS: TopRatedUser[] = [
    { username: "quang.le", rating: 2184 },
    { username: "trang.hoang", rating: 2097 },
    { username: "phuc.dang", rating: 1985 },
    { username: "ngoc.vu", rating: 1942 },
    { username: "khoa.tran", rating: 1903 },
];

export const ROADMAP_STAGES: RoadmapStage[] = [
    { title: "Cơ bản", topics: "SELECT, WHERE, ORDER BY", level: "FOUNDATION" },
    { title: "JOIN", topics: "INNER, LEFT, SELF JOIN", level: "FOUNDATION" },
    { title: "Aggregate", topics: "GROUP BY, HAVING", level: "FOUNDATION" },
    { title: "Subquery", topics: "Tương quan, CTE", level: "ADVANCED" },
    { title: "Window Function", topics: "RANK, LAG, khung cửa sổ", level: "ADVANCED" },
    { title: "Tối ưu", topics: "Index, EXPLAIN", level: "ADVANCED" },
];
