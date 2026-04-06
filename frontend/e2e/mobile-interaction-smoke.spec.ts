import { expect, test } from "../playwright-fixture";

const MOBILE_VIEWPORT = { width: 390, height: 844 };

const waitForUiToSettle = async (page: { waitForTimeout: (timeout: number) => Promise<void> }) => {
  await page.waitForTimeout(250);
};

test.beforeEach(async ({ page }) => {
  await page.setViewportSize(MOBILE_VIEWPORT);
});

test("mobile bottom navigation stays clean across primary routes", async ({ page }) => {
  await page.goto("/");

  await expect(page.getByRole("navigation")).toBeVisible();

  await page.getByRole("link", { name: "Discover" }).click();
  await expect(page).toHaveURL(/\/discover$/);

  await page.getByRole("link", { name: "Lists" }).click();
  await expect(page).toHaveURL(/\/lists$/);

  await page.getByRole("link", { name: "Profile" }).click();
  await expect(page).toHaveURL(/\/profile$/);

  await page.getByRole("link", { name: "Feed" }).click();
  await expect(page).toHaveURL(/\/$/);

  await waitForUiToSettle(page);
});

test("mobile create review flow stays clean when started from bottom navigation", async ({ page }) => {
  await page.goto("/");

  await page.getByRole("link", { name: "Create" }).click();
  await expect(page).toHaveURL(/\/create$/);

  await page.getByPlaceholder("e.g., Flat White, Dyson V15 Detect, Margherita Pizza...").fill("Cappuccino");
  await page.getByRole("button", { name: /Bonanza Coffee/ }).click();
  await page.getByRole("button", { name: "7" }).click();
  await page.getByPlaceholder("Factual observations: quality, speed, price, materials...").fill(
    "Medium body, balanced acidity, and stable milk foam."
  );
  await page.getByPlaceholder("Your personal feelings, experience, and honest opinion...").fill(
    "Comforting everyday cup that feels dependable."
  );
  await page.getByRole("button", { name: "Publish Review" }).click();

  await expect(page).toHaveURL(/\/$/);
  await expect(page.getByRole("heading", { name: "Good evening, Jane" })).toBeVisible();

  await waitForUiToSettle(page);
});

test("mobile create list flow stays clean when started from bottom navigation", async ({ page }) => {
  await page.goto("/");

  await page.getByRole("link", { name: "Lists" }).click();
  await expect(page).toHaveURL(/\/lists$/);

  await page.getByRole("link", { name: "New List" }).click();
  await expect(page).toHaveURL(/\/lists\/create$/);

  await page.getByPlaceholder("e.g., Best espresso in Berlin, Top vacuums 2026...").fill("Late-night coffee spots");
  await page.getByPlaceholder("What's this list about? Help others understand your curation...").fill(
    "Reliable places that still serve good coffee after regular work hours."
  );
  await page.getByRole("button", { name: "Create List" }).click();

  await expect(page).toHaveURL(/\/lists$/);
  await expect(page.getByRole("heading", { name: "My Lists" })).toBeVisible();

  await waitForUiToSettle(page);
});

test("mobile access request dialog stays clean after open and copy flow", async ({ page }) => {
  await page.goto("/discover");

  await page.getByRole("button", { name: "Request Access" }).first().click();

  const dialog = page.getByRole("dialog");
  await expect(dialog).toBeVisible();

  await dialog.getByRole("button", { name: "Create a copy" }).click();

  await expect(dialog).not.toBeVisible();
  await expect(page.getByRole("button", { name: "Request Sent" }).first()).toBeDisabled();

  await waitForUiToSettle(page);
});
