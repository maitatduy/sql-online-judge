import { cn } from "@/lib/utils";

const TOPIC_CLASS_NAMES = [
    "bg-topic-1/12 text-topic-1",
    "bg-topic-2/12 text-topic-2",
    "bg-topic-3/12 text-topic-3",
    "bg-topic-4/12 text-topic-4",
    "bg-topic-5/12 text-topic-5",
    "bg-topic-6/12 text-topic-6",
    "bg-topic-7/12 text-topic-7",
    "bg-topic-8/12 text-topic-8",
];

const TOPIC_FILL_CLASS_NAMES = [
    "bg-topic-1",
    "bg-topic-2",
    "bg-topic-3",
    "bg-topic-4",
    "bg-topic-5",
    "bg-topic-6",
    "bg-topic-7",
    "bg-topic-8",
];

function topicIndexOf(slug: string): number {
    const charCodeSum = Array.from(slug).reduce(
        (sum, character) => sum + character.charCodeAt(0),
        0,
    );
    return charCodeSum % TOPIC_CLASS_NAMES.length;
}

export function topicClassNameOf(slug: string): string {
    return TOPIC_CLASS_NAMES[topicIndexOf(slug)];
}

export function topicFillClassOf(slug: string): string {
    return TOPIC_FILL_CLASS_NAMES[topicIndexOf(slug)];
}

interface TopicTagProps {
    slug: string;
    name: string;
    className?: string;
}

export function TopicTag({ slug, name, className }: TopicTagProps) {
    return (
        <span
            className={cn(
                "rounded-sm px-1.5 py-0.5 text-xs font-medium whitespace-nowrap",
                topicClassNameOf(slug),
                className,
            )}
        >
            {name}
        </span>
    );
}
