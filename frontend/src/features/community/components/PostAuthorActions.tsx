"use client";

import { Pencil, Trash2 } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";

import { DeleteConfirmDialog } from "@/components/shared/DeleteConfirmDialog";
import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { ROUTES } from "@/lib/routes";

import { communityApi } from "../api";
import { useCommunityMutation } from "../hooks/useCommunity";

interface PostAuthorActionsProps {
    postId: string;
}

export function PostAuthorActions({ postId }: PostAuthorActionsProps) {
    const router = useRouter();
    const showToast = useToast();
    const remove = useCommunityMutation(communityApi.deletePost);
    const [isConfirming, setIsConfirming] = useState(false);

    const confirmDelete = () =>
        remove.mutate(postId, {
            onSuccess: () => {
                showToast("Đã xoá bài viết.");
                router.push(ROUTES.community);
            },
            onError: (error) => showToast(error.message, "error"),
        });

    return (
        <div className="flex gap-1">
            <Button asChild variant="ghost" size="sm">
                <Link href={ROUTES.communityEdit(postId)}>
                    <Pencil aria-hidden="true" />
                    Sửa
                </Link>
            </Button>
            <Button type="button" variant="ghost" size="sm" onClick={() => setIsConfirming(true)}>
                <Trash2 aria-hidden="true" />
                Xoá
            </Button>
            <DeleteConfirmDialog
                open={isConfirming}
                onOpenChange={setIsConfirming}
                title="Xoá bài viết?"
                description="Bài viết và bình luận sẽ không còn hiện với mọi người."
                confirmLabel="Xoá bài"
                isPending={remove.isPending}
                onConfirm={confirmDelete}
            />
        </div>
    );
}
