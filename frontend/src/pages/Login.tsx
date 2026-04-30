import { useState, type FormEvent } from "react";
import { motion } from "framer-motion";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useLoginMutation, useRegisterMutation } from "@/lib/server-state";
import { toast } from "@/hooks/use-toast";
import logo from "@/assets/logo.png";
import { NavLink } from "@/components/NavLink";
import { cn } from "@/lib/utils";
import {
  PROFILE_NAME_MAX_LENGTH,
  validateLoginForm,
  validateRegistrationForm,
  type AuthFieldErrors,
} from "@/lib/auth/validation";

const Login = () => {
  const [isSignUp, setIsSignUp] = useState(false);
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [gdprAccepted, setGdprAccepted] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<AuthFieldErrors>({});
  const navigate = useNavigate();
  const loginMutation = useLoginMutation({
    onSuccess: () => {
      navigate("/", { replace: true });
    },
  });
  const registerMutation = useRegisterMutation({
    onSuccess: (_data, variables) => {
      toast({
        title: "Account created",
        description: "You are signed in and ready to continue.",
      });
      loginMutation.mutate({
        login: variables.email,
        password: variables.password,
      });
    },
  });
  const isSubmitPending = loginMutation.isPending || registerMutation.isPending;

  const clearFieldError = (field: keyof AuthFieldErrors) => {
    setFieldErrors((currentErrors) => {
      if (currentErrors[field] === undefined) {
        return currentErrors;
      }

      const nextErrors = { ...currentErrors };
      delete nextErrors[field];
      return nextErrors;
    });
  };

  const switchAuthMode = () => {
    setIsSignUp((currentValue) => !currentValue);
    setName("");
    setEmail("");
    setPassword("");
    setConfirmPassword("");
    setGdprAccepted(false);
    setFieldErrors({});
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const validation = isSignUp
      ? validateRegistrationForm({
        name,
        email,
        password,
        confirmPassword,
        gdprAccepted,
      })
      : validateLoginForm({
        email,
        password,
      });

    if (!validation.success) {
      setFieldErrors(validation.errors);
      return;
    }

    setFieldErrors({});

    if (isSignUp) {
      registerMutation.mutate({
        name: validation.data.name,
        email: validation.data.email,
        password: validation.data.password,
        privacyPolicyAccepted: validation.data.gdprAccepted,
        termsOfServiceAccepted: validation.data.gdprAccepted,
      });
      return;
    }

    loginMutation.mutate({
      login: validation.data.email,
      password: validation.data.password,
    });
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4">
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        className="w-full max-w-sm"
      >
        <div className="mb-8 text-center">
          <img src={logo} alt="r8n" className="mx-auto mb-4 h-10 w-auto" />
          <h1 className="text-2xl font-semibold tracking-tight text-foreground">
            {isSignUp ? "Create your account" : "Welcome back"}
          </h1>
          <p className="mt-1 text-sm text-muted-foreground">
            {isSignUp ? "Start building your trusted network." : "Sign in to continue."}
          </p>
        </div>

        <div className="rounded-2xl border border-border bg-card p-6">
          <form
            onSubmit={handleSubmit}
            className="space-y-4"
            noValidate
          >
            {isSignUp && (
              <div className="space-y-2">
                <Label htmlFor="name" className="text-xs">Display name</Label>
                <Input
                  id="name"
                  type="text"
                  placeholder="Jane Reviewer"
                  value={name}
                  maxLength={PROFILE_NAME_MAX_LENGTH}
                  onChange={(e) => {
                    setName(e.target.value);
                    clearFieldError("name");
                  }}
                  className={cn(
                    "rounded-xl",
                    fieldErrors.name && "border-destructive focus-visible:ring-destructive",
                  )}
                  autoComplete="name"
                  aria-invalid={fieldErrors.name !== undefined}
                  aria-describedby={fieldErrors.name ? "name-error" : undefined}
                />
                {fieldErrors.name && (
                  <p id="name-error" role="alert" className="text-xs font-medium text-destructive">
                    {fieldErrors.name}
                  </p>
                )}
              </div>
            )}

            <div className="space-y-2">
              <Label htmlFor="email" className="text-xs">Email</Label>
              <Input
                id="email"
                type="email"
                placeholder="test@test.test"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value);
                  clearFieldError("email");
                }}
                className={cn(
                  "rounded-xl",
                  fieldErrors.email && "border-destructive focus-visible:ring-destructive",
                )}
                autoComplete="email"
                aria-invalid={fieldErrors.email !== undefined}
                aria-describedby={fieldErrors.email ? "email-error" : undefined}
              />
              {fieldErrors.email && (
                <p id="email-error" role="alert" className="text-xs font-medium text-destructive">
                  {fieldErrors.email}
                </p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="password" className="text-xs">Password</Label>
              <Input
                id="password"
                type="password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value);
                  clearFieldError("password");
                  clearFieldError("confirmPassword");
                }}
                className={cn(
                  "rounded-xl",
                  fieldErrors.password && "border-destructive focus-visible:ring-destructive",
                )}
                autoComplete={isSignUp ? "new-password" : "current-password"}
                aria-invalid={fieldErrors.password !== undefined}
                aria-describedby={fieldErrors.password ? "password-error" : undefined}
              />
              {fieldErrors.password && (
                <p id="password-error" role="alert" className="text-xs font-medium text-destructive">
                  {fieldErrors.password}
                </p>
              )}
            </div>

            {!isSignUp && (
              <p className="text-xs text-muted-foreground">
                Stub credentials for local development: <span className="font-mono">test@test.test / 1234</span>
              </p>
            )}

            {isSignUp && (
              <div className="space-y-2">
                <Label htmlFor="confirmPassword" className="text-xs">Confirm password</Label>
                <Input
                  id="confirmPassword"
                  type="password"
                  placeholder="••••••••"
                  value={confirmPassword}
                  onChange={(e) => {
                    setConfirmPassword(e.target.value);
                    clearFieldError("confirmPassword");
                  }}
                  className={cn(
                    "rounded-xl",
                    fieldErrors.confirmPassword && "border-destructive focus-visible:ring-destructive",
                  )}
                  autoComplete="new-password"
                  aria-invalid={fieldErrors.confirmPassword !== undefined}
                  aria-describedby={fieldErrors.confirmPassword ? "confirm-password-error" : undefined}
                />
                {fieldErrors.confirmPassword && (
                  <p id="confirm-password-error" role="alert" className="text-xs font-medium text-destructive">
                    {fieldErrors.confirmPassword}
                  </p>
                )}
              </div>
            )}

            {isSignUp && (
              <div className="space-y-2">
                <label className="flex items-start gap-2.5 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={gdprAccepted}
                    onChange={(e) => {
                      setGdprAccepted(e.target.checked);
                      clearFieldError("gdprAccepted");
                    }}
                    className="mt-0.5 h-4 w-4 rounded border-border accent-primary"
                    aria-invalid={fieldErrors.gdprAccepted !== undefined}
                    aria-describedby={fieldErrors.gdprAccepted ? "gdpr-accepted-error" : undefined}
                  />
                  <span className="text-xs text-muted-foreground leading-relaxed">
                    I agree to the{" "}
                    <NavLink to="/privacy" className="text-primary hover:underline">Privacy Policy</NavLink>{" "}
                    and{" "}
                    <NavLink to="/terms" className="text-primary hover:underline">Terms of Service</NavLink>.
                    Your data is processed in accordance with GDPR.
                  </span>
                </label>
                {fieldErrors.gdprAccepted && (
                  <p id="gdpr-accepted-error" role="alert" className="text-xs font-medium text-destructive">
                    {fieldErrors.gdprAccepted}
                  </p>
                )}
              </div>
            )}

            {!isSignUp && (
              <div className="text-right">
                <button type="button" className="text-xs text-muted-foreground hover:text-foreground transition-colors">
                  Forgot password?
                </button>
              </div>
            )}

            <Button type="submit" className="w-full rounded-xl" disabled={isSubmitPending}>
              {isSubmitPending
                ? isSignUp
                  ? "Creating account..."
                  : "Signing in..."
                : isSignUp
                  ? "Create account"
                  : "Sign in"}
            </Button>
          </form>

        </div>

        <p className="mt-6 text-center text-xs text-muted-foreground">
          {isSignUp ? "Already have an account?" : "Don't have an account?"}{" "}
          <button
            onClick={switchAuthMode}
            className="font-medium text-primary hover:underline"
          >
            {isSignUp ? "Sign in" : "Create one"}
          </button>
        </p>
      </motion.div>
    </div>
  );
};

export default Login;
