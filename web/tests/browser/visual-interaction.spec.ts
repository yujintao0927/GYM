import { test, expect } from "@playwright/test";
test("approved record console tabs retain labelled fields and keyboard support", async ({
  page,
}) => {
  await page.goto("/");
  await page.getByRole("button", { name: "进入演示" }).click();
  await page.getByRole("link", { name: "添加记录", exact: true }).click();
  const food = page.getByRole("tab", { name: "饮食", exact: true });
  await food.click();
  await expect(food).toHaveAttribute("aria-selected", "true");
  await expect(page.getByLabel("食物名称")).toBeVisible();
  await expect(page.getByLabel("分量", { exact: true })).toBeVisible();
  await food.press("ArrowRight");
  await expect(
    page.getByRole("tab", { name: "训练", exact: true }),
  ).toBeFocused();
  await expect(page.getByLabel("训练名称")).toBeVisible();
  await expect(page.getByLabel("训练时长（分钟）")).toBeVisible();
});

test("small screen navigation follows content without covering the record list", async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/");
  await page.getByRole("button", { name: "进入演示" }).click();
  await page.getByRole("link", { name: "添加记录", exact: true }).click();
  const nav = await page.locator(".mobile-nav").boundingBox();
  const history = await page.locator(".record-history").boundingBox();
  expect(nav!.y).toBeGreaterThanOrEqual(history!.y + history!.height);
});

test("local UI fonts load and photography is available", async ({ page }) => {
  await page.goto("/");
  await page.evaluate(() => document.fonts.ready);
  const fonts = await page.evaluate(async () => ({
    loaded: document.fonts.check('600 20px "Gym Sans"', "欢迎回来"),
    family: getComputedStyle(document.body).fontFamily,
    faces: [...document.fonts].map((f) => ({
      family: f.family,
      status: f.status,
      weight: f.weight,
    })),
  }));
  console.log(JSON.stringify(fonts));
  expect(fonts.loaded).toBe(true);
  for (const img of await page.locator("img").all()) {
    expect(
      await img.evaluate(
        (e: HTMLImageElement) => e.complete && e.naturalWidth > 0,
      ),
    ).toBe(true);
  }
});

test("food and training field errors never overlap the record history", async ({
  page,
}) => {
  await page.setViewportSize({ width: 1440, height: 1024 });
  await page.goto("/");
  await page.getByRole("button", { name: "进入演示" }).click();
  await page.getByRole("link", { name: "添加记录", exact: true }).click();
  for (const name of ["饮食", "训练"]) {
    await page.getByRole("tab", { name, exact: true }).click();
    await page.getByRole("button", { name: "添加到演示列表" }).click();
    const button = await page
      .getByRole("button", { name: "添加到演示列表" })
      .boundingBox();
    const history = await page.locator(".record-history").boundingBox();
    expect(button!.y + button!.height).toBeLessThanOrEqual(history!.y);
  }
});
