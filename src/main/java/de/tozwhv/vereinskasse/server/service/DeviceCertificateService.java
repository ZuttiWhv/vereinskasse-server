package de.tozwhv.vereinskasse.server.service;

import org.bouncycastle.asn1.x500.X500Name;
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
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Date;


@Service
public class DeviceCertificateService {

    private final String keyStoreFile;
    private final char[] keystorePass;
    private final String caAlias;

    // Spring füllt die Parameter automatisch aus den Properties
    public DeviceCertificateService(
            @Value("${server.ssl.key-store}") String keyStoreFile,
            @Value("${server.ssl.key-store-password}") String keyStorePassword,
            @Value("${app.ca-alias}") String caAlias) {

        this.keyStoreFile = keyStoreFile.replace("file:", ""); // Falls Präfix vorhanden
        this.keystorePass = keyStorePassword.toCharArray();
        this.caAlias = caAlias;
    }


    public byte[] createDeviceCertificate(String deviceName) throws Exception {
        // 1. Laden der CA aus dem vorhandenen Keystore
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(new FileInputStream(keyStoreFile), keystorePass);


        KeyStore.PrivateKeyEntry caEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(
                caAlias, new KeyStore.PasswordProtection(keystorePass));

        X509Certificate caCert = (X509Certificate) caEntry.getCertificate();
        PrivateKey caPrivateKey = caEntry.getPrivateKey();

        // 2. Neues Schlüsselpaar für das Terminal-Gerät erzeugen
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(4096);
        KeyPair deviceKeyPair = keyGen.generateKeyPair();

        // 3. Zertifikat-Metadaten (Gültigkeit 10 Jahre)
        X500Name subject = new X500Name("CN=" + deviceName + ", O=Vereinskasse, OU=Terminals");
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore = new Date();
        Date notAfter = new Date(System.currentTimeMillis() + (10L * 365 * 24 * 60 * 60 * 1000));

        // 4. Zertifikat mit Bouncy Castle signieren
        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                caCert, serial, notBefore, notAfter, subject, deviceKeyPair.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").build(caPrivateKey);
        X509Certificate deviceCert = new JcaX509CertificateConverter()
                .getCertificate(certBuilder.build(signer));

        // 5. In ein PKCS12-Bundle (P12) für den Browser-Download verpacken
        KeyStore deviceP12 = KeyStore.getInstance("PKCS12");
        deviceP12.load(null, null);
        deviceP12.setKeyEntry(deviceName, deviceKeyPair.getPrivate(), keystorePass,
                new X509Certificate[]{deviceCert, caCert});

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        deviceP12.store(bos, keystorePass);
        return bos.toByteArray();
    }
}