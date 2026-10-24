import Link from "next/link";

import { Button } from "@/components/ui/button";

interface PageStepLinkProps {
    label: string;
    href: string;
    isAvailable: boolean;
}

export function PageStepLink({ label, href, isAvailable }: PageStepLinkProps) {
    if (!isAvailable) {
        return (
            <Button type="button" size="sm" variant="outline" disabled>
                {label}
            </Button>
        );
    }

    return (
        <Button asChild size="sm" variant="outline">
            <Link href={href}>{label}</Link>
        </Button>
    );
}
