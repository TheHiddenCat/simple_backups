# 💾 SimpleBackups

A PaperMC plugin for creating and managing backups of your world. Backups are stored separately and are compressed to safe storage.

## Features
- Create backups of all loaded worlds asynchronously
- Ensure all data is safely flushed before backup creation
- Tab completion and permission-based access control
- Build for PaperMC 1.21.7+

## Installation

1. Build the `.jar` file using Maven package.
2. Place it in your `plugins/` directory.
3. Restart or reload your server.

## Commands

| Command | Description |
|--------|-------------|
| `/simplebackups backup` | Backup all loaded worlds |
| `/simplebackups help` | Show all commands |
