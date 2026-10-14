const EMAIL_SEPARATORS = /[\n,]/;
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function parseEmailList(text: string): string[] {
    const seenEmails = new Set<string>();
    return text
        .split(EMAIL_SEPARATORS)
        .map((email) => email.trim())
        .filter((email) => {
            const normalizedEmail = email.toLowerCase();
            if (email === "" || seenEmails.has(normalizedEmail)) {
                return false;
            }
            seenEmails.add(normalizedEmail);
            return true;
        });
}

export function invalidEmailsOf(emails: string[]): string[] {
    return emails.filter((email) => !EMAIL_PATTERN.test(email));
}
