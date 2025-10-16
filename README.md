# @capgo/capacitor-pdf-generator
 <a href="https://capgo.app/"><img src='https://raw.githubusercontent.com/Cap-go/capgo/main/assets/capgo_banner.png' alt='Capgo - Instant updates for capacitor'/></a>

<div align="center">
  <h2><a href="https://capgo.app/?ref=plugin"> ➡️ Get Instant updates for your App with Capgo</a></h2>
  <h2><a href="https://capgo.app/consulting/?ref=plugin"> Missing a feature? We’ll build the plugin for you 💪</a></h2>
</div>


Generate PDF files from HTML strings or remote URLs.

Port of the Cordova [pdf-generator](https://github.com/feedhenry-staff/pdf-generator) plugin for Capacitor with a modernized native implementation.

## Install

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
