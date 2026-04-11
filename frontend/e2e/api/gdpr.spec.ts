import { test, expect } from "playwright/test";

test.describe("GDPR / Users API", () => {
  const GDPR_EXPORT_PATH = "/users/export";

  test("should return 401 Unauthorized when no token is provided", async ({ request }) => {
    const response = await request.get(GDPR_EXPORT_PATH);
    expect(response.status()).toBe(401);
  });

  test("should return GDPR data when authenticated with valid token", async ({ request }) => {
    // 1. Login to get a valid token
    const loginResponse = await request.post("/auth/login", {
      data: {
        login: "test@test.test",
        password: "1234",
      },
    });
    expect(loginResponse.status()).toBe(200);
    const { accessToken } = await loginResponse.json();

    // 2. Use the token to get GDPR data
    const response = await request.get(GDPR_EXPORT_PATH, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });

    expect(response.status()).toBe(200);
    const data = await response.json();
    expect(data).toHaveProperty("id", "00000000-0000-0000-0000-000000000000");
    expect(data).toHaveProperty("status", "ACTIVE");
    expect(data).toHaveProperty("personalIdentifiableInformation");
    expect(data.personalIdentifiableInformation).toHaveProperty("name", "Test Testsson");
    expect(data.personalIdentifiableInformation).toHaveProperty("email", "test@test.test");
    expect(data).toHaveProperty("consents");
    expect(data.consents.items[0]).toHaveProperty("type", "PRIVACY_POLICY");
  });
});
