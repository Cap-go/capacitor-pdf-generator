# @capgo/capacitor-pdf-generator

Create PDF files from HTML strings or web pages in your Capacitor app, then return them as base64 or open the share sheet. A modern port of the Cordova pdf-generator plugin.

<a href="https://capgo.app/?ref=plugin_pdf_generator"><img src="https://capgo.app/readme-banner.svg?repo=Cap-go/capacitor-pdf-generator" alt="Capgo - Instant updates for Capacitor" /></a>

<div align="center">
  <p><b>Capgo</b>: push fixes to your Capacitor users in minutes, build signed iOS and Android apps without a Mac, and roll back in one click.</p>
  <h2><a href="https://capgo.app/register/?ref=plugin_pdf_generator">➡️ Get started for free</a></h2>
  <p>14-day unlimited free trial. No credit card required</p>
  <p><a href="https://capgo.app/consulting/?ref=plugin_pdf_generator">Missing a feature? We'll build the plugin for you 💪</a></p>
</div>

<p align="center">
  <img src="https://raw.githubusercontent.com/Cap-go/capacitor-pdf-generator/main/assets/github-social-preview.png" alt="@capgo/capacitor-pdf-generator for Capacitor apps" width="300" />
</p>

## Key features

- **From HTML**: `fromData()` renders an HTML string to PDF.
- **From a URL**: `fromURL()` renders a remote page to PDF.
- **Output**: get `base64` data or open the native share dialog with a file name.
- **Layout**: A3 or A4 pages in portrait or landscape.
- **Platforms**: iOS and Android. Rendered with the native WebView on each platform. Not available on web.

Generate PDF files from HTML strings or remote URLs.

Port of the Cordova [pdf-generator](https://github.com/feedhenry-staff/pdf-generator) plugin for Capacitor with a modernized native implementation.

## Documentation

The most complete doc is available here: https://capgo.app/docs/plugins/pdf-generator/

## Compatibility

| Plugin version | Capacitor compatibility | Maintained |
| -------------- | ----------------------- | ---------- |
| v8.\*.\*       | v8.\*.\*                | ✅          |
| v7.\*.\*       | v7.\*.\*                | On demand   |
| v6.\*.\*       | v6.\*.\*                | ❌          |
| v5.\*.\*       | v5.\*.\*                | ❌          |

> **Note:** The major version of this plugin follows the major version of Capacitor. Use the version that matches your Capacitor installation (e.g., plugin v8 for Capacitor 8). Only the latest major version is actively maintained.

## Install

You can use our AI-Assisted Setup to install the plugin. Add the Capgo skills to your AI tool using the following command:

```bash
npx skills add https://github.com/cap-go/capacitor-skills --skill capacitor-plugins
```

Then use the following prompt:

```text
Use the `capacitor-plugins` skill from `cap-go/capacitor-skills` to install the `@capgo/capacitor-pdf-generator` plugin in my project.
```

If you prefer Manual Setup, install the plugin by running the following commands and follow the platform-specific instructions below:

```bash
npm install @capgo/capacitor-pdf-generator
npx cap sync
```

## Usage

```ts
import { PdfGenerator } from '@capgo/capacitor-pdf-generator';

const result = await PdfGenerator.fromData({
  data: '<html><body><h1>Hello Capgo</h1></body></html>',
  documentSize: 'A4',
  orientation: 'portrait',
  type: 'base64',
  fileName: 'example.pdf',
});

if (result.type === 'base64') {
  console.log(result.base64);
}
```

## API

<docgen-index>

* [`fromURL(...)`](#fromurl)
* [`fromData(...)`](#fromdata)
* [`getPluginVersion()`](#getpluginversion)
* [Interfaces](#interfaces)
* [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### fromURL(...)

```typescript
fromURL(options: PdfGeneratorFromUrlOptions) => Promise<PdfGeneratorResult>
```

Generates a PDF from the provided URL.

| Param         | Type                                                                              |
| ------------- | --------------------------------------------------------------------------------- |
| **`options`** | <code><a href="#pdfgeneratorfromurloptions">PdfGeneratorFromUrlOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#pdfgeneratorresult">PdfGeneratorResult</a>&gt;</code>

--------------------


### fromData(...)

```typescript
fromData(options: PdfGeneratorFromDataOptions) => Promise<PdfGeneratorResult>
```

Generates a PDF from a raw HTML string.

| Param         | Type                                                                                |
| ------------- | ----------------------------------------------------------------------------------- |
| **`options`** | <code><a href="#pdfgeneratorfromdataoptions">PdfGeneratorFromDataOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#pdfgeneratorresult">PdfGeneratorResult</a>&gt;</code>

--------------------


### getPluginVersion()

```typescript
getPluginVersion() => Promise<{ version: string; }>
```

Get the native Capacitor plugin version

**Returns:** <code>Promise&lt;{ version: string; }&gt;</code>

--------------------


### Interfaces


#### PdfGeneratorFromUrlOptions

| Prop      | Type                |
| --------- | ------------------- |
| **`url`** | <code>string</code> |


#### PdfGeneratorFromDataOptions

| Prop          | Type                | Description                                                                                                    |
| ------------- | ------------------- | -------------------------------------------------------------------------------------------------------------- |
| **`data`**    | <code>string</code> | HTML document to render.                                                                                       |
| **`baseUrl`** | <code>string</code> | Base URL to use when resolving relative resources inside the HTML string. When omitted, `about:blank` is used. |


### Type Aliases


#### PdfGeneratorResult

<code>{ type: 'base64'; base64: string; } | { type: 'share'; completed: boolean; }</code>

</docgen-api>
