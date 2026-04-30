import "@testing-library/jest-dom";
import { vi } from "vitest";

vi.stubEnv("VITE_AUTH_EMAIL_MAX_LENGTH", "254");
vi.stubEnv("VITE_AUTH_PASSWORD_MAX_LENGTH", "128");
vi.stubEnv("VITE_REGISTRATION_PASSWORD_MIN_LENGTH", "12");
vi.stubEnv("VITE_PROFILE_NAME_MAX_LENGTH", "255");

Object.defineProperty(window, "matchMedia", {
  writable: true,
  value: (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => {},
  }),
});

const localStorageMock = (() => {
  let store: Record<string, string> = {};

  return {
    getItem: (key: string) => store[key] || null,
    setItem: (key: string, value: string) => {
      store[key] = value.toString();
    },
    removeItem: (key: string) => {
      delete store[key];
    },
    clear: () => {
      store = {};
    },
  };
})();

Object.defineProperty(window, "localStorage", {
  value: localStorageMock,
});
