package ais.action.master.helper;

import java.io.Serializable;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.zkoss.zk.ui.event.EventListener;

import ais.database.model.CicilanPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.PengaturanPembayaranBulanan;

/**
 * Subclass dari {@link ais.action.master.helper.GenericRevisiHelper} untuk entity
 * {@link ais.database.model.CicilanPembayaran} (rencana/jadwal cicilan pembayaran siswa/mahasiswa)
 * — lihat Javadoc class tersebut untuk penjelasan lengkap arsitektur window, alur Envers, dan
 * fitur restore. Berbeda dari kebanyakan subclass lain di package ini, class ini punya logika
 * tambahan nyata: dua konstruktor (dengan/tanpa filter {@link Kegiatan} lewat
 * {@link GenericRevisiHelper.FixedPropertyFilter}) serta override hook
 * {@code afterRestoreInTransaction} untuk memperbaiki data turunan pasca-restore.
 *
 * <p>Field pencarian: {@code nama}, {@code kode}, {@code keterangan}. Konstruktor kedua
 * ({@link #RevisiCicilanPembayaranHelper(EventListener, Kegiatan)}) menyaring riwayat hanya untuk
 * satu {@link Kegiatan} — dipakai saat window revisi dibuka dari konteks satu kegiatan spesifik
 * (mis. dari layar cicilan pembayaran suatu kegiatan), sementara konstruktor pertama menampilkan
 * riwayat seluruh cicilan tanpa penyaringan kegiatan.
 *
 * <p>Kompatibel Java 1.7 / source 1.6.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiCicilanPembayaranHelper extends GenericRevisiHelper<CicilanPembayaran> {

	private static final long serialVersionUID = 6589578552710016753L;
	private static final String[] SEARCH_PROPERTIES = new String[] { "nama", "kode", "keterangan" };

	/**
	 * Membangun daftar {@link QueryCustomizer} berdasarkan {@code kegiatan}: jika {@code null}
	 * mengembalikan array kosong (tanpa penyaringan), selain itu mengembalikan satu
	 * {@link GenericRevisiHelper.FixedPropertyFilter} pada property {@code kegiatan}.
	 */
	private static QueryCustomizer[] buildFilters(Kegiatan kegiatan) {
		java.util.List<QueryCustomizer> filters = new java.util.ArrayList<QueryCustomizer>();
		if (kegiatan != null) {
			filters.add(new GenericRevisiHelper.FixedPropertyFilter("kegiatan", kegiatan));
		}
		return filters.toArray(new QueryCustomizer[filters.size()]);
	}

	/**
	 * Membuka jendela riwayat revisi {@link CicilanPembayaran} tanpa penyaringan kegiatan
	 * (menampilkan riwayat seluruh cicilan pembayaran).
	 *
	 * @param eventListener callback yang diteruskan ke {@link GenericRevisiHelper}, boleh {@code null}
	 * @throws Exception diteruskan apa adanya dari konstruktor {@link GenericRevisiHelper}
	 */
	public RevisiCicilanPembayaranHelper(EventListener eventListener) throws Exception {
		super(CicilanPembayaran.class, "Revisi Cicilan Pembayaran", eventListener, SEARCH_PROPERTIES, buildFilters(null));
	}

	/**
	 * Membuka jendela riwayat revisi {@link CicilanPembayaran} yang disaring hanya untuk satu
	 * {@link Kegiatan}.
	 *
	 * @param eventListener callback yang diteruskan ke {@link GenericRevisiHelper}, boleh {@code null}
	 * @param kegiatan kegiatan yang membatasi riwayat yang ditampilkan; bila {@code null} perilaku
	 *                 sama seperti {@link #RevisiCicilanPembayaranHelper(EventListener)} (tanpa filter)
	 * @throws Exception diteruskan apa adanya dari konstruktor {@link GenericRevisiHelper}
	 */
	public RevisiCicilanPembayaranHelper(EventListener eventListener, Kegiatan kegiatan) throws Exception {
		super(CicilanPembayaran.class, "Revisi Cicilan Pembayaran", eventListener, SEARCH_PROPERTIES, buildFilters(kegiatan));
	}

	/**
	 * Override hook restore milik induk: setelah restore generik selesai (tapi sebelum commit),
	 * pastikan field {@code pengaturanPembayaranBulanan} pada {@link CicilanPembayaran} yang
	 * direstore tetap terisi bila memang seharusnya wajib ada. Diperlukan karena field ini punya
	 * aturan bisnis turunan (dicari dari {@link JenisKegiatan#getHanyaBerupaAngsuran()} atau dari
	 * riwayat Envers terakhir yang punya PPB non-null) yang tidak selalu tersimpan apa adanya pada
	 * snapshot revisi lama — tanpa perbaikan ini, restore murni dari snapshot Envers bisa
	 * menghasilkan {@link CicilanPembayaran} tanpa pengaturan pembayaran bulanan padahal
	 * seharusnya wajib angsuran.
	 *
	 * <p>Langkah: (1) muat ulang entity dari {@code session} agar state terkini pasca-merge; (2)
	 * bila sudah punya PPB, tidak ada yang perlu diperbaiki; (3) tentukan apakah kegiatan terkait
	 * wajib angsuran lewat {@link JenisKegiatan}; (4) cari revisi Envers terakhir (bukan hasil
	 * hapus) yang punya {@code pengaturanPembayaranBulanan} non-null lewat
	 * {@link #findLastNonNullPpb(AuditReader, Serializable)}; (5) bila wajib angsuran atau ada
	 * riwayat PPB, terapkan PPB tersebut (dimuat ulang dari session bila memungkinkan) dan simpan.
	 *
	 * @see ais.action.master.helper.GenericRevisiHelper#afterRestoreInTransaction(org.hibernate.Session, org.hibernate.envers.AuditReader, java.lang.Object)
	 */
	@Override
	protected void afterRestoreInTransaction(Session session, AuditReader reader, Object entity) throws Exception {
		if (!(entity instanceof CicilanPembayaran)) {
			return;
		}
		CicilanPembayaran cicilan = (CicilanPembayaran) entity;
		if (cicilan.getId() == null) {
			return;
		}

		// Muat ulang dari session agar state terkini (post-merge)
		CicilanPembayaran fresh = (CicilanPembayaran) session.get(CicilanPembayaran.class, cicilan.getId());
		if (fresh == null) {
			return;
		}

		// Jika sudah punya PPB → tidak perlu perbaikan
		if (fresh.getPengaturanPembayaranBulanan() != null) {
			return;
		}

		// Cek kondisi wajib angsuran
		boolean wajibAngsuran = false;
		try {
			Kegiatan keg = fresh.getKegiatan();
			if (keg != null) {
				JenisKegiatan jk = keg.getJenisKegiatan();
				if (jk != null) {
					wajibAngsuran = Boolean.TRUE.equals(jk.getHanyaBerupaAngsuran());
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RevisiCicilanPembayaranHelper.java:87");
		}

		// Cari riwayat Envers terakhir yang punya PPB non-null untuk ID ini
		PengaturanPembayaranBulanan ppbTerakhir = findLastNonNullPpb(reader, cicilan.getId());

		boolean adaRiwayatPpb = (ppbTerakhir != null);

		if (!wajibAngsuran && !adaRiwayatPpb) {
			return;
		}

		if (ppbTerakhir == null) {
			return;
		}

		// Muat PPB dari session agar terhubung ke transaksi yang sama
		PengaturanPembayaranBulanan ppbLoaded = null;
		try {
			ppbLoaded = (PengaturanPembayaranBulanan) session.get(PengaturanPembayaranBulanan.class,
					ppbTerakhir.getId());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RevisiCicilanPembayaranHelper.java:108");
		}
		if (ppbLoaded == null) {
			ppbLoaded = ppbTerakhir;
		}

		fresh.setPengaturanPembayaranBulanan(ppbLoaded);
		if (fresh.getDetailBiaya() == null && ppbLoaded.getDetailBiaya() != null) {
			fresh.setDetailBiaya(ppbLoaded.getDetailBiaya());
		}
		session.saveOrUpdate(fresh);
		session.flush();
	}

	/**
	 * Mencari {@link PengaturanPembayaranBulanan} non-null terakhir dari riwayat revisi Envers
	 * {@link CicilanPembayaran} dengan {@code cicilanId} tertentu, dengan mengabaikan revisi
	 * bertipe {@link RevisionType#DEL} (hapus). Dipakai oleh {@link #afterRestoreInTransaction}
	 * untuk menebak nilai PPB yang seharusnya berlaku ketika snapshot revisi yang direstore
	 * sendiri tidak membawa nilai tersebut.
	 *
	 * @param reader {@link AuditReader} Envers aktif pada session/transaksi restore
	 * @param cicilanId id {@link CicilanPembayaran} yang riwayatnya ditelusuri
	 * @return PPB non-null terakhir yang ditemukan, atau {@code null} bila tidak ada riwayat
	 *         yang cocok atau terjadi kegagalan saat query (kegagalan ditelan, dicatat lewat
	 *         {@code ErrorAuditUtil} bila relevan)
	 */
	private PengaturanPembayaranBulanan findLastNonNullPpb(AuditReader reader, Serializable cicilanId) {
		try {
			AuditQuery q = reader.createQuery()
					.forRevisionsOfEntity(CicilanPembayaran.class, false, true);
			q.add(AuditEntity.id().eq(cicilanId));
			q.add(AuditEntity.property("pengaturanPembayaranBulanan").isNotNull());
			q.addOrder(AuditEntity.revisionNumber().desc());
			q.setMaxResults(1);
			List results = q.getResultList();
			for (int i = 0; i < results.size(); i++) {
				Object row = results.get(i);
				RevisionType revType = extractRevisionType(row);
				if (revType == RevisionType.DEL) {
					continue;
				}
				Object ent = extractEntity(row);
				if (ent instanceof CicilanPembayaran) {
					CicilanPembayaran c = (CicilanPembayaran) ent;
					if (c.getPengaturanPembayaranBulanan() != null) {
						return c.getPengaturanPembayaranBulanan();
					}
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RevisiCicilanPembayaranHelper.java:145");
		}
		return null;
	}

	/**
	 * Buka langsung tab "Seluruh Data Revisi" (tempat tombol Restore per-revisi dan
	 * "Restore Terbaru mulai tanggal" berada) — dipakai tombol Restore di toolbar.
	 * Mirror {@code RevisiPembayaranSiswaDetailHelper.bukaTabSeluruhData()}.
	 */
	public void bukaTabSeluruhData() {
		try {
			if (mainTabbox != null) {
				mainTabbox.setSelectedIndex(2);
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit RevisiCicilanPembayaranHelper.bukaTabSeluruhData");
		}
	}

}
