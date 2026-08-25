package ais.action.master.sosial.test;

import ais.action.master.sosial.helper.SocialSecurity;

public final class SocialSmartlinkSecuritySelfTest {
    public static void main(String[] args){String actual=SocialSecurity.hmacSha256("key","The quick brown fox jumps over the lazy dog");String expected="f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8";if(!SocialSecurity.constantEquals(expected,actual))throw new IllegalStateException("HMAC-SHA256 vector gagal: "+actual);if(SocialSecurity.constantEquals(expected,expected+"00"))throw new IllegalStateException("Constant equality length gagal.");System.out.println("SocialSmartlinkSecuritySelfTest OK");}
}
