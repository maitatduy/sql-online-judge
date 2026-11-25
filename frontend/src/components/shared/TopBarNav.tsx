"use client";

import { Menu } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";

import { Logo } from "@/components/shared/Logo";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";
import { ACCOUNT_MENU_LINKS, isSectionOpen, siteLinksFor } from "@/lib/site-navigation";
import { useCurrentUser } from "@/lib/useCurrentUser";
import { useLogout } from "@/lib/useLogout";
import { cn } from "@/lib/utils";

const MENU_ITEM_CLASS =
    "flex min-h-12 w-full items-center rounded-md px-3 text-[17px] font-semibold hover:bg-surface-2";

export function TopBarNav() {
    const pathname = usePathname();
    const { data: currentUser } = useCurrentUser();
    const [isMenuOpen, setMenuOpen] = useState(false);
    const logout = useLogout();
    const links = siteLinksFor(currentUser?.role);
    const menuLinks = currentUser ? [...links, ...ACCOUNT_MENU_LINKS] : links;

    return (
        <>
            <nav aria-label="Điều hướng chính" className="hidden gap-1 lg:flex">
                {links.map((link) => {
                    const isOpen = isSectionOpen(pathname, link.href);
                    return (
                        <Button
                            key={link.href}
                            asChild
                            variant="ghost"
                            className={cn(
                                isOpen &&
                                    "bg-brand-soft text-brand hover:bg-brand-soft hover:text-brand",
                            )}
                        >
                            <Link href={link.href} aria-current={isOpen ? "page" : undefined}>
                                {link.label}
                            </Link>
                        </Button>
                    );
                })}
            </nav>
            <Button
                type="button"
                variant="ghost"
                size="icon"
                aria-label="Mở menu"
                aria-haspopup="dialog"
                className="order-last size-11 lg:hidden"
                onClick={() => setMenuOpen(true)}
            >
                <Menu aria-hidden="true" className="size-5" />
            </Button>
            <Dialog open={isMenuOpen} onOpenChange={setMenuOpen}>
                <DialogContent className="top-0 right-auto left-0 h-dvh max-h-dvh w-[min(320px,85vw)] translate-x-0 translate-y-0 gap-6 rounded-none border-y-0 border-l-0">
                    <DialogTitle className="sr-only">Menu</DialogTitle>
                    <DialogDescription className="sr-only">
                        Các trang chính của SQL Judge
                    </DialogDescription>
                    <Logo />
                    <nav aria-label="Menu điện thoại">
                        <ul className="flex flex-col">
                            {menuLinks.map((link) => {
                                const isOpen = isSectionOpen(pathname, link.href);
                                return (
                                    <li key={link.href}>
                                        <Link
                                            href={link.href}
                                            aria-current={isOpen ? "page" : undefined}
                                            onClick={() => setMenuOpen(false)}
                                            className={cn(
                                                MENU_ITEM_CLASS,
                                                isOpen &&
                                                    "bg-brand-soft text-brand hover:bg-brand-soft",
                                            )}
                                        >
                                            {link.label}
                                        </Link>
                                    </li>
                                );
                            })}
                            {currentUser ? (
                                <li>
                                    <button
                                        type="button"
                                        disabled={logout.isPending}
                                        onClick={() => {
                                            setMenuOpen(false);
                                            logout.mutate();
                                        }}
                                        className={cn(MENU_ITEM_CLASS, "text-muted-foreground")}
                                    >
                                        Đăng xuất
                                    </button>
                                </li>
                            ) : null}
                        </ul>
                    </nav>
                </DialogContent>
            </Dialog>
        </>
    );
}
