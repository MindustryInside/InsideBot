# About Inside bot
Admin/log bot.

## Configuration and other
Go to [config.json](src/main/resources/settings.properties) and write your bot token & database url with password and username.<br>
Bot will require a [postgresql](https://www.postgresql.org/download/) database

## Building
The build requires a JDK14

**Windows**: `gradlew dist` / `gradlew.bat dist`<br>
**Linux**: `./gradlew dist` (If don\`t work write this command: `chmod +x ./gradlew`)

**Jar file location**: `/build/libs/`
