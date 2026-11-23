"use client";

import { useRatingChanges } from "../hooks/useContests";
import { RatingDelta } from "./RatingDelta";

const HEADER_CLASS = "border-b px-4 py-2.5 text-left text-sm font-semibold text-muted-foreground";
const CELL_CLASS = "border-b px-4 py-3";

interface RatingChangesTableProps {
    slug: string;
}

export function RatingChangesTable({ slug }: RatingChangesTableProps) {
    const { data: changes } = useRatingChanges(slug);

    if (!changes || changes.length === 0) {
        return null;
    }
    return (
        <section className="flex flex-col gap-3">
            <h2 className="text-lg font-bold">Thay đổi rating</h2>
            <div className="overflow-x-auto rounded-lg border bg-card">
                <table
                    aria-label="Thay đổi rating"
                    className="w-full min-w-[420px] border-collapse"
                >
                    <thead>
                        <tr>
                            <th scope="col" className={`${HEADER_CLASS} w-16`}>
                                Hạng
                            </th>
                            <th scope="col" className={HEADER_CLASS}>
                                Người thi
                            </th>
                            <th scope="col" className={HEADER_CLASS}>
                                Rating
                            </th>
                            <th scope="col" className={HEADER_CLASS}>
                                Thay đổi
                            </th>
                        </tr>
                    </thead>
                    <tbody>
                        {changes.map((change) => (
                            <tr key={change.username}>
                                <td className={`${CELL_CLASS} font-extrabold`}>{change.rank}</td>
                                <td className={`${CELL_CLASS} font-semibold`}>{change.username}</td>
                                <td className={`${CELL_CLASS} text-muted-foreground`}>
                                    {`${change.oldRating} → ${change.newRating}`}
                                </td>
                                <td className={CELL_CLASS}>
                                    <RatingDelta delta={change.delta} />
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </section>
    );
}
