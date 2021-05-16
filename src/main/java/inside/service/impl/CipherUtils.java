package inside.service.impl;

import reactor.util.annotation.Nullable;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import java.security.*;
import java.security.spec.*;

final class CipherUtils{

    private CipherUtils(){}

    static SecretKey newSecretKey(String algorithm, PBEKeySpec keySpec){
        try{
            SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm);
            return factory.generateSecret(keySpec);
        }catch(NoSuchAlgorithmException ex){
            throw new IllegalArgumentException("Not a valid encryption algorithm", ex);
        }catch(InvalidKeySpecException ex){
            throw new IllegalArgumentException("Not a valid secret key", ex);
        }
    }

    static Cipher newCipher(String algorithm){
        try{
            return Cipher.getInstance(algorithm);
        }catch(NoSuchAlgorithmException ex){
            throw new IllegalArgumentException("Not a valid encryption algorithm", ex);
        }catch(NoSuchPaddingException ex){
            throw new IllegalStateException("Should not happen", ex);
        }
    }

    static void initCipher(Cipher cipher, int mode, SecretKey secretKey, @Nullable AlgorithmParameterSpec parameterSpec){
        try{
            if(parameterSpec != null){
                cipher.init(mode, secretKey, parameterSpec);
            }else{
                cipher.init(mode, secretKey);
            }
        }catch(InvalidKeyException ex){
            throw new IllegalArgumentException("Unable to initialize due to invalid secret key", ex);
        }catch(InvalidAlgorithmParameterException ex){
            throw new IllegalStateException("Unable to initialize due to invalid decryption parameter spec", ex);
        }
    }

    static byte[] doFinal(Cipher cipher, byte[] input){
        try{
            return cipher.doFinal(input);
        }catch(IllegalBlockSizeException ex){
            throw new IllegalStateException("Unable to invoke Cipher due to illegal block size", ex);
        }catch(BadPaddingException ex){
            throw new IllegalStateException("Unable to invoke Cipher due to bad padding", ex);
        }
    }
}
