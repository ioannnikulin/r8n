/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_E2E_BYPASS_AUTH?: string;
  readonly VITE_API_BASE_URL?: string;
  readonly VITE_API_TIMEOUT_MS?: string;
  readonly VITE_AVATAR_MAX_SIZE_BYTES?: string;
  readonly VITE_PROFILE_NAME_MAX_LENGTH?: string;
  readonly VITE_PROFILE_ABOUT_MAX_LENGTH?: string;
  readonly VITE_PROFILE_LOCATION_MAX_LENGTH?: string;
  readonly VITE_AUTH_EMAIL_MAX_LENGTH?: string;
  readonly VITE_AUTH_PASSWORD_MAX_LENGTH?: string;
  readonly VITE_REGISTRATION_PASSWORD_MIN_LENGTH?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
