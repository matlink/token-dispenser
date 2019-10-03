package com.github.yeriomin.tokendispenser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Properties;

import static spark.Spark.after;
import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.halt;
import static spark.Spark.ipAddress;
import static spark.Spark.notFound;
import static spark.Spark.port;

public class Server {

    static public final Logger LOG = LoggerFactory.getLogger(Server.class.getName());

    static private final String CONFIG_FILE = "/config.properties";

    static final String PROPERTY_SPARK_HOST = "spark-host";
    static final String PROPERTY_SPARK_PORT = "spark-port";
    static final String PROPERTY_STORAGE = "storage";
    static final String PROPERTY_STORAGE_PLAINTEXT_PATH = "storage-plaintext-path";
    static final String PROPERTY_MONGODB_HOST = "mongodb-host";
    static final String PROPERTY_MONGODB_PORT = "mongodb-port";
    static final String PROPERTY_MONGODB_USERNAME = "mongodb-username";
    static final String PROPERTY_MONGODB_PASSWORD = "mongodb-password";
    static final String PROPERTY_MONGODB_DB = "mongodb-databaseNameStorage";
    static final String PROPERTY_MONGODB_COLLECTION = "mongodb-collectionName";
    static final String PROPERTY_EMAIL_RETRIEVAL = "enable-email-retrieval";
    static final String PROPERTY_BASIC_AUTH = "basic-auth";

    static public final String STORAGE_MONGODB = "mongodb";
    static public final String STORAGE_PLAINTEXT = "plaintext";
    static public final String STORAGE_ENV = "env";

    static public final String ENV_TOKEN_CREDENTIALS = "TOKEN_CREDENTIALS";
    
    static PasswordsDbInterface passwords;

    public static void main(String[] args) {
        Properties config = getConfig();
        String host = config.getProperty(PROPERTY_SPARK_HOST, "0.0.0.0");
        int port = Integer.parseInt(config.getProperty(PROPERTY_SPARK_PORT, "8080"));
        String hostDiy = System.getenv("OPENSHIFT_DIY_IP");
        if (null != hostDiy && !hostDiy.isEmpty()) {
            host = hostDiy;
            port = Integer.parseInt(System.getenv("OPENSHIFT_DIY_PORT"));
        }
        ipAddress(host);
        port(port);
        notFound("Not found");
        before((req, res) -> {
            LOG.info(req.requestMethod() + " " + req.url());
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Request-Method", "GET");
        });
        after((req, res) -> res.type("text/plain"));
        String basicAuth = config.getProperty(PROPERTY_BASIC_AUTH, "");
        if (!basicAuth.equals("")) {
            try {
                String[] pair = basicAuth.split(":");
                if (pair.length != 2) { 
                    LOG.error(PROPERTY_BASIC_AUTH + " not in the format '<user>:<pass>'.");
                    return;
                }
                String user = URLDecoder.decode(pair[0], StandardCharsets.UTF_8.name());
                String pass = URLDecoder.decode(pair[1], StandardCharsets.UTF_8.name());
                before((req, res) -> {
                    if (req.pathInfo().equals("/health")) return;
                    String header = req.headers("Authorization");
                    if (header == null) halt(401, "Access denied.");
                    String[] parts = header.split(" ");
                    if (parts.length != 2) halt(400, "Malformed auth header.");
                    if (!parts[0].equals("Basic")) halt(401, "Unsupported auth method.");
                    String[] creds = new String(Base64.getDecoder().decode(parts[1])).split(":");
                    if (creds.length != 2) halt(400, "Malformed auth header.");
                    if (!creds[0].equals(user) || !creds[1].equals(pass)) halt(401, "Access denied.");
                });
            } catch (UnsupportedEncodingException e) {
                Server.LOG.error("UTF-8 is unsupported.");
                return;
            }
        }
        Server.passwords = PasswordsDbFactory.get(config);
        get("/health", (req, res) -> "");
        get("/token/email/:email", (req, res) -> new TokenResource().handle(req, res));
        get("/token-ac2dm/email/:email", (req, res) -> new TokenAc2dmResource().handle(req, res));
        if (config.getProperty(PROPERTY_EMAIL_RETRIEVAL, "false").equals("true")) {
            get("/email", (req, res) -> new EmailResource().handle(req, res));
            get("/email/gsfid", (req, res) -> new TokenAc2dmGsfIdResource().handle(req, res));
            get("/email/gsfid/:device", (req, res) -> new TokenAc2dmGsfIdResource().handle(req, res));
        }
    }

    static Properties getConfig() {
        Properties properties = new Properties();
        try (InputStream input = PasswordsDbMongo.class.getResourceAsStream(CONFIG_FILE)) {
            properties.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        String host = System.getenv("OPENSHIFT_MONGODB_DB_HOST");
        if (null != host && !host.isEmpty()) {
            properties.put(PROPERTY_MONGODB_HOST, host);
            properties.put(PROPERTY_MONGODB_PORT, System.getenv("OPENSHIFT_MONGODB_DB_PORT"));
            properties.put(PROPERTY_MONGODB_USERNAME, System.getenv("OPENSHIFT_MONGODB_DB_USERNAME"));
            properties.put(PROPERTY_MONGODB_PASSWORD, System.getenv("OPENSHIFT_MONGODB_DB_PASSWORD"));
            properties.put(PROPERTY_MONGODB_DB, System.getenv("OPENSHIFT_APP_NAME"));
        }
        String basicAuth = System.getenv(PROPERTY_BASIC_AUTH.replace("-", "_").toUpperCase());
        if (basicAuth != null) {
            properties.put(PROPERTY_BASIC_AUTH, basicAuth);
        }
        String storage = System.getenv(PROPERTY_STORAGE.toUpperCase());
        if (Arrays.asList(STORAGE_MONGODB, STORAGE_PLAINTEXT, STORAGE_ENV).contains(storage)) {
            properties.put(PROPERTY_STORAGE, storage);
        }
        return properties;
    }
}
