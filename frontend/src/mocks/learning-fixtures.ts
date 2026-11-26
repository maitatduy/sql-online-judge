import type { CourseDetail, QuizQuestion } from "@/features/learning/types";

export const LEARNING_COURSES: readonly CourseDetail[] = [
    {
        id: "01a1d2c0-0000-7000-8000-000000000001",
        slug: "sql-co-ban",
        title: "SQL cơ bản",
        summary:
            "Chọn cột, lọc dòng và sắp xếp kết quả.\n- Đọc dữ liệu bằng SELECT\n- Lọc dòng với WHERE\n- Sắp xếp với ORDER BY",
        difficulty: "EASY",
        lessonCount: 4,
        estimatedMinutes: 40,
        publishedAt: "2026-11-01T08:00:00Z",
        chapters: [
            {
                id: "01a1d2c0-0000-7000-8000-000000000011",
                title: "Làm quen với SELECT",
                lessons: [
                    {
                        lessonKey: "01a1d2c0-0000-7000-8000-000000000201",
                        slug: "select-la-gi",
                        title: "SELECT là gì",
                    },
                    {
                        lessonKey: "01a1d2c0-0000-7000-8000-000000000202",
                        slug: "chon-cot",
                        title: "Chọn cột và đặt bí danh",
                    },
                ],
            },
            {
                id: "01a1d2c0-0000-7000-8000-000000000012",
                title: "Lọc và sắp xếp",
                lessons: [
                    {
                        lessonKey: "01a1d2c0-0000-7000-8000-000000000203",
                        slug: "menh-de-where",
                        title: "Lọc dòng với WHERE",
                    },
                    {
                        lessonKey: "01a1d2c0-0000-7000-8000-000000000204",
                        slug: "order-by",
                        title: "Sắp xếp với ORDER BY",
                    },
                ],
            },
        ],
    },
    {
        id: "01a1d2c0-0000-7000-8000-000000000021",
        slug: "join-thuc-chien",
        title: "JOIN thực chiến",
        summary: "Ghép nhiều bảng bằng INNER, LEFT và SELF JOIN.",
        difficulty: "MEDIUM",
        lessonCount: 6,
        estimatedMinutes: 75,
        publishedAt: "2026-11-05T08:00:00Z",
        chapters: [
            {
                id: "01a1d2c0-0000-7000-8000-000000000031",
                title: "Ghép hai bảng",
                lessons: [
                    {
                        lessonKey: "01a1d2c0-0000-7000-8000-000000000301",
                        slug: "inner-join",
                        title: "INNER JOIN",
                    },
                ],
            },
        ],
    },
];

export const QUIZ_LESSON_SLUG = "select-la-gi";
export const QUIZ_CORRECT_OPTION = "option-all-columns";

export const LEARNING_QUIZ: readonly QuizQuestion[] = [
    {
        questionKey: "question-star",
        prompt: "Dấu * sau SELECT nghĩa là gì?",
        multiple: false,
        options: [
            { optionKey: QUIZ_CORRECT_OPTION, label: "Lấy mọi cột của bảng" },
            { optionKey: "option-all-tables", label: "Lấy mọi bảng trong database" },
        ],
    },
];
