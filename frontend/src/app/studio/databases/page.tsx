import type { Metadata } from "next";

import { MyDatasets } from "@/features/studio/components/MyDatasets";

export const metadata: Metadata = {
    title: "Database của tôi",
};

export default function MyDatasetsPage() {
    return <MyDatasets />;
}
