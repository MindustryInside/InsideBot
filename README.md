# Inside Bot

## About

Administration & log bot.

## Configuration

Setup [application.yml](https://github.com/MindustryINSIDE/InsideBot/blob/master/src/main/resources/application.yml):

* Write Discord bot token.
* Set PostgreSQL database url, username and password.

Note: Bot requires [PostgreSQL](https://www.postgresql.org/download/) database.

## Building

Make sure you have [JDK 14](https://adoptopenjdk.net/releases.html?variant=openjdk14) installed, then run the following commands:

* **Windows**: `gradlew bootJar`  
* **Linux**: `./gradlew bootJar`

After building, the .JAR file should be located in `build/libs/InsideBot.jar` folder.

If the terminal returns `Permission denied` or `Command not found`, run `chmod +x ./gradlew`.
