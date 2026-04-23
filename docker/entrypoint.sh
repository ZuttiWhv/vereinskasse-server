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
# NEU: Pfad für das öffentliche CA-Zertifikat für den Nginx Proxy
CA_CRT_PATH="$(dirname "$KEYSTORE_PATH")/ca.crt"

PASSWORD=${SSL_PASSWORD:-KeystorePasswort123}
CA_ALIAS=${APP_CA_ALIAS:-vereinskasse-ca}
SERVER_ALIAS="vereinskasse-server"
CERTS_DIR="$(dirname "$KEYSTORE_PATH")"

# Stelle sicher, dass die Verzeichnisse existieren
mkdir -p "$CERTS_DIR"
TMP_CERTS="/tmp/certs"
mkdir -p "$TMP_CERTS"

if [ ! -f "$KEYSTORE_PATH" ] || [ ! -f "$TRUSTSTORE_PATH" ]; then
    echo "Starte PKI-Initialisierung mit OpenSSL..."

    # --- SCHRITT A: Root-CA privaten Schlüssel generieren ---
    echo "Generiere Root-CA privaten Schlüssel (4096-bit RSA)..."
    openssl genrsa -out "$TMP_CERTS/ca-key.pem" 4096

    # --- SCHRITT B: Root-CA Zertifikat selbst signieren ---
    echo "Erstelle selbstsigniertes Root-CA Zertifikat..."
    openssl req -new -x509 -days 3650 -key "$TMP_CERTS/ca-key.pem" \
      -out "$TMP_CERTS/ca.crt" \
      -subj "/CN=Vereinskasse Root CA/O=Verein/C=DE"

    # NEU: Das CA-Zertifikat sofort in das permanente Verzeichnis kopieren
    cp "$TMP_CERTS/ca.crt" "$CA_CRT_PATH"
    echo "✓ Root-CA erstellt und exportiert nach: $CA_CRT_PATH"

    # --- SCHRITT C: Server privaten Schlüssel generieren ---
    echo "Generiere Server privaten Schlüssel (2048-bit RSA)..."
    openssl genrsa -out "$TMP_CERTS/server-key.pem" 2048

    # --- SCHRITT D: Server CSR erstellen ---
    echo "Erstelle Server Certificate Signing Request (CSR)..."
    openssl req -new -key "$TMP_CERTS/server-key.pem" \
      -out "$TMP_CERTS/server.csr" \
      -subj "/CN=localhost/OU=Server/O=Vereinskasse/C=DE"

    # --- SCHRITT E: Server-Zertifikat mit CA signieren ---
    echo "Signiere Server-Zertifikat mit Root-CA..."
    cat > "$TMP_CERTS/server.conf" <<EOF
[ v3_req ]
basicConstraints = CA:FALSE
keyUsage = digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth
subjectAltName = DNS:localhost,DNS:127.0.0.1,DNS:vereinskasse-server,IP:127.0.0.1
EOF

    openssl x509 -req -days 1825 \
      -in "$TMP_CERTS/server.csr" \
      -CA "$TMP_CERTS/ca.crt" \
      -CAkey "$TMP_CERTS/ca-key.pem" \
      -CAcreateserial \
      -out "$TMP_CERTS/server.crt" \
      -extensions v3_req \
      -extfile "$TMP_CERTS/server.conf"

    # --- SCHRITT F: Zertifikatskette kombinieren ---
    cat "$TMP_CERTS/server.crt" "$TMP_CERTS/ca.crt" > "$TMP_CERTS/server-chain.crt"

    # --- SCHRITT G: Keystore (PKCS12) erstellen ---
    echo "Erstelle PKCS12 Keystore..."
    openssl pkcs12 -export \
      -in "$TMP_CERTS/server-chain.crt" \
      -inkey "$TMP_CERTS/server-key.pem" \
      -out "$KEYSTORE_PATH" \
      -name "$SERVER_ALIAS" \
      -password "pass:$PASSWORD"

    # CA-Key für Signatur-Dienst importieren
    openssl pkcs12 -export \
      -in "$TMP_CERTS/ca.crt" \
      -inkey "$TMP_CERTS/ca-key.pem" \
      -out "$TMP_CERTS/ca-bundle.p12" \
      -name "$CA_ALIAS" \
      -password "pass:$PASSWORD"

    keytool -importkeystore \
      -srckeystore "$TMP_CERTS/ca-bundle.p12" \
      -srcstoretype PKCS12 \
      -srcstorepass "$PASSWORD" \
      -destkeystore "$KEYSTORE_PATH" \
      -deststorepass "$PASSWORD" \
      -noprompt

    # --- SCHRITT H: Trust-Store (PKCS12) erstellen ---
    echo "Erstelle PKCS12 Trust-Store..."
    keytool -import \
      -alias "$CA_ALIAS" \
      -file "$TMP_CERTS/ca.crt" \
      -keystore "$TRUSTSTORE_PATH" \
      -storepass "$PASSWORD" \
      -noprompt \
      -storetype PKCS12 2>/dev/null

    # Aufräumen
    rm -rf "$TMP_CERTS"
    echo "✓✓✓ PKI Initialisierung abgeschlossen."
else
    echo "✓ Vorhandene Keystores werden verwendet."

    # NEU: Falls der Keystore da ist, aber die ca.crt fehlt (z.B. Volume gelöscht),
    # extrahieren wir sie einfach wieder aus dem Truststore.
    if [ ! -f "$CA_CRT_PATH" ]; then
        echo "Extrahiere fehlende ca.crt aus dem Truststore..."
        keytool -exportcert -alias "$CA_ALIAS" \
          -keystore "$TRUSTSTORE_PATH" \
          -storepass "$PASSWORD" \
          -file "$CA_CRT_PATH" -rfc
    fi
fi

# --- Finale Verifizierung ---
[ -f "$CA_CRT_PATH" ] && echo "✓ CA-Zertifikat verfügbar für Proxy: $CA_CRT_PATH"

echo "Starte Spring Boot Anwendung..."
exec java -jar /vereinskasse/server/server.jar