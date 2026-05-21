# ============================================================================
# 멀티 스테이지 빌드 — 빌드 환경과 런타임 분리로 이미지 크기 ↓
# ============================================================================

# ----- Build stage -----
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src
# 의존성 캐시 활용 + offline 모드 없이 빌드
RUN chmod +x gradlew && ./gradlew --no-daemon -x test bootJar

# ----- Runtime stage -----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# 비root 사용자로 실행 (보안)
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/build/libs/simulator.jar /app/simulator.jar
USER app
EXPOSE 8080

# JVM 옵션 (PRD §13.2 EC2 t3.micro 1GB RAM 대응)
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"
ENV SPRING_PROFILES_ACTIVE=local

ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/simulator.jar"]
