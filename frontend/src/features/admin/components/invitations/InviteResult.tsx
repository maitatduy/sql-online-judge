import { Button } from "@/components/ui/button";
import type { InviteRolesResponse } from "@/features/admin/invitations/types";

interface InviteResultProps {
    result: InviteRolesResponse;
    onDone: () => void;
}

export function InviteResult({ result, onDone }: InviteResultProps) {
    return (
        <div className="flex flex-col gap-3">
            <p className="text-[15px] font-semibold" role="status">
                Đã gửi {result.sentCount} lời mời.
            </p>
            {result.skipped.length > 0 ? (
                <div>
                    <p className="mb-1.5 text-sm text-muted-foreground">
                        Bỏ qua {result.skipped.length} người:
                    </p>
                    <ul className="flex flex-col gap-1 text-sm">
                        {result.skipped.map((skipped) => (
                            <li key={skipped.email} className="flex flex-wrap gap-x-2">
                                <span className="font-medium">{skipped.email}</span>
                                <span className="text-muted-foreground">{skipped.reason}</span>
                            </li>
                        ))}
                    </ul>
                </div>
            ) : null}
            <div className="flex justify-end">
                <Button type="button" onClick={onDone}>
                    Xong
                </Button>
            </div>
        </div>
    );
}
