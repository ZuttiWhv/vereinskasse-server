package de.tozwhv.vereinskasse.server.config;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Date;

@Component
public class CertificateInitializer implements ApplicationRunner {
    @Value("${app.ca-alias}")
    private String caAlias;

    @Value("${server.ssl.key-store}")
    private String keystorePath;

    @Value("${server.ssl.key-store-password}")
    private String keystorePassword;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        File ksFile = new File(keystorePath);
        KeyStore keyStore = KeyStore.getInstance("PKCS12");

        if (ksFile.exists()) {
            try (FileInputStream fis = new FileInputStream(ksFile)) {
                keyStore.load(fis, keystorePassword.toCharArray());
            }
        } else {
            keyStore.load(null, null); // Neuen leeren KeyStore erstellen
        }

        if (!keyStore.containsAlias(caAlias)) {
            System.out.println("Keine Root-CA gefunden. Generiere neue Vereins-CA...");
            generateRootCA(keyStore);

            try (FileOutputStream fos = new FileOutputStream(ksFile)) {
                keyStore.store(fos, keystorePassword.toCharArray());
            }
            System.out.println("Root-CA erfolgreich unter 'certs/keystore.p12' gespeichert.");
        }
    }

    private void generateRootCA(KeyStore keyStore) throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(4096); // CA sollte stark sein
        KeyPair caKeyPair = keyGen.generateKeyPair();

        X500Name issuer = new X500Name("CN=Vereinskasse Root CA, O=DeinVerein, C=DE");
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore = new Date();
        Date notAfter = new Date(System.currentTimeMillis() + (20L * 365 * 24 * 60 * 60 * 1000)); // 20 Jahre

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer, serial, notBefore, notAfter, issuer, caKeyPair.getPublic());

        // WICHTIG: Als CA markieren
        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));

        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").build(caKeyPair.getPrivate());
        X509Certificate caCert = new JcaX509CertificateConverter().getCertificate(certBuilder.build(signer));

        keyStore.setKeyEntry(caAlias, caKeyPair.getPrivate(), keystorePassword.toCharArray(),
                new X509Certificate[]{caCert});
    }
}