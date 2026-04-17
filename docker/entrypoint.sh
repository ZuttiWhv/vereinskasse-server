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
        JWT_SECRET=$(head -c 64 /dev/urandom | base64 | tr -d '\n')
        echo "$JWT_SECRET" > "$JWT_KEY_FILE"
        echo "Neuer Schlüssel wurde in $JWT_KEY_FILE gespeichert."
    fi
fi

export JWT_SECRET

# 2. SSL PFADE & ALIASE
KEYSTORE_PATH=${SSL_KEYSTORE_PATH:-/app/certs/keystore.p12}
TRUSTSTORE_PATH=${SSL_TRUSTSTORE_PATH:-/app/certs/truststore.p12}
PASSWORD=${SSL_PASSWORD:-KeystorePasswort123}
CA_ALIAS=${APP_CA_ALIAS:-vereinskasse-ca}
SERVER_ALIAS="vereinskasse-server"

# Stelle sicher, dass die Verzeichnisse existieren
mkdir -p "$(dirname "$KEYSTORE_PATH")"
mkdir -p "$(dirname "$TRUSTSTORE_PATH")"

if [ ! -f "$KEYSTORE_PATH" ] || [ ! -f "$TRUSTSTORE_PATH" ]; then
    echo "Starte PKI-Initialisierung..."

    # --- SCHRITT A: Root-CA generieren ---
    echo "Generiere Root-CA..."
    keytool -genkeypair \
      -alias "$CA_ALIAS" \
      -keyalg RSA \
      -keysize 4096 \
      -ext "bc=ca:true" \
      -ext "ku=keyCertSign,cRLSign" \
      -validity 3650 \
      -keystore "$KEYSTORE_PATH" \
      -storepass "$PASSWORD" \
      -keypass "$PASSWORD" \
      -dname "CN=Vereinskasse Root CA, O=Verein, C=DE" \
      -storetype PKCS12

    # --- SCHRITT B: Server-Zertifikat generieren ---
    echo "Generiere Server-Zertifikat..."
    keytool -genkeypair \
      -alias "$SERVER_ALIAS" \
      -keyalg RSA \
      -keysize 2048 \
      -validity 1825 \
      -keystore "$KEYSTORE_PATH" \
      -storepass "$PASSWORD" \
      -keypass "$PASSWORD" \
      -dname "CN=localhost, OU=Server, O=Vereinskasse, C=DE" \
      -storetype PKCS12

    # --- SCHRITT C: Signierungsprozess ---
    echo "Signiere Server-Zertifikat mit Root-CA..."

    # 1. CSR erstellen
    keytool -certreq \
      -alias "$SERVER_ALIAS" \
      -keystore "$KEYSTORE_PATH" \
      -storepass "$PASSWORD" \
      -file /tmp/server.csr

    # 2. CSR signieren
    keytool -gencert \
      -alias "$CA_ALIAS" \
      -keystore "$KEYSTORE_PATH" \
      -storepass "$PASSWORD" \
      -infile /tmp/server.csr \
      -outfile /tmp/server.crt \
      -validity 1825 \
      -ext "ku=digitalSignature,keyEncipherment" \
      -ext "eku=serverAuth"

    # 3. CA-Zertifikat exportieren
    echo "Exportiere CA-Zertifikat..."
    keytool -exportcert \
      -alias "$CA_ALIAS" \
      -keystore "$KEYSTORE_PATH" \
      -storepass "$PASSWORD" \
      -rfc \
      -file /tmp/ca.crt

    # 4. Server-Zertifikat + CA-Kette kombinieren
    echo "Erstelle Zertifikatskette..."
    cat /tmp/server.crt /tmp/ca.crt > /tmp/server-chain.crt

    # 5. Server-Zertifikat mit Kette importieren (NACH Signierung!)
    # WICHTIG: Zuerst die CA importieren
    echo "Importiere CA in Keystore..."
    keytool -importcert \
      -alias "${CA_ALIAS}-import" \
      -keystore "$KEYSTORE_PATH" \
      -storepass "$PASSWORD" \
      -file /tmp/ca.crt \
      -noprompt

    # Dann das signierte Server-Zertifikat mit Kette
    echo "Importiere signiertes Server-Zertifikat..."
    keytool -importcert \
      -alias "$SERVER_ALIAS" \
      -keystore "$KEYSTORE_PATH" \
      -storepass "$PASSWORD" \
      -file /tmp/server-chain.crt \
      -noprompt

    # --- SCHRITT D: SEPARATEN TRUST-STORE ERSTELLEN ---
    echo "Erstelle separaten Trust-Store..."

    # Trust-Store mit CA-Zertifikat
    keytool -import \
      -alias "$CA_ALIAS" \
      -file /tmp/ca.crt \
      -keystore "$TRUSTSTORE_PATH" \
      -storepass "$PASSWORD" \
      -noprompt \
      -storetype PKCS12

    # Aufräumen
    rm -f /tmp/server.csr /tmp/server.crt /tmp/server-chain.crt /tmp/ca.crt

    echo "✓ Zertifikate erfolgreich erstellt:"
    echo "  - Keystore: $KEYSTORE_PATH"
    echo "  - Truststore: $TRUSTSTORE_PATH"
else
    echo "✓ Vorhandene Keystores werden verwendet."
fi

# Überprüfe, ob Trust-Store existiert und nicht leer ist
if [ ! -f "$TRUSTSTORE_PATH" ]; then
    echo "ERROR: Trust-Store existiert nicht: $TRUSTSTORE_PATH"
    exit 1
fi

echo "Starte Spring Boot Anwendung..."
exec java -jar /vereinskasse/server/server.jar