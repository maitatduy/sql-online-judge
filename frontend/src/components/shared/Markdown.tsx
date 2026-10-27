import ReactMarkdown, { type Components } from "react-markdown";
import rehypeSanitize from "rehype-sanitize";

import { cn } from "@/lib/utils";

const COMPONENTS: Components = {
    a: ({ href, children }) => (
        <a
            href={href}
            target="_blank"
            rel="noopener noreferrer nofollow ugc"
            className="text-brand underline-offset-2 hover:underline"
        >
            {children}
        </a>
    ),
    code: ({ className, children }) => (
        <code
            className={cn(
                "rounded-sm bg-surface-2 px-1.5 py-0.5 font-mono text-[0.92em]",
                className,
            )}
        >
            {children}
        </code>
    ),
};

interface MarkdownProps {
    children: string;
    className?: string;
}

export function Markdown({ children, className }: MarkdownProps) {
    return (
        <div
            className={cn(
                "flex flex-col gap-3 leading-relaxed [&_ol]:pl-5 [&_ul]:pl-5 [&_ul]:list-disc [&_ol]:list-decimal",
                className,
            )}
        >
            <ReactMarkdown rehypePlugins={[rehypeSanitize]} components={COMPONENTS}>
                {children}
            </ReactMarkdown>
        </div>
    );
}
