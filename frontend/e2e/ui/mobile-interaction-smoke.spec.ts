import { expect, test } from "../../playwright-fixture";

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
  // The "/" route redirects to /lists for authenticated users.
  await expect(page).toHaveURL(/\/lists$/);

  await waitForUiToSettle(page);
});

test("mobile create review form renders and accepts input without console issues", async ({ page }) => {
  await page.goto("/");

  await page.getByRole("link", { name: "Create" }).click();
  await expect(page).toHaveURL(/\/create$/);

  await page.getByLabel("What are you reviewing?").fill("Cappuccino");
  await page.getByRole("spinbutton").fill("7");
  await page.getByLabel("Objective Notes").fill(
    "Medium body, balanced acidity, and stable milk foam."
  );
  await page.getByLabel("Your Opinion").fill(
    "Comforting everyday cup that feels dependable."
  );

  // Supplier search is required before submitting — verify the input is present
  await expect(page.getByPlaceholder("Search restaurant, brand, shop...")).toBeVisible();

  await waitForUiToSettle(page);
});

test("mobile create list flow stays clean when started from bottom navigation", async ({ page }) => {
  await page.goto("/lists/create");

  await page.getByLabel("List name").fill("Late-night coffee spots");
  await page.getByLabel("Description").fill(
    "Reliable places that still serve good coffee after regular work hours."
  );
  await page.getByRole("button", { name: "Create List" }).click();

  await expect(page).toHaveURL(/\/lists$/);
  await expect(page.getByRole("heading", { name: "My Lists" })).toBeVisible();

  await waitForUiToSettle(page);
});

test("mobile discover page renders search and lists section without console issues", async ({ page }) => {
  await page.goto("/discover");

  await expect(page.getByPlaceholder("Search everything...")).toBeVisible();
  await expect(page.getByRole("heading", { name: "Lists" })).toBeVisible();

  await waitForUiToSettle(page);
});
