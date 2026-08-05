package ais.action.master.library;

import java.io.File;
import java.io.FileInputStream;
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
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.East;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
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
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.library.helper.AmbilDataAnggotaBanbox;
import ais.action.master.library.helper.AmbilDataPerpustakaanBanbox;
import ais.action.master.library.helper.KembaliPengadaanItemDetailAction;
import ais.action.master.library.helper.KembaliPengadaanItemPunyaItemHelper;
import ais.action.master.library.util.LibraryUtil;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.report.Report;
import ais.action.report.format1.library.LaporanPengembalianPerAnggota;
import ais.action.servlet.Wa;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.library.KembaliPengadaanItemDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.library.Anggota;
import ais.database.model.library.DendaKeterlambatanItem;
import ais.database.model.library.DetailTransaksi;
import ais.database.model.library.KembaliPengadaanItem;
import ais.database.model.library.KembaliPengadaanItemDetail;
import ais.database.model.library.KunjunganAnggota;
import ais.database.model.library.PeminjamanPengadaanItem;
import ais.database.model.library.PeminjamanPengadaanItemDetail;
import ais.database.model.library.Perpustakaan;
import ais.database.model.library.PesananAnggota;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.delivery.email.sender.MailSender;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
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
 * <h2>KembaliPengadaanItemAction — Layar Pengembalian Item/Buku Perpustakaan</h2>
 *
 * <p><b>Untuk apa kelas ini:</b><br>
 * Kelas ini menjalankan halaman <code>kembali_pengadaan_item.zul</code> (Stasiun Scan
 * berwarna hijau/teal). Tugasnya adalah memproses <i>pengembalian</i> item yang tadinya
 * dipinjam: petugas men-scan barcode item yang dikembalikan (atau barcode anggota),
 * sistem mencocokkannya dengan transaksi peminjaman, menghitung keterlambatan &amp; denda,
 * menandai item sebagai sudah kembali, menerima pembayaran denda, lalu menampilkan &amp;
 * menyaring daftar pengembalian pada tabel. Sama seperti kelas peminjaman, ini adalah
 * <code>GenericAutowireComposer</code> ZK — komponen ZUL ber-<code>id</code> otomatis
 * di-<i>wire</i> ke field, dan <code>forward="onClick=onXxx"</code> dipetakan ke method
 * <code>onXxx(Event)</code>.</p>
 *
 * <p><b>Tanggung jawab utama (bahasa sederhana):</b></p>
 * <ol>
 *   <li><b>Scan pengembalian</b> — <code>onKodeAnggota</code> menerima hasil scan pada
 *       kotak besar: barcode item yang dikembalikan langsung dicocokkan dengan
 *       peminjamannya, atau barcode anggota untuk menampilkan seluruh pinjaman anggota
 *       yang belum kembali. Tekan Enter, selesai.</li>
 *   <li><b>Pencarian &amp; filter</b> — <code>onSearchDefault</code> menyaring daftar
 *       pengembalian berdasarkan nomor, perpustakaan, anggota, rentang tanggal, barcode,
 *       judul, ISBN, serta penanda "belum bayar" dan "belum lunas".</li>
 *   <li><b>Hitung denda &amp; pelunasan</b> — memakai aturan denda terpusat
 *       (<code>LibraryUtil</code>) sehingga konsisten dengan layar peminjaman, lalu
 *       mencatat berapa yang sudah dibayar dan sisa yang belum lunas.</li>
 *   <li><b>Tambah / ubah</b> — <code>onAdd</code> membuka form detail pengembalian.</li>
 * </ol>
 *
 * <p><b>Penanganan sesi Hibernate:</b><br>
 * Kelas ini <u>hanya</u> memakai <code>HibernateUtil.currentSession()</code> (sesi yang
 * dikelola per-request). Sesuai aturan, sesi jenis ini <u>TIDAK ditutup</u> secara manual
 * karena kerangka kerja sudah menutupnya otomatis di akhir request; menutupnya sendiri
 * akan memunculkan "Session is closed!". Tidak ada <code>openSession()</code> maupun
 * <code>currentNativeSession()</code> pada kelas ini, sehingga tidak ada sesi yang perlu
 * ditutup di blok <code>finally</code> — berbeda dengan
 * {@link PeminjamanPengadaanItemAction} yang memakai sesi native di thread latar.</p>
 *
 * <p><b>Prinsip pemeliharaan &amp; reuse:</b> logika bisnis pengembalian (kecocokan item,
 * perhitungan hari terlambat, denda, status lunas) dijaga terpusat di
 * <code>LibraryUtil</code> dan model <code>KembaliPengadaanItem</code>/
 * <code>KembaliPengadaanItemDetail</code> agar layar peminjaman dan pengembalian memakai
 * satu sumber kebenaran yang sama. Menambah filter baru cukup dengan menambah field
 * ber-<code>id</code> yang sama di sini lalu menyesuaikan <code>onSearchDefault</code>.
 * Kode dijaga kompatibel Java 1.7 (tanpa lambda / <i>try-with-resources</i>) dengan blok
 * <code>try/catch</code> gaya Java 1.6 (tanpa <i>multi-catch</i>).</p>
 *
 * @see PeminjamanPengadaanItemAction Layar peminjaman item (pasangan kelas ini).
 * @see ais.action.master.library.util.LibraryUtil Pusat aturan denda &amp; util perpustakaan.
 */
