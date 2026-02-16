import Capacitor
import Foundation
import WebKit

@objc(PdfGeneratorPlugin)
public class PdfGeneratorPlugin: CAPPlugin, CAPBridgedPlugin {
    private let pluginVersion: String = "8.0.14"
    public let identifier = "PdfGeneratorPlugin"
    public let jsName = "PdfGenerator"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "fromURL", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "fromData", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getPluginVersion", returnType: CAPPluginReturnPromise)
    ]

    private var tasks: [PdfGenerationTask] = []

    @objc func fromURL(_ call: CAPPluginCall) {
        guard let urlString = call.getString("url"), let url = URL(string: urlString) else {
            call.reject("A valid 'url' is required.")
            return
        }

        let options = PdfGeneratorOptions(from: call)
        let task = PdfGenerationTask(plugin: self, call: call, source: .url(url), options: options)
        enqueue(task)
    }

    @objc func fromData(_ call: CAPPluginCall) {
        guard let htmlData = call.getString("data") else {
            call.reject("The 'data' option is required.")
            return
        }

        let options = PdfGeneratorOptions(from: call)
        let task = PdfGenerationTask(plugin: self, call: call, source: .html(htmlData, options.baseUrl), options: options)
        enqueue(task)
    }

    private func enqueue(_ task: PdfGenerationTask) {
        tasks.append(task)
        task.start()
    }

    fileprivate func taskDidComplete(_ task: PdfGenerationTask) {
        tasks.removeAll { $0 === task }
    }

    fileprivate func handle(pdfData: Data, for task: PdfGenerationTask) {
        switch task.options.outputType {
        case .base64:
            resolveBase64(data: pdfData, for: task)
        case .share:
            share(pdfData: pdfData, for: task)
        }
    }

    private func resolveBase64(data: Data, for task: PdfGenerationTask) {
        let base64 = data.base64EncodedString()
        DispatchQueue.main.async {
            task.call.resolve([
                "type": "base64",
                "base64": base64
            ])
            task.finish()
        }
    }

    private func share(pdfData: Data, for task: PdfGenerationTask) {
        let temporaryURL = FileManager.default.temporaryDirectory.appendingPathComponent(task.options.fileName)

        do {
            try pdfData.write(to: temporaryURL, options: .atomic)
        } catch {
            task.call.reject("Failed to write PDF data: \(error.localizedDescription)")
            task.finish()
            return
        }

        DispatchQueue.main.async { [weak self] in
            guard let self = self, let presenter = self.bridge?.viewController else {
                task.call.reject("Unable to present share sheet.")
                try? FileManager.default.removeItem(at: temporaryURL)
                task.finish()
                return
            }

            let activity = UIActivityViewController(activityItems: [temporaryURL], applicationActivities: nil)
            activity.completionWithItemsHandler = { _, completed, _, error in
                if let error = error {
                    task.call.reject("Share failed: \(error.localizedDescription)")
                } else {
                    task.call.resolve([
                        "type": "share",
                        "completed": completed
                    ])
                }
                try? FileManager.default.removeItem(at: temporaryURL)
                task.finish()
            }

            if let popover = activity.popoverPresentationController, let view = presenter.view {
                popover.sourceView = view
                popover.sourceRect = CGRect(x: view.bounds.midX, y: view.bounds.midY, width: 0, height: 0)
                popover.permittedArrowDirections = []
            }

            presenter.present(activity, animated: true)
        }
    }

    @objc func getPluginVersion(_ call: CAPPluginCall) {
        call.resolve(["version": self.pluginVersion])
    }
}

private final class PdfGenerationTask: NSObject, WKNavigationDelegate {
    enum Source {
        case url(URL)
        case html(String, URL?)
    }

    let call: CAPPluginCall
    let options: PdfGeneratorOptions

    private weak var plugin: PdfGeneratorPlugin?
    private let source: Source
    private var webView: WKWebView?
    private var didFinish = false

    init(plugin: PdfGeneratorPlugin, call: CAPPluginCall, source: Source, options: PdfGeneratorOptions) {
        self.plugin = plugin
        self.call = call
        self.source = source
        self.options = options
        super.init()
    }

