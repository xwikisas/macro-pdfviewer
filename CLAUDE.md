# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

PDF Viewer Macro (Pro) — an XWiki SAS paid extension (`com.xwiki.pdfviewer`, repo `xwikisas/macro-pdfviewer`) that
renders PDF attachments, or PDFs from external URLs, inside wiki pages using an embedded pdf.js viewer. Minimal
supported XWiki version: 15.10 (parent `com.xwiki.parent:xwikisas-parent-platform`, see root `pom.xml`); CI builds
with Java 17 (`Jenkinsfile`).

Differences from the generic XWiki org conventions:

- **Issues are tracked on GitHub** (`github.com/xwikisas/macro-pdfviewer/issues`), not JIRA. Commit summaries follow
  the existing history: `<issue title> #<number>` (e.g. `Missing title in iframe #90`), or `[Misc] …` for trivial
  changes.
- Security reports follow the XWiki Pro store policy linked in `SECURITY.md`.
- The macro is licensed: `PDFViewerMacro#execute` checks `Licensor.hasLicensure` on `PDFViewer.Code.WebHome` and
  renders the `missingLicenseMessage` macro when no license is present. Functional tests depend on
  `application-licensing-test-api` for this reason.

## Build & test

Always pass `-B -ntp`. Use the `xwiki-build` skill for details.

- Full build (without functional tests): `mvn clean install -B -ntp`
- Unit tests of the Java module only: `mvn test -B -ntp -pl macro-pdfviewer-api`
- Single unit test: `mvn test -B -ntp -pl macro-pdfviewer-api -Dtest=PDFFileBuilderTest`
- Docker functional tests are only in the `integration-tests` profile (module `macro-pdfviewer-test`):
  `mvn clean install -B -ntp -Pintegration-tests` (build the other modules first so the UI/XAR is installed).
  `AllIT` is the entry point and nests `PDFViewerIT`.
- `macro-pdfviewer-api` enforces a JaCoCo ratio (`xwiki.jacoco.instructionRatio` in its pom) and checkstyle
  (suppressions in `src/checkstyle/checkstyle-suppressions.xml`).
- Jasmine tests of the webjar are disabled in its pom.

## Modules

- `macro-pdfviewer-api` — Java components (`META-INF/components.txt`), the Velocity template
  `templates/pdfviewer/pdfviewer.vm`, and the edit displayer for the `PDFResourceReference` parameter type.
- `macro-pdfviewer-webjar` — repackages the upstream `org.webjars:pdf-js` (version in `pdfjs.version`) and **patches
  it at build time** with `maven-replacer-plugin` regexes on `web/viewer.mjs`, `viewer.html` and `viewer.css`
  (trusted origins from the query string, `withCredentials`, loading `pdfviewer-customizations.js`, a permalink
  button). Bumping pdf.js can silently break these token regexes — check the built output after an upgrade.
- `macro-pdfviewer-ui` — XAR pages: `XWiki.PDFViewerMacro` (JSX used by the iframe), translations,
  `PDFViewer.Code.PDFViewerConfiguration(Class)` (admin settings `trustedOrigins`, `withCredentials`).
- `macro-pdfviewer-xip` — XIP packaging for offline install.
- `macro-pdfviewer-test` — page objects + Docker `@UITest` ITs.

## Architecture: rendering flow and delegated view rights

1. `PDFViewerMacro` (hint `pdfviewer`) splits the `file` / `fileFromExternalUrl` parameters on `.pdf,` and builds one
   `PDFFile` per entry via `PDFFileBuilder`, then binds `params` and `files` into the script context and executes
   `pdfviewer/pdfviewer.vm`.
2. `PDFFileBuilder` resolves each entry:
   - `http(s)://` URLs go through `handleExternalURL`; the `resource/standardURL` resolver detects whether the URL
     actually points to an attachment of this wiki (then rights are checked) or to a truly external file.
   - Otherwise `handleInternalAttachment` tries, for backward compatibility with pre-11.5 macros, the `document`
     parameter, then the current document, then the value as a full attachment reference.
3. `PDFViewerAuthorizationManager` decides view rights. With the `asAuthor` parameter (`1`/`true`/`yes`) and when the
   viewer lacks VIEW but the content author of the macro's secure document (`XWikiDocument.CKEY_SDOC`) has it, rights
   are *delegated*.
4. Delegated files are not served via the normal attachment URL. `DelegatedTokenManager` (an in-memory singleton map,
   so tokens do not survive restarts nor cluster nodes) issues a token bound to (author, attachment, macro origin
   document). The URL becomes `/rest/pdfmacro/contents?access_token=<base64>&form_token=<CSRF>`, served by
   `DefaultPDFMacroResource`, which re-checks token validity, author access and the CSRF token before streaming the
   attachment.
5. `PDFViewerListener` clears tokens on `AttachmentDeletedEvent`.
6. `pdfviewer.vm` renders an iframe to the webjar's `web/viewer.html` with `file`, `trustedOrigins`,
   `withCredentials` and `delegatedView` query parameters; multiple files use a tabbed layout.

`MJSMimeTypeRegistrar` registers the `.mjs` MIME type at first macro execution so the servlet container serves the
pdf.js ES modules correctly.

This delegated-rights path (token issuance, author checks, the REST endpoint, external URL resolution) is the
security-sensitive part of the code base; changes there should come with unit tests in the matching `*Test` classes.
