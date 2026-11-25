import { CtaEditorPreview } from "./CtaEditorPreview";
import { StartLearningActions } from "./StartLearningActions";

export function CtaBanner() {
    return (
        <section className="mx-auto grid max-w-7xl items-center gap-10 px-6 py-16 md:py-24 lg:grid-cols-[2fr_3fr] lg:gap-14">
            <div className="min-w-0 lg:order-2">
                <h2 className="text-[40px] leading-[1.05] font-extrabold tracking-[-0.035em] sm:text-5xl md:text-[56px]">
                    Câu truy vấn đầu tiên chỉ mất <span className="text-brand">30 giây</span>.
                </h2>
                <p className="mt-6 text-[19px] leading-[1.6] text-muted-foreground">
                    Không cần cài database. Miễn phí.
                </p>
                <StartLearningActions />
            </div>
            <CtaEditorPreview />
        </section>
    );
}
