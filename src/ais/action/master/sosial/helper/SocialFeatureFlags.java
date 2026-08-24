package ais.action.master.sosial.helper;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import ais.common.Common;
import ais.database.model.Konfigurasi;
import ais.database.model.sosial.SocialTenantSetting;

/** Feature flags default-off for every monetary mutation. */
public final class SocialFeatureFlags {
    public static final String PORTAL="sosial_portal_enabled",COLLECTION="sosial_public_collection_enabled",CALCULATOR="sosial_zakat_calculator_enabled",SMARTLINK="sosial_smartlink_enabled",RECEIPT="sosial_receipt_enabled",REGISTRATION="sosial_general_registration_enabled",GUEST="sosial_allow_guest_donation",TRANSPARENCY="sosial_public_transparency_enabled",ACCOUNTING="sosial_accounting_integration_enabled";
    private SocialFeatureFlags(){}
    public static boolean enabled(Session session,SocialRequestContext context,String key){
        try{SocialTenantSetting s=(SocialTenantSetting)session.createCriteria(SocialTenantSetting.class).add(Restrictions.eq("tenantKey",context.getTenantKey())).setMaxResults(1).uniqueResult();if(s!=null&&s.getFeatureFlags()!=null){JSONObject f=new JSONObject(s.getFeatureFlags());if(f.has(key))return f.optBoolean(key,false);}}
        catch(Exception e){ais.common.ErrorAuditUtil.record(e,"SocialFeatureFlags:"+key);}
        try{return Konfigurasi.AKTIF.equalsIgnoreCase(Common.getKonfigurasi(key,Konfigurasi.TIDAK_AKTIF).getNilai());}catch(Exception e){return false;}
    }
}
