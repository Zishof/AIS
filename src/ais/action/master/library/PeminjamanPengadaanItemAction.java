
package ais.action.master.library;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.East;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.library.helper.AmbilDataAnggotaBanbox;
import ais.action.master.library.helper.AmbilDataPerpustakaanBanbox;
import ais.action.master.library.helper.PeminjamanPengadaanItemDetailAction;
import ais.action.master.library.helper.PeminjamanPengadaanItemPunyaItemHelper;
import ais.action.master.library.util.LibraryUtil;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.report.Report;
import ais.action.report.format1.library.LaporanPeminjamanPerAnggota;
import ais.action.report.format1.library.LaporanPeminjamanPerAnggotaBelumDikembalikan;
import ais.action.servlet.Wa;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.library.PeminjamanPengadaanItemDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.library.Anggota;
import ais.database.model.library.AnggotaYangDiblokir;
import ais.database.model.library.DendaKeterlambatanItem;
import ais.database.model.library.DetailTransaksi;
import ais.database.model.library.KembaliPengadaanItemDetail;
import ais.database.model.library.KunjunganAnggota;
import ais.database.model.library.PeminjamanPengadaanItem;
import ais.database.model.library.PeminjamanPengadaanItemDetail;
import ais.database.model.library.Perpustakaan;
import ais.database.model.library.PesananAnggota;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.delivery.email.sender.MailSender;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeFactory;
import net.sourceforge.barbecue.BarcodeImageHandler;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * <h2>PeminjamanPengadaanItemAction — Layar Peminjaman Item/Buku Perpustakaan</h2>
 *
 * <p><b>Untuk apa kelas ini:</b><br>
 * Kelas ini adalah "otak" di balik halaman <code>peminjaman_pengadaan_item.zul</code>
 * (Stasiun Scan berwarna biru). Ia mengatur seluruh proses <i>meminjamkan</i> item
 * koleksi perpustakaan — mulai dari petugas men-scan barcode buku atau mengetik kode
 * anggota, membuat transaksi peminjaman baru, menampilkan daftar peminjaman pada tabel,
 * menghitung batas waktu &amp; denda keterlambatan, menyetujui/menolak, mencetak barcode
 * &amp; kartu bukti, mengirim notifikasi (WhatsApp/e-mail), sampai mengekspor rekap ke
 * Excel. Kelas ini merupakan sebuah <code>GenericAutowireComposer</code> ZK: setiap
 * komponen ZUL yang ber-<code>id</code> (mis. <code>searchkodeangota</code>,
 * <code>searchkode</code>, <code>grid</code>, <code>paging</code>) otomatis di-<i>wire</i>
 * menjadi field pada kelas ini, dan setiap <code>forward="onClick=onXxx"</code> pada ZUL
 * dipetakan ke method <code>onXxx(Event)</code> di sini.</p>
 *
 * <p><b>Tanggung jawab utama (ringkas untuk pengguna awam):</b></p>
 * <ol>
 *   <li><b>Scan cepat</b> — <code>onKodeAnggota</code> menerima hasil scan/ketikan pada
 *       kotak besar di atas: bila yang di-scan barcode buku maka item ditambahkan ke
 *       keranjang peminjaman, bila yang diketik kode anggota maka form peminjaman untuk
 *       anggota tersebut dibuka. Petugas cukup "tembak" barcode lalu tekan Enter.</li>
 *   <li><b>Pencarian &amp; filter</b> — <code>onSearchDefault</code> menyaring daftar
 *       peminjaman berdasarkan nomor peminjaman, perpustakaan, anggota, rentang tanggal,
 *       barcode, judul, ISBN, serta penanda "belum dikembalikan".</li>
 *   <li><b>Tambah / ubah transaksi</b> — <code>onAdd</code> membuka jendela form detail
 *       peminjaman (delegasi ke <code>PeminjamanPengadaanItemDetailAction</code>).</li>
 *   <li><b>Hitung denda &amp; tenggat</b> — melalui <code>LibraryUtil.hitungDendaItem</code>
 *       sehingga aturan denda terpusat dan mudah dipelihara (maksimalkan <i>reuse</i>).</li>
 *   <li><b>Ekspor &amp; cetak</b> — rekap denda/peminjaman dibangun ke berkas Excel
 *       (Apache POI/XSSF) dan dapat diunduh; barcode dicetak via <i>Barbecue</i>.</li>
 * </ol>
 *
 * <p><b>Penanganan sesi Hibernate (WAJIB diperhatikan saat memelihara):</b><br>
 * Ada dua gaya sesi yang dipakai dan keduanya diperlakukan berbeda:
 * <ul>
 *   <li><b><code>HibernateUtil.currentSession()</code></b> — dipakai pada alur render UI
 *       (render baris grid, hitung jumlah, dsb.). Sesi jenis ini <u>TIDAK BOLEH ditutup</u>
 *       secara manual karena sudah dikelola/ditutup otomatis oleh kerangka kerja per
 *       request. Menutupnya justru menimbulkan "Session is closed!".</li>
 *   <li><b><code>HibernateUtil.currentNativeSession()</code></b> — dipakai di dalam
 *       <i>thread latar</i> (recalculate massal &amp; pembuatan Excel) yang berjalan di
 *       luar daur hidup request. Sesi jenis ini <u>WAJIB ditutup di blok
 *       <code>finally</code></u> agar koneksi tidak bocor bila terjadi kesalahan. Seluruh
 *       thread latar pada kelas ini sudah dibungkus <code>try { … } finally {
 *       HibernateUtil.closeSession(); }</code>.</li>
 * </ul>
 * </p>
 *
 * <p><b>Model threading:</b> Operasi berat (rekalkulasi denda seluruh item, penyusunan
 * berkas Excel) dijalankan pada <code>new Thread(...)</code> terpisah sambil memperbarui
 * label progres di layar, sehingga UI tetap responsif dan tidak "membeku". Karena berjalan
 * di thread non-ZK, akses database memakai <code>currentNativeSession()</code> (lihat
 * catatan sesi di atas), dan pembaruan komponen UI dilakukan melalui mekanisme aman ZK.</p>
 *
 * <p><b>Prinsip pemeliharaan:</b> jaga agar logika bisnis (aturan denda, batas hari,
 * blokir anggota) tetap terpusat di <code>LibraryUtil</code> dan model terkait
 * (<code>PeminjamanPengadaanItem</code>, <code>PeminjamanPengadaanItemDetail</code>,
 * <code>DendaKeterlambatanItem</code>) — kelas Action ini idealnya hanya merangkai UI dan
 * memanggil util tersebut. Bila menambah kolom filter baru di ZUL, tambahkan pula field
 * ber-<code>id</code> yang sama di sini dan sesuaikan <code>onSearchDefault</code>. Semua
 * kode dijaga kompatibel dengan Java 1.7 (tanpa lambda, <i>diamond operator</i> penuh, atau
 * <i>try-with-resources</i>) dan blok <code>try/catch</code> gaya Java 1.6 (tanpa
 * <i>multi-catch</i>).</p>
 *
 * @see KembaliPengadaanItemAction Layar pengembalian item (pasangan kelas ini).
 * @see ais.action.master.library.util.LibraryUtil Pusat aturan denda &amp; util perpustakaan.
 * @see ais.action.master.library.helper.PeminjamanPengadaanItemDetailAction Form detail peminjaman.
 */
public class PeminjamanPengadaanItemAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private MyGrid grid;
	private Paging paging;

	private Textbox searchkode;
	private Textbox searchbarkode;
	private Textbox searchjudul;
	private Textbox searchisbn;
	private Textbox searchkodeangota;
	private AmbilDataPerpustakaanBanbox searchperpustakaan;
	private AmbilDataAnggotaBanbox searchanggota;
	private MyDatebox searchmulai;
	private MyDatebox searchsampai;
	private MyCheckboxConfig searchBelumDikembalikan;

	private MyTextbox kode;
	private AmbilDataAnggotaBanbox anggota;
	private MyTextbox keterangan;
	private MyCheckboxConfig overrideKebijakan;
	private MyDatebox tanggalPembuatan;
	private Combobox perpustakaan;

	private boolean edit = false;
	private boolean delete = false;
	// private boolean approve = false;
	// private boolean reject = false;

	private Perpustakaan currentPerpustakaan;

	private PeminjamanPengadaanItem peminjamanPengadaanItem;
	private MyToolbarbuttonConfig add;

	private MyGrid gridItem;
	private PeminjamanPengadaanItemPunyaItemHelper peminjamanPengadaanItemPunyaItemHelper;

	private Combobox kunjunganAnggota;
	private Vbox informasi;
	private Vbox foto;
	private MyDatebox tanggalPersetujuan;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		searchperpustakaan.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		searchanggota.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		searchkodeangota.addEventListener("onFocus", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				searchkodeangota.select();
			}
		});

		if (searchmulai != null) { searchmulai.setReadonly(true); }
		if (searchsampai != null) { searchsampai.setReadonly(true); }

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 7);
		if (searchmulai != null) { searchmulai.setValue(calendar.getTime()); }
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (searchsampai != null) { searchsampai.setValue(calendar.getTime()); }

		// add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		if (add != null) { add.setTooltiptext("Tambah"); }
		if (add != null) { add.setVisible(false); }

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		// approve = CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);
		// reject = CommonPrivilages.checkPrevilages(CommonPrivilages.REJECT);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		final File file = new File("/opt/PerpustakaanDekstop.zip");
		if (file != null && file.exists()) {
			MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Download Aplikasi Desktop", "/img/excel.png");
			Common.appendKeToolbar(upload, add, comp);
			upload.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Filedownload.save(new FileInputStream(file), "application/zip", file.getName());
				}
			});
		}

		// Timer timer = new Timer(5000);
		// timer.setRepeats(true);
		// timer.setParent(page.getFirstRoot());
		// timer.addEventListener("onTimer", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// searchkodeangota.focus();
		// searchkodeangota.select();
		// }
		// });
		// timer.start();

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Proses ulang keterlambatan", "/img/excel.png");
		Common.appendKeToolbar(upload, add, comp);
		upload.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

				final Label label = new Label(ais.common.Common.getBahasaConfig("Proses ulang keterlambatan .."));
				Clients.showBusy(label.getValue());
				final Timer timer = new Timer(200);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.setRepeats(true);
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Clients.showBusy(label.getValue());
						if (label.getValue().isEmpty()) {
							System.out.println("loading file " + file.getAbsolutePath());
							MyMessageboxConfig.show("Proses ulang keterlambatan berhasil dilakukan.", "Pemberitahuan",
									MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											onSearchDefault(arg0);
										}
									});
							Clients.clearBusy();
							timer.detach();
						}

					}
				});
				timer.start();

				new Thread(new Runnable() {

					@Override
					public void run() {
						Session session = HibernateUtil.currentNativeSession();
							List<Long> ids = null;
							try {
								ids = session.createCriteria(PeminjamanPengadaanItemDetail.class)

								.createAlias("itemPunyaBarcode", "itemPunyaBarcode", Criteria.LEFT_JOIN)
								.createAlias("item", "item", Criteria.LEFT_JOIN)

								.add(searchisbn.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.or(
												Restrictions.ilike("item.isbn", searchisbn.getValue().trim(),
														MatchMode.ANYWHERE),
												Restrictions.ilike("item.isbn10", searchisbn.getValue().trim(),
														MatchMode.ANYWHERE)))

								.add(searchjudul.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.ilike("item.nama", searchjudul.getValue().trim(),
												MatchMode.ANYWHERE))

								.add(searchbarkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.ilike("itemPunyaBarcode.barcode",
												searchbarkode.getValue().trim(), MatchMode.EXACT))

								.add(searchBelumDikembalikan.isChecked()
										? Restrictions.isNull("kembaliPengadaanItemDetail")
										: Restrictions.sqlRestriction("true"))

								.setProjection(Projections.property("id")).list();

								System.out.println("Proses ids " + ids.size());
							int rowCount = ids.size();
							for (int i = 0; i < rowCount; i++) {
								try {
									PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail = (PeminjamanPengadaanItemDetail) session
											.createCriteria(PeminjamanPengadaanItemDetail.class)
											.add(Restrictions.idEq(ids.get(i))).uniqueResult();
									if (peminjamanPengadaanItemDetail != null) {
										session.getTransaction().begin();
										session.update(peminjamanPengadaanItemDetail);
										session.getTransaction().commit();

									}

									label.setValue("Memproses data \""
											+ (peminjamanPengadaanItemDetail == null ? ""
													: peminjamanPengadaanItemDetail.getPeminjamanPengadaanItem()
															.getAnggota().toString() + " - "
															+ peminjamanPengadaanItemDetail.getItem().getNama())
											+ "\" (" + Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}

							}
						} catch (Exception e1) {
								e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/library/PeminjamanPengadaanItemAction.java:414");
							} finally {
								HibernateUtil.closeSession();
							}
							ids = null;

						label.setValue("");
					}
				}).start();

			}

		});
	        FilterLanjutHelper.setup(comp);
}

	public void onCetakPeminjamanPerAnggota(Event event) throws Exception {
		LaporanPeminjamanPerAnggota laporan = new LaporanPeminjamanPerAnggota();
		laporan.setTitle("Cetak Laporan");
		page.getFirstRoot().appendChild(laporan);
		laporan.setHeight("95%");
		laporan.setWidth("90%");
		laporan.setClosable(true);
		laporan.onModal();
	}

	public void onSynchronizeDenda(Event event) throws Exception {

		final MyWindow window = new MyWindow("Pilih Tanggal", "none", true);
		window.setParent(page.getFirstRoot());
		window.setHeight("400px");
		window.setWidth("600px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		Center center = new Center();
		center.setParent(borderlayout);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("40%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 1);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai Peminjaman *"));
		final MyDatebox mulai;
		row.appendChild(mulai = new MyDatebox(calendar.getTime()));
		mulai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Sampai Peminjaman *"));
		final MyDatebox sampai;
		row.appendChild(sampai = new MyDatebox(ais.ui.util.WaktuUtil.getDate()));
		sampai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		final MyCheckboxConfig hanyaYgBelumDikembalikan;
		row.appendChild(hanyaYgBelumDikembalikan = new MyCheckboxConfig("Tampilkan hanya yang belum dikembalikan"));
		hanyaYgBelumDikembalikan.setChecked(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		final MyCheckboxConfig hanyaYgAdaDenda;
		row.appendChild(hanyaYgAdaDenda = new MyCheckboxConfig("Tampilkan hanya yang ada denda nya"));
		hanyaYgAdaDenda.setChecked(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		final MyCheckboxConfig hanyaYgTelahMengembalikan;
		row.appendChild(hanyaYgTelahMengembalikan = new MyCheckboxConfig("Tampilkan hanya yang telah mengembalikan"));

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Proses", "/img/save.gif");
		save.setTooltiptext("Download");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();

				final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
				final Intbox intbox = new Intbox(10);
				Clients.showBusy(label.getValue());

				final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/cetak_data_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");
				final File file;
				(file = new File(filename)).createNewFile();

				final Timer timer = new Timer(200);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.setRepeats(true);
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						try {

							Clients.showBusy(label.getValue());
							System.out.println("label " + label.getValue());

							if (label.getValue().trim().equalsIgnoreCase("-")) {
								Clients.clearBusy();
								timer.detach();
							} else if (label.getValue().isEmpty()) {

								Center center = new Center();
								final MyWindow window = new MyWindow("Cetak Data", "none", true);
								window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
								window.setHeight("97%");
								window.setWidth("90%");

								Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
								borderlayout.setParent(window);

								ais.ui.util.ZkCompat.setFlex(center, true);
								center.setParent(borderlayout);

								System.out.println("loading file " + file.getAbsolutePath());
								Common.clear(center);
								Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
								Common.clear(center);
								spreadsheet.setParent(center);
								spreadsheet.setWidth("100%");
								spreadsheet.setHeight("100%");
								spreadsheet.setSrc("../../tmp/" + file.getName());

								spreadsheet.setMaxrows(intbox.getValue() + 1);
								spreadsheet.setMaxcolumns(14);
								ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

								South south = new South();
								south.setParent(borderlayout);

								Toolbar toolbar = new Toolbar();
								// toolbar.setHeight("25px");
								toolbar.setParent(south);
								MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
								cancel.setTooltiptext("Tutup");
								cancel.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										window.detach();
									}
								});
								cancel.setParent(toolbar);

								MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data",
										"/img/excel.png");
								print.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {

										try {
											Filedownload.save(new FileInputStream(file),
													"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
													file.getName());
										} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
									}
								});
								print.setParent(toolbar);

								window.setVisible(true);
								window.onModal();

								Clients.clearBusy();
								timer.detach();
							}

						} catch (Exception e) {
							Clients.clearBusy();
						}

					}
				});
				timer.start();

				try {

					Clients.showBusy(label.getValue());

					new Thread(new Runnable() {

						@SuppressWarnings("unchecked")
						@Override
						public void run() {

							try {
								Session session = HibernateUtil.currentNativeSession();
								List<Long> data = session.createCriteria(PeminjamanPengadaanItemDetail.class)
										.add(hanyaYgBelumDikembalikan.isChecked()
												? Restrictions.isNull("kembaliPengadaanItemDetail")
												: Restrictions.sqlRestriction("true"))

										.add(hanyaYgTelahMengembalikan.isChecked()
												? Restrictions.isNotNull("kembaliPengadaanItemDetail")
												: Restrictions.sqlRestriction("true"))

										.addOrder(Order.desc("id")).setProjection(Projections.property("id"))
										.createAlias("peminjamanPengadaanItem", "peminjamanPengadaanItem")

										.add(Restrictions.sqlRestriction("date(tanggal_pembuatan) between date('"
												+ Common.databaseDateFormat.get().format(mulai.getValue()) + "') and date('"
												+ Common.databaseDateFormat.get().format(sampai.getValue()) + "')"))

										.setMaxResults(1048576).list();
								HibernateUtil.closeSession();

								intbox.setValue(data.size());
								System.out.println("data = " + data.size());

								XSSFWorkbook workbook = new XSSFWorkbook();
								XSSFSheet sheet = workbook.createSheet("CETAK DATA");
								sheet.setDefaultColumnWidth(20);
								int rowIndex = 0;

								XSSFRow rowhead = sheet.createRow((short) 0);
								String[] columns = new String[] { "id", "Anggota", "Item", "Barcode", "Waktu Pinjam",
										"Harus Kembali", "Perpanjangan", "Batas", "Lama Peminjaman", "Terlambat",
										"Tanggal Dikembalikan", "Denda", "Telah dibayar", "Denda Dibayar" };
								for (int i = 0; i < columns.length; i++) {
									rowhead.createCell(i).setCellValue(columns[i].toUpperCase());
								}

								XSSFCellStyle notLocked = workbook.createCellStyle();
								notLocked.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
								notLocked.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));

								for (Long id : data) {
									try {

										session = HibernateUtil.currentNativeSession();
										PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail = (PeminjamanPengadaanItemDetail) session
												.createCriteria(PeminjamanPengadaanItemDetail.class)
												.add(Restrictions.idEq(id)).uniqueResult();

										if (peminjamanPengadaanItemDetail != null) {

//											Double denda = LibraryUtil.hitungDendaItem(peminjamanPengadaanItemDetail);

											DendaKeterlambatanItem dendaPerItem = LibraryUtil
													.hitungDendaItem(peminjamanPengadaanItemDetail);

											Double denda = dendaPerItem == null ? 0.0 : dendaPerItem.getDenda();
											denda = denda * peminjamanPengadaanItemDetail.getJumlah();

											if (hanyaYgAdaDenda.isChecked() && denda < 0.01) {
												continue;
											}
											rowIndex++;

											KembaliPengadaanItemDetail kembaliPengadaanItemDetail = peminjamanPengadaanItemDetail
													.getKembaliPengadaanItemDetail();

											label.setValue("Sedang memproses data "
													+ peminjamanPengadaanItemDetail.toString() + " ("
													+ Common.numberFormat.get().format(rowIndex * 100.0 / data.size())
													+ " %)");

											XSSFRow row = sheet.createRow(rowIndex);

											row.createCell(0).setCellValue(peminjamanPengadaanItemDetail.getId());
											row.createCell(1).setCellValue(peminjamanPengadaanItemDetail
													.getPeminjamanPengadaanItem().getAnggota().toString());

											row.createCell(2)
													.setCellValue(peminjamanPengadaanItemDetail.getItem().toString());

											row.createCell(3).setCellValue(
													peminjamanPengadaanItemDetail.getItemPunyaBarcode() == null ? ""
															: peminjamanPengadaanItemDetail.getItemPunyaBarcode()
																	.getBarcode());
											row.createCell(4).setCellValue(
													Common.dateFormat5.get().format(peminjamanPengadaanItemDetail
															.getPeminjamanPengadaanItem().getTanggalPembuatan()));
											row.createCell(5).setCellValue(
													peminjamanPengadaanItemDetail.getBatasWaktupengembalian() == null
															? ""
															: Common.dateFormat5.get().format(peminjamanPengadaanItemDetail
																	.getBatasWaktupengembalian()));

											row.createCell(6).setCellValue(
													peminjamanPengadaanItemDetail.getJumlahPerpanjangan());
											row.createCell(7)
													.setCellValue(peminjamanPengadaanItemDetail.getJumlahHariBatas());

											row.createCell(8)
													.setCellValue(peminjamanPengadaanItemDetail.getJumlahSelisihHari());
											row.createCell(9).setCellValue(
													peminjamanPengadaanItemDetail.getJumlahHariTerlambat());

											if (kembaliPengadaanItemDetail != null) {
												if (denda.intValue() != kembaliPengadaanItemDetail.getDenda()
														.intValue()) {
													session.refresh(kembaliPengadaanItemDetail);
													kembaliPengadaanItemDetail.setDenda(denda);
													session.getTransaction().begin();
													Common.refreshUpdate(session, kembaliPengadaanItemDetail);
													session.getTransaction().commit();
												}

												row.createCell(10)
														.setCellValue(kembaliPengadaanItemDetail.getTanggal() == null
																? "Belum dikembalikan"
																: Common.dateFormat5.get().format(
																		kembaliPengadaanItemDetail.getTanggal()));

												row.createCell(11).setCellValue(Common.numberFormat.get()
														.format(kembaliPengadaanItemDetail.getDenda()));
												row.createCell(12)
														.setCellValue(kembaliPengadaanItemDetail.getTelahDibayar());
												row.createCell(13).setCellValue(Common.numberFormat.get()
														.format(kembaliPengadaanItemDetail.getDibayarSejumlah()));
											} else {
												row.createCell(10).setCellValue("Belum dikembalikan");

												row.createCell(11).setCellValue(Common.numberFormat.get().format(denda));
												row.createCell(12).setCellValue("-");
												row.createCell(13).setCellValue("-");
											}
										}

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}

									HibernateUtil.closeSession();
								}

								try {
									FileOutputStream fileOut = new FileOutputStream(filename);
									workbook.write(fileOut);
									fileOut.close();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									Common.tampilErrorJikaAdmin(e);
								}
								System.out.println("Your excel file has been generated! ");
								data.clear();
								data = null;
								label.setValue("");
							} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
									label.setValue("-");
								} finally {
									HibernateUtil.closeSession();
								}
						}
					}).start();

				} catch (Exception e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
				}

			}
		});
		save.setParent(toolbar);

		window.onModal();

	}

	public void onCetakPeminjamanPerAnggotaBelumDikembalikan(Event event) throws Exception {
		LaporanPeminjamanPerAnggotaBelumDikembalikan laporan = new LaporanPeminjamanPerAnggotaBelumDikembalikan();
		laporan.setTitle("Cetak Laporan");
		page.getFirstRoot().appendChild(laporan);
		laporan.setHeight("95%");
		laporan.setWidth("90%");
		laporan.setClosable(true);
		laporan.onModal();
	}

	@SuppressWarnings("unchecked")
	private void setujui(PeminjamanPengadaanItem peminjamanPengadaanItem) {
		Session session = HibernateUtil.currentSession();
		peminjamanPengadaanItem.setDisetujuiOleh(Common.getCurrentUser());
		peminjamanPengadaanItem.setTanggalPersetujuan(tanggalPersetujuan.getValue());
		Common.refreshUpdate(session, peminjamanPengadaanItem);

		List<PeminjamanPengadaanItemDetail> peminjamanPengadaanItemDetails = session
				.createCriteria(PeminjamanPengadaanItemDetail.class)
				.add(Restrictions.eq("peminjamanPengadaanItem", peminjamanPengadaanItem)).list();

		for (PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail : peminjamanPengadaanItemDetails) {
			Number alreadyPosted = (Number) session.createCriteria(DetailTransaksi.class)
					.add(Restrictions.eq("peminjamanPengadaanItemDetail", peminjamanPengadaanItemDetail))
					.setProjection(Projections.rowCount()).uniqueResult();
			if (alreadyPosted.longValue() > 0L) continue;
			DetailTransaksi detailTransaksi = new DetailTransaksi();
			detailTransaksi.setAnggota(peminjamanPengadaanItem.getAnggota());
			detailTransaksi.setPeminjamanPengadaanItemDetail(peminjamanPengadaanItemDetail);
			detailTransaksi.setQtyBonus(0.0);

			detailTransaksi.setItem(peminjamanPengadaanItemDetail.getItem());
			detailTransaksi.setKeterangan("Transaksi Peminjaman");
			detailTransaksi.setKodeTransaksi(LibraryUtil.PINJAM_KELUAR);
			detailTransaksi.setPerpustakaan(peminjamanPengadaanItem.getPerpustakaan());
			detailTransaksi.setQty(peminjamanPengadaanItemDetail.getJumlah());
			detailTransaksi.setTanggal(peminjamanPengadaanItem.getTanggalPersetujuan());
			detailTransaksi.setTanggalDanWaktu(peminjamanPengadaanItem.getTanggalPersetujuan());
			detailTransaksi.setItemPunyaBarcode(peminjamanPengadaanItemDetail.getItemPunyaBarcode());
			session.save(detailTransaksi);
		}

		// disetujuiTanggal.setValue(peminjamanPengadaanItem.getTanggalPersetujuan()
		// == null ? ""
		// :
		// Common.dateFormat3.get().format(peminjamanPengadaanItem.getTanggalPersetujuan()));
		// disetujuiOleh.setValue(peminjamanPengadaanItem.getDisetujuiOleh() ==
		// null ? ""
		// : peminjamanPengadaanItem.getDisetujuiOleh().getUserNama());
	}

	class PeminjamanPengadaanItemRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PeminjamanPengadaanItem peminjamanPengadaanItem = (PeminjamanPengadaanItem) arg1;

			(new PeminjamanPengadaanItemDetailAction(peminjamanPengadaanItem)).setParent(arg0);

			LibraryUtil.gambarAnggota(peminjamanPengadaanItem.getAnggota()).setParent(arg0);

			RevisiHelper.createNewRevisi(PeminjamanPengadaanItem.class, peminjamanPengadaanItem,
					peminjamanPengadaanItem.getKode()).setParent(arg0);

			new Label(peminjamanPengadaanItem.getPerpustakaan() == null ? ""
					: peminjamanPengadaanItem.getPerpustakaan().getNama()).setParent(arg0);

			new Label(
					peminjamanPengadaanItem.getAnggota() == null ? "" : peminjamanPengadaanItem.getAnggota().toString())
					.setParent(arg0);

			new Label(peminjamanPengadaanItem.getKembaliPengadaanItem() == null ? "Belum dikembalikan"
					: "Sudah dikembalikan (" + peminjamanPengadaanItem.getKembaliPengadaanItem() + ")").setParent(arg0);

			new Label(peminjamanPengadaanItem.getDibuatOleh() == null ? ""
					: peminjamanPengadaanItem.getDibuatOleh().getUserNama()).setParent(arg0);
			new Label(peminjamanPengadaanItem.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(peminjamanPengadaanItem.getTanggalPembuatan())).setParent(arg0);

			final Html htmldenda = new ais.ui.util.MyHtml();
			htmldenda.setParent(arg0);

			// if (peminjamanPengadaanItem.getKembaliPengadaanItem() == null
			// && ais.ui.util.WaktuUtil.getDate().after(peminjamanPengadaanItem
			// .getBatasWaktupengembalian())) {
			// arg0.setStyle("background-color: rgba(205,92,92,0.4);");
			// }

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					String content = LibraryUtil.tampilanSummaryPeminjaman(null, peminjamanPengadaanItem);
					htmldenda.setContent(content);
				}
			});

			(new Label(peminjamanPengadaanItem.getDisetujuiOleh() == null ? ""
					: peminjamanPengadaanItem.getDisetujuiOleh().getUserNama())).setParent(arg0);

			(new Label(peminjamanPengadaanItem.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(peminjamanPengadaanItem.getTanggalPersetujuan()))).setParent(arg0);
			// new
			// Label(peminjamanPengadaanItem.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Peminjaman Item");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				public void onEvent(Event event) throws Exception {

					File myfilebarcode = new File(application.getRealPath("/report") + "/barcode_"
							+ peminjamanPengadaanItem.getKode() + ".png");
					Barcode mybarcode = BarcodeFactory.createCode128B(peminjamanPengadaanItem.getKode());
					BarcodeImageHandler.savePNG(mybarcode, myfilebarcode);
					String kodeBarcode = myfilebarcode.getAbsolutePath();

					Map parameters = ais.common.HashMapGenerator.getRand();
					parameters.put("id", peminjamanPengadaanItem.getId());
					parameters.put("kode_barcode", kodeBarcode);

					parameters.put("info", LibraryUtil.tampilanSummaryPeminjaman(null, peminjamanPengadaanItem));

					Report.generatePDFReport(Report.PDF, parameters, "library/peminjaman",
							peminjamanPengadaanItem.getTanggalPembuatan());
				}

			});
			button.setParent(toolbar);
			//
			// final MyToolbarbuttonConfig disetujui = new
			// MyToolbarbuttonConfig("",
			// "/img/svg/check2.svg");
			//
			// final MyToolbarbuttonConfig dibatalkan = new
			// MyToolbarbuttonConfig("",
			// "/img/svg/warning-outline.svg");
			final MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			final MyToolbarbuttonConfig rubah = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			//
			// disetujui.setVisible(approve
			// && peminjamanPengadaanItem.getDisetujuiOleh() == null);
			// dibatalkan
			// .setVisible(reject
			// && peminjamanPengadaanItem.getDisetujuiOleh() != null
			// && peminjamanPengadaanItem
			// .getKembaliPengadaanItem() == null);

			// disetujui.setTooltiptext("Persetujuan");
			//
			// disetujui.addEventListener("onClick", new EventListener() {
			// @Override
			// public void onEvent(Event event) throws Exception {
			// MyMessageboxConfig.show("Apakah yakin ingin mensetujui Peminjaman
			// Item
			// ini ?", "Pertanyaan",
			// MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
			// MyMessageboxConfig.QUESTION, new
			// EventListener() {
			//
			// @SuppressWarnings("unchecked")
			// @Override
			// public void onEvent(Event event) throws Exception {
			// int i = Integer.parseInt(event.getData().toString());
			// if (i == MyMessageboxConfig.OK) {
			// Session session = HibernateUtil.currentSession();
			// // Integer countItemjumlah = ((Number)
			// // session
			// // .createCriteria(
			// // PeminjamanPengadaanItemDetail.class)
			// // .setProjection(
			// // Projections
			// // .count("id"))
			// // .add(Restrictions
			// // .eq("peminjamanPengadaanItem",
			// // peminjamanPengadaanItem))
			// // .add(Restrictions.lt(
			// // "jumlah", 1.0))
			// // .uniqueResult())
			// // .intValue();
			// //
			// // if (!countItemjumlah.equals(0)) {
			// // MyMessageboxConfig
			// // .show("Lengkapilah jumlah !",
			// // "Peringatan",
			// // MyMessageboxConfig.OK,
			// // MyMessageboxConfig.EXCLAMATION);
			// // return;
			// // }
			//
			// peminjamanPengadaanItem.setDisetujuiOleh(Common.getCurrentUser());
			// peminjamanPengadaanItem.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
			// Common.refreshUpdate(session, peminjamanPengadaanItem);
			//
			// List<PeminjamanPengadaanItemDetail>
			// peminjamanPengadaanItemDetails = session
			// .createCriteria(PeminjamanPengadaanItemDetail.class).add(Restrictions
			// .eq("peminjamanPengadaanItem", peminjamanPengadaanItem))
			// .list();
			//
			// session.createSQLQuery(
			// "delete from library.detail_transaksi where
			// peminjaman_pengadaan_item_detail in (select id from
			// library.peminjaman_pengadaan_item_detail where
			// peminjaman_pengadaan_item = "
			// + peminjamanPengadaanItem.getId() + ");")
			// .executeUpdate();
			// for (PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail
			// : peminjamanPengadaanItemDetails) {
			// DetailTransaksi detailTransaksi = new DetailTransaksi();
			// detailTransaksi.setAnggota(peminjamanPengadaanItem.getAnggota());
			// detailTransaksi
			// .setPeminjamanPengadaanItemDetail(peminjamanPengadaanItemDetail);
			// detailTransaksi.setQtyBonus(0.0);
			//
			// detailTransaksi.setItem(peminjamanPengadaanItemDetail.getItem());
			// detailTransaksi.setKeterangan("Transaksi Peminjaman");
			// detailTransaksi.setKodeTransaksi(LibraryUtil.PINJAM_KELUAR);
			// detailTransaksi.setPerpustakaan(peminjamanPengadaanItem.getPerpustakaan());
			// detailTransaksi.setQty(peminjamanPengadaanItemDetail.getJumlah());
			// detailTransaksi.setTanggal(ais.ui.util.WaktuUtil.getDate());
			// detailTransaksi.setItemPunyaBarcode(
			// peminjamanPengadaanItemDetail.getItemPunyaBarcode());
			// session.save(detailTransaksi);
			// }
			//
			// disetujuiTanggal
			// .setValue(peminjamanPengadaanItem.getTanggalPersetujuan() == null
			// ? ""
			// : Common.dateFormat3.get().format(
			// peminjamanPengadaanItem.getTanggalPersetujuan()));
			// disetujuiOleh.setValue(peminjamanPengadaanItem.getDisetujuiOleh()
			// == null ? ""
			// : peminjamanPengadaanItem.getDisetujuiOleh().getUserNama());
			// disetujui.setVisible(
			// approve && peminjamanPengadaanItem.getDisetujuiOleh() == null);
			// dibatalkan.setVisible(
			// reject && peminjamanPengadaanItem.getDisetujuiOleh() != null);
			// rubah.setVisible(edit &&
			// peminjamanPengadaanItem.getDisetujuiOleh() == null);
			// hapus.setVisible(delete &&
			// peminjamanPengadaanItem.getDisetujuiOleh() == null);
			// if (detail != null) {
			// Common.clear(detail);
			// detail.display();
			// }
			//
			// final Timer timer = new Timer(500);
			// timer.setParent(page.getFirstRoot());
			// timer.addEventListener("onTimer", new EventListener() {
			//
			// @SuppressWarnings("rawtypes")
			// @Override
			// public void onEvent(Event arg0) throws Exception {
			//
			// final File myfilebarcode = new
			// File(application.getRealPath("/report")
			// + "/barcode_" + peminjamanPengadaanItem.getKode() + ".png");
			// Barcode mybarcode = BarcodeFactory
			// .createCode128B(peminjamanPengadaanItem.getKode());
			// BarcodeImageHandler.savePNG(mybarcode, myfilebarcode);
			// String kodeBarcode = myfilebarcode.getAbsolutePath();
			//
			// final Map parameters = ais.common.HashMapGenerator.getRand();
			// parameters.put("id", peminjamanPengadaanItem.getId());
			// parameters.put("kode_barcode", kodeBarcode);
			//
			// parameters.put("info",
			// LibraryUtil.tampilanSummaryPeminjaman(
			// peminjamanPengadaanItem.getKembaliPengadaanItem(),
			// peminjamanPengadaanItem));
			//
			// Report.generatePDFReport(Report.PDF, parameters,
			// "library/peminjaman",
			// peminjamanPengadaanItem.getTanggalPembuatan());
			// timer.detach();
			// }
			// });
			// timer.start();
			// }
			// }
			// });
			// }
			//
			// });
			// disetujui.setParent(toolbar);
			//
			// dibatalkan.setTooltiptext("Dibatalkan");
			// dibatalkan.addEventListener("onClick", new EventListener() {
			// @Override
			// public void onEvent(Event event) throws Exception {
			//
			// MyMessageboxConfig.show("Apakah yakin ingin membatalkan
			// Peminjaman Item
			// ini ?", "Pertanyaan",
			// MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
			// MyMessageboxConfig.QUESTION, new
			// EventListener() {
			//
			// @Override
			// public void onEvent(Event event) throws Exception {
			// int i = Integer.parseInt(event.getData().toString());
			// if (i == MyMessageboxConfig.OK) {
			// Session session = HibernateUtil.currentSession();
			//
			// peminjamanPengadaanItem.setDisetujuiOleh(null);
			// peminjamanPengadaanItem.setTanggalPersetujuan(null);
			//
			// Common.refreshUpdate(session, peminjamanPengadaanItem);
			//
			// Common.refreshUpdate(session, peminjamanPengadaanItem);
			//
			// session.createSQLQuery(
			// "delete from library.detail_transaksi where
			// peminjaman_pengadaan_item_detail in (select id from
			// library.peminjaman_pengadaan_item_detail where
			// peminjaman_pengadaan_item = "
			// + peminjamanPengadaanItem.getId() + ");")
			// .executeUpdate();
			//
			// disetujuiTanggal
			// .setValue(peminjamanPengadaanItem.getTanggalPersetujuan() == null
			// ? ""
			// : Common.dateFormat3.get().format(
			// peminjamanPengadaanItem.getTanggalPersetujuan()));
			// disetujuiOleh.setValue(peminjamanPengadaanItem.getDisetujuiOleh()
			// == null ? ""
			// : peminjamanPengadaanItem.getDisetujuiOleh().getUserNama());
			// disetujui.setVisible(
			// approve && peminjamanPengadaanItem.getDisetujuiOleh() == null);
			// dibatalkan.setVisible(
			// reject && peminjamanPengadaanItem.getDisetujuiOleh() != null);
			// rubah.setVisible(edit &&
			// peminjamanPengadaanItem.getDisetujuiOleh() == null);
			// hapus.setVisible(delete &&
			// peminjamanPengadaanItem.getDisetujuiOleh() == null);
			// if (detail != null) {
			// Common.clear(detail);
			// detail.display();
			// }
			// }
			// }
			// });
			// }
			//
			// });
			// dibatalkan.setParent(toolbar);

			rubah.setTooltiptext("Ubah Data");
			rubah.setVisible(edit);
			rubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(peminjamanPengadaanItem);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			rubah.setParent(toolbar);

			hapus.setTooltiptext("Transaksi peminjaman yang sudah tercatat tidak dapat dihapus");
			hapus.setVisible(false);
			hapus.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show(
							"Transaksi peminjaman yang sudah dicatat tidak boleh dihapus. Gunakan proses pengembalian/reversal agar histori audit tetap utuh.",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);

				}
			});
			hapus.setParent(toolbar);
			toolbar.setParent(arg0);
		}
	}

	public void onAdd(Event event) throws Exception {
		init(new PeminjamanPengadaanItem());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	protected void initDetail(final PeminjamanPengadaanItem peminjamanPengadaanItem, Component component)
			throws Exception {
		this.peminjamanPengadaanItem = peminjamanPengadaanItem;
		Tabbox tabbox = new Tabbox();
		tabbox.setParent(component);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabDipinjam = new MyTabConfig("Item Dipinjam");
		tabDipinjam.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		final Tabpanel tabpanelDipinjam = new ais.ui.util.MyTabpanel();
		tabpanelDipinjam.setParent(tabpanels);
		tabpanelDipinjam.setWidth("100%");

		tabpanelDipinjam
				.appendChild((peminjamanPengadaanItemPunyaItemHelper = new PeminjamanPengadaanItemPunyaItemHelper(
						gridItem = new MyGrid()))
						.initDetail(PeminjamanPengadaanItemAction.this.peminjamanPengadaanItem));

	}

	private void init(final PeminjamanPengadaanItem peminjamanPengadaanItem) throws Exception {
		this.peminjamanPengadaanItem = peminjamanPengadaanItem;
		addWindow.setTitle(peminjamanPengadaanItem.getId() == null ? "Tambah Peminjaman Item" : "Ubah Peminjaman Item");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		East east = new East();
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setWidth("70%");

		initDetail(peminjamanPengadaanItem, east);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Peminjaman *"));
		String mykode = peminjamanPengadaanItem.getKode();

		row.appendChild(kode = new MyTextbox(
				peminjamanPengadaanItem.getKode() == null ? mykode : peminjamanPengadaanItem.getKode()));
		kode.setWidth("90%");
		kode.setDisabled(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Peminjaman *"));
		row.appendChild(tanggalPembuatan = new MyDatebox(
				peminjamanPengadaanItem.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
						: peminjamanPengadaanItem.getTanggalPembuatan()));
		tanggalPembuatan.setFormat(Common.dateFormat.get().toPattern());

		tanggalPembuatan.setWidth("90%");
		tanggalPembuatan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Persetujuan *"));
		row.appendChild(tanggalPersetujuan = new MyDatebox(
				peminjamanPengadaanItem.getTanggalPersetujuan() == null ? ais.ui.util.WaktuUtil.getDate()
						: peminjamanPengadaanItem.getTanggalPersetujuan()));
		tanggalPersetujuan.setFormat(Common.dateFormat.get().toPattern());

		tanggalPersetujuan.setWidth("90%");
		tanggalPersetujuan.setReadonly(true);

		final EventListener kunjunganEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (peminjamanPengadaanItemPunyaItemHelper != null) {
					peminjamanPengadaanItemPunyaItemHelper.setAnggota((Anggota) anggota.getAttribute("anggota"),
							currentPerpustakaan);
				}

				Common.clear(kunjunganAnggota);
				if (anggota.getAttribute("anggota") == null) {
					return;
				}
				if (perpustakaan.getSelectedItem() == null) {
					return;
				}

				Session session = HibernateUtil.currentSession();
				KunjunganAnggota kunjunganAnggota = (KunjunganAnggota) session
						.createCriteria(KunjunganAnggota.class).add(
								Restrictions.and(
										Restrictions.and(Restrictions.eq("anggota", anggota.getAttribute("anggota")),
												Restrictions.eq("perpustakaan",
														perpustakaan.getSelectedItem().getValue())),
										Restrictions.eq("tgl", tanggalPersetujuan.getValue())))
						.setMaxResults(1).uniqueResult();
				if (kunjunganAnggota == null || kunjunganAnggota.getId() == null) {
					kunjunganAnggota = new KunjunganAnggota();
					kunjunganAnggota.setKeterangan("Berkunjung karena meminjam buku");
					kunjunganAnggota.setAnggota((Anggota) anggota.getAttribute("anggota"));
					kunjunganAnggota.setPerpustakaan((Perpustakaan) perpustakaan.getSelectedItem().getValue());
					kunjunganAnggota.setTanggal(tanggalPersetujuan.getValue());
					kunjunganAnggota.setTgl(tanggalPersetujuan.getValue());

					session.save(kunjunganAnggota);
				}

				System.out.println("kunjunganAnggota = " + kunjunganAnggota);

				Common.insertCombo(PeminjamanPengadaanItemAction.this.kunjunganAnggota, "anggota", "tanggal",
						KunjunganAnggota.class,
						Restrictions.and(
								Restrictions.and(Restrictions.eq("anggota", anggota.getAttribute("anggota")),
										Restrictions.eq("perpustakaan", perpustakaan.getSelectedItem().getValue())),
								Restrictions.eq("tgl", tanggalPersetujuan.getValue())));

				Common.selectComboItem(PeminjamanPengadaanItemAction.this.kunjunganAnggota,
						peminjamanPengadaanItem.getKunjunganAnggota());
				if (PeminjamanPengadaanItemAction.this.kunjunganAnggota.getSelectedItem() == null
						&& !PeminjamanPengadaanItemAction.this.kunjunganAnggota.getChildren().isEmpty()) {
					PeminjamanPengadaanItemAction.this.kunjunganAnggota.setSelectedIndex(0);
				}

				Common.clear(informasi);
				peminjamanPengadaanItem.setAnggota(kunjunganAnggota.getAnggota());
				peminjamanPengadaanItem.setPerpustakaan(kunjunganAnggota.getPerpustakaan());

				Integer[] kuota = LibraryUtil.getKuota(peminjamanPengadaanItem);
				int jumlahMaksimalPeminjaman = kuota[0];
				int totalPeminjaman = kuota[1];
				int totalKembali = kuota[2];
				// int rowSize = 0;

				informasi.appendChild(new Label("Kuota : " + Common.numberFormat.get().format(jumlahMaksimalPeminjaman)));
				informasi.appendChild(new Label("Total Pinjam : " + Common.numberFormat.get().format(totalPeminjaman)));
				informasi.appendChild(new Label("Total Kembali : " + Common.numberFormat.get().format(totalKembali)));
				informasi.appendChild(
						new Label("Belum Kembali : " + Common.numberFormat.get().format(totalPeminjaman - totalKembali)));
				informasi.appendChild(new Label("Sisa Kuota : "
						+ Common.numberFormat.get().format(jumlahMaksimalPeminjaman - (totalPeminjaman - totalKembali))));

				Common.clear(foto);
				LibraryUtil.gambarAnggota(peminjamanPengadaanItem.getAnggota()).setParent(foto);
				foto.appendChild(new MyLabelBoldAja(peminjamanPengadaanItem.getAnggota().getNama()));

			}
		};

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Anggota"));
		row.appendChild(anggota = new AmbilDataAnggotaBanbox());
		anggota.setAttribute("anggota", peminjamanPengadaanItem.getAnggota());
		anggota.setValue(
				peminjamanPengadaanItem.getAnggota() == null ? "" : peminjamanPengadaanItem.getAnggota().toString());
		anggota.setWidth("90%");
		anggota.setEventListener(kunjunganEventListener);
		tanggalPembuatan.addEventListener("onChange", kunjunganEventListener);
		tanggalPersetujuan.addEventListener("onChange", kunjunganEventListener);

		if (peminjamanPengadaanItem.getAnggota() != null) {
			anggota.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Foto Anggota"));
		row.appendChild(foto = new Vbox());
		foto.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Perpustakaan"));
		row.appendChild(perpustakaan = new Combobox());
		Common.insertCombo(perpustakaan, "nama", Perpustakaan.class);
		Common.selectComboItem(perpustakaan, peminjamanPengadaanItem.getPerpustakaan() == null ? currentPerpustakaan
				: peminjamanPengadaanItem.getPerpustakaan());
		perpustakaan.setDisabled(currentPerpustakaan != null);
		perpustakaan.setWidth("90%");
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Perpustakaan p = Common.getCurrentPerpustakaan();
				if (p != null) {
					Common.selectComboItem(true, perpustakaan, p);
					perpustakaan.setDisabled(true);
				}

				currentPerpustakaan = (Perpustakaan) (perpustakaan.getSelectedItem() == null ? null
						: perpustakaan.getSelectedItem().getValue());
				String mykode = LibraryUtil.generateCode(PeminjamanPengadaanItem.class, 8, "PNJ", currentPerpustakaan);
				kode.setValue(mykode);

				peminjamanPengadaanItemPunyaItemHelper.setPerpustakaan(currentPerpustakaan);
				kunjunganEventListener.onEvent(null);

			}
		};
		perpustakaan.addEventListener("onChange", eventListener);
		perpustakaan.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kunjungan Anggota"));
		row.appendChild(kunjunganAnggota = new Combobox());
		kunjunganAnggota.setWidth("90%");
		kunjunganAnggota.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new MyTextbox(
				peminjamanPengadaanItem.getKeterangan() == null ? "" : peminjamanPengadaanItem.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(4);

		row = new MyFormRow();
		row.setParent(rows);
		row.setVisible(Common.getApakahAdmin());
		row.appendChild(new ais.ui.util.MyLabelConfig("Override Kebijakan"));
		row.appendChild(overrideKebijakan = new MyCheckboxConfig(
				"Izinkan override (alasan minimal 10 karakter wajib di Keterangan)"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Informasi"));
		row.appendChild(informasi = new Vbox());
		informasi.setWidth("90%");

		eventListener.onEvent(null);

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
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Setujui dan Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {

					setujui(PeminjamanPengadaanItemAction.this.peminjamanPengadaanItem);
					addWindow.setVisible(false);
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							kirim(PeminjamanPengadaanItemAction.this.peminjamanPengadaanItem);

							onSearchDefault(null);
						}
					});
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Kode Peminjaman harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (anggota.getAttribute("anggota") == null) {
			MyMessageboxConfig.show("Anggota harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (perpustakaan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Perpustakaan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (kunjunganAnggota.getSelectedItem() == null) {
			MyMessageboxConfig.show("Data Kunjungan Anggota harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (tanggalPembuatan.getValue() == null) {
			MyMessageboxConfig.show("Tanggal Pembuatan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (Common.bolehKonfigurasi("anggota_tidak_boleh_meminjam_lagi_meskipun_peminjaman_sebelumnya_belum_dikembalikan")) {
			Anggota myAnggota = (Anggota) anggota.getAttribute("anggota");
			Perpustakaan myPerpustakaan = (Perpustakaan) perpustakaan.getSelectedItem().getValue();
			Session session = HibernateUtil.currentSession();
			Number count = (Number) session.createCriteria(PeminjamanPengadaanItemDetail.class)
					.add(Restrictions.isNull("kembaliPengadaanItemDetail"))
					.createAlias("peminjamanPengadaanItem", "peminjamanPengadaanItem")
					.add(Restrictions.eq("peminjamanPengadaanItem.perpustakaan", myPerpustakaan))
					.add(Restrictions.eq("peminjamanPengadaanItem.anggota", myAnggota))
					.add(peminjamanPengadaanItem.getId() != null
							? Restrictions.ne("peminjamanPengadaanItem.id", peminjamanPengadaanItem.getId())
							: Restrictions.sqlRestriction("true"))
					.setProjection(Projections.rowCount()).uniqueResult();
			if (count.intValue() > 0) {
				MyMessageboxConfig.show(
						"Anggota dengan kode " + myAnggota.getKode() + " dan nama " + myAnggota.getNama()
								+ " masih ada peminjaman item di perpustakaan " + myPerpustakaan.getNama()
								+ ".\n\nAnggota tersebut harus mengembalikan item yang masih dipinjam.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		List<Row> rowsItem = gridItem.getRows().getChildren();

		int jumlahPeminjamanPengadaanItemDetails = ((Number) HibernateUtil.currentSession()
				.createCriteria(PeminjamanPengadaanItemDetail.class)
				.createAlias("peminjamanPengadaanItem", "peminjamanPengadaanItem")
				.add(Restrictions.eq("peminjamanPengadaanItem.anggota", peminjamanPengadaanItem.getAnggota()))
				.setProjection(Projections.rowCount()).add(Restrictions.isNull("kembaliPengadaanItemDetail"))
				.uniqueResult()).intValue();

		Integer jumlahmaksimal = PeminjamanPengadaanItemAction.this.peminjamanPengadaanItem
				.getJumlahMaksimalPeminjaman();
		System.out.println("jumlahmaksimal = " + jumlahmaksimal);
		if (jumlahmaksimal != null
				&& jumlahmaksimal.intValue() < (jumlahPeminjamanPengadaanItemDetails + rowsItem.size())
				&& !catatOverride("BATAS_JUMLAH_PINJAMAN")) {
			MyMessageboxConfig.show(
					"Jumlah maksimal item yang boleh dipinjam adalah " + jumlahmaksimal
							+ " buah.\nItem yang telah dipinjam : " + jumlahPeminjamanPengadaanItemDetails
							+ " buah, dan item yang akan dimpinjam adalah " + rowsItem.size(),
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		for (Row row : rowsItem) {
			PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail = (PeminjamanPengadaanItemDetail) row
					.getAttribute("peminjamanPengadaanItemDetail");
			if (peminjamanPengadaanItemDetail.getItem() == null) {
				MyMessageboxConfig.show("Item harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		Session session = HibernateUtil.currentSession();
		AnggotaYangDiblokir anggotaYangDiblokir = (AnggotaYangDiblokir) session
				.createCriteria(AnggotaYangDiblokir.class)
				.add(Restrictions.eq("anggota", anggota.getAttribute("anggota")))
				.add(Restrictions.le("mulai", ais.ui.util.WaktuUtil.getDate()))
				.add(Restrictions.or(Restrictions.isNull("perpustakaan"),
						Restrictions.eq("perpustakaan", currentPerpustakaan)))
				.add(Restrictions.or(Restrictions.isNull("sampai"),
						Restrictions.ge("sampai", ais.ui.util.WaktuUtil.getDate())))
				.setMaxResults(1).uniqueResult();
		if (anggotaYangDiblokir != null && !catatOverride("ANGGOTA_DIBLOKIR")) {
			MyMessageboxConfig.show(
					"Kode Anggota \"" + searchkodeangota.getValue().trim() + "\" dan nama \""
							+ anggotaYangDiblokir.getAnggota().getNama() + "\" sedang diblokir mulai "
							+ Common.dateFormat1.get().format(anggotaYangDiblokir.getMulai()) + " "
							+ (anggotaYangDiblokir.getSampai() == null ? ""
									: " sampai " + Common.dateFormat1.get().format(anggotaYangDiblokir.getSampai())),
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			searchkodeangota.focus();
			searchkodeangota.select();
			return false;
		}

		if (peminjamanPengadaanItem.getId() != null) {
			peminjamanPengadaanItem = (PeminjamanPengadaanItem) session.load(PeminjamanPengadaanItem.class,
					peminjamanPengadaanItem.getId());

		}

		System.out.println("mulai menyimpan --> 0");

		currentPerpustakaan = (Perpustakaan) (perpustakaan.getSelectedItem() == null ? null
				: perpustakaan.getSelectedItem().getValue());

		peminjamanPengadaanItem.setAnggota((Anggota) anggota.getAttribute("anggota"));
		peminjamanPengadaanItem.setPerpustakaan((Perpustakaan) perpustakaan.getSelectedItem().getValue());
		peminjamanPengadaanItem.setKode(kode.getValue());
		peminjamanPengadaanItem.setKeterangan(keterangan.getValue());
		peminjamanPengadaanItem.setTanggalPembuatan(tanggalPembuatan.getValue());
		peminjamanPengadaanItem.setTanggalPersetujuan(tanggalPersetujuan.getValue());
		peminjamanPengadaanItem.setKunjunganAnggota((KunjunganAnggota) kunjunganAnggota.getSelectedItem().getValue());

		if (peminjamanPengadaanItem.getId() != null) {
			Common.refreshUpdate(session, peminjamanPengadaanItem);
		} else {
			peminjamanPengadaanItem.setDibuatOleh(Common.getCurrentUser());

			peminjamanPengadaanItem.setIndex(
					LibraryUtil.generateMaxByPerpustakaan(PeminjamanPengadaanItem.class, currentPerpustakaan) + 1);
			String mykode = LibraryUtil.generateCode(PeminjamanPengadaanItem.class, 8, "PNJ", currentPerpustakaan);
			kode.setValue(mykode);
			peminjamanPengadaanItem.setKode(mykode);

			Integer jumlahHariBatas = LibraryUtil.getJumlahHariBatas(peminjamanPengadaanItem.getAnggota(),
					peminjamanPengadaanItem.getPerpustakaan());
			peminjamanPengadaanItem.setJumlahHariBatas(jumlahHariBatas == null ? 0 : jumlahHariBatas.intValue());

			Common.refreshSaveOrUpdate(session, peminjamanPengadaanItem);

		}

		for (Row row : rowsItem) {
			PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail = (PeminjamanPengadaanItemDetail) row
					.getAttribute("peminjamanPengadaanItemDetail");
			peminjamanPengadaanItemDetail.setPeminjamanPengadaanItem(peminjamanPengadaanItem);

			// Pickup reservasi dilakukan di transaksi peminjaman yang sama. Antrean milik
			// anggota lain tidak pernah disentuh; reservasi kedaluwarsa juga tidak dipakai.
			PesananAnggota reservasi = (PesananAnggota) session.createCriteria(PesananAnggota.class)
					.add(Restrictions.eq("anggota", peminjamanPengadaanItem.getAnggota()))
					.add(Restrictions.eq("item", peminjamanPengadaanItemDetail.getItem()))
					.add(Restrictions.eq("perpustakaan", peminjamanPengadaanItem.getPerpustakaan()))
					.add(Restrictions.eq("status", PesananAnggota.PESAN))
					.add(Restrictions.ge("kadaluarsa", ais.ui.util.WaktuUtil.getDate()))
					.addOrder(Order.asc("tanggal")).setMaxResults(1).uniqueResult();
			if (reservasi != null) {
				reservasi.setStatus(PesananAnggota.PINJAM);
				reservasi.setKeterangan("Diambil pada transaksi " + peminjamanPengadaanItem.getKode());
				peminjamanPengadaanItemDetail.setPesananAnggota(reservasi);
				session.update(reservasi);
			}
			session.saveOrUpdate(peminjamanPengadaanItemDetail);
		}

		System.out.println("mulai selesai -->");

		return true;
	}

	/** Override hanya untuk administrator, selalu dengan alasan dan jejak Envers pada transaksi. */
	private boolean catatOverride(String policy) {
		if (!Common.getApakahAdmin() || overrideKebijakan == null || !overrideKebijakan.isChecked()) return false;
		String reason = keterangan == null || keterangan.getValue() == null ? "" : keterangan.getValue().trim();
		if (reason.length() < 10) return false;
		Tbmuser user = Common.getCurrentUser();
		String actor = user == null ? "unknown" : user.getUserId() + "/" + user.getUserNama();
		String marker = "[OVERRIDE " + policy + " oleh " + actor + " pada "
				+ Common.dateFormat51.get().format(ais.ui.util.WaktuUtil.getDate()) + "] ";
		if (reason.indexOf("[OVERRIDE " + policy) < 0) keterangan.setValue(marker + reason);
		return true;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void kirim(final PeminjamanPengadaanItem peminjamanPengadaanItem) throws Exception {

		File myfilebarcode = new File(
				Common.ambilREAL_PATH_REPORT() + "/barcode_" + peminjamanPengadaanItem.getKode() + ".png");
		Barcode mybarcode = BarcodeFactory.createCode128B(peminjamanPengadaanItem.getKode());
		BarcodeImageHandler.savePNG(mybarcode, myfilebarcode);
		String kodeBarcode = myfilebarcode.getAbsolutePath();

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("id", peminjamanPengadaanItem.getId());
		parameters.put("kode_barcode", kodeBarcode);

		parameters.put("info", LibraryUtil.tampilanSummaryPeminjaman(null, peminjamanPengadaanItem));

		final File file = Report.generatePDFReport(Report.PDF, parameters, "library/peminjaman",
				peminjamanPengadaanItem.getTanggalPembuatan(), null, Common.locale, null);

		Anggota anggota = peminjamanPengadaanItem.getAnggota();

		Siswa siswa = anggota.getSiswa();
		Mahasiswa mahasiswa = anggota.getMahasiswa();
		Tbmuser tbmuser = anggota.getTbmuser();

		JSONArray userIds = new JSONArray();
		if (siswa != null && !siswa.getNomorIndukNasional().isEmpty()) {
			userIds.put(siswa.getNomorIndukNasional());
		} else if (mahasiswa != null && mahasiswa.getNim() != null) {
			userIds.put(mahasiswa.getNim());
		} else if (tbmuser != null && tbmuser.getUserId() != null) {
			userIds.put(tbmuser.getUserId());
		}

		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		Sekolah sekolahD = SekolahUtil.getSekolah();

		final String sekolah = siswa != null ? siswa.getSekolah().getNama()
				: mahasiswa != null && mahasiswa.getJurusan() != null && mahasiswa.getJurusan().getFakultas() != null
						&& mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() != null
								? mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getNama()
								: anggota.getDosen() != null && anggota.getDosen().getPerguruanTinggi() != null
										? anggota.getDosen().getPerguruanTinggi().getNama()
										: anggota.getGuru() != null && anggota.getGuru().getSekolah() != null
												? anggota.getGuru().getSekolah().getNama()
												: sekolahD != null ? sekolahD.getNama()
														: perguruanTinggi != null ? perguruanTinggi.getNama() : "";

		final String nama = (siswa != null ? siswa.getNama()
				: mahasiswa != null ? mahasiswa.getNama()
						: anggota.getDosen() != null ? anggota.getDosen().getNama()
								: anggota.getGuru() != null ? anggota.getGuru().getNama()
										: anggota.getPegawai() != null ? anggota.getPegawai().getNama()
												: anggota.getTbmuser() != null ? anggota.getTbmuser().getUserNama()
														: "");

		String info = "Atas nama: " + nama;
		String subject = "Informasi Peminjaman Berhasil => " + info;

		String body = "**Yth. " + nama + ",**\r\n" + "\r\n" + "Dengan hormat,\r\n" + "\r\n"
				+ "Kami memberitahukan bahwa peminjaman buku di \""
				+ peminjamanPengadaanItem.getPerpustakaan().getNama() + "\" di " + sekolah
				+ " telah berhasil dilakukan pada "
				+ Common.dateFormat61.get().format(peminjamanPengadaanItem.getTanggalPembuatan()) + ".\r\n" + "\r\n"
				+ "Berikut adalah rincian peminjaman Anda:\r\n" + "\r\n" + "*   **Nama Peminjam:** " + nama + "\r\n"
				+ "*   **Perpustakaan:** " + peminjamanPengadaanItem.getPerpustakaan().getNama() + "\r\n"
				+ "*   **Tanggal Peminjaman:** "
				+ Common.dateFormat51.get().format(peminjamanPengadaanItem.getTanggalPembuatan()) + "\r\n" + "\r\n"
				+ "Mohon untuk menjaga dan merawat buku yang dipinjam, serta mengembalikan buku sesuai dengan tenggat waktu yang telah ditentukan. Detail mengenai buku yang Anda pinjam beserta tenggat waktu pengembalian dapat Anda lihat pada sistem/portal perpustakaan atau pada file terlampir.\r\n"
				+ "\r\n" + "Jika Anda memiliki pertanyaan lebih lanjut, jangan ragu untuk menghubungi kami.\r\n"
				+ "\r\n" + "Terima kasih atas partisipasi Anda dalam memanfaatkan layanan Perpustakaan "
				+ peminjamanPengadaanItem.getPerpustakaan().getNama() + ".";

		String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();

		String emailUser = "";
		if (siswa != null && siswa.getAlamatEmail() != null && Common.isValidEmailAddress(siswa.getAlamatEmail())) {
			emailUser += emailUser.trim().isEmpty() ? siswa.getAlamatEmail().trim()
					: "," + siswa.getAlamatEmail().trim();
		}
		if (mahasiswa != null && mahasiswa.getEmail() != null && Common.isValidEmailAddress(mahasiswa.getEmail())) {
			emailUser += emailUser.trim().isEmpty() ? mahasiswa.getEmail().trim() : "," + mahasiswa.getEmail().trim();
		}
		if (anggota.getDosen() != null && anggota.getDosen().getEmail() != null
				&& Common.isValidEmailAddress(anggota.getDosen().getEmail())) {
			emailUser += emailUser.trim().isEmpty() ? anggota.getDosen().getEmail().trim()
					: "," + anggota.getDosen().getEmail().trim();
		}
		if (anggota.getGuru() != null && anggota.getGuru().getAlamatEmail() != null
				&& Common.isValidEmailAddress(anggota.getGuru().getAlamatEmail())) {
			emailUser += emailUser.trim().isEmpty() ? anggota.getGuru().getAlamatEmail().trim()
					: "," + anggota.getGuru().getAlamatEmail().trim();
		}
		if (anggota.getPegawai() != null && anggota.getPegawai().getEmail() != null
				&& Common.isValidEmailAddress(anggota.getPegawai().getEmail())) {
			emailUser += emailUser.trim().isEmpty() ? anggota.getPegawai().getEmail().trim()
					: "," + anggota.getPegawai().getEmail().trim();
		}
		if (anggota.getTbmuser() != null && anggota.getTbmuser().getEmail() != null
				&& Common.isValidEmailAddress(anggota.getTbmuser().getEmail())) {
			emailUser += emailUser.trim().isEmpty() ? anggota.getTbmuser().getEmail().trim()
					: "," + anggota.getTbmuser().getEmail().trim();
		}
		JSONArray attachmentsData = null;

		MailSender.sendMailLampiran(userIds, subject, body, sender, emailUser, peminjamanPengadaanItem, attachmentsData,
				false, file);

		if (Common.bolehKonfigurasi("aktifkan_kirim_notif_pinjam_buku_perpustakaan_ke_wa")) {
			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					String dawal = Common.getKonfigurasi("pesan_tambahan_notif_awal",
							"*Pesan ini dibuat secara otomatis oleh sistem sebagai notifikasi/pemberitahuan kepada Anda*\n\n")
							.getNilai();

					Set<String> forms = new HashSet<String>();
					forms.add(peminjamanPengadaanItem.getAnggota().getTelp());
					forms.add(peminjamanPengadaanItem.getAnggota().getHp());

					if (peminjamanPengadaanItem.getAnggota().getSiswa() != null) {
						forms.addAll(peminjamanPengadaanItem.getAnggota().getSiswa().ambilTelp());
					}

					for (String from : forms) {

						if (from != null && !from.trim().isEmpty()
								&& !(from == null || from.toString().trim().isEmpty()
										|| from.toString().trim().equals("00000000000000000000")
										|| from.toString().trim().equals("000000000"))) {

							String body = "**Yth. " + nama + ",**\r\n" + "\r\n" + "Dengan hormat,\r\n" + "\r\n"
									+ "Kami memberitahukan bahwa peminjaman buku di \""
									+ peminjamanPengadaanItem.getPerpustakaan().getNama() + "\" di " + sekolah
									+ " telah berhasil dilakukan pada "
									+ Common.dateFormat61.get().format(peminjamanPengadaanItem.getTanggalPembuatan())
									+ ".\r\n" + "\r\n" + "Berikut adalah rincian peminjaman Anda:\r\n" + "\r\n"
									+ "*   **Nama Peminjam:** " + nama + "\r\n" + "*   **Perpustakaan:** "
									+ peminjamanPengadaanItem.getPerpustakaan().getNama() + "\r\n"
									+ "*   **Tanggal Peminjaman:** "
									+ Common.dateFormat51.get().format(peminjamanPengadaanItem.getTanggalPembuatan()) + "\r\n"
									+ "\r\n"
									+ "Mohon untuk menjaga dan merawat buku yang dipinjam, serta mengembalikan buku sesuai dengan tenggat waktu yang telah ditentukan. Detail mengenai buku yang Anda pinjam beserta tenggat waktu pengembalian dapat Anda lihat pada sistem/portal perpustakaan atau pada file terlampir.\r\n"
									+ "\r\n"
									+ "Jika Anda memiliki pertanyaan lebih lanjut, jangan ragu untuk menghubungi kami.\r\n"
									+ "\r\n"
									+ "Terima kasih atas partisipasi Anda dalam memanfaatkan layanan Perpustakaan "
									+ peminjamanPengadaanItem.getPerpustakaan().getNama() + ".";

							String urlD = Common.getRequestHostWithProtocolSimple()
									+ file.getAbsolutePath().split("webapps")[1];

							Wa.kirimWaViaUltramsg(from, dawal + body, "Bukti_Peminjaman.pdf", urlD);
						}
					}
				}
			}, "", false, 2000);
		}
	}

	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PeminjamanPengadaanItem.class);

		boolean ada = !searchbarkode.getValue().trim().isEmpty() || !searchjudul.getValue().trim().isEmpty()
				|| !searchisbn.getValue().trim().isEmpty() || searchBelumDikembalikan.isChecked();

		if (ada) {
			List<Long> ids = session.createCriteria(PeminjamanPengadaanItemDetail.class)

					.createAlias("itemPunyaBarcode", "itemPunyaBarcode", Criteria.LEFT_JOIN)
					.createAlias("item", "item", Criteria.LEFT_JOIN)

					.add(searchisbn.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(
									Restrictions.ilike("item.isbn", searchisbn.getValue().trim(), MatchMode.ANYWHERE),
									Restrictions.ilike("item.isbn10", searchisbn.getValue().trim(),
											MatchMode.ANYWHERE)))

					.add(searchjudul.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("item.nama", searchjudul.getValue().trim(), MatchMode.ANYWHERE))

					.add(searchbarkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("itemPunyaBarcode.barcode", searchbarkode.getValue().trim(),
									MatchMode.EXACT))

					.add(searchBelumDikembalikan.isChecked() ? Restrictions.isNull("kembaliPengadaanItemDetail")
							: Restrictions.sqlRestriction("true"))

					.setMaxResults(32760)

					.setProjection(Projections.groupProperty("peminjamanPengadaanItem.id")).list();

			if (!ids.isEmpty()) {
				criteria.add(Restrictions.in("id", ids));
			}
		}

		criteria.add((searchperpustakaan == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchperpustakaan.getAttribute("perpustakaan") == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("perpustakaan", searchperpustakaan.getAttribute("perpustakaan"))))

				.add((searchanggota == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchanggota.getAttribute("anggota") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("anggota", searchanggota.getAttribute("anggota"))))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE))

				.add((searchmulai == null || searchsampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.tanggal_pembuatan) between date('"
						+ Common.databaseDateFormat.get().format(searchmulai.getValue()) + "') and date('"
						+ Common.databaseDateFormat.get().format(searchsampai.getValue()) + "')")));
		if (order)
			criteria.addOrder(Order.desc("tanggalPembuatan"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<PeminjamanPengadaanItem> peminjamanPengadaanItem = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(peminjamanPengadaanItem);
		grid.setRowRenderer(new PeminjamanPengadaanItemRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void onKodeAnggota(Event event) throws Exception {
		if (searchkodeangota.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Kode Anggota harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		Session session = HibernateUtil.currentSession();
		Anggota anggota = (Anggota) session.createCriteria(Anggota.class)
				.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.createAlias("dosen", "dosen", Criteria.LEFT_JOIN)
				.add(Restrictions.or(
						Restrictions.or(Restrictions.ilike("kode", searchkodeangota.getValue().trim(), MatchMode.EXACT),
								Restrictions.ilike("mahasiswa.nim", searchkodeangota.getValue().trim(),
										MatchMode.EXACT)),
						Restrictions.ilike("dosen.mycode", searchkodeangota.getValue().trim(), MatchMode.EXACT)

				)).setMaxResults(1).uniqueResult();
		if (anggota == null) {
			MyMessageboxConfig.show("Kode Anggota \"" + searchkodeangota.getValue().trim() + "\" tidak ditemukan",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			searchkodeangota.focus();
			searchkodeangota.select();
			return;
		}

		if (!anggota.getAktif()) {
			MyMessageboxConfig.show(
					"Anggota perpustakaan ini tidak aktif, sehingga tidak diizinkan untuk meminjam buku di perpustakaan",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			searchkodeangota.focus();
			searchkodeangota.select();
			return;
		}

		if (anggota.getMahasiswa() != null && Common.bolehKonfigurasi("mahasiswa_dengan_status_tidak_aktif_tidak_diizinkan_meminjam_buku_perpustakaan")) {

			KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(anggota.getMahasiswa(),
					anggota.getMahasiswa().currentSemester(), anggota.getMahasiswa().currentTahapan(), null);

			HistoryStatusMahasiswa tempHistoryStatusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.getHistoryStatusMahasiswa(krsMahasiswa, true);
			String kodeStatus = tempHistoryStatusMahasiswa == null
					|| tempHistoryStatusMahasiswa.getStatusMahasiswa() == null ? ""
							: tempHistoryStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed();
			if (!kodeStatus.equalsIgnoreCase("A")) {
				MyMessageboxConfig.show(
						"Status mahasiswa ini tidak aktif, sehingga tidak diizinkan untuk meminjam buku di perpustakaan",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				searchkodeangota.focus();
				searchkodeangota.select();
				return;
			}
		}

		AnggotaYangDiblokir anggotaYangDiblokir = (AnggotaYangDiblokir) session
				.createCriteria(AnggotaYangDiblokir.class).add(Restrictions.eq("anggota", anggota))
				.add(Restrictions.le("mulai", ais.ui.util.WaktuUtil.getDate()))
				.add(currentPerpustakaan == null || currentPerpustakaan.getId() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.isNull("perpustakaan"),
								Restrictions.eq("perpustakaan", currentPerpustakaan)))
				.add(Restrictions.or(Restrictions.isNull("sampai"),
						Restrictions.ge("sampai", ais.ui.util.WaktuUtil.getDate())))
				.setMaxResults(1).uniqueResult();
		if (anggotaYangDiblokir != null) {
			MyMessageboxConfig.show(
					"Kode Anggota \"" + searchkodeangota.getValue().trim() + "\" dan nama \""
							+ anggotaYangDiblokir.getAnggota().getNama() + "\" sedang diblokir mulai "
							+ Common.dateFormat1.get().format(anggotaYangDiblokir.getMulai()) + " "
							+ (anggotaYangDiblokir.getSampai() == null ? ""
									: " sampai " + Common.dateFormat1.get().format(anggotaYangDiblokir.getSampai())),
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			searchkodeangota.focus();
			searchkodeangota.select();
			return;
		}

		searchkodeangota.setValue("");

		PeminjamanPengadaanItem peminjamanPengadaanItem = new PeminjamanPengadaanItem();
		peminjamanPengadaanItem.setAnggota(anggota);
		peminjamanPengadaanItem.setPerpustakaan(currentPerpustakaan);

		int jumlahPeminjamanPengadaanItemDetails = ((Number) HibernateUtil.currentSession()
				.createCriteria(PeminjamanPengadaanItemDetail.class)
				.createAlias("peminjamanPengadaanItem", "peminjamanPengadaanItem")
				.add(Restrictions.eq("peminjamanPengadaanItem.anggota", peminjamanPengadaanItem.getAnggota()))
				.setProjection(Projections.rowCount()).add(Restrictions.isNull("kembaliPengadaanItemDetail"))
				.uniqueResult()).intValue();

		Integer jumlahmaksimal = LibraryUtil.getJumlahMaksimalPeminjaman(peminjamanPengadaanItem);
		System.out.println("jumlahmaksimal = " + jumlahmaksimal);
		if (jumlahmaksimal != null && jumlahmaksimal.intValue() <= (jumlahPeminjamanPengadaanItemDetails)) {
			MyMessageboxConfig.show(
					"Jumlah maksimal item yang boleh dipinjam adalah " + jumlahmaksimal
							+ " buah.\nItem yang telah dipinjam : " + jumlahPeminjamanPengadaanItemDetails,
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		init(peminjamanPengadaanItem);
		addWindow.setVisible(true);
		addWindow.onModal();
	}
}
