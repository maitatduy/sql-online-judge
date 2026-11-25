import { CtaEditorPreview } from "./CtaEditorPreview";
import { StartLearningActions } from "./StartLearningActions";

export function CtaBanner() {
    return (
        <section className="mx-auto grid max-w-7xl items-center gap-10 px-6 py-16 md:py-24 lg:grid-cols-[2fr_3fr] lg:gap-14">
            <div className="min-w-0 lg:order-2">
                <h2 className="text-[40px] leading-[1.05] font-extrabold tracking-[-0.035em] sm:text-5xl md:text-[56px]">
                    Database đã sẵn sàng, chỉ còn thiếu{" "}
                    <span className="text-brand">câu truy vấn của bạn</span>.
                </h2>
                <p className="mt-6 text-[19px] leading-[1.6] text-muted-foreground">
                    Viết, chạy và chấm SQL ngay trên trình duyệt, miễn phí.
                </p>
                <StartLearningActions />
            </div>
            <CtaEditorPreview />
        </section>
    );
}
