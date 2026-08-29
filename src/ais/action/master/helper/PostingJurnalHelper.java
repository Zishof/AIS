package ais.action.master.helper;

import java.util.Date;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.model.akunting.GrupTransaksi;
import ais.ui.util.MyLabelBold;

/**
 * Helper generic untuk seluruh proses posting jurnal:
 *
 * 1. Sisi dasbor (DrafJurnalAction): membangun baris ringkasan
 *    (Nama Jurnal / Draft / Terposting / Closing / Uraian), menghitung
 *    jumlah data, dan membangun URL jendela posting/closing.
 * 2. Sisi jendela posting (Posting*Action): membaca parameter
 *    sudah_posting / mulai / sampai yang dikirim dasbor lalu
 *    menerapkannya ke filter pencarian.
 *
 * Kriteria hitung di dasbor dan filter di jendela memakai method yang
 * SAMA dari class ini, sehingga angka yang di-click selalu sama dengan
 * data yang keluar di jendela.
 */
public final class PostingJurnalHelper {

	/** Nilai parameter ref untuk grup transaksi yang ref-nya kosong. */
	public static final String REF_KOSONG = "KOSONG";
	/** ref grup transaksi pembayaran dibayar dimuka. */
	public static final String REF_DIMUKA = "dimuka";
	/** ref grup transaksi biaya payment gateway. */
	public static final String REF_PAYMENT_GATEWAY = "payment_gateway";
	/** ref grup transaksi DP pekerjaan vendor. */
	public static final String REF_DP_PEKERJAAN = "DP_PEKERJAAN";
	/** ref grup transaksi jurnal balik DP pekerjaan vendor. */
	public static final String REF_DP_BALIK_PEKERJAAN = "DP_BALIK_PEKERJAAN";
	/** ref terisi namun bukan DP (pekerjaan vendor non-DP). */
	public static final String REF_PEKERJAAN_NON_DP = "PEKERJAAN_NON_DP";
	/** ref grup transaksi pengembalian sisa uang muka (jurnal pengembalian LPJ). */
	public static final String REF_PENGEMBALIAN = "pengembalian";

	/** Properti GrupTransaksi yang boleh dipakai sebagai filter entitas closing. */
	private static final String[] ENTITAS_CLOSING = { "pertangungjawaban", "kasKecil", "kasBesar", "pajak",
			"pemesananPengadaanMasterAsset", "penerimaanPengadaanMasterAsset", "saldoAwalMasterAsset", "pembayaranGaji",
			"detailKegiatan", "cicilanPembayaran", "deposit", "pengeluaranMahasiswa", "logPembayaran", "tagihan",
			"pembayaranSiswaDetail", "depositSiswa", "penyusutanAsset", "daftarPengajuanTransfer", "transitori",
			"pembayaranPengadaanMasterAssetDetail", "pembayaranDpMasterAssetDetail",
			"pembayaranTerminMasterAssetDetail" };

	/** Nilai kolom jenis pada grup transaksi yang boleh dipakai sebagai filter. */
	private static final String[] JENIS_CLOSING = { "PIUTANG_SISWA", "PEMBAYARAN_SISWA_DIBAYAR_DIMUKA",
			"PIUTANG_DENDA_SISWA", "UTANG_DISKON_SISWA" };

	private PostingJurnalHelper() {
	}

	// =====================================================================
	// SISI JENDELA POSTING: pembacaan parameter kiriman dasbor
	// =====================================================================

	/**
	 * Membaca parameter boolean dari execution saat ini. Mengembalikan null
	 * bila parameter tidak ada / kosong (jendela dibuka dari menu biasa).
	 */
	public static Boolean ambilParameterBoolean(String nama) {
		try {
			Execution execution = Executions.getCurrent();
			if (execution == null) {
				return null;
			}
			String nilai = execution.getParameter(nama);
			if (nilai == null || nilai.trim().isEmpty()) {
				return null;
			}
			return Boolean.valueOf(Boolean.parseBoolean(nilai.trim()));
		} catch (Exception e) {
			return null;
		}
	}

	/** Membaca parameter sudah_posting kiriman dasbor draft jurnal. */
	public static Boolean ambilParameterSudahPosting() {
		return ambilParameterBoolean("sudah_posting");
	}

