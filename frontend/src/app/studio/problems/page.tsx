import type { Metadata } from "next";

import { MyProblems } from "@/features/studio/components/MyProblems";

export const metadata: Metadata = {
    title: "Bài của tôi",
};

export default function MyProblemsPage() {
    return <MyProblems />;
}
