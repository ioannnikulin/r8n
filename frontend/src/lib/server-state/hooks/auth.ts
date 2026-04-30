import { useMutation, useQueryClient } from "@tanstack/react-query";
import type { UseMutationOptions } from "@tanstack/react-query";
import { authApi } from "@/lib/api";
import type {
  AuthenticationTokenDto,
  LoginRequestDto,
  RegisterRequestDto,
} from "@/lib/api/auth";
import {
  clearAuthSession,
  setAuthSession,
} from "@/lib/server-state/auth-store";
import type { ApiErrorMeta } from "@/lib/server-state/query-client";

export function useLoginMutation(
  options?: UseMutationOptions<
    AuthenticationTokenDto,
    Error,
    LoginRequestDto,
    unknown
  >,
) {
  return useMutation({
    mutationFn: (variables: LoginRequestDto) => authApi.login(variables),
    ...options,
    meta: {
      errorTitle: "Sign in failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      setAuthSession(data);
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useRegisterMutation(
  options?: UseMutationOptions<
    void,
    Error,
    RegisterRequestDto,
    unknown
  >,
) {
  return useMutation({
    mutationFn: (variables: RegisterRequestDto) => authApi.register(variables),
    ...options,
    meta: {
      errorTitle: "Registration failed",
      ...options?.meta,
    } as ApiErrorMeta,
  });
}

export function useLogoutMutation(
  options?: UseMutationOptions<void, Error, void, unknown>,
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => authApi.logout(),
    ...options,
    meta: {
      errorTitle: "Sign out failed",
      showErrorToast: false,
      ...options?.meta,
    } as ApiErrorMeta,
    onSettled: (data, error, variables, context) => {
      clearAuthSession();
      queryClient.clear();
      options?.onSettled?.(data, error, variables, context);
    },
  });
}
