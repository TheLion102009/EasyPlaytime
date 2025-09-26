# EasyPlaytime

**EasyPlaytime** is a lightweight Minecraft plugin that allows you to display player playtime directly in the **tab list** (via placeholders) or in the **chat** using simple commands.  
Perfect for servers running **Paper** or **Folia** on Minecraft 1.21.x and newer.  

---

## ✨ Features

- Show player playtime in the **tab list** with a placeholder  
- Display playtime in chat with easy-to-use commands  
- Fully configurable time format (days, hours, minutes, seconds)  
- Simple and lightweight setup  
- Works seamlessly with **Paper** and **Folia**  

---

## 📦 Installation

1. Download the latest `.jar` from the [releases page](#).  
2. Place the `.jar` file into your server’s `plugins` folder.  
3. Start your server to generate the default configuration file.  
4. Open `plugins/EasyPlaytime/config.yml` and adjust the settings as desired.  
5. Restart your server or use `/eplaytime reload` to apply changes.  

---

## ⌨️ Commands

| Command | Description |
|---------|-------------|
| `/easyplaytime playtime [player]` | Shows a player’s playtime in chat |
| `/eplaytime reload` | Reloads the configuration |

---

## 🔑 Permissions

| Permission | Description |
|------------|-------------|
| `easyplaytime.playtime` | Allows use of the playtime command |
| `easyplaytime.reload` | Allows reloading the configuration |

---

## ⚙️ Configuration

The configuration file allows you to define which time units are displayed and customize plugin messages.  

**Example `config.yml`:**

```yaml
format:
  show-days: true
  show-hours: true
  show-minutes: true
  show-seconds: true

messages:
  no-permission: "&cYou don’t have permission for this command."
  player-not-found: "&cPlayer '{player}' was not found."
  config-reloaded: "&aConfiguration reloaded!"
