import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { QuizQuestionRequest, StudioQuizQuestion } from "@/features/course-studio/types";

import { apiError } from "./api-error";
import { lessonAccess } from "./studio-course-outline";

const STORAGE_KEY = "sqljudge:mock:studio-lesson-quiz";

type QuizStore = Record<string, StudioQuizQuestion[]>;

const SEEDED_QUIZ: QuizStore = {
    "0192f3f3-0000-7000-8000-000000000001": [
        {
            id: "0192f3f7-0000-7000-8000-000000000001",
            position: 1,
            prompt: "ROW_NUMBER() trả về gì?",
            multiple: false,
            options: [
                { label: "Số thứ tự của dòng trong nhóm", correct: true },
                { label: "Tổng số dòng của bảng", correct: false },
            ],
        },
    ],
};

function readQuiz(): QuizStore {
    const stored = sessionStorage.getItem(STORAGE_KEY);
    return stored ? (JSON.parse(stored) as QuizStore) : SEEDED_QUIZ;
}

function writeQuiz(lessonId: string, questions: StudioQuizQuestion[]): void {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify({ ...readQuiz(), [lessonId]: questions }));
}

function lessonOfQuestion(questionId: string): string | undefined {
    return Object.entries(readQuiz()).find(([, questions]) =>
        questions.some((question) => question.id === questionId),
    )?.[0];
}

function questionNotFound(): Response {
    return apiError(404, "QUIZ_QUESTION_NOT_FOUND", "Không tìm thấy câu hỏi");
}

export const studioLessonQuizHandlers: RequestHandler[] = [
    http.get("*/api/studio/lessons/:lessonId/quiz-questions", async ({ params }) => {
        await delay();
        const lessonId = String(params.lessonId);
        return lessonAccess(lessonId, false) ?? HttpResponse.json(readQuiz()[lessonId] ?? []);
    }),
    http.post("*/api/studio/lessons/:lessonId/quiz-questions", async ({ params, request }) => {
        await delay();
        const lessonId = String(params.lessonId);
        const denied = lessonAccess(lessonId, true);
        if (denied) {
            return denied;
        }
        const questions = readQuiz()[lessonId] ?? [];
        const question: StudioQuizQuestion = {
            ...((await request.json()) as QuizQuestionRequest),
            id: crypto.randomUUID(),
            position: (questions.at(-1)?.position ?? 0) + 1,
        };
        writeQuiz(lessonId, [...questions, question]);
        return HttpResponse.json(question, { status: 201 });
    }),
    http.put("*/api/studio/quiz-questions/:questionId", async ({ params, request }) => {
        await delay();
        const questionId = String(params.questionId);
        const lessonId = lessonOfQuestion(questionId);
        if (!lessonId) {
            return questionNotFound();
        }
        const denied = lessonAccess(lessonId, true);
        if (denied) {
            return denied;
        }
        const body = (await request.json()) as QuizQuestionRequest;
        const questions = (readQuiz()[lessonId] ?? []).map((question) =>
            question.id === questionId ? { ...question, ...body } : question,
        );
        writeQuiz(lessonId, questions);
        return HttpResponse.json(questions.find((question) => question.id === questionId));
    }),
    http.delete("*/api/studio/quiz-questions/:questionId", async ({ params }) => {
        await delay();
        const questionId = String(params.questionId);
        const lessonId = lessonOfQuestion(questionId);
        if (!lessonId) {
            return questionNotFound();
        }
        const denied = lessonAccess(lessonId, true);
        if (denied) {
            return denied;
        }
        writeQuiz(
            lessonId,
            (readQuiz()[lessonId] ?? []).filter((question) => question.id !== questionId),
        );
        return new HttpResponse(null, { status: 204 });
    }),
];
