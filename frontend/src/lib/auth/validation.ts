import { z } from "zod";

export const AUTH_EMAIL_MAX_LENGTH = readPositiveIntegerEnv(
  "VITE_AUTH_EMAIL_MAX_LENGTH",
  import.meta.env.VITE_AUTH_EMAIL_MAX_LENGTH,
);
export const AUTH_PASSWORD_MAX_LENGTH = readPositiveIntegerEnv(
  "VITE_AUTH_PASSWORD_MAX_LENGTH",
  import.meta.env.VITE_AUTH_PASSWORD_MAX_LENGTH,
);
export const REGISTRATION_PASSWORD_MIN_LENGTH = readPositiveIntegerEnv(
  "VITE_REGISTRATION_PASSWORD_MIN_LENGTH",
  import.meta.env.VITE_REGISTRATION_PASSWORD_MIN_LENGTH,
);
export const PROFILE_NAME_MAX_LENGTH = readPositiveIntegerEnv(
  "VITE_PROFILE_NAME_MAX_LENGTH",
  import.meta.env.VITE_PROFILE_NAME_MAX_LENGTH,
);

export type AuthFieldErrors = Partial<
  Record<"name" | "email" | "password" | "confirmPassword" | "gdprAccepted", string>
>;

export interface LoginFormValues {
  email: string;
  password: string;
}

export interface RegistrationFormValues extends LoginFormValues {
  name: string;
  confirmPassword: string;
  gdprAccepted: boolean;
}

interface ValidationSuccess<T> {
  success: true;
  data: T;
  errors: AuthFieldErrors;
}

interface ValidationFailure {
  success: false;
  errors: AuthFieldErrors;
}

type ValidationResult<T> = ValidationSuccess<T> | ValidationFailure;

const emailSchema = z
  .string()
  .trim()
  .min(1, "Enter your email address.")
  .max(AUTH_EMAIL_MAX_LENGTH, "Email address is too long.")
  .email("Enter a valid email address.");

const nameSchema = z
  .string()
  .trim()
  .min(1, "Enter your display name.")
  .max(PROFILE_NAME_MAX_LENGTH, `Display name must be ${PROFILE_NAME_MAX_LENGTH} characters or fewer.`);

const loginFormSchema = z.object({
  email: emailSchema,
  password: z
    .string()
    .max(AUTH_PASSWORD_MAX_LENGTH, "Password is too long.")
    .refine((value) => value.trim().length > 0, "Enter your password."),
});

const registrationFormSchema = loginFormSchema
  .extend({
    name: nameSchema,
    password: z
      .string()
      .min(
        REGISTRATION_PASSWORD_MIN_LENGTH,
        `Password must be at least ${REGISTRATION_PASSWORD_MIN_LENGTH} characters.`,
      )
      .max(AUTH_PASSWORD_MAX_LENGTH, "Password is too long."),
    confirmPassword: z.string().min(1, "Confirm your password."),
    gdprAccepted: z.boolean().refine((value) => value, {
      message: "Accept the Privacy Policy and Terms of Service.",
    }),
  })
  .superRefine((values, context) => {
    if (
      values.confirmPassword.length > 0 &&
      values.password !== values.confirmPassword
    ) {
      context.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["confirmPassword"],
        message: "Passwords do not match.",
      });
    }
  });

export function validateLoginForm(
  values: LoginFormValues,
): ValidationResult<LoginFormValues> {
  return parseForm(loginFormSchema, values);
}

export function validateRegistrationForm(
  values: RegistrationFormValues,
): ValidationResult<RegistrationFormValues> {
  return parseForm(registrationFormSchema, values);
}

function parseForm<T>(
  schema: z.ZodType<T>,
  values: unknown,
): ValidationResult<T> {
  const result = schema.safeParse(values);

  if (result.success) {
    return {
      success: true,
      data: result.data,
      errors: {},
    };
  }

  return {
    success: false,
    errors: toFieldErrors(result.error),
  };
}

function toFieldErrors(error: z.ZodError): AuthFieldErrors {
  return error.issues.reduce<AuthFieldErrors>((fieldErrors, issue) => {
    const field = issue.path[0];

    if (
      typeof field === "string" &&
      isAuthField(field) &&
      fieldErrors[field] === undefined
    ) {
      fieldErrors[field] = issue.message;
    }

    return fieldErrors;
  }, {});
}

function isAuthField(field: string): field is keyof AuthFieldErrors {
  return (
    field === "name" ||
    field === "email" ||
    field === "password" ||
    field === "confirmPassword" ||
    field === "gdprAccepted"
  );
}

function readPositiveIntegerEnv(name: string, value: string | undefined): number {
  const parsedValue = Number(value);

  if (!Number.isInteger(parsedValue) || parsedValue <= 0) {
    throw new Error(`${name} must be a positive integer.`);
  }

  return parsedValue;
}
