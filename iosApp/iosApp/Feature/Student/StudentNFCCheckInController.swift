import CoreNFC
import Foundation
import Shared

final class StudentNFCCheckInController: NSObject, ObservableObject {
    @Published private(set) var isScanning = false

    var isNFCAvailable: Bool {
        NFCTagReaderSession.readingAvailable
    }

    private var session: NFCTagReaderSession?
    private var onPayload: ((String, String, String) -> Void)?
    private var onError: ((String) -> Void)?

    func start(
        onPayload: @escaping (String, String, String) -> Void,
        onError: @escaping (String) -> Void
    ) {
        guard session == nil else { return }
        guard isNFCAvailable else {
            onError("NFC ist auf diesem Gerät nicht verfügbar.")
            return
        }

        self.onPayload = onPayload
        self.onError = onError

        guard let session = NFCTagReaderSession(
            pollingOption: .iso14443,
            delegate: self,
            queue: nil
        ) else {
            onError("Die NFC-Lesesitzung konnte nicht gestartet werden.")
            return
        }
        session.alertMessage = "Halte die Check-in-Karte an die Oberseite deines iPhones."
        self.session = session
        isScanning = true
        session.begin()
    }

    func stop() {
        session?.invalidate()
        session = nil
        isScanning = false
        onPayload = nil
        onError = nil
    }
}

extension StudentNFCCheckInController: NFCTagReaderSessionDelegate {
    func tagReaderSessionDidBecomeActive(_ session: NFCTagReaderSession) {}

    func tagReaderSession(_ session: NFCTagReaderSession, didDetect tags: [NFCTag]) {
        guard tags.count == 1, let tag = tags.first else {
            session.alertMessage = "Bitte halte nur eine Karte an das iPhone."
            session.restartPolling()
            return
        }

        session.connect(to: tag) { [weak self] error in
            guard let self else { return }
            if let error {
                session.invalidate(errorMessage: error.localizedDescription)
                return
            }

            guard let ndefTag = self.ndefTag(from: tag) else {
                session.invalidate(errorMessage: "Dieser NFC-Tag-Typ wird nicht unterstützt.")
                return
            }
            ndefTag.readNDEF { message, error in
                if let error {
                    session.invalidate(errorMessage: error.localizedDescription)
                    return
                }
                guard let message else {
                    session.invalidate(errorMessage: "Die NFC-Karte enthält keine NDEF-Daten.")
                    return
                }
                self.handle(message: message, session: session)
            }
        }
    }

    func tagReaderSession(_ session: NFCTagReaderSession, didInvalidateWithError error: Error) {
        DispatchQueue.main.async { [weak self] in
            guard let self, self.session === session else { return }
            self.session = nil
            self.isScanning = false

            let readerError = error as? NFCReaderError
            if readerError?.code != .readerSessionInvalidationErrorUserCanceled {
                self.onError?(error.localizedDescription)
            }
        }
    }

    private func ndefTag(from tag: NFCTag) -> (any NFCNDEFTag)? {
        switch tag {
        case .iso7816(let tag): tag
        case .miFare(let tag): tag
        case .iso15693(let tag): tag
        case .feliCa(let tag): tag
        @unknown default:
            nil
        }
    }

    private func handle(message: NFCNDEFMessage, session: NFCTagReaderSession) {
        guard let url = message.records
            .compactMap({ $0.wellKnownTypeURIPayload() })
            .first else {
            session.invalidate(errorMessage: "Die NFC-Karte enthält keinen gültigen Check-in-Link.")
            return
        }

        do {
            let payload = try NfcSdmUrlParser.shared.parse(url: url.absoluteString)
            session.alertMessage = "Karte gelesen. Check-in wird geprüft …"
            session.invalidate()
            DispatchQueue.main.async { [weak self] in
                guard let self else { return }
                self.onPayload?(
                    payload.cardPublicId,
                    payload.encryptedPiccDataHex,
                    payload.sdmMacHex
                )
            }
        } catch {
            session.invalidate(errorMessage: error.localizedDescription)
        }
    }
}
