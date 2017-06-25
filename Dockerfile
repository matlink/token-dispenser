FROM buildpack-deps:jessie-scm

# Default values from token-dispenser/src/main/resources/config.properties
ARG MONGO_USER=gplaycli
ARG MONGO_PASSWD=5BKiXgRu84tIuAxu6acm

RUN apt-get update && apt-get install -y --no-install-recommends \
		bzip2 \
		unzip \
		xz-utils

RUN echo 'deb http://deb.debian.org/debian jessie-backports main' > /etc/apt/sources.list.d/jessie-backports.list

# Default to UTF-8 file.encoding
ENV LANG C.UTF-8

# add a simple script that can auto-detect the appropriate JAVA_HOME value
# based on whether the JDK or only the JRE is installed
RUN { \
		echo '#!/bin/sh'; \
		echo 'set -e'; \
		echo; \
		echo 'dirname "$(dirname "$(readlink -f "$(which javac || which java)")")"'; \
	} > /usr/local/bin/docker-java-home \
	&& chmod +x /usr/local/bin/docker-java-home

# do some fancy footwork to create a JAVA_HOME that's cross-architecture-safe
RUN ln -svT "/usr/lib/jvm/java-8-openjdk-$(dpkg --print-architecture)" /docker-java-home

ENV JAVA_HOME /docker-java-home
ENV JAVA_VERSION 8u131
ENV JAVA_DEBIAN_VERSION 8u131-b11-1~bpo8+1

# see https://bugs.debian.org/775775
# and https://github.com/docker-library/java/issues/19#issuecomment-70546872
ENV CA_CERTIFICATES_JAVA_VERSION 20161107~bpo8+1

RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 0C49F3730359A14518585931BC711F9BA15703C6
RUN echo "deb http://repo.mongodb.org/apt/debian jessie/mongodb-org/3.4 main" | tee /etc/apt/sources.list.d/mongodb-org-3.4.list

RUN set -ex; \
	\
	apt-get update; \
	apt-get install -y maven git mongodb-org \
		openjdk-8-jdk="$JAVA_DEBIAN_VERSION" \
		ca-certificates-java="$CA_CERTIFICATES_JAVA_VERSION"  \
	; \
	apt-get autoremove -y; \
	rm -rf /var/lib/apt/lists/*; \
	\
	[ "$(readlink -f "$JAVA_HOME")" = "$(docker-java-home)" ]; \
	\
	update-alternatives --set java /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java

# see CA_CERTIFICATES_JAVA_VERSION notes above
RUN /var/lib/dpkg/info/ca-certificates-java.postinst configure

RUN git clone https://github.com/matlink/token-dispenser /root/token-dispenser \
	&& sed -i.bak s/127.0.0.1/localhost/g /root/token-dispenser/src/main/resources/config.properties \
	&& cd /root/token-dispenser && mvn package

EXPOSE 8080

ENTRYPOINT mkdir -p /data/db \
	&& mongod --fork --logpath /var/log/mongod.log \
	&& mongo tokendispenser --eval "db = db.getSiblingDB('tokendispenser');db.passwords.insertOne( { email: \"$GPLAY_USER\", password: \"$GPLAY_PASSWD\" } ) ; db.createUser( { user: \"$MONGO_USER\", pwd: \"$MONGO_PASSWD\",  roles: [ { role: \"read\", db: \"tokendispenser\" } ] } )" \
	&& java -jar /root/token-dispenser/target/token-dispenser-0.1.jar