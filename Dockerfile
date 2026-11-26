FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src
ARG GITHUB_ACTOR
RUN --mount=type=cache,target=/root/.gradle,sharing=locked \
    --mount=type=secret,id=github_token,required=true \
    GITHUB_TOKEN="$(cat /run/secrets/github_token)" \
    GITHUB_ACTOR="$GITHUB_ACTOR" \
    ./gradlew --no-daemon bootJar && \
    cp build/libs/app.jar /workspace/service.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system --gid 1001 barofit && useradd --system --uid 1001 --gid 1001 barofit
COPY --from=build /workspace/service.jar app.jar
COPY --chmod=755 scripts/docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
EXPOSE 8080
ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
