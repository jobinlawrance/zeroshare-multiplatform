import Foundation

class FileDownloader: NSObject, URLSessionDownloadDelegate {
    private var fileName: String
    private var downloadTask: URLSessionDownloadTask?
    private var onComplete: (() -> Void)?
    private var backgroundSession: URLSession!
    
    init(fileName: String) {
        self.fileName = fileName
        super.init()
        
        let config = URLSessionConfiguration.background(withIdentifier: "com.yourapp.download.\(UUID().uuidString)")
        backgroundSession = URLSession(configuration: config, delegate: self, delegateQueue: nil)
    }
    
    func download(from url: URL, onComplete: @escaping () -> Void) {
        self.onComplete = onComplete
        downloadTask = backgroundSession.downloadTask(with: url)
        downloadTask?.resume()
    }
    
    private func getZeroShareDirectory() -> URL? {
        guard let documentDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first else {
            return nil
        }
        
        let zeroShareDirectory = documentDirectory.appendingPathComponent("ZeroShare", isDirectory: true)
        
        // Create ZeroShare directory if it doesn't exist
        if !FileManager.default.fileExists(atPath: zeroShareDirectory.path) {
            do {
                try FileManager.default.createDirectory(at: zeroShareDirectory, withIntermediateDirectories: true)
            } catch {
                print("Failed to create ZeroShare directory: \(error)")
                return nil
            }
        }
        
        return zeroShareDirectory
    }
    
    // URLSessionDownloadDelegate methods
    func urlSession(_ session: URLSession, downloadTask: URLSessionDownloadTask, didFinishDownloadingTo location: URL) {
        guard let zeroShareDirectory = getZeroShareDirectory() else {
            print("Could not access or create ZeroShare directory")
            return
        }
        
        let destinationURL = zeroShareDirectory.appendingPathComponent(fileName)
        
        do {
            // Remove existing file if it exists
            if FileManager.default.fileExists(atPath: destinationURL.path) {
                try FileManager.default.removeItem(at: destinationURL)
            }
            
            // Move downloaded file to destination
            try FileManager.default.moveItem(at: location, to: destinationURL)
            print("File downloaded successfully to: \(destinationURL.path)")
            
            DispatchQueue.main.async {
                self.onComplete?()
            }
        } catch {
            print("File download error: \(error)")
        }
    }
    
    func urlSession(_ session: URLSession, downloadTask: URLSessionDownloadTask, didWriteData bytesWritten: Int64, totalBytesWritten: Int64, totalBytesExpectedToWrite: Int64) {
        if totalBytesExpectedToWrite > 0 {  // Add check to prevent division by zero
            let progress = Double(totalBytesWritten) / Double(totalBytesExpectedToWrite) * 100
            print("Download progress: \(Int(progress))%")
        }
    }
    
    func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
        if let error = error {
            print("Download failed with error: \(error)")
        }
    }
}
