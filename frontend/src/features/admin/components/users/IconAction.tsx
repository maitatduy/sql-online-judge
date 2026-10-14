import type { LucideIcon } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { cn } from "@/lib/utils";

interface IconActionProps {
    label: string;
    tooltip: string;
    icon: LucideIcon;
    onClick: () => void;
    className?: string;
}

export function IconAction({ label, tooltip, icon: Icon, onClick, className }: IconActionProps) {
    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <Button
                    type="button"
                    variant="ghost"
                    size="icon-sm"
                    aria-label={label}
                    onClick={onClick}
                    className={cn("text-muted-foreground hover:text-foreground", className)}
                >
                    <Icon aria-hidden="true" />
                </Button>
            </TooltipTrigger>
            <TooltipContent>{tooltip}</TooltipContent>
        </Tooltip>
    );
}
