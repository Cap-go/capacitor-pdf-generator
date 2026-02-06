package app.capgo.pdfgenerator;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.print.CapgoPdfPrintUtils;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@CapacitorPlugin(name = "PdfGenerator")
public class PdfGeneratorPlugin extends Plugin {

    private final String pluginVersion = "8.0.12";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<PdfGenerationTask> tasks = new ArrayList<>();

    @PluginMethod
    public void fromURL(PluginCall call) {
        String url = call.getString("url");
        if (url == null || url.trim().isEmpty()) {
            call.reject("A valid 'url' is required.");
            return;
        }

        PdfGeneratorOptions options = PdfGeneratorOptions.from(call);
        PdfSource source = new UrlSource(url);
        enqueueTask(new PdfGenerationTask(this, call, source, options));
    }

    @PluginMethod
    public void fromData(PluginCall call) {
        String data = call.getString("data");
        if (data == null || data.trim().isEmpty()) {
            call.reject("The 'data' option is required.");
            return;
        }

        PdfGeneratorOptions options = PdfGeneratorOptions.from(call);
        PdfSource source = new HtmlSource(data, options.baseUrl);
        enqueueTask(new PdfGenerationTask(this, call, source, options));
    }

    private void enqueueTask(PdfGenerationTask task) {
        synchronized (tasks) {
            tasks.add(task);
        }
        task.start();
    }

    void removeTask(PdfGenerationTask task) {
        synchronized (tasks) {
            tasks.remove(task);
        }
    }

    void generatePdf(PdfGenerationTask task, WebView webView) {
        PrintAttributes attributes = createPrintAttributes(task.options);
        String jobName = task.options.printJobName();
        PrintDocumentAdapter adapter = webView.createPrintDocumentAdapter(jobName);

        if (task.options.outputType == PdfGeneratorOptions.OutputType.BASE64) {
            CapgoPdfPrintUtils.createBase64(
                getContext(),
                adapter,
                attributes,
                new CapgoPdfPrintUtils.Base64Callback() {
                    @Override
                    public void onSuccess(@NonNull String base64) {
                        mainHandler.post(() -> {
                            JSObject result = new JSObject();
                            result.put("type", "base64");
                            result.put("base64", base64);
                            task.call.resolve(result);
                            task.finish();
                        });
                    }

                    @Override
                    public void onError(@NonNull String message) {
                        mainHandler.post(() -> {
                            task.call.reject(message);
                            task.finish();
                        });
                    }
                }
            );
        } else {
            File output = new File(getContext().getCacheDir(), task.options.fileName);
            CapgoPdfPrintUtils.writeToFile(
                getContext(),
                adapter,
                attributes,
                output,
                new CapgoPdfPrintUtils.FileCallback() {
                    @Override
                    public void onSuccess(@NonNull File file) {
                        mainHandler.post(() -> sharePdf(task, file));
                    }

                    @Override
                    public void onError(@NonNull String message) {
                        mainHandler.post(() -> {
                            task.call.reject(message);
                            task.finish();
                        });
                    }
                }
            );
        }
    }

    private void sharePdf(PdfGenerationTask task, File file) {
        Activity activity = getActivity();
        if (activity == null) {
            task.call.reject("Unable to open share dialog: no active activity.");
            task.finish();
            return;
        }

        Uri uri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".capgo.pdfgenerator.fileprovider", file);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        file.deleteOnExit();

        try {
            Intent chooser = Intent.createChooser(shareIntent, task.options.fileName);
            activity.startActivity(chooser);
            JSObject result = new JSObject();
            result.put("type", "share");
            result.put("completed", true);
            task.call.resolve(result);
        } catch (ActivityNotFoundException ex) {
            task.call.reject("No compatible application found to share the PDF.");
        } finally {
            task.finish();
        }
    }

    private PrintAttributes createPrintAttributes(PdfGeneratorOptions options) {
        PrintAttributes.Builder builder = new PrintAttributes.Builder();
        PrintAttributes.MediaSize mediaSize = options.mediaSize();
        builder.setMediaSize(mediaSize);
        builder.setResolution(new PrintAttributes.Resolution("pdf", "pdf", 600, 600));
        builder.setMinMargins(PrintAttributes.Margins.NO_MARGINS);
        return builder.build();
    }

    @PluginMethod
    public void getPluginVersion(final PluginCall call) {
        try {
            final JSObject ret = new JSObject();
            ret.put("version", this.pluginVersion);
            call.resolve(ret);
        } catch (final Exception e) {
            call.reject("Could not get plugin version", e);
        }
    }
}

interface PdfSource {
    void load(WebView webView);
}

final class UrlSource implements PdfSource {

