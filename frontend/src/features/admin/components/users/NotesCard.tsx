import { zodResolver } from "@hookform/resolvers/zod";
import { useForm, useWatch } from "react-hook-form";

import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { useAddUserNote, useUserNotes } from "@/features/admin/hooks/useUserNotes";
import { fieldErrorOf } from "@/features/admin/users/form-errors";
import { userNoteSchema, type UserNoteValues } from "@/features/admin/users/schemas";
import { formatDate } from "@/lib/format";

import { BlockEmpty, BlockError, BlockLoading } from "./BlockStates";
import { PageCard } from "./PageCard";
import { TextAreaField } from "./TextAreaField";

interface NotesCardProps {
    username: string;
    isReadOnly: boolean;
}

export function NotesCard({ username, isReadOnly }: NotesCardProps) {
    const showToast = useToast();
    const { data: notes, error, isPending, refetch } = useUserNotes(username);
    const addNote = useAddUserNote(username);
    const {
        control,
        register,
        handleSubmit,
        reset,
        setError,
        formState: { errors },
    } = useForm<UserNoteValues>({
        resolver: zodResolver(userNoteSchema),
        defaultValues: { body: "" },
    });
    const body = useWatch({ control, name: "body" });

    const submitNote = (values: UserNoteValues) =>
        addNote
            .mutateAsync(values.body)
            .then(() => {
                showToast("Đã thêm ghi chú.");
                reset({ body: "" });
            })
            .catch((failure: Error) => {
                const bodyError = fieldErrorOf(failure, "body");
                if (bodyError) {
                    setError("body", { message: bodyError });
                    return;
                }
                showToast(`Chưa thêm được ghi chú. ${failure.message}`, "error");
            });

    return (
        <PageCard title="Ghi chú nội bộ">
            {isPending ? <BlockLoading label="Đang tải ghi chú" /> : null}
            {error && !notes ? <BlockError error={error} onRetry={() => void refetch()} /> : null}
            {notes && notes.length > 0 ? (
                <ul className="mb-3 flex flex-col gap-2">
                    {notes.map((note) => (
                        <li key={note.id} className="rounded-md bg-surface px-3 py-2.5 text-[13px]">
                            <p className="text-xs font-semibold">
                                {`${note.authorUsername} · ${formatDate(note.createdAt)}`}
                            </p>
                            <p className="mt-0.5 leading-relaxed">{note.body}</p>
                        </li>
                    ))}
                </ul>
            ) : null}
            {notes && notes.length === 0 ? <BlockEmpty>Chưa có ghi chú nào.</BlockEmpty> : null}
            {isReadOnly ? null : (
                <form
                    noValidate
                    onSubmit={handleSubmit(submitNote)}
                    className="mt-3 flex flex-col gap-2"
                >
                    <TextAreaField
                        label="Thêm ghi chú"
                        hint="Chỉ quản trị viên thấy ghi chú này."
                        rows={2}
                        error={errors.body?.message}
                        registration={register("body")}
                    />
                    <Button
                        type="submit"
                        size="sm"
                        variant="outline"
                        disabled={body.trim() === "" || addNote.isPending}
                    >
                        Lưu ghi chú
                    </Button>
                </form>
            )}
        </PageCard>
    );
}
