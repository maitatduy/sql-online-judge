export const MAIN_CONTENT_ID = "main-content";

export function SkipLink() {
    return (
        <a
            href={`#${MAIN_CONTENT_ID}`}
            className="sr-only focus:not-sr-only focus:absolute focus:top-2 focus:left-2 focus:z-50 focus:rounded-md focus:bg-background focus:px-3.5 focus:py-2 focus:font-semibold"
        >
            Bỏ qua tới nội dung chính
        </a>
    );
}
