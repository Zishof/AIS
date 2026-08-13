package ais.common.newui.sekolah;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.InterviewCalonSiswa;
import ais.database.model.sekolah.InterviewPunyaCalonSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

/** Headless parity InterviewCalonSiswaAction tanpa komponen ZK. */
@SuppressWarnings({"rawtypes","unchecked"})
public final class NewUiInterviewCalonSiswaService {
    public Snapshot load(Sekolah school, Yayasan foundation, Long waveId) {
        Session session=HibernateUtil.openSession();
        try {
            Snapshot result=new Snapshot(); Criteria sessions=session.createCriteria(InterviewCalonSiswa.class);
            scope(sessions,school,foundation); if(waveId!=null) sessions.add(Restrictions.eq("gelombangPendaftaranPsb.id",waveId));
            List rows=sessions.addOrder(Order.desc("id")).list();
            for(int i=0;i<rows.size();i++) result.sessions.add(sessionRow(session,(InterviewCalonSiswa)rows.get(i)));
            Criteria employees=session.createCriteria(Pegawai.class).add(Restrictions.or(Restrictions.isNull("aktif"),Restrictions.eq("aktif",Boolean.TRUE))).addOrder(Order.asc("nama")).setMaxResults(500);
            List employeeRows=employees.list();for(int i=0;i<employeeRows.size();i++)result.employees.add(new Option((Pegawai)employeeRows.get(i)));
            Criteria waves=session.createCriteria(GelombangPendaftaranPsb.class).add(Restrictions.or(Restrictions.isNull("aktif"),Restrictions.eq("aktif",Boolean.TRUE)));
            if(school!=null&&school.getId()!=null)waves.add(Restrictions.eq("sekolah",school));else if(foundation!=null&&foundation.getId()!=null)waves.add(Restrictions.eq("yayasan",foundation));
            List waveRows=waves.addOrder(Order.desc("id")).setMaxResults(100).list();for(int i=0;i<waveRows.size();i++)result.waves.add(new Option((GelombangPendaftaranPsb)waveRows.get(i)));
            return result;
        } finally {session.close();}
    }

    public SessionRow saveSession(Long id,String name,String academicYear,Date start,Date end,int platform,
            String zoom,String bbb,String skype,String wa,String other,Integer capacity,String note,
            Long employeeId,Long waveId,Sekolah school,Yayasan foundation,Tbmuser user) {
        validateSession(name,start,end,platform,capacity,zoom,bbb,skype,wa,other);
        Session session=HibernateUtil.openSession();Transaction tx=null;
        try {tx=session.beginTransaction();InterviewCalonSiswa value=id==null?new InterviewCalonSiswa():(InterviewCalonSiswa)session.get(InterviewCalonSiswa.class,id);
            if(value==null)throw new IllegalArgumentException("Sesi wawancara tidak ditemukan.");ensureScope(value,school,foundation,id==null);
            Pegawai employee=employeeId==null?null:(Pegawai)session.get(Pegawai.class,employeeId);if(employeeId!=null&&employee==null)throw new IllegalArgumentException("Pegawai pewawancara tidak ditemukan.");
            GelombangPendaftaranPsb wave=waveId==null?null:(GelombangPendaftaranPsb)session.get(GelombangPendaftaranPsb.class,waveId);if(waveId!=null&&wave==null)throw new IllegalArgumentException("Gelombang PSB tidak ditemukan.");
            if(wave!=null&&!sameScope(wave.getSekolah(),wave.getYayasan(),school,foundation))throw new IllegalArgumentException("Gelombang PSB berada di luar sekolah aktif.");
            value.setNama(name.trim());value.setTahunAjaran(clean(academicYear));value.setMulai(start);value.setSampai(end);value.setOnlineMenggunakan(Integer.valueOf(platform));
            value.setZoomLink(clean(zoom));value.setBbbLink(clean(bbb));value.setSkypeLink(clean(skype));value.setWaLink(clean(wa));value.setLainLink(clean(other));value.setKapasitasRuangan(capacity);value.setKeterangan(clean(note));value.setPegawai(employee);value.setGelombangPendaftaranPsb(wave);value.setSekolah(school);value.setYayasan(foundation);stamp(value,user);
            session.saveOrUpdate(value);session.flush();SessionRow result=sessionRow(session,value);tx.commit();return result;
        }catch(RuntimeException e){rollback(tx);throw e;}finally{session.close();}
    }

