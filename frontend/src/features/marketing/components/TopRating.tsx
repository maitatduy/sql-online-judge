import { Card } from "@/components/ui/card";
import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/utils";

import type { TopRatedUser } from "../types";

interface TopRatingProps {
    users: TopRatedUser[];
}

function getInitials(username: string): string {
    return username
        .split(".")
        .map((part) => part.charAt(0).toUpperCase())
        .join("");
}

export function TopRating({ users }: TopRatingProps) {
    return (
        <section className="min-w-0 flex-[1_1_320px]">
            <h2 id="top-rating-title" className="mb-5 text-[28px] font-extrabold">
                Top rating
            </h2>
            <Card className="gap-0 overflow-hidden py-0">
                <table
                    aria-labelledby="top-rating-title"
                    className="w-full border-collapse text-[13px]"
                >
                    <thead className="sr-only">
                        <tr>
                            <th scope="col">Hạng</th>
                            <th scope="col">Người dùng</th>
                            <th scope="col">Rating</th>
                        </tr>
                    </thead>
                    <tbody>
                        {users.map((user, index) => (
                            <tr key={user.username} className="border-b last:border-b-0">
                                <td
                                    className={cn(
                                        "w-9 px-3 py-2.5 font-extrabold",
                                        index > 0 && "text-muted-foreground",
                                    )}
                                >
                                    {index + 1}
                                </td>
                                <td className="px-3 py-2.5">
                                    <span className="flex items-center gap-2.5">
                                        <span
                                            aria-hidden="true"
                                            className="inline-flex size-7 shrink-0 items-center justify-center rounded-full bg-surface-2 text-[11px] font-semibold"
                                        >
                                            {getInitials(user.username)}
                                        </span>
                                        {user.username}
                                    </span>
                                </td>
                                <td className="px-3 py-2.5 text-right font-mono">
                                    {formatNumber(user.rating)}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </Card>
        </section>
    );
}
