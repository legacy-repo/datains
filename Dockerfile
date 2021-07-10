###################
# STAGE 1: builder
###################

# Build currently doesn't work on > Java 11 (i18n utils are busted) so build on 8 until we fix this
FROM adoptopenjdk/openjdk8:centos as builder

WORKDIR /app/source

ENV FC_LANG en-US
ENV LC_CTYPE en_US.UTF-8

# bash:    various shell scripts
# wget:    installing lein
# git:     ./bin/version
# make:    backend building
# gettext: translations
RUN yum install -y coreutils bash git wget make gettext

# lein:    backend dependencies and building
ADD ./bin/lein /usr/local/bin/lein
RUN chmod 744 /usr/local/bin/lein
RUN lein upgrade

# install dependencies before adding the rest of the source to maximize caching

# backend dependencies
ADD project.clj .
RUN lein deps

# add the rest of the source
ADD . .

# build the app
RUN bin/build

# Install updated cacerts to /etc/ssl/certs/java/cacerts
# RUN yum install -y java-cacerts

# ###################
# # STAGE 2: runner
# ###################

FROM adoptopenjdk/openjdk11:centos-jre as runner

LABEL org.opencontainers.image.source https://github.com/clinico-omics/datains

WORKDIR /app

ENV FC_LANG en-US
ENV LC_CTYPE en_US.UTF-8
ENV PATH="/app/external:/app/bin:/app/external/.env/bin:${PATH}"

# Dependencies
## Zip for zipping dependencies of workflow
RUN yum install -y bash ttf-dejavu fontconfig make python3 python3-dev python3-pip git zip
RUN pip3 install virtualenv --ignore-installed

# add Datains script and uberjar
RUN mkdir -p bin target/uberjar
COPY --from=builder /app/source/target/uberjar/datains.jar /app/target/uberjar/
COPY --from=builder /app/source/external /app/external
COPY --from=builder /app/source/bin/start /app/bin/
COPY --from=builder /app/source/bin/lein /app/bin/
COPY --from=builder /root/.lein/self-installs/leiningen-2.9.3-standalone.jar /root/.lein/self-installs/leiningen-2.9.3-standalone.jar

# Timezone
RUN yum install -y tzdata && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone

# build app-utility
RUN make -f /app/external/Makefile

# expose our default runtime port
EXPOSE 3000

# run it
ENTRYPOINT ["/app/bin/start"]