# Admin-Befehle

## Start auf Server

```bash
cd /srv/bacheckin
export DOCKER_CONFIG=/srv/bacheckin/docker-config
```

## Backend prüfen und aktualisieren

Status und Logs:

```bash
docker compose --env-file .env.production -f compose.production.yml ps
docker compose --env-file .env.production -f compose.production.yml logs --tail=100 backend
```

Aktualisierung:

```bash
docker compose --env-file .env.production -f compose.production.yml pull backend
docker compose --env-file .env.production -f compose.production.yml up -d --no-deps --force-recreate backend
```
---

## Sekretariat

**Schema:**
```bash
ba-admin secretary list
ba-admin secretary create <vorname> <nachname> <email>
ba-admin secretary update <email> <neuer-vorname> <neuer-nachname> <neue-email>
ba-admin secretary code <email>
ba-admin secretary disable <email>
ba-admin secretary enable <email>
```

**Beispiele:**
```bash
ba-admin secretary list
ba-admin secretary create 'Sekretariat' 'MA2' sekretariat.ma2@bacheckin.internal
ba-admin secretary update sekretariat.ma2@bacheckin.internal 'Sekretariat' 'MA 2' sekretariat.ma2@bacheckin.internal
ba-admin secretary code sekretariat.ma1@bacheckin.internal
ba-admin secretary disable sekretariat.ma2@bacheckin.internal
ba-admin secretary enable sekretariat.ma2@bacheckin.internal
```

`create` gibt einen Aktivierungscode aus. `code` erstellt einen neuen Code und
sperrt das bisherige Gerät. Ein Code gilt 30 Minuten.

---

## Dozenten

**Schema:**
```bash
ba-admin lecturer list
ba-admin lecturer create <vorname> <nachname> <email>
ba-admin lecturer update <email> <neuer-vorname> <neuer-nachname> <neue-email>
ba-admin lecturer code <dozent-email> <sekretariat-email>
ba-admin lecturer disable <dozent-email> <sekretariat-email>
ba-admin lecturer enable <email>
```

**Beispiele:**
```bash
ba-admin lecturer list
ba-admin lecturer create Alex Weber alex.weber@bacheckin.internal
ba-admin lecturer update alex.weber@bacheckin.internal Alex Weber alex.weber@bacheckin.internal
ba-admin lecturer code alex.weber@bacheckin.internal sekretariat.ma1@bacheckin.internal
ba-admin lecturer disable alex.weber@bacheckin.internal sekretariat.ma1@bacheckin.internal
ba-admin lecturer enable alex.weber@bacheckin.internal
```

`create` legt das Konto an. Mit `code` bekommt der Dozent einen
Aktivierungscode. Die zweite Mailadresse gehört zum Sekretariat.

Dozenten werden in der App unter **Termine → Termin öffnen** einem Modul
zugeordnet. Die Zuordnung gilt für künftige Termine. Ein einzelner Termin kann
einen anderen Dozenten bekommen.

---

## Studierende

**Schema:**
```bash
ba-admin student list [<kurs>]
ba-admin student create <kurs> <matrikelnummer> <vorname> <nachname> <email>
ba-admin student update <email> <matrikelnummer> <neuer-vorname> <neuer-nachname> <neue-email>
ba-admin student code <student-email> <sekretariat-email>
ba-admin student course <student-email> <neuer-kurs>
ba-admin student disable <student-email> <sekretariat-email>
ba-admin student enable <student-email>
```

**Beispiele:**
```bash
ba-admin student list
ba-admin student list AI_WS24_IV
ba-admin student create AI_WS24_IV 'DEMO-AI24IV 004' Demo Student demo-ai24iv.004@bacheckin.internal
ba-admin student update demo-ai24iv.004@bacheckin.internal 'DEMO-AI24IV 004' Demo Student demo-ai24iv.004@bacheckin.internal
ba-admin student code demo-ai24iv.004@bacheckin.internal sekretariat.ma1@bacheckin.internal
ba-admin student course demo-ai24iv.004@bacheckin.internal WI_WS24_II
ba-admin student disable demo-ai24iv.004@bacheckin.internal sekretariat.ma1@bacheckin.internal
ba-admin student enable demo-ai24iv.004@bacheckin.internal
```

