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
 * Subclass dari {@link ais.action.master.helper.GenericRevisiHelper} untuk entity {@link Ujian},
 * dengan restore master-detail yang atomik — lihat Javadoc class induk untuk penjelasan lengkap
 * arsitektur window, alur Envers, dan fitur restore secara umum.
 *
 * <p>Kekhasan (berbeda dari subclass tipis lain): {@code Ujian} TIDAK memiliki collection
 * Hibernate ke {@link UjianPunyaSoal} (relasi daftar soal pada satu ujian), sehingga logika
 * restore generik di kelas induk — yang mengandalkan graph relasi Hibernate untuk memulihkan
 * dependensi — tidak dapat menemukan/mengembalikan baris {@code UjianPunyaSoal} yang hilang.
 * Helper ini meng-override {@link #afterRestoreInTransaction(Session, AuditReader, Object)} untuk
 * secara manual membaca snapshot Envers {@code UjianPunyaSoal} pada nomor revisi yang sedang
 * direstore, lalu mereplikasikan baris yang belum ada di database dalam TRANSAKSI YANG SAMA
 * dengan restore master {@code Ujian} — sehingga master dan seluruh soalnya pulih atomik (semua
 * berhasil atau semua batal). Field pencarian: {@code nama}, {@code kode}, {@code keterangan}.
 * Tidak ada filter tambahan (seluruh riwayat ujian ditampilkan). Judul window: "Riwayat Ujian dan
 * Soal".</p>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiUjianHelper extends GenericRevisiHelper<Ujian> {

	private static final long serialVersionUID = 7184837931738281177L;
	private static final String[] SEARCH_PROPERTIES = new String[] { "nama", "kode", "keterangan" };

	/**
	 * Membuka window riwayat revisi Ujian beserta restore master-detail soalnya (seluruh data,
	 * tanpa filter).
	 *
	 * @param callback callback yang diteruskan ke {@link ais.action.master.helper.GenericRevisiHelper}.
	 */
	public RevisiUjianHelper(EventListener callback) throws Exception {
		super(Ujian.class, "Riwayat Ujian dan Soal", callback, SEARCH_PROPERTIES);
	}

	/**
	 * Hook restore tambahan: memulihkan baris {@link UjianPunyaSoal} (relasi ujian-soal) yang
	 * hilang setelah master {@link Ujian} direstore, karena {@code Ujian} tidak punya collection
	 * Hibernate ke {@code UjianPunyaSoal} sehingga restore generik di kelas induk tidak dapat
	 * menjangkau relasi ini.
	 *
	 * <p>Alur: (1) ambil nomor revisi yang sedang direstore lewat
	 * {@code getSelectedRestoreRevisionNumber(entity)}; (2) baca snapshot {@code UjianPunyaSoal}
	 * PADA revisi tersebut lewat {@link #detailAtRevision(AuditReader, Long, Integer)}; (3) bila
	 * revisi yang direstore adalah revisi DELETE (dicek lewat
	 * {@link #isDeleteRevision(AuditReader, Long, Integer)}) sehingga snapshot pada revisi itu
	 * sendiri sudah kosong/tidak berlaku, mundur satu revisi untuk mengambil state TEPAT SEBELUM
	 * penghapusan; (4) untuk setiap snapshot yang ID-nya belum ada di database, cari ulang
	 * {@link BankSoal} terkait secara managed (melempar {@link IllegalStateException} bila soal
	 * bank-nya sudah tidak tersedia — mencegah relasi yatim), lalu {@code session.replicate(...,
	 * ReplicationMode.OVERWRITE)} untuk menulis ulang baris tersebut persis seperti snapshot; (5)
	 * flush session dan bersihkan cache lokasi ujian ({@code restoredUjian.bersihkanLokasiUjianPunyaSoal()})
	 * agar dibangun ulang dari database saat daftar soal berikutnya dibuka.</p>
	 *
	 * <p>Dipanggil oleh kelas induk sebelum {@code tx.commit()}, baik pada jalur restore satu
	 * revisi maupun restore massal — lihat dokumentasi hook ini di kelas induk untuk konteks
	 * pemanggilan lengkap.</p>
	 *
	 * @param session Session Hibernate lokal milik transaksi restore yang sedang berjalan.
	 * @param reader  AuditReader Envers untuk membaca snapshot revisi {@code UjianPunyaSoal}.
	 * @param entity  entity {@link Ujian} yang baru saja direstore ke state aktif (tervalidasi
	 *                lewat {@code instanceof}; method langsung kembali bila bukan {@code Ujian}
	 *                atau ID-nya {@code null}).
	 * @throws Exception diteruskan agar transaksi restore di kelas induk dibatalkan bila terjadi
	 *                    kegagalan (mis. {@link IllegalStateException} soal hilang).
	 * @see ais.action.master.helper.GenericRevisiHelper#afterRestoreInTransaction(org.hibernate.Session, org.hibernate.envers.AuditReader, java.lang.Object)
	 */
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

	/**
	 * Mengambil snapshot seluruh {@link UjianPunyaSoal} milik satu {@link Ujian} PADA nomor
	 * revisi tertentu, lewat query Envers {@code forEntitiesAtRevision} yang disaring pada FK
	 * {@code ujian}.
	 *
	 * @param reader   AuditReader Envers untuk query snapshot.
	 * @param ujianId  ID entity Ujian induk.
	 * @param revision nomor revisi yang snapshot detailnya ingin dibaca.
	 * @return daftar {@link UjianPunyaSoal} pada revisi tersebut (bisa kosong bila tidak ada).
	 */
	private List detailAtRevision(AuditReader reader, Long ujianId, Integer revision) {
		AuditQuery query = reader.createQuery().forEntitiesAtRevision(UjianPunyaSoal.class, revision);
		query.add(AuditEntity.relatedId("ujian").eq(ujianId));
		return query.getResultList();
	}

	/**
	 * Memeriksa apakah nomor revisi tertentu pada {@link Ujian} tercatat sebagai revisi
	 * {@link RevisionType#DEL} (penghapusan) di riwayat Envers. Dipakai
	 * {@link #afterRestoreInTransaction(Session, AuditReader, Object)} untuk memutuskan apakah
	 * perlu mundur satu revisi saat mengambil snapshot detail soal.
	 *
	 * @param reader   AuditReader Envers untuk query riwayat revisi Ujian.
	 * @param ujianId  ID entity Ujian yang diperiksa.
	 * @param revision nomor revisi yang diperiksa tipenya.
	 * @return {@code true} bila ada baris riwayat pada revisi tersebut bertipe DELETE.
	 */
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
