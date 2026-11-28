import Link from "next/link";

import { Avatar } from "@/components/shared/Avatar";
import { formatNumber } from "@/lib/format";
import { ROUTES } from "@/lib/routes";
import { cn } from "@/lib/utils";

import type { LeaderboardEntry } from "../api";

const PODIUM_SIZE = 3;
const HEADER_CLASS = "px-4 py-3 font-semibold";
const NUMBER_HEADER_CLASS = `${HEADER_CLASS} text-right`;

interface LeaderboardTableProps {
    entries: LeaderboardEntry[];
    viewerId: string | undefined;
}

function PlayerName({ entry }: { entry: LeaderboardEntry }) {
    const name = entry.displayName ?? entry.username ?? "Người dùng";
    return (
        <span className="flex items-center gap-2.5">
            <Avatar name={entry.username ?? name} />
            {entry.username ? (
                <Link
                    href={ROUTES.profile(entry.username)}
                    className="font-semibold hover:text-brand"
                >
                    {name}
                </Link>
            ) : (
                <span className="font-semibold">{name}</span>
            )}
        </span>
    );
}

export function LeaderboardTable({ entries, viewerId }: LeaderboardTableProps) {
    return (
        <table aria-label="Bảng xếp hạng rating" className="w-full min-w-[640px] text-[15px]">
            <thead>
                <tr className="border-b text-left text-sm text-muted-foreground">
                    <th scope="col" className={`${HEADER_CLASS} w-20`}>
                        Hạng
                    </th>
                    <th scope="col" className={HEADER_CLASS}>
                        Người thi
                    </th>
                    <th scope="col" className={HEADER_CLASS}>
                        Tên người dùng
                    </th>
                    <th scope="col" className={NUMBER_HEADER_CLASS}>
                        Rating
                    </th>
                    <th scope="col" className={NUMBER_HEADER_CLASS}>
                        Contest
                    </th>
                </tr>
            </thead>
            <tbody>
                {entries.map((entry) => {
                    const isViewer = entry.userId === viewerId;
                    return (
                        <tr
                            key={entry.userId}
                            aria-current={isViewer ? "true" : undefined}
                            className={cn(
                                "border-b last:border-b-0",
                                isViewer && "bg-brand-soft/60",
                            )}
                        >
                            <td
                                className={cn(
                                    "border-l-[3px] border-l-transparent px-4 py-3 font-bold tabular-nums",
                                    isViewer && "border-l-brand",
                                    entry.rank <= PODIUM_SIZE
                                        ? "text-lg text-brand"
                                        : "text-muted-foreground",
                                )}
                            >
                                {formatNumber(entry.rank)}
                            </td>
                            <td className="px-4 py-3">
                                <span className="flex flex-wrap items-center gap-2">
                                    <PlayerName entry={entry} />
                                    {isViewer ? (
                                        <span className="rounded-full bg-brand/12 px-2 py-0.5 text-[12px] font-semibold text-brand">
                                            Bạn
                                        </span>
                                    ) : null}
                                </span>
                            </td>
                            <td className="px-4 py-3 text-muted-foreground">
                                {entry.username ? `@${entry.username}` : "-"}
                            </td>
                            <td className="px-4 py-3 text-right text-lg font-extrabold tabular-nums">
                                {formatNumber(entry.rating)}
                            </td>
                            <td className="px-4 py-3 text-right text-muted-foreground tabular-nums">
                                {formatNumber(entry.contestsPlayed)}
                            </td>
                        </tr>
                    );
                })}
            </tbody>
        </table>
    );
}
