import { Check, Lock } from "lucide-react";

import { ChatBubble } from "./ChatBubble";
import { SectionHeading } from "./SectionHeading";

const AI_FEATURES = [
    { icon: Check, label: "Giải thích lỗi cú pháp bằng tiếng Việt" },
    { icon: Check, label: "Phân tích EXPLAIN, gợi ý tối ưu" },
    { icon: Check, label: "Sinh bài luyện theo điểm yếu của bạn" },
    { icon: Lock, label: "Tự tắt trong giờ thi contest" },
];

export function AiSection() {
    return (
        <section className="border-y bg-surface">
            <div className="mx-auto flex max-w-7xl flex-wrap items-center gap-14 px-6 py-16 md:py-24">
                <div className="min-w-0 flex-[1_1_400px]">
                    <SectionHeading eyebrow="Trợ lý AI" title="Gợi ý, không giải hộ." />
                    <p className="mt-4 mb-6 text-[17px] leading-[1.65] text-muted-foreground">
                        AI đọc truy vấn và kết quả của bạn, rồi đưa gợi ý theo ba mức - từ hướng suy
                        nghĩ đến khái niệm, đến cấu trúc câu lệnh. Đáp án đầy đủ chỉ mở sau khi bạn
                        AC.
                    </p>
                    <ul className="flex flex-col gap-4">
                        {AI_FEATURES.map(({ icon: Icon, label }) => (
                            <li key={label} className="flex items-center gap-2.5">
                                <Icon aria-hidden="true" className="size-4 shrink-0" />
                                {label}
                            </li>
                        ))}
                    </ul>
                </div>
                <figure
                    aria-label="Ví dụ hội thoại với trợ lý AI"
                    className="flex min-w-0 flex-[1_1_480px] flex-col gap-3 rounded-xl border bg-background p-5"
                >
                    <ChatBubble sender="user">Vì sao query của tôi bị WA ở testcase 3?</ChatBubble>
                    <ChatBubble sender="assistant" hintLabel="Gợi ý 1 · Gợi hướng">
                        Testcase 3 có nhân viên chưa thuộc phòng ban nào (
                        <code className="font-mono">department_id</code> là NULL).{" "}
                        <code className="font-mono">INNER JOIN</code> đang loại họ khỏi kết quả - đề
                        bài có yêu cầu giữ lại không?
                    </ChatBubble>
                    <ChatBubble sender="user">À, phải dùng LEFT JOIN!</ChatBubble>
                    <ChatBubble sender="assistant">
                        Chính xác. Thử lại và nhớ đặt điều kiện lọc ở{" "}
                        <code className="font-mono">ON</code> thay vì{" "}
                        <code className="font-mono">WHERE</code> nhé.
                    </ChatBubble>
                </figure>
            </div>
        </section>
    );
}
