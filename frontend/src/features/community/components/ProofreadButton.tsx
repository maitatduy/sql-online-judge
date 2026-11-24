"use client";

import { useMutation } from "@tanstack/react-query";
import { Sparkles, Undo2 } from "lucide-react";
import { useState } from "react";

import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";

import { aiErrorText } from "../ai-errors";
import { communityApi } from "../api";

interface ProofreadButtonProps {
    readText: () => string;
    onReplace: (text: string) => void;
}

export function ProofreadButton({ readText, onReplace }: ProofreadButtonProps) {
    const showToast = useToast();
    const [original, setOriginal] = useState<string | null>(null);
    const proofread = useMutation({ mutationFn: communityApi.proofread });

    if (original !== null) {
        return (
            <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={() => {
                    onReplace(original);
                    setOriginal(null);
                }}
            >
                <Undo2 aria-hidden="true" />
                Hoàn tác sửa
            </Button>
        );
    }

    const fix = () => {
        const text = readText();
        if (!text.trim()) {
            return;
        }
        proofread.mutate(text, {
            onSuccess: (result) => {
                setOriginal(text);
                onReplace(result.text);
                showToast("Đã sửa chính tả.");
            },
            onError: (error) => showToast(aiErrorText(error), "error"),
        });
    };

    return (
        <Button
            type="button"
            variant="ghost"
            size="sm"
            disabled={proofread.isPending}
            onClick={fix}
            className="text-ai hover:bg-ai-soft hover:text-ai"
        >
            <Sparkles aria-hidden="true" />
            {proofread.isPending ? "Đang sửa…" : "AI sửa chính tả"}
        </Button>
    );
}
