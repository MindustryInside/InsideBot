# About Inside bot
_Nothing_

## Configuration
Go to [config.json](src/main/resources/config.json) and write the db token and address.<br>
The database address must be written in this format: `jdbc:h2:tcp://localhost:9092/~/folder/db_name`<br>

Where `~`  - bot root.<br>
`folder` - subfolder in ```~```<br>
`db_name` - The name that the database will be created with if it doesn't exist

## Building
The build requires a JDK14

**Windows**: `gradlew dist` / `gradlew.bat dist`<br>
**Linux**: `./gradlew dist` (If don\`t work write this command: `chmod +x ./gradlew`)

**Jar file location**: `/build/libs/`
