import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { aiApi } from "../api";
import type { MessageExchange } from "../types";

const AI_KEY = ["ai"] as const;

export function useAiQuota(isEnabled: boolean) {
    return useQuery({
        queryKey: [...AI_KEY, "quota"],
        queryFn: ({ signal }) => aiApi.quota(signal),
        enabled: isEnabled,
        retry: false,
    });
}

export function useConversations(isEnabled: boolean) {
    return useQuery({
        queryKey: [...AI_KEY, "conversations"],
        queryFn: ({ signal }) => aiApi.conversations(signal),
        enabled: isEnabled,
        retry: false,
    });
}

export function useConversation(conversationId: string | null) {
    return useQuery({
        queryKey: [...AI_KEY, "conversation", conversationId],
        queryFn: ({ signal }) => aiApi.conversation(conversationId ?? "", signal),
        enabled: conversationId !== null,
        retry: false,
    });
}

export function useAssistantProblem(slug: string) {
    return useQuery({
        queryKey: [...AI_KEY, "problem", slug],
        queryFn: ({ signal }) => aiApi.problem(slug, signal),
        staleTime: 5 * 60_000,
    });
}

interface Question {
    conversationId: string | null;
    problemId: string | null;
    content: string;
    code?: string;
}

export function useAskAssistant(onStarted: (conversationId: string) => void) {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ conversationId, problemId, content, code }: Question) => {
            const id = conversationId ?? (await aiApi.start(problemId)).id;
            if (conversationId === null) {
                onStarted(id);
            }
            return { id, exchange: await aiApi.ask({ conversationId: id, content, code }) };
        },
        onSuccess: ({ id, exchange }: { id: string; exchange: MessageExchange }) => {
            queryClient.setQueryData([...AI_KEY, "quota"], exchange.quota);
            return Promise.all([
                queryClient.invalidateQueries({ queryKey: [...AI_KEY, "conversation", id] }),
                queryClient.invalidateQueries({ queryKey: [...AI_KEY, "conversations"] }),
            ]);
        },
    });
}
