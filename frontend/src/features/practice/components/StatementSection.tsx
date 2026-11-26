import { useId } from "react";

interface StatementSectionProps {
    title: string;
    children: React.ReactNode;
}

export function StatementSection({ title, children }: StatementSectionProps) {
    const titleId = useId();

    return (
        <section aria-labelledby={titleId} className="flex flex-col gap-2">
            <h2 id={titleId} className="text-base font-bold">
                {title}
            </h2>
            {children}
        </section>
    );
}
