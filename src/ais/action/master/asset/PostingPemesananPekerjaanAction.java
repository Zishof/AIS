package ais.action.master.asset;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.util.CommonAkunting;
import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.asset.JenisPajakBarang;
import ais.database.model.asset.Lokasi;
import ais.database.model.asset.PemilikAsset;
import ais.database.model.asset.SaldoAwalMasterAsset;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * <h3>PostingPemesananPekerjaanAction — Aksi Posting Jurnal Tagihan Pekerjaan Pengadaan Aset</h3>
 *
 * <p><b>Untuk apa:</b><br>
 * Kelas ini merupakan kontroler ZKoss yang mengelola proses posting jurnal akuntansi untuk
 * tagihan pekerjaan yang berasal dari pengadaan aset. "Tagihan pekerjaan" dalam konteks ini
 * adalah termin pembayaran (progress billing) yang terkandung dalam data JSON ({@code jsonTermin})
 * pada entitas {@code SaldoAwalMasterAsset}. Setiap termin yang sudah disetujui dan memiliki
 * akun akuntansi yang valid akan diposting ke jurnal umum sebagai entri debet-kredit pada akun
 * utang pekerjaan ({@code akunUtangPekerjaan}) dan akun utang uang muka ({@code akunUtangDp}).
 * Posting menghasilkan entri {@code GrupTransaksi} di modul akuntansi yang dapat digunakan
 * untuk laporan keuangan, buku besar, dan rekonsiliasi pembayaran.</p>
 *
 * <p><b>Cara kerja:</b><br>
 * Alur kerja utama kelas ini adalah sebagai berikut:
 * <ol>
 *   <li>Saat halaman dimuat, {@code doAfterCompose} menginisialisasi komponen filter, membaca
 *       parameter URL (sudah_posting, mulai, sampai) untuk mode tampilan tertentu, dan memuat
 *       data awal ke grid menggunakan {@code loadDataDenganProgressPosting}.</li>
 *   <li>Data yang ditampilkan di grid adalah daftar {@code SaldoAwalMasterAsset} yang memiliki
 *       termin (jsonTermin), sudah disetujui, dan bukan merupakan termin DP. Setiap baris
 *       menampilkan informasi tagihan, jurnal debet-kredit yang akan dibuat, status posting,
 *       dan nomor bukti jurnal jika sudah diposting.</li>
 *   <li>Pengguna dapat memposting satu tagihan per baris menggunakan tombol "Posting Data",
 *       atau memposting semua sekaligus melalui tombol "Posting Semua" yang membuka form
 *       konfirmasi dengan input tanggal dan keterangan.</li>
 *   <li>Proses posting semua dilakukan di thread terpisah untuk menghindari timeout UI,
 *       dengan indikator progress melalui label yang diperbarui secara real-time.</li>
 *   <li>Pembatalan posting dapat dilakukan per baris atau secara massal melalui
 *       "Batalkan Posting Semua", yang menghapus entri GrupTransaksi terkait dari database.</li>
 *   <li>Pemuatan data menggunakan mekanisme anti-duplikasi loading ({@code postingJurnalLoadingAktif}
 *       dan {@code postingJurnalReloadTertunda}) untuk menghindari request query yang tumpang tindih.</li>
 * </ol>
 * </p>
 *
 * <p><b>Threading:</b><br>
 * Proses posting semua ({@code onPostingSemua}) menjalankan loop posting di thread baru
 * ({@code new Thread(new Runnable(){...}).start()}) untuk menghindari pemblokiran event thread
 * ZKoss selama proses yang panjang. Akses database di thread ini menggunakan
 * {@code HibernateUtil.currentNativeSession()} yang memberikan sesi Hibernate terpisah.
 * Pembaruan label progress dari thread latar ke UI ZKoss dilakukan secara langsung karena
 * ZKoss menggunakan model push berbasis Comet/WebSocket yang thread-safe untuk label.
 * Satu-satunya potensi race condition adalah pada pembaruan label progress, namun ini
 * hanya berdampak visual dan tidak merusak integritas data.
 * Untuk operasi posting tunggal per baris, menggunakan {@code Common.createDefaultTimer}
 * (timer ZKoss) sehingga tetap di event thread.</p>
 *
 * <p><b>Pemeliharaan:</b><br>
 * Saat menambah jenis termin baru selain DP (merupakan_dp = false), pastikan field JSON
 * yang diperlukan tersedia. Penambahan akun akuntansi baru harus dikonfigurasi melalui
 * {@code JenisPemesananPengadaanAsset} dan tidak di-hardcode di sini. Filter SQL date()
 * pada {@code initCriteria} mengasumsikan format tanggal database PostgreSQL; pastikan
 * kompatibel jika database diganti. DDL kolom {@code saldo_awal_master_asset} tidak boleh
 * diubah tanpa mengupdate query SQL native di {@code onBatalkanPostingSemua} dan
 * event listener batalkan posting per baris.</p>
 *
 * @author AIS Development Team
 * @version 1.0
 * @see SaldoAwalMasterAsset
 * @see PostingHistory
 * @see GrupTransaksi
 * @see CommonAkunting
 */
public class PostingPemesananPekerjaanAction extends GenericAutowireComposer {

	/**
	 * Versi serialisasi kelas untuk keperluan serialisasi Java.
	 */
	private static final long serialVersionUID = -5779730217402400328L;

	/** Grid utama yang menampilkan daftar tagihan pekerjaan beserta status posting dan tombol aksi. */
	private MyGrid grid;

	/** Textbox pencarian berdasarkan kode atau keterangan termin tagihan. */
	private Textbox searchkode;

	/** Combobox filter berdasarkan pemilik aset (unit/satuan kerja pemilik aset). */
	private Combobox searchpemilikAsset;

	/** Combobox filter berdasarkan lokasi aset. */
	private Combobox searchlokasi;

	/** Banbox pencarian berdasarkan ruang/lokasi spesifik aset. */
	private AmbilDataRuangBanbox searchruang;

	/** Checkbox filter untuk menampilkan data yang belum diposting. */
	private MyCheckboxConfig searchtampil;

	/** Checkbox filter untuk menampilkan data yang sudah diposting. */
	private MyCheckboxConfig searchtelahtampil;

	/** Flag yang menunjukkan apakah pengguna memiliki hak akses UPDATE. */
	private boolean edit = false;

	/** Tombol pengiriman/posting data (visibilitasnya bergantung pada hak akses UPDATE). */
	private MyToolbarbuttonConfig sent;

	/**
	 * Flag yang menunjukkan apakah pengguna adalah admin atau memiliki hak APPROVE,
	 * yang memberikan akses ke tombol batalkan posting.
	 */
	public boolean adminLain;

	/** Datebox filter tanggal mulai (awal rentang tanggal persetujuan). */
	private MyDatebox tglMulai;

	/** Datebox filter tanggal sampai (akhir rentang tanggal persetujuan). */
	private MyDatebox tglSampai;

	/** Referensi pengguna yang sedang login, digunakan untuk mencatat siapa yang memposting. */
	private Tbmuser tbmuser;

	/** Panel North yang berisi filter pencarian; disembunyikan jika mode sudah_posting aktif. */
	private North filter;

