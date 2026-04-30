import { test, expect } from "playwright/test";

test.describe("GDPR / Export API", () => {
  const EXPORT_START_PATH = "/api/export/start";
  const EXPORT_STATUS_PATH = "/api/export/status";
  const EXPORT_DOWNLOAD_PATH = "/api/export/download";

  test("should return 401 Unauthorized when no token is provided", async ({ request }) => {
    const response = await request.post(EXPORT_START_PATH);
    expect(response.status()).toBe(401);
  });

  test("should return GDPR data when authenticated with valid token", async ({ request }) => {
    // 1. Get CSRF token
    const csrfResponse = await request.get("/api/auth/csrf");
    const cookies = csrfResponse.headers()["set-cookie"] || "";
    const csrfToken = cookies.match(/XSRF-TOKEN=([^;]+)/)?.[1];
    expect(csrfToken).toBeDefined();

    // 2. Login to get a valid token
    const loginResponse = await request.post("/api/auth/login", {
      data: {
        login: "test@test.test",
        password: "1234",
      },
      headers: {
        "X-XSRF-TOKEN": csrfToken!,
      },
    });
    expect(loginResponse.status()).toBe(200);
    const { accessToken } = await loginResponse.json();

    // 3. Use the token to get GDPR data
    const startResponse = await request.post(EXPORT_START_PATH, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });
    expect(startResponse.status()).toBe(202); // Accepted

    // 4. Check status (with stub data, it should be immediately ready)
    const statusResponse = await request.get(EXPORT_STATUS_PATH, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });
    expect(statusResponse.status()).toBe(200);
    const status = await statusResponse.json();
    expect(status).toHaveProperty("status", "COMPLETED");

    // 5. Download data
    const downloadResponse = await request.get(EXPORT_DOWNLOAD_PATH, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });
    expect(downloadResponse.status()).toBe(200);
    const data = await downloadResponse.json();
    expect(data).toHaveProperty("id", "00000000-0000-0000-0000-000000000000");
    expect(data).toHaveProperty("status", "ACTIVE");
    expect(data).toHaveProperty("personalIdentifiableInformation");
    expect(data.personalIdentifiableInformation).toHaveProperty("name", "Test Testsson");
    expect(data.personalIdentifiableInformation).toHaveProperty("email", "test@test.test");
    expect(data).toHaveProperty("consents");
    expect(data.consents.items[0]).toHaveProperty("type", "PRIVACY_POLICY");
  });
});