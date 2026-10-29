import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";

import { server } from "@/mocks/server";

import { createSseParser, type ServerSentEvent, streamEvents } from "./sse";

function parseAll(chunks: string[]): ServerSentEvent[] {
    const events: ServerSentEvent[] = [];
    const parse = createSseParser((event) => events.push(event));
    chunks.forEach(parse);
    return events;
}

describe("createSseParser", () => {
    it("tách sự kiện theo dòng trống và đọc tên sự kiện", () => {
        expect(
            parseAll(['event: progress\ndata: {"n":1}\n\nevent: completed\ndata: {}\n\n']),
        ).toEqual([
            { event: "progress", data: '{"n":1}' },
            { event: "completed", data: "{}" },
        ]);
    });

    it("ghép sự kiện bị cắt giữa hai lần nhận dữ liệu", () => {
        expect(parseAll(["event: prog", "ress\nda", "ta: 1\n", "\n"])).toEqual([
            { event: "progress", data: "1" },
        ]);
    });

    it("nối nhiều dòng data, chấp nhận CRLF, bỏ qua comment và dùng tên mặc định", () => {
        expect(parseAll([": keep-alive\r\ndata: a\r\ndata: b\r\n\r\n"])).toEqual([
            { event: "message", data: "a\nb" },
        ]);
    });

    it("không phát sự kiện chưa có data", () => {
        expect(parseAll(["event: progress\n\n"])).toEqual([]);
    });
});

describe("streamEvents", () => {
    it("đọc lần lượt các sự kiện từ luồng text/event-stream", async () => {
        server.use(
            http.get("*/api/stream", () => {
                const encoder = new TextEncoder();
                const body = new ReadableStream({
                    start(controller) {
                        controller.enqueue(encoder.encode("event: progress\ndata: 1\n\n"));
                        controller.enqueue(encoder.encode("event: completed\ndata: 2\n\n"));
                        controller.close();
                    },
                });
                return new HttpResponse(body, {
                    headers: { "Content-Type": "text/event-stream" },
                });
            }),
        );
        const events: ServerSentEvent[] = [];

        await streamEvents("/api/stream", { onEvent: (event) => events.push(event) });

        expect(events).toEqual([
            { event: "progress", data: "1" },
            { event: "completed", data: "2" },
        ]);
    });

    it("ném lỗi API khi máy chủ từ chối mở luồng", async () => {
        server.use(
            http.get("*/api/stream", () =>
                HttpResponse.json(
                    { status: 404, code: "SUBMISSION_NOT_FOUND", message: "Không tìm thấy" },
                    { status: 404 },
                ),
            ),
        );

        await expect(
            streamEvents("/api/stream", { onEvent: () => undefined }),
        ).rejects.toMatchObject({ status: 404, code: "SUBMISSION_NOT_FOUND" });
    });
});
