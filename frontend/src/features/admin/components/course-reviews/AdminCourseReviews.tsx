"use client";

import { useState } from "react";

import { EmptyState } from "@/components/shared/EmptyState";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";

import { CourseReviewDetail } from "./CourseReviewDetail";
import { CourseReviewQueue } from "./CourseReviewQueue";

export function AdminCourseReviews() {
    const [selectedId, setSelectedId] = useState<string | null>(null);

    return (
        <main id={MAIN_CONTENT_ID} className="flex flex-col gap-4 px-4 pt-6 pb-10 md:px-7">
            <div>
                <h1 className="text-[24px] font-extrabold tracking-[-0.02em]">Duyệt khoá học</h1>
                <p className="mt-1 text-muted-foreground">
                    Khoá do người soạn gửi lên cần được duyệt trước khi xuất bản.
                </p>
            </div>
            <div className="grid items-start gap-4 lg:grid-cols-[360px_minmax(0,1fr)]">
                <CourseReviewQueue selectedId={selectedId} onSelect={setSelectedId} />
                {selectedId ? (
                    <CourseReviewDetail
                        key={selectedId}
                        courseId={selectedId}
                        onReviewed={() => setSelectedId(null)}
                    />
                ) : (
                    <section aria-label="Chi tiết khoá" className="rounded-lg border bg-card">
                        <EmptyState
                            title="Chọn một khoá trong hàng chờ để xem."
                            className="border-0"
                        />
                    </section>
                )}
            </div>
        </main>
    );
}
