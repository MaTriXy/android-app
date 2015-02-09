// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.util;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;

import javax.crypto.Cipher;

/**
 * Helper methods for encryption
 */
public class EncryptionUtil {

    private static final String TAG = EncryptionUtil.class.getSimpleName();

    /**
     * size of a 1024 bit RSA key
     */
    public static final int KEY_SIZE_1024 = 1024;

    /**
     * size of a 2048 bit RSA key
     */
    public static final int KEY_SIZE_2048 = 2048;

    /**
     * size of a 4096 bit RSA key
     */
    public static final int KEY_SIZE_4096 = 4096;

    /**
     * Generates a public/private RSA key pair of a given size
     *
     * @param keySize the size of the key you want to generate
     * @return the KeyPair object
     */
    public static KeyPair generateRSAKey(int keySize) {
        KeyPairGenerator keyGen = null;

        try {
            keyGen = KeyPairGenerator.getInstance("RSA");

        } catch (Exception e) {
            LogUtils.e(TAG, e.getMessage(), e);
        }

        keyGen.initialize(keySize);

        return keyGen.generateKeyPair();
    }

    public static byte[] decryptRSAEncryptedBytes(byte[] bytes, PrivateKey privateKey) {
        try {
            Cipher rsa = Cipher.getInstance("RSA");
            rsa.init(Cipher.DECRYPT_MODE, privateKey);
            return rsa.doFinal(bytes);

        } catch (Exception e) {
            LogUtils.e(TAG, e.getMessage(), e);
        }

        return null;
    }

    /**
     * Decrypt Encrypted encryption key.
     *
     * @param encryptionKey the encrypted encryption key
     * @return the key
     */
    public static char[] decryptEncryptionKey(byte[] encryptionKey) {
        char[] key = new char[encryptionKey.length];
        for (int i = 0; i < encryptionKey.length; i++) {
            key[i] = (char) encryptionKey[encryptionKey.length - 1 - i];
        }
        return key;
    }

    /**
     * Encrypt encryption key
     *
     * @param encryptionKey the encryption key got from key server
     * @return encrypted encryption key
     */
    public static byte[] encryptEncryptionKey(byte[] encryptionKey) {
        byte[] key = new byte[encryptionKey.length];

        for (int i = 0; i < encryptionKey.length; i++) {
            key[i] = encryptionKey[encryptionKey.length - 1 - i];
        }
        return key;
    }
}