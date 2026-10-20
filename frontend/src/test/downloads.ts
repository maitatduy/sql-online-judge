import { onTestFinished, vi } from "vitest";

const STUB_OBJECT_URL = "blob:http://localhost/stub-download";

interface ObjectUrlStub {
    url: string;
    created: Blob[];
    revoked: string[];
}

interface DownloadStub extends ObjectUrlStub {
    fileNames: string[];
}

export function stubObjectUrls(): ObjectUrlStub {
    const stub: ObjectUrlStub = { url: STUB_OBJECT_URL, created: [], revoked: [] };
    const original = {
        createObjectURL: URL.createObjectURL,
        revokeObjectURL: URL.revokeObjectURL,
    };
    URL.createObjectURL = (blob: Blob | MediaSource) => {
        stub.created.push(blob as Blob);
        return STUB_OBJECT_URL;
    };
    URL.revokeObjectURL = (url: string) => {
        stub.revoked.push(url);
    };
    onTestFinished(() => {
        URL.createObjectURL = original.createObjectURL;
        URL.revokeObjectURL = original.revokeObjectURL;
    });
    return stub;
}

export function stubDownloads(): DownloadStub {
    const stub: DownloadStub = { ...stubObjectUrls(), fileNames: [] };
    const clickSpy = vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(function (
        this: HTMLAnchorElement,
    ) {
        stub.fileNames.push(this.download);
    });
    onTestFinished(() => clickSpy.mockRestore());
    return stub;
}
