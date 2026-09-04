package ais.action.servlet.api;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.KodeTransaksiMedis;

/**
 * Memastikan kode ledger wajib POS Apotik tersedia tanpa bergantung pada urutan
 * seed atau init saat Tomcat hidup.
 *
 * <p>Operasi diserialkan dan idempoten: kode yang sudah ada hanya dibaca,
 * sedangkan kode yang belum ada dibuat tepat sekali lalu dipasang ke
 * {@link ConstantValues} agar modul lama ikut melihatnya tanpa restart.</p>
 */
final class ApotikKodeTransaksiHelper {

	private static final Object LOCK = new Object();

	private ApotikKodeTransaksiHelper() {
	}

	static Long pastikanId(String kode, String nama, int jenis) throws Exception {
		KodeTransaksiMedis cached = dariConstant(kode);
		if (cached != null && cached.getId() != null) {
			return cached.getId();
		}
		synchronized (LOCK) {
			cached = dariConstant(kode);
			if (cached != null && cached.getId() != null) {
				return cached.getId();
			}
			Session session = HibernateUtil.getSessionFactory().openSession();
			Transaction tx = session.beginTransaction();
			try {
				KodeTransaksiMedis nilai = (KodeTransaksiMedis) session
						.createCriteria(KodeTransaksiMedis.class)
						.add(Restrictions.eq("kode", kode))
						.setMaxResults(1).uniqueResult();
				if (nilai == null) {
					nilai = new KodeTransaksiMedis();
					nilai.setKode(kode);
					nilai.setNama(nama);
					nilai.setJenis(Integer.valueOf(jenis));
					nilai.setKeterangan("Dibuat otomatis oleh POS Apotik");
					session.save(nilai);
					session.flush();
				}
				tx.commit();
				pasangConstant(kode, nilai);
				return nilai.getId();
			} catch (Exception e) {
				try { if (tx.isActive()) tx.rollback(); } catch (Exception ignored) { }
				throw e;
			} finally {
				HibernateUtil.closeSessionQuietly(session);
			}
		}
	}

	private static KodeTransaksiMedis dariConstant(String kode) {
		if ("AJ".equals(kode)) return ConstantValues.apotikJual;
		if ("PROD".equals(kode)) return ConstantValues.produksi;
		if ("BB".equals(kode)) return ConstantValues.bahanBaku;
		if ("RAC".equals(kode)) return ConstantValues.jasaRacik;
		return null;
	}

	private static void pasangConstant(String kode, KodeTransaksiMedis nilai) {
		if ("AJ".equals(kode)) ConstantValues.apotikJual = nilai;
		else if ("PROD".equals(kode)) ConstantValues.produksi = nilai;
		else if ("BB".equals(kode)) ConstantValues.bahanBaku = nilai;
		else if ("RAC".equals(kode)) ConstantValues.jasaRacik = nilai;
	}
}
