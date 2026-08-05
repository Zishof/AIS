package ais.action.master.dashboard.akunting;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.action.master.helper.PostingJurnalHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.Deposit;
import ais.database.model.DetailKegiatan;
import ais.database.model.LogPembayaran;
import ais.database.model.PengeluaranMahasiswa;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.KasBesar;
import ais.database.model.akunting.KasKecil;
import ais.database.model.akunting.Pajak;
import ais.database.model.akunting.Pertangungjawaban;
import ais.database.model.akunting.Transaksi;
import ais.database.model.akunting.Transitori;
import ais.database.model.asset.PemesananPengadaanMasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAsset;
import ais.database.model.asset.PenyusutanAsset;
import ais.database.model.asset.SaldoAwalMasterAsset;
import ais.database.model.payroll.PembayaranGaji;
import ais.database.model.sekolah.DepositSiswa;
import ais.database.model.sekolah.PembayaranSiswaDetail;
import ais.database.model.sekolah.Tagihan;
import ais.ui.util.DraftJurnalDashboardUtil;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.UIUtil;

/**
 * Dasbor draft jurnal: satu baris per jenis jurnal dengan angka Draft /
 * Terposting / Closing yang bisa di-click. Penghitung angka dan URL jendela
 * rincian dibangun lewat {@link PostingJurnalHelper} sehingga data yang
 * keluar di jendela selalu sama dengan angka yang di-click.
 */
