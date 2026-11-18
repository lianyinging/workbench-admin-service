# OPC 工作台管理后端 — 多阶段构建
# 运行：docker run -d -p 9080:9080 -e SPRING_PROFILES_ACTIVE=prod <image>

FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /build

COPY pom.xml ./
COPY ruoyi-admin ./ruoyi-admin
COPY ruoyi-common ./ruoyi-common
COPY ruoyi-modules ./ruoyi-modules
COPY ruoyi-extend ./ruoyi-extend

# 镜像默认打 prod；本地联调可 --build-arg MAVEN_PROFILE=dev
ARG MAVEN_PROFILE=prod
RUN mvn -B clean package -Dmaven.test.skip=true -P${MAVEN_PROFILE}

FROM bellsoft/liberica-openjdk-rocky:17.0.16-cds
LABEL maintainer="opc-workbench" \
      app="cm-opc-workbench-admin-service"

RUN mkdir -p /ruoyi/server/logs /ruoyi/server/temp

WORKDIR /ruoyi/server

ENV SERVER_PORT=9080 \
    SNAIL_PORT=29080 \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    TZ=Asia/Shanghai \
    JAVA_OPTS="" \
    SPRING_PROFILES_ACTIVE=prod

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY --from=builder /build/ruoyi-admin/target/ruoyi-admin.jar ./app.jar

EXPOSE ${SERVER_PORT} ${SNAIL_PORT}

SHELL ["/bin/bash", "-c"]

ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom \
           -Dserver.port=${SERVER_PORT} \
           -Dsnail-job.port=${SNAIL_PORT} \
           -XX:+HeapDumpOnOutOfMemoryError -XX:+UseZGC ${JAVA_OPTS} \
           -jar app.jar
