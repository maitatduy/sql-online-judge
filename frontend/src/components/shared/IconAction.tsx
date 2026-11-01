import Link from "next/link";

import type { LucideIcon } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { cn } from "@/lib/utils";

interface IconActionProps {
    label: string;
    tooltip: string;
    icon: LucideIcon;
    onClick?: () => void;
    href?: string;
    disabled?: boolean;
    className?: string;
}

export function IconAction({
    label,
    tooltip,
    icon: Icon,
    onClick,
    href,
    disabled = false,
    className,
}: IconActionProps) {
    const buttonClass = cn("text-muted-foreground hover:text-foreground", className);
    const icon = <Icon aria-hidden="true" />;
    const button = (
        <Button
            type="button"
            variant="ghost"
            size="icon-sm"
            aria-label={label}
            disabled={disabled}
            onClick={onClick}
            className={buttonClass}
        >
            {icon}
        </Button>
    );

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                {disabled ? (
                    <span tabIndex={0} className="inline-flex">
                        {button}
                    </span>
                ) : href ? (
                    <Button asChild variant="ghost" size="icon-sm" className={buttonClass}>
                        <Link href={href} aria-label={label}>
                            {icon}
                        </Link>
                    </Button>
                ) : (
                    button
                )}
            </TooltipTrigger>
            <TooltipContent>{tooltip}</TooltipContent>
        </Tooltip>
    );
}
