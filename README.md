# This fork is deprecated. Please use the original one, which provide a simple way to deploy it without running a mongodb server.
# token-dispenser
Stores email-password pairs, gives out Google Play Store tokens

# Installation (debian jessie)
Deploy a mongodb server (https://docs.mongodb.com/manual/tutorial/install-mongodb-on-debian/)
-----------------------
- `apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 0C49F3730359A14518585931BC711F9BA15703C6`
- `echo"deb http://repo.mongodb.org/apt/debian jessie/mongodb-org/3.4 main" > /etc/apt/sources.list.d/mongodb-org-3.4.list`
- `apt update`
- `apt install mongodb-org`
- `mongo`
- `use tokendispenser`
- `db.passwords.insertOne( { "email": "gplaycliacc@gmail.com", "password": "abcdefgh" } )`
- `db.createUser( { user: "tokenuser", "pwd": "tokenpwd", roles: [ { role: "read", db: "tokendispenser" } ] } )`

Deploy tokendispenser server
----------------------------
- Add `deb http://ftp.de.debian.org/debian jessie-backports main` to `/etc/apt/sources.list`
- `apt update`
- `apt -t jessie-backports install openjdk-8-jdk maven git`
- `git clone https://github.com/matlink/token-dispenser`
- `cd token-dispenser`
- Edit `src/main/resources/config.properties` to match your environment (what you setted in Mongodb, IP is probably 127.0.0.1)
- `mvn package`
- `update-java-alternatives -s java-1.8.0-openjdk-amd64`
- `java -jar target/token-dispenser-0.1.jar`
