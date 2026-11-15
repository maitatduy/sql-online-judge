"use client";

import { useState } from "react";

import Link from "next/link";

import { DifficultyLabel } from "@/components/shared/DifficultyLabel";
import { formatRelativeTime } from "@/lib/format";
import { ROUTES } from "@/lib/routes";
import { cn } from "@/lib/utils";

import type { StudioCourse } from "../types";
import { RevisionStatusBadge } from "./RevisionStatusBadge";

const CELL_CLASS = "border-b px-4 py-3 align-middle";
const HEADER_CLASS = "border-b px-4 py-2.5 text-left text-sm font-semibold text-muted-foreground";

interface MyCoursesTableProps {
    courses: StudioCourse[];
}

export function MyCoursesTable({ courses }: MyCoursesTableProps) {
    const [now] = useState(() => new Date());

    return (
        <div className="overflow-x-auto rounded-lg border bg-card">
            <table aria-label="Khoá học của tôi" className="w-full min-w-[680px] border-collapse">
                <thead>
                    <tr>
                        <th scope="col" className={HEADER_CLASS}>
                            Khoá học
                        </th>
                        <th scope="col" className={cn(HEADER_CLASS, "w-36")}>
                            Trạng thái
                        </th>
                        <th scope="col" className={cn(HEADER_CLASS, "w-32")}>
                            Độ khó
                        </th>
                        <th scope="col" className={cn(HEADER_CLASS, "w-36")}>
                            Cập nhật
                        </th>
                    </tr>
                </thead>
                <tbody>
                    {courses.map((course) => (
                        <tr key={course.id}>
                            <td className={CELL_CLASS}>
                                <Link
                                    href={ROUTES.studioCourse(course.id)}
                                    className="font-semibold hover:text-brand hover:underline"
                                >
                                    {course.title}
                                </Link>
                                <p className="text-sm text-muted-foreground">{course.slug}</p>
                            </td>
                            <td className={CELL_CLASS}>
                                <RevisionStatusBadge status={course.revisionStatus} />
                            </td>
                            <td className={CELL_CLASS}>
                                <DifficultyLabel
                                    difficulty={course.difficulty}
                                    className="text-sm"
                                />
                            </td>
                            <td className={cn(CELL_CLASS, "text-sm text-muted-foreground")}>
                                <time dateTime={course.updatedAt}>
                                    {formatRelativeTime(course.updatedAt, now)}
                                </time>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
