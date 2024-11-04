package com.projects.imagehider.utils;

public class EncryptionConstants
{
    public static final String HASH_ALGORITHM = "SHA-256";
    public static final String AES_ALGORITHM = "AES";
    public static final String END_SEPARATOR = "0111111111111110";
    public static final String BASIC_SEED = "seed";
    public static final int AES_KEY_SIZE = 16;

    private EncryptionConstants()
    {
        // Utility class
    }
}