    public void deleteSession(Long id,Sekolah school,Yayasan foundation){Session session=HibernateUtil.openSession();Transaction tx=null;try{tx=session.beginTransaction();InterviewCalonSiswa value=(InterviewCalonSiswa)session.get(InterviewCalonSiswa.class,id);if(value==null)throw new IllegalArgumentException("Sesi wawancara tidak ditemukan.");ensureScope(value,school,foundation,false);List assignments=session.createCriteria(InterviewPunyaCalonSiswa.class).add(Restrictions.eq("interviewCalonSiswa",value)).list();for(int i=0;i<assignments.size();i++)session.delete(assignments.get(i));session.delete(value);session.flush();tx.commit();}catch(RuntimeException e){rollback(tx);throw e;}finally{session.close();}}

    public ParticipantRow addParticipant(Long sessionId,String registration,Date start,Date end,String note,Sekolah school,Yayasan foundation,Tbmuser user){
        if(registration==null||registration.trim().length()==0)throw new IllegalArgumentException("No. registrasi calon siswa wajib diisi.");if(start!=null&&end!=null&&end.before(start))throw new IllegalArgumentException("Waktu selesai khusus tidak boleh sebelum waktu mulai.");
        Session session=HibernateUtil.openSession();Transaction tx=null;try{tx=session.beginTransaction();InterviewCalonSiswa interview=(InterviewCalonSiswa)session.get(InterviewCalonSiswa.class,sessionId);if(interview==null)throw new IllegalArgumentException("Sesi wawancara tidak ditemukan.");ensureScope(interview,school,foundation,false);
            CalonSiswa candidate=(CalonSiswa)session.createCriteria(CalonSiswa.class).add(Restrictions.eq("noRegistrasi",registration.trim())).setMaxResults(1).uniqueResult();if(candidate==null)throw new IllegalArgumentException("Calon siswa dengan nomor registrasi tersebut tidak ditemukan.");
            Number duplicate=(Number)session.createCriteria(InterviewPunyaCalonSiswa.class).add(Restrictions.eq("interviewCalonSiswa",interview)).add(Restrictions.eq("calonSiswa",candidate)).setProjection(Projections.rowCount()).uniqueResult();if(duplicate!=null&&duplicate.intValue()>0)throw new IllegalArgumentException("Calon siswa sudah menjadi peserta sesi ini.");
            Number count=(Number)session.createCriteria(InterviewPunyaCalonSiswa.class).add(Restrictions.eq("interviewCalonSiswa",interview)).setProjection(Projections.rowCount()).uniqueResult();if(interview.getKapasitasRuangan()!=null&&interview.getKapasitasRuangan().intValue()>0&&count.intValue()>=interview.getKapasitasRuangan().intValue())throw new IllegalArgumentException("Kapasitas sesi wawancara sudah penuh.");
            InterviewPunyaCalonSiswa value=new InterviewPunyaCalonSiswa();value.setInterviewCalonSiswa(interview);value.setCalonSiswa(candidate);value.setMulai(start);value.setSampai(end);value.setKeterangan(clean(note));stamp(value,user);session.save(value);session.flush();ParticipantRow result=new ParticipantRow(value);tx.commit();return result;
        }catch(RuntimeException e){rollback(tx);throw e;}finally{session.close();}}

    public void removeParticipant(Long id,Sekolah school,Yayasan foundation){Session session=HibernateUtil.openSession();Transaction tx=null;try{tx=session.beginTransaction();InterviewPunyaCalonSiswa value=(InterviewPunyaCalonSiswa)session.get(InterviewPunyaCalonSiswa.class,id);if(value==null)throw new IllegalArgumentException("Peserta wawancara tidak ditemukan.");ensureScope(value.getInterviewCalonSiswa(),school,foundation,false);session.delete(value);session.flush();tx.commit();}catch(RuntimeException e){rollback(tx);throw e;}finally{session.close();}}

