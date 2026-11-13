# BA Check-in

Kotlin-Multiplatform-Projekt für Android, iOS und ein Spring-Backend.

## Dokumentation

- [Bedienanleitung](docs/Bedienanleitung.md): Freischaltung und App-Funktionen
  für Studierende, Dozenten und Sekretariat
- [Technische Dokumentation](docs/technische-dokumentation.md): Architektur,
  Sicherheit, Tests und Betrieb
- [App-Backend-API](docs/api.md): Endpunkte für Anmeldung, Studierende,
  Dozenten, Sekretariat, Kalenderimport und NFC
- [NFC](docs/nfc.md): Karten, Check-in und Umgang mit verlorenen Karten
- [Admin-Verwaltung](docs/admin-management.md): serverseitige Verwaltung von
  Konten, Kursen, Terminen, Geräten und NFC-Karten

## Projektstruktur

- [`composeApp`](composeApp/src): Compose-Oberfläche und Android-App
  - `commonMain`: plattformübergreifende UI
  - `androidMain`: Android-spezifische Funktionen, unter anderem NFC
- [`shared`](shared/src): gemeinsame Datenmodelle, HTTP-Anbindung und Fachverträge
- [`backend`](backend): Spring-Backend, PostgreSQL-Schema und REST-API
- [`iosApp`](iosApp/iosApp): iOS-Anwendung und SwiftUI-Einstiegspunkt
- [`docs`](docs): Dokumentation

## Voraussetzungen

- Java 21
- Android SDK und ein gestarteter Android-Emulator (für Android)
- Docker Engine und Docker Compose (für das lokale Backend)
- Xcode (nur für iOS, macOS erforderlich)

Die folgenden Beispiele verwenden Fish unter Linux/macOS. Windows-Befehle stehen
jeweils direkt darunter.

## Schnellstart: Demo im Emulator

Die Demo-App verwendet ein In-Process-Backend. PostgreSQL und das Spring-Backend
sind für diesen Schritt nicht erforderlich.

```fish
cp secrets.json.example secrets.json
./gradlew :composeApp:installFakeDebug
```

Die App kann anschließend im Emulator oder über `adb` gestartet werden:

```fish
adb shell monkey -p de.jscoding.bacheckin.fake 1
```

Unter Windows:

```powershell
Copy-Item secrets.json.example secrets.json
.\gradlew.bat :composeApp:installFakeDebug
adb shell monkey -p de.jscoding.bacheckin.fake 1
```

Für einen reinen Build ohne Installation:

```fish
./gradlew :composeApp:assembleFakeDebug
```

Der Emulator kann den Demo-Ablauf testen. Einen echten NTAG-424-DNA-Scan mit einem NFC-fähigen Android-Gerät testen (nicht Emulator).

## Lokales Backend mit PostgreSQL

Alle Befehle aus dem Repository-Hauptverzeichnis ausführen.

### 1. Datenbank starten

```fish
cp .env.example .env
```

In `.env` mindestens diese Werte setzen:

```dotenv
POSTGRES_DB=ba_check_in
POSTGRES_USER=ba_check_in
POSTGRES_PASSWORD=<lokales-passwort>
POSTGRES_PORT=5432
API_DOCS_USERNAME=<lokaler-benutzer>
API_DOCS_PASSWORD=<lokales-passwort>
```

Dann PostgreSQL starten und den Status prüfen:

```fish
docker compose --env-file .env up -d
docker compose --env-file .env ps
```

Der Container muss den Status `healthy` erreichen.

### 2. Backend starten

Für eine frische Entwicklungsdatenbank zuerst die lokale Bootstrap-Aktivierung
verwenden:

```fish
set -x BOOTSTRAP_SECRETARY_USER_ID "30000000-0000-4000-8000-000000000001"
set -x BOOTSTRAP_ACTIVATION_CODE "2345-ABCD-EFGH-JKMP"

./gradlew :backend:bootRun \
  --args="--spring.profiles.active=dev,local-bootstrap,api-docs"
```

Nach der ersten Aktivierung des Verwaltungsgeräts das Backend beenden und ohne
`local-bootstrap` neu starten:

