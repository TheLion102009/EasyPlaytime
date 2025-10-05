# EasyPlaytime

**EasyPlaytime** is a lightweight Minecraft plugin that allows you to display player playtime directly in the **tab list** (via placeholders) or in the **chat** using simple commands.  
Perfect for servers running **Paper** or **Folia** on Minecraft 1.21.x and newer.

---

## ✨ Features

- Show player playtime in the **tab list** with a placeholder  
- Display playtime in chat with easy-to-use commands  
- Fully configurable time format (days, hours, minutes, seconds)  
- **Cross-server playtime synchronization** with MariaDB/MySQL database support
- Automatic data migration from YAML to database  
- Simple and lightweight setup  
- Works seamlessly with **Paper** and **Folia**  

---

## 📦 Installation

1. Download the latest `.jar` from the [releases page](#).  
2. Place the `.jar` file into your server's `plugins` folder.  
3. Start your server to generate the default configuration file.  
4. (Optional) Set up a MariaDB/MySQL database for cross-server synchronization (see Database Setup section).  
5. Open `plugins/EasyPlaytime/config.yml` and adjust the settings as desired.  
6. Restart your server or use `/eplaytime reload` to apply changes.

---

## 🗄️ Database Setup (Optional)

For cross-server playtime synchronization, you can enable database support:

### Prerequisites
- MariaDB or MySQL server installed and running
- Database user with appropriate permissions

### Configuration
1. Open `plugins/EasyPlaytime/config.yml`
2. Set `database.enabled` to `true`
3. Configure your database connection:

```yaml
database:
  enabled: true
  host: "localhost"
  port: 3306
  database: "easyplaytime"
  username: "minecraft"
  password: "your_secure_password"
  table: "player_playtime"
```

### Migration
When you enable the database for the first time:
1. Update your configuration
2. Use `/eplaytime reload` 
3. The plugin will automatically migrate existing YAML data to the database

**Note:** The database user only needs `SELECT`, `INSERT`, and `UPDATE` permissions on the playtime table.

---

## ⌨️ Commands

| Command | Description |
|---------|-------------|
| `/easyplaytime playtime [player]` | Shows a player's playtime in chat |
| `/eplaytime reload` | Reloads the configuration and migrates data if database is enabled |

---

## 🔑 Permissions

| Permission | Description |
|------------|-------------|
| `easyplaytime.playtime` | Allows use of the playtime command |
| `easyplaytime.reload` | Allows reloading the configuration |

---

## ⚙️ Configuration

The configuration file allows you to define which time units are displayed, customize plugin messages, and configure database settings.

**Example `config.yml`:**

```yaml
# Time format settings
format:
  show-days: true
  show-hours: true
  show-minutes: true
  show-seconds: true

# Database configuration for cross-server synchronization
database:
  enabled: false
  host: "localhost"
  port: 3306
  database: "easyplaytime"
  username: "minecraft"
  password: "password"
  table: "player_playtime"

# Plugin messages
messages:
  no-permission: "&cYou don't have permission for this command."
  player-not-found: "&cPlayer '{player}' was not found."
  config-reloaded: "&aConfiguration reloaded!"
