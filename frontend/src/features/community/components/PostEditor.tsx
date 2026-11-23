"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";

import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ApiRequestError } from "@/lib/api-client";
import { ROUTES } from "@/lib/routes";
import { cn } from "@/lib/utils";

import { communityApi } from "../api";
import { useCommunityMutation } from "../hooks/useCommunity";
import { POST_CATEGORIES, POST_CATEGORY_LABELS } from "../labels";
import { parseTags, postSchema, type PostValues } from "../schemas";
import type { PickedProblem, PostDetail, SavePostInput } from "../types";
import { PostBody } from "./PostBody";
import { ProblemPicker } from "./ProblemPicker";

type PostField = keyof PostValues;

const FIELDS: readonly PostField[] = ["category", "title", "bodyMarkdown", "tags"];
const LABEL_CLASS = "text-[15px] font-semibold";

interface PostEditorProps {
    post?: PostDetail;
    initialProblem?: PickedProblem | null;
}

function problemOf(post: PostDetail | undefined): PickedProblem | null {
    if (!post?.problemId) {
        return null;
    }
    return { id: post.problemId, slug: post.problemSlug ?? "", title: post.problemTitle ?? "" };
}

export function PostEditor({ post, initialProblem = null }: PostEditorProps) {
    const router = useRouter();
    const showToast = useToast();
    const [problem, setProblem] = useState<PickedProblem | null>(problemOf(post) ?? initialProblem);
    const [isPreview, setIsPreview] = useState(false);
    const create = useCommunityMutation(communityApi.createPost);
    const update = useCommunityMutation(communityApi.updatePost);
    const isDraft = !post || post.status === "DRAFT";
    const {
        register,
        handleSubmit,
        watch,
        setValue,
        setError,
        formState: { errors },
    } = useForm<PostValues>({
        resolver: zodResolver(postSchema),
        defaultValues: {
            category: post?.category ?? (initialProblem ? "SOLUTION" : "QUESTION"),
            title: post?.title ?? "",
            bodyMarkdown: post?.bodyMarkdown ?? "",
            tags: post?.tags.join(", ") ?? "",
        },
    });
    const category = watch("category");

    const applyFieldErrors = (error: Error) => {
        if (!(error instanceof ApiRequestError)) {
            return false;
        }
        const fieldErrors = error.errors.filter((fieldError) =>
            FIELDS.includes(fieldError.field as PostField),
        );
        fieldErrors.forEach((fieldError) =>
            setError(fieldError.field as PostField, { message: fieldError.message }),
        );
        return fieldErrors.length > 0;
    };

    const save = (publish: boolean) =>
        handleSubmit((values) => {
            const input: SavePostInput = {
                ...values,
                tags: parseTags(values.tags),
                problemId: problem?.id ?? null,
                publish,
            };
            const saved = post
                ? update.mutateAsync({ postId: post.id, post: input })
                : create.mutateAsync(input);
            return saved
                .then((result) => {
                    showToast(publish ? "Đã đăng bài." : "Đã lưu nháp.");
                    router.push(ROUTES.communityPost(result.id));
                })
                .catch((error: Error) => {
                    if (!applyFieldErrors(error)) {
                        showToast(error.message, "error");
                    }
                });
        });

    const fieldError = (field: PostField) =>
        errors[field] ? (
            <p id={`post-${field}-error`} className="text-sm text-destructive">
                {errors[field]?.message}
            </p>
        ) : null;

    const invalidProps = (field: PostField) =>
        errors[field] ? { "aria-invalid": true, "aria-describedby": `post-${field}-error` } : {};

    const isSaving = create.isPending || update.isPending;

    return (
        <form
            onSubmit={(event) => void save(true)(event)}
            className="mx-auto flex w-full max-w-[880px] flex-col gap-5"
        >
            <h1 className="text-[28px] font-extrabold">{post ? "Sửa bài viết" : "Viết bài"}</h1>
            <fieldset className="flex flex-col gap-2">
                <legend className={cn(LABEL_CLASS, "mb-2")}>Chuyên mục</legend>
                <div className="flex flex-wrap gap-2">
                    {POST_CATEGORIES.map((option) => (
                        <label
                            key={option}
                            className={cn(
                                "cursor-pointer rounded-full border px-3.5 py-1.5 text-[15px] has-focus-visible:ring-2 has-focus-visible:ring-ring",
                                category === option
                                    ? "border-brand bg-brand-soft font-semibold text-brand"
                                    : "text-muted-foreground",
                            )}
                        >
                            <input
                                type="radio"
                                value={option}
                                className="sr-only"
                                checked={category === option}
                                onChange={() => setValue("category", option)}
                            />
                            {POST_CATEGORY_LABELS[option]}
                        </label>
                    ))}
                </div>
            </fieldset>
            <div className="flex flex-col gap-2">
                <label htmlFor="post-title" className={LABEL_CLASS}>
                    Tiêu đề
                </label>
                <Input
                    id="post-title"
                    className="h-11 text-base"
                    {...register("title")}
                    {...invalidProps("title")}
                />
                {fieldError("title")}
            </div>
            <div className="grid gap-5 sm:grid-cols-2">
                <div className="flex flex-col gap-2">
                    <span className={LABEL_CLASS}>Bài tập liên quan</span>
                    <ProblemPicker value={problem} onChange={setProblem} />
                </div>
                <div className="flex flex-col gap-2">
                    <label htmlFor="post-tags" className={LABEL_CLASS}>
                        Tag
                    </label>
                    <Input
                        id="post-tags"
                        placeholder="join, window-function"
                        {...register("tags")}
                        {...invalidProps("tags")}
                    />
                    {fieldError("tags")}
                </div>
            </div>
            <div className="flex flex-col gap-2">
                <div className="flex items-center justify-between gap-3">
                    <label htmlFor="post-body" className={LABEL_CLASS}>
                        Nội dung
                    </label>
                    <div
                        role="group"
                        aria-label="Chế độ soạn"
                        className="flex rounded-md border p-0.5"
                    >
                        {[false, true].map((preview) => (
                            <button
                                key={String(preview)}
                                type="button"
                                aria-pressed={isPreview === preview}
                                onClick={() => setIsPreview(preview)}
                                className={cn(
                                    "rounded-sm px-3 py-1 text-sm font-semibold",
                                    isPreview === preview
                                        ? "bg-surface-2"
                                        : "text-muted-foreground",
                                )}
                            >
                                {preview ? "Xem trước" : "Viết"}
                            </button>
                        ))}
                    </div>
                </div>
                <textarea
                    id="post-body"
                    rows={16}
                    hidden={isPreview}
                    placeholder="Markdown. Khối ```sql run chạy được trên dữ liệu mẫu của bài tập đã gắn."
                    className="rounded-md border bg-transparent px-3 py-2.5 font-mono text-[15px] leading-relaxed"
                    {...register("bodyMarkdown")}
                    {...invalidProps("bodyMarkdown")}
                />
                {isPreview ? (
                    <div className="min-h-64 rounded-md border bg-card p-5">
                        <PostBody
                            markdown={watch("bodyMarkdown")}
                            problemSlug={problem?.slug || null}
                        />
                    </div>
                ) : null}
                {fieldError("bodyMarkdown")}
            </div>
            <div className="flex justify-end gap-2">
                {isDraft ? (
                    <Button
                        type="button"
                        variant="outline"
                        disabled={isSaving}
                        onClick={() => void save(false)()}
                    >
                        Lưu nháp
                    </Button>
                ) : null}
                <Button type="submit" disabled={isSaving}>
                    {isDraft ? "Đăng bài" : "Lưu thay đổi"}
                </Button>
            </div>
        </form>
    );
}
