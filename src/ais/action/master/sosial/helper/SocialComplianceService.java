package ais.action.master.sosial.helper;

import java.util.Date; import org.hibernate.Session; import org.hibernate.criterion.Restrictions;
import ais.database.model.sosial.SocialTenantSetting;

public final class SocialComplianceService {
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
