const REVOKE_DELAY_MS = 1000;

export function downloadFile(blob: Blob, fileName: string): void {
    const objectUrl = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = objectUrl;
    link.download = fileName;
    link.hidden = true;
    document.body.append(link);
    try {
        link.click();
    } finally {
        link.remove();
        setTimeout(() => URL.revokeObjectURL(objectUrl), REVOKE_DELAY_MS);
    }
}
