FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system --gid 1001 barofit && useradd --system --uid 1001 --gid 1001 barofit
COPY build/libs/app.jar app.jar
COPY --chmod=755 scripts/docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
EXPOSE 8080
ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
