import { cn } from "@/lib/utils";

interface SectionHeadingProps {
    eyebrow: string;
    title: string;
    eyebrowClassName?: string;
    className?: string;
}

export function SectionHeading({
    eyebrow,
    title,
    eyebrowClassName,
    className,
}: SectionHeadingProps) {
    return (
        <div className={className}>
            <p
                className={cn(
                    "text-xs font-semibold tracking-[0.06em] text-muted-foreground uppercase",
                    eyebrowClassName,
                )}
            >
                {eyebrow}
            </p>
            <h2 className="mt-2 text-3xl font-extrabold tracking-[-0.02em] md:text-[40px] md:leading-tight">
                {title}
            </h2>
        </div>
    );
}
