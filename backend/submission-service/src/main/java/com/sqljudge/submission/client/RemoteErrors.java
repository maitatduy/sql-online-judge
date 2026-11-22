package com.sqljudge.submission.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.common.error.ErrorCode;
import feign.FeignException;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;

final class RemoteErrors {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Set<Integer> RULE_STATUSES = Set.of(403, 404, 409, 422);

    private RemoteErrors() {}

    static Optional<RemoteRuleException> ruleViolationOf(Throwable cause) {
        if (!(cause instanceof FeignException feignError)
                || !RULE_STATUSES.contains(feignError.status())) {
            return Optional.empty();
        }
        try {
            JsonNode body = OBJECT_MAPPER.readTree(feignError.contentUTF8());
            String code = body.path("code").asText();
            return Arrays.stream(ErrorCode.values())
                    .filter(candidate -> candidate.name().equals(code))
                    .findFirst()
                    .map(
                            errorCode ->
                                    new RemoteRuleException(
                                            errorCode,
                                            HttpStatus.valueOf(feignError.status()),
                                            body.path("message").asText()));
        } catch (Exception unreadableBody) {
            return Optional.empty();
        }
    }
}
