import { Eye, Pencil, Trash2 } from "lucide-react";

import { IconAction } from "./IconAction";

export interface UserRowHandlers {
    onView: (username: string) => void;
    onEdit: (username: string) => void;
    onDelete: (username: string) => void;
}

interface UserRowActionsProps {
    username: string;
    canDelete: boolean;
    handlers: UserRowHandlers;
}

export function UserRowActions({ username, canDelete, handlers }: UserRowActionsProps) {
    return (
        <div className="flex justify-end gap-1">
            <IconAction
                label={`Xem chi tiết ${username}`}
                tooltip="Xem chi tiết"
                icon={Eye}
                onClick={() => handlers.onView(username)}
            />
            <IconAction
                label={`Sửa ${username}`}
                tooltip="Sửa"
                icon={Pencil}
                onClick={() => handlers.onEdit(username)}
            />
            {canDelete ? (
                <IconAction
                    label={`Xoá ${username}`}
                    tooltip="Xoá"
                    icon={Trash2}
                    onClick={() => handlers.onDelete(username)}
                    className="hover:bg-destructive/10 hover:text-destructive"
                />
            ) : (
                <span aria-hidden="true" className="size-8" />
            )}
        </div>
    );
}
