import { CtaEditorPreview } from "./CtaEditorPreview";
import { StartLearningActions } from "./StartLearningActions";

export function CtaBanner() {
    return (
        <section className="mx-auto grid max-w-7xl items-center gap-10 px-6 py-16 md:py-24 lg:grid-cols-[3fr_2fr] lg:gap-14">
            <div className="min-w-0 lg:order-2">
                <h2 className="text-3xl font-extrabold tracking-[-0.02em] md:text-[40px] md:leading-[1.15]">
                    Câu truy vấn đầu tiên chỉ mất 30 giây.
                </h2>
                <p className="mt-4 text-[17px] text-muted-foreground">
                    Không cần cài database. Miễn phí.
                </p>
                <StartLearningActions />
            </div>
            <CtaEditorPreview />
        </section>
    );
}
