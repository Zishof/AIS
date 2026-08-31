package ais.action.master.sosial.helper;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sosial.Donatur;
import ais.database.model.sosial.JenisDanaSosial;
import ais.database.model.sosial.ProgramDonatur;
import ais.database.model.sosial.SocialDonorIdentity;
import ais.database.model.sosial.SocialProgramExtension;

/** Explicit-tenant, idempotent legacy bridge. It never guesses a tenant from legacy rows. */
public final class SocialLegacyMigrationService {
    public Preview preview(SocialRequestContext c){new SocialPrivilegeGuard().require(c,SocialPrivilegeGuard.ADMIN);Session s=null;try{s=HibernateUtil.openSession();long donors=count(s,Donatur.class);long programs=count(s,ProgramDonatur.class);long mappedDonors=countTenant(s,SocialDonorIdentity.class,c.getTenantKey());long mappedPrograms=countTenant(s,SocialProgramExtension.class,c.getTenantKey());return new Preview(donors,programs,mappedDonors,mappedPrograms);}finally{close(s);}}

    @SuppressWarnings("unchecked")
    public Result backfill(SocialRequestContext c,Long defaultFundTypeId,boolean execute){new SocialPrivilegeGuard().require(c,SocialPrivilegeGuard.ADMIN);if(!execute)throw new IllegalArgumentException("Backfill hanya berjalan bila execute=true setelah preview disetujui.");Session s=null;Transaction tx=null;try{s=HibernateUtil.openSession();tx=s.beginTransaction();JenisDanaSosial fund=(JenisDanaSosial)s.get(JenisDanaSosial.class,defaultFundTypeId);if(fund==null||!c.getTenantKey().equals(fund.getTenantKey()))throw new IllegalArgumentException("Default jenis dana tidak valid untuk tenant.");int donorCreated=0,programCreated=0,rejected=0;List<String> rejection=new ArrayList<String>();List<Donatur> donors=s.createCriteria(Donatur.class).addOrder(Order.asc("id")).list();for(Donatur d:donors){SocialDonorIdentity old=(SocialDonorIdentity)s.createCriteria(SocialDonorIdentity.class).add(Restrictions.eq("tenantKey",c.getTenantKey())).add(Restrictions.eq("donatur",d)).setMaxResults(1).uniqueResult();if(old!=null)continue;if(d.getNama()==null||d.getNama().trim().isEmpty()){rejected++;rejection.add("DONATUR:"+d.getId()+":nama kosong");continue;}SocialDonorIdentity i=new SocialDonorIdentity();i.setTenantKey(c.getTenantKey());i.setDonatur(d);i.setDisplayName(d.getNama());i.setEmail(d.getEmail());i.setPhone(d.getTelp());i.setDonorType("LEGACY");i.setExternalMember(Boolean.TRUE);i.setCommunicationConsent(Boolean.FALSE);i.setStatus(Boolean.FALSE.equals(d.getAktif())?"INACTIVE":"ACTIVE");i.setCreatedBy(c.getActorId());s.save(i);donorCreated++;}
        List<ProgramDonatur> programs=s.createCriteria(ProgramDonatur.class).addOrder(Order.asc("id")).list();for(ProgramDonatur p:programs){SocialProgramExtension old=(SocialProgramExtension)s.createCriteria(SocialProgramExtension.class).add(Restrictions.eq("program",p)).setMaxResults(1).uniqueResult();if(old!=null)continue;if(p.getNama()==null||p.getNama().trim().isEmpty()){rejected++;rejection.add("PROGRAM:"+p.getId()+":nama kosong");continue;}SocialProgramExtension e=new SocialProgramExtension();e.setTenantKey(c.getTenantKey());e.setProgram(p);e.setFundType(fund);e.setSlug(uniqueSlug(s,c.getTenantKey(),p.getKode()==null?p.getNama():p.getKode(),p.getId()));e.setShortDescription(shorten(p.getKeterangan(),500));e.setLongStory(p.getKeterangan());e.setPublicStatus("DRAFT");e.setFeatured(Boolean.FALSE);e.setRestricted(fund.getRestricted());e.setAllowAnonymous(Boolean.FALSE);e.setStatus("DRAFT");e.setCreatedBy(c.getActorId());s.save(e);programCreated++;}tx.commit();return new Result(donorCreated,programCreated,rejected,rejection);}catch(RuntimeException e){if(tx!=null)try{tx.rollback();}catch(Exception ignored){}throw e;}finally{close(s);}}

    private String uniqueSlug(Session s,String tenant,String value,Long id){String slug=(value==null?"program":value).toLowerCase().replaceAll("[^a-z0-9]+","-").replaceAll("^-|-$","");if(slug.isEmpty())slug="program";if(slug.length()>160)slug=slug.substring(0,160);slug=slug+"-"+id;Number n=(Number)s.createCriteria(SocialProgramExtension.class).add(Restrictions.eq("tenantKey",tenant)).add(Restrictions.eq("slug",slug)).setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();return n!=null&&n.longValue()>0?slug+"-legacy":slug;}
    private String shorten(String v,int max){if(v==null)return null;v=v.trim();return v.length()>max?v.substring(0,max):v;} private long count(Session s,Class type){Number n=(Number)s.createCriteria(type).setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();return n==null?0:n.longValue();} private long countTenant(Session s,Class type,String tenant){Number n=(Number)s.createCriteria(type).add(Restrictions.eq("tenantKey",tenant)).setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();return n==null?0:n.longValue();} private void close(Session s){if(s!=null)try{s.close();}catch(Exception ignored){}}
    /**
     * Tipe implementasi bersarang {@link Preview} milik {@link SocialLegacyMigrationService}. Kelas ini memberi
     * nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * SocialLegacyMigrationService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code long legacyDonors}, {@code long
     * legacyPrograms}, {@code long mappedDonors}, {@code long mappedPrograms}. Aturan bisnis bersama tetap berada
     * pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see SocialLegacyMigrationService
     */
    public static final class Preview {public final long legacyDonors,legacyPrograms,mappedDonors,mappedPrograms;private Preview(long a,long b,long c,long d){legacyDonors=a;legacyPrograms=b;mappedDonors=c;mappedPrograms=d;}}
    /**
     * Pembawa data/helper lokal milik {@link SocialLegacyMigrationService} untuk result. Tipe ini mengelompokkan
     * nilai antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang jelas.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * SocialLegacyMigrationService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code int donorCreated}, {@code int
     * programCreated}, {@code int rejected}, {@code List rejection}. Aturan bisnis bersama tetap berada pada kelas
     * induk atau service yang dipanggilnya.</p>
     *
     * @see SocialLegacyMigrationService
     */
    public static final class Result {public final int donorCreated,programCreated,rejected;public final List<String> rejection;private Result(int a,int b,int c,List<String> d){donorCreated=a;programCreated=b;rejected=c;rejection=d;}}
}