```fish
./gradlew :backend:bootRun \
  --args="--spring.profiles.active=dev,api-docs"
```

Die Android-App kann nun gegen das lokale Backend gebaut und installiert werden:

```fish
./gradlew :composeApp:installLocalDebug
```

Der Emulator erreicht den Host über `http://10.0.2.2:8080`.

Unter Windows lautet der Backend-Aufruf entsprechend:

```powershell
$env:BOOTSTRAP_SECRETARY_USER_ID = "30000000-0000-4000-8000-000000000001"
$env:BOOTSTRAP_ACTIVATION_CODE = "2345-ABCD-EFGH-JKMP"
.\gradlew.bat :backend:bootRun --args="--spring.profiles.active=dev,local-bootstrap,api-docs"
```

### Lokale Testdaten

Das `dev`-Profil lädt eine separate Flyway-Seed-Migration mit fünf Studenten,
drei Dozenten und drei Verwaltungsbenutzern. Die Einträge besitzen noch keine
Geräte oder Credentials und müssen zuerst über den normalen Aktivierungsablauf
freigeschaltet werden.

## Interne API-Dokumentation

OpenAPI und Swagger UI sind standardmäßig deaktiviert. Sie dürfen nur in einer
privaten Entwicklungsumgebung mit dem Profil `api-docs` aktiviert werden.

Die Zugangsdaten werden aus `API_DOCS_USERNAME` und `API_DOCS_PASSWORD` gelesen.
Mit der oben beschriebenen Fish-Konfiguration startet die Dokumentation zusammen
mit dem Backend:

- Swagger UI: <http://localhost:8080/internal/api-docs>
- OpenAPI: <http://localhost:8080/internal/openapi>

Beide Endpunkte benötigen HTTP-Basic-Authentifizierung.

Außerhalb des `dev`-Profils verweigert das Backend den Start, wenn
`JWT_PRIVATE_KEY_LOCATION`, `JWT_PUBLIC_KEY_LOCATION` oder
`ACTIVATION_CODE_HMAC_SECRET` fehlen. Der private JWT-Schlüssel muss PKCS#8-PEM,
der öffentliche Schlüssel X.509-PEM sein; beide müssen ein RSA-Schlüsselpaar
bilden.

Forwarded-Client-IP-Header werden standardmäßig ignoriert.
`FORWARD_HEADERS_STRATEGY=native` darf nur hinter einem vertrauenswürdigen
Reverse Proxy gesetzt werden, der eingehende Forwarded-Header entfernt und selbst
neu setzt.

## Produktionscontainer

Die produktive Android-App verwendet standardmäßig
`https://bacheckin.brettspielrunde.com`. Die URL kann über
`BACKEND_PROD_BASE_URL` oder die Gradle-Property `backendProdBaseUrl` überschrieben
werden. Vor dem Schreiben von NFC-Tags muss die endgültige Domain feststehen.

Backend-JAR und Container-Image aus dem Repository-Hauptverzeichnis bauen:

```fish
./gradlew :backend:bootJar
docker build -f backend/Dockerfile -t ghcr.io/gruppe5-ba/app-backend:latest .
```

Auf `main` veröffentlicht der GitHub-Workflow das Backend-Image automatisch in
der GitHub Container Registry. Der Workflow baut derzeit nur das Backend; den
Android-Build daher zusätzlich lokal oder in einer eigenen CI-Prüfung ausführen.

Für einen Server:

```fish
cp .env.production.example .env.production
# Platzhalter ersetzen und JWT-/NFC-Schlüsseldateien bereitstellen
docker compose --env-file .env.production -f compose.production.yml up -d
```

PostgreSQL besitzt keinen öffentlichen Host-Port. Das Backend wird ebenfalls
nicht am Host veröffentlicht, sondern ist ausschließlich im externen Docker-Netz
`npm_proxy` als `bacheckin-backend:8080` für Nginx Proxy Manager erreichbar.

## iOS bauen und starten

Das Verzeichnis [`iosApp`](iosApp) in Xcode öffnen und das gewünschte Scheme auf
einem Simulator oder Gerät starten.
