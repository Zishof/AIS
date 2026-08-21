package ais.action.master.repository;

/** Stable repository scope derived from the institution selected for the current domain/request. */
public final class RepositoryTenantScope {
    private RepositoryTenantScope(){}
    public static String currentKey(){
        try{ais.database.model.sekolah.Sekolah s=ais.action.master.sekolah.util.SekolahUtil.getSekolah();if(s!=null&&s.getId()!=null)return "SEKOLAH:"+s.getId();}catch(Exception ignored){}
        try{ais.database.model.PerguruanTinggi p=ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi();if(p!=null&&p.getId()!=null)return "PT:"+p.getId();}catch(Exception ignored){}
        return "AIS:DEFAULT";
    }
}
