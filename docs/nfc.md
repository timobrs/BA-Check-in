# NFC

Für den Check-in werden NTAG-424-DNA-Karten verwendet. Jede Karte hat eine
öffentliche ID und erzeugt beim Lesen neue Prüfdaten. Personen und Termine
werden nicht auf der Karte gespeichert.

## Karte vorbereiten

1. Die echte Karten-UID im Backend registrieren
2. Die Karte mit einem Verwaltungswerkzeug für NFC einrichten
3. Die Karte mit einem echten Android-Gerät testen

Die Studenten-App richtet keine Karten ein. Die Befehle zum Registrieren und
Sperren stehen in [admin-management.md](admin-management.md).

## Check-in

Der Student hält die Karte an sein Handy. Die App liest die Kartendaten und
schickt sie an das Backend. Das Backend prüft die Karte und sucht einen
passenden Termin. Wenn alles stimmt, wird die Anwesenheit eingetragen.

Eine Nachricht von der Karte kann nur einmal verwendet werden. Ohne passenden
Termin gibt es keinen Check-in.

## Betrieb

Verlorene Karten werden im Backend gesperrt. Die Kartenschlüssel bleiben auf
dem Server und gehören nicht in die App oder ins Repository. Eine Karte nach
einem Zählerreset wieder in Betrieb zu nehmen, braucht eine geplante
Schlüsseländerung.
