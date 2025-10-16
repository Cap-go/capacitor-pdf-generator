export type PdfGeneratorDocumentSize = 'A3' | 'A4';

export type PdfGeneratorOutputType = 'base64' | 'share';

export interface PdfGeneratorCommonOptions {
  /**
   * Document size used when rendering the PDF.
   * Only `A3` and `A4` are supported right now and default to `A4`.
   */
  documentSize?: PdfGeneratorDocumentSize;
  /**
   * Page orientation. Defaults to `portrait`.
   */
  orientation?: 'portrait' | 'landscape';
  /**
   * @deprecated Use `orientation` instead. Kept for backward compatibility with the Cordova plugin.
   */
  landscape?: 'portrait' | 'landscape' | boolean;
  /**
   * How the result should be returned. Defaults to `base64`.
   */
  type?: PdfGeneratorOutputType;
  /**
   * File name used when the PDF is exported to disk (share mode).
   */
  fileName?: string;
}

export interface PdfGeneratorFromUrlOptions extends PdfGeneratorCommonOptions {
  url: string;
}

export interface PdfGeneratorFromDataOptions extends PdfGeneratorCommonOptions {
  /**
   * HTML document to render.
   */
  data: string;
  /**
   * Base URL to use when resolving relative resources inside the HTML string.
   * When omitted, `about:blank` is used.
   */
  baseUrl?: string;
}

export type PdfGeneratorResult =
  | {
      type: 'base64';
      base64: string;
    }
  | {
      type: 'share';
      completed: boolean;
    };

export interface PdfGeneratorPlugin {
  /**
   * Generates a PDF from the provided URL.
   */
  fromURL(options: PdfGeneratorFromUrlOptions): Promise<PdfGeneratorResult>;
  /**
   * Generates a PDF from a raw HTML string.
   */
  fromData(options: PdfGeneratorFromDataOptions): Promise<PdfGeneratorResult>;
}
