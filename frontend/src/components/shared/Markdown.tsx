import type { Element } from "hast";
import ReactMarkdown, { type Components } from "react-markdown";
import rehypeSanitize from "rehype-sanitize";

import { cn } from "@/lib/utils";

const SQL_RUN_CLASS = "language-sql-run";

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

function runnableSqlOf(pre: Element | undefined): string | null {
    const code = pre?.children[0];
    if (code?.type !== "element" || code.tagName !== "code") {
        return null;
    }
    const classes = code.properties.className;
    if (!Array.isArray(classes) || !classes.includes(SQL_RUN_CLASS)) {
        return null;
    }
    return code.children
        .map((child) => (child.type === "text" ? child.value : ""))
        .join("")
        .trimEnd();
}

interface MarkdownProps {
    children: string;
    className?: string;
    renderSqlRun?: (sql: string) => React.ReactNode;
}

export function Markdown({ children, className, renderSqlRun }: MarkdownProps) {
    const components: Components = renderSqlRun
        ? {
              ...COMPONENTS,
              pre: ({ node, children: content }) => {
                  const sql = runnableSqlOf(node);
                  return sql === null ? <pre>{content}</pre> : renderSqlRun(sql);
              },
          }
        : COMPONENTS;

    return (
        <div
            className={cn(
                "flex flex-col gap-3 leading-relaxed [&_ol]:pl-5 [&_ul]:pl-5 [&_ul]:list-disc [&_ol]:list-decimal",
                className,
            )}
        >
            <ReactMarkdown rehypePlugins={[rehypeSanitize]} components={components}>
                {children}
            </ReactMarkdown>
        </div>
    );
}
