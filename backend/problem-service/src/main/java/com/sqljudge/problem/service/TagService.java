package com.sqljudge.problem.service;

import com.sqljudge.problem.dto.response.TagResponse;
import com.sqljudge.problem.dto.response.TagsResponse;
import com.sqljudge.problem.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    @Transactional(readOnly = true)
    public TagsResponse listTags() {
        return new TagsResponse(
                tagRepository.findAllByOrderByNameAsc().stream().map(TagResponse::from).toList());
    }
}
