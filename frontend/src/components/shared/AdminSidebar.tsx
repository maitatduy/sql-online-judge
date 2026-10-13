import { ChevronLeft } from "lucide-react";
import Link from "next/link";
import { useId } from "react";

import { AdminSidebarIndicator } from "@/components/shared/AdminSidebarIndicator";
import { Avatar } from "@/components/shared/Avatar";
import { Logo } from "@/components/shared/Logo";
import {
    ADMIN_NAVIGATION_GROUPS,
    ADMIN_OVERVIEW_ITEM,
    type AdminNavigationCounts,
    type AdminNavigationGroup,
    type AdminNavigationItem,
    isAdminItemActive,
} from "@/lib/admin-navigation";
import { ROUTES } from "@/lib/routes";
import { cn } from "@/lib/utils";

const RESIZE_CLASS = "duration-300 ease-in-out motion-reduce:transition-none";
const FADE_CLASS = "transition-opacity duration-200 motion-reduce:transition-none";
const LINK_CLASS =
    "relative flex min-h-11 items-center rounded-md pr-3 font-medium text-sidebar-text hover:bg-sidebar-hover hover:text-sidebar-strong md:min-h-9.5";
const ICON_SLOT_CLASS = "flex w-10 shrink-0 justify-center";
const ICON_CLASS = "size-4.25";

interface AdminSidebarUser {
    username: string;
    displayName: string;
}

interface AdminSidebarProps {
    activePath: string;
    counts?: AdminNavigationCounts;
    user: AdminSidebarUser;
    isCollapsed?: boolean;
    onToggleCollapse?: () => void;
}

interface SidebarLinkProps {
    item: AdminNavigationItem;
    activePath: string;
    counts?: AdminNavigationCounts;
    isCollapsed: boolean;
}

interface SidebarGroupProps {
    group: AdminNavigationGroup;
    activePath: string;
    counts?: AdminNavigationCounts;
    isCollapsed: boolean;
}

interface LinkLabelProps {
    isCollapsed: boolean;
    children: string;
}

interface CollapseToggleProps {
    isCollapsed: boolean;
    onToggle: () => void;
}

function LinkLabel({ isCollapsed, children }: LinkLabelProps) {
    return (
        <span className={cn("whitespace-nowrap", FADE_CLASS, isCollapsed && "opacity-0")}>
            {children}
        </span>
    );
}

function SidebarLink({ item, activePath, counts, isCollapsed }: SidebarLinkProps) {
    const isActive = isAdminItemActive(activePath, item);
    const Icon = item.icon;

    return (
        <Link
            href={item.href}
            aria-current={isActive ? "page" : undefined}
            title={isCollapsed ? item.label : undefined}
            className={cn(
                LINK_CLASS,
                isActive && "bg-sidebar-active font-semibold text-sidebar-strong",
            )}
        >
            <span className={ICON_SLOT_CLASS}>
                <Icon aria-hidden="true" className={ICON_CLASS} />
            </span>
            <LinkLabel isCollapsed={isCollapsed}>{item.label}</LinkLabel>
            {item.indicator && counts ? (
                <AdminSidebarIndicator
                    indicator={item.indicator}
                    counts={counts}
                    isCompact={isCollapsed}
                />
            ) : null}
        </Link>
    );
}

function SidebarGroup({ group, activePath, counts, isCollapsed }: SidebarGroupProps) {
    const titleId = useId();

    return (
        <>
            <div aria-hidden="true" className="mx-2 my-2 border-t border-sidebar-line" />
            <p id={titleId} className="sr-only">
                {group.title}
            </p>
            <ul aria-labelledby={titleId} className="flex flex-col gap-0.5">
                {group.items.map((item) => (
                    <li key={item.href}>
                        <SidebarLink
                            item={item}
                            activePath={activePath}
                            counts={counts}
                            isCollapsed={isCollapsed}
                        />
                    </li>
                ))}
            </ul>
        </>
    );
}

function CollapseToggle({ isCollapsed, onToggle }: CollapseToggleProps) {
    return (
        <button
            type="button"
            aria-label={isCollapsed ? "Mở rộng menu" : "Thu gọn menu"}
            aria-expanded={!isCollapsed}
            onClick={onToggle}
            className="absolute top-6 -right-3.5 z-10 inline-flex size-7 items-center justify-center rounded-full border border-sidebar-line bg-sidebar text-sidebar-text shadow-sm transition-colors after:absolute after:-inset-2 hover:border-brand hover:bg-brand hover:text-white"
        >
            <ChevronLeft
                aria-hidden="true"
                className={cn(
                    "size-4 transition-transform",
                    RESIZE_CLASS,
                    isCollapsed && "rotate-180",
                )}
            />
        </button>
    );
}

export function AdminSidebar({
    activePath,
    counts,
    user,
    isCollapsed = false,
    onToggleCollapse,
}: AdminSidebarProps) {
    return (
        <div
            className={cn(
                "sticky top-0 h-screen shrink-0 transition-[width]",
                RESIZE_CLASS,
                isCollapsed ? "w-16" : "w-68",
            )}
        >
            <nav
                aria-label="Quản trị"
                className="flex h-full flex-col overflow-x-hidden overflow-y-auto border-r border-sidebar-line bg-sidebar px-3 py-4.5 text-sidebar-text"
            >
                <Link
                    href={ROUTES.admin}
                    aria-label="Admin Console"
                    className={cn(
                        "mb-3.5 block shrink-0 overflow-hidden pl-0.5 transition-[max-width]",
                        RESIZE_CLASS,
                        isCollapsed ? "max-w-10" : "max-w-full",
                    )}
                >
                    <Logo className="whitespace-nowrap text-sidebar-strong" />
                </Link>
                <SidebarLink
                    item={ADMIN_OVERVIEW_ITEM}
                    activePath={activePath}
                    isCollapsed={isCollapsed}
                />
                {ADMIN_NAVIGATION_GROUPS.map((group) => (
                    <SidebarGroup
                        key={group.title}
                        group={group}
                        activePath={activePath}
                        counts={counts}
                        isCollapsed={isCollapsed}
                    />
                ))}
                <div className="flex-1" />
                <Link
                    href={ROUTES.home}
                    title={isCollapsed ? "Về trang người học" : undefined}
                    className={cn(LINK_CLASS, "mt-4 shrink-0 text-[13px]")}
                >
                    <span className={ICON_SLOT_CLASS}>
                        <ChevronLeft aria-hidden="true" className={ICON_CLASS} />
                    </span>
                    <LinkLabel isCollapsed={isCollapsed}>Về trang người học</LinkLabel>
                </Link>
                <div className="mt-1.5 flex shrink-0 items-center border-t border-sidebar-line py-3">
                    <span className={ICON_SLOT_CLASS}>
                        <Avatar name={user.displayName} />
                    </span>
                    <span
                        className={cn(
                            "min-w-0 pl-1.5 text-[13px] whitespace-nowrap",
                            FADE_CLASS,
                            isCollapsed && "opacity-0",
                        )}
                    >
                        <strong className="block truncate text-sidebar-strong">
                            {user.username}
                        </strong>
                        <span className="text-[11px] text-sidebar-muted">Quản trị viên</span>
                    </span>
                </div>
            </nav>
            {onToggleCollapse ? (
                <CollapseToggle isCollapsed={isCollapsed} onToggle={onToggleCollapse} />
            ) : null}
        </div>
    );
}
