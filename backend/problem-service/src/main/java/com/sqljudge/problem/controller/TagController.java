package com.sqljudge.problem.controller;

import com.sqljudge.problem.dto.response.TagsResponse;
import com.sqljudge.problem.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping
    @PreAuthorize("permitAll()")
    @Operation(summary = "Danh sách tag để lọc bài tập")
    @ApiResponse(responseCode = "200", description = "Mọi tag, xếp theo tên")
    public TagsResponse listTags() {
        return tagService.listTags();
    }
}