    private SessionRow sessionRow(Session session,InterviewCalonSiswa value){SessionRow row=new SessionRow(value);List participants=session.createCriteria(InterviewPunyaCalonSiswa.class).add(Restrictions.eq("interviewCalonSiswa",value)).addOrder(Order.asc("id")).list();for(int i=0;i<participants.size();i++)row.participants.add(new ParticipantRow((InterviewPunyaCalonSiswa)participants.get(i)));return row;}
    private void validateSession(String name,Date start,Date end,int platform,Integer capacity,String zoom,String bbb,String skype,String wa,String other){if(name==null||name.trim().length()==0)throw new IllegalArgumentException("Nama sesi tidak boleh kosong.");if(start==null||end==null)throw new IllegalArgumentException("Waktu mulai dan selesai wajib diisi.");if(end.before(start))throw new IllegalArgumentException("Waktu selesai tidak boleh sebelum waktu mulai.");if(platform<0||platform>7)throw new IllegalArgumentException("Platform video tidak valid.");if(capacity!=null&&capacity.intValue()<0)throw new IllegalArgumentException("Kapasitas tidak boleh negatif.");if(platform==2&&clean(other)==null)throw new IllegalArgumentException("Tautan Google Meet wajib diisi.");if(platform==3&&clean(zoom)==null)throw new IllegalArgumentException("Tautan Zoom wajib diisi.");if(platform==4&&clean(bbb)==null)throw new IllegalArgumentException("Tautan BigBlueButton wajib diisi.");if(platform==5&&clean(skype)==null)throw new IllegalArgumentException("Tautan Skype wajib diisi.");if(platform==6&&clean(wa)==null)throw new IllegalArgumentException("Tautan WhatsApp wajib diisi.");if(platform==7&&clean(other)==null)throw new IllegalArgumentException("Tautan platform lain wajib diisi.");}
    private void scope(Criteria c,Sekolah s,Yayasan y){if(s!=null&&s.getId()!=null)c.add(Restrictions.eq("sekolah",s));else if(y!=null&&y.getId()!=null)c.add(Restrictions.eq("yayasan",y));else c.add(Restrictions.sqlRestriction("1=0"));}
    private void ensureScope(InterviewCalonSiswa value,Sekolah s,Yayasan y,boolean fresh){if(fresh)return;if(value==null||!sameScope(value.getSekolah(),value.getYayasan(),s,y))throw new IllegalArgumentException("Sesi wawancara berada di luar sekolah aktif.");}
    private boolean sameScope(Sekolah a,Yayasan b,Sekolah s,Yayasan y){if(s!=null&&s.getId()!=null)return a!=null&&s.getId().equals(a.getId());return y!=null&&y.getId()!=null&&b!=null&&y.getId().equals(b.getId());}
    private void stamp(InterviewCalonSiswa v,Tbmuser u){if(u!=null){v.setOleh(u.getUserNama());v.setOlehId(u.getUserId());}}
    private void stamp(InterviewPunyaCalonSiswa v,Tbmuser u){if(u!=null){v.setOleh(u.getUserNama());v.setOlehId(u.getUserId());}}
    private static String clean(String v){return v==null||v.trim().length()==0?null:v.trim();}private void rollback(Transaction t){if(t!=null)try{t.rollback();}catch(Exception ignored){}}
    public static final class Snapshot{public final List<SessionRow> sessions=new ArrayList<SessionRow>();public final List<Option> employees=new ArrayList<Option>();public final List<Option>waves=new ArrayList<Option>();}
    public static final class Option{public final Long id;public final String label;Option(Pegawai v){id=v.getId();label=v.getNama();}Option(GelombangPendaftaranPsb v){id=v.getId();label=v.getNama();}}
    public static final class SessionRow{public final Long id,employeeId,waveId;public final String name,academicYear,employee,wave,zoom,bbb,skype,wa,other,note,conference;public final Date start,end;public final int platform,capacity;public final List<ParticipantRow>participants=new ArrayList<ParticipantRow>();SessionRow(InterviewCalonSiswa v){id=v.getId();name=v.getNama();academicYear=v.getTahunAjaran();start=v.getMulai();end=v.getSampai();platform=v.getOnlineMenggunakan();zoom=v.getZoomLink();bbb=v.getBbbLink();skype=v.getSkypeLink();wa=v.getWaLink();other=v.getLainLink();String link=null;try{if(platform==1)link=v.generateJitsiLink();else if(platform==2||platform==7)link=other;else if(platform==3)link=zoom;else if(platform==4)link=bbb;else if(platform==5)link=skype;else if(platform==6)link=wa;}catch(Exception ignored){}conference=link;capacity=v.getKapasitasRuangan();note=v.getKeterangan();employeeId=v.getPegawai()==null?null:v.getPegawai().getId();employee=v.getPegawai()==null?"-":v.getPegawai().getNama();waveId=v.getGelombangPendaftaranPsb()==null?null:v.getGelombangPendaftaranPsb().getId();wave=v.getGelombangPendaftaranPsb()==null?"-":v.getGelombangPendaftaranPsb().getNama();}}
    public static final class ParticipantRow{public final Long id,candidateId;public final String name,registration,note;public final Date start,end;public final boolean ready;ParticipantRow(InterviewPunyaCalonSiswa v){id=v.getId();candidateId=v.getCalonSiswa()==null?null:v.getCalonSiswa().getId();name=v.getCalonSiswa()==null?"-":v.getCalonSiswa().getNama();registration=v.getCalonSiswa()==null?"-":v.getCalonSiswa().getNoRegistrasi();start=v.getMulai();end=v.getSampai();ready=Boolean.TRUE.equals(v.getSiap());note=v.getKeterangan();}}
}
