interface SqlKeywordProps {
    children: React.ReactNode;
}

export function SqlKeyword({ children }: SqlKeywordProps) {
    return <span className="font-semibold text-syntax-keyword">{children}</span>;
}
