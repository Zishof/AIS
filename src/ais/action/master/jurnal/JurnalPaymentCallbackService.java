package ais.action.master.jurnal;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import ais.database.model.LogPembayaran;

/** Authenticates journal payment callbacks before reconciliation. */
public final class JurnalPaymentCallbackService {
    /**
     * Kontrak callback/strategi bersarang milik {@link JurnalPaymentCallbackService}. Tipe ini memisahkan satu
     * variasi perilaku lokal tanpa membuat service atau interface global yang tumpang tindih.
     *
     * <p><b>Scope:</b> setiap instance terikat pada instance {@link JurnalPaymentCallbackService} dan dapat
     * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code get()}, {@code allowedProviders}().
     * Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
     * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
     * tambahkan perilaku lintas domain pada service bersama.</p>
     *
     * @see JurnalPaymentCallbackService
     */
    public interface SecretResolver { String get(String provider); String allowedProviders(); }
    private static final long DEFAULT_SKEW_MILLIS = 300000L;
    private final SecretResolver secrets;
    private final long skewMillis;

    public JurnalPaymentCallbackService() { this(new EnvironmentSecrets(), DEFAULT_SKEW_MILLIS); }
    public JurnalPaymentCallbackService(SecretResolver secrets, long skewMillis) {
        if (secrets == null || skewMillis < 1000L || skewMillis > 900000L)
            throw new IllegalArgumentException("Konfigurasi callback pembayaran tidak valid.");
        this.secrets = secrets; this.skewMillis = skewMillis;
    }

    public LogPembayaran settle(long timestamp, String signature, Long subscriptionId,
            String externalReference, BigDecimal amount, String currency, String provider,
            String providerReference) {
        String p = token(provider).toUpperCase(Locale.ENGLISH);
        requireAllowed(p);
        if (Math.abs(System.currentTimeMillis() - timestamp) > skewMillis)
            throw new SecurityException("Timestamp callback pembayaran kedaluwarsa.");
        String secret = clean(secrets.get(p));
        if (secret.length() < 32) throw new SecurityException("Secret callback provider belum dikonfigurasi.");
        String canonical = canonical(timestamp, p, subscriptionId, externalReference, amount,
                currency, providerReference);
        byte[] expected = hmac(secret, canonical);
        byte[] supplied = hex(clean(signature));
        if (!MessageDigest.isEqual(expected, supplied))
            throw new SecurityException("Signature callback pembayaran tidak valid.");
        return new JurnalPaymentService().settleVerified(subscriptionId, externalReference, amount,
                currency, p, providerReference, "payment-callback:" + p.toLowerCase(Locale.ENGLISH));
    }

    public static String signForTest(String secret, long timestamp, String provider, Long subscriptionId,
            String externalReference, BigDecimal amount, String currency, String providerReference) {
        return toHex(hmac(secret, canonical(timestamp, token(provider).toUpperCase(Locale.ENGLISH),
                subscriptionId, externalReference, amount, currency, providerReference)));
    }

    private void requireAllowed(String provider) {
        Set<String> allowed = new HashSet<String>();
        for (String x : clean(secrets.allowedProviders()).split(","))
            if (clean(x).length() > 0) allowed.add(token(x).toUpperCase(Locale.ENGLISH));
        if (!allowed.contains(provider)) throw new SecurityException("Provider pembayaran tidak diizinkan.");
    }
    private static String canonical(long timestamp, String provider, Long subscriptionId,
            String externalReference, BigDecimal amount, String currency, String providerReference) {
        if (subscriptionId == null || subscriptionId.longValue() <= 0 || amount == null)
            throw new IllegalArgumentException("Payload callback pembayaran tidak valid.");
        String money;
        try { money = amount.setScale(2).toPlainString(); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("Nominal pembayaran tidak valid."); }
        return timestamp + "\n" + token(provider).toUpperCase(Locale.ENGLISH) + "\n" + subscriptionId
                + "\n" + token(externalReference) + "\n" + money + "\n"
                + currency(currency) + "\n" + token(providerReference);
    }
    private static byte[] hmac(String secret, String value) {
        try { Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) { throw new IllegalStateException("HMAC callback pembayaran gagal.", e); }
    }
    private static byte[] hex(String value) {
        if (!value.matches("(?i)[0-9a-f]{64}")) return new byte[0];
        byte[] out = new byte[value.length() / 2];
        for (int i=0;i<out.length;i++) out[i]=(byte)Integer.parseInt(value.substring(i*2,i*2+2),16);
        return out;
    }
    private static String toHex(byte[] value) { StringBuilder b=new StringBuilder(); for(byte x:value)b.append(String.format("%02x",x&255)); return b.toString(); }
    private static String currency(String value) { String x=clean(value).toUpperCase(Locale.ENGLISH); if(!x.matches("[A-Z]{3}"))throw new IllegalArgumentException("Mata uang tidak valid.");return x; }
    private static String token(String value) { String x=clean(value); if(x.length()<2||x.length()>255||!x.matches("[A-Za-z0-9._:/-]+"))throw new IllegalArgumentException("Token callback pembayaran tidak valid.");return x; }
    private static String clean(String value) { return value==null?"":value.trim(); }

    /**
     * Tipe implementasi bersarang {@link EnvironmentSecrets} milik {@link JurnalPaymentCallbackService}. Kelas ini
     * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * JurnalPaymentCallbackService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API
     * kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code get()}, {@code allowedProviders}().
     * Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
     * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
     * tambahkan perilaku lintas domain pada service bersama.</p>
     *
     * @see JurnalPaymentCallbackService
     */
    private static final class EnvironmentSecrets implements SecretResolver {
        public String get(String provider) { return System.getenv("AIS_JURNAL_PAYMENT_" + provider.replaceAll("[^A-Z0-9]", "_") + "_SECRET"); }
        public String allowedProviders() { return System.getenv("AIS_JURNAL_PAYMENT_PROVIDERS"); }
    }
}
