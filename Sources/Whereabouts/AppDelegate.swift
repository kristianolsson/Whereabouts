import AppKit

final class AppDelegate: NSObject, NSApplicationDelegate {

    private var statusItem: NSStatusItem!
    private let geoLocator = GeoLocator()
    private let networkMonitor = NetworkMonitor()
    private let defaults = UserDefaults.standard

    private let lastCountryKey = "lastCountryCode"
    private let lastDetailKey = "lastDetail"
    private let lastUpdatedKey = "lastUpdated"

    private var detailItem: NSMenuItem!
    private var updatedItem: NSMenuItem!

    // Backstop poll in case a path-change event is ever missed.
    private let backstopInterval: TimeInterval = 30 * 60
    private var backstopTimer: Timer?

    func applicationDidFinishLaunching(_ notification: Notification) {
        NSApp.setActivationPolicy(.accessory)

        statusItem = NSStatusBar.system.statusItem(withLength: NSStatusItem.squareLength)
        buildMenu()

        showCachedStateIfAvailable()
        refresh()

        networkMonitor.onPathChanged = { [weak self] in
            self?.refresh()
        }
        networkMonitor.start()

        backstopTimer = Timer.scheduledTimer(withTimeInterval: backstopInterval, repeats: true) { [weak self] _ in
            self?.refresh()
        }
    }

    func applicationWillTerminate(_ notification: Notification) {
        networkMonitor.stop()
        backstopTimer?.invalidate()
    }

    private func buildMenu() {
        let menu = NSMenu()

        detailItem = NSMenuItem(title: "Looking up location…", action: nil, keyEquivalent: "")
        detailItem.isEnabled = false
        menu.addItem(detailItem)

        updatedItem = NSMenuItem(title: "", action: nil, keyEquivalent: "")
        updatedItem.isEnabled = false
        menu.addItem(updatedItem)

        let refreshItem = NSMenuItem(title: "Refresh Now", action: #selector(refreshTapped), keyEquivalent: "r")
        refreshItem.target = self
        menu.addItem(NSMenuItem.separator())
        menu.addItem(refreshItem)
        menu.addItem(NSMenuItem.separator())
        menu.addItem(NSMenuItem(title: "Quit Whereabouts", action: #selector(NSApplication.terminate(_:)), keyEquivalent: "q"))

        statusItem.menu = menu
    }

    private func showCachedStateIfAvailable() {
        guard let code = defaults.string(forKey: lastCountryKey) else {
            statusItem.button?.title = "🏳️"
            return
        }
        statusItem.button?.title = FlagEmoji.flag(forCountryCode: code)
        detailItem.title = defaults.string(forKey: lastDetailKey) ?? code
        if let updated = defaults.object(forKey: lastUpdatedKey) as? Date {
            updatedItem.title = "Updated \(formatted(updated))"
        }
    }

    @objc private func refreshTapped() {
        refresh()
    }

    private func refresh() {
        Task {
            do {
                let result = try await geoLocator.lookup()
                await MainActor.run {
                    self.apply(result)
                }
            } catch {
                await MainActor.run {
                    self.detailItem.title = "Lookup failed — showing last known"
                }
            }
        }
    }

    private func apply(_ result: GeoResult) {
        let flag = FlagEmoji.flag(forCountryCode: result.countryCode)
        statusItem.button?.title = flag

        let location = [result.city, result.countryName ?? result.countryCode]
            .compactMap { $0 }
            .joined(separator: ", ")
        let detail = result.ip.map { "\(location) (\($0))" } ?? location
        detailItem.title = detail

        let now = Date()
        updatedItem.title = "Updated \(formatted(now))"

        defaults.set(result.countryCode, forKey: lastCountryKey)
        defaults.set(detail, forKey: lastDetailKey)
        defaults.set(now, forKey: lastUpdatedKey)
    }

    private func formatted(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateStyle = .none
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }
}
