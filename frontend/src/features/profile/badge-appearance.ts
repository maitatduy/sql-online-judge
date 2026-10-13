import { Award, Blend, CircleCheck, Flame, Trophy, type LucideIcon } from "lucide-react";

export interface BadgeAppearance {
    icon: LucideIcon;
    className: string;
    milestone?: string;
}

const SOLVED_MILESTONE_PATTERN = /^SOLVED_(\d+)$/;

const STREAK: BadgeAppearance = { icon: Flame, className: "bg-streak/12 text-streak" };
const JOIN: BadgeAppearance = { icon: Blend, className: "bg-brand-soft text-brand" };
const CONTEST: BadgeAppearance = {
    icon: Trophy,
    className: "bg-badge-contest/12 text-badge-contest",
};
const SOLVED: BadgeAppearance = {
    icon: CircleCheck,
    className: "bg-badge-solved/12 text-badge-solved",
};
const DEFAULT_APPEARANCE: BadgeAppearance = {
    icon: Award,
    className: "bg-surface-2 text-muted-foreground",
};

const APPEARANCE_BY_CODE_PREFIX: [string, BadgeAppearance][] = [
    ["STREAK_", STREAK],
    ["JOIN_", JOIN],
    ["CONTEST_", CONTEST],
    ["FIRST_CONTEST", CONTEST],
    ["SOLVED_", SOLVED],
    ["FIRST_AC", SOLVED],
];

export function badgeAppearance(code: string): BadgeAppearance {
    const match = APPEARANCE_BY_CODE_PREFIX.find(([prefix]) => code.startsWith(prefix));
    const appearance = match ? match[1] : DEFAULT_APPEARANCE;
    const milestone = SOLVED_MILESTONE_PATTERN.exec(code)?.[1];
    return milestone ? { ...appearance, milestone } : appearance;
}
