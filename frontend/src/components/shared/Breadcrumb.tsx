import { ChevronRight } from "lucide-react";
import Link from "next/link";

import { cn } from "@/lib/utils";

export interface BreadcrumbItem {
    label: string;
    href?: string;
}

interface BreadcrumbProps {
    items: BreadcrumbItem[];
    className?: string;
}

export function Breadcrumb({ items, className }: BreadcrumbProps) {
    return (
        <nav aria-label="Đường dẫn" className={cn("min-w-0", className)}>
            <ol className="flex min-w-0 items-center gap-1.5 text-sm text-muted-foreground">
                {items.map((item, index) => {
                    const isCurrent = index === items.length - 1;
                    return (
                        <li
                            key={`${item.label}-${index}`}
                            className={cn(
                                "flex min-w-0 items-center gap-1.5",
                                !isCurrent && "shrink-0",
                            )}
                        >
                            {index > 0 ? (
                                <ChevronRight aria-hidden="true" className="size-3.5 shrink-0" />
                            ) : null}
                            {item.href && !isCurrent ? (
                                <Link href={item.href} className="hover:text-foreground">
                                    {item.label}
                                </Link>
                            ) : (
                                <span
                                    aria-current={isCurrent ? "page" : undefined}
                                    className={cn(
                                        "truncate",
                                        isCurrent && "font-medium text-foreground",
                                    )}
                                >
                                    {item.label}
                                </span>
                            )}
                        </li>
                    );
                })}
            </ol>
        </nav>
    );
}
