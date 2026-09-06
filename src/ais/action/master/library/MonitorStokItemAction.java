package ais.action.master.library;

import java.util.List;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Vbox;

import ais.action.master.dashboard.admin.DashboardMonitorStokPerJenisItem;
import ais.action.master.dashboard.admin.DashboardMonitorStokPerTipeItem;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.helper.AmbilDataPerpustakaanBanbox;
import ais.action.master.library.helper.DetailTransaksiHelper;
import ais.action.master.library.helper.TampilanHasilScanPerHalamanWindow;
import ais.action.master.library.util.LibraryUtil;
import ais.action.report.format1.library.LaporanStokItem;
import ais.action.report.format1.library.LaporanTrackingStokItem;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.file.FotoImagePerHalamanItem;
import ais.database.model.library.DetailTransaksi;
import ais.database.model.library.Item;
import ais.database.model.library.JenisItem;
import ais.database.model.library.Perpustakaan;
import ais.database.model.library.TipeItem;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIframe;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk monitor stok item. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code MyTextbox
 * searchnama}, {@code MyTextbox searchkode}, {@code MyTextbox searchbarcodeItem}, {@code MyTextbox
 * searchbarcode}, {@code Combobox searchtipeItem}, {@code Combobox searchjenisItem}, {@code
 * AmbilDataPerpustakaanBanbox searchperpustakaan}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * doAfterCompose()}, {@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}); pelaporan/ekspor
 * ({@code onCetak()}, {@code onCetakTrack()}); operasi domain lain ({@code onBarcodeItem()}, {@code
 * onPerTipeItem()}, {@code onPerJenisItem()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class MonitorStokItemAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;

	private MyGrid grid;
	private MyTextbox searchnama;
	private MyTextbox searchkode;
	private MyTextbox searchbarcodeItem;
	private MyTextbox searchbarcode;
	protected Combobox searchtipeItem;
	private Combobox searchjenisItem;
	private AmbilDataPerpustakaanBanbox searchperpustakaan;
	private MyDatebox searchperTanggal;
	private Combobox searchfakultas;
	private Combobox searchjurusan;

	// private Footer stok;

	private MyToolbarbuttonConfig cetak;
	private MyToolbarbuttonConfig cetakTrack;

	private boolean padaSaatpendataanItemPerpustakaanTampilkanPilihanFakultasDanProgramStudi;

	private Perpustakaan perpustakaan = null;

	private Paging paging;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		padaSaatpendataanItemPerpustakaanTampilkanPilihanFakultasDanProgramStudi = Common.bolehKonfigurasi("saat_pendataan_item_perpustakaan_tampilkan_pilihan_fakultas_dan_prodi", Konfigurasi.TIDAK_AKTIF);

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		if (searchfakultas != null) { searchfakultas.setVisible(padaSaatpendataanItemPerpustakaanTampilkanPilihanFakultasDanProgramStudi); }
		if (searchjurusan != null) { searchjurusan.setVisible(padaSaatpendataanItemPerpustakaanTampilkanPilihanFakultasDanProgramStudi); }

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && (tbmuser.getMahasiswa() != null || tbmuser.ambilDosen() != null)) {
			cetak.setVisible(false);
			cetakTrack.setVisible(false);
		}

		searchperpustakaan.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		if (searchperTanggal != null) { searchperTanggal.setValue(ais.ui.util.WaktuUtil.getDate()); }
		Common.insertCombo(searchjenisItem, "nama", JenisItem.class);
		Common.insertCombo(searchtipeItem, "nama", "kode", TipeItem.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));

		if (execution.getParameter("perpustakaan") != null) {
			Session session = HibernateUtil.currentSession();
			Perpustakaan perpustakaan = (Perpustakaan) session.createCriteria(Perpustakaan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("perpustakaan").trim())))
					.uniqueResult();
			if (perpustakaan != null) {
				this.perpustakaan = perpustakaan;
				searchperpustakaan.setValue(perpustakaan.getNama());
				searchperpustakaan.setAttribute("perpustakaan", perpustakaan);
				searchperpustakaan.setDisabled(true);
			}
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	        FilterLanjutHelper.setup(comp);
}

	private Tabpanel barcodeItem;

	public void onBarcodeItem(Event event) {

		if (barcodeItem.getChildren().size() == 0) {

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(barcodeItem);

			Center center = new Center();
			ais.ui.util.ZkCompat.setFlex(center, true);
			center.setParent(borderlayout);
			center.setBorder("none");

			MyIframe include = new MyIframe("/pages/master/library/barcode_item.zul?perpustakaan="
					+ (perpustakaan == null || perpustakaan.getId() == null ? -1L : perpustakaan.getId()));
			include.setHeight("700px");
			include.setWidth("100%");
			include.setParent(center);
		}
	}

	private Tabpanel monitorPerTipeItem;

	public void onPerTipeItem(Event event) {

		if (monitorPerTipeItem.getChildren().size() == 0) {
			DashboardMonitorStokPerTipeItem laporan = new DashboardMonitorStokPerTipeItem(perpustakaan);
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, monitorPerTipeItem,
				"Stok per Tipe", "Jumlah koleksi dan stok tersedia berdasarkan tipe item perpustakaan.");
		}
	}

	private Tabpanel monitorPerJenisItem;

	public void onPerJenisItem(Event event) {

		if (monitorPerJenisItem.getChildren().size() == 0) {
			DashboardMonitorStokPerJenisItem laporan = new DashboardMonitorStokPerJenisItem(perpustakaan);
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, monitorPerJenisItem,
				"Stok per Jenis", "Jumlah koleksi dan stok tersedia berdasarkan jenis item perpustakaan.");
		}
	}

	public void onCetak(Event event) throws Exception {
		LaporanStokItem laporan = new LaporanStokItem(perpustakaan, null);
		laporan.setTitle("Cetak Laporan");
		page.getFirstRoot().appendChild(laporan);
		laporan.setHeight("95%");
		laporan.setWidth("90%");
		laporan.setClosable(true);
		laporan.onModal();
	}

	public void onCetakTrack(Event event) throws Exception {
		LaporanTrackingStokItem laporan = new LaporanTrackingStokItem(perpustakaan, null);
		laporan.setTitle("Cetak Laporan");
		page.getFirstRoot().appendChild(laporan);
		laporan.setHeight("95%");
		laporan.setWidth("90%");
		laporan.setClosable(true);
		laporan.onModal();
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link MonitorStokItemAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link MonitorStokItemAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see MonitorStokItemAction
	 */
	class JenisBarangRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			Object[] objects = (Object[]) arg1;
			Long itemId = ((Number) objects[0]).longValue();
			// Date tanggalTerakhirPengadaan = (Date) objects[2];
			Number stok = (Number) objects[3];
			String perpustakaan = (String) objects[4];
			Long p = ((Number) objects[5]).longValue();
			Session session = HibernateUtil.currentSession();
			final Item item = (Item) session.createCriteria(Item.class).add(Restrictions.idEq(itemId)).uniqueResult();
			final Perpustakaan perpustakaanData = (Perpustakaan) session.createCriteria(Perpustakaan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.idEq(p)).uniqueResult();

			final DetailTransaksiHelper detail = new DetailTransaksiHelper(null, item, perpustakaanData);
			detail.setParent(arg0);

			Image image = LibraryUtil.generateImage(item);
			image.setWidth("100%");
			image.setParent(arg0);

			new ais.ui.util.MyHtml("<font style=\"font-size: x-small;\">ISBN 10 : "
					+ (item.getIsbn10() == null ? "" : item.getIsbn10()) + "<br>ISBN 13 : "
					+ (item.getIsbn() == null ? "" : item.getIsbn()) + "<br>ISSN : "
					+ (item.getIssn() == null ? "" : item.getIssn()) + "<br>DDC : "
					+ (item.getDdcItem() == null ? item.getDeweyDecimalClass() : item.getDdcItem()) + "<br>UDC : "
					+ (item.getUdcItem() == null ? "" : item.getUdcItem()) + "</font>").setParent(arg0);

			RevisiHelper.createNewRevisi(Item.class, item, item.getNama()).setParent(arg0);

			new Label(stok == null ? "" : Common.numberFormat.get().format(stok)).setParent(arg0);

			DetailTransaksi detailTransaksi = (DetailTransaksi) session.createCriteria(DetailTransaksi.class)
					.add(Restrictions.eq("item", item)).addOrder(Order.desc("tanggalDanWaktu")).setMaxResults(1)
					.uniqueResult();
			new Label(detailTransaksi == null ? ""
					: (Common.dateFormat3.get().format(detailTransaksi.getTanggalDanWaktu()) + " "
							+ (detailTransaksi.getKodeTransaksi() == null ? ""
									: (detailTransaksi.getKodeTransaksi().getKode() + " - "
											+ detailTransaksi.getKodeTransaksi().getNama()))
							+ " - " + DetailTransaksiHelper.dapatkanInfo(detailTransaksi)))
					.setParent(arg0);

			new Label(perpustakaan).setParent(arg0);

			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Stok", "/img/print.png");
			button.setOrient("vertical");
			button.setTooltiptext("Cetak Stok");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					LaporanStokItem laporan = new LaporanStokItem(perpustakaanData, item);
					laporan.setTitle("Cetak Laporan");
					page.getFirstRoot().appendChild(laporan);
					laporan.setHeight("95%");
					laporan.setWidth("90%");
					laporan.setClosable(true);
					laporan.onModal();

				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Track", "/img/print.png");
			button.setOrient("vertical");
			button.setTooltiptext("Cetak Stok");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					LaporanTrackingStokItem laporan = new LaporanTrackingStokItem(perpustakaanData, item);
					laporan.setTitle("Cetak Laporan");
					page.getFirstRoot().appendChild(laporan);
					laporan.setHeight("95%");
					laporan.setWidth("90%");
					laporan.setClosable(true);
					laporan.onModal();

				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Google", "/img/Apps-Google-Play-Books-icon.png");
			button.setOrient("vertical");
			button.setTooltiptext("Baca Buku via Google");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					JSONObject jsonObject = new JSONObject(item.getInfoLain()).getJSONObject("volumeInfo");
					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(jsonObject.getString("previewLink"), "_blank");
					} else {
						Clients.evalJavaScript("popupCenter({url: '" + Common.jsEscape(jsonObject.getString("previewLink"))
								+ "', title: 'Book', w: 1200, h: 600});");
					}

				}

			});
			aksiButtons.add(button);
			button.setVisible(item.getGoogleBookId() != null && !item.getGoogleBookId().trim().isEmpty());

			button = new MyToolbarbuttonConfig("Baca", "/img/Book-icon.png");
			button.setOrient("vertical");
			button.setTooltiptext("Lihat Isi Buku");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					TampilanHasilScanPerHalamanWindow halamanWindow = new TampilanHasilScanPerHalamanWindow("Isi Buku",
							"none", true);

					halamanWindow.init(item);
					try {
						page.getFirstRoot().appendChild(halamanWindow);
						halamanWindow.onModal();
					} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				}

			});
			aksiButtons.add(button);
			Session mysession = StreamingHibernateUtil.getInstance().currentSession();

			int qty = ((Number) mysession.createCriteria(FotoImagePerHalamanItem.class)
					.add(Restrictions.eq("item", item.getId())).setProjection(Projections.rowCount()).uniqueResult())
					.intValue();

			StreamingHibernateUtil.getInstance().closeSession();

			button.setVisible(qty > 0);

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);

		}

	}

	public SQLQuery initCriteria(boolean order, int count, int start) {
		Session session = HibernateUtil.currentSession();

		JenisItem jenisItem = (JenisItem) (searchjenisItem.getSelectedItem() == null ? null
				: searchjenisItem.getSelectedItem().getValue());
		Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
		Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());
		TipeItem tipeItem = (TipeItem) (searchtipeItem.getSelectedItem() == null ? null
				: searchtipeItem.getSelectedItem().getValue());

		Perpustakaan perpustakaan = (Perpustakaan) (searchperpustakaan.getAttribute("perpustakaan"));

		String sql = "select a.item, max(c.nama) as nama_item, max(a.tanggal) as tanggal_terakhir_pengadaan, "
				+ "sum((a.qty+a.qtybonus)*b.jenis) as stok, max(d.nama) as perpustakaan, max(d.id) as idPerpus "
				+ "from library.detail_transaksi a "
				+ "inner join library.kode_transaksi b on (a.kode_transaksi = b.id) "

				+ "left join library.item c on (a.item = c.id) "
				+ "left join library.perpustakaan d on (a.perpustakaan = d.id) where 1=1 and (c.aktif is null or c.aktif=true) "
				+ (searchkode.getValue().trim().equals("") ? ""
						: " and c.isbn ilike '%" + searchkode.getValue().trim() + "%' ")
				+ " "
				+ (searchnama.getValue().trim().equals("") ? ""
						: " and c.nama ilike '%" + searchnama.getValue().trim() + "%' ")
				+ "  "
				+ (searchbarcode.getValue().trim().equals("") ? ""
						: "and c.issn ilike '%" + searchbarcode.getValue().trim() + "%'")
				+ "  and c.jenis_item = " + (jenisItem == null ? "c.jenis_item" : jenisItem.getId())
				+ " and a.perpustakaan = " + (perpustakaan == null ? "a.perpustakaan" : perpustakaan.getId())
				+ (searchbarcodeItem != null && !searchbarcodeItem.getValue().trim().isEmpty()
						? " and c.id in (select item from library.item_punya_barcode where barcode='"
								+ searchbarcodeItem.getValue().trim() + "') "
						: " ")
				+ (tipeItem == null ? "" : " and c1.tipe_item=" + tipeItem.getId() + " ")
				+ (fakultas == null ? "" : " and c.fakultas=" + fakultas.getId() + " ")
				+ (jurusan == null ? "" : " and c.jurusan=" + jurusan.getId() + " ") + " and date(a.tanggal) <= date('"
				+ (Common.databaseDateFormat.get()
						.format(searchperTanggal.getValue() == null ? ais.ui.util.WaktuUtil.getDate()
								: searchperTanggal.getValue()))
				+ "') group by a.perpustakaan,a.item  ";

		if (order)
			sql += " order by stok asc ";

		sql += " LIMIT " + count + " OFFSET " + start;

		return session.createSQLQuery(sql);
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		int size = 50000;
		paging.setPageIncrement(Common.isMobile() ? 5 : 10);
		paging.setMold("os");
		paging.setDetailed(false);
		paging.setTotalSize(size);
		paging.setVisible(size > Common.ROWS_COUNT_ON_PAGE);
		List<Object[]> item = initCriteria(true, Common.ROWS_COUNT_ON_PAGE,
				Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(item);
		grid.setRowRenderer(new JenisBarangRenderer());
		grid.setModelCheckMobile(strset);

	}

}
