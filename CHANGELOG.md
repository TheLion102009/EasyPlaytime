# Changelog

## Version 1.2 (2025-01-22)

### 🎉 Neue Features
- **Automatisches Config-Update-System**: Die Config wird beim Plugin-Start automatisch aktualisiert, ohne dass Sie die config.yml löschen müssen
- **Config-Versionierung**: `config-version` wird automatisch verwaltet und für Updates verwendet
- **show-seconds Option**: Jetzt können Sie in der Config `format.show-seconds: true/false` einstellen
- **Sync-Debug-Modus**: Mit `database.sync-debug: true/false` können Sie Debug-Ausgaben für Datenbank-Synchronisationen ein-/ausschalten
- **Smart-Synchronization**: Nur Online-Spieler werden synchronisiert für bessere Performance

### 🔧 Verbesserungen
- Debug-Nachrichten wie "Datenquellen-Synchronisation abgeschlossen. X Spieler synchronisiert." werden nur noch angezeigt wenn `sync-debug: true` ist
- Performance-Optimierungen bei der Synchronisation

### 📝 Änderungen
- Version erhöht von 1.1 auf 1.2
- Config-Version auf 2 erhöht

---

## Version 1.1
- Cross-server playtime synchronization mit MariaDB/MySQL
- Redis Support
- HikariCP Connection Pooling
- Batch-Updates für bessere Performance
- Folia Support

## Version 1.0
- Initiales Release
- Grundlegende Spielzeit-Tracking-Funktionalität
- PlaceholderAPI Integration
- YAML-Datenspeicherung

