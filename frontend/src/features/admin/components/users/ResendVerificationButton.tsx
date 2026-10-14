import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { useResendVerification } from "@/features/admin/hooks/useResendVerification";

interface ResendVerificationButtonProps {
    username: string;
    email: string;
}

export function ResendVerificationButton({ username, email }: ResendVerificationButtonProps) {
    const showToast = useToast();
    const resendVerification = useResendVerification();

    function handleResend() {
        resendVerification.mutate(username, {
            onSuccess: () => showToast(`Đã gửi lại email xác thực tới ${email}.`),
            onError: (error) =>
                showToast(`Chưa gửi được email xác thực tới ${email}. ${error.message}`, "error"),
        });
    }

    return (
        <Button
            type="button"
            size="sm"
            variant="outline"
            disabled={resendVerification.isPending}
            onClick={handleResend}
        >
            Gửi lại email xác thực
        </Button>
    );
}
