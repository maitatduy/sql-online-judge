"use client";

import { useId, useState } from "react";

import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";

import { communityApi } from "../api";
import { useCommunityMutation } from "../hooks/useCommunity";
import type { CommentThread } from "../types";

interface CommentFormProps {
    thread: CommentThread;
    parentId?: string;
    initialBody?: string;
    placeholder: string;
    onDone?: () => void;
}

export function CommentForm({
    thread,
    parentId,
    initialBody = "",
    placeholder,
    onDone,
}: CommentFormProps) {
    const showToast = useToast();
    const fieldId = useId();
    const write = useCommunityMutation(communityApi.writeComment);
    const [body, setBody] = useState(initialBody);

    const submit = (event: React.FormEvent) => {
        event.preventDefault();
        write.mutate(
            { ...thread, parentId, body: body.trim() },
            {
                onSuccess: () => {
                    setBody("");
                    onDone?.();
                },
                onError: (error) => showToast(error.message, "error"),
            },
        );
    };

    return (
        <form onSubmit={submit} className="flex flex-col gap-2">
            <label htmlFor={fieldId} className="sr-only">
                {placeholder}
            </label>
            <textarea
                id={fieldId}
                rows={parentId ? 2 : 3}
                maxLength={10_000}
                placeholder={placeholder}
                value={body}
                onChange={(event) => setBody(event.target.value)}
                className="rounded-md border bg-transparent px-3 py-2 text-[15px] leading-relaxed"
            />
            <div className="flex justify-end gap-2">
                {onDone && parentId ? (
                    <Button type="button" variant="ghost" size="sm" onClick={onDone}>
                        Huỷ
                    </Button>
                ) : null}
                <Button type="submit" size="sm" disabled={!body.trim() || write.isPending}>
                    Gửi
                </Button>
            </div>
        </form>
    );
}
