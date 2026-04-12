FROM eclipse-temurin:25-jdk

WORKDIR /vereinskasse/server

# Kopiere das gebaute Jar
COPY *.jar server.jar
# Kopiere das Einstiegsskript
COPY entrypoint.sh .

RUN chmod +x entrypoint.sh

# Port 8443 für HTTPS öffnen
EXPOSE 8443

# Volumen deklarieren (an die Compose-Pfade angepasst!)
VOLUME ["/app/data", "/app/certs"]

ENTRYPOINT ["./entrypoint.sh"]