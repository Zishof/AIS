package ais.action.master.sosial.test;

import ais.action.master.sosial.helper.SocialSecurity;

/**
 * Harness uji manual (dijalankan lewat {@code main}) untuk memverifikasi utilitas kriptografi
 * {@link SocialSecurity} yang dipakai mengamankan tautan pintar (smartlink) modul sosial:
 * memeriksa hasil {@link SocialSecurity#hmacSha256(String, String)} terhadap vektor uji tetap,
 * dan memastikan {@link SocialSecurity#constantEquals(String, String)} menolak string dengan
 * panjang berbeda (bagian dari jaminan perbandingan waktu-konstan untuk mencegah timing attack).
 */
public final class SocialSmartlinkSecuritySelfTest {
    /** Menjalankan pemeriksaan vektor HMAC-SHA256 dan perilaku {@code constantEquals}; melempar {@link IllegalStateException} bila salah satu gagal. */
    public static void main(String[] args){String actual=SocialSecurity.hmacSha256("key","The quick brown fox jumps over the lazy dog");String expected="f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8";if(!SocialSecurity.constantEquals(expected,actual))throw new IllegalStateException("HMAC-SHA256 vector gagal: "+actual);if(SocialSecurity.constantEquals(expected,expected+"00"))throw new IllegalStateException("Constant equality length gagal.");System.out.println("SocialSmartlinkSecuritySelfTest OK");}
}
