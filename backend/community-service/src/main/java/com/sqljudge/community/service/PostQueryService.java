package com.sqljudge.community.service;

import com.sqljudge.common.web.PageResponse;
import com.sqljudge.community.dto.response.CommunityOverviewResponse;
import com.sqljudge.community.dto.response.PostDetailResponse;
import com.sqljudge.community.dto.response.PostSummaryResponse;
import com.sqljudge.community.entity.PostCategory;
import com.sqljudge.community.entity.PostStatus;
import com.sqljudge.community.repository.PostRepository;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostQueryService {

    private static final int EXCERPT_LENGTH = 220;
    private static final Pattern CODE_BLOCK = Pattern.compile("```[\\s\\S]*?```");
    private static final Pattern MARKDOWN_SYMBOLS = Pattern.compile("[#*_>`\\[\\]()!-]+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final PostReader postReader;
    private final PostRepository postRepository;
    private final AuthorNames authorNames;

    public PageResponse<PostSummaryResponse> list(PostSearch search, int page, int size) {
        Page<PostSnapshot> posts =
                postReader.listed(search, PageRequest.of(page - 1, size, sortOf(search.tab())));
        Map<UUID, String> authors = authorNames.of(posts.map(PostSnapshot::authorId).toSet());
        return PageResponse.from(posts, post -> toSummary(post, authors));
    }

    public PostDetailResponse detail(UUID postId) {
        PostSnapshot post = postReader.readable(postId);
        PostReader.ViewerState viewer = postReader.viewerState(postId);
        String author = authorNames.of(Set.of(post.authorId())).get(post.authorId());
        return new PostDetailResponse(
                post.id(),
                post.category(),
                post.status(),
                post.title(),
                post.bodyMarkdown(),
                post.tags(),
                post.problemId(),
                post.problemSlug(),
                post.problemTitle(),
                post.authorId(),
                author,
                post.voteScore(),
                post.commentCount(),
                post.viewCount(),
                post.pinned(),
                post.publishedAt(),
                post.updatedAt(),
                viewer.myVote(),
                viewer.isBookmarked());
    }

    @Transactional(readOnly = true)
    public CommunityOverviewResponse overview() {
        Map<PostCategory, Long> counts = new EnumMap<>(PostCategory.class);
        postRepository
                .countByCategory(PostStatus.PUBLISHED)
                .forEach(row -> counts.put(row.getCategory(), row.getTotal()));
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        return new CommunityOverviewResponse(total, counts);
    }

    private static PostSummaryResponse toSummary(PostSnapshot post, Map<UUID, String> authors) {
        return new PostSummaryResponse(
                post.id(),
                post.category(),
                post.title(),
                excerptOf(post.bodyMarkdown()),
                post.tags(),
                post.problemId(),
                post.authorId(),
                authors.get(post.authorId()),
                post.voteScore(),
                post.commentCount(),
                post.pinned(),
                post.publishedAt());
    }

    static String excerptOf(String markdown) {
        String text = CODE_BLOCK.matcher(markdown).replaceAll(" ");
        text = MARKDOWN_SYMBOLS.matcher(text).replaceAll(" ");
        text = WHITESPACE.matcher(text).replaceAll(" ").trim();
        return text.length() <= EXCERPT_LENGTH ? text : text.substring(0, EXCERPT_LENGTH) + "…";
    }

    private static Sort sortOf(PostTab tab) {
        return tab == PostTab.HOT
                ? Sort.by(
                        Sort.Order.desc("pinned"),
                        Sort.Order.desc("voteScore"),
                        Sort.Order.desc("publishedAt"))
                : Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("publishedAt"));
    }
}
