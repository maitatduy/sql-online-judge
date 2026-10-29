import { appendFileSync, existsSync, readdirSync, readFileSync } from "node:fs";
import { join } from "node:path";

const REPORT_DIRS = ["surefire-reports", "failsafe-reports"];
const STACK_TRACE_LINES = 12;
const TESTCASE = /<testcase\b([^>]*?)(?:\/>|>([\s\S]*?)<\/testcase>)/g;
const PROBLEM = /<(failure|error)\b([^>]*?)(?:\/>|>([\s\S]*?)<\/\1>)/;
const XML_ENTITIES = { amp: "&", lt: "<", gt: ">", quot: '"', apos: "'" };

function decodeXml(text) {
    return text
        .replace(/<!\[CDATA\[([\s\S]*?)\]\]>/g, "$1")
        .replace(/&#(\d+);/g, (_, code) => String.fromCodePoint(Number(code)))
        .replace(/&(amp|lt|gt|quot|apos);/g, (_, name) => XML_ENTITIES[name]);
}

function attribute(attributes, name) {
    const match = new RegExp(`\\b${name}="([^"]*)"`).exec(attributes);
    return match ? decodeXml(match[1]) : "";
}

function escapeData(text) {
    return text.replace(/%/g, "%25").replace(/\r/g, "%0D").replace(/\n/g, "%0A");
}

function escapeProperty(text) {
    return escapeData(text).replace(/:/g, "%3A").replace(/,/g, "%2C");
}

function escapeMarkdownCell(text) {
    return text
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/\|/g, "\\|")
        .replace(/\s+/g, " ");
}

function reportFiles(root) {
    return readdirSync(root, { withFileTypes: true })
        .filter((entry) => entry.isDirectory())
        .flatMap((module) =>
            REPORT_DIRS.map((reportDir) => join(root, module.name, "target", reportDir)),
        )
        .filter((directory) => existsSync(directory))
        .flatMap((directory) =>
            readdirSync(directory)
                .filter((file) => file.startsWith("TEST-") && file.endsWith(".xml"))
                .map((file) => join(directory, file)),
        );
}

function failuresIn(file) {
    return [...readFileSync(file, "utf8").matchAll(TESTCASE)].flatMap(([, attributes, body]) => {
        const problem = body ? PROBLEM.exec(body) : null;
        if (!problem) {
            return [];
        }
        const [, kind, problemAttributes, details = ""] = problem;
        const className = attribute(attributes, "classname").split(".").pop();
        return [
            {
                title: `${className}.${attribute(attributes, "name")}`,
                kind,
                message:
                    attribute(problemAttributes, "message") || attribute(problemAttributes, "type"),
                stackTrace: decodeXml(details)
                    .trim()
                    .split("\n")
                    .slice(0, STACK_TRACE_LINES)
                    .join("\n"),
            },
        ];
    });
}

const failures = reportFiles(process.argv[2] ?? ".").flatMap(failuresIn);

for (const { title, message, stackTrace } of failures) {
    console.log(
        `::error title=${escapeProperty(title)}::${escapeData(`${message}\n${stackTrace}`)}`,
    );
}

if (process.env.GITHUB_STEP_SUMMARY && failures.length > 0) {
    const rows = failures.map(
        ({ title, kind, message }) =>
            `| \`${title}\` | ${kind} | ${escapeMarkdownCell(message.slice(0, 300))} |`,
    );
    const summary = [
        "### Failed tests",
        "",
        "| Test | Kind | Message |",
        "|---|---|---|",
        ...rows,
        "",
    ];
    appendFileSync(process.env.GITHUB_STEP_SUMMARY, summary.join("\n"));
}
