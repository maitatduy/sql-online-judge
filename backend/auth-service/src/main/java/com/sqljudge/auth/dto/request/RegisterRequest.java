package com.sqljudge.auth.dto.request;

import com.sqljudge.auth.validation.NotReservedUsername;
import com.sqljudge.auth.validation.UsernameRules;
import com.sqljudge.auth.validation.ValidPassword;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Vui lòng nhập tên người dùng") @Pattern(regexp = UsernameRules.PATTERN, message = UsernameRules.PATTERN_MESSAGE) @NotReservedUsername
                String username,
        @NotBlank(message = "Vui lòng nhập email") @Email(message = "Email không hợp lệ") @Size(max = 254, message = "Email quá dài") String email,
        @NotBlank(message = "Vui lòng nhập mật khẩu") @ValidPassword String password,
        @AssertTrue(message = "Bạn cần đồng ý với điều khoản để tiếp tục") boolean acceptedTerms) {}
