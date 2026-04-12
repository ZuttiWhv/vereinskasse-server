#!/bin/sh

# 1. ABSOLUTE PFADE (Container-Sicht)
CONFIG_DIR="/app/data/config"
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
        JWT_SECRET=$(head -c 64 /dev/urandom | base64 | tr -d '\n')
        echo "$JWT_SECRET" > "$JWT_KEY_FILE"
        echo "Neuer Schlüssel wurde in $JWT_KEY_FILE gespeichert."
    fi
fi

# Exportiere die Variable, damit Spring Boot sie sieht
export JWT_SECRET

# 2. PFAD ZUM KEYSTORE: Korrekte Syntax (:-) und absoluter Mount-Pfad
# Aus deiner Compose-Datei: Volume ./certs-backend liegt auf /app/certs
KEYSTORE_PATH=${SSL_KEYSTORE_PATH:-/app/certs/keystore.p12}

# 3. SICHERES DEFAULT PASSWORT (ohne Shell-Sonderzeichen)
PASSWORD=${SSL_PASSWORD:-KeystorePasswort123}

if [ ! -f "$KEYSTORE_PATH" ]; then
    echo "Kein SSL-Zertifikat gefunden. Generiere neues Zertifikat..."

    # 4. BUGFIX: Erstelle nur das Verzeichnis (z.B. /app/certs), nicht die p12-Datei als Verzeichnis!
    mkdir -p "$(dirname "$KEYSTORE_PATH")"

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

# 5. Startet die Java-Anwendung
# WICHTIG: Dieser Pfad muss exakt dem Pfad in deinem Dockerfile entsprechen!
exec java -jar /vereinskasse/server/server.jar