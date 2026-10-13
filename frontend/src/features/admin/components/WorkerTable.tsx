import { formatNumber } from "@/lib/format";

import type { JudgeWorker } from "../types";
import { CpuMeter } from "./CpuMeter";
import { WorkerStatusLabel } from "./WorkerStatusLabel";

const HEADER_CELL_CLASS =
    "border-b bg-surface px-3 py-2.25 text-left text-xs font-semibold whitespace-nowrap text-muted-foreground";
const BODY_CELL_CLASS = "border-b px-3 py-2.25 align-middle";

interface WorkerTableProps {
    workers: JudgeWorker[];
}

export function WorkerTable({ workers }: WorkerTableProps) {
    return (
        <div
            role="region"
            aria-label="Danh sách worker"
            tabIndex={0}
            className="overflow-x-auto pt-2.5"
        >
            <table className="w-full min-w-[460px] border-collapse text-[13px]">
                <thead>
                    <tr>
                        <th scope="col" className={HEADER_CELL_CLASS}>
                            Worker
                        </th>
                        <th scope="col" className={HEADER_CELL_CLASS}>
                            Trạng thái
                        </th>
                        <th scope="col" className={`${HEADER_CELL_CLASS} text-right`}>
                            Đã chấm/giờ
                        </th>
                        <th scope="col" className={`${HEADER_CELL_CLASS} w-35`}>
                            CPU
                        </th>
                    </tr>
                </thead>
                <tbody>
                    {workers.map((worker) => (
                        <tr key={worker.name}>
                            <th
                                scope="row"
                                className={`${BODY_CELL_CLASS} text-left font-mono font-semibold`}
                            >
                                {worker.name}
                            </th>
                            <td className={BODY_CELL_CLASS}>
                                <WorkerStatusLabel status={worker.status} />
                            </td>
                            <td className={`${BODY_CELL_CLASS} text-right font-mono`}>
                                {formatNumber(worker.judgedPerHour)}
                            </td>
                            <td className={BODY_CELL_CLASS}>
                                <CpuMeter cpuPercent={worker.cpuPercent} />
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