	/**
	 * Baris tambahan yang muncul hanya saat mode {@code sudah_posting} aktif,
	 * menunjukkan informasi tambahan status posting.
	 */
	private Row rowPosting;

	/**
	 * Parameter dari URL yang menentukan mode tampilan:
	 * {@code true} untuk hanya menampilkan yang sudah diposting,
	 * {@code false} untuk hanya menampilkan yang belum diposting,
	 * {@code null} untuk mode normal (semua, difilter oleh checkbox).
	 */
	private Boolean sudah_posting = null;

	/**
	 * <p><b>Tujuan:</b><br>
	 * Metode hook ZKoss yang dipanggil sebelum komponen halaman di-compose, untuk melakukan
	 * pemeriksaan keamanan akses halaman sebelum UI dibangun.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Memanggil {@code Common.doCheckSecurity()} untuk memverifikasi sesi aktif dan valid,
	 * kemudian melanjutkan ke implementasi superclass untuk proses compose normal.</p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param page     Halaman ZKoss yang sedang di-compose.
	 * @param parent   Komponen induk dari root komponen halaman.
	 * @param compInfo Informasi metadata komponen ZKoss.
	 *
	 * <p><b>Return:</b></p>
	 * @return {@code ComponentInfo} dari superclass untuk melanjutkan proses compose.
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jangan hapus pemanggilan security check ini karena merupakan lapisan keamanan pertama
	 * sebelum halaman ditampilkan ke pengguna.</p>
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <p><b>Tujuan:</b><br>
	 * Metode inisialisasi utama yang dipanggil ZKoss setelah seluruh komponen UI di-wire
	 * ke field kontroler. Menyiapkan semua state awal halaman: validasi sesi, inisialisasi
	 * bahasa, pembacaan parameter URL, konfigurasi filter, pengisian combobox referensi,
	 * dan pemuatan data awal ke grid.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Memvalidasi sesi dan hak akses READ; jika gagal, logoff pengguna.</li>
	 *   <li>Inisialisasi bahasa tampilan melalui {@code Common.initLaguage()}.</li>
	 *   <li>Membaca parameter URL {@code sudah_posting} untuk menentukan mode tampilan;
	 *       jika aktif, menyembunyikan panel filter dan menampilkan baris posting.</li>
	 *   <li>Menginisialisasi rentang tanggal default: mulai = 6 bulan lalu, sampai = hari ini.</li>
	 *   <li>Membaca parameter URL {@code mulai} dan {@code sampai} untuk override tanggal filter
	 *       secara programatis (format: yyyyMMdd).</li>
	 *   <li>Memeriksa hak akses UPDATE dan mengatur visibilitas tombol sent.</li>
	 *   <li>Mengisi combobox searchpemilikAsset dan searchlokasi dari database.</li>
	 *   <li>Mengunci filter lokasi berdasarkan sesi pengguna jika ada atribut "Lokasi" di sesi.</li>
	 *   <li>Memuat data grid awal melalui {@code loadDataDenganProgressPosting}.</li>
	 *   <li>Menyiapkan filter lanjutan melalui {@code FilterLanjutHelper.setup}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param comp Komponen root halaman ZUL yang sudah ter-compose.
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Parsing tanggal dari parameter URL dibungkus try-catch; jika format tidak valid,
	 * error ditampilkan hanya kepada admin melalui {@code Common.tampilErrorJikaAdmin}.
	 * Kegagalan parsing tidak menghentikan inisialisasi, filter tanggal tetap menggunakan
	 * nilai default.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika menambah parameter URL baru, baca di sini menggunakan {@code execution.getParameter}.
	 * Format tanggal URL menggunakan {@code Common.dateFormat8} (yyyyMMdd), pastikan konsisten
	 * dengan pemanggil yang menghasilkan URL.</p>
	 *
	 * @throws Exception jika terjadi kesalahan saat compose atau inisialisasi komponen.
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		tbmuser = Common.getCurrentUser();
		if (execution.getParameter("sudah_posting") != null
				&& !execution.getParameter("sudah_posting").trim().isEmpty()) {
			sudah_posting = Boolean.parseBoolean(execution.getParameter("sudah_posting").trim());
		}

		if (sudah_posting != null && filter != null) {
			filter.setVisible(false);
			if (rowPosting != null) rowPosting.setVisible(true);
		}

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
		if (tglSampai != null) tglSampai.setValue(ais.ui.util.WaktuUtil.getDate());
		if (tglMulai != null) tglMulai.setValue(calendar.getTime());
		if (tglMulai != null) tglMulai.setReadonly(true);
		if (tglSampai != null) tglSampai.setReadonly(true);

		adminLain = Common.getApakahAdmin() || CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);

		if (execution.getParameter("mulai") != null) {
			try {
				if (tglMulai != null) tglMulai.setValue(Common.dateFormat8.get().parse(execution.getParameter("mulai")));
				if (tglMulai != null) tglMulai.setDisabled(true);
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

		if (execution.getParameter("sampai") != null) {
			try {
				if (tglSampai != null) tglSampai.setValue(Common.dateFormat8.get().parse(execution.getParameter("sampai")));
				if (tglSampai != null) tglSampai.setDisabled(true);
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		if (sent != null) { sent.setVisible(edit); }

		Common.insertComboDanSemua(searchpemilikAsset, new String[] { "nama", "id" }, "keterangan", PemilikAsset.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertComboDanSemua(searchlokasi, new String[] { "nama" }, "alamat", Lokasi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		if (session.getAttribute("Lokasi") != null) {
			Common.selectComboItem(searchlokasi, session.getAttribute("Lokasi"));
			searchlokasi.setDisabled(true);
			session.removeAttribute("Lokasi");
		}
		LokasiAction.kunciLokasi(searchlokasi);

		loadDataDenganProgressPosting(null);

	        FilterLanjutHelper.setup(comp);
}

	/**
	 * <p><b>Tujuan:</b><br>
	 * Event handler yang dipanggil ketika pengguna mengklik tombol "Batalkan Posting Semua".
	 * Menampilkan dialog konfirmasi, dan jika dikonfirmasi, membatalkan semua posting jurnal
	 * untuk tagihan pekerjaan yang saat ini tampil di grid sesuai filter aktif.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Menampilkan dialog konfirmasi kepada pengguna.</li>
	 *   <li>Jika dikonfirmasi (OK):
	 *     <ul>
	 *       <li>Mengambil semua {@code SaldoAwalMasterAsset} yang memiliki {@code postingHistory}
	 *           (sudah diposting) sesuai criteria aktif.</li>
	 *       <li>Untuk setiap data, mengosongkan field {@code postingHistory} dan menyimpan perubahan.</li>
	 *       <li>Menghapus semua entri {@code GrupTransaksi} terkait dengan SQL native, dengan
	 *           kondisi {@code saldo_awal_master_asset = id AND ref IS NOT NULL AND ref != 'DP_PEKERJAAN'
	 *           AND closing IS NULL} untuk menghindari penghapusan data DP atau data closing.</li>
	 *     </ul>
	 *   </li>
	 *   <li>Memuat ulang data grid melalui timer default setelah operasi selesai.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param event Event ZKoss yang memicu aksi (biasanya onClick dari tombol Batalkan Posting Semua).
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Tidak ada penanganan error eksplisit; exception akan merambat ke framework ZKoss.
	 * Jika penghapusan SQL gagal (misalnya karena constraint), transaksi Hibernate akan
	 * di-rollback secara otomatis.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Kondisi SQL {@code ref != 'DP_PEKERJAAN'} harus konsisten dengan nilai konstanta
	 * yang digunakan saat posting; periksa jika ada perubahan pada nilai ref yang digunakan
	 * saat posting data DP. Kondisi {@code closing IS NULL} melindungi entri jurnal closing
	 * dari penghapusan yang tidak disengaja.</p>
	 *
	 * @throws Exception jika terjadi kesalahan database saat pembatalan posting.
	 */
	public void onBatalkanPostingSemua(Event event) throws Exception {

		MyMessageboxConfig.show("Apakah yakin ingin membatalkan posting transaksi tagihan pekerjaan ?", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							List<SaldoAwalMasterAsset> saldoAwalMasterAssets = initCriteria(true)
									.add(Restrictions.isNotNull("postingHistory")).list();

							for (SaldoAwalMasterAsset saldoAwalMasterAsset : saldoAwalMasterAssets) {
								saldoAwalMasterAsset.setPostingHistory(null);
								Common.refreshSaveOrUpdate(saldoAwalMasterAsset);
								HibernateUtil.currentSession()
										.createSQLQuery(
												"delete from akunting.grup_transaksi where saldo_awal_master_asset="
														+ saldoAwalMasterAsset.getId()
														+ " and ref is not null and ref != 'DP_PEKERJAAN'" + " and closing is null")
										.executeUpdate();
							}
						}

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadDataDenganProgressPosting(null);
							}
						});
					}
				});

	}

	/**
	 * <p><b>Tujuan:</b><br>
	 * Event handler yang dipanggil ketika pengguna mengklik tombol "Posting Semua". Membuka
	 * jendela modal form untuk mengisi tanggal posting dan keterangan, lalu memproses posting
	 * semua tagihan pekerjaan yang belum diposting sesuai kriteria filter aktif secara batch
	 * di thread terpisah.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Membuat jendela modal ({@code MyWindow}) baru dan menambahkannya ke halaman.</li>
	 *   <li>Membangun form konfirmasi dengan field: tanggal posting (datebox), nama poster
	 *       (label read-only dari pengguna aktif), dan keterangan (textbox multiline).</li>
	 *   <li>Menambahkan tombol Batal dan Simpan. Tombol Simpan:
	 *     <ul>
	 *       <li>Memvalidasi tanggal harus diisi.</li>
	 *       <li>Menampilkan dialog konfirmasi kedua sebelum eksekusi posting.</li>
	 *       <li>Jika dikonfirmasi, menampilkan label progress dan menjalankan posting
	 *           di thread baru.</li>
	 *     </ul>
	 *   </li>
	 *   <li>Thread posting:
	 *     <ul>
	 *       <li>Membuat entitas {@code PostingHistory} baru dan menyimpannya.</li>
	 *       <li>Mengambil semua data sesuai criteria dan mengiterasi setiap tagihan.</li>
	 *       <li>Mem-parsing JSON termin untuk mendapatkan nilai key, nama, nomor, penagihan, pinalti, ppn, pajak.</li>
	 *       <li>Hanya memproses termin yang sudah disetujui ({@code setuju = true}) dan
	 *           belum memiliki entri GrupTransaksi dengan ref yang sama.</li>
	 *       <li>Menghitung nilai netto = |total (penagihan + ppn%) - pinalti|.</li>
	 *       <li>Jika ada pajak dengan akun, membuat jurnal dengan split kredit: pajak dan sisa.</li>
	 *       <li>Jika tidak ada pajak atau nilainya kecil, membuat jurnal tunggal debet-kredit.</li>
	 *       <li>Memperbarui label progress dengan persentase yang sudah diproses.</li>
	 *       <li>Menutup sesi Hibernate setelah semua data diproses.</li>
	 *     </ul>
	 *   </li>
	 *   <li>Setelah thread selesai, callback ke UI event thread untuk menampilkan
	 *       notifikasi sukses dan memuat ulang grid.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param event Event ZKoss yang memicu pembukaan form posting semua.
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception di dalam loop posting dibungkus try-catch bertingkat yang hanya
	 * mencatat error ke admin ({@code Common.tampilErrorJikaAdmin}) dan melanjutkan
	 * ke data berikutnya. Ini mencegah satu data bermasalah menghentikan seluruh batch.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Nilai threshold {@code nilai > 0.1} digunakan untuk menentukan arah jurnal (debet/kredit
	 * dibalik jika nilai negatif kecil). Ini mengasumsikan tidak ada transaksi yang valid
	 * dengan nilai antara -0.1 dan 0.1 selain nol efektif. Jika bisnis mengizinkan nilai
	 * yang sangat kecil, pertimbangkan untuk menurunkan threshold ini.</p>
	 *
	 * @throws Exception jika terjadi kesalahan saat membangun form modal.
	 */
	public void onPostingSemua(Event event) throws Exception {

		final MyWindow addWindow = new MyWindow();
		page.getFirstRoot().appendChild(addWindow);
		addWindow.setTitle("Posting Transaksi tagihan pekerjaan");
		addWindow.setWidth("800px");
		addWindow.setHeight("300px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid mygrid = new MyGrid();
		mygrid.setWidth("100%");
		mygrid.setParent(center);
		mygrid.setWidth("100%");
		mygrid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(mygrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("100px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");

		Rows rows = new Rows();
		rows.setParent(mygrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal / Waktu"));
		final MyDatebox tanggal;
		row.appendChild(tanggal = new MyDatebox(ais.ui.util.WaktuUtil.getDate()));
		tanggal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diposting oleh"));
		row.appendChild(new ais.ui.util.MyLabelConfig(
				Common.getCurrentUser().ambilPegawai() == null ? Common.getCurrentUser().getUserId()
						: Common.getCurrentUser().ambilPegawai().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		final MyTextbox keterangan;
		row.appendChild(keterangan = new MyTextbox());
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.detach();
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				final Date tgl = tanggal.getValue();
				if (tgl == null) {
					MyMessageboxConfig.show("Mohon maaf, Tanggal Posting Tagihan Pekerjaan belum diisi. Langkah yang dapat dilakukan: (1) Klik field Tanggal dan pilih tanggal dari kalender; (2) Pastikan tanggal berada dalam periode akuntansi yang aktif; (3) ulangi proses posting ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				MyMessageboxConfig.show("Apakah yakin ingin memposting semua transaksi tagihan pekerjaan ?",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									final Tbmuser tbmuser = Common.getCurrentUser();

									final Label label = Common.displayLoadBar(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											MyMessageboxConfig.show(
													"Posting transaksi Pembayaran DP berhasil dilakukan", "Informasi",
													MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
													new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															onSearchDefault(arg0);
														}
													});

											addWindow.detach();
										}
									});

									new Thread(new Runnable() {

										@Override
										public void run() {
											try {

											Session session = HibernateUtil.currentNativeSession();

											PostingHistory postingHistory = new PostingHistory(
													PostingHistory.JENIS_TAGIHAN_PEKERJAAN);
											postingHistory.setTanggal(tgl);
											postingHistory.setTbmuser(tbmuser);
											postingHistory.setKeterangan(keterangan.getValue().trim() + " \nTgl:"
													+ Common.dateFormat.get().format(tglMulai.getValue()) + " s.d "
													+ Common.dateFormat.get().format(tglSampai.getValue()));
											session.getTransaction().begin();
											session.save(postingHistory);
											session.getTransaction().commit();

											List<SaldoAwalMasterAsset> saldoAwalMasterAssets = initCriteria(true)
													.list();

											int rowIndex = 1;
											for (SaldoAwalMasterAsset saldoAwalMasterAsset : saldoAwalMasterAssets) {

												SatuanKerja satuanKerja = (SatuanKerja) saldoAwalMasterAsset
														.getSatuanKerja();

												if (saldoAwalMasterAsset != null) {

													try {

														try {

															JSONObject jsonObject = new JSONObject(
																	saldoAwalMasterAsset.getJsonTermin());

															if (jsonObject.isNull("key")) {
																continue;
															}

															Boolean setuju;
															if (!jsonObject.isNull("setuju")) {
																setuju = Boolean
																		.parseBoolean(jsonObject.get("setuju") + "");
															} else {
																setuju = false;
															}

															if (setuju) {

																final String key;

																if (!jsonObject.isNull("key")) {
																	key = jsonObject.get("key") + "";
																} else {
																	key = "";
																}

																Number bukti = (Number) session
																		.createCriteria(GrupTransaksi.class)
																		.add(Restrictions.eq("saldoAwalMasterAsset",
																				saldoAwalMasterAsset))
																		.add(Restrictions.eq("ref", key))
																		.setProjection(Projections.rowCount())
																		.uniqueResult();

																if (bukti == null || bukti.intValue() == 0) {

																	final String nama;

																	if (!jsonObject.isNull("nama")) {
																		nama = jsonObject.get("nama") + "";
																	} else {
																		nama = "";
																	}

																	final String nomor;

																	if (!jsonObject.isNull("nomor")) {
																		nomor = jsonObject.get("nomor") + "";
																	} else {
																		nomor = "";
																	}

																	final Double penagihan;
																	if (!jsonObject.isNull("penagihan")) {
																		penagihan = jsonObject.getDouble("penagihan");
																	} else {
																		penagihan = 0.0;
																	}

																	Double pinalti = 0.0;
																	if (!jsonObject.isNull("pinalti")) {
																		pinalti = jsonObject.getDouble("pinalti");
																	}
																	Double ppn = 0.0;
																	if (!jsonObject.isNull("ppn")) {
																		ppn = jsonObject.getDouble("ppn");
																	}
																	Double total = penagihan
																			+ ((ppn / 100.0) * penagihan);
																	final Double nilai = Math.abs(total - pinalti);

																	final JenisPajakBarang jenisPajakBarang;
																	if (!jsonObject.isNull("pajak")) {
																		jenisPajakBarang = (JenisPajakBarang) ConstantValues
																				.ambil(JenisPajakBarang.class.getName(),
																						Long.parseLong(
																								jsonObject.get("pajak")
																										+ ""));
																	} else {
																		jenisPajakBarang = null;
																	}

																	try {

																		Akun akunDebet = saldoAwalMasterAsset
																				.getPenerimaanPengadaanMasterAsset()
																				.getPemesananPengadaanMasterAsset()
																				.getJenisPemesananPengadaanAsset() == null
																						? null
																						: saldoAwalMasterAsset
																								.getPenerimaanPengadaanMasterAsset()
																								.getPemesananPengadaanMasterAsset()
																								.getJenisPemesananPengadaanAsset()
																								.getAkunUtangPekerjaan();

																		Akun akunKredit = saldoAwalMasterAsset
																				.getPenerimaanPengadaanMasterAsset()
																				.getPemesananPengadaanMasterAsset()
																				.getJenisPemesananPengadaanAsset() == null
																						? null
																						: saldoAwalMasterAsset
																								.getPenerimaanPengadaanMasterAsset()
																								.getPemesananPengadaanMasterAsset()
																								.getJenisPemesananPengadaanAsset()
																								.getAkunUtangDp();

																		Boolean apakahUangMasuk = true;
																		Akun akunDenda = null;
																		Akun akunPiutangDenda = null;
																		Double denda = 0.0;

																		String ket = "";
																		try {

																			ket = "Tagihan pekerjaan terhadap pemesanan \""
																					+ (saldoAwalMasterAsset.getKode()
																							+ "-" + nomor + "-" + nama
																							+ " " + saldoAwalMasterAsset
																									.getKeterangan());

																		} catch (Exception e) {
																			Common.tampilErrorJikaAdmin(e);
																		}

																		label.setValue(ket + " (" + Common.numberFormat.get()
																				.format(rowIndex * 100.0
																						/ saldoAwalMasterAssets.size())
																				+ " %)");

																		if (jenisPajakBarang != null
																				&& jenisPajakBarang.getAkun() != null) {

																			Double nilaiPajak = (penagihan
																					* (jenisPajakBarang.getPersen()
																							/ 100.0));

																			session.getTransaction().begin();
																			CommonAkunting.saveTransaksi(
																					new Akun[] { akunDebet },
																					new Akun[] { akunKredit,
																							jenisPajakBarang
																									.getAkun() },
																					akunDenda, akunPiutangDenda,
																					postingHistory, apakahUangMasuk,
																					ket,
																					saldoAwalMasterAsset
																							.getTanggalPersetujuan(),
																					new Double[] { nilai },
																					new Double[] { nilai - nilaiPajak,
																							nilaiPajak },
																					denda, saldoAwalMasterAsset,
																					satuanKerja, key, session);
																			session.getTransaction().commit();

																		} else {

																			if (akunDebet != null
																					&& akunKredit != null) {

																				try {

																					session.getTransaction().begin();
																					if (nilai > 0.1) {
																						CommonAkunting.saveTransaksi(
																								akunDebet, akunKredit,
																								akunDenda,
																								akunPiutangDenda,
																								postingHistory,
																								apakahUangMasuk, ket,
																								saldoAwalMasterAsset
																										.getTanggalPersetujuan(),
																								nilai, denda,
																								saldoAwalMasterAsset,
																								satuanKerja, key,
																								session);
																					} else {
																						CommonAkunting.saveTransaksi(
																								akunKredit, akunDebet,
																								akunDenda,
																								akunPiutangDenda,
																								postingHistory,
																								apakahUangMasuk, ket,
																								saldoAwalMasterAsset
																										.getTanggalPersetujuan(),
																								nilai, denda,
																								saldoAwalMasterAsset,
																								satuanKerja, key,
																								session);
																					}
																					session.getTransaction().commit();
																				} catch (Exception e) {
																					Common.tampilErrorJikaAdmin(e);
																				}
																			}

																			saldoAwalMasterAsset
																					.setPostingHistory(postingHistory);
																			session.getTransaction().begin();
																			session.update(saldoAwalMasterAsset);
																			session.getTransaction().commit();
																		}
																	} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PostingPemesananPekerjaanAction.java:811");
																		// Exception per item diabaikan agar batch tidak terhenti
																	}

																}
																rowIndex++;
															}
														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PostingPemesananPekerjaanAction.java:818");
															// Exception parsing JSON per baris diabaikan
														}

													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PostingPemesananPekerjaanAction.java:822");
														// Exception level item diabaikan agar iterasi berlanjut
													}
												}
											}

											label.setValue("");
											HibernateUtil.closeSession();
																					} finally {
												ais.database.hibernate.HibernateUtil.closeSession();
											}
										}
									}).start();

								}

							}
						});
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
		addWindow.onModal();
	}

	/**
	 * <h3>SaldoAwalMasterAssetRenderer — Renderer Baris Grid Tagihan Pekerjaan</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Inner class yang bertanggung jawab merender setiap baris data tagihan pekerjaan
	 * pada grid utama. Setiap baris menampilkan informasi termin tagihan yang di-parse
	 * dari JSON, termasuk kode aset, nomor dan nama termin, nama penyedia, jenis pemesanan,
	 * nilai tagihan (penagihan + PPN - pinalti), tanggal, jurnal debet-kredit yang akan
	 * dibuat, status posting, nomor bukti jurnal, serta tombol posting dan batalkan posting.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Menerima {@code Object[] objects} dimana {@code objects[0]} adalah {@code JSONObject}
	 * data termin dan {@code objects[1]} adalah entitas {@code SaldoAwalMasterAsset}.
	 * Me-parse JSON untuk mendapatkan semua field termin, menampilkan informasi secara
	 * terstruktur, menghitung nilai netto, dan menampilkan preview jurnal menggunakan
	 * {@code GrupTransaksi.tampilkanJurnal}. Tombol posting hanya terlihat jika belum
	 * diposting; tombol batalkan hanya terlihat jika sudah diposting dan pengguna
	 * adalah admin atau memiliki hak APPROVE.</p>
	 *
	 * <p><b>Threading:</b><br>
	 * Berjalan pada event thread ZKoss. Operasi database dilakukan sinkron.
	 * Timer ZKoss digunakan untuk operasi batalkan dan posting tunggal agar
	 * commit dapat diselesaikan sebelum UI di-refresh.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika format JSON termin berubah (menambah field baru), update parsing di metode
	 * {@code render} ini, di {@code onPostingSemua} (loop batch), dan di helper yang
	 * menghasilkan JSON tersebut secara konsisten.</p>
	 */
	class SaldoAwalMasterAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <p><b>Tujuan:</b><br>
		 * Merender satu baris data tagihan pekerjaan ke dalam komponen-komponen ZKoss
		 * yang ditampilkan pada grid. Dipanggil oleh engine ZKoss untuk setiap pasangan
		 * (JSONObject, SaldoAwalMasterAsset) dalam model data grid.</p>
		 *
		 * <p><b>Cara kerja:</b><br>
		 * <ol>
		 *   <li>Menerima array objek: index 0 = JSONObject termin, index 1 = entitas SaldoAwalMasterAsset.</li>
		 *   <li>Mem-parse field JSON: key, nama, nomor, penagihan, pinalti, ppn, tanggalD, pajak;
		 *       setiap field dicek null-nya dengan {@code jsonObject.isNull()} sebelum dibaca.</li>
		 *   <li>Menghitung nilai netto: total = penagihan + (ppn/100 * penagihan), lalu
		 *       nilai = |total - pinalti|.</li>
		 *   <li>Menampilkan kode aset, nomor dan nama termin menggunakan RevisiHelper untuk
		 *       mendukung tampilan revisi entitas.</li>
		 *   <li>Menampilkan link SOP jika disposisi SOP ada.</li>
		 *   <li>Menampilkan nama penyedia dan jenis pemesanan pengadaan.</li>
		 *   <li>Menampilkan nilai total dan pinalti (jika ada).</li>
		 *   <li>Menampilkan preview jurnal debet-kredit jika kedua akun valid, atau pesan
		 *       error informatif jika salah satu akun tidak dikonfigurasi.</li>
		 *   <li>Menampilkan status posting: "Belum diposting" atau detail PostingHistory
		 *       berikut nomor bukti GrupTransaksi.</li>
		 *   <li>Tombol "Batalkan Posting": visible hanya jika sudah diposting dan adminLain.</li>
		 *   <li>Tombol "Posting Data": visible hanya jika belum diposting dan pengguna valid.</li>
		 * </ol>
		 * </p>
		 *
		 * <p><b>Parameter:</b></p>
		 * @param arg0 Baris ({@code Row}) ZKoss tempat komponen akan ditambahkan.
		 * @param arg1 Array objek {@code Object[]}: [0] = JSONObject termin, [1] = SaldoAwalMasterAsset.
		 *
		 * <p><b>Penanganan error:</b><br>
		 * Parsing tanggal dari JSON menggunakan {@code Common.dateFormat1} yang bisa melempar
		 * ParseException; hasilnya null jika parse gagal dan tanggal persetujuan entitas
		 * digunakan sebagai fallback. Error tampilan label akun ditampilkan sebagai pesan
		 * teks informatif pada kolom jurnal.</p>
		 *
		 * <p><b>Pemeliharaan:</b><br>
		 * Query nomor bukti GrupTransaksi ({@code Projections.property("kode")}) harus
		 * konsisten dengan field yang diset saat {@code CommonAkunting.saveTransaksi} dipanggil.
		 * Nilai default tanggal menggunakan {@code saldoAwalMasterAsset.getTanggalPersetujuan()}
		 * jika field tanggalD di JSON tidak tersedia.</p>
		 *
		 * @throws Exception jika terjadi kesalahan saat membangun komponen UI atau query database.
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			Object[] objects = (Object[]) arg1;
			final SaldoAwalMasterAsset saldoAwalMasterAsset = (SaldoAwalMasterAsset) objects[1];
			JSONObject jsonObject = (JSONObject) objects[0];

			final String key;

			if (!jsonObject.isNull("key")) {
				key = jsonObject.get("key") + "";
			} else {
				key = "";
			}

			final String nama;

			if (!jsonObject.isNull("nama")) {
				nama = jsonObject.get("nama") + "";
			} else {
				nama = "";
			}

			final String nomor;

			if (!jsonObject.isNull("nomor")) {
				nomor = jsonObject.get("nomor") + "";
			} else {
				nomor = "";
			}

			final Double penagihan;
			if (!jsonObject.isNull("penagihan")) {
				penagihan = jsonObject.getDouble("penagihan");
			} else {
				penagihan = 0.0;
			}

			Double pinalti = 0.0;
			if (!jsonObject.isNull("pinalti")) {
				pinalti = jsonObject.getDouble("pinalti");
			}

			Double ppn = 0.0;
			if (!jsonObject.isNull("ppn")) {
				ppn = jsonObject.getDouble("ppn");
			}

			Double total = penagihan + ((ppn / 100.0) * penagihan);
			final Double nilai = Math.abs(total - pinalti);

			Date tanggalD = null;

			if (!jsonObject.isNull("tanggalD")) {
				tanggalD = jsonObject.get("tanggalD").toString().isEmpty() ? null
						: Common.dateFormat1.get().parse(jsonObject.get("tanggalD") + "");
			}

			final JenisPajakBarang jenisPajakBarang;
			if (!jsonObject.isNull("pajak")) {
				jenisPajakBarang = (JenisPajakBarang) ConstantValues.ambil(JenisPajakBarang.class.getName(),
						Long.parseLong(jsonObject.get("pajak") + ""));
			} else {
				jenisPajakBarang = null;
			}

			Vbox aa;
			(aa = RevisiHelper
					.createNewRevisi(SaldoAwalMasterAsset.class, saldoAwalMasterAsset,
							saldoAwalMasterAsset.getKode() == null ? ""
									: saldoAwalMasterAsset.getKode() + (nomor.isEmpty() ? "" : " " + nomor)))
					.setParent(arg0);
			aa.appendChild(new Label(nomor));
			aa.appendChild(new Label(nama));

			if (saldoAwalMasterAsset.getDisposisiSop() != null) {
				A aaa;
				(aaa = new A()).setParent(aa);
				aaa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aaa, "SOP " + saldoAwalMasterAsset.getDisposisiSop().getKeterangan() + " ("
						+ saldoAwalMasterAsset.getDisposisiSop().getSop().getNama() + ")");
				aaa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(saldoAwalMasterAsset.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}

			new Label(saldoAwalMasterAsset.getPenyedia() == null ? "" : saldoAwalMasterAsset.getPenyedia().getNama())
					.setParent(arg0);

			new Label(saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset()
					.getJenisPemesananPengadaanAsset() == null
							? ""
							: saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset()
									.getPemesananPengadaanMasterAsset().getJenisPemesananPengadaanAsset().getNama())
					.setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setWidth("95%");
			vbox.setAlign("right");
			vbox.setPack("end");
			new Label(Common.numberFormat.get().format(total)).setParent(vbox);
			if (pinalti > 0.1) {
				new Label("Pinalti " + Common.numberFormat.get().format(pinalti)).setParent(vbox);
			}

			new Label(Common.dateFormat3.get()
					.format(tanggalD != null ? tanggalD : saldoAwalMasterAsset.getTanggalPersetujuan()))
					.setParent(arg0);

			Akun akunDebet = saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset()
					.getJenisPemesananPengadaanAsset() == null
							? null
							: saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset()
									.getPemesananPengadaanMasterAsset().getJenisPemesananPengadaanAsset()
									.getAkunUtangPekerjaan();

			Akun akunKredit = saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset()
					.getPemesananPengadaanMasterAsset().getJenisPemesananPengadaanAsset() == null
							? null
							: saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset()
									.getPemesananPengadaanMasterAsset().getJenisPemesananPengadaanAsset()
									.getAkunUtangDp();

			if (akunDebet != null && akunKredit != null) {

				List<Akun> akunsDebets = new ArrayList<Akun>();
				List<Akun> akunsKredits = new ArrayList<Akun>();

				List<Double> nilaiDebets = new ArrayList<Double>();
				List<Double> nilaiKredits = new ArrayList<Double>();

				akunsDebets.add(akunDebet);
				nilaiDebets.add(nilai);

				if (jenisPajakBarang != null && jenisPajakBarang.getAkun() != null) {

					Double nilaiPajak = (penagihan * (jenisPajakBarang.getPersen() / 100.0));

					if (nilaiPajak > 0.01) {
						akunsKredits.add(jenisPajakBarang.getAkun());
						nilaiKredits.add(nilaiPajak);
					}
					akunsKredits.add(akunKredit);
					nilaiKredits.add(nilai - nilaiPajak);

				} else {

					akunsKredits.add(akunKredit);
					nilaiKredits.add(nilai);

				}

				GrupTransaksi.tampilkanJurnal(akunsDebets, nilaiDebets, akunsKredits, nilaiKredits).setParent(arg0);

			} else {
				new Label("Transaksi tidak valid."
						+ (akunDebet != null ? " Debet: " + akunDebet.getKode() + "-" + akunDebet.getNama() + "."
								: " Akun debet tidak ada.")
						+ (akunKredit != null ? " Kredit: " + akunKredit.getKode() + "-" + akunKredit.getNama() + "."
								: " Akun kredit tidak ada."))
						.setParent(arg0);
			}

			String bukti = "";
			Session session = HibernateUtil.currentSession();
			bukti = (String) session.createCriteria(GrupTransaksi.class)
					.add(Restrictions.eq("saldoAwalMasterAsset", saldoAwalMasterAsset)).add(Restrictions.eq("ref", key))
					.setMaxResults(1).setProjection(Projections.property("kode")).uniqueResult();

			new Label(saldoAwalMasterAsset.getPostingHistory() == null ? Common.getBahasaConfig("Belum diposting")
					: saldoAwalMasterAsset.getPostingHistory().toString() + ", no. bukti : " + bukti).setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);

			if (akunDebet != null && akunKredit != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data");
				button.setVisible(edit && adminLain && saldoAwalMasterAsset.getPostingHistory() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								saldoAwalMasterAsset.setPostingHistory(null);
								Common.refreshSaveOrUpdate(saldoAwalMasterAsset);
								HibernateUtil.currentSession()
										.createSQLQuery(
												"delete from akunting.grup_transaksi where saldo_awal_master_asset="
														+ saldoAwalMasterAsset.getId() + " and ref = '" + key + "'" + " and closing is null")
										.executeUpdate();

								Common.createDefaultTimer(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										loadDataDenganProgressPosting(null);
									}
								});
							}
						});

					}

				});
				button.setParent(toolbar);

				button = new MyToolbarbuttonConfig("", "/img/svg/check2-circle.svg");
				button.setTooltiptext("Posting Data");
				button.setVisible(edit && saldoAwalMasterAsset.getPostingHistory() == null && tbmuser != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();

								PostingHistory postingHistory = new PostingHistory(
										PostingHistory.JENIS_TAGIHAN_PEKERJAAN);
								postingHistory.setTbmuser(Common.getCurrentUser());
								postingHistory.setTanggal(ais.ui.util.WaktuUtil.getDate());
								postingHistory.setKeterangan(
										"Posting manual oleh " + Common.getCurrentUser().getUserNama() + " pada waktu "
												+ Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
								session.save(postingHistory);

								Akun akunDebet = saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset()
										.getPemesananPengadaanMasterAsset().getJenisPemesananPengadaanAsset() == null
												? null
												: saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset()
														.getPemesananPengadaanMasterAsset()
														.getJenisPemesananPengadaanAsset().getAkunUtangPekerjaan();

								Akun akunKredit = saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset()
										.getPemesananPengadaanMasterAsset().getJenisPemesananPengadaanAsset() == null
												? null
												: saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset()
														.getPemesananPengadaanMasterAsset()
														.getJenisPemesananPengadaanAsset().getAkunUtangDp();

								if (akunDebet != null && akunKredit != null) {
									Boolean apakahUangMasuk = true;

									String ket = "";
									try {

										ket = "Tagihan pekerjaan terhadap pemesanan \""
												+ (saldoAwalMasterAsset.getKode() + "-" + nomor + "-" + nama + " "
														+ saldoAwalMasterAsset.getKeterangan());

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}

									Akun akunDenda = null;
									Akun akunPiutangDenda = null;
									Double denda = 0.0;

									SatuanKerja satuanKerja = (SatuanKerja) (saldoAwalMasterAsset != null
											&& saldoAwalMasterAsset.getSatuanKerja() != null
													? saldoAwalMasterAsset.getSatuanKerja()
													: tbmuser.ambilSatuanKerja());

									if (tbmuser.ambilSatuanKerja() != null) {
										satuanKerja = tbmuser.ambilSatuanKerja();
									}

									if (jenisPajakBarang != null && jenisPajakBarang.getAkun() != null) {

										Double nilaiPajak = (penagihan * (jenisPajakBarang.getPersen() / 100.0));

										CommonAkunting.saveTransaksi(new Akun[] { akunDebet },
												new Akun[] { akunKredit, jenisPajakBarang.getAkun() }, akunDenda,
												akunPiutangDenda, postingHistory, apakahUangMasuk, ket,
												saldoAwalMasterAsset.getTanggalPersetujuan(), new Double[] { nilai },
												new Double[] { nilai - nilaiPajak, nilaiPajak }, denda,
												saldoAwalMasterAsset, satuanKerja, key, session);

									} else {
										if (nilai > 0.1) {
											CommonAkunting.saveTransaksi(akunDebet, akunKredit, akunDenda,
													akunPiutangDenda, postingHistory, apakahUangMasuk, ket,
													saldoAwalMasterAsset.getTanggalPersetujuan(), nilai, denda,
													saldoAwalMasterAsset, satuanKerja, key, session);
										} else {
											CommonAkunting.saveTransaksi(akunKredit, akunDebet, akunDenda,
													akunPiutangDenda, postingHistory, apakahUangMasuk, ket,
													saldoAwalMasterAsset.getTanggalPersetujuan(), nilai, denda,
													saldoAwalMasterAsset, satuanKerja, key, session);
										}
									}

									saldoAwalMasterAsset.setPostingHistory(postingHistory);
									session.update(saldoAwalMasterAsset);
								}

								loadDataDenganProgressPosting(null);
							}
						});

					}

				});
				button.setParent(toolbar);
			}

		}
	}

	/**
	 * <p><b>Tujuan:</b><br>
	 * Membangun dan mengembalikan objek {@code Criteria} Hibernate untuk query data
	 * {@code SaldoAwalMasterAsset} yang akan ditampilkan di grid, sesuai semua filter
	 * aktif (tanggal, status posting, pemilik, lokasi, ruang, kode) dan mode tampilan
	 * ({@code sudah_posting} dari parameter URL atau checkbox UI).</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membangun criteria dengan cabang logika berbeda tergantung {@code sudah_posting}:
	 * <ul>
	 *   <li><b>Mode URL (sudah_posting != null):</b> Menggunakan filter ketat: data harus
	 *       memiliki penerimaan pengadaan, jsonTermin, nilai tidak nol, sudah disetujui,
	 *       aktif, dalam rentang tanggal, dan status posting sesuai parameter (sudah atau belum).</li>
	 *   <li><b>Mode Normal (sudah_posting == null):</b> Menggunakan filter serupa tetapi
	 *       dikontrol oleh checkbox UI (searchtampil/searchtelahtampil); jika tampil dicentang,
	 *       filter belum diposting; jika telahtampil dicentang, filter sudah diposting.</li>
	 * </ul>
	 * Setelah itu, filter tambahan diterapkan untuk pemilik, lokasi, ruang, dan kode pencarian.
	 * Jika parameter {@code order} true, tambahkan ORDER BY id DESC.
	 * </p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param order {@code true} untuk menambahkan ORDER BY id DESC; {@code false} tanpa ordering.
	 *
	 * <p><b>Return:</b></p>
	 * @return Objek {@code Criteria} Hibernate siap pakai untuk query data tagihan pekerjaan.
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Blok try-catch di dalam cabang {@code sudah_posting != null} menangkap exception
	 * saat membangun kriteria dan mencatatnya ke admin. Ini memungkinkan halaman tetap
	 * berfungsi meski ada error sementara pada kriteria.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Filter SQL date() menggunakan format database yang mungkin spesifik PostgreSQL.
	 * Jika ada pergantian database, pastikan fungsi date() yang setara tersedia.
	 * Join alias {@code postingHistory} dibuat dengan LEFT_JOIN agar data tanpa posting
	 * tetap muncul; jangan ubah ke INNER_JOIN.</p>
	 */
	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(SaldoAwalMasterAsset.class);

		if (sudah_posting != null) {

			try {

				criteria.add(Restrictions.isNotNull("penerimaanPengadaanMasterAsset"))
						.add(Restrictions.isNotNull("jsonTermin"))
						.add(Restrictions.ne("nilai", 0.0))
						.add(Restrictions.isNotNull("nilai"))
						.add(Restrictions.isNotNull("disetujuiOleh"))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add((tglMulai == null || tglSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(tanggal_persetujuan) between date('"
								+ Common.databaseDateFormat.get().format(tglMulai.getValue()) + "') and date('"
								+ Common.databaseDateFormat.get().format(tglSampai.getValue()) + "')")));

				if (sudah_posting) {
					criteria.createAlias("postingHistory", "postingHistory", Criteria.LEFT_JOIN)
							.add(Restrictions.eq("postingHistory.posting", true));
				} else {
					criteria.createAlias("postingHistory", "postingHistory", Criteria.LEFT_JOIN)
							.add(Restrictions.or(Restrictions.isNull("postingHistory.id"),
									Restrictions.eq("postingHistory.posting", false)));
				}

			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}

		} else {

			if (searchtampil.isChecked()) {
				criteria.createAlias("postingHistory", "postingHistory", Criteria.LEFT_JOIN).add(Restrictions.or(
						Restrictions.isNull("postingHistory.id"), Restrictions.eq("postingHistory.posting", false)));

				searchtelahtampil.setChecked(false);
			}

			else if (searchtelahtampil.isChecked()) {
				criteria.createAlias("postingHistory", "postingHistory", Criteria.LEFT_JOIN).add(Restrictions.or(
						Restrictions.isNull("postingHistory.id"), Restrictions.eq("postingHistory.posting", false)));

				searchtampil.setChecked(false);
			}

			criteria.add(Restrictions.isNotNull("penerimaanPengadaanMasterAsset"))
					.add(Restrictions.isNotNull("jsonTermin"))
					.add(Restrictions.ne("nilai", 0.0))
					.add(Restrictions.isNotNull("nilai"))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

					.add(Restrictions.isNotNull("disetujuiOleh"))

					.add((tglMulai == null || tglSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(tanggal_persetujuan) between date('"
							+ Common.databaseDateFormat.get().format(tglMulai.getValue()) + "') and  date('"
							+ Common.databaseDateFormat.get().format(tglSampai.getValue()) + "')")));
		}

		criteria.add(
				searchpemilikAsset.getSelectedItem() == null || searchpemilikAsset.getSelectedItem().getValue() == null
						|| searchpemilikAsset.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("pemilikAsset", searchpemilikAsset.getSelectedItem().getValue()))
				.add(searchlokasi.getSelectedItem() == null || searchlokasi.getSelectedItem().getValue() == null
						|| searchlokasi.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("lokasi", searchlokasi.getSelectedItem().getValue()))
				.add((searchruang == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchruang.getAttribute("ruang") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("ruang", searchruang.getAttribute("ruang"))))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE),
								Restrictions.ilike("keteranganTermin", searchkode.getValue(), MatchMode.ANYWHERE)));

		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	/**
	 * <p><b>Tujuan:</b><br>
	 * Metode privat yang melakukan query data tagihan pekerjaan dan memuat hasilnya ke grid
	 * tanpa menggunakan indikator progress loading. Digunakan oleh
	 * {@code loadDataDenganProgressPosting} sebagai implementasi aktual setelah lapisan
	 * anti-duplikasi dan progress dikelola.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Mengeksekusi criteria dengan batas 1000 baris dan pengurutan descending.</li>
	 *   <li>Mengiterasi hasilnya: untuk setiap {@code SaldoAwalMasterAsset}, mem-parse
	 *       JSON termin dan memeriksa apakah field "key" ada. Jika tidak ada, data dilewati.</li>
	 *   <li>Memeriksa flag "merupakan_dp" pada JSON; hanya menampilkan baris yang bukan DP
	 *       ({@code merupakan_dp = false}).</li>
	 *   <li>Membungkus setiap pasangan (JSONObject, SaldoAwalMasterAsset) dalam array Object[]
	 *       dan menambahkannya ke daftar yang akan ditampilkan.</li>
	 *   <li>Mengatur model, renderer, dan paging grid dengan mold "paging", 10 item per halaman,
	 *       dan style paging "os" (overflow scroll).</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param event Event ZKoss yang memicu refresh (bisa null jika dipanggil secara programatis).
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception parsing JSON per item dicatat ke admin dan item tersebut dilewati tanpa
	 * menghentikan iterasi keseluruhan.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Batas 1000 baris di-hardcode; jika volume data besar melebihi batas ini, pertimbangkan
	 * untuk menambahkan paging server-side pada criteria. Flag "merupakan_dp" di JSON harus
	 * konsisten dengan cara generator JSON mengisi field ini.</p>
	 */
	@SuppressWarnings("unchecked")
	private void onSearchDefaultTanpaProgress(Event event) {

		List<SaldoAwalMasterAsset> saldoAwalMasterAsset = initCriteria(true).setMaxResults(1000).list();

		List<Object[]> objects = new ArrayList<Object[]>();
		for (SaldoAwalMasterAsset pemesananPengadaan : saldoAwalMasterAsset) {
			try {

				JSONObject jsonObject = new JSONObject(pemesananPengadaan.getJsonTermin());

				if (jsonObject.isNull("key")) {
					continue;
				}

				Boolean merupakan_dp;
				if (!jsonObject.isNull("merupakan_dp")) {
					merupakan_dp = Boolean.parseBoolean(jsonObject.get("merupakan_dp") + "");
				} else {
					merupakan_dp = false;
				}

				if (!merupakan_dp) {
					objects.add(new Object[] { jsonObject, pemesananPengadaan });
				}

			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

		ListModel strset = new SimpleListModel(objects);
		grid.setRowRenderer(new SaldoAwalMasterAssetRenderer());
		grid.setModelCheckMobile(strset);
		grid.setMold("paging");
		grid.setPageSize(10);
		grid.getPagingChild().setMold("os");

	}

	/**
	 * <p><b>Tujuan:</b><br>
	 * Event handler publik yang dipanggil oleh ZKoss saat pengguna memicu pencarian ulang,
	 * misalnya saat mengklik tombol cari atau mengubah filter. Mendelegasikan ke
	 * {@code loadDataDenganProgressPosting} untuk memuat data dengan indikator progress.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Merupakan delegator satu baris ke {@code loadDataDenganProgressPosting} untuk
	 * mempertahankan antarmuka event handler yang konsisten dengan konvensi ZKoss
	 * (nama metode diawali "on" + nama event) sekaligus mendapatkan manfaat dari
	 * mekanisme anti-duplikasi loading yang ada di metode tersebut.</p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param event Event ZKoss yang memicu pencarian (bisa null jika dipanggil secara programatis
	 *              dari callback lain seperti setelah posting berhasil).
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jangan menambahkan logika langsung di sini; semua logika pencarian ada di
	 * {@code onSearchDefaultTanpaProgress} dan manajemen loading ada di
	 * {@code loadDataDenganProgressPosting}.</p>
	 */
	public void onSearchDefault(Event event) {
		loadDataDenganProgressPosting(event);
	}

	/**
	 * Flag yang menunjukkan apakah proses pemuatan data posting jurnal sedang aktif berjalan.
	 * Digunakan untuk mencegah pemuatan ganda yang tumpang tindih.
	 */
	private boolean postingJurnalLoadingAktif = false;

	/**
	 * Flag yang menunjukkan ada permintaan reload baru yang masuk saat loading sedang berjalan.
	 * Setelah loading selesai, jika flag ini true, reload akan dijalankan ulang.
	 */
	private boolean postingJurnalReloadTertunda = false;

	/**
	 * <p><b>Tujuan:</b><br>
	 * Metode privat yang mengelola pemuatan data grid dengan indikator progress visual
	 * ({@code PostingJurnalLoadingUtil}) dan mekanisme anti-duplikasi loading. Memastikan
	 * bahwa jika ada permintaan reload baru saat loading sedang berjalan, permintaan tersebut
	 * tidak diabaikan melainkan dijadwalkan ulang setelah loading selesai.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Jika loading sedang aktif ({@code postingJurnalLoadingAktif = true}), set flag
	 *       {@code postingJurnalReloadTertunda = true} dan tampilkan pesan bahwa reload
	 *       akan dilakukan setelah proses yang berjalan selesai, lalu langsung return.</li>
	 *   <li>Set {@code postingJurnalLoadingAktif = true} dan reset {@code postingJurnalReloadTertunda}.</li>
	 *   <li>Tampilkan indikator progress awal melalui {@code PostingJurnalLoadingUtil.show}
	 *       dengan persentase 7%.</li>
	 *   <li>Jadwalkan eksekusi aktual menggunakan {@code Common.createDefaultTimer}:
	 *     <ul>
	 *       <li>Perbarui indikator progress ke 48%.</li>
	 *       <li>Panggil {@code onSearchDefaultTanpaProgress} untuk query dan tampilkan data.</li>
	 *       <li>Perbarui indikator progress ke 92%.</li>
	 *       <li>Dalam blok {@code finally}: reset flag loading, periksa apakah ada reload
	 *           tertunda, dan jika ada, jadwalkan reload ulang melalui timer baru.
	 *           Jika tidak ada reload tertunda, tandai loading selesai (100%).</li>
	 *     </ul>
	 *   </li>
	 * </ol>
	 * Pola ini memastikan UI tidak pernah menampilkan data yang basi akibat request
	 * yang datang terlambat (race condition antara request lama dan baru).
	 * </p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param event Event ZKoss yang memicu pemuatan (bisa null jika dipanggil programatis).
	 *              Event ini diteruskan ke {@code onSearchDefaultTanpaProgress} jika diperlukan.
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Blok {@code try-finally} memastikan flag {@code postingJurnalLoadingAktif} selalu
	 * direset meski terjadi exception di dalam {@code onSearchDefaultTanpaProgress}.
	 * Ini mencegah halaman terjebak dalam state "loading" yang tidak bisa di-reset.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Persentase progress (7%, 48%, 92%, 96%, 100%) bersifat estetik dan dapat disesuaikan.
	 * String pesan di {@code PostingJurnalLoadingUtil} harus informatif bagi pengguna
	 * yang menunggu proses selesai. Jangan mengubah logika flag tanpa memahami sepenuhnya
	 * pola anti-duplikasi yang diimplementasikan.</p>
	 */
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