public class KembaliPengadaanItemAction extends GenericAutowireComposer {

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
	private AmbilDataPerpustakaanBanbox searchperpustakaan;
	private AmbilDataAnggotaBanbox searchanggota;
	private MyCheckboxConfig searchBelumBayar;
	private MyCheckboxConfig searchBelumLunas;

	private Textbox searchkodeangota;
	private MyDatebox searchmulai;
	private MyDatebox searchsampai;

	private MyTextbox kode;
	private MyTextbox keterangan;
	private MyDatebox tanggalPembuatan;
	// private AmbilDataPeminjamanPengadaanItemBanbox peminjamanPengadaanItem;

	private boolean edit = false;
	private boolean delete = false;
	// private boolean approve = false;
	// private boolean reject = false;

	private Perpustakaan currentPerpustakaan;

	private KembaliPengadaanItem kembaliPengadaanItem;
	private MyToolbarbuttonConfig add;
	private MyGrid gridItem;
	private KembaliPengadaanItemPunyaItemHelper kembaliPengadaanItemPunyaItemHelper;

	private Combobox kunjunganAnggota;
	private PeminjamanPengadaanItem peminjamanPengadaanItem;

	private String barcodeItem = null;
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

		if (add != null) { add.setVisible(false); }
		if (add != null) { add.setTooltiptext("Tambah"); }

