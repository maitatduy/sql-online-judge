import { useQuery } from "@tanstack/react-query";

import { useDebouncedValue } from "@/lib/useDebouncedValue";

import { authApi } from "../api";
import { USERNAME_PATTERN } from "@/lib/username";

const DEBOUNCE_DELAY_MS = 400;

export type UsernameAvailabilityStatus = "idle" | "checking" | "available" | "taken";

export function useUsernameAvailability(username: string): UsernameAvailabilityStatus {
    const typedUsername = username.trim();
    const settledUsername = useDebouncedValue(typedUsername, DEBOUNCE_DELAY_MS);
    const isTypedValid = USERNAME_PATTERN.test(typedUsername);
    const isSettled = typedUsername === settledUsername;

    const { data, isPending, isError } = useQuery({
        queryKey: ["username-availability", settledUsername],
        queryFn: ({ signal }) => authApi.checkUsername(settledUsername, signal),
        enabled: isTypedValid && isSettled,
    });

    if (!isTypedValid || isError) {
        return "idle";
    }
    if (!isSettled || isPending || !data) {
        return "checking";
    }
    return data.available ? "available" : "taken";
}
