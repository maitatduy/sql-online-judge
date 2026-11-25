import type { Dialect } from "@/lib/dialect";
import type { Page } from "@/lib/pagination";
import type { ResultSet } from "@/lib/result-set";

export type PostCategory = "QUESTION" | "SOLUTION" | "INTERVIEW" | "BLOG" | "FEEDBACK";
export type PostTab = "HOT" | "NEW" | "UNANSWERED" | "SAVED";
export type CommentSort = "TOP" | "NEW";
export type ReportReason = "SPAM" | "OFFENSIVE" | "CONTEST_SPOILER" | "PLAGIARISM" | "OTHER";
export type ReportTarget = "POST" | "COMMENT" | "USER";
export type VoteTarget = "POST" | "COMMENT";

export interface PostSummary {
    id: string;
    category: PostCategory;
    title: string;
    excerpt: string;
    tags: string[];
    problemId: string | null;
    authorId: string;
    authorUsername: string | null;
    voteScore: number;
    commentCount: number;
    isPinned: boolean;
    publishedAt: string;
}

export type PostPage = Page<PostSummary>;

export interface PostDetail {
    id: string;
    category: PostCategory;
    status: "DRAFT" | "PUBLISHED" | "HIDDEN";
    title: string;
    bodyMarkdown: string;
    tags: string[];
    problemId: string | null;
    problemSlug: string | null;
    problemTitle: string | null;
    authorId: string;
    authorUsername: string | null;
    voteScore: number;
    commentCount: number;
    viewCount: number;
    isPinned: boolean;
    publishedAt: string | null;
    updatedAt: string;
    myVote: number;
    isBookmarked: boolean;
}

export interface CommunityOverview {
    totalPosts: number;
    categoryCounts: Partial<Record<PostCategory, number>>;
}

export interface Contributor {
    userId: string;
    username: string | null;
    points: number;
}

export interface ProblemDiscussion {
    commentId: string;
    problemId: string;
    problemSlug: string;
    problemTitle: string;
    authorId: string;
    authorUsername: string | null;
    excerpt: string;
    isReply: boolean;
    createdAt: string;
}

export interface CommentItem {
    id: string;
    postId: string | null;
    problemId: string | null;
    parentId: string | null;
    authorId: string;
    authorUsername: string | null;
    body: string;
    voteScore: number;
    myVote: number;
    createdAt: string;
    updatedAt: string;
    replies: CommentItem[];
}

export type CommentPage = Page<CommentItem>;

export interface CommentThread {
    postId?: string;
    problemId?: string;
}

export interface SavePostInput {
    category: PostCategory;
    title: string;
    bodyMarkdown: string;
    problemId: string | null;
    tags: string[];
    publish: boolean;
}

export interface PickedProblem {
    id: string;
    slug: string;
    title: string;
}

export interface ProblemOption extends PickedProblem {
    number: number;
}

export interface LinkedProblem extends ProblemOption {
    dialects: Dialect[];
}

export interface CodeRun {
    id: string;
    status: "PENDING" | "COMPLETED";
    outcome: "OK" | "RE" | "TLE" | "FS" | "SE" | null;
    result: ResultSet | null;
    errorMessage: string | null;
    runtimeMs: number | null;
}

export interface ProblemEditorial {
    status: "AVAILABLE" | "LOCKED" | "NONE";
    editorialMarkdown: string | null;
}

export interface AiText {
    text: string;
}

export interface VoteResult {
    voteScore: number;
    myVote: number;
}
