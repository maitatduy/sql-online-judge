import { Markdown } from "@/components/shared/Markdown";

const LESSON_BODY_CLASSES =
    "[&_h2]:mt-2 [&_h2]:text-lg [&_h2]:font-bold [&_pre]:overflow-x-auto [&_pre]:rounded-lg [&_pre]:border [&_pre]:bg-surface-2 [&_pre]:p-4 [&_pre_code]:bg-transparent [&_pre_code]:p-0";

interface LessonMarkdownProps {
    children: string;
}

export function LessonMarkdown({ children }: LessonMarkdownProps) {
    return <Markdown className={LESSON_BODY_CLASSES}>{children}</Markdown>;
}
