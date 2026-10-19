package com.sqljudge.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sqljudge.auth.validation.ValidUserUpdate;
import com.sqljudge.common.security.Role;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@ValidUserUpdate
public class UpdateUserRequest {

    private Role role;

    @JsonIgnore private boolean roleProvided;

    private String displayName;

    @JsonIgnore private boolean displayNameProvided;

    @Size(max = 500, message = "Tối đa 500 ký tự") private String reason;

    public void setRole(Role role) {
        this.role = role;
        this.roleProvided = true;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        this.displayNameProvided = true;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
