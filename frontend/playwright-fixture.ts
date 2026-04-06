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

  return `${header}\n${details.join("\n")}`;
};

const attachPageErrorListener = (page: Page, issues: BrowserIssue[]) => {
  const handlePageError = (error: Error) => {
    issues.push({
      source: "pageerror",
      pageUrl: page.url(),
      message: error.message,
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
      await attachIssuesReport(testInfo, issues);
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

const attachIssuesReport = async (testInfo: TestInfo, issues: BrowserIssue[]) => {
  await testInfo.attach("browser-console-issues", {
    body: issues.map(formatIssue).join("\n\n"),
    contentType: "text/plain",
  });
};

export { test, expect };
