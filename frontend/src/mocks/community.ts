import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type {
    CommentItem,
    PostCategory,
    PostDetail,
    PostSummary,
    SavePostInput,
    VoteTarget,
} from "@/features/community/types";

import { apiError } from "./api-error";
import { findProblem } from "./practice-sql";
import { readMockSession } from "./session";

const AUTHOR_ID = "0192f3a0-0000-7000-8000-0000000000c1";

interface MockPost extends Omit<PostDetail, "myVote" | "isBookmarked"> {
    votes: Map<string, number>;
    savedBy: Set<string>;
}

interface MockComment extends Omit<CommentItem, "myVote" | "replies"> {
    votes: Map<string, number>;
}

let posts: MockPost[] = [];
let comments: MockComment[] = [];

function mockPost(
    index: number,
    category: PostCategory,
    title: string,
    bodyMarkdown: string,
    tags: string[],
): MockPost {
    const publishedAt = new Date(Date.UTC(2026, 10, 20 - index, 8)).toISOString();
    return {
        id: `0192f3a0-0000-7000-9000-00000000000${index}`,
        category,
        status: "PUBLISHED",
        title,
        bodyMarkdown,
        tags,
        problemId: null,
        problemSlug: null,
        problemTitle: null,
        authorId: AUTHOR_ID,
        authorUsername: "an.bui",
        voteScore: 12 - index * 2,
        commentCount: 0,
        viewCount: 240 - index * 30,
        isPinned: index === 1,
        publishedAt,
        updatedAt: publishedAt,
        votes: new Map(),
        savedBy: new Set(),
    };
}

export function resetCommunity() {
    posts = [
        mockPost(
            1,
            "BLOG",
            "Khi nào nên dùng window function thay cho GROUP BY",
            "Window function giữ nguyên số dòng.\n\n```sql\nSELECT name, RANK() OVER (ORDER BY salary DESC)\nFROM employees;\n```",
            ["window"],
        ),
        mockPost(
            2,
            "QUESTION",
            "LEFT JOIN trả về NULL ở cột đếm",
            "Mình dùng `COUNT(*)` sau LEFT JOIN nhưng phòng trống vẫn ra 1.",
            ["join", "group-by"],
        ),
        mockPost(
            3,
            "INTERVIEW",
            "Câu hỏi phỏng vấn: lương cao thứ hai",
            "Có những cách nào để lấy lương cao thứ hai?",
            ["subquery"],
        ),
    ];
    comments = [];
}

resetCommunity();

function viewerId(): string | null {
    return readMockSession()?.id ?? null;
}

function summaryOf(post: MockPost): PostSummary {
    return {
        id: post.id,
        category: post.category,
        title: post.title,
        excerpt: post.bodyMarkdown.slice(0, 160),
        tags: post.tags,
        problemId: post.problemId,
        authorId: post.authorId,
        authorUsername: post.authorUsername,
        voteScore: post.voteScore,
        commentCount: post.commentCount,
        isPinned: post.isPinned,
        publishedAt: post.publishedAt ?? post.updatedAt,
    };
}

function detailOf(post: MockPost, userId: string | null): PostDetail {
    const { votes, savedBy, ...detail } = post;
    return {
        ...detail,
        myVote: userId ? (votes.get(userId) ?? 0) : 0,
        isBookmarked: userId ? savedBy.has(userId) : false,
    };
}

function itemOf(comment: MockComment, userId: string | null): CommentItem {
    const { votes, ...item } = comment;
    return {
        ...item,
        myVote: userId ? (votes.get(userId) ?? 0) : 0,
        replies: comments
            .filter((reply) => reply.parentId === comment.id)
            .map((reply) => itemOf(reply, userId)),
    };
}

function visiblePosts(url: URL, userId: string | null): MockPost[] {
    const category = url.searchParams.get("category");
    const problemId = url.searchParams.get("problemId");
    const tab = url.searchParams.get("tab");
    const query = (url.searchParams.get("q") ?? "").toLowerCase();
    const matching = posts.filter(
        (post) =>
            post.status === "PUBLISHED" &&
            (!category || post.category === category) &&
            (!problemId || post.problemId === problemId) &&
            (!query || post.title.toLowerCase().includes(query)) &&
            (tab !== "UNANSWERED" || post.commentCount === 0) &&
            (tab !== "SAVED" || (userId !== null && post.savedBy.has(userId))),
    );
    if (tab === "NEW") {
        return [...matching].sort((a, b) => b.updatedAt.localeCompare(a.updatedAt));
    }
    return [...matching].sort((a, b) => Number(b.isPinned) - Number(a.isPinned));
}