`student course` verschiebt einen Studenten in einen anderen Kurs.

---

## Kurse

**Schema:**
```bash
ba-admin course list
ba-admin course create <kurs>
ba-admin course update <alter-kurs> <neuer-kurs>
ba-admin course disable <kurs>
ba-admin course enable <kurs>
```

**Beispiele:**
```bash
ba-admin course list
ba-admin course create AI_WS24_IV
ba-admin course update ALTER_NAME NEUER_NAME
ba-admin course disable AI_WS24_IV
ba-admin course enable AI_WS24_IV
```

Der Trainex-Import legt fehlende Kurse automatisch an. Bereits vorhandene
deaktivierte Kurse müssen vor dem Import wieder aktiviert werden. Module und
Termine legt der Import ebenfalls selbst an.

---

## Termine (manuell, Standard = Auto-Import)

**Schema:**
```bash
ba-admin lecture list <kurs>
ba-admin lecture create <kurs> <modul> <titel> <start-iso> <ende-iso> <raum|->
ba-admin lecture update <termin-id> <modul> <titel> <start-iso> <ende-iso> <raum|->
ba-admin lecture delete <termin-id>
```

**Beispiele:**
```bash
ba-admin lecture list AI_WS24_IV
ba-admin lecture create AI_WS24_IV 'NFC-Test' 'Testtermin' '2026-09-15T18:00:00+02:00' '2026-09-15T19:00:00+02:00' 'Testraum'
ba-admin lecture update TERMIN-ID 'NFC-Test' 'Neuer Titel' '2026-09-15T18:30:00+02:00' '2026-09-15T19:30:00+02:00' 'Testraum'
ba-admin lecture delete TERMIN-ID
```

Die `TERMIN-ID` steht bei `list` oder in der Ausgabe von `create`. Zeiten
brauchen eine Zeitzone, zum Beispiel `+02:00` oder `Z`. Für keinen Raum `-`
eintragen.

Manuelle Termine bleiben beim Trainex-Import erhalten. Importierte Termine
können beim nächsten Import überschrieben werden. Vergangene Termine lassen
sich unabhängig von erfasster oder bestätigter Anwesenheit löschen. Dabei
werden die Anwesenheitseinträge des Termins mitgelöscht; NFC-Ereignisse bleiben
ohne Terminzuordnung erhalten.
Geöffnete oder abgeschlossene Termine lassen sich nicht mehr ändern.

---

## Geräte

**Schema:**
```bash
ba-admin device list <email>
ba-admin device revoke <device-id> <sekretariat-email>
```

**Beispiele:**
```bash
ba-admin device list sekretariat.ma1@bacheckin.internal
ba-admin device revoke DEVICE-ID sekretariat.ma1@bacheckin.internal
```

Die `DEVICE-ID` steht bei `device list`. `revoke` sperrt das Gerät.

---

## NFC-Karten

**Schema:**
```bash
ba-admin card list
ba-admin card show <uid>
ba-admin card create <uid> <bezeichnung>
ba-admin card update <uid> <neue-bezeichnung>
ba-admin card disable <uid>
ba-admin card enable <uid>
```

**Beispiele:**
```bash
ba-admin card list
ba-admin card show 047262CA9C1790
ba-admin card create ECHTE-UID 'BA-Karte 002'
ba-admin card update 047262CA9C1790 'BA-Karte 001'
ba-admin card disable 047262CA9C1790
ba-admin card enable 047262CA9C1790
```

Die UID hat 14 Hex-Zeichen. `create` registriert die Karte im Backend. Die
Karte muss zusätzlich mit NDEF und SDM beschrieben werden. `disable` sperrt
neue Check-ins. Alte Anwesenheiten bleiben erhalten.
