import { expect, test } from "../playwright-fixture";

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

  await page.getByRole("link", { name: "Profile" }).click();
  await expect(page).toHaveURL(/\/profile$/);

  await page.getByRole("link", { name: "Settings" }).click();
  await expect(page).toHaveURL(/\/settings$/);

  await page.getByRole("link", { name: "Write Review" }).click();
  await expect(page).toHaveURL(/\/create$/);

  await page.getByRole("link", { name: "Create List" }).click();
  await expect(page).toHaveURL(/\/lists\/create$/);

  await waitForUiToSettle(page);
});

test("access request dialog flow stays clean after opening, selecting a list, and submitting", async ({ page }) => {
  await page.goto("/discover");

  await page.getByRole("button", { name: "Request Access" }).first().click();

  const dialog = page.getByRole("dialog");
  await expect(dialog).toBeVisible();
  await expect(dialog.getByRole("heading", { name: "Request Access" })).toBeVisible();

  await dialog.getByRole("button", { name: "Merge into existing list" }).click();
  await dialog.getByRole("button", { name: "Best espresso in Berlin" }).click();
  await dialog.getByRole("button", { name: "Request & Merge" }).click();

  await expect(dialog).not.toBeVisible();
  await expect(page.getByRole("button", { name: "Request Sent" }).first()).toBeDisabled();

  await waitForUiToSettle(page);
});

test("create review flow stays clean after filling and submitting the form", async ({ page }) => {
  await page.goto("/create");

  await page.getByLabel("What are you reviewing?").fill("Flat White");
  await page.getByRole("button", { name: /Bonanza Coffee/ }).click();
  await page.getByRole("button", { name: "8" }).click();
  await page.getByLabel("Objective Notes").fill(
    "Balanced extraction, silky milk texture, served warm."
  );
  await page.getByLabel("Your Opinion").fill(
    "Comforting and well-structured cup with a clean finish."
  );
  await page.getByLabel("Add to List (optional)").selectOption("Best espresso in Berlin");
  await page.getByRole("button", { name: "Publish Review" }).click();

  await expect(page).toHaveURL(/\/$/);
  await expect(page.getByRole("heading", { name: "Good evening, Jane" })).toBeVisible();

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

test("requests management interactions stay clean when hiding and restoring an incoming request", async ({ page }) => {
  await page.goto("/requests");

  await page.getByRole("button", { name: "Hide" }).first().click();
  await expect(page.getByRole("button", { name: /Show hidden/ })).toBeVisible();

  await page.getByRole("button", { name: /Show hidden/ }).click();
  await expect(page.getByRole("button", { name: "Unhide" })).toBeVisible();

  await page.getByRole("button", { name: "Unhide" }).click();
  await expect(page.getByRole("button", { name: /Show hidden/ })).toHaveCount(0);

  await waitForUiToSettle(page);
});
