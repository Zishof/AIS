package ais.action.master.sosial.helper;

import org.hibernate.Session; import org.hibernate.criterion.Restrictions;
import ais.database.model.Tbmuser; import ais.database.model.sosial.SocialDonorIdentity;

public final class SocialIdentityService {
    public SocialDonorIdentity resolveOrCreate(Session session,SocialRequestContext context){
        Tbmuser user=context.getUser(); if(user==null)return null;
        SocialDonorIdentity found=(SocialDonorIdentity)session.createCriteria(SocialDonorIdentity.class).add(Restrictions.eq("tenantKey",context.getTenantKey())).add(Restrictions.eq("tbmuser",user)).setMaxResults(1).uniqueResult();
        if(found==null){found=new SocialDonorIdentity();found.setTenantKey(context.getTenantKey());found.setTbmuser(user);found.setDisplayName(text(user.getNama(),user.getUserId()));found.setEmail(user.getEmail());found.setPhone(user.getHp());found.setDonorType(resolveType(user));found.setExternalMember(Boolean.FALSE);found.setStatus("ACTIVE");found.setCreatedBy(context.getActorId());session.save(found);}
        found.setLastLogin(ais.ui.util.WaktuUtil.getDate());found.setUpdatedBy(context.getActorId());return found;
    }
    private String resolveType(Tbmuser u){try{if(u.getMahasiswa()!=null)return "MAHASISWA";}catch(Exception ignored){}try{if(u.getSiswa()!=null)return "SISWA";}catch(Exception ignored){}try{if(u.getDosen()!=null)return "DOSEN";}catch(Exception ignored){}try{if(u.getGuru()!=null)return "GURU";}catch(Exception ignored){}try{if(u.getPegawai()!=null)return "PEGAWAI";}catch(Exception ignored){}return "AIS_USER";}
    private String text(String a,String b){return a==null||a.trim().isEmpty()?b:a.trim();}
}