    func start() {
        DispatchQueue.main.async {
            // Create WKWebView on main thread
            let configuration = WKWebViewConfiguration()
            configuration.preferences.javaScriptEnabled = true
            let webView = WKWebView(frame: .zero, configuration: configuration)
            webView.navigationDelegate = self
            self.webView = webView
            
            switch self.source {
            case let .url(url):
                let request = URLRequest(url: url)
                webView.load(request)
            case let .html(html, baseUrl):
                webView.loadHTMLString(html, baseURL: baseUrl)
            }
        }
    }

    func finish() {
        guard !didFinish else { return }
        didFinish = true
        cleanup()
    }

    private func cleanup() {
        DispatchQueue.main.async {
            self.webView?.stopLoading()
            self.webView?.navigationDelegate = nil
            self.webView?.removeFromSuperview()
        }
        plugin?.taskDidComplete(self)
    }

    private func fail(with message: String) {
        guard !didFinish else { return }
        didFinish = true
        DispatchQueue.main.async {
            self.call.reject(message)
        }
        cleanup()
    }

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        generatePdf()
    }

    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        fail(with: "Failed to load content: \(error.localizedDescription)")
    }

    func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
        fail(with: "Failed to load content: \(error.localizedDescription)")
    }

    private func generatePdf() {
        guard let webView = self.webView else {
            fail(with: "WebView not initialized")
            return
        }
        
        let configuration = WKPDFConfiguration()
        configuration.rect = CGRect(origin: .zero, size: options.pageSize)

        webView.createPDF(configuration: configuration) { [weak self] result in
            guard let self else { return }
            switch result {
            case let .success(data):
                self.plugin?.handle(pdfData: data, for: self)
            case let .failure(error):
                self.fail(with: "Failed to generate PDF: \(error.localizedDescription)")
            }
        }
    }
}

private struct PdfGeneratorOptions {
    enum OutputType {
        case base64
        case share

        init(string: String?) {
            switch string?.lowercased() {
            case "share":
                self = .share
            default:
                self = .base64
            }
        }
    }

    enum DocumentSize {
        case a3
        case a4

        init(string: String?) {
            switch string?.uppercased() {
            case "A3":
                self = .a3
            default:
                self = .a4
            }
        }

        private var portraitSize: CGSize {
            let pointsPerMillimetre = 72.0 / 25.4
            switch self {
            case .a3:
                return CGSize(width: 297.0 * pointsPerMillimetre, height: 420.0 * pointsPerMillimetre)
            case .a4:
                return CGSize(width: 210.0 * pointsPerMillimetre, height: 297.0 * pointsPerMillimetre)
            }
        }

        func size(isLandscape: Bool) -> CGSize {
            let size = portraitSize
            return isLandscape ? CGSize(width: size.height, height: size.width) : size
        }
    }

    let documentSize: DocumentSize
    let isLandscape: Bool
    let outputType: OutputType
    let fileName: String
    let baseUrl: URL?

    var pageSize: CGSize {
        documentSize.size(isLandscape: isLandscape)
    }

    init(from call: CAPPluginCall) {
        let orientationValue = PdfGeneratorOptions.orientationString(from: call)
        self.documentSize = DocumentSize(string: call.getString("documentSize"))
        self.isLandscape = orientationValue == "landscape"
        self.outputType = OutputType(string: call.getString("type"))
        self.fileName = PdfGeneratorOptions.sanitizedFileName(from: call.getString("fileName"))
        self.baseUrl = PdfGeneratorOptions.baseUrl(from: call.getString("baseUrl"))
    }

    private static func orientationString(from call: CAPPluginCall) -> String {
        if let landscapeFlag = call.options["landscape"] as? Bool {
            return landscapeFlag ? "landscape" : "portrait"
        }

        let orientation = call.getString("orientation") ?? call.getString("landscape") ?? "portrait"
        return orientation.lowercased()
    }

    private static func sanitizedFileName(from raw: String?) -> String {
        let trimmed = raw?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        var name = trimmed.isEmpty ? "default.pdf" : trimmed
        name = name.replacingOccurrences(of: "[/\\\\:]", with: "_", options: .regularExpression)
        if !name.lowercased().hasSuffix(".pdf") {
            name += ".pdf"
        }
        return name
    }

    private static func baseUrl(from raw: String?) -> URL? {
        guard let raw = raw?.trimmingCharacters(in: .whitespacesAndNewlines), !raw.isEmpty else {
            return nil
        }
        if raw == "BUNDLE" {
            return Bundle.main.bundleURL
        }
        return URL(string: raw)
    }
}
