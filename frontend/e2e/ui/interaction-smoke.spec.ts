import { expect, test } from "../../playwright-fixture";

const waitForUiToSettle = async (page: Parameters<typeof test>[0]["page"]) => {
  await page.waitForTimeout(250);
};

test("sidebar and quick actions navigate through key routes without browser console issues", async ({ page }) => {
  await page.goto("/");

  await page.getByRole("link", { name: "Discover" }).click();
  await expect(page).toHaveURL(/\/discover$/);

  await page.getByRole("link", { name: "My Lists" }).click();
  await expect(page).toHaveURL(/\/lists$/);

  await page.getByRole("link", { name: "Requests" }).click();
  await expect(page).toHaveURL(/\/requests$/);

  await page.getByRole("link", { name: /open .*profile/i }).click();
  await expect(page).toHaveURL(/\/profile$/);

  await page.getByRole("link", { name: "Settings" }).click();
  await expect(page).toHaveURL(/\/settings$/);

  await page.getByRole("link", { name: "Write Review" }).click();
  await expect(page).toHaveURL(/\/create$/);

  await page.getByRole("link", { name: "Create List" }).click();
  await expect(page).toHaveURL(/\/lists\/create$/);

  await waitForUiToSettle(page);
});

test("discover page renders search and lists section without console issues", async ({ page }) => {
  await page.goto("/discover");

  await expect(page.getByPlaceholder("Search everything...")).toBeVisible();
  await expect(page.getByRole("heading", { name: "Lists" })).toBeVisible();

  await waitForUiToSettle(page);
});

test("create review form renders and accepts input without console issues", async ({ page }) => {
  await page.goto("/create");

  await page.getByLabel("What are you reviewing?").fill("Flat White");
  await page.getByRole("spinbutton").fill("8");
  await page.getByLabel("Objective Notes").fill(
    "Balanced extraction, silky milk texture, served warm."
  );
  await page.getByLabel("Your Opinion").fill(
    "Comforting and well-structured cup with a clean finish."
  );

  // Supplier search is required before submitting — verify the input is present
  await expect(page.getByPlaceholder("Search restaurant, brand, shop...")).toBeVisible();

  await waitForUiToSettle(page);
});

test("create list flow stays clean after changing visibility and submitting", async ({ page }) => {
  await page.goto("/lists/create");

  await page.getByLabel("List name").fill("Quiet cafes for writing");
  await page.getByLabel("Description").fill(
    "Calm spots with stable Wi-Fi, good coffee, and enough space to focus."
  );
  await page.getByRole("button", { name: "Searchable Discoverable by others" }).click();
  await page.getByRole("button", { name: "Create List" }).click();

  await expect(page).toHaveURL(/\/lists$/);
  await expect(page.getByRole("heading", { name: "My Lists" })).toBeVisible();

  await waitForUiToSettle(page);
});

test("requests page renders incoming and outgoing sections without console issues", async ({ page }) => {
  await page.goto("/requests");

  await expect(page.getByRole("heading", { name: "Access Requests" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Incoming" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Outgoing" })).toBeVisible();

  await waitForUiToSettle(page);
});
