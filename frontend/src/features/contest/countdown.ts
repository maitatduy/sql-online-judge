const MS_PER_SECOND = 1000;
const SECONDS_PER_MINUTE = 60;
const SECONDS_PER_HOUR = 3_600;
const SECONDS_PER_DAY = 86_400;
const VIETNAM_TIME_ZONE = "Asia/Ho_Chi_Minh";
const WEEKDAYS: Record<string, string> = {
    Mon: "Thứ 2",
    Tue: "Thứ 3",
    Wed: "Thứ 4",
    Thu: "Thứ 5",
    Fri: "Thứ 6",
    Sat: "Thứ 7",
    Sun: "CN",
};

const weekdayFormatter = new Intl.DateTimeFormat("en-US", {
    timeZone: VIETNAM_TIME_ZONE,
    weekday: "short",
});
const dayFormatter = new Intl.DateTimeFormat("en-GB", {
    timeZone: VIETNAM_TIME_ZONE,
    day: "2-digit",
});
const monthFormatter = new Intl.DateTimeFormat("en-GB", {
    timeZone: VIETNAM_TIME_ZONE,
    month: "numeric",
});

export interface CountdownPart {
    value: string;
    unit: string;
}

export interface ContestDay {
    weekday: string;
    day: string;
    month: string;
}

function twoDigits(value: number): string {
    return String(value).padStart(2, "0");
}

export function countdownParts(target: string, now: Date): CountdownPart[] {
    const remaining = Math.max(
        0,
        Math.floor((new Date(target).getTime() - now.getTime()) / MS_PER_SECOND),
    );
    const days = Math.floor(remaining / SECONDS_PER_DAY);
    const parts: CountdownPart[] = [
        {
            value: twoDigits(Math.floor((remaining % SECONDS_PER_DAY) / SECONDS_PER_HOUR)),
            unit: "giờ",
        },
        {
            value: twoDigits(Math.floor((remaining % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE)),
            unit: "phút",
        },
        { value: twoDigits(remaining % SECONDS_PER_MINUTE), unit: "giây" },
    ];
    return days > 0 ? [{ value: twoDigits(days), unit: "ngày" }, ...parts] : parts;
}

export function formatContestDay(date: string): ContestDay {
    const moment = new Date(date);
    return {
        weekday: WEEKDAYS[weekdayFormatter.format(moment)],
        day: dayFormatter.format(moment),
        month: `Tháng ${monthFormatter.format(moment)}`,
    };
}
