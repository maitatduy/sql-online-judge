import { useCurrentUser } from "@/lib/useCurrentUser";

export function useIsProfileOwner(username: string): boolean {
    const { data: currentUser } = useCurrentUser();
    return currentUser?.username === username;
}
