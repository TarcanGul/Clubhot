package models

import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import com.typesafe.config.ConfigFactory
import javax.inject._

@Singleton
class EncryptorService {
    def encrypt(plaintext: String): String = {
        val encryptionKeyBytes = ConfigFactory.load().getString("token.encryption.key").getBytes("UTF-8")
        val secretKey = new SecretKeySpec(encryptionKeyBytes, "AES")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val result = cipher.doFinal(plaintext.getBytes)
        Base64.getEncoder().encodeToString(result)
    }

    def decrypt(ciphertext: String): String = {
        val encryptionKeyBytes = ConfigFactory.load().getString("token.encryption.key").getBytes("UTF-8")
        val secretKey = new SecretKeySpec(encryptionKeyBytes, "AES")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        val decoded = Base64.getDecoder().decode(ciphertext.getBytes)
        new String(cipher.doFinal(decoded))
    }
}
