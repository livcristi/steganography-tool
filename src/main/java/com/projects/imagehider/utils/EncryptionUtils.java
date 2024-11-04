package com.projects.imagehider.utils;


import com.projects.imagehider.exception.MissingDataException;
import org.apache.logging.log4j.util.Strings;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

public class EncryptionUtils
{
    private EncryptionUtils()
    {
        throw new IllegalStateException("Utility class");
    }

    public static String hashSimpleData(String data)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance(EncryptionConstants.HASH_ALGORITHM);

            byte[] encodedHash = digest.digest(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash)
            {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e)
        {
            return "error";
        }
    }

    public static SecretKeySpec hashData(String data) throws NoSuchAlgorithmException
    {
        return hashData(data, EncryptionConstants.BASIC_SEED);
    }

    /**
     * Hashes the given data and seed to generate a {@link SecretKeySpec} used later for encryption / decryption.
     */
    public static SecretKeySpec hashData(String data, String seed) throws NoSuchAlgorithmException
    {
        // Validate the data
        if (Strings.isBlank(data) || Strings.isBlank(seed))
        {
            throw new MissingDataException("Missing data or seed");
        }

        MessageDigest digest = MessageDigest.getInstance(EncryptionConstants.HASH_ALGORITHM);

        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        byte[] seedBytes = seed.getBytes(StandardCharsets.UTF_8);

        // Sum the data and seed bytes, for a better masking
        int seedLen = seedBytes.length;
        int seedIndex = 0;
        for (int i = 0; i < dataBytes.length; i++)
        {
            dataBytes[i] = (byte) (dataBytes[i] ^ seedBytes[seedIndex]);
            seedIndex = (seedIndex + 1) % seedLen;
        }

        // Add the bytes to the message digest, then create the SecretKeySpec
        digest.update(dataBytes);
        byte[] hash = Arrays.copyOf(digest.digest(), EncryptionConstants.AES_KEY_SIZE);
        return new SecretKeySpec(hash, EncryptionConstants.AES_ALGORITHM);
    }

    /**
     * Encrypts the received data using the given {@link SecretKeySpec}.
     */
    public static String encrypt(String message, SecretKeySpec secretKeySpec) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException
    {
        Cipher cipher = Cipher.getInstance(EncryptionConstants.AES_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        byte[] encryptedData = cipher.doFinal(message.getBytes());
        return Base64.getEncoder().encodeToString(encryptedData);
    }

    /**
     * Decrypts the received data using the given {@link SecretKeySpec}.
     */
    public static String decrypt(String message, SecretKeySpec secretKeySpec) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException
    {
        Cipher cipher = Cipher.getInstance(EncryptionConstants.AES_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        byte[] encodedData = Base64.getDecoder().decode(message);
        byte[] decryptedData = cipher.doFinal(encodedData);
        return new String(decryptedData);
    }
}
