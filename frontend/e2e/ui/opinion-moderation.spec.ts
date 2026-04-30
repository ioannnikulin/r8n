import { expect, test } from "../../playwright-fixture";

const PENDING_OPINION_ID = "e2e-opinion-pending";

const moderatedOpinion = {
  componentMark: null,
  components: [],
  id: PENDING_OPINION_ID,
  mark: 8.5,
  objective: ["Receipt from 2026-04-12", "Paid 3.20 EUR"],
  owner: "e2e-reviewer",
  ownerName: "E2E Reviewer",
  status: "PUBLISHED",
  subject: "e2e-subject",
  subjective: ["Consistent coffee quality", "Mentions a staff member by name"],
  subjectName: "E2E Espresso Lab",
  timestamp: new Date().toISOString(),
};

test("moderator can review and approve a pending opinion", async ({ page }) => {
  let approveRequested = false;

  await page.route(`**/api/opinions/${PENDING_OPINION_ID}/approve`, async (route) => {
    approveRequested = true;
    expect(route.request().method()).toBe("POST");
    expect(route.request().headers().authorization).toBe("Bearer e2e-access-token");

    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify(moderatedOpinion),
    });
  });

  await page.goto("/moderation/opinions");

  await expect(page.getByRole("heading", { name: "Opinion Moderation" })).toBeVisible();
  await expect(page.getByText("E2E Espresso Lab")).toBeVisible();
  await expect(page.getByText(/Submitted by E2E Reviewer/)).toBeVisible();
  await expect(page.getByText(/Consistent coffee quality/)).toBeVisible();
  await expect(page.getByText(/Receipt from 2026-04-12/)).toBeVisible();
  await expect(page.getByText("1 pending")).toBeVisible();

  await page.getByRole("button", { name: "Approve" }).click();
  await expect.poll(() => approveRequested).toBe(true);

  await page.waitForTimeout(250);
});

test("moderator must enter a rejection reason before rejecting", async ({ page }) => {
  let rejectionBody: unknown;

  await page.route(`**/api/opinions/${PENDING_OPINION_ID}/reject`, async (route) => {
    rejectionBody = route.request().postDataJSON();
    expect(route.request().method()).toBe("POST");
    expect(route.request().headers().authorization).toBe("Bearer e2e-access-token");

    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        ...moderatedOpinion,
        status: "REJECTED",
      }),
    });
  });

  await page.goto("/moderation/opinions");

  await page.getByRole("button", { name: "Reject" }).click();
  await expect(page.getByRole("dialog", { name: "Reject opinion" })).toBeVisible();

  await page.getByRole("button", { name: "Reject with reason" }).click();
  await expect(page.getByText("Rejection reason is required.")).toBeVisible();
  expect(rejectionBody).toBeUndefined();

  await page.getByLabel("Rejection reason").fill("  Needs factual support.  ");
  await page.getByRole("button", { name: "Reject with reason" }).click();

  await expect.poll(() => rejectionBody).toEqual({
    reason: "Needs factual support.",
  });

  await page.waitForTimeout(250);
});
