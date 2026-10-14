export const USERNAME_PATTERN = /^[a-z0-9._]{3,30}$/;
export const RESERVED_USERNAME_MESSAGE = "Tên người dùng này được hệ thống dành riêng";

const RESERVED_USERNAMES = new Set(["availability", "me"]);

export function isReservedUsername(username: string): boolean {
    return RESERVED_USERNAMES.has(username);
}
