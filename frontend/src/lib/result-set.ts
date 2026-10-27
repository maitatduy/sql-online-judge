export type ResultCell = string | null;

export interface ResultSet {
    columns: string[];
    rows: ResultCell[][];
    isTruncated: boolean;
}
