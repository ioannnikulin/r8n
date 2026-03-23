import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";

// https://vitejs.dev/config/
export default defineConfig(() => ({
  server: {
    proxy: {
      "^/api(/|$)": {
        target: "https://localhost:8080",
        changeOrigin: true,
        secure: true,
        rewrite: (requestPath) => requestPath.replace(/^\/api/, ""),
      },
    },
    host: "127.0.0.1",
    port: 5173,
    hmr: {
      overlay: false,
    },
  },
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
}));
