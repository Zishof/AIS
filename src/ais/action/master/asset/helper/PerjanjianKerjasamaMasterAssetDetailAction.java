package ais.action.master.asset.helper;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.MasterAsset;
import ais.database.model.asset.PerjanjianKerjasamaMasterAsset;
import ais.database.model.asset.PerjanjianKerjasamaMasterAssetDetail;
import ais.database.model.asset.PermintaanPengadaanMasterAsset;
import ais.database.model.file.LampiranLain;
import ais.database.model.library.SaldoAwalDetail;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk perjanjian kerjasama master asset detail. Tipe ini merupakan titik
 * masuk UI yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi
 * khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code PerjanjianKerjasamaMasterAsset
 * perjanjianKerjasamaMasterAsset}, {@code MyGrid grid}, {@code boolean edit}, {@code boolean delete}, {@code
 * Paging paging}, {@code Textbox barcode}, {@code Textbox cari}; inisialisasi/lifecycle ({@code
 * initCriteria()}); pembacaan/pencarian ({@code loadData()}, {@code loadBarcode()}); operasi domain lain ({@code
 * display()}); konfigurasi constructor: {@code delete}, {@code edit}, {@code paging}. Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see MyDetail
 */
public class PerjanjianKerjasamaMasterAssetDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private PerjanjianKerjasamaMasterAsset perjanjianKerjasamaMasterAsset;
	private MyGrid grid;
	private boolean edit = false;
	private boolean delete = false;

	private Paging paging;

	private Textbox barcode;

	private Textbox cari;

	public PerjanjianKerjasamaMasterAssetDetailAction(PerjanjianKerjasamaMasterAsset perjanjianKerjasamaMasterAsset) {
		super();
		this.perjanjianKerjasamaMasterAsset = perjanjianKerjasamaMasterAsset;
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(PerjanjianKerjasamaMasterAssetDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link PerjanjianKerjasamaMasterAssetDetailAction}. Kelas ini
	 * menerjemahkan satu item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik
	 * kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PerjanjianKerjasamaMasterAssetDetailAction}
	 * dan dapat mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PerjanjianKerjasamaMasterAssetDetailAction
	 */
	class PerjanjianKerjasamaMasterAssetDetailRenderer extends ais.ui.util.MyRowRenderer {

		public PerjanjianKerjasamaMasterAssetDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final PerjanjianKerjasamaMasterAssetDetail perjanjianKerjasamaMasterAssetDetail = (PerjanjianKerjasamaMasterAssetDetail) data;

			Hbox hbox = new Hbox();
			hbox.setParent(row);

			LampiranLain.createDownloadUploadFileLain(hbox,
					perjanjianKerjasamaMasterAssetDetail.getMasterAsset().getId(), LampiranLain.GAMBAR_MASTER_ASSET,
					"Gambar", false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, true, null, false, true);

			final Label total = new MyLabelKecil(Common.numberFormat.get().format((perjanjianKerjasamaMasterAssetDetail.getJumlah()
					* perjanjianKerjasamaMasterAssetDetail.getHargaBeli())
					- ((perjanjianKerjasamaMasterAssetDetail.getHargaPotongan() / 100.0)
							* (perjanjianKerjasamaMasterAssetDetail.getJumlah()
									* perjanjianKerjasamaMasterAssetDetail.getHargaBeli()))));

			final MyDoublebox jumlah = new MyDoublebox(perjanjianKerjasamaMasterAssetDetail.getJumlah() == null ? 0.0
					: perjanjianKerjasamaMasterAssetDetail.getJumlah());

			final MyDoublebox hargaBeli = new MyDoublebox(perjanjianKerjasamaMasterAssetDetail.getHargaBeli());

			final MyDoublebox hargaPotongan = new MyDoublebox(perjanjianKerjasamaMasterAssetDetail.getHargaPotongan());

			new Label(perjanjianKerjasamaMasterAssetDetail.getMasterAsset() == null ? ""
					: perjanjianKerjasamaMasterAssetDetail.getMasterAsset().getKode()).setParent(row);

			Vbox a;
			(a = RevisiHelper.createNewRevisi(PerjanjianKerjasamaMasterAssetDetail.class,
					perjanjianKerjasamaMasterAssetDetail,
					perjanjianKerjasamaMasterAssetDetail.getMasterAsset() == null ? ""
							: perjanjianKerjasamaMasterAssetDetail.getMasterAsset().getNama()))
					.setParent(row);
			if (perjanjianKerjasamaMasterAssetDetail.getPermintaanPengadaanMasterAssetDetail() != null) {
				RevisiHelper.createNewRevisi(PermintaanPengadaanMasterAsset.class,
						perjanjianKerjasamaMasterAssetDetail.getPermintaanPengadaanMasterAssetDetail()
								.getPermintaanPengadaanMasterAsset(),
						perjanjianKerjasamaMasterAssetDetail.getPermintaanPengadaanMasterAssetDetail()
								.getPermintaanPengadaanMasterAsset().getKode())
						.setParent(a);
			}

			SatuanKerja satuanKerja = perjanjianKerjasamaMasterAssetDetail == null
					|| perjanjianKerjasamaMasterAssetDetail.getPermintaanPengadaanMasterAssetDetail() == null
					|| perjanjianKerjasamaMasterAssetDetail.getPermintaanPengadaanMasterAssetDetail()
							.getPermintaanPengadaanMasterAsset() == null ? null
									: perjanjianKerjasamaMasterAssetDetail.getPermintaanPengadaanMasterAssetDetail()
											.getPermintaanPengadaanMasterAsset().getSatuanKerja();
			if (satuanKerja == null) {
				satuanKerja = perjanjianKerjasamaMasterAssetDetail.getPerjanjianKerjasamaMasterAsset().getSatuanKerja();
			}

			new Label(satuanKerja == null ? "" : satuanKerja.getNama()).setParent(row);

			(jumlah).setParent(row);
			jumlah.setDisabled(
					perjanjianKerjasamaMasterAssetDetail.getPerjanjianKerjasamaMasterAsset().getDisetujuiOleh() != null
							|| !edit);
			jumlah.setStyle("text-align:right");
			jumlah.setWidth("90%");
			jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					perjanjianKerjasamaMasterAssetDetail.setJumlah(jumlah.getValue());
					Common.refreshUpdate(session, (perjanjianKerjasamaMasterAssetDetail));

					total.setValue(Common.numberFormat.get().format((perjanjianKerjasamaMasterAssetDetail.getJumlah()
							* perjanjianKerjasamaMasterAssetDetail.getHargaBeli())
							- ((perjanjianKerjasamaMasterAssetDetail.getHargaPotongan() / 100.0)
									* (perjanjianKerjasamaMasterAssetDetail.getJumlah()
											* perjanjianKerjasamaMasterAssetDetail.getHargaBeli()))));
				}
			});

			(hargaBeli).setParent(row);
			hargaBeli.setDisabled(
					perjanjianKerjasamaMasterAssetDetail.getPerjanjianKerjasamaMasterAsset().getDisetujuiOleh() != null
							|| !edit);
			hargaBeli.setStyle("text-align:right");
			hargaBeli.setWidth("90%");
			hargaBeli.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					perjanjianKerjasamaMasterAssetDetail.setHargaBeli(hargaBeli.getValue());
					Common.refreshUpdate(session, (perjanjianKerjasamaMasterAssetDetail));

					MasterAsset masterAsset = perjanjianKerjasamaMasterAssetDetail.getMasterAsset();
					session.refresh(masterAsset);
					masterAsset.setHargaBeliDefault(hargaBeli.getValue());
					Common.refreshUpdate(session, masterAsset);

					total.setValue(Common.numberFormat.get().format((perjanjianKerjasamaMasterAssetDetail.getJumlah()
							* perjanjianKerjasamaMasterAssetDetail.getHargaBeli())
							- ((perjanjianKerjasamaMasterAssetDetail.getHargaPotongan() / 100.0)
									* (perjanjianKerjasamaMasterAssetDetail.getJumlah()
											* perjanjianKerjasamaMasterAssetDetail.getHargaBeli()))));
				}
			});

			(hargaPotongan).setParent(row);
			hargaPotongan.setDisabled(
					perjanjianKerjasamaMasterAssetDetail.getPerjanjianKerjasamaMasterAsset().getDisetujuiOleh() != null
							|| !edit);
			hargaPotongan.setStyle("text-align:right");
			hargaPotongan.setWidth("90%");
			hargaPotongan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					perjanjianKerjasamaMasterAssetDetail.setHargaPotongan(hargaPotongan.getValue());
					Common.refreshUpdate(session, (perjanjianKerjasamaMasterAssetDetail));

					total.setValue(Common.numberFormat.get().format((perjanjianKerjasamaMasterAssetDetail.getJumlah()
							* perjanjianKerjasamaMasterAssetDetail.getHargaBeli())
							- ((perjanjianKerjasamaMasterAssetDetail.getHargaPotongan() / 100.0)
									* (perjanjianKerjasamaMasterAssetDetail.getJumlah()
											* perjanjianKerjasamaMasterAssetDetail.getHargaBeli()))));
				}
			});

			total.setStyle("text-align:right");
			total.setParent(row);

			final MyTextbox keterangan = new MyTextbox(perjanjianKerjasamaMasterAssetDetail.getKeterangan() == null ? ""
					: perjanjianKerjasamaMasterAssetDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setParent(row);
			keterangan.setDisabled(
					perjanjianKerjasamaMasterAssetDetail.getPerjanjianKerjasamaMasterAsset().getDisetujuiOleh() != null
							|| !edit);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					perjanjianKerjasamaMasterAssetDetail.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (perjanjianKerjasamaMasterAssetDetail));
				}
			});

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setDisabled(
					perjanjianKerjasamaMasterAssetDetail.getPerjanjianKerjasamaMasterAsset().getDisetujuiOleh() != null
							|| !delete);
			button.setTooltiptext("Hapus Data");
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

											Common.refreshDelete(perjanjianKerjasamaMasterAssetDetail);

											loadData(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException(
												"penghapusan data ini",
												"Data yang Bapak/Ibu coba hapus kemungkinan besar masih digunakan/direferensikan oleh data transaksi Asset lain di sistem (mis. dokumen pengadaan, penerimaan, pembayaran, peminjaman, atau riwayat terkait), sehingga database menolak penghapusan demi menjaga integritas data.",
												e,
												new String[] {
														"Periksa apakah data ini masih digunakan/dirujuk oleh transaksi atau data lain yang berelasi.",
														"Hapus atau ubah terlebih dahulu data yang masih berelasi tersebut, baru ulangi penghapusan data ini.",
														"Nonaktifkan saja data ini (bukan menghapus) apabila data ini memang masih perlu dirujuk oleh data lain." });

										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);
			toolbar.setParent(row);

		}
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Common.initPaging(initCriteria(false), paging);
		List<SaldoAwalDetail> saldoAwalDetails = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(saldoAwalDetails);
		grid.setRowRenderer(new PerjanjianKerjasamaMasterAssetDetailRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(PerjanjianKerjasamaMasterAssetDetail.class)
				.createAlias("masterAsset", "masterAsset")

				.add(cari == null || cari.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("masterAsset.kode", cari.getValue().trim()),
								Restrictions.or(Restrictions.ilike("masterAsset.nama", cari.getValue().trim()),
										Restrictions.ilike("masterAsset.nama", cari.getValue().trim(),
												MatchMode.ANYWHERE))))

				.add(Restrictions.eq("perjanjianKerjasamaMasterAsset", perjanjianKerjasamaMasterAsset));
		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	public void display() {

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(this);
		groupbox.appendChild(new MyCaptionStyled("Pemesanan Pengadaan"));

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("40px");
		toolbar.setParent(groupbox);

		new Label(ais.common.Common.getBahasaConfig("Cari")).setParent(toolbar);
		cari = new Textbox();
		cari.setParent(toolbar);
		cari.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		MyToolbarbuttonConfig pencari = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		pencari.setParent(toolbar);
		pencari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Data Barang/Jasa", "/img/add_masterAsset.png");
		button.setDisabled(perjanjianKerjasamaMasterAsset.getDisetujuiOleh() != null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<MasterAsset> masterAssets = session.createCriteria(PerjanjianKerjasamaMasterAssetDetail.class)
						.setProjection(Projections.groupProperty("masterAsset"))
						.add(Restrictions.eq("perjanjianKerjasamaMasterAsset", perjanjianKerjasamaMasterAsset)).list();

				AmbilDataMasterAssetBanyak ambilDataMasterAssetBanyak = new AmbilDataMasterAssetBanyak(masterAssets,
						null);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataMasterAssetBanyak);
				ambilDataMasterAssetBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<MasterAsset> masterAssets = (List<MasterAsset>) arg0.getData();
						Session session = HibernateUtil.currentSession();
						for (MasterAsset masterAsset : masterAssets) {
							PerjanjianKerjasamaMasterAssetDetail perjanjianKerjasamaMasterAssetDetail = new PerjanjianKerjasamaMasterAssetDetail();
							perjanjianKerjasamaMasterAssetDetail.setMasterAsset(masterAsset);
							perjanjianKerjasamaMasterAssetDetail.setJumlah(0.0);
							perjanjianKerjasamaMasterAssetDetail.setKeterangan("");
							perjanjianKerjasamaMasterAssetDetail
									.setPerjanjianKerjasamaMasterAsset(perjanjianKerjasamaMasterAsset);
							session.save(perjanjianKerjasamaMasterAssetDetail);
						}

						loadData(null);
					}
				});
				ambilDataMasterAssetBanyak.setWidth("97%");
				ambilDataMasterAssetBanyak.setHeight("97%");
				ambilDataMasterAssetBanyak.setVisible(true);
				ambilDataMasterAssetBanyak.onModal();
			}

		});
		button.setParent(toolbar);

		new Space().setParent(toolbar);
		new Space().setParent(toolbar);
		new Space().setParent(toolbar);

		new Label("Kode/Nama").setParent(toolbar);
		new Space().setParent(toolbar);
		barcode = new Textbox();
		barcode.setDisabled(perjanjianKerjasamaMasterAsset.getDisetujuiOleh() != null);
		barcode.setParent(toolbar);
		barcode.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadBarcode();
			}
		});

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Cari Kode/Nama", "/img/svg/search.svg");
		cari.setDisabled(perjanjianKerjasamaMasterAsset.getDisetujuiOleh() != null);
		cari.setParent(toolbar);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadBarcode();
			}
		});

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("120px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Satuan Kerja");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Qty");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Harga");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Diskon");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Total");
		column.setAlign("right");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);

		paging.setParent(groupbox);
	}

	public void loadBarcode() throws Exception {
		String barcode = this.barcode.getText().trim();
		if (barcode.trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, field Kode/Nama Barang/Jasa belum diisi. Langkah yang dapat dilakukan: (1) Scan barcode barang atau ketik kode/nama barang/jasa yang dicari; (2) Pastikan kode atau nama sesuai dengan data di master aset; (3) ulangi proses pencarian. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		Session session = HibernateUtil.currentSession();

		MasterAsset masterAsset = (MasterAsset) session.createCriteria(MasterAsset.class)
				.add(Restrictions.or(Restrictions.ilike("kode", barcode, MatchMode.EXACT),
						Restrictions.ilike("nama", barcode, MatchMode.EXACT)))
				.setMaxResults(1).uniqueResult();

		if (masterAsset == null) {
			MyMessageboxConfig.show("Kode/Nama Barang/Jasa tidak ditemukan", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		PerjanjianKerjasamaMasterAssetDetail perjanjianKerjasamaMasterAssetDetail = new PerjanjianKerjasamaMasterAssetDetail();
		perjanjianKerjasamaMasterAssetDetail.setMasterAsset(masterAsset);
		perjanjianKerjasamaMasterAssetDetail.setJumlah(1.0);
		perjanjianKerjasamaMasterAssetDetail.setKeterangan("");
		perjanjianKerjasamaMasterAssetDetail.setPerjanjianKerjasamaMasterAsset(perjanjianKerjasamaMasterAsset);
		session.save(perjanjianKerjasamaMasterAssetDetail);
		loadData(null);
		this.barcode.focus();
		this.barcode.select();
	}

}
