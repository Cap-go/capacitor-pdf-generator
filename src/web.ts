import { WebPlugin } from '@capacitor/core';

import type {
  PdfGeneratorFromDataOptions,
  PdfGeneratorFromUrlOptions,
  PdfGeneratorPlugin,
  PdfGeneratorResult,
} from './definitions';

export class PdfGeneratorWeb extends WebPlugin implements PdfGeneratorPlugin {
  async fromURL(_options: PdfGeneratorFromUrlOptions): Promise<PdfGeneratorResult> {
    throw this.unimplemented('fromURL is not available in the web implementation.');
  }

  async fromData(_options: PdfGeneratorFromDataOptions): Promise<PdfGeneratorResult> {
    throw this.unimplemented('fromData is not available in the web implementation.');
  }
}
