package ais.common;


import java.security.MessageDigest;

public class SHA512 {
    
    public static String SHA512Hash(String password) {
        return sha512(password);
    }
    
    
    private static String sha512(String password) {
        MessageDigest sha = null;
        byte[] hash = null;
        try {
            sha = MessageDigest.getInstance("SHA-512");
            hash = sha.digest(password.getBytes("UTF-8"));
        } catch (Exception e) {
            System.err.println(e);
        }
        return convertToHex(hash);
    }
 
    private static String convertToHex(byte[] raw) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < raw.length; i++) {
            sb.append(Integer.toString((raw[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }
}