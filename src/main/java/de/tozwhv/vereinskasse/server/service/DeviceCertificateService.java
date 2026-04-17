package de.tozwhv.vereinskasse.server.service;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Date;

@Service
public class DeviceCertificateService {

    private final String keystorePath;
    private final String trustStorePath;
    private final char[] keystorePassword;
    private final String caAlias;
    private final String keystoreType;

    public DeviceCertificateService(
            @Value("${server.ssl.key-store}") String keystorePath,
            @Value("${server.ssl.trust-store}") String trustStorePath,
            @Value("${server.ssl.key-store-password}") String keystorePassword,
            @Value("${app.ca-alias:vereinskasse-ca}") String caAlias,
            @Value("${server.ssl.key-store-type}") String keystoreType) {

        // Spring reicht den Pfad oft mit "file:" Präfix rein -> für Java File API entfernen
        this.keystorePath = keystorePath.replace("file:", "");
        this.trustStorePath = trustStorePath.replace("file:", "");
        this.keystorePassword = keystorePassword.toCharArray();
        this.caAlias = caAlias;
        this.keystoreType = keystoreType;
    }

    public byte[] createDeviceCertificate(String deviceName) throws Exception {
        // 1. Trust-Store laden, um an die Root-CA zu kommen
        X509Certificate caCert = loadCACertificateFromTrustStore();

        // 2. Keystore laden, um an den CA-Private-Key zu kommen
        KeyStore.PrivateKeyEntry caEntry = loadCAPrivateKeyFromKeystore();

        // 3. Neues Schlüsselpaar für das Terminal (Raspberry Pi) erzeugen
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair deviceKeyPair = keyGen.generateKeyPair();

        // 4. Zertifikat für das Terminal konfigurieren
        X500Name issuer = X500Name.getInstance(caCert.getSubjectX500Principal().getEncoded());
        X500Name subject = new X500Name("CN=" + deviceName + ", O=Vereinskasse, C=DE");

        // Gültigkeit: Ab jetzt für 5 Jahre
        Date notBefore = new Date();
        Date notAfter = new Date(System.currentTimeMillis() + (5L * 365 * 24 * 60 * 60 * 1000));

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer,
                BigInteger.valueOf(System.currentTimeMillis()),
                notBefore,
                notAfter,
                subject,
                deviceKeyPair.getPublic());

        // --- KRITISCH: Die Chain-Validierung (Extensions) ---
        JcaX509ExtensionUtils extensionUtils = new JcaX509ExtensionUtils();

        // Verknüpft das Zertifikat eindeutig mit dem Key der CA (Authority Key Identifier)
        certBuilder.addExtension(Extension.authorityKeyIdentifier, false,
                extensionUtils.createAuthorityKeyIdentifier(caCert));

        // Erstellt eine ID für den Schlüssel des Geräts selbst (Subject Key Identifier)
        certBuilder.addExtension(Extension.subjectKeyIdentifier, false,
                extensionUtils.createSubjectKeyIdentifier(deviceKeyPair.getPublic()));

        // Standard-Constraints: Kein CA, nur digitale Signatur & Verschlüsselung
        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        certBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));

        // 5. Zertifikat signieren mit dem CA-Private-Key
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").build(caEntry.getPrivateKey());
        X509Certificate deviceCert = new JcaX509CertificateConverter()
                .setProvider(new BouncyCastleProvider())
                .getCertificate(certBuilder.build(signer));

        // 6. Validierung (optional, aber sicher ist sicher)
        deviceCert.verify(caCert.getPublicKey());

        // 7. Alles in ein neues P12-Bundle für das Gerät packen
        KeyStore deviceP12 = KeyStore.getInstance(keystoreType);
        deviceP12.load(null, null);

        // Erstelle ein Array vom Typ Certificate (Basisklasse)
        java.security.cert.Certificate[] chain = new java.security.cert.Certificate[2];
        chain[0] = deviceCert;
        chain[1] = caCert;

        // Setze den Key und die Kette
        deviceP12.setKeyEntry(deviceName, deviceKeyPair.getPrivate(), keystorePassword, chain);

        // Als Byte-Array exportieren
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        deviceP12.store(bos, keystorePassword);

        return bos.toByteArray();
    }

    /**
     * Lädt das CA-Zertifikat aus dem Trust-Store.
     * Der Trust-Store enthält nur öffentliche Zertifikate (keine privaten Keys).
     */
    private X509Certificate loadCACertificateFromTrustStore() throws Exception {
        KeyStore trustStore = KeyStore.getInstance(keystoreType);
        try (FileInputStream fis = new FileInputStream(trustStorePath)) {
            trustStore.load(fis, keystorePassword);
        }

        X509Certificate caCert = (X509Certificate) trustStore.getCertificate(caAlias);

        if (caCert == null) {
            throw new IllegalStateException(
                    "Root-CA mit Alias '" + caAlias + "' nicht im Trust-Store gefunden! " +
                            "Trust-Store: " + trustStorePath);
        }

        return caCert;
    }

    /**
     * Lädt den privaten Schlüssel der CA aus dem Keystore.
     * Der Keystore enthält den Server-Key und die CA für die Signierung von Gerätezertifikaten.
     */
    private KeyStore.PrivateKeyEntry loadCAPrivateKeyFromKeystore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(keystoreType);
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            keyStore.load(fis, keystorePassword);
        }

        KeyStore.PrivateKeyEntry caEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(
                caAlias, new KeyStore.PasswordProtection(keystorePassword));

        if (caEntry == null) {
            throw new IllegalStateException(
                    "Root-CA mit Alias '" + caAlias + "' nicht im Keystore gefunden! " +
                            "Keystore: " + keystorePath + ". " +
                            "Stelle sicher, dass die CA in den Keystore importiert wurde.");
        }

        return caEntry;
    }

    /**
     * Extrahiert das öffentliche Root-CA-Zertifikat aus dem Trust-Store.
     */
    public byte[] getPublicCACertificate() throws Exception {
        X509Certificate caCert = loadCACertificateFromTrustStore();
        return caCert.getEncoded();
    }
}