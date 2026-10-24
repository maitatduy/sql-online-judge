package com.sqljudge.auth.client;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.sqljudge.auth.config.CloudinaryProperties;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.ServiceUnavailableException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableConfigurationProperties(CloudinaryProperties.class)
public class CloudinaryAvatarStorage implements AvatarStorage {

    private final Cloudinary cloudinary;
    private final CloudinaryProperties properties;

    public CloudinaryAvatarStorage(CloudinaryProperties properties) {
        this.properties = properties;
        this.cloudinary =
                new Cloudinary(
                        ObjectUtils.asMap(
                                "cloud_name", properties.cloudName(),
                                "api_key", properties.apiKey(),
                                "api_secret", properties.apiSecret(),
                                "secure", true));
    }

    @Override
    public StoredAvatar upload(AvatarUpload upload) {
        try {
            Map<?, ?> result =
                    cloudinary
                            .uploader()
                            .upload(
                                    upload.bytes(),
                                    ObjectUtils.asMap(
                                            "public_id", UUID.randomUUID().toString(),
                                            "folder", properties.folder(),
                                            "resource_type", "image",
                                            "overwrite", false,
                                            "timeout", properties.timeoutSeconds()));
            return new StoredAvatar(
                    (String) result.get("secure_url"), (String) result.get("public_id"));
        } catch (IOException | RuntimeException ex) {
            log.warn("avatar_upload_failed format={}", upload.format(), ex);
            throw new ServiceUnavailableException(
                    ErrorCode.AVATAR_STORAGE_UNAVAILABLE,
                    "Chưa lưu được ảnh, vui lòng thử lại sau");
        }
    }

    @Override
    public void delete(String publicId) {
        try {
            cloudinary
                    .uploader()
                    .destroy(
                            publicId,
                            ObjectUtils.asMap(
                                    "resource_type",
                                    "image",
                                    "invalidate",
                                    true,
                                    "timeout",
                                    properties.timeoutSeconds()));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
