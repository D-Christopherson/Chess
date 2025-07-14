FROM gcr.io/distroless/java21-debian12:debug AS build

WORKDIR /app
COPY . /app
SHELL ["/busybox/sh", "-c"]
# Commands like `ls -a` will work without /busybox/sh being explicitly called here. But for this command specifically,
# it can't find gradlew even if I fully qualify the path (like `RUN ./gradlew bootJar` or `RUN /app/gradlew bootJar`).
RUN /busybox/sh gradlew bootJar

FROM gcr.io/distroless/java21-debian12

COPY --from=build /app/build/libs/chess.jar .
EXPOSE 8080

# Nothing else is running in the container on ECS, so we might as well pre-allocate the whole heap to save time later.
# The memory utilization from the OS is already an unreliable metric anyway since the JVM rarely willingly gives up
# memory that it has allocated.
ENTRYPOINT ["java", "-jar", "chess.jar", "-Xms400m", "-Xmx400m"]