package com.sqljudge.auth.client;

public interface AvatarStorage {

    StoredAvatar upload(AvatarUpload upload);

    void delete(String publicId);
}
