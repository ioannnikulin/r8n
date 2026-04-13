FROM eclipse-temurin:21-jre
WORKDIR /app
COPY app.jar /app/app.jar
COPY startup.sh /app/startup.sh
RUN chmod +x /app/startup.sh
ENTRYPOINT ["/app/startup.sh"]
CMD ["java", "-jar", "/app/app.jar"]