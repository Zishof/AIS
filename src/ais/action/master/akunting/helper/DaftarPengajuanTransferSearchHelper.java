package ais.action.master.akunting.helper;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;

/**
 * <h3>DaftarPengajuanTransferSearchHelper — Pencarian Pengajuan Transfer yang Dapat Dipakai Ulang</h3>
 *
 * <p><b>Untuk apa kelas ini.</b> Menyeragamkan cara mencari data pengajuan transfer
 * ({@code DaftarPengajuanTransfer}) berdasarkan <b>kode pengajuan</b> dan <b>judul pengajuan</b> —
 * termasuk kasus pembayaran <i>vendor termin</i> yang kode induknya berada di dokumen lain (Termin,
 * PO, atau BAST) dan nama vendornya di entitas penyedia. Sebelumnya logika pencarian kaya ini hanya
 * ada di tab "Daftar Transfer"; kelas ini mengangkatnya menjadi perkakas bersama sehingga tab "Proses
 * Transfer" (mis. dialog pilih pengajuan) dapat memakai pencarian yang PERSIS SAMA — cukup memanggil
 * {@link #pasangAlias(Criteria)} lalu {@link #filterKodeJudul(Session, String)}.</p>
 *
 * <p><b>Yang dicari (OR):</b> kode pengajuan, judul/nama pengajuan, nama vendor penyedia
 * ({@code saldoAwalMasterAsset.penyedia.nama}), kode dokumen pajak ({@code pajak.kode}), serta kode
 * INDUK termin (Termin/PO/BAST) — baik untuk baris tagihan vendor termin (relasi langsung) maupun baris
 * PPh/Pajak termin (via {@code pajak.keyData} = id termin-detail).</p>
 *
 * <p><b>Cara pakai.</b> Pada {@link Criteria} sumber, panggil {@link #pasangAlias(Criteria)} untuk
 * menyiapkan alias {@code vendorPenyedia} dan {@code pjkBd} (bila belum ada), lalu tambahkan
 * {@code criteria.add(filterKodeJudul(session, teks))}. Semua akses dibungkus {@code try/catch};
 * kelas {@code final} tanpa state, aman dipakai bersama antar-thread.</p>
 */
public final class DaftarPengajuanTransferSearchHelper {

	private static final String DPT = "ais.database.model.akunting.DaftarPengajuanTransfer";
	private static final String BAST = "ais.database.model.asset.PenerimaanPengadaanMasterAsset";
	private static final String TD = "ais.database.model.asset.PembayaranTerminMasterAssetDetail";

	private DaftarPengajuanTransferSearchHelper() {
	}

