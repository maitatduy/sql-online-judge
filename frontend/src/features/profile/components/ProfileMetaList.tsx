import { GraduationCap, Link2, MapPin } from "lucide-react";

import { displayUrl, safeExternalUrl } from "@/lib/safe-external-url";

interface ProfileMetaListProps {
    school?: string;
    location?: string;
    websiteUrl?: string;
}

const ITEM_CLASS_NAME = "flex items-center gap-2";
const ICON_CLASS_NAME = "size-4 shrink-0";

export function ProfileMetaList({ school, location, websiteUrl }: ProfileMetaListProps) {
    const website = safeExternalUrl(websiteUrl);

    if (!school && !location && !website) {
        return null;
    }

    return (
        <ul
            aria-label="Thông tin thêm"
            className="flex flex-col gap-2 text-[13px] text-muted-foreground"
        >
            {school ? (
                <li className={ITEM_CLASS_NAME}>
                    <GraduationCap aria-hidden="true" className={ICON_CLASS_NAME} />
                    {school}
                </li>
            ) : null}
            {location ? (
                <li className={ITEM_CLASS_NAME}>
                    <MapPin aria-hidden="true" className={ICON_CLASS_NAME} />
                    {location}
                </li>
            ) : null}
            {website ? (
                <li className={ITEM_CLASS_NAME}>
                    <Link2 aria-hidden="true" className={ICON_CLASS_NAME} />
                    <a
                        href={website}
                        target="_blank"
                        rel="noopener noreferrer nofollow ugc"
                        className="min-w-0 truncate text-brand hover:underline"
                    >
                        {displayUrl(website)}
                    </a>
                </li>
            ) : null}
        </ul>
    );
}
