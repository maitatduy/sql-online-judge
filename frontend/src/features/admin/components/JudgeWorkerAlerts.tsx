import { TriangleAlert } from "lucide-react";

import { Button } from "@/components/ui/button";

import { formatVietnamTime } from "../format";
import type { JudgeWorker } from "../types";

const ALERT_CLASS = "mx-4.5 mt-3.5 flex items-center gap-3 rounded-lg border px-3.5 py-3 text-sm";

interface JudgeWorkerAlertsProps {
    workers: JudgeWorker[];
    onRestart: (workerName: string) => void;
}

interface WorkerAlertProps {
    worker: JudgeWorker;
    onRestart: (workerName: string) => void;
}

function DisconnectedAlert({ worker, onRestart }: WorkerAlertProps) {
    return (
        <div role="alert" className={`${ALERT_CLASS} border-status-down/30 bg-status-down/8`}>
            <TriangleAlert aria-hidden="true" className="size-4.5 shrink-0 text-status-down" />
            <span className="flex-1">
                <strong className="font-mono">{worker.name}</strong> mất kết nối
                {worker.disconnectedAt ? ` từ ${formatVietnamTime(worker.disconnectedAt)}` : ""}
            </span>
            <Button
                type="button"
                size="sm"
                variant="destructive"
                aria-label={`Khởi động lại ${worker.name}`}
                onClick={() => onRestart(worker.name)}
            >
                Khởi động lại
            </Button>
        </div>
    );
}

function RestartingNotice({ worker }: { worker: JudgeWorker }) {
    return (
        <div role="status" className={`${ALERT_CLASS} border-status-busy/30 bg-status-busy/12`}>
            <span aria-hidden="true" className="size-2 shrink-0 rounded-full bg-status-busy" />
            <span className="flex-1">
                <span>
                    Đang khởi động lại <strong className="font-mono">{worker.name}</strong>
                </span>
                <span className="mt-0.5 block text-xs text-muted-foreground">
                    Thường mất khoảng 30 giây
                </span>
            </span>
        </div>
    );
}

export function JudgeWorkerAlerts({ workers, onRestart }: JudgeWorkerAlertsProps) {
    return (
        <>
            {workers.map((worker) => {
                if (worker.status === "DISCONNECTED") {
                    return (
                        <DisconnectedAlert
                            key={worker.name}
                            worker={worker}
                            onRestart={onRestart}
                        />
                    );
                }
                return worker.status === "RESTARTING" ? (
                    <RestartingNotice key={worker.name} worker={worker} />
                ) : null;
            })}
        </>
    );
}
