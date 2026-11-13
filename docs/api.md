# App-Backend-API

Alle Pfade beginnen mit `/api/v1`.
Die Tabellen zeigen nur den restlichen Pfad:

## Aktivierung und Gerät

| Aufruf | Beschreibung |
|---|---|
| `POST /secretary/users/{userId}/device-activation-codes` | Aktivierungscode für einen Benutzer erstellen |
| `POST /device-activations/challenges` | Challenge für die Geräteaktivierung anfordern |
| `POST /device-activations` | Gerät mit seinem Schlüssel aktivieren |
| `POST /device-sessions/challenges` | Challenge für die Anmeldung anfordern |
| `POST /device-sessions/open` | Gerätesitzung öffnen |
| `POST /device-sessions/refresh` | Sitzungstoken erneuern |
| `POST /device-sessions/logout` | Gerätesitzung beenden |
| `DELETE /devices/current` | Eigenes Gerät vom Konto lösen |

## Studierende

| Aufruf | Beschreibung |
|---|---|
| `GET /student/home` | Startdaten des Studenten laden |
| `POST /student/absences` | Abwesenheit melden |
| `POST /student/nfc/check-ins` | Mit NFC einchecken |

## Dozenten

| Aufruf | Beschreibung |
|---|---|
| `GET /lecturer/home` | Eigene Termine laden |
| `POST /lecturer/appointments/{appointmentId}/open` | Termin öffnen |
| `PUT /lecturer/appointments/{appointmentId}/attendances/{studentId}` | Anwesenheit eines Studenten ändern |
| `POST /lecturer/appointments/{appointmentId}/attendances/validate` | Anwesenheiten bestätigen |
| `PUT /lecturer/appointments/{appointmentId}/protokollant` | Protokollant festlegen |

## Sekretariat

| Aufruf | Beschreibung |
|---|---|
| `GET /secretary/home` | Verwaltungsdaten laden |
| `PUT /secretary/appointments/{appointmentId}/attendances/{studentId}` | Anwesenheit eines Studenten ändern |
| `POST /secretary/appointments/{appointmentId}/attendances/validate` | Anwesenheiten bestätigen |
| `POST /secretary/users/{userId}/device/revoke` | Aktives Gerät eines Benutzers sperren |
| `POST /secretary/devices/{deviceId}/revoke` | Ausgewähltes Gerät sperren |
| `PUT /secretary/users/{userId}/modules/{moduleId}` | Studenten einem Modul zuordnen |
| `DELETE /secretary/users/{userId}/modules/{moduleId}` | Modulzuordnung eines Studenten entfernen |
| `PUT /secretary/appointments/{appointmentId}/module` | Modul eines Termins ändern |
| `PUT /secretary/appointments/{appointmentId}/ignored` | Ignorierstatus eines Termins ändern |
| `PUT /secretary/modules/{moduleId}/ignored` | Ignorierstatus eines Moduls ändern |
| `PUT /secretary/modules/{moduleId}/lecturer` | Dozenten eines Moduls festlegen |
| `PUT /secretary/appointments/{appointmentId}/lecturer` | Dozenten eines Termins festlegen |
| `PUT /secretary/absences/{absenceId}` | Abwesenheitsmeldung prüfen |
| `PUT /secretary/calendar-imports/{sourceKey}` | Trainex Kalender importieren |

---

Genaue Felder stehen auch in der OpenAPI unter`/internal/api-docs` nach lokalem Build
