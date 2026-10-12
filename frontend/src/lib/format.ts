const numberFormatter = new Intl.NumberFormat("vi-VN");

export function formatNumber(value: number): string {
    return numberFormatter.format(value);
}
