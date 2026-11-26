import { Markdown } from "@/components/shared/Markdown";

const LESSON_BODY_CLASSES =
    "gap-4 text-[16px] leading-[1.8] [&_h2]:mt-6 [&_h2]:text-xl [&_h2]:font-bold [&_h2]:leading-snug [&_h3]:mt-4 [&_h3]:text-[17px] [&_h3]:font-bold [&_li]:my-1 [&_strong]:font-semibold [&_blockquote]:border-l-4 [&_blockquote]:border-brand [&_blockquote]:bg-brand-soft [&_blockquote]:px-4 [&_blockquote]:py-3 [&_pre]:overflow-x-auto [&_pre]:rounded-lg [&_pre]:border [&_pre]:bg-surface-2 [&_pre]:p-4 [&_pre]:text-[14px] [&_pre]:leading-relaxed [&_pre_code]:bg-transparent [&_pre_code]:p-0";

interface LessonMarkdownProps {
    children: string;
}

export function LessonMarkdown({ children }: LessonMarkdownProps) {
    return <Markdown className={LESSON_BODY_CLASSES}>{children}</Markdown>;
}
