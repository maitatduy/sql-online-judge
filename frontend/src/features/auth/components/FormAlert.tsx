import { CircleAlert } from "lucide-react";

interface FormAlertProps {
    message: string;
}

export function FormAlert({ message }: FormAlertProps) {
    return (
        <p
            role="alert"
            className="flex items-start gap-2 rounded-lg border border-verdict-wa/30 bg-verdict-wa/10 px-3.5 py-3 text-sm text-verdict-wa"
        >
            <CircleAlert aria-hidden="true" className="mt-0.5 size-4 shrink-0" />
            {message}
        </p>
    );
}
