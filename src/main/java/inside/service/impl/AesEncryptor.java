package inside.service.impl;

import inside.util.codec.*;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.SecureRandom;

class AesEncryptor{
    private static final String AES_CBC_ALGORITHM = "AES/CBC/PKCS5Padding";

    private final SecretKey secretKey;
    private final Cipher encryptor;
    private final Cipher decryptor;
    private final SecureRandom rand = new SecureRandom();

    public AesEncryptor(String password, CharSequence salt){
        this(CipherUtils.newSecretKey("PBKDF2WithHmacSHA1", new PBEKeySpec(password.toCharArray(), Hex.decode(salt), 1024, 256)));
    }

    public AesEncryptor(SecretKey secretKey){
        this.secretKey = new SecretKeySpec(secretKey.getEncoded(), "AES");
        this.encryptor = CipherUtils.newCipher(AES_CBC_ALGORITHM);
        this.decryptor = CipherUtils.newCipher(AES_CBC_ALGORITHM);
    }

    private byte[] generateKey(){
        byte[] bytes = new byte[16];
        rand.nextBytes(bytes);
        return bytes;
    }

    public byte[] encrypt(byte[] bytes){
        synchronized(encryptor){
            byte[] iv = generateKey();
            CipherUtils.initCipher(encryptor, Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
            byte[] encrypted = CipherUtils.doFinal(encryptor, bytes);
            return concatenate(iv, encrypted);
        }
    }

    public byte[] decrypt(byte[] encryptedBytes){
        synchronized(decryptor){
            byte[] iv = iv(encryptedBytes);
            CipherUtils.initCipher(decryptor, Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            return CipherUtils.doFinal(decryptor, encrypted(encryptedBytes, iv.length));
        }
    }

    private byte[] iv(byte[] encrypted){
        return subArray(encrypted, 0, 16);
    }

    private byte[] encrypted(byte[] encryptedBytes, int ivLength){
        return subArray(encryptedBytes, ivLength, encryptedBytes.length);
    }

    public String encrypt(String text){
        return new String(Hex.encode(encrypt(Utf8.encode(text))));
    }

    public String decrypt(String encryptedText){
        return Utf8.decode(decrypt(Hex.decode(encryptedText)));
    }

    static byte[] concatenate(byte[]... arrays){
        int length = 0;
        for(byte[] array : arrays){
            length += array.length;
        }
        byte[] newArray = new byte[length];
        int destPos = 0;
        for(byte[] array : arrays){
            System.arraycopy(array, 0, newArray, destPos, array.length);
            destPos += array.length;
        }
        return newArray;
    }

    static byte[] subArray(byte[] array, int beginIndex, int endIndex){
        int length = endIndex - beginIndex;
        byte[] subarray = new byte[length];
        System.arraycopy(array, beginIndex, subarray, 0, length);
        return subarray;
    }
}
