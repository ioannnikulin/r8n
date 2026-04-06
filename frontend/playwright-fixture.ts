import {
  expect,
  test as base,
  type ConsoleMessage,
  type Page,
  type TestInfo,
} from "@playwright/test";

type BrowserIssue = {
  source: "console" | "pageerror";
  pageUrl: string;
  message: string;
  type?: string;
  location?: string;
  stack?: string;
};

type BrowserIssueAttachment = {
  issueCount: number;
  issues: BrowserIssue[];
  pages: {
    url: string;
    title?: string;
    screenshotAttachment?: string;
  }[];
};

const TRACKED_CONSOLE_TYPES = new Set(["warning", "error"]);

const formatLocation = (message: ConsoleMessage) => {
  const location = message.location();
  if (!location.url) {
    return undefined;
  }

  const line = location.lineNumber + 1;
  const column = location.columnNumber + 1;

  return `${location.url}:${line}:${column}`;
};

const formatIssue = (issue: BrowserIssue) => {
  const header = issue.source === "console"
    ? `[console:${issue.type}] ${issue.message}`
    : `[pageerror] ${issue.message}`;

  const details = [`page=${issue.pageUrl}`];
  if (issue.location) {
    details.push(`location=${issue.location}`);
  }
  if (issue.stack) {
    details.push(`stack=${issue.stack}`);
  }

  return `${header}\n${details.join("\n")}`;
};

const attachPageErrorListener = (page: Page, issues: BrowserIssue[]) => {
  const handlePageError = (error: Error) => {
    issues.push({
      source: "pageerror",
      pageUrl: page.url(),
      message: error.message,
      stack: error.stack,
    });
  };

  page.on("pageerror", handlePageError);

  return () => {
    page.off("pageerror", handlePageError);
  };
};

const test = base.extend<{ _consoleAudit: void }>({
  _consoleAudit: [async ({ context }, use, testInfo) => {
    const issues: BrowserIssue[] = [];
    const detachPageErrorListeners = new Map<Page, () => void>();

    const handleConsole = (message: ConsoleMessage) => {
      if (!TRACKED_CONSOLE_TYPES.has(message.type())) {
        return;
      }

      issues.push({
        source: "console",
        type: message.type(),
        pageUrl: message.page()?.url() ?? "unknown",
        message: message.text(),
        location: formatLocation(message),
      });
    };

    const handlePage = (page: Page) => {
      if (detachPageErrorListeners.has(page)) {
        return;
      }

      detachPageErrorListeners.set(page, attachPageErrorListener(page, issues));
    };

    context.on("console", handleConsole);
    context.on("page", handlePage);

    for (const page of context.pages()) {
      handlePage(page);
    }

    await use();

    context.off("console", handleConsole);
    context.off("page", handlePage);

    for (const detach of detachPageErrorListeners.values()) {
      detach();
    }

    if (issues.length > 0) {
      await attachIssuesReport(testInfo, context.pages(), issues);
    }

    expect(issues, buildFailureMessage(issues)).toEqual([]);
  }, { auto: true }],
});

const buildFailureMessage = (issues: BrowserIssue[]) => {
  if (issues.length === 0) {
    return "No browser console issues captured.";
  }

  return [
    `${issues.length} browser console issue(s) captured during the test.`,
    ...issues.map(formatIssue),
  ].join("\n\n");
};

const attachIssuesReport = async (testInfo: TestInfo, pages: Page[], issues: BrowserIssue[]) => {
  const attachment = await buildIssuesAttachment(testInfo, pages, issues);

  await testInfo.attach("browser-console-issues", {
    body: issues.map(formatIssue).join("\n\n"),
    contentType: "text/plain",
  });

  await testInfo.attach("browser-console-issues.json", {
    body: JSON.stringify(attachment, null, 2),
    contentType: "application/json",
  });
};

const buildIssuesAttachment = async (
  testInfo: TestInfo,
  pages: Page[],
  issues: BrowserIssue[],
): Promise<BrowserIssueAttachment> => {
  const pagesWithIssues = pages.filter((page) =>
    !page.isClosed() && issues.some((issue) => issue.pageUrl === page.url()),
  );

  const pageDiagnostics = await Promise.all(
    pagesWithIssues.map(async (page, index) => {
      const screenshotAttachment = `browser-console-page-${index + 1}.png`;

      await testInfo.attach(screenshotAttachment, {
        body: await page.screenshot({ fullPage: true }),
        contentType: "image/png",
      });

      return {
        url: page.url(),
        title: await readPageTitle(page),
        screenshotAttachment,
      };
    }),
  );

  return {
    issueCount: issues.length,
    issues,
    pages: pageDiagnostics,
  };
};

const readPageTitle = async (page: Page) => {
  try {
    return await page.title();
  } catch {
    return undefined;
  }
};

export { test, expect };
