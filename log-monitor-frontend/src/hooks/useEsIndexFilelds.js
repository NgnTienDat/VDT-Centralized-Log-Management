import { useQuery } from "@tanstack/react-query";
import { fetchIndexFields } from "../api/logApi";

const ES_INDEX_FIELDS_QUERY_KEY = ["esIndexFields", "sys-logs-*"];

export function useEsIndexFilelds(indexPattern = "sys-logs-*") {
    const query = useQuery({
        queryKey: ["esIndexFields", indexPattern],
        queryFn: () => fetchIndexFields(indexPattern),
        staleTime: 5 * 60 * 1000,
        placeholderData: [],
    });

    return {
        fields: query.data ?? [],
        isLoadingFields: query.isLoading,
        isFieldsError: query.isError,
        fieldsError: query.error,
        refetchFields: query.refetch,
    };
}