# token-dispenser
Stores email-password pairs, gives out Google Play Store tokens.

Using Google Play Store API requires logging in using email and password. If you have a project which works with Google Play Store API you no longer have to make the users use their live accounts or ship your software with your account credentials inside. You can deploy a token dispenser instance and it will provide auth tokens on demand without letting the world know your password.

### Building

1. `git clone https://github.com/matlink/token-dispenser`
2. `cd token-dispenser`
3. Edit `src/main/resources/config.properties`
4. `mvn install`
5. `java -jar target/token-dispenser.jar`

### Docker image

1. Run it. `docker run --name td -d -t -p 8080:8080 matlink/token-dispenser:latest`
2. Enter container shell. `docker exec -it td bash`
3. Edit `passwords/passwords.txt` and add your email-password pairs. One pair - one line. `nano` is included in the image.
4. Exit container shell and restart the container. `docker restart td`

### Configuration

[config.properties](/src/main/resources/config.properties) holds token dispenser's configuration.

Two things are configurable:
* web server
* storage

#### Web server

Token dispenser uses [spark framework](http://sparkjava.com/). To configure network address and port on which spark should listen change `spark-host` and `spark-port`.

Basic auth is also available. To enable, change `basic-auth` to `<user>:<pass>`.

#### Storage

There are three storage options supported:
* **Plain text** Set `storage` to `plaintext` to use it. `storage-plaintext-path` property is used to store filesystem path to a plain text file with email-password pairs. There is an example [here](/passwords/passwords.txt). Securing it is up to you.
* **MongoDB** Set `storage` to `mongodb` to use it. Configurable parameters are self-explanatory.
* **Environment** Set `storage` to `env` to use it. Set the environment variable `TOKEN_CREDENTIALS` before starting Token dispenser. `TOKEN_CREDENTIALS` takes URL-encoded, comma-separated pairs of emails and passwords. Each pair must contain a colon to separate the email and password. For example, `TOKEN_CREDENTIALS=myemail%40gmail.com:password,myotheremail%40yahoo.com:1234`.

### Usage
Once server is configured, you can get the tokens for **regular requests** at http://server-address:port/token/email/youremail@gmail.com
and tokens for **checkin requests** at http://server-address:port/token-ac2dm/email/youremail@gmail.com
#### with gplaycli
gplaycli requires also the GSFid. Token and GSFid can be retrieved using http://server-adress:port/email/gsfid. A random couple (login, password) will be fetched from the provided ones, to load balance over multiple accounts, mitigating account ban possibilities.

### Credits

* [play-store-api](https://github.com/yeriomin/play-store-api)
* [spark](http://sparkjava.com/)

