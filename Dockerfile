# 1. AŞAMA: Derleme (Build)
# Projeyi derlemek için içinde Maven ve Java 21 olan bir sanal makine kuruyoruz
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# 2. AŞAMA: Çalıştırma (Run)
# Sadece çalışmaya yetecek kadar hafif, yepyeni bir Java 21 makinesi kuruyoruz
FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app
# İlk aşamada derlenen .jar dosyasını bu yeni makineye kopyalıyoruz
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]