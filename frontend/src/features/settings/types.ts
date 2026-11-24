export interface AccountProfile {
    username: string;
    email: string;
    displayName: string;
    bio: string | null;
    school: string | null;
    location: string | null;
    websiteUrl: string | null;
    avatarUrl: string | null;
    hasPassword: boolean;
}

export interface UpdateAccountProfileRequest {
    displayName: string;
    bio: string;
    school: string;
    location: string;
    websiteUrl: string;
}

export interface ChangePasswordRequest {
    currentPassword?: string;
    newPassword: string;
}

export interface AccountAvatar {
    avatarUrl: string;
}
