package ais.database.hibernate;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.Status;
import org.hibernate.event.FlushEntityEvent;
import org.hibernate.event.def.DefaultFlushEntityEventListener;
import org.hibernate.persister.entity.EntityPersister;

import ais.database.model.GeneralValueObject;

/**
 * Penjaga terakhir SEBELUM Hibernate menjadwalkan SQL UPDATE.
 *
 * Berbeda dengan AuditTimestampInterceptor (hanya aktif jika session dibuka
 * lewat HibernateUtil.openSession) dan SaveOrUpdateAuditEventListener (hanya
 * aktif untuk session.saveOrUpdate), listener ini terdaftar di SessionFactory
 * pada event "flush-entity" sehingga berlaku untuk SEMUA session — termasuk
 * session yang dibuka oleh ZK (zkplus OpenSessionInView) tanpa interceptor,
 * dan update transparan via dirty-check entity attached saat flush/commit.
 *
 * Dua jalur yang dijaga:
 * 1. dirtyProperties TERISI (entity attached / merge): jika semua kolom dirty
 *    hanyalah tanggal_dirubah/olehId/oleh, update dibatalkan.
 * 2. dirtyProperties NULL (entity detached di-update()/saveOrUpdate() tanpa
 *    snapshot): Hibernate 3.6 normalnya langsung UPDATE SEMUA kolom tanpa
 *    membandingkan apa pun. Listener ini mengambil snapshot row dari database
 *    lalu membandingkan sendiri; jika tidak ada perubahan bisnis, update
 *    dibatalkan total.
 *
 * Hasil pembatalan: tidak ada SQL UPDATE, tidak ada baris Envers, tidak ada
 * row lock pemicu deadlock.
 */
public class FlushEntityAuditEventListener extends DefaultFlushEntityEventListener {

	private static final long serialVersionUID = 1L;

	private static final int[] TANPA_PERUBAHAN = new int[0];

	/**
	 * Log satu baris setiap kali ada UPDATE yang dibatalkan. Sengaja selalu
	 * aktif (tidak ikut AuditTrailHelper.debug) supaya mudah dibuktikan di
	 * catalina.out bahwa guard benar-benar bekerja. Set false jika sudah stabil.
	 */
	public static boolean logPembatalan = true;

	public FlushEntityAuditEventListener() {
		/*
		 * Banner sekali saat SessionFactory dibangun. Jika baris ini TIDAK muncul
		 * di log startup Tomcat, berarti hibernate.cfg.xml / class hasil compile
		 * yang baru BELUM ikut ter-deploy ke runtime.
		 */
		System.out.println("[AIS-AUDIT] FlushEntityAuditEventListener AKTIF — "
				+ "guard pembatalan UPDATE kolom audit (tanggal_dirubah/olehId/oleh) terpasang di event flush-entity.");
	}

	@Override
	protected void dirtyCheck(FlushEntityEvent event) throws HibernateException {
		super.dirtyCheck(event);
		try {
			batalkanUpdateJikaHanyaKolomAudit(event);
		} catch (Exception e) {
			// Fail-open: jika guard gagal, biarkan update berjalan agar data tidak hilang.
			AuditTrailHelper.debug("FlushEntityAuditEventListener gagal memeriksa dirty, update dibiarkan", e);
		}
	}

	private void batalkanUpdateJikaHanyaKolomAudit(FlushEntityEvent event) {
		Object entity = event.getEntity();
		if (!(entity instanceof GeneralValueObject) || !AuditTrailHelper.isAuditable(entity)) {
			return;
		}
		EntityEntry entry = event.getEntityEntry();
		if (entry == null || entry.getStatus() != Status.MANAGED) {
			// Entity yang sedang dihapus/loading jangan disentuh.
			return;
		}
		EntityPersister persister = entry.getPersister();
		Object[] currentState = event.getPropertyValues();
		if (persister == null || currentState == null) {
			return;
		}
		String[] propertyNames = persister.getPropertyNames();
		int[] dirtyProperties = event.getDirtyProperties();

		if (dirtyProperties != null) {
			// Jalur 1: dirty-check normal berhasil dihitung.
			if (dirtyProperties.length == 0) {
				return; // memang tidak ada update
			}
			for (int i = 0; i < dirtyProperties.length; i++) {
				int index = dirtyProperties[i];
				if (index < 0 || index >= propertyNames.length
						|| !AuditTrailHelper.isIgnoredUpdateProperty(propertyNames[index])) {
					return; // ada perubahan bisnis sungguhan: update + audit jalan normal
				}
			}
			Object[] previousState = entry.getLoadedState();
			if (previousState == null) {
				previousState = event.getDatabaseSnapshot();
			}
			if (previousState != null) {
				AuditTrailHelper.restoreIgnoredUpdateProperties(entity, currentState, previousState, propertyNames);
			}
			event.setDirtyProperties(TANPA_PERUBAHAN);
			logCancel(entity, entry, "dirty-check");
			return;
		}

		/*
		 * Jalur 2: dirtyProperties == null artinya dirty-check TIDAK MUNGKIN
		 * (entity detached di-reattach tanpa loadedState/snapshot). Pada kondisi
		 * ini DefaultFlushEntityEventListener.isUpdateNecessary() mengembalikan
		 * true tanpa syarat -> Hibernate UPDATE semua kolom walau tidak ada satu
		 * pun nilai yang berubah. Bandingkan sendiri dengan row di database.
		 */
		Serializable id = entry.getId();
		if (id == null) {
			return;
		}
		Object[] snapshot;
		try {
			snapshot = event.getSession().getPersistenceContext().getDatabaseSnapshot(id, persister);
		} catch (Exception e) {
			AuditTrailHelper.debug("Gagal mengambil snapshot database untuk "
					+ AuditTrailHelper.describeEntity(entity, id) + ", update dibiarkan", e);
			return;
		}
		if (snapshot == null) {
			return; // row belum ada di DB, biarkan Hibernate memutuskan
		}
		if (AuditTrailHelper.hasBusinessChange(currentState, snapshot, propertyNames)) {
			return; // ada perubahan bisnis: update + audit jalan normal
		}
		AuditTrailHelper.restoreIgnoredUpdateProperties(entity, currentState, snapshot, propertyNames);
		event.setDirtyProperties(TANPA_PERUBAHAN);
		// Tanpa flag ini isUpdateNecessary() tetap menganggap update wajib.
		event.setDirtyCheckPossible(true);
		logCancel(entity, entry, "detached-snapshot");
	}

	private void logCancel(Object entity, EntityEntry entry, String jalur) {
		if (!logPembatalan) {
			return;
		}
		try {
			System.out.println("[AIS-AUDIT] UPDATE dibatalkan (hanya kolom audit yang berubah, jalur=" + jalur + ") "
					+ AuditTrailHelper.describeEntity(entity, entry == null ? null : entry.getId()));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/hibernate/FlushEntityAuditEventListener.java:151");
			// Jangan ganggu flush hanya karena log gagal.
		}
	}
}
