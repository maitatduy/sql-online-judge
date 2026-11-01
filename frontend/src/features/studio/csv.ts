export type CsvCell = string | null;

export interface CsvRows {
    rows: CsvCell[][];
    error: string | null;
}

const NEEDS_QUOTES = /[",\r\n]/;

export function parseCsv(csv: string): string[][] {
    const records: string[][] = [];
    let fields: string[] = [];
    let value = "";
    let inQuotes = false;
    let hasContent = false;
    for (let index = 0; index < csv.length; index++) {
        const character = csv[index];
        hasContent = true;
        if (inQuotes) {
            if (character !== '"') {
                value += character;
            } else if (csv[index + 1] === '"') {
                value += '"';
                index += 1;
            } else {
                inQuotes = false;
            }
        } else if (character === '"') {
            inQuotes = true;
        } else if (character === ",") {
            fields.push(value);
            value = "";
        } else if (character === "\r" || character === "\n") {
            if (character === "\r" && csv[index + 1] === "\n") {
                index += 1;
            }
            fields.push(value);
            records.push(fields);
            fields = [];
            value = "";
            hasContent = false;
        } else {
            value += character;
        }
    }
    if (hasContent) {
        fields.push(value);
        records.push(fields);
    }
    return records;
}

export function rowsFromCsv(csv: string, columns: string[]): CsvRows {
    const [header = [], ...records] = parseCsv(csv);
    const positions = columns.map((column) => header.indexOf(column));
    if (header.length !== columns.length || positions.some((position) => position < 0)) {
        return { rows: [], error: `Dòng tiêu đề phải gồm các cột: ${columns.join(", ")}` };
    }
    return {
        rows: records.map((record) =>
            positions.map((position) => (record[position] ? record[position] : null)),
        ),
        error: null,
    };
}

function toField(cell: CsvCell): string {
    if (cell === null) {
        return "";
    }
    return NEEDS_QUOTES.test(cell) ? `"${cell.replaceAll('"', '""')}"` : cell;
}

export function rowsToCsv(columns: string[], rows: CsvCell[][]): string {
    return [columns, ...rows].map((row) => `${row.map(toField).join(",")}\n`).join("");
}