public class DrafJurnalAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 3173385938131248092L;

	private MyGrid grid;

	private MyDatebox searchmulai;
	private MyDatebox searchsampai;

	private MyToolbarbuttonConfig find;

	private Html summaryHtml;
	private Label processInfo;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}
		if (searchmulai != null) { searchmulai.setReadonly(true); }
		if (searchsampai != null) { searchsampai.setReadonly(true); }

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
		if (searchmulai != null) { searchmulai.setValue(calendar.getTime()); }
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (searchsampai != null) { searchsampai.setValue(calendar.getTime()); }

		loadDataDenganProgressPosting(null);

		if (find != null) {
			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download", "/img/print.png");
			toolbarbutton.setParent(find.getParent());
			toolbarbutton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					UIUtil.downloadGrid(grid);
				}
			});
		}
	}

	// ============ MUAT DASBOR MULTI-THREAD: compute PARALEL, render sekuensial di event-thread ============
	/** Snapshot rentang tanggal agar dibaca AMAN oleh thread pool (jangan akses komponen ZK lintas-thread). */
	private volatile java.util.Date snapMulaiVal;
	private volatile java.util.Date snapSampaiVal;
	/** Kumpulan baris hasil hitung (thread-safe) sebelum dirender ke grid di event-thread. */
	private final java.util.List<DraftRow> draftCollector = java.util.Collections
			.synchronizedList(new java.util.ArrayList<DraftRow>());
	/** Urutan tampilan per-jenis + sub-urutan (di-set per task) agar hasil paralel tetap terurut. */
	private final ThreadLocal<Integer> orderTL = new ThreadLocal<Integer>();
	private final ThreadLocal<Integer> subTL = new ThreadLocal<Integer>();

	private java.util.Date snapMulai() {
		java.util.Date s = snapMulaiVal;
		return s != null ? s : (searchmulai == null ? null : searchmulai.getValue());
	}

	private java.util.Date snapSampai() {
		java.util.Date s = snapSampaiVal;
		return s != null ? s : (searchsampai == null ? null : searchsampai.getValue());
	}

	/** Data satu baris jurnal dasbor (hasil hitung) — dirender belakangan di event-thread. */
	private static final class DraftRow {
		final int order;
		final int sub;
		final String nama;
		final int draft;
		final int posting;
		final int closing;
		final String draftUrl;
		final String postingUrl;
		final String closingUrl;
		final String keterangan;

		DraftRow(int order, int sub, String nama, int draft, int posting, int closing, String draftUrl,
				String postingUrl, String closingUrl, String keterangan) {
			this.order = order;
			this.sub = sub;
			this.nama = nama;
			this.draft = draft;
			this.posting = posting;
			this.closing = closing;
			this.draftUrl = draftUrl;
			this.postingUrl = postingUrl;
			this.closingUrl = closingUrl;
			this.keterangan = keterangan;
		}
	}

	/** Produsen satu jenis jurnal: menghitung memakai session dedikasi (dijalankan di thread pool). */
	private interface Produsen {
		void jalankan(Session session) throws Exception;
	}

	/** Bungkus satu produsen jadi task: set urutan (ThreadLocal), buka session sendiri, tutup di finally. */
	private Runnable buatTask(final int order, final Produsen p) {
		return new Runnable() {
			@Override
			public void run() {
				orderTL.set(Integer.valueOf(order));
				subTL.set(Integer.valueOf(0));
				Session s = null;
				try {
					s = HibernateUtil.openSession();
					p.jalankan(s);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				} finally {
					closeOpenedSession(s);
					orderTL.remove();
					subTL.remove();
				}
			}
		};
	}

	private void onSearchDefaultTanpaProgress(Event event) {
		final Rows rows = grid.getRows() == null ? new Rows() : grid.getRows();
		if (rows.getParent() == null) {
			grid.appendChild(rows);
		}
		Common.clear(rows);
		// Snapshot tanggal (dibaca aman lintas-thread) + reset kumpulan hasil.
		snapMulaiVal = searchmulai == null ? null : searchmulai.getValue();
		snapSampaiVal = searchsampai == null ? null : searchsampai.getValue();
		draftCollector.clear();
		try {
			showProcessInfo("Menghitung draft, jurnal terposting, dan closing (paralel).");

			// Produsen tiap jenis jurnal (urutan tampilan dipertahankan lewat 'order').
			final java.util.List<Runnable> tugas = new java.util.ArrayList<Runnable>();
			int o = 0;
			tugas.add(buatTask(o++, new Produsen() { public void jalankan(Session s) { jurnalUmum(rows, s); } }));
			tugas.add(buatTask(o++, new Produsen() { public void jalankan(Session s) { lpj(rows, s); } }));
			tugas.add(buatTask(o++, new Produsen() { public void jalankan(Session s) { kaskecil(rows, s); } }));
			tugas.add(buatTask(o++, new Produsen() { public void jalankan(Session s) { kasbesar(rows, s); } }));
			tugas.add(buatTask(o++, new Produsen() { public void jalankan(Session s) { pajak(rows, s); } }));
			tugas.add(buatTask(o++, new Produsen() { public void jalankan(Session s) { penerimaanTagihanVendor(rows, s); } }));
			tugas.add(buatTask(o++, new Produsen() { public void jalankan(Session s) { pekerjaanVendor(rows, s); } }));
			tugas.add(buatTask(o++, new Produsen() { public void jalankan(Session s) { dPVendor(rows, s); } }));
			tugas.add(buatTask(o++, new Produsen() { public void jalankan(Session s) { dPPekerjaanVendor(rows, s); } }));
			tugas.add(buatTask(o++, new Produsen() { public void jalankan(Session s) { jurnalBalikDPPekerjaan(rows, s); } }));
			tugas.add(buatTask(o++, new Produsen() { public void jalankan(Session s) { pembayaranGaji(rows, s); } }));
			tugas.add(buatTask(o++, new Produsen() { public void jalankan(Session s) { pembayaranMahasiswa(rows, s); } }));
			tugas.add(buatTask(o++, new Produsen() { public void jalankan(Session s) { pembayaranSiswa(rows, s); } }));
			tugas.add(buatTask(o++, new Produsen() { public void jalankan(Session s) { jurnalPenyusutan(rows, s); } }));
			tugas.add(buatTask(o++, new Produsen() { public void jalankan(Session s) { jurnalPengajuanTransfer(rows, s); } }));
			tugas.add(buatTask(o++, new Produsen() { public void jalankan(Session s) { transitori(rows, s); } }));
			tugas.add(buatTask(o++, new Produsen() { public void jalankan(Session s) { closing(rows, s); } }));
			tugas.add(buatTask(o++, new Produsen() { public void jalankan(Session s) { postingHpp(rows, s); } }));

			// Eksekusi PARALEL — sebanyak jumlah jenis yang dimuat, MAKSIMAL 50 thread.
			java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors
					.newFixedThreadPool(Math.max(1, Math.min(50, tugas.size())));
			try {
				for (Runnable r : tugas) {
					pool.submit(r);
				}
				pool.shutdown();
				pool.awaitTermination(10, java.util.concurrent.TimeUnit.MINUTES);
			} finally {
				try {
					pool.shutdownNow();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/dashboard/akunting/DrafJurnalAction.java:236");
				}
			}

			// RENDER terurut di EVENT-THREAD (Rows ZK tidak thread-safe).
			java.util.List<DraftRow> hasil;
			synchronized (draftCollector) {
				hasil = new java.util.ArrayList<DraftRow>(draftCollector);
			}
			java.util.Collections.sort(hasil, new java.util.Comparator<DraftRow>() {
				@Override
				public int compare(DraftRow a, DraftRow b) {
					return a.order != b.order ? (a.order - b.order) : (a.sub - b.sub);
				}
			});
			for (DraftRow r : hasil) {
				PostingJurnalHelper.tambahBarisJurnal(rows, r.nama, r.draft, r.posting, r.closing, r.draftUrl,
						r.postingUrl, r.closingUrl, r.keterangan, postingDispatcher, batalkanDispatcher);
			}

			updateSummaryDashboard(rows);
			showProcessInfo("Data draft jurnal sudah diperbarui sesuai rentang tanggal.");
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			showProcessInfo("Data belum berhasil dimuat. Silakan coba ulang atau hubungi administrator.");
		} finally {
			snapMulaiVal = null;
			snapSampaiVal = null;
		}
	}

	// =====================================================================
	// BARIS JURNAL UMUM & AKUNTING
	// =====================================================================

	private Criteria kriteriaJurnalUmum(Session session) {
		return session.createCriteria(GrupTransaksi.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("jenisJurnal", Transaksi.JURNAL_UMUM))
				.add(Restrictions.sqlRestriction(dateSql("this_.tanggal_transaksi")))
				.add(Restrictions.isNull("closing"));
	}

	private void jurnalUmum(Rows rows, Session session) {
		int closing = 0;
		try {
			closing = PostingJurnalHelper.hitung(session.createCriteria(GrupTransaksi.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("jenisJurnal", Transaksi.JURNAL_UMUM))
					.add(Restrictions.sqlRestriction(dateSql("this_.tanggal_transaksi")))
					.add(Restrictions.isNotNull("closing")));
		} catch (Exception e) {
			rollbackIfNeeded(session);
			Common.tampilErrorJikaAdmin(e);
		}
		appendDraftRow(rows, "Jurnal Umum", hitungPostingHistory(session, kriteriaJurnalUmum(session), false),
				hitungPostingHistory(session, kriteriaJurnalUmum(session), true), closing,
				urlGrupTransaksiTanggal("sudah_posting=false"), urlGrupTransaksiTanggal("sudah_posting=true"),
				urlGrupTransaksiTanggal("sudah_closing=true"),
				"Jurnal umum manual dipantau dari draft, terposting, sampai terkunci closing.");
	}

	private Criteria kriteriaLpj(Session session) {
		/* disetujuiOleh ikut difilter, sama dengan PostingPertangungjawabanAction. */
		return session.createCriteria(Pertangungjawaban.class).setProjection(Projections.rowCount())
				.add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"))
				.add(Restrictions.isNotNull("disetujuiOleh"))
				.add(Restrictions.sqlRestriction(dateSql("this_.tanggal_persetujuan")));
	}

	private void lpj(Rows rows, Session session) {
		appendDraftRow(rows, "Uang Muka", hitungPostingHistory(session, kriteriaLpj(session), false),
				hitungPostingHistory(session, kriteriaLpj(session), true),
				hitungClosing(session, "pertangungjawaban", null, null),
				urlPosting("/pages/master/akunting/posting_pertanggunjawaban.zul", false),
				urlPosting("/pages/master/akunting/posting_pertanggunjawaban.zul", true),
				urlClosing("pertangungjawaban", null, null),
				"Pertanggungjawaban uang muka yang disetujui dipastikan menjadi jurnal.");
	}

	private Criteria kriteriaKasKecil(Session session) {
		/* disetujuiOleh ikut difilter, sama dengan PostingKasKecilAction. */
		return session.createCriteria(KasKecil.class).setProjection(Projections.rowCount())
				.add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"))
				.add(Restrictions.isNotNull("disetujuiOleh"))
				.add(Restrictions.sqlRestriction(dateSql("this_.tanggal_persetujuan")));
	}

	private void kaskecil(Rows rows, Session session) {
		appendDraftRow(rows, "Kas Kecil", hitungPostingHistory(session, kriteriaKasKecil(session), false),
				hitungPostingHistory(session, kriteriaKasKecil(session), true),
				hitungClosing(session, "kasKecil", null, null),
				urlPosting("/pages/master/akunting/posting_kas_kecil.zul", false),
				urlPosting("/pages/master/akunting/posting_kas_kecil.zul", true),
				urlClosing("kasKecil", null, null),
				"Pengeluaran kas kecil yang disetujui dipantau sampai menjadi jurnal kas.");
	}

	private Criteria kriteriaKasBesar(Session session) {
		/* disetujuiOleh ikut difilter, sama dengan PostingKasBesarAction. */
		return session.createCriteria(KasBesar.class).setProjection(Projections.rowCount())
				.add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"))
				.add(Restrictions.isNotNull("disetujuiOleh"))
				.add(Restrictions.sqlRestriction(dateSql("this_.tanggal_persetujuan")));
	}

	private void kasbesar(Rows rows, Session session) {
		appendDraftRow(rows, "Kas Besar", hitungPostingHistory(session, kriteriaKasBesar(session), false),
				hitungPostingHistory(session, kriteriaKasBesar(session), true),
				hitungClosing(session, "kasBesar", null, null),
				urlPosting("/pages/master/akunting/posting_kas_besar.zul", false),
				urlPosting("/pages/master/akunting/posting_kas_besar.zul", true),
				urlClosing("kasBesar", null, null),
				"Transaksi kas besar yang disetujui dipantau sampai menjadi jurnal kas/bank.");
	}

	private Criteria kriteriaPajak(Session session) {
		return session.createCriteria(Pajak.class).setProjection(Projections.rowCount())
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"))
				.add(Restrictions.sqlRestriction(dateSql("this_.tanggal_transaksi")));
	}

	private void pajak(Rows rows, Session session) {
		appendDraftRow(rows, "Pajak", hitungPostingHistory(session, kriteriaPajak(session), false),
				hitungPostingHistory(session, kriteriaPajak(session), true),
				hitungClosing(session, "pajak", null, null),
				urlPosting("/pages/master/akunting/posting_pertanggunjawaban_pajak.zul", false),
				urlPosting("/pages/master/akunting/posting_pertanggunjawaban_pajak.zul", true),
				urlClosing("pajak", null, null),
				"Setoran pajak yang tercatat dipastikan sudah memiliki jurnal pajak.");
	}

	// =====================================================================
	// BARIS VENDOR / ASSET
	// =====================================================================

	private Criteria kriteriaPenerimaanTagihanVendor(Session session) {
		return session.createCriteria(SaldoAwalMasterAsset.class).setProjection(Projections.rowCount())
				.add(Restrictions.isNull("jsonTermin")).add(Restrictions.isNotNull("disetujuiOleh"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"))
				.add(Restrictions.sqlRestriction(dateSql("this_.tanggal_persetujuan")));
	}

	private void penerimaanTagihanVendor(Rows rows, Session session) {
		appendDraftRow(rows, "Penerimaan Tagihan Vendor",
				hitungPostingHistory(session, kriteriaPenerimaanTagihanVendor(session), false),
				hitungPostingHistory(session, kriteriaPenerimaanTagihanVendor(session), true),
				hitungClosing(session, "saldoAwalMasterAsset", PostingJurnalHelper.REF_KOSONG, null),
				urlPosting("/pages/master/asset/posting_pengadaan.zul", false),
				urlPosting("/pages/master/asset/posting_pengadaan.zul", true),
				urlClosing("saldoAwalMasterAsset", PostingJurnalHelper.REF_KOSONG, null),
				"Tagihan vendor non termin yang disetujui dipantau sampai menjadi jurnal utang.");
	}

	private Criteria kriteriaPekerjaanVendor(Session session) {
		return session.createCriteria(SaldoAwalMasterAsset.class).setProjection(Projections.rowCount())
				.add(Restrictions.isNotNull("penerimaanPengadaanMasterAsset"))
				.add(Restrictions.isNotNull("jsonTermin")).add(Restrictions.isNotNull("disetujuiOleh"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"))
				.add(Restrictions.sqlRestriction(dateSql("this_.tanggal_persetujuan")));
	}

	private void pekerjaanVendor(Rows rows, Session session) {
		appendDraftRow(rows, "Pekerjaan Vendor",
				hitungPostingHistory(session, kriteriaPekerjaanVendor(session), false),
				hitungPostingHistory(session, kriteriaPekerjaanVendor(session), true),
				hitungClosing(session, "saldoAwalMasterAsset", PostingJurnalHelper.REF_PEKERJAAN_NON_DP, null),
				urlPosting("/pages/master/asset/posting_pesanan_pekerjaan.zul", false),
				urlPosting("/pages/master/asset/posting_pesanan_pekerjaan.zul", true),
				urlClosing("saldoAwalMasterAsset", PostingJurnalHelper.REF_PEKERJAAN_NON_DP, null),
				"Termin pekerjaan vendor yang diterima dipantau sampai menjadi jurnal.");
	}

	private Criteria kriteriaDpVendor(Session session) {
		return session.createCriteria(PemesananPengadaanMasterAsset.class).setProjection(Projections.rowCount())
				.add(Restrictions.isNotNull("disetujuiOleh"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.ne("dp", 0.0)).add(Restrictions.isNotNull("dp"))
				.add(Restrictions.sqlRestriction(dateSql("this_.tanggal_persetujuan")));
	}

	private void dPVendor(Rows rows, Session session) {
		appendDraftRow(rows, "DP Vendor", hitungPostingHistory(session, kriteriaDpVendor(session), false),
				hitungPostingHistory(session, kriteriaDpVendor(session), true),
				hitungClosing(session, "pemesananPengadaanMasterAsset", PostingJurnalHelper.REF_KOSONG, null),
				urlPosting("/pages/master/asset/posting_dp.zul", false),
				urlPosting("/pages/master/asset/posting_dp.zul", true),
				urlClosing("pemesananPengadaanMasterAsset", PostingJurnalHelper.REF_KOSONG, null),
				"Uang muka pemesanan vendor dipantau sampai menjadi jurnal uang muka.");
	}

	private Criteria kriteriaDpPekerjaanVendor(Session session) {
		return session.createCriteria(SaldoAwalMasterAsset.class).setProjection(Projections.rowCount())
				.add(Restrictions.isNotNull("penerimaanPengadaanMasterAsset"))
				.add(Restrictions.ilike("jsonTermin", "\"merupakan_dp\":true", MatchMode.ANYWHERE))
				.add(Restrictions.isNotNull("jsonTermin")).add(Restrictions.isNotNull("disetujuiOleh"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"))
				.add(Restrictions.sqlRestriction(dateSql("this_.tanggal_persetujuan")));
	}

	private void dPPekerjaanVendor(Rows rows, Session session) {
		appendDraftRow(rows, "DP Pekerjaan Vendor",
				hitungPostingHistory(session, kriteriaDpPekerjaanVendor(session), false),
				hitungPostingHistory(session, kriteriaDpPekerjaanVendor(session), true),
				hitungClosing(session, "saldoAwalMasterAsset", PostingJurnalHelper.REF_DP_PEKERJAAN, null),
				urlPosting("/pages/master/asset/posting_dp_pesanan_pekerjaan.zul", false),
				urlPosting("/pages/master/asset/posting_dp_pesanan_pekerjaan.zul", true),
				urlClosing("saldoAwalMasterAsset", PostingJurnalHelper.REF_DP_PEKERJAAN, null),
				"Termin DP pekerjaan vendor dipisahkan agar jurnal uang muka tetap rapi.");
	}

	private Criteria kriteriaJurnalBalikDpPekerjaan(Session session) {
		return session.createCriteria(PemesananPengadaanMasterAsset.class).setProjection(Projections.rowCount())
				.add(Restrictions.isNotNull("disetujuiOleh"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.ilike("formula", "\"setuju\":true", MatchMode.ANYWHERE))
				.add(Restrictions.ilike("formula", "\"merupakan_dp\":true", MatchMode.ANYWHERE))
				.add(Restrictions.eq("byTermin", true))
				.add(Restrictions.sqlRestriction(dateSql("this_.tanggal_persetujuan")));
	}

	private void jurnalBalikDPPekerjaan(Rows rows, Session session) {
		appendDraftRow(rows, "Jurnal Balik DP Pekerjaan",
				hitungPostingHistory(session, kriteriaJurnalBalikDpPekerjaan(session), false),
				hitungPostingHistory(session, kriteriaJurnalBalikDpPekerjaan(session), true),
				hitungClosing(session, "pemesananPengadaanMasterAsset", PostingJurnalHelper.REF_DP_BALIK_PEKERJAAN,
						null),
				urlPosting("/pages/master/asset/posting_dp_balik_pesanan_pekerjaan.zul", false),
				urlPosting("/pages/master/asset/posting_dp_balik_pesanan_pekerjaan.zul", true),
				urlClosing("pemesananPengadaanMasterAsset", PostingJurnalHelper.REF_DP_BALIK_PEKERJAAN, null),
				"Jurnal balik DP pekerjaan dipantau supaya uang muka tidak terhitung ganda.");
	}

	// =====================================================================
	// BARIS PAYROLL
	// =====================================================================

	private Criteria kriteriaPembayaranGaji(Session session) {
		return session.createCriteria(PembayaranGaji.class).setProjection(Projections.rowCount())
				.add(Restrictions.isNotNull("standingInstruction")).add(Restrictions.isNotNull("disetujuiOleh"))
				.add(Restrictions.sqlRestriction(dateSql("this_.waktubayar")));
	}

	private void pembayaranGaji(Rows rows, Session session) {
		appendDraftRow(rows, "Gaji", hitungPostingHistory(session, kriteriaPembayaranGaji(session), false),
				hitungPostingHistory(session, kriteriaPembayaranGaji(session), true),
				hitungClosing(session, "pembayaranGaji", null, null),
				urlPosting("/pages/master/payroll/posting_transaksi_transaksi_penggajian.zul", false),
				urlPosting("/pages/master/payroll/posting_transaksi_transaksi_penggajian.zul", true),
				urlClosing("pembayaranGaji", null, null),
				"Pembayaran gaji yang disetujui dipastikan sudah menjadi jurnal beban gaji.");
	}

	// =====================================================================
	// BARIS MAHASISWA
	// =====================================================================

	private void pembayaranMahasiswa(Rows rows, Session session) {
		appendDraftRow(rows, "Mahasiswa - Piutang Tagihan", countDetailKegiatan(session, false),
				countDetailKegiatan(session, true), hitungClosing(session, "detailKegiatan", null, null),
				urlPosting("/pages/master/posting_piutang_mahasiswa.zul", false),
				urlPosting("/pages/master/posting_piutang_mahasiswa.zul", true),
				urlClosing("detailKegiatan", null, null),
				"Tagihan mahasiswa yang sudah muncul bisa dicek sebelum dibuat jurnal piutang.");

		/* Filter nilai disamakan dengan PostingCicilanMahasiswaAction. */
		appendDraftRow(rows, "Mahasiswa - Pembayaran", countPostingByDateSql(session, CicilanPembayaran.class,
				"postingHistory", "this_.tanggal", "this_.nilai is not null and this_.nilai <> 0", false),
				countPostingByDateSql(session, CicilanPembayaran.class, "postingHistory", "this_.tanggal",
						"this_.nilai is not null and this_.nilai <> 0", true),
				hitungClosing(session, "cicilanPembayaran", PostingJurnalHelper.REF_KOSONG, null),
				urlPosting("/pages/master/posting_cicilan_mahasiswa.zul", false),
				urlPosting("/pages/master/posting_cicilan_mahasiswa.zul", true),
				urlClosing("cicilanPembayaran", PostingJurnalHelper.REF_KOSONG, null),
				"Pembayaran mahasiswa yang masuk dapat dipastikan sudah memiliki jurnal penerimaan.");

		/* Filter nilai disamakan dengan PostingCicilanDibayarDimukaMahasiswaAction. */
		appendDraftRow(rows, "Mahasiswa - Dibayar Dimuka", countPostingByDateSql(session, CicilanPembayaran.class,
				"postingHistoryDimuka", "this_.tanggal_tagihan",
				"date(this_.tanggal)<date(this_.tanggal_tagihan) and this_.nilai is not null and this_.nilai <> 0", false),
				countPostingByDateSql(session, CicilanPembayaran.class, "postingHistoryDimuka", "this_.tanggal_tagihan",
						"date(this_.tanggal)<date(this_.tanggal_tagihan) and this_.nilai is not null and this_.nilai <> 0", true),
				hitungClosing(session, "cicilanPembayaran", PostingJurnalHelper.REF_DIMUKA, null),
				urlPosting("/pages/master/posting_cicilan_mahasiswa_dibayar_dimuka.zul", false),
				urlPosting("/pages/master/posting_cicilan_mahasiswa_dibayar_dimuka.zul", true),
				urlClosing("cicilanPembayaran", PostingJurnalHelper.REF_DIMUKA, null),
				"Pembayaran lebih awal dipisahkan agar pendapatan dan uang muka tidak tercampur.");

		appendDraftRow(rows, "Mahasiswa - Tabungan/Deposit", countDepositMahasiswa(session, false),
				countDepositMahasiswa(session, true), hitungClosing(session, "deposit", null, null),
				urlPosting("/pages/master/posting_tabungan_mahasiswa.zul", false),
				urlPosting("/pages/master/posting_tabungan_mahasiswa.zul", true), urlClosing("deposit", null, null),
				"Dana titipan mahasiswa terlihat terpisah dari pembayaran tagihan biasa.");

		appendDraftRow(rows, "Mahasiswa - Pengeluaran/Refund", countPengeluaranMahasiswa(session, false),
				countPengeluaranMahasiswa(session, true), hitungClosing(session, "pengeluaranMahasiswa", null, null),
				urlPosting("/pages/master/posting_pengeluaran_mahasiswa.zul", false),
				urlPosting("/pages/master/posting_pengeluaran_mahasiswa.zul", true),
				urlClosing("pengeluaranMahasiswa", null, null),
				"Pengeluaran kepada mahasiswa dapat dipantau sebelum menjadi jurnal kas keluar.");

		appendDraftRow(rows, "Mahasiswa - Biaya Administrasi", countLogPembayaran(session, "postingHistory", false),
				countLogPembayaran(session, "postingHistory", true),
				hitungClosing(session, "logPembayaran", null, null),
				urlPosting("/pages/master/posting_biaya_administrasi_pembayaran_mahasiswa.zul", false),
				urlPosting("/pages/master/posting_biaya_administrasi_pembayaran_mahasiswa.zul", true),
				urlClosing("logPembayaran", null, null),
				"Biaya administrasi pembayaran dipisahkan agar pendapatan biaya layanan mudah diperiksa.");

		appendDraftRow(rows, "Mahasiswa - Biaya Payment Gateway", countLogPembayaran(session,
				"postingHistoryPaymentGateway", false), countLogPembayaran(session, "postingHistoryPaymentGateway", true),
				hitungClosing(session, "logPembayaran", PostingJurnalHelper.REF_PAYMENT_GATEWAY, null),
				urlPosting("/pages/master/posting_biaya_payment_gateway_pembayaran_mahasiswa.zul", false),
				urlPosting("/pages/master/posting_biaya_payment_gateway_pembayaran_mahasiswa.zul", true),
				urlClosing("logPembayaran", PostingJurnalHelper.REF_PAYMENT_GATEWAY, null),
				"Biaya dari kanal pembayaran online bisa dicek sebelum diposting ke akun biaya layanan.");
	}

	// =====================================================================
	// BARIS SISWA
	// =====================================================================

	private void pembayaranSiswa(Rows rows, Session session) {
		appendDraftRow(rows, "Siswa - Piutang Tagihan", countTagihanSiswaPiutang(session, false),
				countTagihanSiswaPiutang(session, true), hitungClosing(session, "tagihan", null, "PIUTANG_SISWA"),
				urlPosting("/pages/master/sekolah/posting_piutang.zul", false),
				urlPosting("/pages/master/sekolah/posting_piutang.zul", true),
				urlClosing("tagihan", null, "PIUTANG_SISWA"),
				"Tagihan siswa yang perlu dibuat jurnal piutang mudah dilihat dari satu tempat.");

		appendDraftRow(rows, "Siswa - Pembayaran", countPembayaranSiswaDetail(session, false),
				countPembayaranSiswaDetail(session, true), hitungClosing(session, "pembayaranSiswaDetail", null, null),
				urlPosting("/pages/master/sekolah/posting_pembayaran.zul", false),
				urlPosting("/pages/master/sekolah/posting_pembayaran.zul", true),
				urlClosing("pembayaranSiswaDetail", null, null),
				"Pembayaran siswa yang masuk dapat dipastikan sudah berubah menjadi jurnal penerimaan.");

		appendDraftRow(rows, "Siswa - Dibayar Dimuka", countTagihanSiswaDibayarDimuka(session, false),
				countTagihanSiswaDibayarDimuka(session, true),
				hitungClosing(session, "tagihan", null, "PEMBAYARAN_SISWA_DIBAYAR_DIMUKA"),
				urlPosting("/pages/master/sekolah/posting_dibayar_dimuka.zul", false),
				urlPosting("/pages/master/sekolah/posting_dibayar_dimuka.zul", true),
				urlClosing("tagihan", null, "PEMBAYARAN_SISWA_DIBAYAR_DIMUKA"),
				"Pembayaran sebelum masa tagihan dipisahkan agar uang muka tetap rapi.");

		appendDraftRow(rows, "Siswa - Deposit", countPostingByDateSql(session, DepositSiswa.class,
				"postingHistory", "tanggal_bayar", null, false),
				countPostingByDateSql(session, DepositSiswa.class, "postingHistory", "tanggal_bayar", null, true),
				hitungClosing(session, "depositSiswa", null, null),
				urlPosting("/pages/master/sekolah/posting_deposit.zul", false),
				urlPosting("/pages/master/sekolah/posting_deposit.zul", true), urlClosing("depositSiswa", null, null),
				"Deposit siswa dipantau terpisah dari pembayaran tagihan agar saldo titipan tetap jelas.");

		appendDraftRow(rows, "Siswa - Piutang Denda", countTagihanSiswaPiutangDenda(session, false),
				countTagihanSiswaPiutangDenda(session, true),
				hitungClosing(session, "tagihan", null, "PIUTANG_DENDA_SISWA"),
				urlPosting("/pages/master/sekolah/posting_piutang_denda.zul", false),
				urlPosting("/pages/master/sekolah/posting_piutang_denda.zul", true),
				urlClosing("tagihan", null, "PIUTANG_DENDA_SISWA"),
				"Denda yang sudah terbentuk dapat dipastikan masuk ke jurnal denda yang benar.");

		appendDraftRow(rows, "Siswa - Utang Diskon", countTagihanSiswaUtangDiskon(session, false),
				countTagihanSiswaUtangDiskon(session, true),
				hitungClosing(session, "tagihan", null, "UTANG_DISKON_SISWA"),
				urlPosting("/pages/master/sekolah/posting_utang_diskon.zul", false),
				urlPosting("/pages/master/sekolah/posting_utang_diskon.zul", true),
				urlClosing("tagihan", null, "UTANG_DISKON_SISWA"),
				"Diskon yang harus dibayar balik atau dicatat sebagai utang terlihat jelas sebelum diposting.");
	}

	// =====================================================================
	// BARIS ASSET / TRANSFER / TRANSITORI / CLOSING
	// =====================================================================

	/** Jenis jurnal "Penyusutan" — 3 sub-tab: Fix Aset (BAST), Aset dalam Pekerjaan (BAST), Penyusutan. */
	private void jurnalPenyusutan(Rows rows, Session session) {
		// (1) Fix Aset (Jurnal Saat BAST): penerimaan barang/jasa (BAST) aset TETAP (bukan CIP).
		appendDraftRow(rows, "Fix Aset (Jurnal Saat BAST)", countPenerimaanBast(session, false, "fixasset"),
				countPenerimaanBast(session, true, "fixasset"),
				hitungClosing(session, "penerimaanPengadaanMasterAsset", null, null),
				"/pages/master/asset/posting_saldo_awal_asset.zul?filterKelompok=fixasset",
				"/pages/master/asset/posting_saldo_awal_asset.zul?filterKelompok=fixasset",
				urlClosing("penerimaanPengadaanMasterAsset", null, null),
				"Penerimaan (BAST) aset tetap dipantau sampai menjadi jurnal aset.");

		// (2) Aset dalam Pekerjaan (Jurnal Saat BAST): BAST untuk aset dalam pekerjaan (CIP). Closing
		// per-entitas penerimaan TIDAK bisa dipisah CIP → ditampilkan di baris Fix Aset saja (baris ini 0).
		appendDraftRow(rows, "Aset dalam Pekerjaan (Jurnal Saat BAST)", countPenerimaanBast(session, false, "pekerjaan"),
				countPenerimaanBast(session, true, "pekerjaan"), 0,
				"/pages/master/asset/posting_saldo_awal_asset.zul?filterKelompok=pekerjaan",
				"/pages/master/asset/posting_saldo_awal_asset.zul?filterKelompok=pekerjaan",
				urlClosing("penerimaanPengadaanMasterAsset", null, null),
				"BAST aset dalam pekerjaan (CIP) dipantau sampai menjadi jurnal.");

		// (3) Penyusutan.
		appendDraftRow(rows, "Jurnal Penyusutan", countPenyusutan(session, false), countPenyusutan(session, true),
				hitungClosing(session, "penyusutanAsset", null, null),
				urlPosting("/pages/master/asset/posting_penyusutan_asset.zul", false),
				urlPosting("/pages/master/asset/posting_penyusutan_asset.zul", true),
				urlClosing("penyusutanAsset", null, null),
				"Nilai penyusutan aset dapat dicek sebelum dicatat sebagai beban penyusutan.");
	}

	/** EXISTS CIP: BAST punya detail-aset yang kelompoknya "pekerjaan dalam pelaksanaan" (CIP). */
	private static final String SQL_EXISTS_KELOMPOK_CIP = "exists (select 1 from asset.penerimaan_pengadaan_master_asset_detail d "
			+ "join asset.master_asset m on d.masterasset = m.id "
			+ "join asset.kelompok_asset k on m.kelompok_asset = k.id "
			+ "where d.penerimaan_pengadaan_master_asset = this_.id "
			+ "and coalesce(k.merupakanpekerjaandalampelaksanaan, false) = true)";

	/**
	 * Hitung BAST (PenerimaanPengadaanMasterAsset) draft/posting. {@code filterKelompok}: "fixasset"
	 * (bukan CIP), "pekerjaan" (CIP), atau null (semua). Kriteria sama dengan halaman posting BAST
	 * (posting_saldo_awal_asset.zul): disetujui, nilai&ne;0, rentang tanggal persetujuan, status
	 * posting = postingHistory null/notnull.
	 */
	private int countPenerimaanBast(Session session, boolean posted, String filterKelompok) {
		try {
			Criteria c = session.createCriteria(PenerimaanPengadaanMasterAsset.class)
					.setProjection(Projections.rowCount())
					.createAlias("pemesananPengadaanMasterAsset", "pemesananPengadaanMasterAsset")
					.add(Restrictions.isNotNull("disetujuiOleh")).add(Restrictions.ne("nilai", 0.0))
					.add(Restrictions.isNotNull("nilai"))
					.add(Restrictions.sqlRestriction(dateSql("this_.tanggal_persetujuan")))
					.add(PostingJurnalHelper.restriksiPosting("postingHistory", Boolean.valueOf(posted)));
			if ("pekerjaan".equals(filterKelompok)) {
				c.add(Restrictions.sqlRestriction(SQL_EXISTS_KELOMPOK_CIP));
			} else if ("fixasset".equals(filterKelompok)) {
				c.add(Restrictions.sqlRestriction("not " + SQL_EXISTS_KELOMPOK_CIP));
			}
			return PostingJurnalHelper.hitung(c);
		} catch (Exception e) {
			rollbackIfNeeded(session);
			Common.tampilErrorJikaAdmin(e);
			return 0;
		}
	}

	private void jurnalPengajuanTransfer(Rows rows, Session session) {
		appendDraftRow(rows, "Jurnal Pengajuan Transfer", countPengajuanTransfer(session, false),
				countPengajuanTransfer(session, true), hitungClosing(session, "daftarPengajuanTransfer", null, null),
				urlPosting("/pages/master/akunting/posting_proses_transfer.zul", false),
				urlPosting("/pages/master/akunting/posting_proses_transfer.zul", true),
				urlClosing("daftarPengajuanTransfer", null, null),
				"Transfer yang sudah direalisasikan dapat dipastikan sudah menjadi jurnal kas/bank.");
	}

	private void transitori(Rows rows, Session session) {
		appendDraftRow(rows, "Transitori", countTransitori(session, false), countTransitori(session, true),
				hitungClosing(session, "transitori", null, null),
				urlPosting("/pages/master/akunting/posting_proses_transitori.zul", false),
				urlPosting("/pages/master/akunting/posting_proses_transitori.zul", true),
				urlClosing("transitori", null, null),
				"Transaksi perantara dapat dikawal sampai saldo berpindah ke akun tujuan.");
	}

	private void closing(Rows rows, Session session) {
		int belumClosing = countGrupTransaksiClosing(session, false);
		int sudahClosing = countGrupTransaksiClosing(session, true);
		appendDraftRow(rows, "Closing", belumClosing, sudahClosing, sudahClosing,
				urlGrupTransaksiTanggal("sudah_closing=false&semua_jurnal=true"),
				urlGrupTransaksiTanggal("sudah_closing=true&semua_jurnal=true"),
				urlGrupTransaksiTanggal("sudah_closing=true&semua_jurnal=true"),
				"Jurnal yang belum dan sudah dikunci pada periode ini terlihat jelas sebelum periode ditutup.");
	}

	/**
	 * "Posting HPP": beban pokok penjualan barang kantin yang tertaut persediaan aset (lihat
	 * {@link ais.action.master.koperasi.PostingHppKantinAction}). Modelnya BEDA dari baris lain
	 * di sini -- bukan per-transaksi (postingHistory di tiap baris), tapi per PERIODE (admin pilih
	 * rentang tanggal, satu jurnal batch untuk semua barang sekaligus). Karena itu "Draft" berarti
	 * jumlah barang persediaan yang HPP-nya belum masuk batch manapun sejak batch terakhir (bukan
	 * jumlah baris transaksi), dan "Terposting" berarti jumlah BATCH yang sudah pernah diposting.
	 * "Closing" tidak dihitung terpisah di sini -- begitu sebuah batch diposting, ia menjadi jurnal
	 * biasa yang statusnya sudah tercakup di baris "Closing" di atas.
	 *
	 * <p>Baris ini hanya tampil bila admin mengaktifkan tab "Posting HPP" di Konfigurasi &gt; Posting
	 * Jurnal (kunci {@code Konfigurasi.POSTING_JURNAL_TAB_PREFIX + "posting_hpp"}), supaya dasbor
	 * yang belum memakai modul Kantin/HPP tidak terganggu baris yang tidak relevan.</p>
	 */
	private void postingHpp(Rows rows, Session session) {
		if (!Common.bolehKonfigurasi(ais.database.model.Konfigurasi.POSTING_JURNAL_TAB_PREFIX + "posting_hpp")) {
			return;
		}
		int draft = ais.action.master.koperasi.PostingHppKantinAction.hitungDraftPending(session);
		int posting = ais.action.master.koperasi.PostingHppKantinAction.hitungTerposting(session);
		appendDraftRow(rows, "Posting HPP", draft, posting, 0,
				"/pages/master/koperasi/posting_hpp_kantin.zul", "/pages/master/koperasi/posting_hpp_kantin.zul", null,
				"Beban pokok penjualan barang kantin yang tertaut persediaan aset -- diposting per periode "
						+ "(bukan per transaksi) dari tab \"Posting HPP\".");
	}

	// =====================================================================
	// PENGHITUNG GENERIC (delegasi ke PostingJurnalHelper)
	// =====================================================================

	/** Hitung dengan pola join posting history (draft / posting=true). */
	private int hitungPostingHistory(Session session, Criteria criteria, boolean sudahPosting) {
		try {
			PostingJurnalHelper.terapkanStatusPostingHistory(criteria, sudahPosting);
			return PostingJurnalHelper.hitung(criteria);
		} catch (Exception e) {
			rollbackIfNeeded(session);
			Common.tampilErrorJikaAdmin(e);
			return 0;
		}
	}

	private int hitungClosing(Session session, String entitas, String ref, String jenis) {
		return PostingJurnalHelper.hitungClosing(session, entitas, ref, jenis, snapMulai(),
				snapSampai());
	}

	@SuppressWarnings("rawtypes")
	private int countPostingByDateSql(Session session, Class clazz, String postingField, String dateColumn,
			String extraSql, boolean posted) {
		try {
			Criteria criteria = session.createCriteria(clazz).setProjection(Projections.rowCount())
					.add(Restrictions.sqlRestriction(dateSql(dateColumn)));
			if (extraSql != null && extraSql.trim().length() > 0) {
				criteria.add(Restrictions.sqlRestriction(extraSql));
			}
			criteria.add(PostingJurnalHelper.restriksiPosting(postingField, Boolean.valueOf(posted)));
			return PostingJurnalHelper.hitung(criteria);
		} catch (Exception e) {
			rollbackIfNeeded(session);
			Common.tampilErrorJikaAdmin(e);
			return 0;
		}
	}

	private int countDetailKegiatan(Session session, boolean posted) {
		try {
			Criteria criteria = session.createCriteria(DetailKegiatan.class).setProjection(Projections.rowCount())
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.sqlRestriction("this_.item_biaya in (select item_biaya from item_biaya_punya_piutang where akun is not null and item_biaya is not null group by item_biaya)"))
					.add(Restrictions.isNotNull("tanggal"))
					.add(Restrictions.or(Restrictions.isNotNull("postingHistory"), Restrictions.ne("biaya", 0.0)))
					.add(Restrictions.or(Restrictions.isNotNull("postingHistory"), Restrictions.isNotNull("biaya")))
					.add(Restrictions.sqlRestriction(dateSql("this_.tanggal")))
					.add(PostingJurnalHelper.restriksiPosting("postingHistory", Boolean.valueOf(posted)));
			return PostingJurnalHelper.hitung(criteria);
		} catch (Exception e) {
			rollbackIfNeeded(session);
			Common.tampilErrorJikaAdmin(e);
			return 0;
		}
	}

	private int countDepositMahasiswa(Session session, boolean posted) {
		try {
			Criteria criteria = session.createCriteria(Deposit.class).setProjection(Projections.rowCount())
					.add(Restrictions.or(Restrictions.isNotNull("mahasiswa"), Restrictions.isNotNull("biodataCalonMahasiswa")))
					.add(Restrictions.isNotNull("waktu"))
					.add(Restrictions.or(Restrictions.isNotNull("postingHistory"), Restrictions.ne("nominal", 0.0)))
					.add(Restrictions.or(Restrictions.isNotNull("postingHistory"), Restrictions.isNotNull("nominal")))
					.add(Restrictions.sqlRestriction(dateSql("this_.waktu")))
					.add(PostingJurnalHelper.restriksiPosting("postingHistory", Boolean.valueOf(posted)));
			return PostingJurnalHelper.hitung(criteria);
		} catch (Exception e) {
			rollbackIfNeeded(session);
			Common.tampilErrorJikaAdmin(e);
			return 0;
		}
	}

	private int countPengeluaranMahasiswa(Session session, boolean posted) {
		try {
			Criteria criteria = session.createCriteria(PengeluaranMahasiswa.class).setProjection(Projections.rowCount())
					.add(Restrictions.isNotNull("waktu"))
					.add(Restrictions.or(Restrictions.isNotNull("postingHistory"), Restrictions.ne("nominal", 0.0)))
					.add(Restrictions.or(Restrictions.isNotNull("postingHistory"), Restrictions.isNotNull("nominal")))
					.add(Restrictions.sqlRestriction(dateSql("this_.waktu")))
					.add(PostingJurnalHelper.restriksiPosting("postingHistory", Boolean.valueOf(posted)));
			return PostingJurnalHelper.hitung(criteria);
		} catch (Exception e) {
			rollbackIfNeeded(session);
			Common.tampilErrorJikaAdmin(e);
			return 0;
		}
	}

	private int countLogPembayaran(Session session, String postingField, boolean posted) {
		try {
			Criteria criteria = session.createCriteria(LogPembayaran.class).setProjection(Projections.rowCount())
					.add(Restrictions.sqlRestriction(dateSql("this_.tanggal")))
					.add(PostingJurnalHelper.restriksiPosting(postingField, Boolean.valueOf(posted)));
			if ("postingHistoryPaymentGateway".equals(postingField)) {
				criteria.add(Restrictions.gt("biayaPaymentGateway", 0.1));
			} else {
				criteria.add(Restrictions.gt("biayaAdministrasi", 0.1));
			}
			return PostingJurnalHelper.hitung(criteria);
		} catch (Exception e) {
			rollbackIfNeeded(session);
			Common.tampilErrorJikaAdmin(e);
			return 0;
		}
	}

	private int countPembayaranSiswaDetail(Session session, boolean posted) {
		try {
			Date mulai = snapMulai();
			Date sampai = snapSampai();
			Criteria criteria = session.createCriteria(PembayaranSiswaDetail.class).setProjection(Projections.rowCount())
					.createAlias("pembayaranSiswa", "pembayaranSiswa").createAlias("tagihan", "tagihan")
					.add(Restrictions.between("pembayaranSiswa.tanggalBayar", mulai, sampai))
					.add(PostingJurnalHelper.restriksiPosting("postingHistory", Boolean.valueOf(posted)));
			return PostingJurnalHelper.hitung(criteria);
		} catch (Exception e) {
			rollbackIfNeeded(session);
			Common.tampilErrorJikaAdmin(e);
			return 0;
		}
	}

	private int countTagihanSiswaPiutang(Session session, boolean posted) {
		try {
			Criteria criteria = session.createCriteria(Tagihan.class).setProjection(Projections.rowCount())
					.createAlias("itemBiayaSekolah", "itemBiayaSekolah")
					.add(Restrictions.eq("itemBiayaSekolah.aktif", true))
					.add(Restrictions.isNotNull("itemBiayaSekolah.akunPiutang"))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.createAlias("nominalBiaya", "nominalBiaya")
					.createAlias("nominalBiaya.pengaturanBiaya", "pengaturanBiaya")
					.createAlias("pengaturanBiaya.jenisBiayaSekolah", "jenisBiayaSekolah")
					.add(Restrictions.or(Restrictions.eq("jenisBiayaSekolah.periode", "Bulanan"),
							Restrictions.and(Restrictions.eq("bayarKe", 1),
									Restrictions.eq("jenisBiayaSekolah.periode", "Insidentil"))))
					.add(Restrictions.sqlRestriction(dateSql("tanggal_tagihan")))
					.add(PostingJurnalHelper.restriksiPosting("postingHistory", Boolean.valueOf(posted)));
			return PostingJurnalHelper.hitung(criteria);
		} catch (Exception e) {
			rollbackIfNeeded(session);
			Common.tampilErrorJikaAdmin(e);
			return 0;
		}
	}

	private int countTagihanSiswaDibayarDimuka(Session session, boolean posted) {
		try {
			Criteria criteria = session.createCriteria(Tagihan.class).setProjection(Projections.rowCount())
					.createAlias("pembayaranSiswaDetail", "pembayaranSiswaDetail")
					.createAlias("pembayaranSiswaDetail.pembayaranSiswa", "pembayaranSiswa")
					.add(Restrictions.gt("pembayaranSiswaDetail.nominal", 0.1))
					.add(Restrictions.sqlRestriction("date(tanggal_bayar)<date(tanggal_tagihan)"))
					.add(Restrictions.sqlRestriction(dateSql("tanggal_bayar")))
					.add(PostingJurnalHelper.restriksiPosting("postingHistoryUangMuka", Boolean.valueOf(posted)));
			return PostingJurnalHelper.hitung(criteria);
		} catch (Exception e) {
			rollbackIfNeeded(session);
			Common.tampilErrorJikaAdmin(e);
			return 0;
		}
	}

	private int countTagihanSiswaPiutangDenda(Session session, boolean posted) {
		try {
			Criteria criteria = session.createCriteria(Tagihan.class).setProjection(Projections.rowCount())
					.add(Restrictions.isNotNull("tanggalDeadline"))
					.add(Restrictions.gt("denda", 0.1))
					.add(Restrictions.sqlRestriction(dateSql("tanggal_tagihan")))
					.add(PostingJurnalHelper.restriksiPosting("postingHistoryDenda", Boolean.valueOf(posted)));
			return PostingJurnalHelper.hitung(criteria);
		} catch (Exception e) {
			rollbackIfNeeded(session);
			Common.tampilErrorJikaAdmin(e);
			return 0;
		}
	}

	private int countTagihanSiswaUtangDiskon(Session session, boolean posted) {
		try {
			Criteria criteria = session.createCriteria(Tagihan.class).setProjection(Projections.rowCount())
					.createAlias("diskonSiswa", "diskonSiswa")
					.add(Restrictions.eq("diskonSiswa.memotongTagihan", false))
					.add(Restrictions.isNotNull("tanggalBayar"))
					.add(Restrictions.gt("diskonTidakLangsung", 0.1))
					.add(Restrictions.sqlRestriction(dateSql("this_.tanggalbayar")))
					.add(PostingJurnalHelper.restriksiPosting("postingHistoryDiskon", Boolean.valueOf(posted)));
			return PostingJurnalHelper.hitung(criteria);
		} catch (Exception e) {
			rollbackIfNeeded(session);
			Common.tampilErrorJikaAdmin(e);
			return 0;
		}
	}

	private int countPenyusutan(Session session, boolean posted) {
		try {
			Criteria criteria = session.createCriteria(PenyusutanAsset.class).setProjection(Projections.rowCount())
					.add(Restrictions.ne("nilaiPenyusutan", 0.0)).add(Restrictions.isNotNull("nilaiPenyusutan"))
					.add(Restrictions.sqlRestriction(dateSql("this_.pertanggal")))
					.add(PostingJurnalHelper.restriksiPosting("postingHistory", Boolean.valueOf(posted)));
			return PostingJurnalHelper.hitung(criteria);
		} catch (Exception e) {
			rollbackIfNeeded(session);
			Common.tampilErrorJikaAdmin(e);
			return 0;
		}
	}

	private int countPengajuanTransfer(Session session, boolean posted) {
		try {
			Criteria criteria = session.createCriteria(DaftarPengajuanTransfer.class).setProjection(Projections.rowCount())
					.createAlias("disposisiSop", "disposisiSop", Criteria.LEFT_JOIN)
					.createAlias("prosesTransfer", "prosesTransfer")
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"), Restrictions.eq("disposisiSop.aktif", true)))
					.add(Restrictions.isNotNull("prosesTransfer.realisasikanOleh"))
					.add(Restrictions.isNotNull("prosesTransfer.disetujuiOleh"))
					.add(Restrictions.ne("nominal", 0.0)).add(Restrictions.isNotNull("nominal"))
					.add(Restrictions.between("prosesTransfer.tanggalRealisasikan", snapMulai(), snapSampai()))
					.add(PostingJurnalHelper.restriksiPosting("postingHistory", Boolean.valueOf(posted)));
			return PostingJurnalHelper.hitung(criteria);
		} catch (Exception e) {
			rollbackIfNeeded(session);
			Common.tampilErrorJikaAdmin(e);
			return 0;
		}
	}

	private int countTransitori(Session session, boolean posted) {
		try {
			Criteria criteria = session.createCriteria(Transitori.class).setProjection(Projections.rowCount())
					.createAlias("prosesTransitori", "prosesTransitori")
					.createAlias("daftarPengajuanTransfer", "daftarPengajuanTransfer", Criteria.LEFT_JOIN)
					.add(Restrictions.isNotNull("prosesTransitori.disetujuiOleh"))
					.add(Restrictions.ne("daftarPengajuanTransfer.nominal", 0.0))
					.add(Restrictions.isNotNull("daftarPengajuanTransfer.nominal"))
					.add(Restrictions.between("prosesTransitori.tanggalPersetujuan", snapMulai(), snapSampai()))
					.add(Restrictions.eq("transfer", true))
					.add(PostingJurnalHelper.restriksiPosting("postingHistory", Boolean.valueOf(posted)));
			return PostingJurnalHelper.hitung(criteria);
		} catch (Exception e) {
			rollbackIfNeeded(session);
			Common.tampilErrorJikaAdmin(e);
			return 0;
		}
	}

	private int countGrupTransaksiClosing(Session session, boolean sudahClosing) {
		try {
			Criteria criteria = session.createCriteria(GrupTransaksi.class).setProjection(Projections.rowCount())
					.add(Restrictions.sqlRestriction(dateSql("this_.tanggal_transaksi")));
			criteria.add(sudahClosing ? Restrictions.isNotNull("closing") : Restrictions.isNull("closing"));
			return PostingJurnalHelper.hitung(criteria);
		} catch (Exception e) {
			rollbackIfNeeded(session);
			Common.tampilErrorJikaAdmin(e);
			return 0;
		}
	}

	// =====================================================================
	// URL & UTIL (delegasi ke PostingJurnalHelper)
	// =====================================================================

	private String dateSql(String column) {
		return PostingJurnalHelper.dateSql(column, snapMulai(), snapSampai());
	}

	private String urlPosting(String baseUrl, boolean posted) {
		return PostingJurnalHelper.urlPosting(baseUrl, posted, snapMulai(), snapSampai());
	}

	private String urlClosing(String entitas, String ref, String jenis) {
		return PostingJurnalHelper.urlClosing(entitas, ref, jenis, snapMulai(), snapSampai());
	}

	private String urlGrupTransaksiTanggal(String parameter) {
		return PostingJurnalHelper.urlGrupTransaksi(parameter, snapMulai(), snapSampai());
	}

	/**
	 * Dipanggil dari thread pool saat compute PARALEL: TIDAK merender ke ZK (tidak thread-safe),
	 * melainkan menampung {@link DraftRow} ke {@link #draftCollector}. Urutan tampilan dijaga lewat
	 * {@link #orderTL} (per-jenis) + sub-urutan {@link #subTL} (untuk jenis yang menambah &gt;1 baris).
	 * Render sesungguhnya dilakukan di event-thread pada {@code onSearchDefaultTanpaProgress}.
	 */
	private void appendDraftRow(Rows rows, String nama, int draft, int posting, int closing, String draftUrl,
			String postingUrl, String closingUrl, String keterangan) {
		Integer o = orderTL.get();
		Integer sb = subTL.get();
		int order = o == null ? Integer.MAX_VALUE : o.intValue();
		int sub = sb == null ? 0 : sb.intValue();
		subTL.set(Integer.valueOf(sub + 1));
		draftCollector.add(new DraftRow(order, sub, nama, draft, posting, closing, draftUrl, postingUrl, closingUrl,
				keterangan));
	}

	// =====================================================================
	// SATU-KLIK POSTING / BATALKAN POSTING (per jenis jurnal) dari dasbor.
	// Tombol menyimpan "namaJenis" sebagai atribut; dispatcher menyalurkan ke
	// method STATIC modul terkait. Modul yang belum dikonversi diberi info.
	// =====================================================================
	private final org.zkoss.zk.ui.event.EventListener postingDispatcher = new org.zkoss.zk.ui.event.EventListener() {
		@Override
		public void onEvent(org.zkoss.zk.ui.event.Event event) throws Exception {
			final String nama = String.valueOf(event.getTarget().getAttribute("namaJenis"));
			ais.ui.util.MyMessageboxConfig.show("Posting SEMUA draft jurnal \"" + nama + "\" pada periode terpilih?",
					"Konfirmasi Posting", ais.ui.util.MyMessageboxConfig.OK | ais.ui.util.MyMessageboxConfig.CANCEL,
					ais.ui.util.MyMessageboxConfig.QUESTION, new org.zkoss.zk.ui.event.EventListener() {
						@Override
						public void onEvent(org.zkoss.zk.ui.event.Event ev) throws Exception {
							if (Integer.parseInt(ev.getData().toString()) == ais.ui.util.MyMessageboxConfig.OK) {
								jalankanPostingJenis(nama);
							}
						}
					});
		}
	};

	private final org.zkoss.zk.ui.event.EventListener batalkanDispatcher = new org.zkoss.zk.ui.event.EventListener() {
		@Override
		public void onEvent(org.zkoss.zk.ui.event.Event event) throws Exception {
			final String nama = String.valueOf(event.getTarget().getAttribute("namaJenis"));
			ais.ui.util.MyMessageboxConfig.show(
					"Batalkan posting jurnal \"" + nama + "\"? Jurnal yang SUDAH closing tidak akan dibatalkan.",
					"Konfirmasi Batalkan Posting",
					ais.ui.util.MyMessageboxConfig.OK | ais.ui.util.MyMessageboxConfig.CANCEL,
					ais.ui.util.MyMessageboxConfig.QUESTION, new org.zkoss.zk.ui.event.EventListener() {
						@Override
						public void onEvent(org.zkoss.zk.ui.event.Event ev) throws Exception {
							if (Integer.parseInt(ev.getData().toString()) == ais.ui.util.MyMessageboxConfig.OK) {
								jalankanBatalkanJenis(nama);
							}
						}
					});
		}
	};

	private void jalankanPostingJenis(String nama) {
		try {
			if ("Kas Kecil".equals(nama)) {
				int n = ais.action.master.akunting.PostingKasKecilAction.postingSemua(snapMulai(),
						snapSampai(), Common.getCurrentUser(), new java.util.Date());
				selesaiAksiJenis(n + " transaksi \"" + nama + "\" berhasil diposting.");
			} else {
				infoBelumTersedia(nama);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void jalankanBatalkanJenis(String nama) {
		try {
			if ("Kas Kecil".equals(nama)) {
				int n = ais.action.master.akunting.PostingKasKecilAction.batalkanPostingSemua(snapMulai(),
						snapSampai());
				selesaiAksiJenis(n + " transaksi \"" + nama + "\" dibatalkan postingnya (jurnal closing tetap utuh).");
			} else {
				infoBelumTersedia(nama);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void selesaiAksiJenis(String pesan) throws Exception {
		ais.ui.util.MyMessageboxConfig.show(pesan, "Selesai", ais.ui.util.MyMessageboxConfig.OK,
				ais.ui.util.MyMessageboxConfig.INFORMATION);
		loadDataDenganProgressPosting(null);
	}

	private void infoBelumTersedia(String nama) throws Exception {
		ais.ui.util.MyMessageboxConfig.show("Posting/Batalkan satu-klik untuk \"" + nama
				+ "\" sedang disiapkan (rilis bertahap). Sementara ini klik angka Draft/Terposting untuk membuka halaman posting modulnya.",
				"Sedang disiapkan", ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.INFORMATION);
	}

	private void rollbackIfNeeded(Session session) {
		try {
			if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/akunting/DrafJurnalAction.java:1090");
		}
	}

	private void closeOpenedSession(Session session) {
		if (session != null) {
			try { session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/akunting/DrafJurnalAction.java:1096");}
			try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/akunting/DrafJurnalAction.java:1097");}
			try { if (session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/akunting/DrafJurnalAction.java:1098");}
		}
	}

	// =====================================================================
	// RINGKASAN & PROGRESS
	// =====================================================================

	@SuppressWarnings("rawtypes")
	private void updateSummaryDashboard(Rows rows) {
		if (summaryHtml == null) {
			return;
		}
		try {
			List<DraftJurnalDashboardUtil.Stat> stats = new ArrayList<DraftJurnalDashboardUtil.Stat>();
			List children = rows == null ? null : rows.getChildren();
			if (children != null) {
				for (int i = 0; i < children.size(); i++) {
					Object object = children.get(i);
					if (!(object instanceof Row)) {
						continue;
					}
					Row row = (Row) object;
					String nama = extractRowLabel(row, 0);
					if (nama == null || nama.trim().length() == 0) {
						continue;
					}
					int draft = extractRowNumber(row, 1);
					int posted = extractRowNumber(row, 2);
					int closing = extractRowNumber(row, 3);
					stats.add(new DraftJurnalDashboardUtil.Stat(nama, draft, posted, closing));
				}
			}
			summaryHtml.setContent(DraftJurnalDashboardUtil.buildDraftSummary(stats,
					Common.dateFormat.get().format(snapMulai()) + " s.d. "
							+ Common.dateFormat.get().format(snapSampai())));
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private String extractRowLabel(Row row, int index) {
		try {
			Object child = row.getChildren().get(index);
			if (child instanceof Label) {
				return ((Label) child).getValue();
			}
			if (child instanceof A) {
				return ((A) child).getLabel();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/akunting/DrafJurnalAction.java:1148");
		}
		return "";
	}

	private int extractRowNumber(Row row, int index) {
		try {
			String value = extractRowLabel(row, index);
			if (value == null) {
				return 0;
			}
			value = value.replace(".", "").replace(",", "").replace(" ", "").trim();
			if (value.length() == 0) {
				return 0;
			}
			return Integer.parseInt(value);
		} catch (Exception e) {
			return 0;
		}
	}

	private void showProcessInfo(String message) {
		try {
			if (processInfo != null) {
				processInfo.setValue(message == null ? "" : message);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/akunting/DrafJurnalAction.java:1174");
		}
	}

	public void onSearchDefault(Event event) {
		loadDataDenganProgressPosting(event);
	}

	private boolean postingJurnalLoadingAktif = false;
	private boolean postingJurnalReloadTertunda = false;

	private void loadDataDenganProgressPosting(final org.zkoss.zk.ui.event.Event event) {
		if (postingJurnalLoadingAktif) {
			postingJurnalReloadTertunda = true;
			ais.ui.util.PostingJurnalLoadingUtil.show("Memuat Ulang Data Posting Jurnal",
					"Permintaan reload baru diterima. Data akan dimuat ulang setelah proses yang berjalan selesai.", 12);
			return;
		}
		postingJurnalLoadingAktif = true;
		postingJurnalReloadTertunda = false;
		ais.ui.util.PostingJurnalLoadingUtil.show("Memuat Data Posting Jurnal",
				"Menyiapkan filter dan tabel data jurnal.", 7);
		Common.createDefaultTimer(new org.zkoss.zk.ui.event.EventListener() {
			@Override
			public void onEvent(org.zkoss.zk.ui.event.Event timerEvent) throws Exception {
				try {
					ais.ui.util.PostingJurnalLoadingUtil.update("Mengambil Data Posting Jurnal",
							"Mencari data sesuai tanggal, status posting, dan filter halaman.", 48);
					onSearchDefaultTanpaProgress(event);
					ais.ui.util.PostingJurnalLoadingUtil.update("Merapikan Tampilan",
							"Menyusun tabel, paging, status posting, dan preview jurnal.", 92);
				} finally {
					boolean reloadLagi = postingJurnalReloadTertunda;
					postingJurnalReloadTertunda = false;
					postingJurnalLoadingAktif = false;
					if (reloadLagi) {
						ais.ui.util.PostingJurnalLoadingUtil.update("Memuat Ulang Data Posting Jurnal",
								"Filter atau halaman berubah saat data sedang diproses. Data akan dimuat ulang sekarang.", 96);
						Common.createDefaultTimer(new org.zkoss.zk.ui.event.EventListener() {
							@Override
							public void onEvent(org.zkoss.zk.ui.event.Event ulangEvent) throws Exception {
								loadDataDenganProgressPosting(event);
							}
						});
					} else {
						ais.ui.util.PostingJurnalLoadingUtil.complete("Data Posting Jurnal Siap",
								"Tabel sudah selesai dimuat dan siap digunakan.", 100);
					}
				}
			}
		});
	}

}
