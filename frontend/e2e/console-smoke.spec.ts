import { expect, test } from "../playwright-fixture";

const APP_ROUTES = [
  "/",
  "/discover",
  "/lists",
  "/requests",
  "/profile",
  "/profile/jane-doe",
  "/supplier",
  "/supplier/coda-dessert-bar",
  "/list/cappuccino",
  "/create",
  "/lists/create",
  "/settings",
  "/login",
  "/create-profile",
];

for (const route of APP_ROUTES) {
  test(`route ${route} renders without browser console warnings or errors`, async ({ page }) => {
    await page.goto(route);
    await expect(page.locator("body")).toBeVisible();

    // Give route-level effects and React warnings time to surface in the console.
    await page.waitForTimeout(250);
  });
}
