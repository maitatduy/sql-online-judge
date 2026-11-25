"use client";

import { Menu } from "lucide-react";
import Link from "next/link";
import { useState } from "react";

import { Logo } from "@/components/shared/Logo";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";
import { useCurrentUser } from "@/lib/useCurrentUser";

import { marketingLinksFor } from "../navigation";

export function MarketingNav() {
    const { data: currentUser } = useCurrentUser();
    const [isMenuOpen, setMenuOpen] = useState(false);
    const links = marketingLinksFor(currentUser?.role);

    return (
        <>
            <nav aria-label="Điều hướng chính" className="hidden gap-1 lg:flex">
                {links.map((link) => (
                    <Button key={link.href} asChild variant="ghost">
                        <Link href={link.href}>{link.label}</Link>
                    </Button>
                ))}
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
                            {links.map((link) => (
                                <li key={link.href}>
                                    <Link
                                        href={link.href}
                                        onClick={() => setMenuOpen(false)}
                                        className="flex min-h-12 items-center rounded-md px-3 text-[17px] font-semibold hover:bg-surface-2"
                                    >
                                        {link.label}
                                    </Link>
                                </li>
                            ))}
                        </ul>
                    </nav>
                </DialogContent>
            </Dialog>
        </>
    );
}
