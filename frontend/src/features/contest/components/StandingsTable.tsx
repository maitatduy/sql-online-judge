import { formatNumber } from "@/lib/format";

import type { StandingsPage } from "../types";
import { RatingDelta } from "./RatingDelta";
import { StandingCellView } from "./StandingCellView";

const HEADER_CLASS = "border-b px-3 py-2.5 text-sm font-semibold text-muted-foreground";

interface StandingsTableProps {
    standings: StandingsPage;
    ratingDeltas: Map<string, number>;
}

export function StandingsTable({ standings, ratingDeltas }: StandingsTableProps) {
    const hasRatings = ratingDeltas.size > 0;

    return (
        <div className="overflow-x-auto rounded-lg border bg-card">
            <table aria-label="Bảng xếp hạng" className="w-full min-w-[560px] border-collapse">
                <thead>
                    <tr>
                        <th scope="col" className={`${HEADER_CLASS} w-16 text-center`}>
                            Hạng
                        </th>
                        <th scope="col" className={`${HEADER_CLASS} text-left`}>
                            Người thi
                        </th>
                        <th scope="col" className={`${HEADER_CLASS} w-16 text-center`}>
                            Giải
                        </th>
                        <th scope="col" className={`${HEADER_CLASS} w-20 text-center`}>
                            Phạt
                        </th>
                        {hasRatings ? (
                            <th scope="col" className={`${HEADER_CLASS} w-20 text-center`}>
                                Rating
                            </th>
                        ) : null}
                        {standings.problems.map((letter) => (
                            <th key={letter} scope="col" className={`${HEADER_CLASS} text-center`}>
                                {letter}
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {standings.items.map((item) => (
                        <tr key={item.username}>
                            <td className="border-b px-3 py-1.5 text-center text-lg font-extrabold">
                                {item.rank}
                            </td>
                            <td className="border-b px-3 py-1.5">
                                <p className="font-semibold">{item.username}</p>
                                {item.displayName ? (
                                    <p className="text-sm text-muted-foreground">
                                        {item.displayName}
                                    </p>
                                ) : null}
                            </td>
                            <td className="border-b px-3 py-1.5 text-center text-lg font-extrabold">
                                {item.solved}
                            </td>
                            <td className="border-b px-3 py-1.5 text-center text-muted-foreground">
                                {formatNumber(item.penaltyMinutes)}
                            </td>
                            {hasRatings ? (
                                <td className="border-b px-3 py-1.5 text-center">
                                    {ratingDeltas.has(item.username) ? (
                                        <RatingDelta delta={ratingDeltas.get(item.username) ?? 0} />
                                    ) : null}
                                </td>
                            ) : null}
                            {item.cells.map((cell) => (
                                <StandingCellView key={cell.letter} cell={cell} />
                            ))}
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
