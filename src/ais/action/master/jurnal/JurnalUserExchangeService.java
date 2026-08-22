package ais.action.master.jurnal;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.jurnal.PenugasanTahapJurnal;
import ais.database.model.penelitiandanpengabdian.JurnalPenelitian;

/** Privacy-bounded user-role exchange: existing accounts are exported; imports create invitations only. */
public final class JurnalUserExchangeService {
    private final JurnalAuthorizationService auth=new JurnalAuthorizationService();
    @SuppressWarnings("unchecked") public void exportCsv(Long journalId,Writer out,Tbmuser actor)throws IOException{auth.requireCrud(actor,"people","read");Session s=HibernateUtil.currentSession();journal(s,journalId,actor);out.write("user_id,email,role,scope,scope_key,status\n");int page=0;while(page<1000){Query q=s.createQuery("from PenugasanTahapJurnal where jurnalPenelitianId=:j and aktif=true order by id");q.setLong("j",journalId);q.setFirstResult(page*200);q.setMaxResults(200);List<PenugasanTahapJurnal> rows=q.list();for(PenugasanTahapJurnal x:rows){Object email=s.createSQLQuery("select email from public.tbmuser where userid=:u").setString("u",x.getUserId()).setMaxResults(1).uniqueResult();out.write(csv(x.getUserId())+","+csv(email==null?"":String.valueOf(email))+","+csv(x.getRoleKey())+","+csv(x.getStageKey())+","+csv(x.getSectionKey())+","+csv(x.getStatus())+"\n");}if(rows.size()<200)break;page++;}}
    public List<JurnalInvitationService.Issued> importInvitations(Long journalId,String csv,Tbmuser actor){auth.requireCrud(actor,"people","create");if(csv==null||csv.length()==0||csv.length()>1048576)throw new IllegalArgumentException("Ukuran CSV pengguna tidak valid.");List<String[]> rows=parse(csv);if(rows.size()<2||rows.size()>1001)throw new IllegalArgumentException("Jumlah baris CSV pengguna tidak valid.");String[] h=rows.get(0);if(h.length!=4||!"email".equalsIgnoreCase(h[0])||!"role".equalsIgnoreCase(h[1])||!"scope".equalsIgnoreCase(h[2])||!"scope_key".equalsIgnoreCase(h[3]))throw new IllegalArgumentException("Header CSV pengguna harus email,role,scope,scope_key.");Session s=HibernateUtil.currentSession();Transaction tx=s.getTransaction();boolean own=!tx.isActive();List<JurnalInvitationService.Issued> out=new ArrayList<JurnalInvitationService.Issued>();try{if(own)tx.begin();journal(s,journalId,actor);JurnalInvitationService invitations=new JurnalInvitationService();for(int i=1;i<rows.size();i++){String[] r=rows.get(i);if(r.length!=4)throw new IllegalArgumentException("Kolom CSV pengguna tidak konsisten pada baris "+(i+1)+".");out.add(invitations.issue(journalId,null,r[0],r[1],r[2],r[3],604800000L,actor));}if(own)tx.commit();return out;}catch(RuntimeException e){if(own&&tx.isActive())tx.rollback();throw e;}}
    private JurnalPenelitian journal(Session s,Long id,Tbmuser actor){JurnalPenelitian j=(JurnalPenelitian)s.get(JurnalPenelitian.class,id);if(j==null||!Boolean.TRUE.equals(j.getAktif()))throw new IllegalArgumentException("Jurnal tidak ditemukan.");auth.requireJournalScope(s,actor,id,null,null,false,"JOURNAL");return j;}
    private static List<String[]> parse(String text){List<String[]>out=new ArrayList<String[]>();List<String>row=new ArrayList<String>();StringBuilder cell=new StringBuilder();boolean quoted=false;for(int i=0;i<text.length();i++){char c=text.charAt(i);if(quoted){if(c=='\"'&&i+1<text.length()&&text.charAt(i+1)=='\"'){cell.append('\"');i++;}else if(c=='\"')quoted=false;else cell.append(c);}else if(c=='\"'&&cell.length()==0)quoted=true;else if(c==','){row.add(cell.toString().trim());cell.setLength(0);}else if(c=='\n'){row.add(cell.toString().trim());cell.setLength(0);if(!(row.size()==1&&row.get(0).length()==0))out.add(row.toArray(new String[row.size()]));row=new ArrayList<String>();}else if(c!='\r')cell.append(c);}if(quoted)throw new IllegalArgumentException("Quote CSV pengguna tidak tertutup.");row.add(cell.toString().trim());if(!(row.size()==1&&row.get(0).length()==0))out.add(row.toArray(new String[row.size()]));return out;}
    private static String csv(String v){String x=v==null?"":v;if(x.startsWith("=")||x.startsWith("+")||x.startsWith("-")||x.startsWith("@"))x="'"+x;return"\""+x.replace("\"","\"\"").replace("\r"," ").replace("\n"," ")+"\"";}
}
