const NEEDS_QUOTING = /[",\r\n]/;

function csvField(value: string): string {
    return NEEDS_QUOTING.test(value) ? `"${value.replaceAll('"', '""')}"` : value;
}

export function toCsv(rows: readonly (readonly string[])[]): string {
    return rows.map((row) => `${row.map(csvField).join(",")}\n`).join("");
}
