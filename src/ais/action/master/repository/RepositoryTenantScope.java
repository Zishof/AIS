package ais.action.master.repository;

/** Stable repository scope derived from the institution selected for the current domain/request. */
public final class RepositoryTenantScope {
    private RepositoryTenantScope(){}
    public static String currentKey(){
        try{ais.database.model.sekolah.Sekolah s=ais.action.master.sekolah.util.SekolahUtil.getSekolah();if(s!=null&&s.getId()!=null)return "SEKOLAH:"+s.getId();}catch(Exception ignored){}
        try{ais.database.model.PerguruanTinggi p=ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi();if(p!=null&&p.getId()!=null)return "PT:"+p.getId();}catch(Exception ignored){}
        return "AIS:DEFAULT";
    }

	/**
	 * Compatibility hook retained for existing callers.
	 *
	 * <p>Schema creation belongs to Hibernate.  This method deliberately performs no
	 * database mutation: assigning every legacy row with an empty tenant to whichever
	 * institution happens to issue the first request can leak records across tenants.
	 * RepositorySyncService assigns the tenant per source record while synchronizing,
	 * where the owning institution is known.</p>
	 */
	public static void ensureSchema() {
		// Intentionally empty. Do not reintroduce runtime DDL or bulk tenant backfill.
	}
}
