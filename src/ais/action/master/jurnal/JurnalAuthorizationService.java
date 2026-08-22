package ais.action.master.jurnal;

import ais.common.JurnalAksesKatalog;
import ais.database.model.Menu;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;

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
