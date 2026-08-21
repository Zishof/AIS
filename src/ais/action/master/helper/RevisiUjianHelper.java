package ais.action.master.helper;

import java.util.List;

import org.hibernate.ReplicationMode;
import org.hibernate.Session;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.zkoss.zk.ui.event.EventListener;

import ais.database.model.BankSoal;
import ais.database.model.Ujian;
import ais.database.model.UjianPunyaSoal;

/**
 * Riwayat Ujian dengan restore master-detail yang atomik.
 *
 * Ujian tidak memiliki collection Hibernate ke UjianPunyaSoal, sehingga restore
 * generik tidak dapat menemukan detail tersebut. Helper ini membaca snapshot
 * detail pada nomor revisi yang dipilih dan mengembalikan relasi soal yang hilang
 * di transaksi yang sama dengan master Ujian.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiUjianHelper extends GenericRevisiHelper<Ujian> {

	private static final long serialVersionUID = 7184837931738281177L;
	private static final String[] SEARCH_PROPERTIES = new String[] { "nama", "kode", "keterangan" };

	public RevisiUjianHelper(EventListener callback) throws Exception {
		super(Ujian.class, "Riwayat Ujian dan Soal", callback, SEARCH_PROPERTIES);
	}

	@Override
	protected void afterRestoreInTransaction(Session session, AuditReader reader, Object entity) throws Exception {
		if (!(entity instanceof Ujian) || ((Ujian) entity).getId() == null) {
			return;
		}

		Ujian restoredUjian = (Ujian) session.get(Ujian.class, ((Ujian) entity).getId());
		Integer revision = getSelectedRestoreRevisionNumber(entity);
		if (restoredUjian == null || revision == null || revision.intValue() < 1) {
			return;
		}

		List snapshots = detailAtRevision(reader, restoredUjian.getId(), revision);
		/* Pada revisi DELETE, state detail sudah dianggap tidak berlaku. Ambil state
		 * tepat sebelum transaksi delete agar master beserta seluruh soalnya pulih. */
		if ((snapshots == null || snapshots.isEmpty()) && revision.intValue() > 1
				&& isDeleteRevision(reader, restoredUjian.getId(), revision)) {
			snapshots = detailAtRevision(reader, restoredUjian.getId(),
					Integer.valueOf(revision.intValue() - 1));
		}

		for (int i = 0; snapshots != null && i < snapshots.size(); i++) {
			Object value = snapshots.get(i);
			if (!(value instanceof UjianPunyaSoal)) {
				continue;
			}
			UjianPunyaSoal snapshot = (UjianPunyaSoal) value;
			if (snapshot.getId() == null
					|| session.get(UjianPunyaSoal.class, snapshot.getId()) != null) {
				continue;
			}

			BankSoal bankSoal = snapshot.getBankSoal();
			BankSoal managedBankSoal = bankSoal == null || bankSoal.getId() == null ? null
					: (BankSoal) session.get(BankSoal.class, bankSoal.getId());
			if (managedBankSoal == null) {
				throw new IllegalStateException("Soal ujian tidak dapat direstore karena Bank Soal ID "
						+ (bankSoal == null ? "-" : bankSoal.getId()) + " sudah tidak tersedia.");
			}

			snapshot.setUjian(restoredUjian);
			snapshot.setBankSoal(managedBankSoal);
			session.replicate(snapshot, ReplicationMode.OVERWRITE);
		}

		session.flush();
		/* Cache file akan dibangun ulang dari database ketika daftar soal dibuka. */
		restoredUjian.bersihkanLokasiUjianPunyaSoal();
	}

	private List detailAtRevision(AuditReader reader, Long ujianId, Integer revision) {
		AuditQuery query = reader.createQuery().forEntitiesAtRevision(UjianPunyaSoal.class, revision);
		query.add(AuditEntity.relatedId("ujian").eq(ujianId));
		return query.getResultList();
	}

	private boolean isDeleteRevision(AuditReader reader, Long ujianId, Integer revision) {
		AuditQuery query = reader.createQuery().forRevisionsOfEntity(Ujian.class, false, true);
		query.add(AuditEntity.id().eq(ujianId));
		query.add(AuditEntity.revisionNumber().eq(revision));
		List rows = query.getResultList();
		for (int i = 0; rows != null && i < rows.size(); i++) {
			if (extractRevisionType(rows.get(i)) == RevisionType.DEL) {
				return true;
			}
		}
		return false;
	}
}
