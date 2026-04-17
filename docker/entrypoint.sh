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
PASSWORD=${SSL_PASSWORD:-KeystorePasswort123}
CA_ALIAS=${APP_CA_ALIAS:-vereinskasse-ca}
SERVER_ALIAS="vereinskasse-server"

if [ ! -f "$KEYSTORE_PATH" ]; then
    echo "Kein SSL-Keystore gefunden. Starte PKI-Initialisierung..."
    mkdir -p "$(dirname "$KEYSTORE_PATH")"

    # --- SCHRITT A: Root-CA generieren ---
    # Manche Versionen wollen 'ca:true', manche 'ca=true'.
    # Wir nutzen hier die stabilste Variante:
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
    keytool -exportcert \
      -alias "$CA_ALIAS" \
      -keystore "$KEYSTORE_PATH" \
      -storepass "$PASSWORD" \
      -file /tmp/ca.crt


  # CA-Zertifikat ZUERST importieren (als Trust-Anchor)
   keytool -importcert \
     -alias "$CA_ALIAS" \
     -keystore "$KEYSTORE_PATH" \
     -storepass "$PASSWORD" \
     -file /tmp/ca.crt \
     -noprompt

   # 2. DANN das signierte Server-Zertifikat mit Kette importieren
   # Die Kette MUSS enthalten: Server-Cert + CA-Cert
   keytool -importcert \
     -alias "$SERVER_ALIAS" \
     -keystore "$KEYSTORE_PATH" \
     -storepass "$PASSWORD" \
     -file /tmp/server.crt \
     -noprompt

    rm /tmp/server.csr /tmp/server.crt /tmp/ca.crt
    echo "Zertifikate erfolgreich unter $KEYSTORE_PATH erstellt."
else
    echo "Vorhandener Keystore wird verwendet."
fi

# 5. Startet die Java-Anwendung
exec java -jar /vereinskasse/server/server.jar