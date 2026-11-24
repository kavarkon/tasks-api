FROM eclipse-temurin:21-jre-jammy
COPY --chmod=0444 build/libs/app.jar /app.jar
USER 65534:65534
ENTRYPOINT ["java","-jar","/app.jar"]
