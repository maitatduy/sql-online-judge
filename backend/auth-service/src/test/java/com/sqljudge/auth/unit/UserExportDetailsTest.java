package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.auth.dto.request.ExportUsersRequest;
import com.sqljudge.auth.service.UserExportDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserExportDetailsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @DisplayName("Chi tiết nhật ký xuất theo bộ lọc chỉ có phạm vi, số dòng, bộ lọc; từ khoá bị ẩn")
    void filtered_hidesKeyword() throws Exception {
        ExportUsersRequest request =
                parse(
                        "{\"scope\":\"FILTERED\",\"filters\":{\"q\":\"an.bui@gmail.com\","
                                + "\"role\":\"SETTER\",\"status\":\"BANNED\","
                                + "\"emailVerified\":true,\"deleted\":false},"
                                + "\"includeEmail\":true,\"reason\":\"x\"}");

        String details = UserExportDetails.of(request, 12);

        assertThat(details)
                .isEqualTo(
                        "scope=FILTERED;rows=12;q=*;role=SETTER;status=BANNED;emailVerified=true;deleted=false");
        assertThat(details).doesNotContain("an.bui");
    }

    @Test
    @DisplayName("Chi tiết nhật ký xuất theo danh sách chọn không ghi username nào")
    void selected_omitsUsernames() throws Exception {
        ExportUsersRequest request =
                parse(
                        "{\"scope\":\"SELECTED\",\"usernames\":[\"an.bui\",\"viet.luu\"],"
                                + "\"includeEmail\":true,\"reason\":\"x\"}");

        assertThat(UserExportDetails.of(request, 2)).isEqualTo("scope=SELECTED;rows=2");
    }

    @Test
    @DisplayName("Bộ lọc trống chỉ ghi phạm vi và số dòng")
    void emptyFilters_onlyScopeAndRows() throws Exception {
        ExportUsersRequest request =
                parse(
                        "{\"scope\":\"FILTERED\",\"filters\":{},\"includeEmail\":true,\"reason\":\"x\"}");

        assertThat(UserExportDetails.of(request, 0)).isEqualTo("scope=FILTERED;rows=0");
    }

    private static ExportUsersRequest parse(String json) throws Exception {
        return MAPPER.readValue(json, ExportUsersRequest.class);
    }
}
