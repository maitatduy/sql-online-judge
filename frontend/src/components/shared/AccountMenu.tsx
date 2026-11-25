"use client";

import Link from "next/link";
import { DropdownMenu } from "radix-ui";

import { Avatar } from "@/components/shared/Avatar";
import type { CurrentUser } from "@/lib/current-user";
import { ROUTES } from "@/lib/routes";
import { roleLinksFor } from "@/lib/site-navigation";
import { useLogout } from "@/lib/useLogout";

const ITEM_CLASS =
    "flex min-h-10 cursor-pointer items-center rounded-md px-2.5 text-[15px] font-medium outline-none select-none data-[highlighted]:bg-surface-2 data-[disabled]:opacity-50";

interface AccountMenuProps {
    user: CurrentUser;
}

export function AccountMenu({ user }: AccountMenuProps) {
    const logout = useLogout();
    const roleLinks = roleLinksFor(user.role);

    return (
        <DropdownMenu.Root modal={false}>
            <DropdownMenu.Trigger
                aria-label="Tài khoản của tôi"
                className="inline-flex min-h-11 min-w-11 items-center justify-center rounded-full outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
                <Avatar name={user.displayName} src={user.avatarUrl} />
            </DropdownMenu.Trigger>
            <DropdownMenu.Portal>
                <DropdownMenu.Content
                    align="end"
                    sideOffset={8}
                    className="z-50 w-64 rounded-lg border bg-card p-1.5 shadow-lg"
                >
                    <div className="px-2.5 py-2">
                        <p className="truncate font-semibold">{user.displayName}</p>
                        <p className="truncate text-sm text-muted-foreground">@{user.username}</p>
                    </div>
                    <DropdownMenu.Separator className="my-1 h-px bg-border" />
                    <DropdownMenu.Item asChild className={ITEM_CLASS}>
                        <Link href={ROUTES.profile(user.username)}>Hồ sơ của tôi</Link>
                    </DropdownMenu.Item>
                    <DropdownMenu.Item asChild className={ITEM_CLASS}>
                        <Link href={ROUTES.settings}>Cài đặt</Link>
                    </DropdownMenu.Item>
                    {roleLinks.length > 0 ? (
                        <>
                            <DropdownMenu.Separator className="my-1 h-px bg-border" />
                            {roleLinks.map((link) => (
                                <DropdownMenu.Item key={link.href} asChild className={ITEM_CLASS}>
                                    <Link href={link.href}>{link.label}</Link>
                                </DropdownMenu.Item>
                            ))}
                        </>
                    ) : null}
                    <DropdownMenu.Separator className="my-1 h-px bg-border" />
                    <DropdownMenu.Item
                        className={ITEM_CLASS}
                        disabled={logout.isPending}
                        onSelect={() => logout.mutate()}
                    >
                        Đăng xuất
                    </DropdownMenu.Item>
                </DropdownMenu.Content>
            </DropdownMenu.Portal>
        </DropdownMenu.Root>
    );
}
