import { useId } from "react";

interface StatementSectionProps {
    title: string;
    children: React.ReactNode;
}

export function StatementSection({ title, children }: StatementSectionProps) {
    const titleId = useId();

    return (
        <section aria-labelledby={titleId} className="flex flex-col gap-3">
            <h2 id={titleId} className="border-b pb-2 text-lg font-bold">
                {title}
            </h2>
            {children}
        </section>
    );
}
