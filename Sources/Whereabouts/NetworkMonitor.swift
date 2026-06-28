import Foundation
import Network

final class NetworkMonitor {

    private let monitor = NWPathMonitor()
    private let queue = DispatchQueue(label: "com.kristian.whereabouts.netmonitor")
    private var debounceTask: Task<Void, Never>?

    var onPathChanged: (() -> Void)?

    func start() {
        monitor.pathUpdateHandler = { [weak self] _ in
            self?.scheduleDebouncedCallback()
        }
        monitor.start(queue: queue)
    }

    func stop() {
        monitor.cancel()
    }

    private func scheduleDebouncedCallback() {
        debounceTask?.cancel()
        debounceTask = Task {
            try? await Task.sleep(nanoseconds: 1_500_000_000)
            guard !Task.isCancelled else { return }
            await MainActor.run {
                self.onPathChanged?()
            }
        }
    }
}
