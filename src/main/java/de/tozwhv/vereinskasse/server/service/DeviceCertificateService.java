package de.tozwhv.vereinskasse.server.service;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
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
    private final char[] keystorePassword;
    private final String caAlias;
    private final String keystoreType;

    public DeviceCertificateService(
            @Value("${server.ssl.key-store}") String keystorePath,
            @Value("${server.ssl.key-store-password}") String keystorePassword,
            @Value("${app.ca-alias:vereinskasse-ca}") String caAlias,
            @Value("${server.ssl.key-store-type") String keystoreType) {

        // Spring reicht den Pfad oft mit "file:" Präfix rein -> für Java File API entfernen
        this.keystorePath = keystorePath.replace("file:", "");
        this.keystorePassword = keystorePassword.toCharArray();
        this.caAlias = caAlias;
        this.keystoreType = keystoreType;
    }

    public byte[] createDeviceCertificate(String deviceName) throws Exception {
        // 1. Keystore laden
        KeyStore keyStore = KeyStore.getInstance(keystoreType);
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            keyStore.load(fis, keystorePassword);
        }

        // 2. Den CA-Eintrag holen (den dein Shell-Skript erstellt hat)
        KeyStore.PrivateKeyEntry caEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(
                caAlias, new KeyStore.PasswordProtection(keystorePassword));

        if (caEntry == null) {
            throw new IllegalStateException("Root-CA mit Alias " + caAlias + " nicht im Keystore gefunden!");
        }

        // 3. Neues Schlüsselpaar für das Terminal (Raspberry Pi) erzeugen
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair deviceKeyPair = keyGen.generateKeyPair();

        // 4. Zertifikat für das Terminal erstellen und mit CA signieren
        X509Certificate caCert = (X509Certificate) caEntry.getCertificate();
        X500Name issuer = new X500Name(caCert.getSubjectX500Principal().getName());
        X500Name subject = new X500Name("CN=" + deviceName + ", O=Vereinskasse, C=DE");

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer,
                BigInteger.valueOf(System.currentTimeMillis()),
                new Date(),
                new Date(System.currentTimeMillis() + 157680000000L), // 5 Jahre
                subject,
                deviceKeyPair.getPublic());

        // Terminals sind keine CAs
        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        certBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));

        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").build(caEntry.getPrivateKey());
        X509Certificate deviceCert = new JcaX509CertificateConverter().getCertificate(certBuilder.build(signer));

        // 5. Alles in ein P12-Bundle für den Browser-Download packen
        KeyStore deviceP12 = KeyStore.getInstance(keystoreType);
        deviceP12.load(null, null);

        // Die Kette besteht aus: [Terminal-Zertifikat, CA-Zertifikat]
        deviceP12.setKeyEntry(deviceName, deviceKeyPair.getPrivate(), keystorePassword,
                new X509Certificate[]{deviceCert, caCert});

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        deviceP12.store(bos, keystorePassword);
        return bos.toByteArray();
    }


    /**
     * Extrahiert das öffentliche Root-CA-Zertifikat aus dem Keystore.
     */
    public byte[] getPublicCACertificate() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(keystoreType);
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            keyStore.load(fis, keystorePassword);
        }

        // Wir holen nur das Zertifikat über den CA-Alias
        // Das Shell-Skript hat die CA unter caAlias gespeichert
        X509Certificate caCert = (X509Certificate) keyStore.getCertificate(caAlias);

        if (caCert == null) {
            throw new IllegalStateException("Root-CA Zertifikat nicht im Keystore gefunden!");
        }

        return caCert.getEncoded();
    }

}