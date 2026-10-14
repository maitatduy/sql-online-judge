import type { ContestResult } from "@/features/admin/users/types";
import { formatNumber } from "@/lib/format";

import { RatingDelta } from "./RatingDelta";
import { BODY_CELL_CLASS, HEADER_CELL_CLASS } from "./table-cells";

const NUMBER_CELL_CLASS = `${BODY_CELL_CLASS} text-right font-mono`;
const NUMBER_HEADER_CLASS = `${HEADER_CELL_CLASS} text-right`;

interface ContestTableProps {
    results: ContestResult[];
}

export function ContestTable({ results }: ContestTableProps) {
    return (
        <div className="-mx-5 overflow-x-auto">
            <table className="w-full min-w-[420px] border-collapse text-[13px]">
                <thead>
                    <tr>
                        <th scope="col" className={`${HEADER_CELL_CLASS} pl-5`}>
                            Contest
                        </th>
                        <th scope="col" className={NUMBER_HEADER_CLASS}>
                            Hạng
                        </th>
                        <th scope="col" className={NUMBER_HEADER_CLASS}>
                            Bài
                        </th>
                        <th scope="col" className={`${NUMBER_HEADER_CLASS} pr-5`}>
                            Rating
                        </th>
                    </tr>
                </thead>
                <tbody>
                    {results.map((result) => (
                        <tr key={result.contestSlug}>
                            <th
                                scope="row"
                                className={`${BODY_CELL_CLASS} pl-5 text-left font-semibold`}
                            >
                                {result.contestTitle}
                            </th>
                            <td className={NUMBER_CELL_CLASS}>{formatNumber(result.rank)}</td>
                            <td className={NUMBER_CELL_CLASS}>
                                {formatNumber(result.solvedCount)}
                            </td>
                            <td className={`${NUMBER_CELL_CLASS} pr-5`}>
                                <RatingDelta delta={result.ratingDelta} />
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
