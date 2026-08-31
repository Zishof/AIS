package ais.action.master.sosial.helper;

import org.hibernate.Session; import org.hibernate.criterion.Restrictions;
import ais.database.model.Tbmuser; import ais.database.model.sosial.SocialDonorIdentity;

/**
 * Layanan modul sosial yang meresolusi identitas donatur ({@link SocialDonorIdentity}) untuk user
 * AIS ({@link Tbmuser}) yang login, dibuat otomatis (auto-provisioning) saat pertama kali diakses
 * untuk kombinasi tenant+user tersebut. Dipakai antara lain oleh {@code ZakatCalculatorService}
 * untuk mengaitkan hasil perhitungan zakat ke identitas donatur yang terautentikasi.
 */
public final class SocialIdentityService {
    /**
     * Mencari {@link SocialDonorIdentity} milik user pada {@code context} (tenant + {@link Tbmuser});
     * membuat baris baru bila belum ada (nama tampilan dari nama user atau userId sebagai fallback,
     * email/telepon disalin dari user, jenis donatur diresolusi lewat {@link #resolveType}, status
     * {@code ACTIVE}). Baik baris baru maupun yang sudah ada selalu diperbarui {@code lastLogin} dan
     * {@code updatedBy} setiap kali dipanggil.
     *
     * @return identitas donatur yang ditemukan/dibuat, atau {@code null} bila {@code context} tidak punya user terautentikasi
     */
    public SocialDonorIdentity resolveOrCreate(Session session,SocialRequestContext context){
        Tbmuser user=context.getUser(); if(user==null)return null;
        SocialDonorIdentity found=(SocialDonorIdentity)session.createCriteria(SocialDonorIdentity.class).add(Restrictions.eq("tenantKey",context.getTenantKey())).add(Restrictions.eq("tbmuser",user)).setMaxResults(1).uniqueResult();
        if(found==null){found=new SocialDonorIdentity();found.setTenantKey(context.getTenantKey());found.setTbmuser(user);found.setDisplayName(text(user.getNama(),user.getUserId()));found.setEmail(user.getEmail());found.setPhone(user.getHp());found.setDonorType(resolveType(user));found.setExternalMember(Boolean.FALSE);found.setStatus("ACTIVE");found.setCreatedBy(context.getActorId());session.save(found);}
        found.setLastLogin(ais.ui.util.WaktuUtil.getDate());found.setUpdatedBy(context.getActorId());return found;
    }
    /** Menentukan jenis donatur ({@code "MAHASISWA"}/{@code "SISWA"}/{@code "DOSEN"}/{@code "GURU"}/{@code "PEGAWAI"}, dicek berurutan) dari relasi peran {@code u}; {@code "AIS_USER"} bila tidak ada peran spesifik yang cocok. */
    private String resolveType(Tbmuser u){try{if(u.getMahasiswa()!=null)return "MAHASISWA";}catch(Exception ignored){}try{if(u.getSiswa()!=null)return "SISWA";}catch(Exception ignored){}try{if(u.getDosen()!=null)return "DOSEN";}catch(Exception ignored){}try{if(u.getGuru()!=null)return "GURU";}catch(Exception ignored){}try{if(u.getPegawai()!=null)return "PEGAWAI";}catch(Exception ignored){}return "AIS_USER";}
    /** @return {@code a} bila tidak kosong/null (di-trim), selain itu {@code b} sebagai fallback. */
    private String text(String a,String b){return a==null||a.trim().isEmpty()?b:a.trim();}
}
