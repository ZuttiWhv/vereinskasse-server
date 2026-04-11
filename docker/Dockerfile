FROM eclipse-temurin:25.0.2_10-jdk

WORKDIR /vereinskasse/server

# Kopiere das gebaute Jar
COPY *.jar server.jar
# Kopiere das Einstiegsskript
COPY entrypoint.sh .

RUN chmod +x entrypoint.sh

# Port 8443 für HTTPS öffnen
EXPOSE 8443

# Volumen für Zertifikate (damit sie beim Container-Update erhalten bleiben)
VOLUME /vereinskasse/server/data

ENTRYPOINT ["./entrypoint.sh"]
