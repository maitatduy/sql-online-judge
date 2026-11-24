package com.sqljudge.problem.dto.response;

import com.sqljudge.problem.entity.EditorialStatus;

public record ProblemEditorialResponse(EditorialStatus status, String editorialMarkdown) {

    public static ProblemEditorialResponse available(String editorialMarkdown) {
        return new ProblemEditorialResponse(EditorialStatus.AVAILABLE, editorialMarkdown);
    }

    public static ProblemEditorialResponse withheld(EditorialStatus status) {
        return new ProblemEditorialResponse(status, null);
    }
}
