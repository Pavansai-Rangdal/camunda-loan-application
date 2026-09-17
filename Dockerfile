FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN apt-get update && \
    apt-get install -y maven && \
    mvn clean package -DskipTests && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

EXPOSE 8090

CMD ["sh", "-c", "java -jar target/loan-approval-backend-0.1.0.jar --server.port=${PORT:-8090}"]