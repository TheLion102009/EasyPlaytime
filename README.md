EasyPlaytime

EasyPlaytime is a lightweight Minecraft plugin that lets you display player playtime directly in the tab list (via placeholders) or in the chat using simple commands. Perfect for servers running Paper or Folia on Minecraft 1.21.x and beyond, it provides a clean and customizable way to track how long players have been online.

Features

Show player playtime in the tab list with a placeholder

Display playtime in chat with easy-to-use commands

Fully configurable time format (days, hours, minutes, seconds)

Compatible with Paper and Folia for best performance

Simple setup, minimal configuration required

Installation

Download the latest .jar from the releases page
.

Place the .jar file in your server’s plugins folder.

Start your server to generate the default configuration file.

Open plugins/EasyPlaytime/config.yml and adjust the settings as needed.

Restart your server or use /eplaytime reload to apply changes.

Commands
Command	Description
/easyplaytime playtime [player]	Shows a player’s playtime in chat
/eplaytime reload	Reloads the configuration
Permissions
Permission	Description
easyplaytime.playtime	Allows use of the playtime command
easyplaytime.reload	Allows reloading the configuration
Configuration

The configuration file allows you to choose which time units are displayed and customize plugin messages. Example:

format:
  show-days: true
  show-hours: true
  show-minutes: true
  show-seconds: true

messages:
  no-permission: "&cYou don’t have permission for this command."
  player-not-found: "&cPlayer '{player}' was not found."
  config-reloaded: "&aConfiguration reloaded!"

Placeholder

Use the built-in placeholder to display player playtime in the tab list or anywhere else:

%easyplaytime_playtime%
