# ─────────────────────────────────────────────────────────────
# Stage 1: Build
# Usa un'immagine con Maven e JDK completo solo per compilare.
# L'artefatto prodotto viene copiato nello stage 2.
# ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copia prima solo il pom.xml e scarica le dipendenze.
# Questo layer viene cachato finché il pom.xml non cambia:
# rebuild solo del codice senza riscaricare l'internet.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -q

# Copia il sorgente e compila
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn package -DskipTests -q

# Estrai i layer del JAR con Spring Boot Layertools.
# Produce layer separati per dependencies, spring-boot-loader,
# snapshot-dependencies e application — ottimizza la cache Docker
# per rebuild che cambiano solo il codice applicativo.
RUN java -Djarmode=layertools \
    -jar target/order-service-*.jar extract \
    --destination target/extracted

# ─────────────────────────────────────────────────────────────
# Stage 2: Runtime
# Immagine minimale JRE — niente JDK, niente Maven, niente shell
# tools non necessari. Riduce la superficie di attacco.
# ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Utente non-root obbligatorio in produzione
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copia i layer nell'ordine dalla meno alla più frequentemente
# modificata — massimizza il riuso della cache Docker tra deploy.
COPY --from=builder --chown=appuser:appgroup \
    /build/target/extracted/dependencies/ ./

COPY --from=builder --chown=appuser:appgroup \
    /build/target/extracted/spring-boot-loader/ ./

COPY --from=builder --chown=appuser:appgroup \
    /build/target/extracted/snapshot-dependencies/ ./

COPY --from=builder --chown=appuser:appgroup \
    /build/target/extracted/application/ ./

USER appuser

# Porta applicativa
EXPOSE 8080

# Porta Actuator (opzionale — utile per health check separato)
EXPOSE 8081

# JVM flags ottimizzati per container:
# -XX:+UseContainerSupport      legge i cgroup limits (CPU/memoria)
# -XX:MaxRAMPercentage=75.0     usa max il 75% della RAM del container
# -XX:+ExitOnOutOfMemoryError   crasha invece di degradare silenziosamente
# -Djava.security.egd           entropia più veloce per UUID/token
ENV JAVA_OPTS="\
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+ExitOnOutOfMemoryError \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=prod"

# Profilo di default sovrascrivibile a runtime
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]