package android.print;

import android.content.Context;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.util.Base64;
import androidx.annotation.NonNull;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

/**
 * Internal helpers to generate PDF output from a PrintDocumentAdapter without relying on
 * platform print dialogs. Located inside the {@code android.print} package so we can access
 * the package-private constructors of the framework callbacks.
 */
public final class CapgoPdfPrintUtils {

    private CapgoPdfPrintUtils() {}

    public interface Base64Callback {
        void onSuccess(@NonNull String base64);
        void onError(@NonNull String message);
    }

    public interface FileCallback {
        void onSuccess(@NonNull File file);
        void onError(@NonNull String message);
    }

    public static void createBase64(Context context, PrintDocumentAdapter adapter, PrintAttributes attributes, Base64Callback callback) {
        Writer writer = new Writer(adapter, attributes);
        writer.writeToTempFile(
            context,
            new Writer.FileResultCallback() {
                @Override
                public void onSuccess(@NonNull File file, @NonNull ParcelFileDescriptor descriptor) {
                    new Thread(() -> {
                        try {
                            String base64 = PdfIoUtils.readBase64(file);
                            callback.onSuccess(base64);
                        } catch (Exception ex) {
                            callback.onError("Failed to convert PDF to base64.");
                        } finally {
                            PdfIoUtils.closeQuietly(descriptor);
                            // best-effort cleanup
                            //noinspection ResultOfMethodCallIgnored
                            file.delete();
                        }
                    })
                        .start();
                }

                @Override
                public void onError(@NonNull String message) {
                    callback.onError(message);
                }
            }
        );
    }

    public static void writeToFile(
        Context context,
        PrintDocumentAdapter adapter,
        PrintAttributes attributes,
        File target,
        FileCallback callback
    ) {
        Writer writer = new Writer(adapter, attributes);
        writer.writeToFile(
            context,
            target,
            new Writer.FileResultCallback() {
                @Override
                public void onSuccess(@NonNull File file, @NonNull ParcelFileDescriptor descriptor) {
                    PdfIoUtils.closeQuietly(descriptor);
                    callback.onSuccess(file);
                }

                @Override
                public void onError(@NonNull String message) {
                    callback.onError(message);
                }
            }
        );
    }

    private static final class Writer {

        interface FileResultCallback {
            void onSuccess(@NonNull File file, @NonNull ParcelFileDescriptor descriptor);
            void onError(@NonNull String message);
        }

        private static final PageRange[] ALL_PAGES = new PageRange[] { PageRange.ALL_PAGES };

        private final PrintDocumentAdapter adapter;
        private final PrintAttributes attributes;

        Writer(PrintDocumentAdapter adapter, PrintAttributes attributes) {
            this.adapter = adapter;
            this.attributes = attributes;
        }

        void writeToTempFile(Context context, FileResultCallback callback) {
            try {
                File tempFile = File.createTempFile("capgo_pdf_generator", ".pdf", context.getCacheDir());
                writeToDescriptor(tempFile, callback);
            } catch (Exception ex) {
                callback.onError("Failed to create temporary file for PDF.");
            }
        }

        void writeToFile(Context context, File file, FileResultCallback callback) {
            try {
                if (file.exists() && !file.delete()) {
                    callback.onError("Failed to override existing PDF file.");
                    return;
                }
                File parent = file.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    callback.onError("Failed to prepare output directory for PDF.");
                    return;
                }
                writeToDescriptor(file, callback);
            } catch (Exception ex) {
                callback.onError("Failed to prepare PDF output file.");
            }
        }

        private void writeToDescriptor(File file, FileResultCallback callback) {
            try {
                ParcelFileDescriptor descriptor = ParcelFileDescriptor.open(
                    file,
                    ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_TRUNCATE
                );
                CancellationSignal cancellationSignal = new CancellationSignal();
                adapter.onLayout(
                    null,
                    attributes,
                    cancellationSignal,
                    new PrintDocumentAdapter.LayoutResultCallback() {
                        @Override
                        public void onLayoutFailed(CharSequence error) {
                            callback.onError("PDF layout failed: " + (error == null ? "" : error));
                        }

                        @Override
                        public void onLayoutFinished(PrintDocumentInfo info, boolean changed) {
                            adapter.onWrite(
                                ALL_PAGES,
                                descriptor,
                                cancellationSignal,
                                new PrintDocumentAdapter.WriteResultCallback() {
                                    @Override
                                    public void onWriteFinished(PageRange[] pages) {
                                        callback.onSuccess(file, descriptor);
                                    }

                                    @Override
                                    public void onWriteFailed(CharSequence error) {
                                        callback.onError("PDF write failed: " + (error == null ? "" : error));
                                    }
                                }
                            );
                        }
                    },
                    null
                );
            } catch (Exception ex) {
                callback.onError("Failed to create PDF output.");
            }
        }
    }

    private static final class PdfIoUtils {

        private PdfIoUtils() {}

        static String readBase64(File file) throws Exception {
            FileInputStream inputStream = new FileInputStream(file);
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);
            } finally {
                inputStream.close();
            }
        }

        static void closeQuietly(ParcelFileDescriptor descriptor) {
            if (descriptor == null) {
                return;
            }
            try {
                descriptor.close();
            } catch (Exception ignored) {}
        }
    }
}
