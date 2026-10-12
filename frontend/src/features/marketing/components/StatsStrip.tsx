import { formatNumber } from "@/lib/format";

import type { Stat } from "../types";

interface StatsStripProps {
    stats: Stat[];
}

export function StatsStrip({ stats }: StatsStripProps) {
    return (
        <section aria-label="Số liệu nổi bật" className="border-y bg-surface">
            <dl className="mx-auto grid max-w-7xl grid-cols-[repeat(auto-fit,minmax(220px,1fr))] gap-6 px-6 py-8">
                {stats.map((stat) => (
                    <div key={stat.label} className="flex flex-col-reverse">
                        <dt className="mt-0.5 text-muted-foreground">{stat.label}</dt>
                        <dd className="font-mono text-[32px] leading-tight font-extrabold">
                            {typeof stat.value === "number" ? formatNumber(stat.value) : stat.value}
                        </dd>
                    </div>
                ))}
            </dl>
        </section>
    );
}
