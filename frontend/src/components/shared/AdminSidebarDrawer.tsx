"use client";

import { Menu, X } from "lucide-react";
import { useState } from "react";

import { AdminSidebar } from "@/components/shared/AdminSidebar";
import { Logo } from "@/components/shared/Logo";
import {
    Dialog,
    DialogClose,
    DialogContent,
    DialogTitle,
    DialogTrigger,
} from "@/components/ui/dialog";
import type { AdminNavigationCounts } from "@/lib/admin-navigation";

const ICON_BUTTON_CLASS =
    "inline-flex size-11 shrink-0 items-center justify-center rounded-md text-sidebar-strong hover:bg-sidebar-hover";

interface AdminSidebarDrawerProps {
    activePath: string;
    counts?: AdminNavigationCounts;
    user: { username: string; displayName: string };
}

export function AdminSidebarDrawer({ activePath, counts, user }: AdminSidebarDrawerProps) {
    const [openedAtPath, setOpenedAtPath] = useState<string | null>(null);

    return (
        <Dialog
            open={openedAtPath === activePath}
            onOpenChange={(isOpen) => setOpenedAtPath(isOpen ? activePath : null)}
        >
            <header className="sticky top-0 z-40 flex items-center gap-1 border-b border-sidebar-line bg-sidebar px-2 py-1 lg:hidden">
                <DialogTrigger aria-label="Mở menu quản trị" className={ICON_BUTTON_CLASS}>
                    <Menu aria-hidden="true" className="size-5" />
                </DialogTrigger>
                <Logo className="text-sidebar-strong" />
            </header>
            <DialogContent
                aria-describedby={undefined}
                className="top-0 left-0 h-full w-68 max-w-[80vw] translate-x-0 translate-y-0 gap-0 rounded-none border-0 bg-sidebar p-0"
            >
                <DialogTitle className="sr-only">Menu quản trị</DialogTitle>
                <AdminSidebar activePath={activePath} counts={counts} user={user} />
                <DialogClose
                    aria-label="Đóng menu quản trị"
                    className={`${ICON_BUTTON_CLASS} absolute top-2 left-full ml-2 border border-sidebar-line bg-sidebar`}
                >
                    <X aria-hidden="true" className="size-5" />
                </DialogClose>
            </DialogContent>
        </Dialog>
    );
}
