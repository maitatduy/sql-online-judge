"use client";

import { Check, TriangleAlert, X } from "lucide-react";
import { Toast } from "radix-ui";
import { createContext, useCallback, useContext, useRef, useState } from "react";

const TOAST_DURATION_MS = 4000;

export type ToastVariant = "success" | "error";

type ShowToast = (message: string, variant?: ToastVariant) => void;

interface ToastMessage {
    id: number;
    message: string;
    variant: ToastVariant;
}

interface ToastProviderProps {
    children: React.ReactNode;
}

const ToastContext = createContext<ShowToast | null>(null);

export function useToast(): ShowToast {
    const showToast = useContext(ToastContext);
    if (!showToast) {
        throw new Error("useToast must be used inside ToastProvider");
    }
    return showToast;
}

export function ToastProvider({ children }: ToastProviderProps) {
    const [toasts, setToasts] = useState<ToastMessage[]>([]);
    const nextToastId = useRef(0);

    const showToast = useCallback<ShowToast>((message, variant = "success") => {
        nextToastId.current += 1;
        const id = nextToastId.current;
        setToasts((currentToasts) => [...currentToasts, { id, message, variant }]);
    }, []);

    function dismissToast(id: number) {
        setToasts((currentToasts) => currentToasts.filter((toast) => toast.id !== id));
    }

    return (
        <ToastContext.Provider value={showToast}>
            <Toast.Provider label="Thông báo" duration={TOAST_DURATION_MS}>
                {children}
                {toasts.map((toast) => (
                    <Toast.Root
                        key={toast.id}
                        type={toast.variant === "error" ? "foreground" : "background"}
                        onOpenChange={(isOpen) => {
                            if (!isOpen) {
                                dismissToast(toast.id);
                            }
                        }}
                        className="flex items-start gap-2.5 rounded-lg bg-foreground px-3.5 py-3 text-sm text-background shadow-lg"
                    >
                        {toast.variant === "error" ? (
                            <TriangleAlert aria-hidden="true" className="mt-0.5 size-4 shrink-0" />
                        ) : (
                            <Check aria-hidden="true" className="mt-0.5 size-4 shrink-0" />
                        )}
                        <Toast.Description className="flex-1">{toast.message}</Toast.Description>
                        <Toast.Close
                            aria-label="Đóng thông báo"
                            className="inline-flex size-5 shrink-0 items-center justify-center rounded-sm opacity-70 hover:opacity-100"
                        >
                            <X aria-hidden="true" className="size-4" />
                        </Toast.Close>
                    </Toast.Root>
                ))}
                <Toast.Viewport className="fixed right-6 bottom-6 z-60 flex w-90 max-w-[calc(100vw-3rem)] flex-col gap-2 outline-none" />
            </Toast.Provider>
        </ToastContext.Provider>
    );
}
