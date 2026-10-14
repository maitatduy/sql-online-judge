import * as webStreams from "node:stream/web";

const missingStreamGlobals = {
    ReadableStream: webStreams.ReadableStream,
    WritableStream: webStreams.WritableStream,
    TransformStream: webStreams.TransformStream,
    TextEncoderStream: webStreams.TextEncoderStream,
    TextDecoderStream: webStreams.TextDecoderStream,
};

Object.entries(missingStreamGlobals).forEach(([name, value]) => {
    if (!(name in globalThis)) {
        Object.defineProperty(globalThis, name, { value, configurable: true, writable: true });
    }
});
