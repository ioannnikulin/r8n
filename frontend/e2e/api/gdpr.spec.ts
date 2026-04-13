import { test, expect } from "playwright/test";

test.describe("GDPR / Export API", () => {
  const EXPORT_START_PATH = "/export/users/00000000-0000-0000-0000-000000000000/start";
  const EXPORT_STATUS_PATH = "/export/users/00000000-0000-0000-0000-000000000000/status";
  const EXPORT_DOWNLOAD_PATH = "/export/users/00000000-0000-0000-0000-000000000000/download";

  test("should return 401 Unauthorized when no token is provided", async ({ request }) => {
    const response = await request.post(EXPORT_START_PATH);
    expect(response.status()).toBe(401);
  });

  test("should return GDPR data when authenticated with stub token", async ({ request }) => {
    // Start export
    const startResponse = await request.post(EXPORT_START_PATH, {
      headers: {
        Authorization: "Bearer stub-access-token-123",
      },
    });
    expect(startResponse.status()).toBe(202); // Accepted

    // Check status (with stub data, it should be immediately completed)
    const statusResponse = await request.get(EXPORT_STATUS_PATH, {
      headers: {
        Authorization: "Bearer stub-access-token-123",
      },
    });
    expect(statusResponse.status()).toBe(200);
    const status = await statusResponse.json();
    expect(status).toHaveProperty("status", "COMPLETED");

    // Download data
    const downloadResponse = await request.get(EXPORT_DOWNLOAD_PATH, {
      headers: {
        Authorization: "Bearer stub-access-token-123",
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
