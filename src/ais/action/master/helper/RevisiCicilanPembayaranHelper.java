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
 * Versi generic dari helper revisi lama.
 *
 * Semua proses baca/restore revisi dipusatkan di GenericRevisiHelper<T> agar:
 * - code lebih ringkas dan mudah dirawat;
 * - semua Hibernate Session memakai openSession();
 * - semua Session ditutup di finally melalui session.clear(), session.disconnect(), dan session.close();
 * - fitur restore satu revisi dan restore massal dari tanggal tertentu tetap tersedia.
 *
 * Kompatibel Java 1.7 / source 1.6.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiCicilanPembayaranHelper extends GenericRevisiHelper<CicilanPembayaran> {

	private static final long serialVersionUID = 6589578552710016753L;
	private static final String[] SEARCH_PROPERTIES = new String[] { "nama", "kode", "keterangan" };

	private static QueryCustomizer[] buildFilters(Kegiatan kegiatan) {
		java.util.List<QueryCustomizer> filters = new java.util.ArrayList<QueryCustomizer>();
		if (kegiatan != null) {
			filters.add(new GenericRevisiHelper.FixedPropertyFilter("kegiatan", kegiatan));
		}
		return filters.toArray(new QueryCustomizer[filters.size()]);
	}

	public RevisiCicilanPembayaranHelper(EventListener eventListener) throws Exception {
		super(CicilanPembayaran.class, "Revisi Cicilan Pembayaran", eventListener, SEARCH_PROPERTIES, buildFilters(null));
	}

	public RevisiCicilanPembayaranHelper(EventListener eventListener, Kegiatan kegiatan) throws Exception {
		super(CicilanPembayaran.class, "Revisi Cicilan Pembayaran", eventListener, SEARCH_PROPERTIES, buildFilters(kegiatan));
	}

	/**
	 * Setelah restore generik selesai (tapi sebelum commit), pastikan
	 * pengaturanPembayaranBulanan tidak null jika jenisKegiatan wajib angsuran
	 * atau ada riwayat PPB non-null di Envers.
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
