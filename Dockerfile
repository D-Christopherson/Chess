FROM gcr.io/distroless/java21-debian12:debug AS build

WORKDIR /app
COPY . /app
SHELL ["/busybox/sh", "-c"]
# Commands like `ls -a` will work without /busybox/sh being explicitly called here. But for this command specifically,
# it can't find gradlew even if I fully qualify the path.
RUN /busybox/sh gradlew bootJar

FROM gcr.io/distroless/java21-debian12

COPY --from=build /app/build/libs/chess.jar .
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "chess.jar"]