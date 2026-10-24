package com.sqljudge.auth.integration;

import com.sqljudge.auth.client.AvatarStorage;
import com.sqljudge.auth.client.AvatarUpload;
import com.sqljudge.auth.client.StoredAvatar;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
class InMemoryAvatarStorage implements AvatarStorage {

    private final Map<String, AvatarUpload> stored = new ConcurrentHashMap<>();

    @Override
    public StoredAvatar upload(AvatarUpload upload) {
        String publicId = "avatars/" + UUID.randomUUID();
        stored.put(publicId, upload);
        return new StoredAvatar("https://res.cloudinary.test/" + publicId, publicId);
    }

    @Override
    public void delete(String publicId) {
        stored.remove(publicId);
    }

    boolean contains(String publicId) {
        return stored.containsKey(publicId);
    }
}
