# Etapa 1: Build do projeto com Gradle
FROM gradle:8.5-jdk21 AS builder
WORKDIR /app

# Copia apenas os arquivos essenciais para o cache funcionar
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY gradlew ./

# Faz um build leve para baixar dependências
RUN ./gradlew build -x test || return 0

# Copia o restante do código
COPY . .

# Compila o projeto e gera o JAR
RUN ./gradlew bootJar

# Etapa 2: Imagem final, menor e otimizada
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copia o JAR gerado da etapa anterior
COPY --from=builder /app/build/libs/*.jar app.jar

# Expõe a porta (ajuste conforme seu application.yml/properties)
EXPOSE 9999

# Comando para rodar a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]