		if (searchmulai != null) { searchmulai.setReadonly(true); }
		if (searchsampai != null) { searchsampai.setReadonly(true); }

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 7);
		if (searchmulai != null) { searchmulai.setValue(calendar.getTime()); }
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (searchsampai != null) { searchsampai.setValue(calendar.getTime()); }

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

		String[] contents = new String[] { "id", "kembaliPengadaanItem", "item", "itemPunyaBarcode", "tanggal", "denda",
				"dibayarSejumlah", "telahDibayar", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(KembaliPengadaanItemDetail.class,
				new DataCriteria() {

					@Override
					public Criteria initCriteria(boolean order) {
						return KembaliPengadaanItemAction.this.createriaDetail(order);
					}
				}, "Download Data", "/img/print.png", contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

	        FilterLanjutHelper.setup(comp);
}

	private Criteria createriaDetail(Boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KembaliPengadaanItemDetail.class)
				.add(searchBelumBayar.isChecked() ? Restrictions.eq("telahDibayar", false)
						: Restrictions.sqlRestriction("true"))
				.add(searchBelumLunas.isChecked() ? Restrictions.sqlRestriction("this_.denda>this_.dibayarsejumlah")
						: Restrictions.sqlRestriction("true"))
				.createCriteria("kembaliPengadaanItem");

		criteria.createAlias("peminjamanPengadaanItem", "peminjamanPengadaanItem")
				.add(searchperpustakaan.getAttribute("perpustakaan") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("perpustakaan", searchperpustakaan.getAttribute("perpustakaan")))

				.add(searchanggota.getAttribute("anggota") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("peminjamanPengadaanItem.anggota", searchanggota.getAttribute("anggota")))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchmulai.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.ge("tanggalPembuatan", searchmulai.getValue()))
				.add(searchsampai.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.le("tanggalPembuatan", searchsampai.getValue()));

		if (order)
			criteria.addOrder(Order.desc("tanggalPembuatan"));

		return criteria;
	}

	public void onCetakPengembalianPerAnggota(Event event) throws Exception {
		LaporanPengembalianPerAnggota laporan = new LaporanPengembalianPerAnggota();
		laporan.setTitle("Cetak Laporan");
		page.getFirstRoot().appendChild(laporan);
		laporan.setHeight("95%");
		laporan.setWidth("90%");
		laporan.setClosable(true);
		laporan.onModal();
	}

	@SuppressWarnings("unchecked")
	private void setujui(final KembaliPengadaanItem kembaliPengadaanItem) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Session session = HibernateUtil.currentSession();
				session.refresh(kembaliPengadaanItem);

				kembaliPengadaanItem.setDisetujuiOleh(Common.getCurrentUser());
				// kembaliPengadaanItem.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());

				if (tanggalPersetujuan != null) {
					kembaliPengadaanItem.setTanggalPersetujuan(tanggalPersetujuan.getValue());
				} else {
					kembaliPengadaanItem.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
				}

				Common.refreshUpdate(session, kembaliPengadaanItem);

				List<PeminjamanPengadaanItem> peminjamanPengadaanItems = session
						.createCriteria(KembaliPengadaanItemDetail.class)
						.createAlias("peminjamanPengadaanItemDetail", "peminjamanPengadaanItemDetail")
						.setProjection(
								Projections.groupProperty("peminjamanPengadaanItemDetail.peminjamanPengadaanItem"))
						.add(Restrictions.eq("kembaliPengadaanItem", kembaliPengadaanItem)).list();
				for (PeminjamanPengadaanItem peminjamanPengadaanItem : peminjamanPengadaanItems) {
					peminjamanPengadaanItem.setKembaliPengadaanItem(kembaliPengadaanItem);
					Common.refreshUpdate(session, peminjamanPengadaanItem);
				}

				List<KembaliPengadaanItemDetail> kembaliPengadaanItemDetails = session
						.createCriteria(KembaliPengadaanItemDetail.class)
						.add(Restrictions.eq("kembaliPengadaanItem", kembaliPengadaanItem)).list();

				session.createSQLQuery(
						"delete from library.detail_transaksi where kembali_pengadaan_item_detail in (select id from library.kembali_pengadaan_item_detail where kembali_pengadaan_item = "
								+ kembaliPengadaanItem.getId() + ");")
						.executeUpdate();
				for (KembaliPengadaanItemDetail kembaliPengadaanItemDetail : kembaliPengadaanItemDetails) {

					if (kembaliPengadaanItemDetail.getPeminjamanPengadaanItemDetail() != null
							&& kembaliPengadaanItemDetail.getPeminjamanPengadaanItemDetail()
									.getPesananAnggota() != null) {

						PesananAnggota pesananAnggota = kembaliPengadaanItemDetail.getPeminjamanPengadaanItemDetail()
								.getPesananAnggota();
						session.refresh(pesananAnggota);
						pesananAnggota.setStatus(PesananAnggota.DIKEMBALIKAN);
						Common.refreshUpdate(session, pesananAnggota);
					}

					DetailTransaksi detailTransaksi = new DetailTransaksi();
					detailTransaksi.setAnggota(kembaliPengadaanItem.getPeminjamanPengadaanItem().getAnggota());
					detailTransaksi.setKembaliPengadaanItemDetail(kembaliPengadaanItemDetail);
					detailTransaksi.setQtyBonus(0.0);

					detailTransaksi.setItem(kembaliPengadaanItemDetail.getItem());
					detailTransaksi.setKeterangan("Transaksi Kembali");
					detailTransaksi.setKodeTransaksi(LibraryUtil.PENGEMBALIAN_MASUK);
					detailTransaksi
							.setPerpustakaan(kembaliPengadaanItem.getPeminjamanPengadaanItem().getPerpustakaan());
					detailTransaksi.setQty(kembaliPengadaanItemDetail.getDikembali());
					detailTransaksi.setTanggal(kembaliPengadaanItem.getTanggalPersetujuan());
					detailTransaksi.setTanggalDanWaktu(kembaliPengadaanItem.getTanggalPersetujuan());

					session.save(detailTransaksi);
				}

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onSearchDefault(arg0);
					}
				});
			}
		});

	}

	class KembaliPengadaanItemRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KembaliPengadaanItem kembaliPengadaanItem = (KembaliPengadaanItem) arg1;
			PeminjamanPengadaanItem peminjamanPengadaanItem = kembaliPengadaanItem.getPeminjamanPengadaanItem();

			(new KembaliPengadaanItemDetailAction(kembaliPengadaanItem)).setParent(arg0);

			LibraryUtil.gambarAnggota(kembaliPengadaanItem.getPeminjamanPengadaanItem().getAnggota()).setParent(arg0);

			RevisiHelper
					.createNewRevisi(KembaliPengadaanItem.class, kembaliPengadaanItem, kembaliPengadaanItem.getKode())
					.setParent(arg0);

			new Label(kembaliPengadaanItem.getPeminjamanPengadaanItem().getPerpustakaan() == null ? ""
					: kembaliPengadaanItem.getPeminjamanPengadaanItem().getPerpustakaan().getNama()).setParent(arg0);

			new Label(kembaliPengadaanItem.getPeminjamanPengadaanItem().getAnggota() == null ? ""
					: kembaliPengadaanItem.getPeminjamanPengadaanItem().getAnggota().toString()).setParent(arg0);

			final Html htmldenda = new ais.ui.util.MyHtml();
			htmldenda.setParent(arg0);

			new Label(kembaliPengadaanItem.getDibuatOleh() == null ? ""
					: kembaliPengadaanItem.getDibuatOleh().getUserNama()).setParent(arg0);
			new Label(kembaliPengadaanItem.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(kembaliPengadaanItem.getTanggalPembuatan())).setParent(arg0);

			(new Label(kembaliPengadaanItem.getDisetujuiOleh() == null ? ""
					: kembaliPengadaanItem.getDisetujuiOleh().getUserNama())).setParent(arg0);

			(new Label(kembaliPengadaanItem.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(kembaliPengadaanItem.getTanggalPersetujuan()))).setParent(arg0);
			new Label(kembaliPengadaanItem.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Kembali Item");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				public void onEvent(Event event) throws Exception {

					File myfilebarcode = new File(
							application.getRealPath("/report") + "/barcode_" + kembaliPengadaanItem.getKode() + ".png");
					Barcode mybarcode = BarcodeFactory.createCode128B(kembaliPengadaanItem.getKode());
					BarcodeImageHandler.savePNG(mybarcode, myfilebarcode);
					String kodeBarcode = myfilebarcode.getAbsolutePath();

					Map parameters = ais.common.HashMapGenerator.getRand();
					parameters.put("id", kembaliPengadaanItem.getId());
					parameters.put("kode_barcode", kodeBarcode);

					parameters.put("info", LibraryUtil.tampilanSummaryPeminjaman(kembaliPengadaanItem,
							kembaliPengadaanItem.getPeminjamanPengadaanItem()));

					Report.generatePDFReport(Report.PDF, parameters, "library/pengembalian",
							kembaliPengadaanItem.getTanggalPembuatan());
				}

			});
			button.setParent(toolbar);

			// final MyToolbarbuttonConfig disetujui = new
			// MyToolbarbuttonConfig("",
			// "/img/svg/check2.svg");
			//
			// final MyToolbarbuttonConfig dibatalkan = new
			// MyToolbarbuttonConfig("",
			// "/img/svg/warning-outline.svg");
			final MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			final MyToolbarbuttonConfig rubah = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");

			rubah.setTooltiptext("Ubah Data");
			rubah.setVisible(edit);
			rubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(kembaliPengadaanItem);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			rubah.setParent(toolbar);

			hapus.setTooltiptext("Hapus Data");
			hapus.setVisible(delete);
			hapus.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											KembaliPengadaanItemDao kembaliPengadaanItemDao = DaoFactory.getInstance()
													.getKembaliPengadaanItemDao();

											Session session = kembaliPengadaanItemDao.getCurrentSession();
											List<KembaliPengadaanItemDetail> kembaliPengadaanItemDetails = session
													.createCriteria(KembaliPengadaanItemDetail.class)
													.add(Restrictions.eq("kembaliPengadaanItem", kembaliPengadaanItem))
													.list();
											for (KembaliPengadaanItemDetail kembaliPengadaanItemDetail : kembaliPengadaanItemDetails) {
												session.delete(kembaliPengadaanItemDetail);
											}

											Common.refreshDelete(kembaliPengadaanItem);

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
			hapus.setParent(toolbar);
			toolbar.setParent(arg0);

			PeminjamanPengadaanItem p = peminjamanPengadaanItem;
			String content = LibraryUtil.tampilanSummaryPeminjaman(kembaliPengadaanItem, p);
			htmldenda.setContent(content);
		}
	}

	public void onAdd(Event event) throws Exception {
		init(new KembaliPengadaanItem());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	protected void initDetail(final KembaliPengadaanItem kembaliPengadaanItem, Component component) throws Exception {
		Tabbox tabbox = new Tabbox();
		tabbox.setParent(component);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabDipinjam = new MyTabConfig("Item yang dikembalikan");
		tabDipinjam.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		final Tabpanel tabpanelDipinjam = new ais.ui.util.MyTabpanel();
		tabpanelDipinjam.setParent(tabpanels);
		tabpanelDipinjam.setWidth("100%");

		tabpanelDipinjam.appendChild(
				(kembaliPengadaanItemPunyaItemHelper = new KembaliPengadaanItemPunyaItemHelper(gridItem = new MyGrid()))
						.initDetail(kembaliPengadaanItem, barcodeItem));

	}

	private void init(final KembaliPengadaanItem kembaliPengadaanItem) throws Exception {
		this.kembaliPengadaanItem = kembaliPengadaanItem;
		this.peminjamanPengadaanItem = kembaliPengadaanItem.getPeminjamanPengadaanItem();
		addWindow.setTitle(kembaliPengadaanItem.getId() == null ? "Tambah Kembali Item" : "Ubah Kembali Item");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		East east = new East();
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setWidth("75%");

		initDetail(kembaliPengadaanItem, east);

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Pengembalian *"));
		String mykode = kembaliPengadaanItem.getKode();

		row.appendChild(
				kode = new MyTextbox(kembaliPengadaanItem.getKode() == null ? mykode : kembaliPengadaanItem.getKode()));
		kode.setWidth("90%");
		kode.setDisabled(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pengembalian *"));
		row.appendChild(tanggalPembuatan = new MyDatebox(
				kembaliPengadaanItem.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
						: kembaliPengadaanItem.getTanggalPembuatan()));
		tanggalPembuatan.setFormat(Common.dateFormat.get().toPattern());

		tanggalPembuatan.setWidth("90%");
		tanggalPembuatan.setReadonly(true);

		
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Persetujuan *"));
		row.appendChild(tanggalPersetujuan = new MyDatebox(
				kembaliPengadaanItem.getTanggalPersetujuan() == null ? ais.ui.util.WaktuUtil.getDate()
						: kembaliPengadaanItem.getTanggalPersetujuan()));
		tanggalPersetujuan.setFormat(Common.dateFormat.get().toPattern());

		tanggalPersetujuan.setWidth("90%");
		tanggalPersetujuan.setReadonly(true); 
		
		final EventListener kunjunganEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					Common.clear(kunjunganAnggota);

					// PeminjamanPengadaanItem mypeminjamanPengadaanItem =
					// (PeminjamanPengadaanItem) peminjamanPengadaanItem
					// .getAttribute("peminjamanPengadaanItem");

					PeminjamanPengadaanItem mypeminjamanPengadaanItem = peminjamanPengadaanItem;

					if (mypeminjamanPengadaanItem == null || mypeminjamanPengadaanItem.getAnggota() == null) {
						return;
					}
					if (mypeminjamanPengadaanItem == null || mypeminjamanPengadaanItem.getPerpustakaan() == null) {
						return;
					}

					Session session = HibernateUtil.currentSession();
					session.refresh(mypeminjamanPengadaanItem);

					KunjunganAnggota kunjunganAnggota = (KunjunganAnggota) session
							.createCriteria(KunjunganAnggota.class)
							.add(Restrictions.eq("anggota", mypeminjamanPengadaanItem.getAnggota()))
							.add(Restrictions.eq("perpustakaan", mypeminjamanPengadaanItem.getPerpustakaan()))
							.add(Restrictions.eq("tgl", tanggalPembuatan.getValue())).setMaxResults(1).uniqueResult();
					if (kunjunganAnggota == null) {
						kunjunganAnggota = new KunjunganAnggota();
						kunjunganAnggota.setKeterangan("Berkunjung karena mengembalikan buku");
						kunjunganAnggota.setAnggota(mypeminjamanPengadaanItem.getAnggota());
						kunjunganAnggota.setPerpustakaan(mypeminjamanPengadaanItem.getPerpustakaan());
						kunjunganAnggota.setTanggal(tanggalPembuatan.getValue());
						kunjunganAnggota.setTgl(tanggalPembuatan.getValue());

						session.save(kunjunganAnggota);
					}

					Common.insertCombo(KembaliPengadaanItemAction.this.kunjunganAnggota, "anggota", "tanggal",
							KunjunganAnggota.class,
							Restrictions.and(
									Restrictions.and(Restrictions.eq("anggota", mypeminjamanPengadaanItem.getAnggota()),
											Restrictions.eq("perpustakaan",
													mypeminjamanPengadaanItem.getPerpustakaan())),
									Restrictions.eq("tgl", tanggalPembuatan.getValue())));

					Common.selectComboItem(KembaliPengadaanItemAction.this.kunjunganAnggota,
							kembaliPengadaanItem.getKunjunganAnggota());
					if (KembaliPengadaanItemAction.this.kunjunganAnggota.getSelectedItem() == null
							&& !KembaliPengadaanItemAction.this.kunjunganAnggota.getChildren().isEmpty()) {
						KembaliPengadaanItemAction.this.kunjunganAnggota.setSelectedIndex(0);
					}

					Common.clear(foto);
					LibraryUtil.gambarAnggota(mypeminjamanPengadaanItem.getAnggota()).setParent(foto);
					foto.appendChild(new MyLabelBoldAja(mypeminjamanPengadaanItem.getAnggota().getNama()));

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		};

		final MyToolbarbuttonConfig perpanjang = new MyToolbarbuttonConfig("Perpanjang Peminjaman", "/img/corner.gif");
		// perpanjang
		// .setVisible(kembaliPengadaanItem.getPeminjamanPengadaanItem() != null
		// && kembaliPengadaanItem.getPeminjamanPengadaanItem()
		// .getId() != null);
		perpanjang.setVisible(false);

		final MyToolbarbuttonConfig batalPerpanjang = new MyToolbarbuttonConfig("Batal Perpanjang",
				"/img/svg/warning-outline.svg");
		// batalPerpanjang
		// .setVisible(kembaliPengadaanItem.getPeminjamanPengadaanItem() != null
		// && kembaliPengadaanItem.getPeminjamanPengadaanItem()
		// .getId() != null
		// && kembaliPengadaanItem.getPeminjamanPengadaanItem()
		// .getJumlahPerpanjangan() > 0);

		batalPerpanjang.setVisible(false);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Data Peminjaman"));
		row.appendChild(new ais.ui.util.MyLabelConfig(kembaliPengadaanItem.getPeminjamanPengadaanItem() == null ? ""
				: kembaliPengadaanItem.getPeminjamanPengadaanItem().toString()));
		// row.appendChild(peminjamanPengadaanItem = new
		// AmbilDataPeminjamanPengadaanItemBanbox());
		// peminjamanPengadaanItem.setWidth("90%");
		// peminjamanPengadaanItem.setAttribute("peminjamanPengadaanItem",
		// kembaliPengadaanItem.getPeminjamanPengadaanItem());
		// peminjamanPengadaanItem.setValue(kembaliPengadaanItem.getPeminjamanPengadaanItem()
		// == null ? ""
		// : kembaliPengadaanItem.getPeminjamanPengadaanItem().toString());
		// peminjamanPengadaanItem.setReadonly(true);
		// peminjamanPengadaanItem.setEventListener(new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// PeminjamanPengadaanItem mypeminjamanPengadaanItem =
		// (PeminjamanPengadaanItem) peminjamanPengadaanItem
		// .getAttribute("peminjamanPengadaanItem");
		// currentPerpustakaan = mypeminjamanPengadaanItem.getPerpustakaan();
		// String mykode = LibraryUtil.generateCode(KembaliPengadaanItem.class,
		// 8, "KMB", currentPerpustakaan);
		// kode.setValue(mykode);
		// kembaliPengadaanItemPunyaItemHelper.setPerpustakaan(currentPerpustakaan);
		// kembaliPengadaanItemPunyaItemHelper.setPeminjamanPengadaanItem(mypeminjamanPengadaanItem);
		//
		// perpanjang.setVisible(mypeminjamanPengadaanItem != null);
		//
		// Common.createDefaultTimer(kunjunganEventListener);
		// }
		// });

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kunjungan Anggota"));
		row.appendChild(kunjunganAnggota = new Combobox());
		kunjunganAnggota.setWidth("90%");
		kunjunganAnggota.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Foto Anggota"));
		row.appendChild(foto = new Vbox());
		foto.setWidth("90%");

		kunjunganEventListener.onEvent(null);

		if (kembaliPengadaanItem.getPerpustakaan() != null) {
			currentPerpustakaan = kembaliPengadaanItem.getPerpustakaan();
			kembaliPengadaanItemPunyaItemHelper.setPerpustakaan(currentPerpustakaan);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new MyTextbox(
				kembaliPengadaanItem.getKeterangan() == null ? "" : kembaliPengadaanItem.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(10);

		if (kembaliPengadaanItem.getPeminjamanPengadaanItem() != null) {
			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kembaliPengadaanItemPunyaItemHelper
							.setPeminjamanPengadaanItem(kembaliPengadaanItem.getPeminjamanPengadaanItem());
				}
			}, "Sedang menyiapkan data item yang dipinjam anggota ...\nHarap tunggu");
		}

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
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

		// perpanjang.setTooltiptext("Perpanjang");
		// perpanjang.addEventListener("onClick", new EventListener() {
		// @Override
		// public void onEvent(Event event) throws Exception {
		// if (onPerpanjang(event)) {
		// onSearchDefault(null);
		// addWindow.setVisible(false);
		// }
		// }
		// });
		// perpanjang.setParent(toolbar);

		// batalPerpanjang.setTooltiptext("Batal Perpanjang");
		// batalPerpanjang.addEventListener("onClick", new EventListener() {
		// @Override
		// public void onEvent(Event event) throws Exception {
		// if (onBatalPerpanjang(event)) {
		// onSearchDefault(null);
		// addWindow.setVisible(false);
		// }
		// }
		// });
		// batalPerpanjang.setParent(toolbar);

		borderlayout.setParent(addWindow);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void kirim(final KembaliPengadaanItem kembaliPengadaanItem) throws Exception {

		File myfilebarcode = new File(Common.ambilREAL_PATH_REPORT() + "/barcode_" + kembaliPengadaanItem.getKode() + ".png");
		Barcode mybarcode = BarcodeFactory.createCode128B(kembaliPengadaanItem.getKode());
		BarcodeImageHandler.savePNG(mybarcode, myfilebarcode);
		String kodeBarcode = myfilebarcode.getAbsolutePath();

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("id", kembaliPengadaanItem.getId());
		parameters.put("kode_barcode", kodeBarcode);

		parameters.put("info", LibraryUtil.tampilanSummaryPeminjaman(kembaliPengadaanItem,
				kembaliPengadaanItem.getPeminjamanPengadaanItem()));

		final File file = Report.generatePDFReport(Report.PDF, parameters, "library/pengembalian",
				kembaliPengadaanItem.getTanggalPembuatan(), null, Common.locale, null);

		final PeminjamanPengadaanItem peminjamanPengadaanItem = kembaliPengadaanItem.getPeminjamanPengadaanItem();

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
				+ "Kami memberitahukan bahwa pengembalian buku di \"" + kembaliPengadaanItem.getPerpustakaan().getNama()
				+ "\" di " + sekolah + " telah berhasil diproses pada "
				+ Common.dateFormat61.get().format(kembaliPengadaanItem.getTanggalPembuatan()) + ".\r\n" + "\r\n"
				+ "Berikut adalah rincian pengembalian Anda:\r\n" + "\r\n" + "*   **Nama Anggota:** " + nama + "\r\n"
				+ "*   **Perpustakaan:** " + kembaliPengadaanItem.getPerpustakaan().getNama() + "\r\n"
				+ "*   **Tanggal Pengembalian:** "
				+ Common.dateFormat51.get().format(kembaliPengadaanItem.getTanggalPembuatan()) + "\r\n" + "\r\n"
				+ "Kami mengucapkan terima kasih atas pengembalian buku yang telah dilakukan tepat waktu. Kami berharap Anda dapat terus memanfaatkan layanan dan koleksi yang kami sediakan di Perpustakaan "
				+ peminjamanPengadaanItem.getPerpustakaan().getNama()
				+ ". Detail mengenai buku yang Anda kembalikan dapat Anda lihat pada sistem/portal perpustakaan atau pada file terlampir.\r\n"
				+ "\r\n"
				+ "Jika Anda memiliki pertanyaan lebih lanjut, silakan menghubungi kami melalui informasi kontak yang tertera di kartu anggota perpustakaan atau di website perpustakaan kami.\r\n"
				+ "\r\n" + "Terima kasih atas partisipasi Anda dalam memanfaatkan layanan Perpustakaan "
				+ kembaliPengadaanItem.getPerpustakaan().getNama() + ".";

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

		MailSender.sendMailLampiran(userIds, subject, body, sender, emailUser, kembaliPengadaanItem, attachmentsData,
				false, file);

		if (Common.bolehKonfigurasi("aktifkan_kirim_notif_pengembalian_buku_perpustakaan_ke_wa")) {
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
									+ "Kami memberitahukan bahwa pengembalian buku di \""
									+ kembaliPengadaanItem.getPerpustakaan().getNama() + "\" di " + sekolah
									+ " telah berhasil diproses pada "
									+ Common.dateFormat61.get().format(kembaliPengadaanItem.getTanggalPembuatan()) + ".\r\n"
									+ "\r\n" + "Berikut adalah rincian pengembalian Anda:\r\n" + "\r\n"
									+ "*   **Nama Anggota:** " + nama + "\r\n" + "*   **Perpustakaan:** "
									+ kembaliPengadaanItem.getPerpustakaan().getNama() + "\r\n"
									+ "*   **Tanggal Pengembalian:** "
									+ Common.dateFormat51.get().format(kembaliPengadaanItem.getTanggalPembuatan()) + "\r\n"
									+ "\r\n"
									+ "Kami mengucapkan terima kasih atas pengembalian buku yang telah dilakukan tepat waktu. Kami berharap Anda dapat terus memanfaatkan layanan dan koleksi yang kami sediakan di Perpustakaan "
									+ peminjamanPengadaanItem.getPerpustakaan().getNama()
									+ ". Detail mengenai buku yang Anda kembalikan dapat Anda lihat pada sistem/portal perpustakaan atau pada file terlampir.\r\n"
									+ "\r\n"
									+ "Jika Anda memiliki pertanyaan lebih lanjut, silakan menghubungi kami melalui informasi kontak yang tertera di kartu anggota perpustakaan atau di website perpustakaan kami.\r\n"
									+ "\r\n"
									+ "Terima kasih atas partisipasi Anda dalam memanfaatkan layanan Perpustakaan "
									+ kembaliPengadaanItem.getPerpustakaan().getNama() + ".";

							String urlD = Common.getRequestHostWithProtocolSimple()
									+ file.getAbsolutePath().split("webapps")[1];

							Wa.kirimWaViaUltramsg(from, dawal + body, "Bukti_Pengembalian.pdf", urlD);
						}
					}
				}
			}, "", false, 2000);
		}
	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {

		if (kunjunganAnggota.getSelectedItem() == null) {
			MyMessageboxConfig.show("Data Kunjungan Anggota harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		final List<Row> rowsItem = gridItem.getRows().getChildren();
		for (Row row : rowsItem) {
			KembaliPengadaanItemDetail kembaliPengadaanItemDetail = (KembaliPengadaanItemDetail) row
					.getAttribute("kembaliPengadaanItemDetail");
			if (kembaliPengadaanItemDetail.getItem() == null) {
				MyMessageboxConfig.show("Item harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		KembaliPengadaanItemDao kembaliPengadaanItemDao = DaoFactory.getInstance().getKembaliPengadaanItemDao();
		if (kembaliPengadaanItem.getId() != null) {
			kembaliPengadaanItem = kembaliPengadaanItemDao.load(kembaliPengadaanItem.getId());

		}

		kembaliPengadaanItem.setPeminjamanPengadaanItem(peminjamanPengadaanItem);
		// kembaliPengadaanItem.setPeminjamanPengadaanItem(
		// (PeminjamanPengadaanItem)
		// peminjamanPengadaanItem.getAttribute("peminjamanPengadaanItem"));
		kembaliPengadaanItem.setPerpustakaan(kembaliPengadaanItem.getPeminjamanPengadaanItem().getPerpustakaan());
		kembaliPengadaanItem.setKode(kode.getValue());
		kembaliPengadaanItem.setKeterangan(keterangan.getValue());
		kembaliPengadaanItem.setTanggalPembuatan(tanggalPembuatan.getValue());
		kembaliPengadaanItem.setTanggalPersetujuan(tanggalPersetujuan.getValue());
		kembaliPengadaanItem.setKunjunganAnggota((KunjunganAnggota) kunjunganAnggota.getSelectedItem().getValue());

		Session session = HibernateUtil.currentSession();
		if (kembaliPengadaanItem.getId() != null) {
			Common.refreshUpdate(session, kembaliPengadaanItem);
		} else {
			kembaliPengadaanItem.setDibuatOleh(Common.getCurrentUser());

			currentPerpustakaan = kembaliPengadaanItem.getPeminjamanPengadaanItem().getPerpustakaan();
			kembaliPengadaanItem.setIndex(
					LibraryUtil.generateMaxByPerpustakaan(KembaliPengadaanItem.class, currentPerpustakaan) + 1);
			String mykode = LibraryUtil.generateCode(KembaliPengadaanItem.class, 8, "KMB", currentPerpustakaan);
			kode.setValue(mykode);
			kembaliPengadaanItem.setKode(mykode);
			session.save(kembaliPengadaanItem);
		}

		// generateDetail(kembaliPengadaanItem);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = HibernateUtil.currentSession();
				for (Row row : rowsItem) {
					try {
						KembaliPengadaanItemDetail kembaliPengadaanItemDetail = (KembaliPengadaanItemDetail) row
								.getAttribute("kembaliPengadaanItemDetail");
						if (kembaliPengadaanItemDetail.getId() != null) {
							session.refresh(kembaliPengadaanItemDetail);
						}
						kembaliPengadaanItemDetail.setKembaliPengadaanItem(kembaliPengadaanItem);

						MyCheckboxConfig checkbox = (MyCheckboxConfig) row.getAttribute("checkbox");

						if (checkbox.isChecked()) {
							// if (kembaliPengadaanItemDetail.getId() == null) {

							Datebox datebox = (Datebox) row.getAttribute("tanggal");

							PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail = kembaliPengadaanItemDetail
									.getPeminjamanPengadaanItemDetail();
							session.refresh(peminjamanPengadaanItemDetail);
							peminjamanPengadaanItemDetail.setTanggalKembali(datebox.getValue());
							DendaKeterlambatanItem dendaPerItem = LibraryUtil
									.hitungDendaItem(peminjamanPengadaanItemDetail);

							Double denda = dendaPerItem == null ? 0.0 : dendaPerItem.getDenda();
							denda = denda * peminjamanPengadaanItemDetail.getJumlah();
							kembaliPengadaanItemDetail.setDenda(denda);

							MyCheckboxConfig telahDibayar = (MyCheckboxConfig) row.getAttribute("telahDibayar");
							MyDoublebox dibayarSejumlah = (MyDoublebox) row.getAttribute("dibayarSejumlah");

							kembaliPengadaanItemDetail.setTelahDibayar(telahDibayar.isChecked());
							kembaliPengadaanItemDetail.setDibayarSejumlah(dibayarSejumlah.getValue());

							Common.refreshSaveOrUpdate(session, kembaliPengadaanItemDetail);

							peminjamanPengadaanItemDetail.setKembaliPengadaanItemDetail(kembaliPengadaanItemDetail);
							Common.refreshSaveOrUpdate(session, peminjamanPengadaanItemDetail);

							// }
						} else if (kembaliPengadaanItemDetail.getId() != null) {
							// session.refresh(kembaliPengadaanItemDetail);
							// session.delete(kembaliPengadaanItemDetail);
							session.createSQLQuery("delete from library.kembali_pengadaan_item_detail where id = "
									+ kembaliPengadaanItemDetail.getId()).executeUpdate();
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}

				Double denda = LibraryUtil.hitungDendaPerItem(kembaliPengadaanItem);
				PeminjamanPengadaanItem peminjamanPengadaanItem = kembaliPengadaanItem.getPeminjamanPengadaanItem();

				// session.refresh(peminjamanPengadaanItem);
				// peminjamanPengadaanItem.setDendaKeterlambatanPerItem(denda);
				// session.update(peminjamanPengadaanItem);
				session.createSQLQuery("update library.peminjaman_pengadaan_item set dendaketerlambatanperitem=" + denda
						+ " where id=" + peminjamanPengadaanItem.getId()).executeUpdate();

				setujui(KembaliPengadaanItemAction.this.kembaliPengadaanItem);

			}
		});

		return true;
	}

	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(KembaliPengadaanItem.class);
		boolean ada = searchBelumLunas.isChecked() || searchBelumBayar.isChecked()
				|| !searchbarkode.getValue().trim().isEmpty() || !searchjudul.getValue().trim().isEmpty()
				|| !searchisbn.getValue().trim().isEmpty();
		if (ada) {
			List<Long> ids = session.createCriteria(KembaliPengadaanItemDetail.class)
					.add(Restrictions.sqlRestriction("this_.denda>0.1"))
					.add(searchBelumBayar.isChecked() ? Restrictions.eq("telahDibayar", false)
							: Restrictions.sqlRestriction("true"))
					.add(searchBelumLunas.isChecked() ? Restrictions.sqlRestriction("this_.denda>this_.dibayarsejumlah")
							: Restrictions.sqlRestriction("true"))

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

					.setProjection(Projections.groupProperty("kembaliPengadaanItem.id"))

					.setMaxResults(32760)

					.list();

			if (!ids.isEmpty()) {
				criteria.add(Restrictions.in("id", ids));
			}
		}

		criteria.createAlias("peminjamanPengadaanItem", "peminjamanPengadaanItem")
				.add((searchperpustakaan == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchperpustakaan.getAttribute("perpustakaan") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("perpustakaan", searchperpustakaan.getAttribute("perpustakaan"))))

				.add((searchanggota == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchanggota.getAttribute("anggota") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("peminjamanPengadaanItem.anggota", searchanggota.getAttribute("anggota"))))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
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
		List<KembaliPengadaanItem> kembaliPengadaanItem = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kembaliPengadaanItem);
		grid.setRowRenderer(new KembaliPengadaanItemRenderer());
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
						Restrictions.or(Restrictions.eq("kode", searchkodeangota.getValue().trim()),
								Restrictions.eq("mahasiswa.nim", searchkodeangota.getValue().trim())),
						Restrictions.eq("dosen.mycode", searchkodeangota.getValue().trim())

				)).setMaxResults(1).uniqueResult();

		if (anggota == null) {
			anggota = (Anggota) session.createCriteria(PeminjamanPengadaanItemDetail.class)
					.createAlias("itemPunyaBarcode", "itemPunyaBarcode")
					.createAlias("peminjamanPengadaanItem", "peminjamanPengadaanItem")
					.setProjection(Projections.property("peminjamanPengadaanItem.anggota"))
					.add(Restrictions.eq("itemPunyaBarcode.barcode", searchkodeangota.getValue().trim()))
					.add(Restrictions.isNull("kembaliPengadaanItemDetail")).addOrder(Order.desc("id")).setMaxResults(1)
					.uniqueResult();

			if (anggota != null) {
				barcodeItem = searchkodeangota.getValue().trim();
			} else {
				barcodeItem = null;
			}
		} else {
			barcodeItem = null;
		}

		if (anggota == null) {
			MyMessageboxConfig.show("Kode Anggota \"" + searchkodeangota.getValue().trim() + "\" tidak ditemukan",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							searchkodeangota.focus();
							searchkodeangota.select();
						}
					});
			return;
		}

		PeminjamanPengadaanItem peminjamanPengadaanItem = (PeminjamanPengadaanItem) session
				.createCriteria(PeminjamanPengadaanItemDetail.class)
				.createAlias("peminjamanPengadaanItem", "peminjamanPengadaanItem")
				.setProjection(Projections.property("peminjamanPengadaanItem"))
				.add(Restrictions.eq("peminjamanPengadaanItem.anggota", anggota))
				.add(Restrictions.isNull("kembaliPengadaanItemDetail")).addOrder(Order.desc("id")).setMaxResults(1)
				.uniqueResult();

		if (peminjamanPengadaanItem != null) {

			KembaliPengadaanItem kembaliPengadaanItem = (KembaliPengadaanItem) session
					.createCriteria(KembaliPengadaanItem.class)
					.add(Restrictions.eq("peminjamanPengadaanItem", peminjamanPengadaanItem)).addOrder(Order.desc("id"))
					.setMaxResults(1).uniqueResult();
			if (kembaliPengadaanItem != null) {
				init(kembaliPengadaanItem);
				addWindow.setVisible(true);
				addWindow.onModal();
			} else {
				kembaliPengadaanItem = new KembaliPengadaanItem();
				kembaliPengadaanItem.setPeminjamanPengadaanItem(peminjamanPengadaanItem);
				init(kembaliPengadaanItem);
				addWindow.setVisible(true);
				addWindow.onModal();
			}
		} else {

			MyMessageboxConfig.show(
					"Anggota dengan kode \"" + searchkodeangota.getValue().trim()
							+ "\" belum melakukan proses peminjaman",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							searchkodeangota.focus();
							searchkodeangota.select();
						}
					});

		}

		searchkodeangota.setValue("");
		searchkodeangota.focus();
		searchkodeangota.select();
	}
}
