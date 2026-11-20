# syntax=docker/dockerfile:1
# Production image for the Spring Boot API. Three stages:
#   1. build   : full JDK + Maven wrapper compile the jar (tests run in CI, not here)
#   2. extract : split the fat jar into layers (dependencies change rarely, our code often)
#   3. runtime : small JRE-only Alpine image, running as a non-root user
# Layering means a code-only change re-uploads a few hundred KB instead of ~70 MB.

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /src
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS extract
WORKDIR /x
COPY --from=build /src/target/campusoa.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination layers

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=extract /x/layers/dependencies/ ./
COPY --from=extract /x/layers/spring-boot-loader/ ./
COPY --from=extract /x/layers/snapshot-dependencies/ ./
COPY --from=extract /x/layers/application/ ./

# Render's free instance has 512 MB RAM: cap the heap to 70% of the container, use the
# low-overhead Serial GC, and smaller thread stacks.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k -XX:+ExitOnOutOfMemoryError"
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
