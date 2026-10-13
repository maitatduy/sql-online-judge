import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/utils";

import { formatDateKey, vietnamDateKey } from "../activity-heatmap";
import { badgeAppearance } from "../badge-appearance";
import type { ProfileBadge } from "../types";

interface BadgeGridProps {
    badges: ProfileBadge[];
}

function BadgeTile({ badge }: { badge: ProfileBadge }) {
    const { icon: Icon, className, milestone } = badgeAppearance(badge.code);

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <button
                    type="button"
                    aria-label={badge.name}
                    className={cn(
                        "flex aspect-square w-full items-center justify-center rounded-[10px] transition-transform hover:scale-105",
                        className,
                    )}
                >
                    {milestone ? (
                        <span className="font-mono text-sm font-extrabold">{milestone}</span>
                    ) : (
                        <Icon aria-hidden="true" className="size-5.5" />
                    )}
                </button>
            </TooltipTrigger>
            <TooltipContent>
                <p className="font-semibold">{badge.name}</p>
                <p className="opacity-80">
                    Đạt ngày {formatDateKey(vietnamDateKey(new Date(badge.earnedAt)))}
                </p>
            </TooltipContent>
        </Tooltip>
    );
}

export function BadgeGrid({ badges }: BadgeGridProps) {
    return (
        <section className="border-t pt-4">
            <h2 className="mb-2.5 text-[11px] font-semibold tracking-[0.06em] text-muted-foreground uppercase">
                Huy hiệu · {formatNumber(badges.length)}
            </h2>
            {badges.length === 0 ? (
                <p className="text-[13px] text-muted-foreground">Chưa có huy hiệu nào</p>
            ) : (
                <ul aria-label="Huy hiệu" className="grid grid-cols-4 gap-2">
                    {badges.map((badge) => (
                        <li key={badge.code}>
                            <BadgeTile badge={badge} />
                        </li>
                    ))}
                </ul>
            )}
        </section>
    );
}
