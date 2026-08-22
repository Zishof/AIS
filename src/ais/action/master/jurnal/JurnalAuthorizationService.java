package ais.action.master.jurnal;

import ais.common.JurnalAksesKatalog;
import ais.database.model.Menu;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import java.util.Date;
import org.hibernate.Query;
import org.hibernate.Session;
import ais.database.model.penelitiandanpengabdian.JurnalPenelitian;
import ais.database.model.repository.RepoItem;

/** Active-role + menu + capability gate. Object scope is checked separately. */
public final class JurnalAuthorizationService {
    public boolean canRead(Tbmuser user, String resource) {
        return canMenu(user, resource) && JurnalAksesKatalog.bolehCrud(raw(user), resource, "read");
    }
    public boolean canCrud(Tbmuser user, String resource, String action) {
        return canMenu(user, resource) && JurnalAksesKatalog.bolehCrud(raw(user), resource, action);
    }
    public boolean canWorkflow(Tbmuser user, String action) {
        Tbmrole role=activeRole(user);
        return role!=null && hasJournalModule(role) && JurnalAksesKatalog.bolehWorkflow(role.getJurnalAksesJson(),action);
    }
    public boolean canMenu(Tbmuser user, String resource) {
        Tbmrole role=activeRole(user);
        return role!=null && hasJournalModule(role) && JurnalAksesKatalog.bolehMenu(role.getJurnalAksesJson(),resource);
    }
    public void requireRead(Tbmuser user,String resource){if(!canRead(user,resource))throw new SecurityException("Hak akses jurnal tidak tersedia.");}
    public void requireCrud(Tbmuser user,String resource,String action){if(!canCrud(user,resource,action))throw new SecurityException("Hak akses jurnal tidak tersedia.");}
    public void requireWorkflow(Tbmuser user,String action){if(!canWorkflow(user,action))throw new SecurityException("Hak akses workflow jurnal tidak tersedia.");}

    /**
     * Scope object jurnal. Administrator aktif dapat lintas jurnal, sedangkan
     * role lain wajib mempunyai penugasan aktif pada jurnal/item atau menjadi
     * owner bila command tersebut secara eksplisit mengizinkan owner.
     */
    public void requireJournalScope(Session session,Tbmuser user,Long journalId,Long itemId,
            String ownerId,boolean ownerAllowed,String stage){
        if(session==null||user==null||journalId==null)throw new SecurityException("Scope jurnal tidak lengkap.");
        if(isAdministrator(user))return;
        String actor=user.getUserId();
        if(ownerAllowed&&actor!=null&&actor.equals(ownerId))return;
        StringBuilder hql=new StringBuilder("select count(*) from PenugasanTahapJurnal where aktif=true and status='ACTIVE' and jurnalPenelitianId=:j and userId=:u and startsAt<=:now and (endsAt is null or endsAt>:now) and (itemId is null");
        if(itemId!=null)hql.append(" or itemId=:i");
        hql.append(")");
        if(stage!=null&&stage.trim().length()>0)hql.append(" and (stageKey='ALL' or stageKey='JOURNAL' or stageKey=:stage)");
        Query query=session.createQuery(hql.toString()).setLong("j",journalId).setString("u",actor).setTimestamp("now",new Date());
        if(itemId!=null)query.setLong("i",itemId);
        if(stage!=null&&stage.trim().length()>0)query.setString("stage",stage.trim().toUpperCase());
        Number count=(Number)query.uniqueResult();
        if(count==null||count.longValue()<1L)throw new SecurityException("Objek jurnal berada di luar penugasan aktif.");
    }

    public void requireAdministrator(Tbmuser user){
        if(!isAdministrator(user))throw new SecurityException("Hanya administrator aktif yang diizinkan.");
    }

    public Long requireItemScope(Session session,Tbmuser user,RepoItem item,boolean ownerAllowed,String stage){
        if(item==null||item.getCollectionId()==null)throw new SecurityException("Scope item jurnal tidak lengkap.");
        Query query=session.createQuery("from JurnalPenelitian where repoCollectionId=:c and aktif=true");
        query.setLong("c",item.getCollectionId());query.setMaxResults(1);
        JurnalPenelitian journal=(JurnalPenelitian)query.uniqueResult();
        if(journal==null)throw new SecurityException("Item tidak terhubung ke jurnal aktif.");
        if(journal.getTenantKey()!=null&&journal.getTenantKey().trim().length()>0
                && item.getTenantKey()!=null&&item.getTenantKey().trim().length()>0
                && !journal.getTenantKey().trim().equals(item.getTenantKey().trim()))
            throw new SecurityException("Item berada di tenant lain.");
        requireJournalScope(session,user,journal.getId(),item.getId(),item.getOwnerId(),ownerAllowed,stage);
        return journal.getId();
    }

    public Long requireCollectionScope(Session session,Tbmuser user,Long collectionId,String stage){
        if(collectionId==null)throw new SecurityException("Scope collection jurnal tidak lengkap.");
        Query query=session.createQuery("from JurnalPenelitian where repoCollectionId=:c and aktif=true");
        query.setLong("c",collectionId);query.setMaxResults(1);
        JurnalPenelitian journal=(JurnalPenelitian)query.uniqueResult();
        if(journal==null)throw new SecurityException("Collection tidak terhubung ke jurnal aktif.");
        requireJournalScope(session,user,journal.getId(),null,null,false,stage);
        return journal.getId();
    }

    public boolean isAdministrator(Tbmuser user){
        Tbmrole role=activeRole(user);
        return role!=null&&Tbmrole.ADMINISTRATOR.equalsIgnoreCase(role.getRoleId());
    }

    public void requireObjectScope(String expectedTenant,String actualTenant,Long expectedJournal,Long actualJournal,
            String actorId,String ownerId,boolean assignedOrManager){
        if(expectedTenant==null||actualTenant==null||!expectedTenant.equals(actualTenant))throw new SecurityException("Objek berada di tenant lain.");
        if(expectedJournal!=null&&(actualJournal==null||!expectedJournal.equals(actualJournal)))throw new SecurityException("Objek berada di jurnal lain.");
        if(!assignedOrManager&&(actorId==null||!actorId.equals(ownerId)))throw new SecurityException("Objek berada di luar penugasan.");
    }

    private boolean hasJournalModule(Tbmrole role){
        try{
            if(role.getMenus()==null)return false;
            for(Menu menu:role.getMenus())if(menu!=null&&menu.getId()!=null&&menu.getId().longValue()>=2000460500L&&menu.getId().longValue()<=2000460528L)return true;
        }catch(Exception e){return false;}
        return false;
    }
    private String raw(Tbmuser user){Tbmrole role=activeRole(user);return role==null?null:role.getJurnalAksesJson();}
    private Tbmrole activeRole(Tbmuser user){try{return user==null?null:user.hakAkses();}catch(Exception e){return null;}}
}
