# token-dispenser
Stores email-password pairs, gives out Google Play Store tokens

# Installation (debian jessie)
- Add `deb http://ftp.de.debian.org/debian jessie-backports main` to `/etc/apt/sources.list`
- `apt update`
- `apt -t jessie-backports install openjdk-8-jdk maven git`
- `git clone https://github.com/matlink/token-dispenser`
- `cd token-dispenser`
- Edit `src/main/resources/config.properties` to match your environment
- `mvn package`
- `update-java-alternatives -s java-1.8.0-openjdk-amd64`
- `java -jar target/token-dispenser-0.1.jar`