	/**
	 * Menyiapkan alias yang dibutuhkan {@link #filterKodeJudul(Session, String)}: {@code vendorPenyedia}
	 * (dari {@code saldoAwalMasterAsset.penyedia}) dan {@code pjkBd} (dari {@code pajak}). Aman dipanggil
	 * walau alias sejenis sudah ada (dibungkus try/catch).
	 */
	public static void pasangAlias(Criteria criteria) {
		if (criteria == null) {
			return;
		}
		try { criteria.createAlias("saldoAwalMasterAsset", "vendorSaldo", Criteria.LEFT_JOIN); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DaftarPengajuanTransferSearchHelper.java:51"); }
		try { criteria.createAlias("vendorSaldo.penyedia", "vendorPenyedia", Criteria.LEFT_JOIN); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DaftarPengajuanTransferSearchHelper.java:52"); }
		try { criteria.createAlias("pajak", "pjkBd", Criteria.LEFT_JOIN); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DaftarPengajuanTransferSearchHelper.java:53"); }
	}

	/**
	 * {@code true} bila DPT adalah baris TERMIN (tagihan vendor termin langsung ATAU PPh termin via
	 * {@code pajak.keyData}) yang {@code PembayaranTerminMasterAsset} induknya BELUM DISETUJUI
	 * ({@code getDisetujuiOleh() == null}). Untuk baris NON-termin, termin yang SUDAH disetujui, atau
	 * data tak lengkap → {@code false} (fail-open: jangan disembunyikan). Dipakai menyaring kandidat
	 * "Tambah Proses Transfer" agar HANYA termin yang sudah disetujui yang bisa ditarik ke DPC —
	 * termasuk data LAMA yang DPT-nya terlanjur dibuat sebelum disetujui. Approval termin sering
	 * lewat disposisi SOP, sehingga memakai getter (computed) {@code getDisetujuiOleh()}, BUKAN kolom
	 * SQL langsung (kolom {@code disetujui_oleh} sering null padahal sudah disetujui via SOP).
	 */
	public static boolean terminBelumDisetujui(Session session,
			ais.database.model.akunting.DaftarPengajuanTransfer d) {
		return belumDisetujui(session, d);
	}

	/**
	 * {@code true} bila DPT adalah baris tagihan berbasis PERSETUJUAN (TERMIN, DP pengadaan, atau
	 * PEMBAYARAN pengadaan) yang dokumen INDUK-nya BELUM DISETUJUI ({@code getDisetujuiOleh()==null}).
	 * Untuk baris jenis lain, yang sudah disetujui, atau data tak lengkap → {@code false} (fail-open:
	 * jangan disembunyikan). Dipakai menyaring kandidat "Tambah Proses Transfer" agar HANYA tagihan
	 * yang sudah disetujui yang bisa ditarik ke DPC — termasuk data LAMA yang DPT-nya terlanjur dibuat
	 * saat SAVE (Dp/Pengadaan) atau sebelum disetujui (termin). Approval sering lewat disposisi SOP →
	 * memakai getter (computed) {@code getDisetujuiOleh()}, BUKAN kolom SQL langsung.
	 */
	public static boolean belumDisetujui(Session session, ais.database.model.akunting.DaftarPengajuanTransfer d) {
		try {
			if (d == null) {
				return false;
			}
			// TERMIN: vendor termin langsung ATAU PPh termin via pajak.keyData.
			ais.database.model.asset.PembayaranTerminMasterAssetDetail det = null;
			if (d.getPembayaranTerminMasterAssetDetail() != null) {
				det = d.getPembayaranTerminMasterAssetDetail();
			} else if (d.getPajak() != null && d.getPajak().getKeyData() != null
					&& d.getPajak().getSaldoAwalMasterAssetDetail() == null && d.getPajak().getSaldoAwal() == null
					&& d.getPajak().getPertangungjawaban() == null
					&& d.getPajak().getPertangungjawabanKasBesar() == null && session != null) {
				det = (ais.database.model.asset.PembayaranTerminMasterAssetDetail) session.get(
						ais.database.model.asset.PembayaranTerminMasterAssetDetail.class, d.getPajak().getKeyData());
			}
			if (det != null) {
				ais.database.model.asset.PembayaranTerminMasterAsset induk = det.getPembayaranTerminMasterAsset();
				return induk != null && induk.getDisetujuiOleh() == null;
			}
			// DP PENGADAAN: induk = PembayaranDpMasterAsset.
			if (d.getPembayaranDpMasterAssetDetail() != null) {
				ais.database.model.asset.PembayaranDpMasterAsset induk = d.getPembayaranDpMasterAssetDetail()
						.getPembayaranDpMasterAsset();
				return induk != null && induk.getDisetujuiOleh() == null;
			}
			// PEMBAYARAN PENGADAAN: induk = PembayaranPengadaanMasterAsset.
			if (d.getPembayaranPengadaanMasterAssetDetail() != null) {
				ais.database.model.asset.PembayaranPengadaanMasterAsset induk = d
						.getPembayaranPengadaanMasterAssetDetail().getPembayaranPengadaanMasterAsset();
				return induk != null && induk.getDisetujuiOleh() == null;
			}
			return false; // jenis lain (mis. Kas Besar/Uang Muka/SaldoAwal yg sudah gated) → jangan sembunyikan
		} catch (Exception e) {
			return false; // fail-open
		}
	}

	/**
	 * Filter OR berdasarkan kode/judul pengajuan + nama vendor + kode pajak + kode induk termin.
	 * Membutuhkan alias {@code vendorPenyedia} dan {@code pjkBd} (lihat {@link #pasangAlias(Criteria)}).
	 */
	public static Criterion filterKodeJudul(Session session, String search) {
		Disjunction or = Restrictions.disjunction();
		or.add(Restrictions.ilike("kode", search, MatchMode.ANYWHERE));
		or.add(Restrictions.ilike("nama", search, MatchMode.ANYWHERE));
		or.add(Restrictions.ilike("vendorPenyedia.nama", search, MatchMode.ANYWHERE));
		or.add(Restrictions.ilike("pjkBd.kode", search, MatchMode.ANYWHERE));
		// Kolom teks langsung pada DPT + kode dokumen pengadaan (SaldoAwalMasterAsset).
		or.add(Restrictions.ilike("keterangan", search, MatchMode.ANYWHERE));
		or.add(Restrictions.ilike("atasNamaSumber", search, MatchMode.ANYWHERE));
		or.add(Restrictions.ilike("noRekSumber", search, MatchMode.ANYWHERE));
		or.add(Restrictions.ilike("vendorSaldo.kode", search, MatchMode.ANYWHERE));
		java.util.Set<Long> dptIds = dptIdByKodeInduk(session, search);
		if (dptIds != null && !dptIds.isEmpty()) {
			or.add(Restrictions.in("id", dptIds));
		}
		return or;
	}

	/**
	 * Kumpulkan id {@code DaftarPengajuanTransfer} yang kode INDUK termin-nya (Termin/PO/BAST) mengandung
	 * {@code search}. Baris tagihan vendor termin lewat relasi langsung; baris PPh/Pajak termin lewat
	 * {@code pajak.keyData} (= id termin-detail).
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static java.util.Set<Long> dptIdByKodeInduk(Session session, String search) {
		java.util.Set<Long> ids = new java.util.HashSet<Long>();
		if (session == null || search == null || search.trim().isEmpty()) {
			return ids;
		}
		String q = "%" + search.trim() + "%";
		// (A) Baris TAGIHAN VENDOR termin (relasi langsung).
		tambahIdDpt(session, ids, "select dpt.id from " + DPT + " dpt "
				+ "where dpt.pembayaranTerminMasterAssetDetail.pembayaranTerminMasterAsset.kode like :q", q);
		tambahIdDpt(session, ids, "select dpt.id from " + DPT + " dpt "
				+ "where dpt.pembayaranTerminMasterAssetDetail.pemesananPengadaanMasterAsset.kode like :q", q);
		tambahIdDpt(session, ids, "select dpt.id from " + DPT + " dpt, " + BAST + " b "
				+ "where b.pemesananPengadaanMasterAsset = dpt.pembayaranTerminMasterAssetDetail.pemesananPengadaanMasterAsset "
				+ "and b.kode like :q", q);
		// (B) Baris PPh/PAJAK termin (via keyData = id termin-detail).
		java.util.Set<Long> td = new java.util.HashSet<Long>();
		tambahIdDpt(session, td, "select d.id from " + TD + " d where d.pembayaranTerminMasterAsset.kode like :q", q);
		tambahIdDpt(session, td, "select d.id from " + TD + " d where d.pemesananPengadaanMasterAsset.kode like :q", q);
		tambahIdDpt(session, td, "select d.id from " + TD + " d, " + BAST + " b "
				+ "where b.pemesananPengadaanMasterAsset = d.pemesananPengadaanMasterAsset and b.kode like :q", q);
		if (!td.isEmpty()) {
			try {
				java.util.List res = session.createQuery("select dpt.id from " + DPT + " dpt "
						+ "where dpt.pajak.keyData in (:ids) "
						+ "and dpt.pajak.saldoAwalMasterAssetDetail is null and dpt.pajak.saldoAwal is null "
						+ "and dpt.pajak.pertangungjawaban is null and dpt.pajak.pertangungjawabanKasBesar is null")
						.setParameterList("ids", td).list();
				for (Object o : res) {
					if (o instanceof Long) {
						ids.add((Long) o);
					}
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		return ids;
	}

	/** Jalankan satu query HQL (parameter {@code :q}) dan tambahkan id Long hasilnya ke {@code out}. */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void tambahIdDpt(Session session, java.util.Set<Long> out, String hql, String qParam) {
		try {
			java.util.List hasil = session.createQuery(hql).setString("q", qParam).list();
			for (Object o : hasil) {
				if (o instanceof Long) {
					out.add((Long) o);
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}
}
