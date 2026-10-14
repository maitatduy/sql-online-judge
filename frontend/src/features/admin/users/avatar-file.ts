export const MAX_AVATAR_BYTES = 2 * 1024 * 1024;
export const ACCEPTED_AVATAR_TYPES = ["image/png", "image/jpeg", "image/webp"];

export type AvatarChange =
    { kind: "keep" } | { kind: "replace"; file: File; previewUrl: string } | { kind: "remove" };

export function avatarFileError(file: File): string | null {
    if (!ACCEPTED_AVATAR_TYPES.includes(file.type)) {
        return "Ảnh phải là PNG, JPG hoặc WEBP";
    }
    return file.size > MAX_AVATAR_BYTES ? "Ảnh tối đa 2 MB" : null;
}

export function readAsDataUrl(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(String(reader.result));
        reader.onerror = () => reject(reader.error ?? new Error("Không đọc được ảnh"));
        reader.readAsDataURL(file);
    });
}
