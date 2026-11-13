import { test, expect } from "@playwright/test";
const root = "../docs/deliveries/TASK-WEB001/evidence";
test("approved desktop and mobile design evidence", async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 1024 });
  await page.goto("/");
  await page.evaluate(() => document.fonts.ready);
  await page.screenshot({
    path: `${root}/approved-login-desktop.png`,
    fullPage: true,
  });
  await page.getByRole("button", { name: "进入演示" }).click();
  await page.screenshot({
    path: `${root}/approved-home-desktop.png`,
    fullPage: true,
  });
  await page.getByRole("link", { name: "添加记录", exact: true }).click();
  await page.getByLabel("记录时间（本地时间）").fill("2026-11-13T20:30");
  await page.getByLabel("体重（kg）").fill("1");
  await page.getByRole("button", { name: "添加到演示列表" }).click();
  await page.getByRole("tab", { name: "饮食", exact: true }).click();
  await page.getByLabel("食物名称").fill("演示食物");
  await page.getByLabel("分量", { exact: true }).fill("演示分量");
  await page.getByRole("button", { name: "添加到演示列表" }).click();
  await page.getByRole("tab", { name: "训练", exact: true }).click();
  await page.getByLabel("训练名称").fill("演示训练");
  await page.getByLabel("训练时长（分钟）").fill("1");
  await page.getByRole("button", { name: "添加到演示列表" }).click();
  await page
    .getByRole("navigation", { name: "主导航", exact: true })
    .getByRole("link", { name: "首页", exact: true })
    .click();
  await page.getByRole("link", { name: "添加记录", exact: true }).click();
  await page.locator("h1").focus();
  await page.screenshot({
    path: `${root}/approved-records-desktop.png`,
    fullPage: true,
  });
  await page.getByRole("tab", { name: "饮食", exact: true }).click();
  await page.screenshot({
    path: `${root}/approved-food-desktop.png`,
    fullPage: true,
  });
  await page.getByRole("tab", { name: "训练", exact: true }).click();
  await page.screenshot({
    path: `${root}/approved-training-desktop.png`,
    fullPage: true,
  });
  await page.setViewportSize({ width: 390, height: 844 });
  await page.getByRole("tab", { name: "体重", exact: true }).click();
  await page.screenshot({
    path: `${root}/approved-records-mobile.png`,
    fullPage: true,
  });
  await page
    .getByRole("navigation", { name: "小屏导航" })
    .getByRole("link", { name: "首页", exact: true })
    .click();
  await page.screenshot({
    path: `${root}/approved-home-mobile.png`,
    fullPage: true,
  });
  await page.getByRole("button", { name: "退出登录" }).click();
  await page.screenshot({
    path: `${root}/approved-login-mobile.png`,
    fullPage: true,
  });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
});
