import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type {
  UseMutationOptions,
  UseMutationResult,
  UseQueryOptions,
  UseQueryResult,
} from "@tanstack/react-query";

const IS_E2E_AUTH_BYPASS_ENABLED =
  import.meta.env.DEV && import.meta.env.VITE_E2E_BYPASS_AUTH === "true";

export function useAuthorizedQuery<TData, TError = Error, TQueryKey extends readonly unknown[] = readonly unknown[]>(
  options: Omit<UseQueryOptions<TData, TError, TData, TQueryKey>, "queryFn"> & {
    queryFn: () => Promise<TData>;
  },
): UseQueryResult<TData, TError> {
  const enabled = IS_E2E_AUTH_BYPASS_ENABLED ? false : (options.enabled ?? true);

  return useQuery({
    ...options,
    enabled,
    queryFn: () => options.queryFn(),
  });
}

export function useAuthorizedMutation<
  TData,
  TError = Error,
  TVariables = void,
  TContext = unknown,
>(
  options: Omit<
    UseMutationOptions<TData, TError, TVariables, TContext>,
    "mutationFn"
  > & {
    mutationFn: (variables: TVariables) => Promise<TData>;
  },
): UseMutationResult<TData, TError, TVariables, TContext> {
  return useMutation({
    ...options,
    mutationFn: (variables: TVariables) => options.mutationFn(variables),
  });
}

export function useApiInvalidation() {
  const queryClient = useQueryClient();

  return (queryKey: readonly unknown[]) =>
    queryClient.invalidateQueries({ queryKey });
}
