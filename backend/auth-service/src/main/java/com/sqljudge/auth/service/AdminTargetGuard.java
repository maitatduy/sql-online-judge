package com.sqljudge.auth.service;

import com.sqljudge.auth.entity.User;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import com.sqljudge.common.security.Role;
import java.util.UUID;

final class AdminTargetGuard {

    enum Action {
        BAN(
                "Bạn không thể tự khoá tài khoản của chính mình",
                "Không thể khoá tài khoản quản trị viên khác"),
        DELETE(
                "Bạn không thể tự xoá tài khoản của chính mình",
                "Không thể xoá tài khoản quản trị viên khác"),
        CHANGE_ROLE(
                "Bạn không thể tự đổi vai trò của chính mình",
                "Không thể đổi vai trò của quản trị viên khác"),
        RESTRICT(
                "Bạn không thể tự đặt hạn chế cho chính mình",
                "Không thể đặt hạn chế cho quản trị viên khác"),
        REVOKE_SESSIONS(null, "Không thể đăng xuất phiên của quản trị viên khác");

        private final String selfMessage;
        private final String otherAdminMessage;

        Action(String selfMessage, String otherAdminMessage) {
            this.selfMessage = selfMessage;
            this.otherAdminMessage = otherAdminMessage;
        }
    }

    private AdminTargetGuard() {}

    static void requireModerable(User target, UUID actorId, Action action) {
        if (target.getId().equals(actorId)) {
            if (action.selfMessage != null) {
                throw new BusinessRuleException(ErrorCode.CANNOT_MODIFY_SELF, action.selfMessage);
            }
            return;
        }
        if (target.getRole() == Role.ADMIN) {
            throw new BusinessRuleException(
                    ErrorCode.CANNOT_MODIFY_ADMIN, action.otherAdminMessage);
        }
    }
}
