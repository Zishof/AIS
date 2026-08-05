package ais.common;



import java.security.MessageDigest;

/**
 * Demonstrates how to generate SHA256 hash in Java
 * @author JJ
 */
public class SHA256InJava {

    

    /**
     * Returns a hexadecimal encoded SHA-256 hash for the input String.
     * @param data
     * @return 
     */
    public String getSHA256Hash(String data) {
        String result = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes("UTF-8"));
            return bytesToHex(hash); // make it printable
        }catch(Exception ex) {
            ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/common/SHA256InJava.java:27");
        }
        return result;
    }
    
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte hashByte : bytes) {
            int intVal = 0xff & hashByte;
            if (intVal < 0x10) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(intVal));
        }
        return sb.toString();
    }
}