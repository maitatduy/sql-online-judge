import { useMutation } from "@tanstack/react-query";

import { downloadFile } from "@/lib/download-file";

import { adminApi } from "../api";
import type { AuditFilters } from "../audit/search-params";

const FALLBACK_FILE_NAME = "audit-log.csv";

async function exportAndDownload(filters: AuditFilters): Promise<void> {
    const { blob, fileName } = await adminApi.exportAuditLog(filters);
    downloadFile(blob, fileName ?? FALLBACK_FILE_NAME);
}

export function useExportAuditLog() {
    return useMutation({ mutationFn: exportAndDownload });
}
