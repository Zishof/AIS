package ais.common;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.Certificate;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PasswordFinder;
import org.bouncycastle.util.encoders.Base64;

/**
 * For decrypting data encrypted with PHP's openssl_seal()
 * 
 * Example - String envKey is the Base64 encoded, RSA encrypted envelop key
 * and String sealedData is the Base64 encoded, RC4 encrypted payload, which
 * you got from PHP's openssl_seal() then;
 * 
 * <pre>
 * KeyPair keyPair = OpenSSLInterop.keyPairPEMFile(
 *          "/path/to/openssl/privkey_rsa.pem"
 *      );
 * 
 * PrivateKey privateKey = keyPair.getPrivate();
 * String plainText = OpenSSLInterop.decrypt(sealedData, envKey, privateKey);
 * </pre>
 * 
 * @see http://www.php.net/openssl_seal
 * @author Harry Fuecks
 * @since Oct 25, 2007
 * @version $Id$
 */
public class OpenSSLInterop {

    private static boolean bcInitialized = false;

    /**
     * @param cipherKey
     * @param cipherText
     * @param privateKey
     * @return
     * @throws Exception
     */
    public static String decrypt(String cipherText, String cipherKey, PrivateKey privateKey)
    throws Exception {

        return decrypt(cipherText, cipherKey, privateKey, "UTF-8");

    }

    /**
     * @param cipherKey Base64 encoded, RSA encrypted key for RC4 decryption
     * @param cipherText Base64 encoded, RC4 encrypted payload
     * @param privateKey
     * @param charsetName
     * @return decrypted payload
     * @throws Exception
     */
    public static String decrypt(String cipherText, String cipherKey, PrivateKey privateKey, String charsetName)
    throws Exception {

        byte[] plainKey = decryptRSA(Base64.decode(cipherKey), privateKey);
        byte[] plaintext = decryptRC4(plainKey, Base64.decode(cipherText));
        return new String(plaintext, charsetName);

    }

    /**
     * Loads a KeyPair object from an OpenSSL private key file
     * (Just wrapper around Bouncycastles PEMReader)
     * @param filename
     * @return
     * @throws Exception
     */
    @SuppressWarnings("resource")
	public static KeyPair keyPairFromPEMFile(String filename, final String password) throws Exception {

        if ( !bcInitialized ) {
            Security.addProvider(new BouncyCastleProvider());
            bcInitialized = true;
        }

        FileReader keyFile = new FileReader(filename);
        PEMReader pemReader = new PEMReader(keyFile,  new PasswordFinder() {

            public char[] getPassword() {
                return password.toCharArray();
            }});

        return (KeyPair)pemReader.readObject();

    }

    /**
     * Returns a KeyPair from a Java keystore file.
     * 
     * Note that you can convert OpenSSL keys into Java Keystore using the
     * "Not yet commons-ssl" KeyStoreBuilder
     * See - http://juliusdavies.ca/commons-ssl/utilities/
     * e.g.
     * 
     * $ java -cp not-yet-commons-ssl-0.3.8.jar org.apache.commons.ssl.KeyStoreBuilder \
     *     "privkey password" ./privkey.pem ./pubkey.pem 
     *   
     * @param filename
     * @param alias
     * @param keystorePassword
     * @return
     */
    public static KeyPair keyPairFromKeyStore(String filename, String alias, String keystorePassword)
    throws Exception {

        KeyStore keystore = KeyStore.getInstance("JKS");
        File keystoreFile = new File(filename);

        FileInputStream in = new FileInputStream(keystoreFile);
        keystore.load(in, keystorePassword.toCharArray());
        in.close();

        Key key = keystore.getKey(alias, keystorePassword.toCharArray());
        Certificate cert = keystore.getCertificate(alias);
        PublicKey publicKey = cert.getPublicKey();

        return new KeyPair(publicKey, (PrivateKey)key);

    }

    /**
     * @param cipherKey
     * @param privateKey
     * @return
     * @throws Exception
     */
    private static byte[] decryptRSA(byte[] cipherKey, PrivateKey privateKey) throws Exception {

        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(cipherKey);

    }

    /**
     * Defaults to UTF-8 as the output encoding
     * @param plainKey Base64 encoded, RSA encrypted key for RC4 decryption
     * @param cipherText Base64 encoded, RC4 encrypted payload
     * @return decrypted payload 
     * @throws Exception
     */
    private static byte[] decryptRC4(byte[] plainKey, byte[] cipherText)
    throws Exception {

        SecretKey skeySpec = new SecretKeySpec(plainKey, "RC4");
        Cipher cipher = Cipher.getInstance("RC4");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        return cipher.doFinal(cipherText);

    }

}