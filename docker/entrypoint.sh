#!/bin/sh

# Pfad zum Keystore
KEYSTORE_PATH="/vereinskasse/server/certs/keystore.p12"
PASSWORD=${SSL_PASSWORD:-PLEASE123SET456inEnv$$$$$} # Nutzt Umgebungsvariable oder Default

if [ ! -f "$KEYSTORE_PATH" ]; then
    echo "Kein SSL-Zertifikat gefunden. Generiere neues Zertifikat..."
    mkdir -p /vereinskasse/server/certs

    # Generiert einen selbstsignierten Keystore
    keytool -genkeypair \
      -alias vereinskasse \
      -keyalg RSA \
      -keysize 4096 \
      -validity 3650 \
      -keystore "$KEYSTORE_PATH" \
      -storepass "$PASSWORD" \
      -keypass "$PASSWORD" \
      -dname "CN=vereinskasse.local, OU=Verein, O=Vereinskasse, L=Heim, C=DE" \
      -storetype PKCS12

    echo "Zertifikat wurde unter $KEYSTORE_PATH erstellt."
else
    echo "Vorhandenes Zertifikat wird verwendet."
fi

# Startet die Java-Anwendung
exec java -jar /vereinskasse/server/app.jar