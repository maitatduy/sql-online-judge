import { describe, expect, it } from "vitest";

import type { WorkerStatus } from "./types";
import {
    countActiveWorkers,
    cpuLevel,
    findRecoveredWorkers,
    workersRefreshMs,
} from "./worker-status";

describe("cpuLevel", () => {
    it.each([
        [0, "normal"],
        [59, "normal"],
        [60, "high"],
        [84, "high"],
        [85, "critical"],
        [100, "critical"],
    ] as const)("CPU %i%% ở mức %s", (cpuPercent, level) => {
        expect(cpuLevel(cpuPercent)).toBe(level);
    });
});

describe("countActiveWorkers", () => {
    it("tính worker online và đang bận là đang hoạt động", () => {
        expect(countActiveWorkers(["ONLINE", "BUSY", "DISCONNECTED", "RESTARTING"])).toBe(2);
    });
});

describe("findRecoveredWorkers", () => {
    const worker = (name: string, status: WorkerStatus) => ({ name, status });

    it("trả về worker vừa hoạt động lại sau khi khởi động lại", () => {
        expect(
            findRecoveredWorkers(
                [worker("judge-my-02", "RESTARTING"), worker("judge-pg-01", "BUSY")],
                [worker("judge-my-02", "ONLINE"), worker("judge-pg-01", "ONLINE")],
            ),
        ).toEqual(["judge-my-02"]);
    });

    it("tính cả worker bận ngay sau khi khởi động lại", () => {
        expect(
            findRecoveredWorkers(
                [worker("judge-my-02", "RESTARTING")],
                [worker("judge-my-02", "BUSY")],
            ),
        ).toEqual(["judge-my-02"]);
    });

    it("bỏ qua worker còn đang khởi động lại hoặc lại mất kết nối", () => {
        expect(
            findRecoveredWorkers(
                [worker("judge-my-01", "RESTARTING"), worker("judge-my-02", "RESTARTING")],
                [worker("judge-my-01", "RESTARTING"), worker("judge-my-02", "DISCONNECTED")],
            ),
        ).toEqual([]);
    });

    it("bỏ qua worker tự kết nối lại mà không qua khởi động lại", () => {
        expect(
            findRecoveredWorkers(
                [worker("judge-my-02", "DISCONNECTED")],
                [worker("judge-my-02", "ONLINE")],
            ),
        ).toEqual([]);
    });
});

describe("workersRefreshMs", () => {
    it("làm mới mỗi 30 giây khi không có worker nào đang khởi động lại", () => {
        expect(workersRefreshMs(["ONLINE", "BUSY", "DISCONNECTED"])).toBe(30_000);
        expect(workersRefreshMs([])).toBe(30_000);
    });

    it("làm mới mỗi 2 giây khi có worker đang khởi động lại", () => {
        expect(workersRefreshMs(["ONLINE", "RESTARTING"])).toBe(2000);
    });
});
