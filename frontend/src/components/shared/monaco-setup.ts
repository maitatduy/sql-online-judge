import { loader } from "@monaco-editor/react";
import * as monaco from "monaco-editor/editor/editor.api";
import "monaco-editor/languages/definitions/pgsql/register";

globalThis.MonacoEnvironment = {
    getWorker: () =>
        new Worker(new URL("./monaco-editor.worker.ts", import.meta.url), { type: "module" }),
};

loader.config({ monaco });
