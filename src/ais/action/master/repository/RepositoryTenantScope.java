package ais.action.master.repository;

import org.hibernate.Session;
import org.hibernate.Transaction;

import ais.database.hibernate.HibernateUtil;

/** Stable repository scope derived from the institution selected for the current domain/request. */
public final class RepositoryTenantScope {
	private static volatile boolean schemaReady;
    private RepositoryTenantScope(){}
    public static String currentKey(){
        try{ais.database.model.sekolah.Sekolah s=ais.action.master.sekolah.util.SekolahUtil.getSekolah();if(s!=null&&s.getId()!=null)return "SEKOLAH:"+s.getId();}catch(Exception ignored){}
        try{ais.database.model.PerguruanTinggi p=ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi();if(p!=null&&p.getId()!=null)return "PT:"+p.getId();}catch(Exception ignored){}
        return "AIS:DEFAULT";
    }

	/**
	 * Migrasi idempoten untuk instalasi lama yang tabel repository-nya dibuat sebelum
	 * dukungan multi-tenant. Dijalankan sekali per JVM memakai session mandiri agar DDL
	 * tidak merusak transaksi request/scheduler milik pemanggil.
	 */
	public static void ensureSchema() {
		if (schemaReady) return;
		synchronized (RepositoryTenantScope.class) {
			if (schemaReady) return;
			String tenant = currentKey();
			Session session = null;
			Transaction transaction = null;
			try {
				session = HibernateUtil.openSession();
				transaction = session.beginTransaction();
				session.createSQLQuery("alter table if exists public.repo_collection add column if not exists tenant_key varchar(120)").executeUpdate();
				session.createSQLQuery("alter table if exists public.repo_item add column if not exists tenant_key varchar(120)").executeUpdate();
				session.createSQLQuery("update public.repo_collection set tenant_key = :tenant where tenant_key is null or trim(tenant_key) = ''")
						.setString("tenant", tenant).executeUpdate();
				session.createSQLQuery("update public.repo_item set tenant_key = :tenant where tenant_key is null or trim(tenant_key) = ''")
						.setString("tenant", tenant).executeUpdate();
				transaction.commit();
				schemaReady = true;
			} catch (RuntimeException e) {
				if (transaction != null && transaction.isActive()) {
					try { transaction.rollback(); } catch (Exception ignored) {}
				}
				throw e;
			} finally {
				HibernateUtil.closeSessionQuietly(session);
			}
		}
	}
}
