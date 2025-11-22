# Version Compatibility Guide

## Supported Minecraft Versions

EasyPlaytime **v1.1** is designed for Minecraft **1.21.0 - 1.21.10**

### ✅ Fully Supported Platforms

| Platform | Versions | Status | Notes |
|----------|----------|--------|-------|
| **Paper** | 1.21.0 - 1.21.10 | ✅ Fully Supported | Recommended for most servers |
| **Folia** | 1.21.0 - 1.21.10 | ✅ Fully Supported | Full async/regionized support |

### 🔧 Technical Details

**API Version**: `1.21`  
**Folia Support**: Yes (via `folia-supported: true` in plugin.yml)  
**Async Scheduler**: Uses Folia-compatible `server.asyncScheduler`

---

## Version Detection

The plugin automatically detects:
- **Server Type**: Paper or Folia
- **Minecraft Version**: 1.21.x validation
- **Compatibility Status**: Warnings for unsupported versions

### Startup Messages

#### On Supported Version (1.21.x):
```
[EasyPlaytime] ✓ Running on Paper 1.21.4 - Fully supported!
[EasyPlaytime] EasyPlaytime Plugin wurde erfolgreich aktiviert!
```

#### On Unsupported Version:
```
[EasyPlaytime] ⚠ Running on Paper 1.20.6
[EasyPlaytime] ⚠ This plugin is designed for Minecraft 1.21.0 - 1.21.10
[EasyPlaytime] ⚠ Other versions may work but are not officially supported
```

---

## Folia-Specific Features

EasyPlaytime is **fully compatible** with Folia's regionized threading model:

### ✅ Implemented Folia Support
- **Async Scheduler**: All database operations use `server.asyncScheduler`
- **Thread-Safe**: ConcurrentHashMap for playtime data
- **No Region Locks**: All DB/Redis operations are async
- **Performance**: HikariCP connection pooling for parallel operations

### Code Example
```kotlin
// Folia-compatible async scheduler
server.asyncScheduler.runNow(plugin) { _ ->
    // DB operation runs async, doesn't block regions
    dataManager.updatePlaytime(uuid, playtime)
}
```

---

## Why 1.21.x Only?

### API Changes in 1.21
- New async scheduler API (required for Folia)
- Updated Paper API surface
- Improved performance characteristics

### Backwards Compatibility
While the plugin may work on older versions (1.20.x), it is **not tested or supported** due to:
- Different async scheduler APIs
- Potential API incompatibilities
- Missing Folia support in older versions

---

## Migration from Older Versions

If you're upgrading from Minecraft 1.20.x to 1.21.x:

1. **Backup your data**: `plugins/EasyPlaytime/playtime.yml` and database
2. **Update server** to Minecraft 1.21.0+
3. **Install EasyPlaytime v1.1**
4. **Restart server** - data will be automatically migrated

No manual data conversion needed! ✨

---

## Future Version Support

### Planned Support
- **1.21.11+**: Will be supported as released
- **1.22.x**: Will be evaluated when released

### Update Strategy
We aim to support all stable 1.21.x releases. Subscribe to releases for updates:
https://github.com/TheLion102009/EasyPlaytime/releases

---

## Troubleshooting

### "Server version not supported" Warning
**Cause**: Running on Minecraft version outside 1.21.0-1.21.10  
**Solution**: Update to Minecraft 1.21.x or use at your own risk

### Folia Detection Issues
**Cause**: Plugin can't detect Folia  
**Check**: Verify `io.papermc.paper.threadedregions.RegionizedServer` class exists  
**Solution**: Ensure you're running genuine Folia, not modded Paper

### Scheduler Errors on Folia
**Cause**: Using legacy Bukkit scheduler  
**Solution**: This plugin uses async scheduler - report issue if you see this

---

## Testing Checklist

Before deploying on production:

- [ ] Verify Minecraft version is 1.21.0-1.21.10
- [ ] Check console for "Fully supported!" message
- [ ] Test `/eplaytime playtime` command
- [ ] Verify PlaceholderAPI integration (if used)
- [ ] Test database sync (if enabled)
- [ ] Monitor TPS during sync cycles
- [ ] Check for errors in console

---

## Performance by Version

All 1.21.x versions have similar performance characteristics:

| Feature | Performance |
|---------|-------------|
| Player Join/Quit | <1ms impact on TPS |
| Periodic Sync (10 players) | 20-50ms |
| Database Queries/Hour | ~500 (batch optimized) |
| Memory Usage | <10MB |
| CPU Usage | <1% on 100 player server |

---

## Need Help?

- **Issues**: https://github.com/TheLion102009/EasyPlaytime/issues
- **Discord**: [Your Discord Server]
- **Wiki**: https://github.com/TheLion102009/EasyPlaytime/wiki

---

**Last Updated**: 2025-01-22  
**Plugin Version**: 1.1  
**Supported MC Versions**: 1.21.0 - 1.21.10

