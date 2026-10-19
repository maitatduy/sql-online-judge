package com.sqljudge.common.unit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sqljudge.common.web.GlobalExceptionHandler;
import com.sqljudge.common.web.OptimisticLockExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class OptimisticLockExceptionHandlerTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-10-19T05:00:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName(
            "Hai người cùng sửa một bản ghi: người ghi sau nhận 409 CONCURRENT_MODIFICATION, không"
                    + " phải 500")
    void optimisticLockFailure_returnsConflict() throws Exception {
        MockMvc mockMvc =
                MockMvcBuilders.standaloneSetup(new StaleWriteController())
                        .setControllerAdvice(
                                new GlobalExceptionHandler(CLOCK),
                                new OptimisticLockExceptionHandler(CLOCK))
                        .setMessageConverters(
                                new MappingJackson2HttpMessageConverter(
                                        JsonMapper.builder()
                                                .addModule(new JavaTimeModule())
                                                .disable(
                                                        SerializationFeature
                                                                .WRITE_DATES_AS_TIMESTAMPS)
                                                .build()))
                        .build();

        mockMvc.perform(get("/stale-write"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"))
                .andExpect(
                        jsonPath("$.message")
                                .value("Dữ liệu vừa được người khác sửa, vui lòng tải lại"));
    }

    @RestController
    static class StaleWriteController {

        @GetMapping("/stale-write")
        void staleWrite() {
            throw new ObjectOptimisticLockingFailureException(Object.class, "id");
        }
    }
}
