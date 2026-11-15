import { Clock } from "lucide-react";

import { DashboardSection } from "./DashboardSection";

interface ComingSoonSectionProps {
    title: string;
    note: string;
}

export function ComingSoonSection({ title, note }: ComingSoonSectionProps) {
    return (
        <DashboardSection title={title} className="flex-[1_1_320px]">
            <div className="flex flex-1 flex-col items-center justify-center gap-2 rounded-lg bg-surface-2 px-5 py-10 text-center">
                <Clock aria-hidden="true" className="size-7 text-muted-foreground" />
                <p className="font-semibold">Sắp có</p>
                <p className="text-sm text-muted-foreground">{note}</p>
            </div>
        </DashboardSection>
    );
}
