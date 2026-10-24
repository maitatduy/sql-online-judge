import { useMutation } from "@tanstack/react-query";

import { downloadFile } from "@/lib/download-file";

import { adminApi } from "../api";
import type { ExportUsersRequest } from "../users/types";

const FALLBACK_FILE_NAME = "users.csv";

async function exportAndDownload(request: ExportUsersRequest): Promise<void> {
    const { blob, fileName } = await adminApi.exportUsers(request);
    downloadFile(blob, fileName ?? FALLBACK_FILE_NAME);
}

export function useExportUsers() {
    return useMutation({ mutationFn: exportAndDownload });
}
