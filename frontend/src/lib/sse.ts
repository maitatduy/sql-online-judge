import { apiClient } from "./api-client";

const DEFAULT_EVENT_NAME = "message";
const EVENT_SEPARATOR = /\r?\n\r?\n/;
const LINE_SEPARATOR = /\r?\n/;

export interface ServerSentEvent {
    event: string;
    data: string;
}

type OnEvent = (event: ServerSentEvent) => void;

interface StreamOptions {
    onEvent: OnEvent;
    signal?: AbortSignal;
}

function parseBlock(block: string): ServerSentEvent | null {
    let event = DEFAULT_EVENT_NAME;
    const dataLines: string[] = [];
    block.split(LINE_SEPARATOR).forEach((line) => {
        const separatorIndex = line.indexOf(":");
        if (separatorIndex <= 0) {
            return;
        }
        const field = line.slice(0, separatorIndex);
        const value = line.slice(separatorIndex + 1).replace(/^ /, "");
        if (field === "event") {
            event = value;
        } else if (field === "data") {
            dataLines.push(value);
        }
    });
    return dataLines.length > 0 ? { event, data: dataLines.join("\n") } : null;
}

export function createSseParser(onEvent: OnEvent): (chunk: string) => void {
    let buffer = "";
    return (chunk) => {
        buffer += chunk;
        const blocks = buffer.split(EVENT_SEPARATOR);
        buffer = blocks.pop() ?? "";
        blocks.forEach((block) => {
            const event = parseBlock(block);
            if (event) {
                onEvent(event);
            }
        });
    };
}

export async function streamEvents(path: string, { onEvent, signal }: StreamOptions) {
    const response = await apiClient.openStream(path, { signal });
    if (!response.body) {
        return;
    }
    const reader = response.body.pipeThrough(new TextDecoderStream()).getReader();
    const parse = createSseParser(onEvent);
    for (;;) {
        const { done, value } = await reader.read();
        if (done) {
            return;
        }
        parse(value);
    }
}
