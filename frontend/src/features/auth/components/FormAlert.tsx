import { CircleAlert } from "lucide-react";

interface FormAlertProps {
    message: string;
    action?: React.ReactNode;
}

export function FormAlert({ message, action }: FormAlertProps) {
    return (
        <div
            role="alert"
            className="flex items-start gap-2 rounded-lg border border-verdict-wa/30 bg-verdict-wa/10 px-3.5 py-3 text-sm text-verdict-wa"
        >
            <CircleAlert aria-hidden="true" className="mt-0.5 size-4 shrink-0" />
            <div className="flex flex-col items-start gap-1">
                <p>{message}</p>
                {action}
            </div>
        </div>
    );
}
