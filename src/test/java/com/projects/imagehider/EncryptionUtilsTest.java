package com.projects.imagehider;


import com.projects.imagehider.utils.EncryptionUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class EncryptionUtilsTest
{
    @Test
    public void shouldHashDataDifferently() throws NoSuchAlgorithmException
    {
        // given
        String data = "secret";
        String otherData = "otherSecret";

        // when
        SecretKeySpec key = EncryptionUtils.hashData(data);
        SecretKeySpec otherKey = EncryptionUtils.hashData(otherData);

        // then
        Assertions.assertEquals(key.getAlgorithm(), otherKey.getAlgorithm());
        Assertions.assertNotEquals(key.getEncoded()[0], otherKey.getEncoded()[0]);
    }

    @Test
    public void shouldHashDataSame() throws NoSuchAlgorithmException
    {
        // given
        String data = "secret";
        String otherData = "secret";

        // when
        SecretKeySpec key = EncryptionUtils.hashData(data);
        SecretKeySpec otherKey = EncryptionUtils.hashData(otherData);

        // then
        Assertions.assertEquals(key.getAlgorithm(), otherKey.getAlgorithm());
        Assertions.assertArrayEquals(key.getEncoded(), otherKey.getEncoded());
    }

    @Test
    public void shouldHashDifferentWhenSeedDiffers() throws NoSuchAlgorithmException
    {
        // given
        String data = "secret";
        String otherData = "secret";
        String seed = "seed";
        String otherSeed = "otherSeed";

        // when
        SecretKeySpec key = EncryptionUtils.hashData(data, seed);
        SecretKeySpec otherKey = EncryptionUtils.hashData(otherData, otherSeed);

        // then
        Assertions.assertEquals(key.getAlgorithm(), otherKey.getAlgorithm());
        Assertions.assertNotEquals(key.getEncoded()[0], otherKey.getEncoded()[0]);
    }

    @Test
    public void shouldEncryptSameMessage() throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException
    {
        // given
        String key = "key";
        SecretKeySpec secretKeySpec = EncryptionUtils.hashData(key);
        String message = "message";
        String otherMessage = "message";

        // when
        String encryptedMessaged = EncryptionUtils.encrypt(message, secretKeySpec);
        String encryptedOtherMessaged = EncryptionUtils.encrypt(otherMessage, secretKeySpec);

        // then
        Assertions.assertEquals(encryptedMessaged, encryptedOtherMessaged);
    }

    @Test
    public void shouldEncryptDifferentMessage() throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException
    {
        // given
        String key = "key";
        SecretKeySpec secretKeySpec = EncryptionUtils.hashData(key);
        String message = "message";
        String otherMessage = "otherMessage";

        // when
        String encryptedMessaged = EncryptionUtils.encrypt(message, secretKeySpec);
        String encryptedOtherMessaged = EncryptionUtils.encrypt(otherMessage, secretKeySpec);

        // then
        Assertions.assertNotEquals(encryptedMessaged, encryptedOtherMessaged);
    }

    @Test
    public void shouldEncryptAndDecrypt() throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException
    {
        // given
        String key = "key";
        SecretKeySpec secretKeySpec = EncryptionUtils.hashData(key);
        String message = "message";

        // when
        String encryptedMessaged = EncryptionUtils.encrypt(message, secretKeySpec);
        String decryptedMessage = EncryptionUtils.decrypt(encryptedMessaged, secretKeySpec);

        // then
        Assertions.assertEquals(message, decryptedMessage);
    }

    @Test
    public void shouldNotDecryptIfKeyDifferent() throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException
    {
        // given
        String key = "key";
        SecretKeySpec secretKeySpec = EncryptionUtils.hashData(key);
        SecretKeySpec otherSecretKeySpec = EncryptionUtils.hashData(key + "key");
        String message = "message";

        // when
        String encryptedMessaged = EncryptionUtils.encrypt(message, secretKeySpec);

        // then
        Assertions.assertThrowsExactly(BadPaddingException.class, () -> EncryptionUtils.decrypt(encryptedMessaged, otherSecretKeySpec));
    }
}
