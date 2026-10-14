interface SelfSkipNoticeProps {
    username: string;
}

export function SelfSkipNotice({ username }: SelfSkipNoticeProps) {
    return (
        <p className="rounded-md border bg-surface px-3 py-2.5 text-[13px]">
            {`Tài khoản của bạn, ${username}, sẽ được bỏ qua.`}
        </p>
    );
}
