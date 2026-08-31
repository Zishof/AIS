package ais.action.master.sosial.helper;

import java.util.Date; import org.hibernate.Session; import org.hibernate.criterion.Restrictions;
import ais.database.model.sosial.SocialTenantSetting;

/**
 * Penjaga kepatuhan (compliance gate) sebelum modul Portal Sosial diizinkan mengumpulkan dana
 * publik untuk satu tenant. Memusatkan seluruh syarat legal/operasional yang harus terpenuhi
 * (konfigurasi tenant ada, pengumpulan publik & gateway pembayaran diaktifkan, mode operasi
 * bukan sandbox-saja, kebijakan privasi/ketentuan terisi, izin/SK masih berlaku, dan feature
 * flag pengumpulan aktif) di satu tempat, sehingga tiap titik masuk donasi tidak perlu
 * mengulang pengecekan yang sama.
 */
public final class SocialComplianceService {
    /**
     * Memvalidasi bahwa tenant pada {@code context} sudah lengkap dan diizinkan mengumpulkan
     * donasi publik, memeriksa seluruh syarat kepatuhan secara berurutan.
     *
     * @param session sesi Hibernate aktif untuk membaca {@link SocialTenantSetting}
     * @param context konteks permintaan, sumber {@code tenantKey} yang diperiksa
     * @return pengaturan tenant yang sudah lolos seluruh syarat kepatuhan
     * @throws IllegalStateException dengan pesan spesifik untuk syarat pertama yang tidak
     *                                terpenuhi (konfigurasi belum ada, pengumpulan/gateway
     *                                nonaktif, mode sandbox-saja, kebijakan belum lengkap, izin
     *                                kedaluwarsa, atau feature flag nonaktif)
     */
    public SocialTenantSetting requireCollectionReady(Session session,SocialRequestContext context){
        SocialTenantSetting s=(SocialTenantSetting)session.createCriteria(SocialTenantSetting.class).add(Restrictions.eq("tenantKey",context.getTenantKey())).setMaxResults(1).uniqueResult();
        if(s==null)throw new IllegalStateException("Konfigurasi Sosial tenant belum tersedia.");
        if(!Boolean.TRUE.equals(s.getPublicCollectionEnabled()))throw new IllegalStateException("Pengumpulan publik belum diaktifkan.");
        if(!Boolean.TRUE.equals(s.getGatewayEnabled()))throw new IllegalStateException("Gateway sosial belum diaktifkan.");
        if(s.getOperationMode()==null||"SANDBOX_ONLY".equals(s.getOperationMode()))throw new IllegalStateException("Mode operasi belum mengizinkan pengumpulan publik.");
        if(s.getPrivacyVersion()==null||s.getTermsVersion()==null)throw new IllegalStateException("Kebijakan privasi dan ketentuan belum lengkap.");
        if(s.getPermitValidUntil()!=null&&s.getPermitValidUntil().before(new Date()))throw new IllegalStateException("Masa berlaku izin/SK telah berakhir.");
        if(!SocialFeatureFlags.enabled(session,context,SocialFeatureFlags.COLLECTION))throw new IllegalStateException("Feature flag pengumpulan publik masih nonaktif.");
        return s;
    }
}