function applyVote(votes: Map<string, number>, userId: string, value: number): number {
    const delta = value - (votes.get(userId) ?? 0);
    if (value === 0) {
        votes.delete(userId);
    } else {
        votes.set(userId, value);
    }
    return delta;
}

function voteOn(targetType: VoteTarget, targetId: string, userId: string, value: number) {
    const target =
        targetType === "POST"
            ? posts.find((post) => post.id === targetId)
            : comments.find((comment) => comment.id === targetId);
    if (!target) {
        return null;
    }
    target.voteScore += applyVote(target.votes, userId, value);
    return { voteScore: target.voteScore, myVote: value };
}

function contentOf(input: SavePostInput) {
    const problem = input.problemId ? findProblem(input.problemId) : null;
    return {
        category: input.category,
        title: input.title.trim(),
        bodyMarkdown: input.bodyMarkdown,
        tags: input.tags,
        problemId: problem?.id ?? null,
        problemSlug: problem?.slug ?? null,
        problemTitle: problem?.title ?? null,
    };
}

function unauthenticated() {
    return apiError(401, "UNAUTHORIZED", "Bạn cần đăng nhập");
}

export const communityHandlers: RequestHandler[] = [
    http.get("*/api/problems/:slug/editorial", async () => {
        await delay(100);
        return HttpResponse.json(
            readMockSession()
                ? {
                      status: "AVAILABLE",
                      editorialMarkdown:
                          "Gom nhóm theo `status` rồi đếm.\n\n```sql run\nSELECT status, COUNT(*) AS total\nFROM orders\nGROUP BY status;\n```",
                  }
                : { status: "LOCKED", editorialMarkdown: null },
        );
    }),
    http.get("*/api/community/overview", async () => {
        await delay(100);
        const categoryCounts: Partial<Record<PostCategory, number>> = {};
        posts.forEach((post) => {
            categoryCounts[post.category] = (categoryCounts[post.category] ?? 0) + 1;
        });
        return HttpResponse.json({ totalPosts: posts.length, categoryCounts });
    }),
    http.get("*/api/community/contributors", async () => {
        await delay(100);
        return HttpResponse.json([{ userId: AUTHOR_ID, username: "an.bui", points: 42 }]);
    }),
    http.get("*/api/community/problem-discussions", async ({ request }) => {
        await delay(100);
        const size = Number(new URL(request.url).searchParams.get("size") ?? 6);
        const discussions = [...comments]
            .reverse()
            .filter((comment) => comment.problemId)
            .flatMap((comment) => {
                const problem = findProblem(comment.problemId ?? "");
                return problem
                    ? [
                          {
                              commentId: comment.id,
                              problemId: problem.id,
                              problemSlug: problem.slug,
                              problemTitle: problem.title,
                              authorId: comment.authorId,
                              authorUsername: comment.authorUsername,
                              excerpt: comment.body,
                              isReply: comment.parentId !== null,
                              createdAt: comment.createdAt,
                          },
                      ]
                    : [];
            })
            .slice(0, size);
        return HttpResponse.json(discussions);
    }),
    http.get("*/api/community/posts", async ({ request }) => {
        await delay(150);
        const url = new URL(request.url);
        const page = Number(url.searchParams.get("page") ?? 1);
        const size = Number(url.searchParams.get("size") ?? 20);
        const matching = visiblePosts(url, viewerId());
        return HttpResponse.json({
            items: matching.slice((page - 1) * size, page * size).map(summaryOf),
            page,
            size,
            totalItems: matching.length,
            totalPages: Math.max(1, Math.ceil(matching.length / size)),
        });
    }),
    http.get("*/api/community/posts/:postId", async ({ params }) => {
        await delay(100);
        const post = posts.find((candidate) => candidate.id === params.postId);
        return post
            ? HttpResponse.json(detailOf(post, viewerId()))
            : apiError(404, "POST_NOT_FOUND", "Không tìm thấy bài viết");
    }),
    http.post("*/api/community/posts", async ({ request }) => {
        const session = readMockSession();
        if (!session) {
            return unauthenticated();
        }
        const input = (await request.json()) as SavePostInput;
        const now = new Date().toISOString();
        const post: MockPost = {
            ...contentOf(input),
            id: crypto.randomUUID(),
            status: input.publish ? "PUBLISHED" : "DRAFT",
            authorId: session.id,
            authorUsername: session.username,
            voteScore: 0,
            commentCount: 0,
            viewCount: 0,
            isPinned: false,
            publishedAt: input.publish ? now : null,
            updatedAt: now,
            votes: new Map(),
            savedBy: new Set(),
        };
        posts.push(post);
        return HttpResponse.json(detailOf(post, session.id), { status: 201 });
    }),
    http.put("*/api/community/posts/:postId", async ({ params, request }) => {
        const userId = viewerId();
        const post = posts.find((candidate) => candidate.id === params.postId);
        if (!post || post.authorId !== userId) {
            return apiError(404, "POST_NOT_FOUND", "Không tìm thấy bài viết");
        }
        const input = (await request.json()) as SavePostInput;
        Object.assign(post, contentOf(input), { updatedAt: new Date().toISOString() });
        if (input.publish && post.status === "DRAFT") {
            post.status = "PUBLISHED";
            post.publishedAt = post.updatedAt;
        }
        return HttpResponse.json(detailOf(post, userId));
    }),
    http.delete("*/api/community/posts/:postId", ({ params }) => {
        const userId = viewerId();
        const post = posts.find((candidate) => candidate.id === params.postId);
        if (!post || post.authorId !== userId) {
            return apiError(404, "POST_NOT_FOUND", "Không tìm thấy bài viết");
        }
        posts = posts.filter((candidate) => candidate.id !== post.id);
        return new HttpResponse(null, { status: 204 });
    }),
    http.put("*/api/community/posts/:postId/bookmark", ({ params }) => {
        const userId = viewerId();
        if (!userId) {
            return unauthenticated();
        }
        posts.find((post) => post.id === params.postId)?.savedBy.add(userId);
        return new HttpResponse(null, { status: 204 });
    }),
    http.delete("*/api/community/posts/:postId/bookmark", ({ params }) => {
        const userId = viewerId();
        if (!userId) {
            return unauthenticated();
        }
        posts.find((post) => post.id === params.postId)?.savedBy.delete(userId);
        return new HttpResponse(null, { status: 204 });
    }),
    http.get("*/api/community/comments", async ({ request }) => {
        await delay(100);
        const url = new URL(request.url);
        const postId = url.searchParams.get("postId");
        const problemId = url.searchParams.get("problemId");
        const isNewest = url.searchParams.get("sort") === "NEW";
        const roots = comments
            .filter(
                (comment) =>
                    comment.parentId === null &&
                    comment.postId === postId &&
                    comment.problemId === problemId,
            )
            .sort((a, b) =>
                isNewest ? b.createdAt.localeCompare(a.createdAt) : b.voteScore - a.voteScore,
            );
        return HttpResponse.json({
            items: roots.map((comment) => itemOf(comment, viewerId())),
            page: 1,
            size: 50,
            totalItems: roots.length,
            totalPages: 1,
        });
    }),
    http.post("*/api/community/comments", async ({ request }) => {
        const session = readMockSession();
        if (!session) {
            return unauthenticated();
        }
        const body = (await request.json()) as {
            postId?: string;
            problemId?: string;
            parentId?: string;
            body: string;
        };
        const now = new Date().toISOString();
        comments.push({
            id: crypto.randomUUID(),
            postId: body.postId ?? null,
            problemId: body.problemId ?? null,
            parentId: body.parentId ?? null,
            authorId: session.id,
            authorUsername: session.username,
            body: body.body,
            voteScore: 0,
            createdAt: now,
            updatedAt: now,
            votes: new Map(),
        });
        const post = posts.find((candidate) => candidate.id === body.postId);
        if (post) {
            post.commentCount += 1;
        }
        return new HttpResponse(null, { status: 201 });
    }),
    http.delete("*/api/community/comments/:commentId", ({ params }) => {
        const userId = viewerId();
        const comment = comments.find((candidate) => candidate.id === params.commentId);
        if (!comment || comment.authorId !== userId) {
            return apiError(404, "COMMENT_NOT_FOUND", "Không tìm thấy bình luận");
        }
        comments = comments.filter(
            (candidate) => candidate.id !== comment.id && candidate.parentId !== comment.id,
        );
        const post = posts.find((candidate) => candidate.id === comment.postId);
        if (post) {
            post.commentCount = Math.max(0, post.commentCount - 1);
        }
        return new HttpResponse(null, { status: 204 });
    }),
    http.put("*/api/community/votes", async ({ request }) => {
        const userId = viewerId();
        if (!userId) {
            return unauthenticated();
        }
        const vote = (await request.json()) as {
            targetType: VoteTarget;
            targetId: string;
            value: number;
        };
        const result = voteOn(vote.targetType, vote.targetId, userId, vote.value);
        return result
            ? HttpResponse.json(result)
            : apiError(404, "POST_NOT_FOUND", "Không tìm thấy bài viết");
    }),
    http.post("*/api/community/reports", () =>
        viewerId() ? new HttpResponse(null, { status: 201 }) : unauthenticated(),
    ),
];
