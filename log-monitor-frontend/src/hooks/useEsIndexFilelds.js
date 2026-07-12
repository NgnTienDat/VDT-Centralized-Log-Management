import { useQuery } from "@tanstack/react-query";
import { fetchIndexFields, fetchNumericFields } from "../api/logApi";

export function useEsIndexFilelds(indexPattern = "sys-logs-*") {
    const query = useQuery({
        queryKey: ["esIndexFields", indexPattern],
        queryFn: () => fetchIndexFields(indexPattern),
        staleTime: 5 * 60 * 1000,
        placeholderData: [],
    });

    const numericQuery = useQuery({
        queryKey: ["esNumericFields", indexPattern],
        queryFn: () => fetchNumericFields(indexPattern),
        staleTime: 5 * 60 * 1000,
        placeholderData: [],
    });

    return {
        fields: query.data ?? [],
        isLoadingFields: query.isLoading,
        isFieldsError: query.isError,
        fieldsError: query.error,
        refetchFields: query.refetch,

        numericFields: numericQuery.data ?? [],
        isLoadingNumericFields: numericQuery.isLoading,
        isNumericFieldsError: numericQuery.isError,
        numericFieldsError: numericQuery.error,
        refetchNumericFields: numericQuery.refetch,
    };
}