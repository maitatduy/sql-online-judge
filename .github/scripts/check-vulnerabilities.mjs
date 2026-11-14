import { readFileSync } from "node:fs";

const BLOCKING_SEVERITY = 7;

const report = JSON.parse(readFileSync(process.argv[2], "utf8"));

const findings = (report.results ?? []).flatMap((result) =>
    result.packages.flatMap((entry) =>
        entry.groups.map((group) => ({
            pkg: `${entry.package.name}@${entry.package.version}`,
            ids: group.ids.join(", "),
            severity: Number.parseFloat(group.max_severity),
        })),
    ),
);

const blocking = findings.filter((finding) => finding.severity >= BLOCKING_SEVERITY);
const unrated = findings.filter((finding) => Number.isNaN(finding.severity));

for (const finding of unrated) {
    console.log(`::warning::${finding.pkg} ${finding.ids} has no severity score`);
}
for (const finding of blocking) {
    console.log(`::error::${finding.pkg} ${finding.ids} severity ${finding.severity}`);
}

console.log(
    `${findings.length} vulnerability groups, ${blocking.length} at severity ${BLOCKING_SEVERITY} or above`,
);
process.exit(blocking.length > 0 ? 1 : 0);