	/**
	 * Membaca parameter entitas (properti GrupTransaksi) dan memvalidasinya
	 * terhadap whitelist. Mengembalikan null bila tidak ada / tidak dikenal.
	 */
	public static String ambilParameterEntitasClosing() {
		try {
			Execution execution = Executions.getCurrent();
			if (execution == null) {
				return null;
			}
			String nilai = execution.getParameter("entitas");
			if (nilai == null || nilai.trim().isEmpty()) {
				return null;
			}
			nilai = nilai.trim();
			for (int i = 0; i < ENTITAS_CLOSING.length; i++) {
				if (ENTITAS_CLOSING[i].equals(nilai)) {
					return nilai;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PostingJurnalHelper.java:116");
		}
		return null;
	}

	/**
	 * Menerapkan parameter mulai / sampai (format dateFormat8) ke datebox
	 * filter. Bila parameter tidak ada atau gagal di-parse, nilai default
	 * datebox dibiarkan apa adanya.
	 */
	public static void terapkanParameterTanggal(Datebox mulai, Datebox sampai) {
		try {
			Execution execution = Executions.getCurrent();
			if (execution == null) {
				return;
			}
			String nilaiMulai = execution.getParameter("mulai");
			if (mulai != null && nilaiMulai != null && !nilaiMulai.trim().isEmpty()) {
				try {
					mulai.setValue(Common.dateFormat8.get().parse(nilaiMulai.trim()));
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
			String nilaiSampai = execution.getParameter("sampai");
			if (sampai != null && nilaiSampai != null && !nilaiSampai.trim().isEmpty()) {
				try {
					sampai.setValue(Common.dateFormat8.get().parse(nilaiSampai.trim()));
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Restriksi status posting berbasis isNull/isNotNull properti posting
	 * history. Null = tanpa filter (jendela dibuka dari menu biasa).
	 */
	public static Criterion restriksiPosting(String property, Boolean sudahPosting) {
		if (sudahPosting == null) {
			return Restrictions.sqlRestriction("1=1");
		}
		return sudahPosting.booleanValue() ? Restrictions.isNotNull(property) : Restrictions.isNull(property);
	}

	// =====================================================================
	// SISI DASBOR: penghitung jumlah data
	// =====================================================================

	/** SQL restriksi rentang tanggal: date(kolom) between mulai dan sampai. */
	public static String dateSql(String kolom, Date mulai, Date sampai) {
		return "date(" + kolom + ") between date('" + Common.databaseDateFormat.get().format(mulai)
				+ "') and date('" + Common.databaseDateFormat.get().format(sampai) + "')";
	}

	/** Mengeksekusi criteria rowCount menjadi int yang aman dari null. */
	public static int hitung(Criteria criteriaRowCount) {
		Object nilai = criteriaRowCount.setMaxResults(1).uniqueResult();
		return nilai == null ? 0 : ((Number) nilai).intValue();
	}

	/**
	 * Menerapkan filter status posting pola join posting history:
	 * draft = history kosong atau posting=false, terposting = history ADA dan tidak
	 * dinonaktifkan (posting=true ATAU posting null).
	 *
	 * <p>Bendera null dihitung TERPOSTING, bukan diabaikan: PostingHistory memakai
	 * dynamic-insert dan kolom posting tidak punya default DB, sehingga posting massal
	 * lama (tombol ZK "Posting Semua" tidak pernah menyetel bendera) meninggalkan
	 * posting=null. Versi lama filter ini menuntut posting=true untuk terposting DAN
	 * posting=false untuk draf, sehingga dokumen bercap riwayat ber-bendera null lenyap
	 * dari KEDUA hitungan dasbor. Dokumen bercap riwayat adalah dokumen terposting;
	 * hanya penonaktifan eksplisit (posting=false) yang mengembalikannya ke draf.</p>
	 */
	public static void terapkanStatusPostingHistory(Criteria criteria, boolean sudahPosting) {
		criteria.createAlias("postingHistory", "postingHistory", Criteria.LEFT_JOIN);
		if (sudahPosting) {
			criteria.add(Restrictions.and(Restrictions.isNotNull("postingHistory.id"),
					Restrictions.or(Restrictions.eq("postingHistory.posting", true),
							Restrictions.isNull("postingHistory.posting"))));
		} else {
			criteria.add(Restrictions.or(Restrictions.isNull("postingHistory.id"),
					Restrictions.eq("postingHistory.posting", false)));
		}
	}

	/**
	 * Restriksi ref grup transaksi sesuai whitelist. Dipakai oleh penghitung
	 * dasbor DAN filter GrupTransaksiAction supaya keduanya identik.
	 */
	public static Criterion restriksiRefClosing(String ref) {
		if (ref == null || ref.trim().isEmpty()) {
			return Restrictions.sqlRestriction("1=1");
		}
		ref = ref.trim();
		if (REF_KOSONG.equals(ref)) {
			return Restrictions.sqlRestriction("(ref is null or ref='')");
		}
		if (REF_PEKERJAAN_NON_DP.equals(ref)) {
			return Restrictions
					.sqlRestriction("ref is not null and ref != 'DP_PEKERJAAN' and ref != 'DP_BALIK_PEKERJAAN'");
		}
		if (REF_DIMUKA.equals(ref) || REF_PAYMENT_GATEWAY.equals(ref) || REF_DP_PEKERJAAN.equals(ref)
				|| REF_DP_BALIK_PEKERJAAN.equals(ref) || REF_PENGEMBALIAN.equals(ref)) {
			return Restrictions.sqlRestriction("ref = '" + ref + "'");
		}
		return Restrictions.sqlRestriction("1=1");
	}

	/** Restriksi kolom jenis grup transaksi sesuai whitelist. */
	public static Criterion restriksiJenisClosing(String jenis) {
		if (jenis == null || jenis.trim().isEmpty()) {
			return Restrictions.sqlRestriction("1=1");
		}
		jenis = jenis.trim();
		for (int i = 0; i < JENIS_CLOSING.length; i++) {
			if (JENIS_CLOSING[i].equals(jenis)) {
				return Restrictions.sqlRestriction("jenis='" + jenis + "'");
			}
		}
		return Restrictions.sqlRestriction("1=1");
	}

	/**
	 * Menghitung grup transaksi yang sudah closing untuk satu entitas sumber.
	 * Parameter entitas/ref/jenis sama persis dengan parameter URL yang
	 * dibuka saat angka closing di-click (lihat urlClosing).
	 */
	public static int hitungClosing(Session session, String entitas, String ref, String jenis, Date mulai,
			Date sampai) {
		try {
			Criteria criteria = session.createCriteria(GrupTransaksi.class).setProjection(Projections.rowCount())
					.add(Restrictions.isNotNull("closing"))
					.add(Restrictions.sqlRestriction(dateSql("this_.tanggal_transaksi", mulai, sampai)));
			if (entitas != null && !entitas.trim().isEmpty()) {
				criteria.add(Restrictions.isNotNull(entitas.trim()));
			}
			criteria.add(restriksiRefClosing(ref));
			criteria.add(restriksiJenisClosing(jenis));
			return hitung(criteria);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return 0;
		}
	}

	// =====================================================================
	// SISI DASBOR: pembangun URL jendela
	// =====================================================================

	private static String parameterTanggal(Date mulai, Date sampai) {
		return "&mulai=" + Common.dateFormat8.get().format(mulai) + "&sampai="
				+ Common.dateFormat8.get().format(sampai);
	}

	/** URL jendela posting dengan parameter sudah_posting + rentang tanggal. */
	public static String urlPosting(String halaman, boolean sudahPosting, Date mulai, Date sampai) {
		return halaman + "?sudah_posting=" + sudahPosting + parameterTanggal(mulai, sampai);
	}

	/**
	 * URL grup transaksi closing dengan filter entitas/ref/jenis yang sama
	 * dengan hitungClosing. entitas null = seluruh jurnal umum (default).
	 */
	public static String urlClosing(String entitas, String ref, String jenis, Date mulai, Date sampai) {
		StringBuilder url = new StringBuilder("/pages/master/akunting/grup_transaksi.zul?sudah_closing=true");
		if (entitas != null && !entitas.trim().isEmpty()) {
			url.append("&entitas=").append(entitas.trim());
		}
		if (ref != null && !ref.trim().isEmpty()) {
			url.append("&ref=").append(ref.trim());
		}
		if (jenis != null && !jenis.trim().isEmpty()) {
			url.append("&jenis=").append(jenis.trim());
		}
		url.append(parameterTanggal(mulai, sampai));
		return url.toString();
	}

	/** URL grup transaksi dengan parameter bebas (sudah dirakit pemanggil). */
	public static String urlGrupTransaksi(String parameter, Date mulai, Date sampai) {
		return "/pages/master/akunting/grup_transaksi.zul?" + parameter + parameterTanggal(mulai, sampai);
	}

	// =====================================================================
	// SISI DASBOR: komponen UI baris jurnal
	// =====================================================================

	/** Membuka jendela posting/closing ukuran standar dasbor. */
	public static void bukaJendela(String url) {
		try {
			Common.displayWindow(url, true, "95%", "95%", null, "", false);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"membuka jendela rincian posting/closing jurnal",
					e, new String[] {
							"Muat ulang (refresh) halaman dasbor ini lalu coba klik kembali.",
							"Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	/**
	 * Link angka yang membuka jendela rincian. Angka nol tetap bisa di-click
	 * supaya pengguna dapat memastikan memang tidak ada datanya.
	 */
	public static A buatLinkAngka(int jumlah, final String url) {
		A a = new A(Common.numberFormat.get().format(jumlah));
		a.setSclass(jumlah == 0 ? "ais-posting-angka ais-posting-angka-nol" : "ais-posting-angka");
		a.setTooltiptext("Klik untuk membuka rincian data sesuai angka ini");
		if (url != null && url.trim().length() > 0) {
			a.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					bukaJendela(url);
				}
			});
		}
		return a;
	}

	/**
	 * Satu baris dasbor draft jurnal: nama, tiga angka (draft / terposting /
	 * closing) yang bisa di-click, dan uraian singkat.
	 */
	public static void tambahBarisJurnal(Rows rows, String nama, int draft, int posting, int closing,
			String urlDraft, String urlPosting, String urlClosing, String uraian, EventListener postingListener,
			EventListener batalkanListener) {
		Row row = new Row();
		row.setParent(rows);
		row.setSclass("ais-posting-baris");
		row.appendChild(new MyLabelBold(nama));
		row.appendChild(buatLinkAngka(draft, urlDraft));
		row.appendChild(buatLinkAngka(posting, urlPosting));
		row.appendChild(buatLinkAngka(closing, urlClosing));
		Hbox hbox = new Hbox();
		hbox.setWidth("100%");
		hbox.setSclass("ais-posting-uraian");
		Label label = new Label(uraian == null ? "" : uraian);
		label.setStyle("white-space:normal;");
		label.setParent(hbox);
		hbox.setParent(row);

		// Kolom POSTING (satu-klik): aktif bila ada draft. Nama jenis disimpan sebagai atribut
		// agar listener dasbor tahu modul mana yang harus diposting.
		ais.ui.util.MyToolbarbuttonConfig btnPosting = new ais.ui.util.MyToolbarbuttonConfig("Posting",
				"/img/svg/check2-circle.svg");
		btnPosting.setAttribute("namaJenis", nama);
		btnPosting.setStyle("color:#16a34a;font-weight:bold;");
		btnPosting.setTooltiptext("Posting semua draft \"" + nama + "\" pada periode terpilih");
		if (draft > 0 && postingListener != null) {
			btnPosting.addEventListener("onClick", postingListener);
		} else {
			btnPosting.setDisabled(true);
		}
		row.appendChild(btnPosting);

		// Kolom BATALKAN POSTING: aktif bila ada yang sudah terposting.
		ais.ui.util.MyToolbarbuttonConfig btnBatal = new ais.ui.util.MyToolbarbuttonConfig("Batalkan Posting",
				"/img/svg/close-circle-line.svg");
		btnBatal.setAttribute("namaJenis", nama);
		btnBatal.setStyle("color:#dc2626;font-weight:bold;");
		btnBatal.setTooltiptext("Batalkan posting \"" + nama + "\" (jurnal yang sudah closing tidak dibatalkan)");
		if (posting > 0 && batalkanListener != null) {
			btnBatal.addEventListener("onClick", batalkanListener);
		} else {
			btnBatal.setDisabled(true);
		}
		row.appendChild(btnBatal);
	}

}