    private final String url;

    UrlSource(String url) {
        this.url = url;
    }

    @Override
    public void load(WebView webView) {
        webView.loadUrl(url);
    }
}

final class HtmlSource implements PdfSource {

    private final String html;
    private final String baseUrl;

    HtmlSource(String html, String baseUrl) {
        this.html = html;
        this.baseUrl = baseUrl;
    }

    @Override
    public void load(WebView webView) {
        webView.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null);
    }
}

final class PdfGenerationTask extends WebViewClient {

    final PluginCall call;
    final PdfGeneratorOptions options;

    private final PdfGeneratorPlugin plugin;
    private WebView webView;
    private boolean finished;

    PdfGenerationTask(PdfGeneratorPlugin plugin, PluginCall call, PdfSource source, PdfGeneratorOptions options) {
        this.plugin = plugin;
        this.call = call;
        this.options = options;
        this.source = source;
    }

    private final PdfSource source;

    void start() {
        Activity activity = plugin.getActivity();
        if (activity == null) {
            call.reject("No activity available.");
            return;
        }

        activity.runOnUiThread(() -> {
            webView = new WebView(plugin.getContext());
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDatabaseEnabled(true);
            webView.setWebViewClient(this);
            source.load(webView);
        });
    }

    void finish() {
        if (finished) {
            return;
        }
        finished = true;
        Activity activity = plugin.getActivity();
        if (activity != null) {
            activity.runOnUiThread(() -> {
                if (webView != null) {
                    webView.stopLoading();
                    webView.setWebViewClient(null);
                    webView.destroy();
                    webView = null;
                }
                plugin.removeTask(this);
            });
        } else {
            plugin.removeTask(this);
        }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        plugin.generatePdf(this, view);
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        call.reject("Failed to load content: " + error.getDescription());
        finish();
    }
}

final class PdfGeneratorOptions {

    enum OutputType {
        BASE64,
        SHARE;

        static OutputType from(String value) {
            return "share".equalsIgnoreCase(value) ? SHARE : BASE64;
        }
    }

    final OutputType outputType;
    final String documentSize;
    final boolean landscape;
    final String fileName;
    final String baseUrl;

    private PdfGeneratorOptions(OutputType outputType, String documentSize, boolean landscape, String fileName, String baseUrl) {
        this.outputType = outputType;
        this.documentSize = documentSize;
        this.landscape = landscape;
        this.fileName = ensurePdfExtension(fileName);
        this.baseUrl = baseUrl;
    }

    static PdfGeneratorOptions from(PluginCall call) {
        String docSize = call.getString("documentSize", "A4");
        boolean landscape = resolveLandscape(call);
        String type = call.getString("type", "base64");
        String fileName = call.getString("fileName", "default.pdf");
        String baseUrl = normalizeBaseUrl(call.getString("baseUrl"));
        return new PdfGeneratorOptions(OutputType.from(type), docSize, landscape, fileName, baseUrl);
    }

    private static boolean resolveLandscape(PluginCall call) {
        JSObject data = call.getData();
        if (data != null && data.has("landscape")) {
            Object landscapeOption = data.opt("landscape");
            if (landscapeOption instanceof Boolean) {
                return (Boolean) landscapeOption;
            }
            if (landscapeOption instanceof String) {
                return ((String) landscapeOption).equalsIgnoreCase("landscape");
            }
        }

        String orientation = call.getString("orientation");
        if (orientation != null) {
            return orientation.equalsIgnoreCase("landscape");
        }

        String landscapeString = call.getString("landscape", "portrait");
        return landscapeString.equalsIgnoreCase("landscape");
    }

    private static String ensurePdfExtension(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            trimmed = "default.pdf";
        }
        String sanitized = trimmed.replaceAll("[\\\\/:]", "_");
        if (!sanitized.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            sanitized = sanitized + ".pdf";
        }
        return sanitized;
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null) {
            return null;
        }
        String trimmed = baseUrl.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if ("BUNDLE".equalsIgnoreCase(trimmed)) {
            return "file:///android_asset/";
        }
        return trimmed;
    }

    PrintAttributes.MediaSize mediaSize() {
        PrintAttributes.MediaSize mediaSize = PrintAttributes.MediaSize.ISO_A4;
        if ("A3".equalsIgnoreCase(documentSize)) {
            mediaSize = PrintAttributes.MediaSize.ISO_A3;
        }
        return landscape ? mediaSize.asLandscape() : mediaSize.asPortrait();
    }

    String printJobName() {
        String name = fileName;
        int dotIndex = name.lastIndexOf('.');
        return dotIndex > 0 ? name.substring(0, dotIndex) : name;
    }
}
