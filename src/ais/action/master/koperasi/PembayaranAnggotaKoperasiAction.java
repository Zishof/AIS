package ais.action.master.koperasi;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.koperasi.helper.AmbilDataAnggotaKoperasiBanbox;
import ais.action.report.Report;
import ais.action.servlet.Wa;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.IndonesianNumberToWords;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.VirtualAccountBank;
import ais.database.model.bni.BniRequest;
import ais.database.model.bri.BriRequest;
import ais.database.model.bsi.BsiRequest;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.CaraPembayaranKoperasi;
import ais.database.model.koperasi.Koperasi;
import ais.database.model.koperasi.PembayaranAnggotaKoperasi;
import ais.database.model.koperasi.PembayaranAnggotaKoperasiDetail;
import ais.database.model.koperasi.ProdukKoperasi;
import ais.database.model.koperasi.TransaksiKoperasi;
import ais.database.model.koperasi.TransaksiKoperasiDetail;
import ais.delivery.email.sender.MailSender;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyFormRow;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * <h2>PembayaranAnggotaKoperasiAction — Riwayat &amp; Bukti Pelunasan Angsuran Koperasi</h2>
 *
 * <p>
 * Composer ZK ini menampilkan <b>daftar pembayaran/pelunasan angsuran anggota koperasi</b> yang
 * telah diterima, lengkap dengan siapa anggotanya, cara membayarnya, nominal, tanggal, dan
 * keterangannya. Dari halaman ini pengurus dapat <b>mencetak struk/bukti pembayaran</b> (PDF) untuk
 * tiap transaksi, sekaligus mengirim pemberitahuan otomatis (email dan/atau WhatsApp) ke anggota,
 * serta menghapus data bila memang perlu dan diizinkan oleh hak akses. Halaman ini bersifat
 * riwayat/monitor: pembentukan pembayaran itu sendiri dilakukan dari alur penagihan angsuran
 * ({@code TransaksiKoperasiDetail}) melalui {@link PembayaranAnggotaKoperasi#saveDetail}, sehingga di
 * sini fokusnya adalah menelusuri, mencetak, dan memberi bukti atas pembayaran yang sudah terjadi.
 * </p>
 *
 * <h3>Sumber data &amp; keterhubungan</h3>
 * <p>
 * Setiap baris adalah satu {@link PembayaranAnggotaKoperasi}. Sebuah pembayaran dapat melunasi satu
 * atau beberapa cicilan sekaligus; rinciannya tersimpan pada {@link PembayaranAnggotaKoperasiDetail}
 * yang masing-masing menunjuk ke sebuah {@link TransaksiKoperasiDetail} (baris jadwal angsuran) dan,
 * melalui itu, ke akad {@link TransaksiKoperasi} beserta {@link ProdukKoperasi}-nya. Ketika struk
 * dicetak, {@link #dataPembayaran} menyusun rincian tiap cicilan yang dibayar (angsuran ke berapa,
 * jatuh tempo, nominal, jenis biaya) menjadi parameter laporan Jasper.
 * </p>
 *
 * <h3>Ketahanan terhadap data tidak lengkap (anti-NullPointerException)</h3>
 * <p>
 * Data koperasi di lapangan kadang tidak lengkap: sebuah pembayaran bisa saja kehilangan relasi ke
 * anggota, produk, atau koperasi (mis. akibat migrasi atau penghapusan data induk). Karena itu
 * seluruh pembacaan relasi berantai pada composer ini dibungkus pemeriksaan null berlapis: baris
 * grid tetap tampil dengan tanda "-" alih-alih menggagalkan seluruh halaman, dan penyusunan struk
 * tetap berjalan dengan nilai default yang aman. Perbaikan ini menutup beberapa titik yang
 * sebelumnya berpotensi melempar {@code NullPointerException} saat data tidak lengkap
 * (mis. {@code getAnggotaKoperasi().getNama()} atau
 * {@code getProdukKoperasi().getTipeProdukKoperasi().getId()}).
 * </p>
 *
 * <h3>Ringkasan visual (panel atas)</h3>
 * <p>
 * Di atas tabel ditambahkan panel ringkasan singkat yang memberi gambaran cepat: berapa total uang
 * pelunasan yang sudah diterima, berapa banyak transaksinya, rata-ratanya, dan grafik tren
 * penerimaan per bulan (12 bulan terakhir). Seluruh grafik digambar dengan {@link DashboardUiKit}
 * (HTML/CSS/SVG murni — <b>tanpa JFreeChart</b>), sehingga rapi dan responsif di layar HP maupun
 * desktop. Panel ini bersifat tambahan dan tidak mengubah cara kerja tabel maupun pencetakan struk.
 * </p>
 *
 * <h3>Manajemen sesi &amp; efisiensi</h3>
 * <p>
 * Seluruh operasi baca/tulis memakai {@link HibernateUtil#currentSession()} yang <b>ditutup otomatis
 * oleh kerangka kerja</b> di akhir request — karena itu tidak ada penutupan sesi manual di sini
 * (menutupnya justru keliru). Query ringkasan memakai agregasi di sisi basis data (SUM/COUNT dan
 * GROUP BY per bulan) sehingga hemat memori: aplikasi tidak perlu memuat seluruh baris pembayaran ke
 * memori hanya untuk menjumlahkan. Daftar pada tabel dibatasi per halaman melalui paging bawaan.
 * Pemuatan relasi pada struk dibaca sekali per baris rincian. Struktur ini menjaga jejak memori tetap
 * kecil meski jumlah pembayaran sudah banyak.
 * </p>
 *
 * <h3>Kompatibilitas</h3>
 * <p>
 * Kelas ini dijaga kompatibel dengan Java 1.7 (tanpa lambda maupun operator diamond yang tidak
 * didukung) dan blok {@code try/catch} gaya lama (tanpa multi-catch/try-with-resources), agar tetap
 * dapat dikompilasi pada lingkungan yang ada. Seluruh fungsi lama — pencetakan struk, pengiriman
 * email/WhatsApp, penyusunan data laporan, pencarian, dan penghapusan — dipertahankan apa adanya dan
 * hanya dirapikan serta dibuat lebih tahan terhadap data tak lengkap.
 * </p>
 *
 * @author AIS — Sub-modul Koperasi Simpan Pinjam
 * @see PembayaranAnggotaKoperasi
 * @see PembayaranAnggotaKoperasiDetail
 * @see DashboardUiKit
 */
public class PembayaranAnggotaKoperasiAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault {

	private static final long serialVersionUID = -5779730267402400328L;

	private Paging paging;
	private MyGrid grid;
	private Textbox searchnama;
	/** Kontainer opsional untuk panel ringkasan (bila ada di ZUL). */
	private Div ringkasanHost;

	private boolean delete = false;
	private MyToolbarbuttonConfig add;
	private MyToolbarbuttonConfig pelunasan;

	// Komponen dialog "Pelunasan Angsuran" (auto-wired addWindow + kontrol yang dibangun dinamis).
	private MyWindow addWindow;
	private AmbilDataAnggotaKoperasiBanbox pelunasanAnggota;
	private Combobox pelunasanCara;
	private MyGrid pelunasanGrid;
	private Label pelunasanTotalLabel;
	private boolean bolehTambah = false;

	// ════════════════════════════════════════════════════════════════════════════════════════
	// Siklus hidup ZK
	// ════════════════════════════════════════════════════════════════════════════════════════

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Inisialisasi halaman: menyembunyikan tombol tambah (halaman ini bersifat riwayat), menyiapkan
	 * hak hapus, membangun panel ringkasan, memuat data awal, memasang paging, serta menautkan tombol
	 * cetak/ekspor data.
	 */
	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();

		if (add != null) {
			add.setVisible(false);
			add.setTooltiptext("Tambah");
		}

		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		bolehTambah = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		if (pelunasan != null) {
			pelunasan.setVisible(bolehTambah);
		}

		buildRingkasan();
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		String[] contents = new String[] { "id", "anggotaKoperasi.kode", "anggotaKoperasi.nama", "nominal",
				"caraPembayaranKoperasi.nama", "tanggal", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(PembayaranAnggotaKoperasi.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);
	}

	// ════════════════════════════════════════════════════════════════════════════════════════
	// Panel ringkasan (KPI + tren) — reuse DashboardUiKit
	// ════════════════════════════════════════════════════════════════════════════════════════

	/**
	 * Bangun panel ringkasan di atas tabel: total uang pelunasan yang diterima, jumlah transaksi,
	 * rata-rata per transaksi, dan tren penerimaan per bulan (12 bulan terakhir). Ringkasan dihitung
	 * lewat agregasi di basis data agar hemat memori. Bila kontainer {@code ringkasanHost} tidak ada
	 * pada ZUL, metode ini tidak melakukan apa-apa (kompatibel dengan ZUL lama).
	 */
	private void buildRingkasan() {
		if (ringkasanHost == null) {
			return;
		}
		ringkasanHost.getChildren().clear();
		try {
			Session session = HibernateUtil.currentSession();

			double total = 0.0;
			long jumlah = 0L;
			try {
				Object[] agg = (Object[]) session
						.createQuery("select coalesce(sum(p.nominal),0), count(p.id) from PembayaranAnggotaKoperasi p")
						.uniqueResult();
				if (agg != null) {
					total = agg[0] == null ? 0.0 : ((Number) agg[0]).doubleValue();
					jumlah = agg[1] == null ? 0L : ((Number) agg[1]).longValue();
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
			double rata = jumlah > 0 ? total / jumlah : 0.0;

			List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
			kartu.add(new DashboardUiKit.Stat("Total Pelunasan Diterima", "Rp " + DashboardUiKit.money(total),
					"seluruh pembayaran angsuran", DashboardUiKit.GOOD));
			kartu.add(new DashboardUiKit.Stat("Jumlah Transaksi", DashboardUiKit.money(jumlah), "kali pembayaran",
					DashboardUiKit.PRIMARY));
			kartu.add(new DashboardUiKit.Stat("Rata-rata per Transaksi", "Rp " + DashboardUiKit.money(rata),
					"nilai rata-rata", DashboardUiKit.ACCENT));

			StringBuilder sb = new StringBuilder();
			sb.append(DashboardUiKit.descChip(
					"Ringkasan uang pelunasan angsuran yang sudah diterima koperasi. Cetak struk lewat ikon printer "
							+ "pada tiap baris."));
			sb.append(DashboardUiKit.cards(kartu));
			sb.append(DashboardUiKit.sparkline("Tren Penerimaan Pelunasan (12 Bulan)",
					"Naik-turunnya uang pelunasan yang masuk tiap bulan.", trenPerBulan(session),
					DashboardUiKit.ACCENT, "Belum ada pembayaran pada periode ini."));
			ringkasanHost.appendChild(DashboardUiKit.html(sb.toString()));
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Ambil total pembayaran per bulan untuk 12 bulan terakhir (urut lama→baru) sebagai bahan
	 * sparkline. Memakai agregasi SQL agar tidak memuat seluruh baris ke memori. Bila query gagal,
	 * mengembalikan daftar kosong sehingga sparkline menampilkan pesan "belum ada data".
	 */
	@SuppressWarnings("unchecked")
	private List<Double> trenPerBulan(Session session) {
		java.util.LinkedHashMap<String, Double> bucket = new java.util.LinkedHashMap<String, Double>();
		Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
		int baseTahun = cal.get(Calendar.YEAR);
		int baseBulan = cal.get(Calendar.MONTH); // 0-based
		String[] urut = new String[12];
		for (int k = 11; k >= 0; k--) {
			int m = baseBulan - k, y = baseTahun;
			while (m < 0) {
				m += 12;
				y--;
			}
			String key = y + "-" + (m + 1 < 10 ? "0" : "") + (m + 1);
			urut[11 - k] = key;
			bucket.put(key, Double.valueOf(0.0));
		}
		try {
			List<Object[]> hasil = session.createSQLQuery(
					"SELECT TO_CHAR(tanggal,'YYYY-MM'), COALESCE(SUM(nominal),0) "
							+ "FROM koperasi.pembayaran_anggota_koperasi "
							+ "WHERE tanggal >= (CURRENT_DATE - INTERVAL '11 months') GROUP BY 1")
					.list();
			for (Object[] r : hasil) {
				if (r == null || r[0] == null) {
					continue;
				}
				String key = String.valueOf(r[0]);
				if (bucket.containsKey(key)) {
					bucket.put(key, Double.valueOf(r[1] == null ? 0.0 : ((Number) r[1]).doubleValue()));
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		List<Double> nilai = new ArrayList<Double>();
		for (String key : urut) {
			nilai.add(bucket.get(key));
		}
		return nilai;
	}

	// ════════════════════════════════════════════════════════════════════════════════════════
	// Renderer baris tabel
	// ════════════════════════════════════════════════════════════════════════════════════════

	/**
	 * Renderer satu baris pembayaran: identitas anggota (dengan tombol revisi), cara bayar, nominal,
	 * tanggal, keterangan, serta tombol cetak struk dan hapus. Seluruh pembacaan relasi anggota
	 * di-guard agar baris tetap tampil meski data anggota tidak lengkap.
	 */
	class PembayaranAnggotaKoperasiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PembayaranAnggotaKoperasi pembayaranAnggotaKoperasi = (PembayaranAnggotaKoperasi) arg1;

			AnggotaKoperasi ak = pembayaranAnggotaKoperasi.getAnggotaKoperasi();
			String namaAnggota = ak == null || ak.getNama() == null ? "-" : ak.getNama();
			String kodeAnggota = ak == null || ak.getKode() == null ? "" : ak.getKode();

			Vbox a;
			(a = RevisiHelper.createNewRevisi(PembayaranAnggotaKoperasi.class, pembayaranAnggotaKoperasi, namaAnggota))
					.setParent(arg0);
			new Label(kodeAnggota).setParent(a);

			new Label(pembayaranAnggotaKoperasi.getCaraPembayaranKoperasi() == null ? ""
					: pembayaranAnggotaKoperasi.getCaraPembayaranKoperasi().getNama()).setParent(arg0);

			new Label(Common.numberFormat.get().format(pembayaranAnggotaKoperasi.getNominal())).setParent(arg0);
			new Label(Common.dateFormat4.get().format(pembayaranAnggotaKoperasi.getTanggal())).setParent(arg0);
			new Label(pembayaranAnggotaKoperasi.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					PembayaranAnggotaKoperasiAction.cetakStruk(pembayaranAnggotaKoperasi);
				}
			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(pembayaranAnggotaKoperasi);
											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}
									}
								}
							});
				}
			});
			button.setParent(toolbar);
		}
	}

	// ════════════════════════════════════════════════════════════════════════════════════════
	// Pencarian
	// ════════════════════════════════════════════════════════════════════════════════════════

	/**
	 * Susun kriteria pencarian pembayaran. Bila kotak cari kosong, seluruh data ditampilkan; bila
	 * diisi, dicocokkan pada nama atau kode anggota (mengandung). Diurutkan dari yang terbaru.
	 */
	@Override
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PembayaranAnggotaKoperasi.class).createAlias("anggotaKoperasi",
				"anggotaKoperasi");

		if (order) {
			criteria.addOrder(Order.desc("id"));
		}
		String cari = searchnama == null ? "" : searchnama.getValue().trim();
		criteria.add(cari.isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.or(Restrictions.ilike("anggotaKoperasi.nama", cari, MatchMode.ANYWHERE),
						Restrictions.ilike("anggotaKoperasi.kode", cari, MatchMode.ANYWHERE)));
		return criteria;
	}

	/** Muat ulang daftar pembayaran sesuai kriteria &amp; halaman aktif, lalu render ke tabel. */
	@Override
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PembayaranAnggotaKoperasi> pembayaranAnggotaKoperasi = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pembayaranAnggotaKoperasi);
		grid.setRowRenderer(new PembayaranAnggotaKoperasiRenderer());
		grid.setModelCheckMobile(strset);
	}

	// ════════════════════════════════════════════════════════════════════════════════════════
	// Cetak struk + notifikasi
	// ════════════════════════════════════════════════════════════════════════════════════════

	/**
	 * Cetak struk/bukti pembayaran (PDF) untuk sebuah pembayaran, lalu — best-effort — kirim
	 * lampiran struk ke anggota via email/WhatsApp. Menyusun parameter laporan (kode transaksi,
	 * waktu cetak, properti pembayaran, dan rincian tiap cicilan) sebelum memanggil Jasper.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void cetakStruk(PembayaranAnggotaKoperasi pembayaranAnggotaKoperasi) throws Exception {
		Map parameters = new HashMap();
		parameters.put("id_pembayaran", pembayaranAnggotaKoperasi == null || pembayaranAnggotaKoperasi.getId() == null ? -1L : pembayaranAnggotaKoperasi.getId());
		parameters.put("id_bri", -1L);
		parameters.put("id_bni", -1L);
		parameters.put("id_bsi", -1L);
		parameters.put("id_va", -1L);

		Long idPembayaran = pembayaranAnggotaKoperasi == null ? null : pembayaranAnggotaKoperasi.getId();
		String kode_transaksi = "0000000000000000000000" + idPembayaran;
		kode_transaksi = StringUtils.substring(kode_transaksi, kode_transaksi.length() - 8);
		parameters.put("kode_transaksi", kode_transaksi);

		parameters.put("waktu_cetak",
				pembayaranAnggotaKoperasi == null ? Common.dateFormat1.get().format(WaktuUtil.getDate())
						: Common.dateFormat1.get().format(pembayaranAnggotaKoperasi.getTanggal()));

		Common.insertProperty(PembayaranAnggotaKoperasi.class, pembayaranAnggotaKoperasi, parameters, "", 2);

		dataPembayaran(pembayaranAnggotaKoperasi, null, null, null, null, parameters);

		Report.generatePDFReport(Report.PDF, parameters, "koperasi/struk_pembayaran", ais.ui.util.WaktuUtil.getDate(),
				Common.locale);

		try {
			File file = Report.generateFileReport(Report.PDF, parameters, "koperasi/struk_pembayaran",
					ais.ui.util.WaktuUtil.getDate(), Common.locale);
			PembayaranAnggotaKoperasiAction.kirim(pembayaranAnggotaKoperasi, file);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Siapkan tautan bukti pembayaran (ter-enkripsi) lalu teruskan ke {@link #kirim(PembayaranAnggotaKoperasi,
	 * Double, String, File)}.
	 */
	public static void kirim(PembayaranAnggotaKoperasi pembayaranAnggotaKoperasi, File file) throws Exception {
		String url = pembayaranAnggotaKoperasi == null || pembayaranAnggotaKoperasi.getId() == null ? ""
				: Common.getRequestHostWithProtocol() + "/StrukKoperasi?id=" + URLEncoder.encode(
						("EE" + Common.desEncrypter.get().encrypt(pembayaranAnggotaKoperasi.getId() + "")), "UTF-8");

		kirim(pembayaranAnggotaKoperasi,
				pembayaranAnggotaKoperasi == null ? null : pembayaranAnggotaKoperasi.getNominal(), url, file);
	}

	/**
	 * Kirim pemberitahuan pembayaran berhasil ke anggota. Bersifat <b>best-effort</b>: jika anggota
	 * tidak ada, proses dihentikan diam-diam (pembayaran sudah tersimpan; notifikasi opsional). Nama
	 * koperasi memakai nilai cadangan bila belum di-set. Email dikirim langsung; WhatsApp dikirim
	 * lewat timer tertunda bila fitur diaktifkan lewat konfigurasi.
	 */
	public static void kirim(final PembayaranAnggotaKoperasi pembayaranAnggotaKoperasi, final Double nilai,
			final String url, final File file) throws Exception {

		// GUARD NPE: tanpa anggota koperasi tidak ada penerima notifikasi -> hentikan.
		// Pembayaran sudah tersimpan; notifikasi (email/WA) bersifat best-effort.
		final AnggotaKoperasi anggotaKoperasi = pembayaranAnggotaKoperasi == null ? null
				: pembayaranAnggotaKoperasi.getAnggotaKoperasi();
		if (anggotaKoperasi == null) {
			return;
		}

		// Nama koperasi BISA null (FK koperasi belum di-set pada anggota) -> pakai fallback
		// agar tidak NullPointerException saat menyusun isi email/WA.
		String namaKoperasiTmp = "Koperasi";
		try {
			if (anggotaKoperasi.getKoperasi() != null && anggotaKoperasi.getKoperasi().getNama() != null
					&& anggotaKoperasi.getKoperasi().getNama().trim().length() > 0) {
				namaKoperasiTmp = anggotaKoperasi.getKoperasi().getNama();
			}
		} catch (Exception eKoperasi) { ais.common.ErrorAuditUtil.record(eKoperasi, "auto-audit(empty-catch) src/ais/action/master/koperasi/PembayaranAnggotaKoperasiAction.java:515");
			// biarkan memakai fallback "Koperasi"
		}
		final String namaKoperasi = namaKoperasiTmp;

		JSONArray userIds = new JSONArray();
		if (anggotaKoperasi.getSiswa() != null && anggotaKoperasi.getSiswa().getNomorIndukNasional() != null
				&& !anggotaKoperasi.getSiswa().getNomorIndukNasional().isEmpty()) {
			userIds.put(anggotaKoperasi.getSiswa().getNomorIndukNasional());
		} else if (anggotaKoperasi.getMahasiswa() != null && anggotaKoperasi.getMahasiswa().getNim() != null) {
			userIds.put(anggotaKoperasi.getMahasiswa().getNim());
		}

		final String nama = anggotaKoperasi.getNama() == null ? "" : anggotaKoperasi.getNama();
		String info = "Anggota Koperasi: " + nama;
		String subject = "Informasi Pembayaran Berhasil => " + info;
		String waktu = salamWaktu();

		String body = "Selamat " + waktu + ",<br><br>Yth. Bapak/Ibu Anggota Koperasi,<br>" + "<br><br>"
				+ "Kami dari pengurus <b>" + namaKoperasi + "</b> menginformasikan bahwa pembayaran atas nama " + nama
				+ (nilai != null ? " senilai Rp " + Common.numberFormat.get().format(nilai) + " " : "")
				+ " telah berhasil dilakukan. Silakan periksa catatan transaksi pada file terlampir atau pada aplikasi atau portal kami"
				+ (url == null || url.trim().isEmpty() ? "." : ", atau juga bisa buka di link " + url) + "\r\n"
				+ "<br><br>" + "\nTerima kasih atas partisipasi Bapak/Ibu.";

		String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
		String emailUser = anggotaKoperasi.getEmail();
		JSONArray attachmentsData = null;

		MailSender.sendMailLampiran(userIds, subject, body, sender, emailUser, anggotaKoperasi, attachmentsData, false,
				file);

		if (Common.bolehKonfigurasi("aktifkan_kirim_notif_pembayaran_ke_wa")) {
			Common.createDefaultTimer(new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					String dawal = Common.getKonfigurasi("pesan_tambahan_notif_awal",
							"*Pesan ini dibuat secara otomatis oleh sistem sebagai notifikasi/pemberitahuan kepada Anda*\n\n")
							.getNilai();
					Set<String> forms = new HashSet<String>();
					if (anggotaKoperasi.getHp() != null && !anggotaKoperasi.getHp().trim().isEmpty()) {
						forms.add(anggotaKoperasi.getHp());
					}
					if (anggotaKoperasi.getTelp() != null && !anggotaKoperasi.getTelp().trim().isEmpty()) {
						forms.add(anggotaKoperasi.getTelp());
					}

					for (String from : forms) {
						if (from != null && !from.trim().isEmpty() && !from.trim().equals("00000000000000000000")
								&& !from.trim().equals("000000000")) {
							String body = "Selamat " + salamWaktu() + ",\n\nYth. Bapak/Ibu Anggota Koperasi,\n\n"
									+ "Kami dari pihak pengurus *" + namaKoperasi
									+ "* menginformasikan bahwa pembayaran atas nama *" + nama + "*"
									+ (nilai != null ? " senilai Rp " + Common.numberFormat.get().format(nilai) + " " : "")
									+ " telah berhasil dilakukan. Silakan periksa catatan transaksi pada file terlampir atau pada aplikasi atau portal kami"
									+ (url == null || url.trim().isEmpty() ? "." : ", atau juga bisa buka di link " + url)
									+ "\r\n" + "\n\n" + "*Terima kasih atas partisipasi Bapak/Ibu.*";

							String urlD = url;
							if (file != null) {
								urlD = Common.getRequestHostWithProtocolSimple()
										+ file.getAbsolutePath().split("webapps")[1];
							}

							Wa.kirimWaViaUltramsg(from, dawal + body, "Bukti_Pembayaran.pdf", urlD,
									Wa.buatProfile(null, PerguruanTinggiUtil.getPerguruanTinggi()));
						}
					}
				}
			}, "", false, 2000);
		}
	}

	/** Kata sapaan sesuai jam saat ini (Pagi/Siang/Sore/Malam). */
	private static String salamWaktu() {
		int jam = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		if (jam >= 10 && jam < 15) {
			return "Siang";
		} else if (jam >= 15 && jam < 18) {
			return "Sore";
		} else if (jam >= 18 && jam <= 24) {
			return "Malam";
		}
		return "Pagi";
	}

	// ════════════════════════════════════════════════════════════════════════════════════════
	// Penyusunan data laporan (rincian tiap cicilan yang dibayar)
	// ════════════════════════════════════════════════════════════════════════════════════════

	/**
	 * Susun parameter rincian laporan struk: mengambil seluruh {@link PembayaranAnggotaKoperasiDetail}
	 * milik sebuah pembayaran (atau berdasarkan permintaan bank/VA), memetakan tiap cicilan menjadi
	 * satu baris rincian (angsuran ke-berapa, jatuh tempo, jenis biaya, nominal, dsb.), menjumlahkan
	 * totalnya, dan menaruh terbilang serta daftar rincian ke {@code parameters}. Seluruh pembacaan
	 * relasi berantai di-guard agar tetap aman meski data induk tak lengkap.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void dataPembayaran(PembayaranAnggotaKoperasi pembayaranAnggotaKoperasi, BniRequest bniRequest,
			BriRequest briRequest, BsiRequest bsiRequest, VirtualAccountBank virtualAccountBank, Map parameters)
			throws Exception {

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(PembayaranAnggotaKoperasiDetail.class);

		if (bniRequest != null && bniRequest.getId() != null) {
			criteria.createAlias("pembayaranAnggotaKoperasi", "pembayaranAnggotaKoperasi")
					.add(Restrictions.eq("pembayaranAnggotaKoperasi.bniRequest", bniRequest));
		} else if (briRequest != null && briRequest.getId() != null) {
			criteria.createAlias("pembayaranAnggotaKoperasi", "pembayaranAnggotaKoperasi")
					.add(Restrictions.eq("pembayaranAnggotaKoperasi.briRequest", briRequest));
		} else if (bsiRequest != null && bsiRequest.getId() != null) {
			criteria.createAlias("pembayaranAnggotaKoperasi", "pembayaranAnggotaKoperasi")
					.add(Restrictions.eq("pembayaranAnggotaKoperasi.bsiRequest", bsiRequest));
		} else if (virtualAccountBank != null && virtualAccountBank.getId() != null) {
			criteria.createAlias("pembayaranAnggotaKoperasi", "pembayaranAnggotaKoperasi")
					.add(Restrictions.eq("pembayaranAnggotaKoperasi.virtualAccountBank", virtualAccountBank));
		} else {
			criteria.add(Restrictions.eq("pembayaranAnggotaKoperasi", pembayaranAnggotaKoperasi));
		}

		List<PembayaranAnggotaKoperasiDetail> pembayaranAnggotaKoperasiDetails = criteria
				.createAlias("transaksiKoperasiDetail", "transaksiKoperasiDetail")
				.createAlias("transaksiKoperasiDetail.transaksiKoperasi", "transaksiKoperasi")
				.addOrder(Order.asc("transaksiKoperasi.id")).addOrder(Order.asc("transaksiKoperasiDetail.ke")).list();
		List<Map> maps = new ArrayList<Map>();

		Double total = 0.0;
		Tbmuser tbmuserValidator = null;
		for (PembayaranAnggotaKoperasiDetail pembayaranAnggotaKoperasiDetail : pembayaranAnggotaKoperasiDetails) {

			// Resolusi relasi berantai sekali di depan, dengan guard null berlapis.
			TransaksiKoperasiDetail tkDetail = pembayaranAnggotaKoperasiDetail.getTransaksiKoperasiDetail();
			TransaksiKoperasi tk = tkDetail == null ? null : tkDetail.getTransaksiKoperasi();
			ProdukKoperasi produk = tk == null ? null : tk.getProdukKoperasi();
			PembayaranAnggotaKoperasi pay = pembayaranAnggotaKoperasiDetail.getPembayaranAnggotaKoperasi();
			AnggotaKoperasi anggota = pay == null ? null : pay.getAnggotaKoperasi();
			Koperasi koperasi = anggota == null ? null : anggota.getKoperasi();

			Double nominal = pembayaranAnggotaKoperasiDetail.getNominalManual() != null
					? pembayaranAnggotaKoperasiDetail.getNominalManual()
					: pembayaranAnggotaKoperasiDetail.getNominal();
			if (nominal == null) {
				nominal = 0.0;
			}
			total += nominal;

			Map map = new HashMap();
			maps.add(map);
			Common.insertProperty(PembayaranAnggotaKoperasiDetail.class, pembayaranAnggotaKoperasiDetail, map, "", 1);
			map.put("jenis_biaya_id", produk == null ? null : produk.getId());
			map.put("jenis_biaya", produk == null || produk.getNama() == null ? "" : produk.getNama());

			map.put("tanggal", pay == null ? null : pay.getTanggalBayar());
			map.put("tanggal_jatuh_tempo", tkDetail == null ? null : tkDetail.getTanggal());
			map.put("id_transaksi", pay == null ? null : pay.getId());

			boolean isPinjaman = produk != null && produk.getTipeProdukKoperasi() != null
					&& produk.getTipeProdukKoperasi().getId() != null && ConstantValues.PINJAMAN != null
					&& produk.getTipeProdukKoperasi().getId().equals(ConstantValues.PINJAMAN.getId());
			Integer ke = tkDetail == null ? null : tkDetail.getKe();
			String item = (isPinjaman ? "Angsuran ke-" : "Pembayaran ke-") + ke;
			String tglDetail = tkDetail == null || tkDetail.getTanggal() == null ? ""
					: Common.dateFormat11.get().format(tkDetail.getTanggal());
			map.put("item_biaya", (item + " " + tglDetail).trim());

			map.put("nominal", nominal);
			map.put("denda", 0.0);
			map.put("nilai", pembayaranAnggotaKoperasiDetail.getNominal());
			map.put("diskon", 0.0);

			map.put("cara", pay == null || pay.getCaraPembayaranKoperasi() == null ? ""
					: pay.getCaraPembayaranKoperasi().getNama());

			map.put("tambahan_deposit", 0.0);
			map.put("validator", pay == null ? "" : pay.getValidator());
			tbmuserValidator = pay == null ? null : pay.getValidatorUser();

			map.put("bayarke", ke);
			map.put("dibayarsebayak", tk == null ? null : tk.getJumlahAngsur());

			map.put("nomor_induk", anggota == null ? "" : anggota.getKodeIdentitas());
			map.put("nama_siswa", anggota == null || anggota.getNama() == null ? "" : anggota.getNama());
			map.put("koperasi_id", koperasi == null ? null : koperasi.getId());

			map.put("kelas", "");
			map.put("asrama", "");
		}

		try {
			if (tbmuserValidator != null && tbmuserValidator.getUserId() != null) {
				Common.insertProperty(Tbmuser.class, tbmuserValidator, parameters, "validatorData", 2);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/PembayaranAnggotaKoperasiAction.java:709");
			// validator opsional pada struk — abaikan bila gagal.
		}

		if (pembayaranAnggotaKoperasi != null) {
			Double nominalHeader = pembayaranAnggotaKoperasi.getNominal();
			if (nominalHeader == null || total.intValue() != nominalHeader.intValue()) {
				pembayaranAnggotaKoperasi.setNominal(total);
				pembayaranAnggotaKoperasi.setTambahanDeposit(total);
				Common.refreshUpdate(session, pembayaranAnggotaKoperasi);
			}
		}
		parameters.put("terbilang", IndonesianNumberToWords.convert(total.longValue()));
		parameters.put("maps", maps);

		pembayaranAnggotaKoperasiDetails.clear();
		pembayaranAnggotaKoperasiDetails = null;
	}

	// ════════════════════════════════════════════════════════════════════════════════════════
	// Pelunasan Angsuran (membuat pembayaran baru dari tagihan yang belum dibayar)
	// ════════════════════════════════════════════════════════════════════════════════════════

	/**
	 * Buka dialog pelunasan angsuran: pilih anggota, muat daftar cicilan yang belum dibayar, centang
	 * yang akan dilunasi, pilih cara pembayaran, lalu simpan. Dialog dibangun dinamis di dalam
	 * {@code addWindow} agar tetap ringan (tidak membebani halaman utama sampai benar-benar dipakai).
	 */
	public void onPelunasan(Event event) throws Exception {
		if (addWindow == null) {
			MyMessageboxConfig.show("Mohon maaf, formulir pelunasan tidak tersedia pada halaman ini. Langkah yang dapat dilakukan: (1) buka menu Pembayaran Anggota Koperasi; (2) pilih transaksi yang akan dilunasi; (3) gunakan tombol Lunasi yang tersedia.", "Informasi",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}
		addWindow.setTitle("Pelunasan Angsuran Anggota");
		Common.clear(addWindow);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		// NORTH: pilih anggota + cara bayar + tombol muat tagihan.
		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);
		MyGrid form = new MyGrid();
		form.setWidth("100%");
		form.setParent(north);
		Columns fcols = new Columns();
		fcols.setParent(form);
		MyColumnConfig fc = new MyColumnConfig();
		fc.setWidth("110px");
		fc.setParent(fcols);
		fc = new MyColumnConfig();
		fc.setParent(fcols);
		fc = new MyColumnConfig();
		fc.setWidth("110px");
		fc.setParent(fcols);
		fc = new MyColumnConfig();
		fc.setParent(fcols);
		Rows frows = new Rows();
		frows.setParent(form);

		MyFormRow fr = new MyFormRow();
		fr.setParent(frows);
		fr.appendChild(new MyLabelConfig("Anggota *"));
		pelunasanAnggota = new AmbilDataAnggotaKoperasiBanbox();
		pelunasanAnggota.setWidth("95%");
		// Saat anggota dipilih dari bandbox, otomatis muat tagihannya.
		pelunasanAnggota.setEventListener(new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				muatTagihan();
			}
		});
		fr.appendChild(pelunasanAnggota);
		fr.appendChild(new MyLabelConfig("Cara Bayar *"));
		pelunasanCara = new Combobox();
		pelunasanCara.setWidth("95%");
		pelunasanCara.setReadonly(true);
		Common.insertCombo(pelunasanCara, "nama", CaraPembayaranKoperasi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		fr.appendChild(pelunasanCara);

		fr = new MyFormRow();
		fr.setParent(frows);
		fr.appendChild(new MyLabelConfig(""));
		MyToolbarbuttonConfig muat = new MyToolbarbuttonConfig("Muat Tagihan", "/img/refresh.gif");
		muat.setTooltiptext("Tampilkan cicilan yang belum dibayar milik anggota terpilih");
		muat.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				muatTagihan();
			}
		});
		fr.appendChild(muat);
		fr.appendChild(new MyLabelConfig(""));
		fr.appendChild(new MyLabelConfig(""));

		// CENTER: grid tagihan (checkbox pilih).
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		pelunasanGrid = new MyGrid();
		pelunasanGrid.setWidth("100%");
		pelunasanGrid.setHeight("100%");
		pelunasanGrid.setParent(center);
		Columns gcols = new Columns();
		gcols.setParent(pelunasanGrid);
		String[] judul = new String[] { "Pilih", "Produk / Akad", "Angsuran", "Jatuh Tempo", "Pokok", "Bunga",
				"Jumlah" };
		String[] lebar = new String[] { "50px", null, "80px", "110px", null, null, null };
		for (int c = 0; c < judul.length; c++) {
			MyColumnConfig col = new MyColumnConfig();
			col.setLabel(judul[c]);
			if (lebar[c] != null) {
				col.setWidth(lebar[c]);
			}
			col.setParent(gcols);
		}
		Rows grows = new Rows();
		grows.setParent(pelunasanGrid);

		// SOUTH: total terpilih + tombol aksi.
		South south = new South();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);
		Hbox sbar = new Hbox();
		sbar.setParent(south);
		pelunasanTotalLabel = new Label(ais.common.Common.getBahasaConfig("Total dipilih: Rp 0"));
		pelunasanTotalLabel.setParent(sbar);
		MyToolbarbuttonConfig lunasi = new MyToolbarbuttonConfig("Lunasi & Simpan", "/img/save.gif");
		lunasi.setTooltiptext("Simpan pembayaran atas cicilan yang dicentang");
		lunasi.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				simpanPelunasan();
			}
		});
		lunasi.setParent(sbar);
		MyToolbarbuttonConfig batal = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		batal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				addWindow.setVisible(false);
			}
		});
		batal.setParent(sbar);

		borderlayout.setParent(addWindow);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Muat cicilan (angsuran) yang <b>belum dibayar</b> milik anggota terpilih ke dalam grid, tiap
	 * baris dilengkapi kotak centang. Baris menyimpan objek {@link TransaksiKoperasiDetail} pada
	 * atribut untuk dipakai saat penyimpanan &amp; perhitungan total.
	 */
	@SuppressWarnings("unchecked")
	private void muatTagihan() {
		if (pelunasanGrid == null) {
			return;
		}
		Rows rows = pelunasanGrid.getRows();
		if (rows == null) {
			rows = new Rows();
			rows.setParent(pelunasanGrid);
		}
		rows.getChildren().clear();

		AnggotaKoperasi anggota = pelunasanAnggota == null ? null
				: (AnggotaKoperasi) pelunasanAnggota.getAttribute("anggotaKoperasi");
		if (anggota == null || anggota.getId() == null) {
			updateTotalLabel();
			return;
		}

		try {
			Session session = HibernateUtil.currentSession();
			List<TransaksiKoperasiDetail> tagihan = session.createQuery(
					"select distinct d from TransaksiKoperasiDetail d left join fetch d.transaksiKoperasi t "
							+ "left join fetch t.produkKoperasi p where d.pembayaranAnggotaKoperasiDetail is null "
							+ "and t.anggotaKoperasi.id = :aid order by t.id, d.ke")
					.setParameter("aid", anggota.getId()).list();
			for (TransaksiKoperasiDetail d : tagihan) {
				try {
					TransaksiKoperasi t = d.getTransaksiKoperasi();
					ProdukKoperasi p = t == null ? null : t.getProdukKoperasi();
					double jumlah = d.getPokok() + d.getMargin();

					Row row = new Row();
					final MyCheckboxConfig cb = new MyCheckboxConfig("");
					cb.addEventListener("onCheck", new EventListener() {
						@Override
						public void onEvent(Event e) throws Exception {
							updateTotalLabel();
						}
					});
					row.setAttribute("pilih", cb);
					row.setAttribute("detail", d);
					cb.setParent(row);
					new Label(p == null || p.getNama() == null ? "-" : p.getNama()).setParent(row);
					new Label("Ke-" + (d.getKe() == null ? 0 : d.getKe())).setParent(row);
					new Label(d.getTanggal() == null ? "" : Common.dateFormat4.get().format(d.getTanggal()))
							.setParent(row);
					new Label(Common.numberFormat.get().format(d.getPokok())).setParent(row);
					new Label(Common.numberFormat.get().format(d.getMargin())).setParent(row);
					new Label(Common.numberFormat.get().format(jumlah)).setParent(row);
					row.setParent(rows);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
			if (rows.getChildren().isEmpty()) {
				Row row = new Row();
				new Label(ais.common.Common.getBahasaConfig("Tidak ada cicilan yang belum dibayar untuk anggota ini.")).setParent(row);
				row.setParent(rows);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		updateTotalLabel();
	}

	/** Jumlahkan nilai (pokok + bunga) seluruh cicilan yang dicentang pada grid tagihan. */
	@SuppressWarnings("unchecked")
	private double hitungTotalTerpilih() {
		double total = 0.0;
		if (pelunasanGrid == null || pelunasanGrid.getRows() == null) {
			return 0.0;
		}
		for (Object o : pelunasanGrid.getRows().getChildren()) {
			try {
				Row row = (Row) o;
				MyCheckboxConfig cb = (MyCheckboxConfig) row.getAttribute("pilih");
				TransaksiKoperasiDetail d = (TransaksiKoperasiDetail) row.getAttribute("detail");
				if (cb != null && cb.isChecked() && d != null) {
					total += d.getPokok() + d.getMargin();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/PembayaranAnggotaKoperasiAction.java:947");
			}
		}
		return total;
	}

	/** Perbarui label total pada bilah bawah dialog pelunasan. */
	private void updateTotalLabel() {
		if (pelunasanTotalLabel != null) {
			pelunasanTotalLabel.setValue("Total dipilih: Rp " + Common.numberFormat.get().format(hitungTotalTerpilih()));
		}
	}

	/**
	 * Simpan pelunasan: buat satu {@link PembayaranAnggotaKoperasi} lalu untuk tiap cicilan yang
	 * dicentang buat {@link PembayaranAnggotaKoperasiDetail} dan <b>tandai cicilannya lunas</b>
	 * (mengisi {@code transaksiKoperasiDetail.pembayaranAnggotaKoperasiDetail}) — persis pola engine
	 * pembayaran yang sudah ada, agar cicilan tidak muncul lagi sebagai tagihan.
	 *
	 * <p>
	 * Penyimpanan memakai <b>sesi khusus</b> {@link HibernateUtil#openSession()} dengan transaksi
	 * eksplisit, dan sesi tersebut <b>ditutup pada blok {@code finally}</b>. Ini sesuai kaidah: sesi
	 * yang dibuka sendiri (openSession) wajib ditutup, sementara sesi bersama
	 * ({@code currentSession}) tidak boleh ditutup. Entitas dibaca ulang di sesi baru agar tidak ada
	 * masalah objek terpisah (detached).
	 * </p>
	 */
	@SuppressWarnings("unchecked")
	private void simpanPelunasan() throws Exception {
		AnggotaKoperasi anggotaPilih = pelunasanAnggota == null ? null
				: (AnggotaKoperasi) pelunasanAnggota.getAttribute("anggotaKoperasi");
		CaraPembayaranKoperasi caraPilih = pelunasanCara == null || pelunasanCara.getSelectedItem() == null ? null
				: (CaraPembayaranKoperasi) pelunasanCara.getSelectedItem().getValue();

		if (anggotaPilih == null || anggotaPilih.getId() == null) {
			MyMessageboxConfig.show("Mohon maaf, anggota koperasi belum dipilih. Langkah yang dapat dilakukan: (1) pilih anggota koperasi dari daftar pencarian; (2) pastikan anggota sudah terdaftar di sistem; (3) ulangi proses pelunasan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}
		if (caraPilih == null || caraPilih.getId() == null) {
			MyMessageboxConfig.show("Mohon maaf, cara pembayaran belum dipilih. Langkah yang dapat dilakukan: (1) pilih cara pembayaran dari daftar yang tersedia; (2) pastikan cara pembayaran aktif; (3) ulangi proses pelunasan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}

		List<Long> idDetail = new ArrayList<Long>();
		if (pelunasanGrid != null && pelunasanGrid.getRows() != null) {
			for (Object o : pelunasanGrid.getRows().getChildren()) {
				try {
					Row row = (Row) o;
					MyCheckboxConfig cb = (MyCheckboxConfig) row.getAttribute("pilih");
					TransaksiKoperasiDetail d = (TransaksiKoperasiDetail) row.getAttribute("detail");
					if (cb != null && cb.isChecked() && d != null && d.getId() != null) {
						idDetail.add(d.getId());
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/PembayaranAnggotaKoperasiAction.java:1002");
				}
			}
		}
		if (idDetail.isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, belum ada cicilan yang dipilih. Langkah yang dapat dilakukan: (1) centang minimal satu cicilan pada daftar yang ditampilkan; (2) pastikan cicilan yang dipilih belum lunas; (3) ulangi proses pelunasan.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}

		String validator = "";
		try {
			Tbmuser u = Common.getCurrentUser();
			validator = u == null || u.getUserNama() == null ? "" : u.getUserNama();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/PembayaranAnggotaKoperasiAction.java:1016");
		}

		Long idPembayaran = null;
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.openSession();
			tx = session.beginTransaction();

			AnggotaKoperasi anggota = (AnggotaKoperasi) session.get(AnggotaKoperasi.class, anggotaPilih.getId());
			CaraPembayaranKoperasi cara = (CaraPembayaranKoperasi) session.get(CaraPembayaranKoperasi.class,
					caraPilih.getId());

			double total = 0.0;
			PembayaranAnggotaKoperasi pembayaran = new PembayaranAnggotaKoperasi();
			pembayaran.setAnggotaKoperasi(anggota);
			pembayaran.setCaraPembayaranKoperasi(cara);
			pembayaran.setTanggal(WaktuUtil.getDate());
			pembayaran.setTanggalBayar(WaktuUtil.getDate());
			pembayaran.setKeterangan("Pelunasan angsuran");
			pembayaran.setValidator(validator);
			pembayaran.setNominal(Double.valueOf(0.0));
			session.save(pembayaran);

			for (Long id : idDetail) {
				TransaksiKoperasiDetail d = (TransaksiKoperasiDetail) session.get(TransaksiKoperasiDetail.class, id);
				if (d == null || d.getId() == null) {
					continue;
				}
				double nominal = d.getPokok() + d.getMargin();
				total += nominal;

				PembayaranAnggotaKoperasiDetail pd = new PembayaranAnggotaKoperasiDetail();
				pd.setNominal(Double.valueOf(nominal));
				pd.setNominalManual(Double.valueOf(nominal));
				pd.setPembayaranAnggotaKoperasi(pembayaran);
				pd.setTransaksiKoperasiDetail(d);
				session.save(pd);

				d.setPembayaranAnggotaKoperasiDetail(pd);
				session.update(d);
			}

			pembayaran.setNominal(Double.valueOf(total));
			session.update(pembayaran);

			tx.commit();
			tx = null;
			idPembayaran = pembayaran.getId();
		} catch (Exception e) {
			if (tx != null && tx.isActive()) {
				try {
					tx.rollback();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/koperasi/PembayaranAnggotaKoperasiAction.java:1070");
				}
			}
			Common.tampilErrorJikaAdmin(e);
			MyMessageboxConfig.show("Gagal menyimpan pelunasan: " + e.getMessage(), "Kesalahan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		} finally {
			if (session != null) {
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/PembayaranAnggotaKoperasiAction.java:1081");
				}
			}
		}

		if (addWindow != null) {
			addWindow.setVisible(false);
		}
		buildRingkasan();
		onSearchDefault(null);

		final Long idFinal = idPembayaran;
		MyMessageboxConfig.show("Pelunasan berhasil disimpan. Cetak struk sekarang?", "Informasi",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						if (Integer.parseInt(e.getData().toString()) == MyMessageboxConfig.OK && idFinal != null) {
							try {
								PembayaranAnggotaKoperasi fresh = (PembayaranAnggotaKoperasi) HibernateUtil
										.currentSession().get(PembayaranAnggotaKoperasi.class, idFinal);
								if (fresh != null) {
									cetakStruk(fresh);
								}
							} catch (Exception ex) {
								Common.tampilErrorJikaAdmin(ex);
							}
						}
					}
				});
	}
}
