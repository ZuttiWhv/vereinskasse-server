#!/bin/sh

# Pfade auf dem persistenten Volume
CONFIG_DIR="/vereinskasse/server/data/config"
JWT_KEY_FILE="$CONFIG_DIR/jwt.secret"
mkdir -p "$CONFIG_DIR"

# --- JWT SECRET LOGIK ---
if [ -z "$JWT_SECRET" ]; then
    if [ -f "$JWT_KEY_FILE" ]; then
        echo "Lade vorhandenes JWT Secret aus Datei..."
        JWT_SECRET=$(cat "$JWT_KEY_FILE")
    else
        echo "Kein JWT_SECRET gesetzt. Generiere neuen sicheren Schlüssel..."
        # Generiert 64 zufällige Bytes und kodiert sie als Base64 (sicher für JJWT)
        # Wir nutzen /dev/urandom für echte Zufallszahlen
        JWT_SECRET=$(head -c 64 /dev/urandom | base64 | tr -d '\n')
        echo "$JWT_SECRET" > "$JWT_KEY_FILE"
        echo "Neuer Schlüssel wurde in $JWT_KEY_FILE gespeichert."
    fi
fi

# Exportiere die Variable, damit Spring Boot sie sieht
export JWT_SECRET

# Pfad zum Keystore
KEYSTORE_PATH="/vereinskasse/server/data/certs/keystore.p12"
PASSWORD=${SSL_PASSWORD:-PLEASE123SET456inEnv$$$$$} # Nutzt Umgebungsvariable oder Default

if [ ! -f "$KEYSTORE_PATH" ]; then
    echo "Kein SSL-Zertifikat gefunden. Generiere neues Zertifikat..."
    mkdir -p /vereinskasse/server/data/certs

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
exec java -jar /vereinskasse/server/server.jar