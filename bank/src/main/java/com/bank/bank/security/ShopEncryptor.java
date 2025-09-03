package com.bank.bank.security;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class ShopEncryptor {

    @Value("${crypto.shop.public-key-path}")
    private Resource shopPublicPem;

    private PublicKey shopPublicKey;

    @PostConstruct
    void init() throws Exception {
        String pem = new String(shopPublicPem.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
                .replace("-----BEGIN PUBLIC KEY-----","")
                .replace("-----END PUBLIC KEY-----","")
                .replaceAll("\\s+","");
        byte[] der = Base64.getDecoder().decode(pem);
        shopPublicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
    }

    public String encryptToBase64(String plaintext) {
        try {
            Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            c.init(Cipher.ENCRYPT_MODE, shopPublicKey);
            byte[] out = c.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) { throw new RuntimeException("Shop encryption failed", e); }
    }
}