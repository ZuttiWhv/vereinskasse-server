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

    if [ $? -ne 0 ]; then
        echo "ERROR: CA-Schlüssel-Generierung fehlgeschlagen"
        exit 1
    fi

    # --- SCHRITT B: Root-CA Zertifikat selbst signieren ---
    echo "Erstelle selbstsigniertes Root-CA Zertifikat..."
    openssl req -new -x509 -days 3650 -key "$TMP_CERTS/ca-key.pem" \
      -out "$TMP_CERTS/ca.crt" \
      -subj "/CN=Vereinskasse Root CA/O=Verein/C=DE"

    if [ $? -ne 0 ]; then
        echo "ERROR: CA-Zertifikat-Erstellung fehlgeschlagen"
        exit 1
    fi

    echo "✓ Root-CA erstellt"

    # --- SCHRITT C: Server privaten Schlüssel generieren ---
    echo "Generiere Server privaten Schlüssel (2048-bit RSA)..."
    openssl genrsa -out "$TMP_CERTS/server-key.pem" 2048

    if [ $? -ne 0 ]; then
        echo "ERROR: Server-Schlüssel-Generierung fehlgeschlagen"
        exit 1
    fi

    # --- SCHRITT D: Server CSR erstellen ---
    echo "Erstelle Server Certificate Signing Request (CSR)..."
    openssl req -new -key "$TMP_CERTS/server-key.pem" \
      -out "$TMP_CERTS/server.csr" \
      -subj "/CN=localhost/OU=Server/O=Vereinskasse/C=DE"

    if [ $? -ne 0 ]; then
        echo "ERROR: Server-CSR-Erstellung fehlgeschlagen"
        exit 1
    fi

    # --- SCHRITT E: Server-Zertifikat mit CA signieren ---
    echo "Signiere Server-Zertifikat mit Root-CA..."

    # Erstelle eine Konfigurationsdatei für die x509-Erweiterungen
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

    if [ $? -ne 0 ]; then
        echo "ERROR: Server-Zertifikat-Signierung fehlgeschlagen"
        exit 1
    fi

    echo "✓ Server-Zertifikat signiert"

    # --- SCHRITT F: Zertifikatskette kombinieren ---
    echo "Erstelle Zertifikatskette..."
    cat "$TMP_CERTS/server.crt" "$TMP_CERTS/ca.crt" > "$TMP_CERTS/server-chain.crt"

    # --- SCHRITT G: Keystore (PKCS12) für Server erstellen ---
# --- SCHRITT G: Keystore (PKCS12) für Server UND CA-Signierung erstellen ---
    echo "Erstelle PKCS12 Keystore..."

    # 1. Zuerst den Server-Eintrag (wie bisher)
    openssl pkcs12 -export \
      -in "$TMP_CERTS/server-chain.crt" \
      -inkey "$TMP_CERTS/server-key.pem" \
      -out "$KEYSTORE_PATH" \
      -name "$SERVER_ALIAS" \
      -password "pass:$PASSWORD"

    # 2. JETZT WICHTIG: Den CA-Key ebenfalls in denselben Keystore importieren
    # Wir nutzen ein temporäres Bundle, um es mit keytool zu mergen oder
    # fügen es direkt hinzu. Am einfachsten ist es, beide in eine Datei zu packen:

    echo "Füge CA-Privatschlüssel zum Keystore hinzu..."
    openssl pkcs12 -export \
      -in "$TMP_CERTS/ca.crt" \
      -inkey "$TMP_CERTS/ca-key.pem" \
      -out "$TMP_CERTS/ca-bundle.p12" \
      -name "$CA_ALIAS" \
      -password "pass:$PASSWORD"

    # Jetzt mergen wir das CA-Bundle in den Haupt-Keystore
    keytool -importkeystore \
      -srckeystore "$TMP_CERTS/ca-bundle.p12" \
      -srcstoretype PKCS12 \
      -srcstorepass "$PASSWORD" \
      -destkeystore "$KEYSTORE_PATH" \
      -deststorepass "$PASSWORD" \
      -noprompt

    # --- SCHRITT H: Trust-Store (PKCS12) für mTLS erstellen ---
    # WICHTIG: -nokey ist nicht bei allen OpenSSL-Versionen vorhanden
    # Fallback: Wir erstellen ein leeres Keystore und importieren dann die CA
    echo "Erstelle PKCS12 Trust-Store mit CA-Zertifikat..."

    # Versuch 1: Mit keytool (Java-Standard, immer vorhanden)
    keytool -import \
      -alias "$CA_ALIAS" \
      -file "$TMP_CERTS/ca.crt" \
      -keystore "$TRUSTSTORE_PATH" \
      -storepass "$PASSWORD" \
      -noprompt \
      -storetype PKCS12 2>/dev/null

    if [ $? -ne 0 ]; then
        echo "Fallback: Verwende OpenSSL für Trust-Store..."

        # Versuch 2: Erstelle ein leeres PKCS12 mit einem Dummy-Zertifikat
        # und ersetze es dann durch die CA
        openssl pkcs12 -export \
          -in "$TMP_CERTS/ca.crt" \
          -out "$TRUSTSTORE_PATH" \
          -name "$CA_ALIAS" \
          -password "pass:$PASSWORD" \
          -noout 2>/dev/null

        if [ $? -ne 0 ]; then
            echo "Fallback 2: Direkter OpenSSL-Export..."
            # Versuch 3: Alternative OpenSSL-Syntax (für ältere Versionen)
            cat "$TMP_CERTS/ca.crt" | \
            openssl pkcs12 -export \
              -name "$CA_ALIAS" \
              -password "pass:$PASSWORD" \
              -out "$TRUSTSTORE_PATH" \
              -nokey -nocerts -in /dev/stdin
        fi
    fi

    if [ ! -f "$TRUSTSTORE_PATH" ] || [ ! -s "$TRUSTSTORE_PATH" ]; then
        echo "ERROR: PKCS12-Trust-Store-Erstellung fehlgeschlagen"
        exit 1
    fi

    echo "✓ Trust-Store erstellt: $TRUSTSTORE_PATH"

    # --- Aufräumen ---
    rm -rf "$TMP_CERTS"

    echo ""
    echo "✓✓✓ Zertifikate erfolgreich erstellt:"
    echo "  - Keystore (Server): $KEYSTORE_PATH"
    echo "  - Trust-Store (mTLS): $TRUSTSTORE_PATH"
    echo "  - Passwort: $PASSWORD"
    echo ""

else
    echo "✓ Vorhandene Keystores werden verwendet."
fi

# --- Verifizierung ---
if [ ! -f "$KEYSTORE_PATH" ]; then
    echo "ERROR: Keystore existiert nicht: $KEYSTORE_PATH"
    exit 1
fi

if [ ! -f "$TRUSTSTORE_PATH" ]; then
    echo "ERROR: Trust-Store existiert nicht: $TRUSTSTORE_PATH"
    exit 1
fi

echo "Verifiziere Keystores..."
keytool -list -keystore "$KEYSTORE_PATH" -storepass "$PASSWORD" -v 2>/dev/null | head -20

echo ""
echo "Starte Spring Boot Anwendung..."
exec java -jar /vereinskasse/server/server.jar