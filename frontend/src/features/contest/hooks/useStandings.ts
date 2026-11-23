import { useEffect } from "react";

import { keepPreviousData, useQuery, useQueryClient } from "@tanstack/react-query";

import { streamEvents } from "@/lib/sse";

import { contestApi } from "../api";

const STANDINGS_KEY = "contest-standings";
const MY_STANDING_KEY = "contest-my-standing";
const RECONNECT_DELAY_MS = 2000;
const MAX_RECONNECT_DELAY_MS = 30_000;
const STANDINGS_EVENT = "standings";

export function useStandings(slug: string, page: number) {
    return useQuery({
        queryKey: [STANDINGS_KEY, slug, page],
        queryFn: ({ signal }) => contestApi.standings(slug, page, signal),
        placeholderData: keepPreviousData,
    });
}

export function useMyStanding(slug: string, isSignedIn: boolean) {
    return useQuery({
        queryKey: [MY_STANDING_KEY, slug],
        queryFn: ({ signal }) => contestApi.myStanding(slug, signal),
        enabled: isSignedIn,
        retry: false,
    });
}

export function useStandingsEvents(slug: string) {
    const queryClient = useQueryClient();

    useEffect(() => {
        const controller = new AbortController();
        let reconnectTimer: ReturnType<typeof setTimeout> | undefined;
        let attempts = 0;

        function refresh() {
            attempts = 0;
            void queryClient.invalidateQueries({ queryKey: [STANDINGS_KEY, slug] });
            void queryClient.invalidateQueries({ queryKey: [MY_STANDING_KEY, slug] });
        }

        function reconnectLater() {
            if (controller.signal.aborted) {
                return;
            }
            attempts += 1;
            const delay = Math.min(RECONNECT_DELAY_MS * attempts, MAX_RECONNECT_DELAY_MS);
            reconnectTimer = setTimeout(connect, delay);
        }

        function connect() {
            streamEvents(`/api/contests/${encodeURIComponent(slug)}/standings/events`, {
                signal: controller.signal,
                onEvent: (event) => {
                    if (event.event === STANDINGS_EVENT) {
                        refresh();
                    }
                },
            }).then(reconnectLater, reconnectLater);
        }

        connect();
        return () => {
            controller.abort();
            clearTimeout(reconnectTimer);
        };
    }, [slug, queryClient]);
}
