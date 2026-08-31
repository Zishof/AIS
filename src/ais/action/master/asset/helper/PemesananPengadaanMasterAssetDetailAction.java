package ais.action.master.asset.helper;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.A;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
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

import ais.action.master.asset.PemesananPengadaanMasterAssetAction;
import ais.action.master.asset.PenerimaanPengadaanMasterAssetAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.JenisPajakBarang;
import ais.database.model.asset.JenisPajakPpn;
import ais.database.model.asset.MasterAsset;
import ais.database.model.asset.PemesananPengadaanMasterAsset;
import ais.database.model.asset.PemesananPengadaanMasterAssetDetail;
import ais.database.model.asset.PermintaanPengadaanMasterAsset;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk pemesanan pengadaan master asset detail. Tipe ini merupakan titik
 * masuk UI yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi
 * khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code PemesananPengadaanMasterAsset
 * pemesananPengadaanMasterAsset}, {@code MyGrid grid}, {@code boolean edit}, {@code boolean delete}, {@code
 * Paging paging}, {@code Textbox barcode}, {@code Textbox cari}, {@code Footer footerTotalSemua};
 * inisialisasi/lifecycle ({@code initCriteria()}); pembacaan/pencarian ({@code loadData()}, {@code
 * loadBarcode()}); operasi domain lain ({@code display()}); konfigurasi constructor: {@code delete}, {@code
 * edit}, {@code paging}. Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
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
public class PemesananPengadaanMasterAssetDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset;
	private MyGrid grid;
	private boolean edit = false;
	private boolean delete = false;

	private Paging paging;

	private Textbox barcode;

	private Textbox cari;

	private Footer footerTotalSemua;

	private String kodeTermin = null;

	public PemesananPengadaanMasterAssetDetailAction(PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset) {
		this(pemesananPengadaanMasterAsset, null);
	}

	public PemesananPengadaanMasterAssetDetailAction(PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset,
			String kodeTermin) {
		super();
		this.kodeTermin = kodeTermin;
		this.pemesananPengadaanMasterAsset = pemesananPengadaanMasterAsset;
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(PemesananPengadaanMasterAssetDetailAction.this);
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

	class PemesananPengadaanMasterAssetDetailRenderer extends ais.ui.util.MyRowRenderer {

		public PemesananPengadaanMasterAssetDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");
			final PemesananPengadaanMasterAssetDetail pemesananPengadaanMasterAssetDetail = (PemesananPengadaanMasterAssetDetail) data;

			final Label total = new Label(
					Common.numberFormat.get().format(pemesananPengadaanMasterAssetDetail.getHargaTotal()));

			final MyDoublebox jumlah = new MyDoublebox(pemesananPengadaanMasterAssetDetail.getJumlah() == null ? 0.0
					: pemesananPengadaanMasterAssetDetail.getJumlah());

			final MyDoublebox hargaBeli = new MyDoublebox(pemesananPengadaanMasterAssetDetail.getHargaBeli());
			final MyCheckboxConfig diskonDalamBentukPersen = new MyCheckboxConfig("Persen");
			diskonDalamBentukPersen.setChecked(pemesananPengadaanMasterAssetDetail.getDiskonDalamBentukPersen());
			final MyDoublebox hargaPotongan = new MyDoublebox(pemesananPengadaanMasterAssetDetail.getHargaPotongan());

			final Combobox persenPpn = new Combobox();
			Common.insertComboDanSemua(persenPpn, new String[] { "nama" }, "keterangan", JenisPajakPpn.class,
					"Tanpa Pajak", Restrictions.eq("aktif", true));
			Common.selectComboItem(persenPpn, pemesananPengadaanMasterAssetDetail.getJenisPajakPpn());

			final Combobox persenPph = new Combobox();
			Common.insertComboDanSemua(persenPph, new String[] { "nama", "persen" }, "keterangan",
					JenisPajakBarang.class, "Tanpa Pajak", Restrictions.eq("aktif", true));
			Common.selectComboItem(persenPph, pemesananPengadaanMasterAssetDetail.getJenisPajakBarang());

			Vbox a;
			(a = RevisiHelper.createNewRevisi(PemesananPengadaanMasterAssetDetail.class,
					pemesananPengadaanMasterAssetDetail,
					pemesananPengadaanMasterAssetDetail.getMasterAsset() == null ? ""
							: pemesananPengadaanMasterAssetDetail.getMasterAsset().getNama()))
					.setParent(row);
			if (pemesananPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAssetDetail() != null) {
				RevisiHelper.createNewRevisi(PermintaanPengadaanMasterAsset.class,
						pemesananPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAssetDetail()
								.getPermintaanPengadaanMasterAsset(),
						pemesananPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAssetDetail()
								.getPermintaanPengadaanMasterAsset().getKode())
						.setParent(a);
			}

			if (pemesananPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAssetDetail() != null
					&& pemesananPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAssetDetail()
							.getPenerimaanPengadaanMasterAsset() != null) {

				A aa = new A(pemesananPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAssetDetail()
						.getPenerimaanPengadaanMasterAsset().getKode());
				aa.setParent(a);
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						PenerimaanPengadaanMasterAssetAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

							}
						}, pemesananPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAssetDetail()
								.getPenerimaanPengadaanMasterAsset());

					}
				});
				aa.setStyle("font-size:9px;");
			}

			new Label(pemesananPengadaanMasterAssetDetail.getMasterAsset() == null ? ""
					: pemesananPengadaanMasterAssetDetail.getMasterAsset().getKode()).setParent(a);

			SatuanKerja satuanKerja = pemesananPengadaanMasterAssetDetail == null
					|| pemesananPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAssetDetail() == null
					|| pemesananPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAssetDetail()
							.getPermintaanPengadaanMasterAsset() == null ? null
									: pemesananPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAssetDetail()
											.getPermintaanPengadaanMasterAsset().getSatuanKerja();
			if (satuanKerja == null) {
				satuanKerja = pemesananPengadaanMasterAssetDetail.getPemesananPengadaanMasterAsset().getSatuanKerja();
			}

			new Label(satuanKerja == null ? "" : satuanKerja.getNama()).setParent(a);

			boolean persetujuan = pemesananPengadaanMasterAsset.getDisetujuiOleh() != null
					|| pemesananPengadaanMasterAsset.getPembelianLangsung();
			boolean admin = Common.getApakahAdmin();
			if (admin) {
				persetujuan = false;
			}

			if (persetujuan) {
				new MyLabelKecil(Common.numberFormat.get().format(pemesananPengadaanMasterAssetDetail.getJumlah()))
						.setParent(row);
			} else {
				(jumlah).setParent(row);
			}

			if (!admin)
				jumlah.setDisabled(pemesananPengadaanMasterAssetDetail.getPemesananPengadaanMasterAsset()
						.getDisetujuiOleh() != null || !edit);
			jumlah.setStyle("text-align:right");
			jumlah.setWidth("90%");
			jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					pemesananPengadaanMasterAssetDetail.setJumlah(jumlah.getValue());
					Common.refreshUpdate(session, (pemesananPengadaanMasterAssetDetail));

					total.setValue(Common.numberFormat1.get().format(pemesananPengadaanMasterAssetDetail.getHargaTotal()));

					eventListenerHitungUlang.onEvent(null);
				}
			});

			if (!pemesananPengadaanMasterAsset.getPembelianLangsung()) {

				if (persetujuan || !pemesananPengadaanMasterAssetDetail.getMasterAsset().getHargaBolehDiubah()) {
					new MyLabelKecil(Common.numberFormat.get().format(pemesananPengadaanMasterAssetDetail.getHargaBeli()))
							.setParent(row);
				} else {
					(hargaBeli).setParent(row);
				}

				if (!admin)
					hargaBeli.setDisabled(pemesananPengadaanMasterAssetDetail.getPemesananPengadaanMasterAsset()
							.getDisetujuiOleh() != null || !edit);
				hargaBeli.setStyle("text-align:right");
				hargaBeli.setWidth("90%");
				hargaBeli.addEventListener(Events.ON_CHANGE, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						pemesananPengadaanMasterAssetDetail.setHargaBeli(hargaBeli.getValue());
						Common.refreshUpdate(session, (pemesananPengadaanMasterAssetDetail));

						MasterAsset masterAsset = pemesananPengadaanMasterAssetDetail.getMasterAsset();
						session.refresh(masterAsset);
						masterAsset.setHargaBeliDefault(hargaBeli.getValue());
						Common.refreshUpdate(session, masterAsset);

						total.setValue(Common.numberFormat1.get().format(pemesananPengadaanMasterAssetDetail.getHargaTotal()));

						eventListenerHitungUlang.onEvent(null);
					}
				});

				if (persetujuan) {
					new Label(pemesananPengadaanMasterAssetDetail.getDiskonDalamBentukPersen() ? "Ya" : "Tidak")
							.setParent(row);
				} else {
					(diskonDalamBentukPersen).setParent(row);
				}

				diskonDalamBentukPersen.setDisabled(pemesananPengadaanMasterAssetDetail
						.getPemesananPengadaanMasterAsset().getDisetujuiOleh() != null || !edit);
				diskonDalamBentukPersen.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						pemesananPengadaanMasterAssetDetail
								.setDiskonDalamBentukPersen(diskonDalamBentukPersen.isChecked());
						Common.refreshUpdate(session, (pemesananPengadaanMasterAssetDetail));

						total.setValue(Common.numberFormat1.get().format(pemesananPengadaanMasterAssetDetail.getHargaTotal()));

						eventListenerHitungUlang.onEvent(null);
					}
				});

				if (persetujuan) {
					new MyLabelKecil(Common.numberFormat.get().format(pemesananPengadaanMasterAssetDetail.getHargaPotongan()))
							.setParent(row);
				} else {
					(hargaPotongan).setParent(row);
				}

				if (!admin)
					hargaPotongan.setDisabled(pemesananPengadaanMasterAssetDetail.getPemesananPengadaanMasterAsset()
							.getDisetujuiOleh() != null || !edit);
				hargaPotongan.setStyle("text-align:right");
				hargaPotongan.setWidth("90%");
				hargaPotongan.addEventListener(Events.ON_CHANGE, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						pemesananPengadaanMasterAssetDetail.setHargaPotongan(hargaPotongan.getValue());
						Common.refreshUpdate(session, (pemesananPengadaanMasterAssetDetail));

						total.setValue(Common.numberFormat1.get().format(pemesananPengadaanMasterAssetDetail.getHargaTotal()));

						eventListenerHitungUlang.onEvent(null);
					}
				});

				if (persetujuan) {
					new Label(pemesananPengadaanMasterAssetDetail.getJenisPajakPpn() == null ? ""
							: pemesananPengadaanMasterAssetDetail.getJenisPajakPpn().getNama()).setParent(row);
				} else {
					(persenPpn).setParent(row);
				}

				if (!admin)
					persenPpn.setDisabled(pemesananPengadaanMasterAssetDetail.getPemesananPengadaanMasterAsset()
							.getDisetujuiOleh() != null || !edit);
				persenPpn.setStyle("text-align:right");
				persenPpn.setWidth("90%");
				persenPpn.addEventListener(Events.ON_CHANGE, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						pemesananPengadaanMasterAssetDetail
								.setJenisPajakPpn((JenisPajakPpn) (persenPpn.getSelectedItem() == null ? null
										: persenPpn.getSelectedItem().getValue()));
						if (pemesananPengadaanMasterAssetDetail.getId() != null) {
							Common.refreshUpdate(session, (pemesananPengadaanMasterAssetDetail));
						}

						total.setValue(Common.numberFormat1.get().format(pemesananPengadaanMasterAssetDetail.getHargaTotal()));

						eventListenerHitungUlang.onEvent(null);
					}
				});

				if (persetujuan) {
					new Label(pemesananPengadaanMasterAssetDetail.getJenisPajakBarang() == null ? ""
							: pemesananPengadaanMasterAssetDetail.getJenisPajakBarang().getNama()).setParent(row);
				} else {
					(persenPph).setParent(row);
				}

				if (!admin)
					persenPph.setDisabled(pemesananPengadaanMasterAssetDetail.getPemesananPengadaanMasterAsset()
							.getDisetujuiOleh() != null || !edit);
				persenPph.setStyle("text-align:right");
				persenPph.setWidth("90%");
				persenPph.addEventListener(Events.ON_CHANGE, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						pemesananPengadaanMasterAssetDetail
								.setJenisPajakBarang((JenisPajakBarang) (persenPph.getSelectedItem() == null ? null
										: persenPph.getSelectedItem().getValue()));
						Common.refreshUpdate(session, (pemesananPengadaanMasterAssetDetail));

						total.setValue(Common.numberFormat1.get().format(pemesananPengadaanMasterAssetDetail.getHargaTotal()));

						eventListenerHitungUlang.onEvent(null);
					}
				});

			}

			new Label(Common.numberFormat.get().format(pemesananPengadaanMasterAssetDetail.hitungPpn())).setParent(row);
			new Label(Common.numberFormat.get().format(pemesananPengadaanMasterAssetDetail.hitungPph())).setParent(row);

			total.setStyle("text-align:right");
			total.setParent(row);

			final MyTextbox keterangan = new MyTextbox(pemesananPengadaanMasterAssetDetail.getKeterangan() == null ? ""
					: pemesananPengadaanMasterAssetDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			if (persetujuan) {
				new MyLabelKecil(pemesananPengadaanMasterAssetDetail.getKeterangan()).setParent(row);
			} else {
				keterangan.setParent(row);
			}

			if (!admin)
				keterangan.setDisabled(pemesananPengadaanMasterAssetDetail.getPemesananPengadaanMasterAsset()
						.getDisetujuiOleh() != null || !edit);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					pemesananPengadaanMasterAssetDetail.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (pemesananPengadaanMasterAssetDetail));

					eventListenerHitungUlang.onEvent(null);
				}
			});

			Hbox toolbar = new Hbox();
			if (!persetujuan || admin) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");

				if (!admin)
					button.setDisabled(pemesananPengadaanMasterAssetDetail.getPemesananPengadaanMasterAsset()
							.getDisetujuiOleh() != null || !delete);
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

												Common.refreshDelete(pemesananPengadaanMasterAssetDetail);

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
			}
			toolbar.setParent(row);

		}
	}

	private List<PemesananPengadaanMasterAssetDetail> pemesananPengadaanMasterAssetDetails = null;

	@SuppressWarnings("unchecked")
	public void loadData(Object value) throws Exception {
		Common.initPaging(initCriteria(false), paging);
		pemesananPengadaanMasterAssetDetails = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(pemesananPengadaanMasterAssetDetails);
		grid.setRowRenderer(new PemesananPengadaanMasterAssetDetailRenderer());
		grid.setModelCheckMobile(strset);

		eventListenerHitungUlang.onEvent(null);
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(PemesananPengadaanMasterAssetDetail.class)
				.createAlias("masterAsset", "masterAsset")

				.add(cari == null || cari.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("masterAsset.kode", cari.getValue().trim()),
								Restrictions.or(Restrictions.ilike("masterAsset.nama", cari.getValue().trim()),
										Restrictions.ilike("masterAsset.nama", cari.getValue().trim(),
												MatchMode.ANYWHERE))))

				.add(Restrictions.eq("pemesananPengadaanMasterAsset", pemesananPengadaanMasterAsset));
		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	public void display() throws Exception {

		MyGroupboxStyled groupbox = new MyGroupboxStyled();
		groupbox.setStyleLangsung("min-height: 300px;");
		groupbox.setParent(this);
		groupbox.appendChild(new MyCaptionStyled("Pemesanan Barang/Jasa"));

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
		if (pemesananPengadaanMasterAsset.getTampaPermintaan()) {
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Data Barang/Jasa",
					"/img/add_masterAsset.png");
			button.setDisabled(pemesananPengadaanMasterAsset.getDisetujuiOleh() != null);
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {
					Session session = HibernateUtil.currentSession();

					List<MasterAsset> masterAssets = session.createCriteria(PemesananPengadaanMasterAssetDetail.class)
							.setProjection(Projections.groupProperty("masterAsset"))
							.add(Restrictions.eq("pemesananPengadaanMasterAsset", pemesananPengadaanMasterAsset))
							.list();

					AmbilDataMasterAssetBanyak ambilDataMasterAssetBanyak = new AmbilDataMasterAssetBanyak(masterAssets,
							null);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
							.appendChild(ambilDataMasterAssetBanyak);
					ambilDataMasterAssetBanyak.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							List<MasterAsset> masterAssets = (List<MasterAsset>) arg0.getData();
							Session session = HibernateUtil.currentSession();
							for (MasterAsset masterAsset : masterAssets) {
								PemesananPengadaanMasterAssetDetail pemesananPengadaanMasterAssetDetail = new PemesananPengadaanMasterAssetDetail();
								pemesananPengadaanMasterAssetDetail.setMasterAsset(masterAsset);
								pemesananPengadaanMasterAssetDetail.setJumlah(0.0);
								pemesananPengadaanMasterAssetDetail.setKeterangan("");
								pemesananPengadaanMasterAssetDetail
										.setPemesananPengadaanMasterAsset(pemesananPengadaanMasterAsset);
								session.save(pemesananPengadaanMasterAssetDetail);
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
		}

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		button.setVisible(pemesananPengadaanMasterAsset.getId() != null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				RevisiPemesananPengadaanMasterAssetDetailHelper revisiHelper = new RevisiPemesananPengadaanMasterAssetDetailHelper(
						pemesananPengadaanMasterAsset, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(null);
							}
						});
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();

			}

		});
		button.setParent(toolbar);

		new Space().setParent(toolbar);
		new Space().setParent(toolbar);
		new Space().setParent(toolbar);

		new Label("Kode/Nama").setParent(toolbar);
		new Space().setParent(toolbar);
		barcode = new Textbox();
		barcode.setDisabled(pemesananPengadaanMasterAsset.getDisetujuiOleh() != null);
		barcode.setParent(toolbar);
		barcode.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadBarcode();
			}
		});

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Cari Kode/Nama", "/img/svg/search.svg");
		cari.setDisabled(pemesananPengadaanMasterAsset.getDisetujuiOleh() != null);
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
		column.setLabel("Nama");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Qty");
		column.setWidth("5%");

		if (!pemesananPengadaanMasterAsset.getPembelianLangsung()) {
			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Harga");
			column.setWidth("10%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Persen");
			column.setWidth("5%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Diskon");
			column.setWidth("8%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("PPN");
			column.setWidth("10%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("PPH");
			column.setWidth("5%");

		}
		
		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("N.PPN");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("N.PPH");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Total");
		column.setAlign("right");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		footerTotalSemua = new Footer(Common.numberFormat.get().format(pemesananPengadaanMasterAsset.getNilai()));

		Foot foot = new Foot();
		foot.setParent(grid);

		Footer footer = new Footer("Total");
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		if (!pemesananPengadaanMasterAsset.getPembelianLangsung()) {

			footer = new Footer();
			foot.appendChild(footer);

			footer = new Footer();
			foot.appendChild(footer);

			footer = new Footer();
			foot.appendChild(footer);

			footer = new Footer();
			foot.appendChild(footer);

			footer = new Footer();
			foot.appendChild(footer);

		}

		footer = new Footer();
		foot.appendChild(footer);
		
		footer = new Footer();
		foot.appendChild(footer);

		foot.appendChild(footerTotalSemua);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		loadData(null);

		paging.setParent(groupbox);

		if (pemesananPengadaanMasterAsset.getByTermin()) {
			JSONArray array = new JSONArray(pemesananPengadaanMasterAsset.getFormula());
			Row rowFormula = Common.tampilanScroll1(groupbox);
			PemesananPengadaanMasterAssetAction.reloadFormula(rowFormula, array, false, pemesananPengadaanMasterAsset,
					kodeTermin);
		}
	}

	public EventListener eventListenerHitungUlang = new EventListener() {

		@Override
		public void onEvent(Event arg0) throws Exception {

			Double totalSemua = 0.0;
			for (PemesananPengadaanMasterAssetDetail pemesananPengadaanMasterAssetDetail : pemesananPengadaanMasterAssetDetails) {
				Double j = pemesananPengadaanMasterAssetDetail == null ? 0.0
						: pemesananPengadaanMasterAssetDetail.getHargaTotal();

				totalSemua += j;
			}

			if (pemesananPengadaanMasterAsset.getNilai().intValue() != totalSemua.intValue()) {
				pemesananPengadaanMasterAsset.setNilai(totalSemua);
				Common.refreshUpdate(pemesananPengadaanMasterAsset);
			}

			footerTotalSemua.setLabel(Common.numberFormat.get().format(totalSemua));
		}
	};

	public void loadBarcode() throws Exception {
		String barcode = this.barcode.getText().trim();
		if (barcode.trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, field Kode/Nama Barang/Jasa belum diisi. Langkah yang dapat dilakukan: (1) Scan barcode barang atau ketik kode/nama barang/jasa yang dicari; (2) Pastikan kode atau nama sesuai dengan data di master aset; (3) ulangi proses pencarian barang. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
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

		PemesananPengadaanMasterAssetDetail pemesananPengadaanMasterAssetDetail = new PemesananPengadaanMasterAssetDetail();
		pemesananPengadaanMasterAssetDetail.setMasterAsset(masterAsset);
		pemesananPengadaanMasterAssetDetail.setJumlah(1.0);
		pemesananPengadaanMasterAssetDetail.setKeterangan("");
		pemesananPengadaanMasterAssetDetail.setPemesananPengadaanMasterAsset(pemesananPengadaanMasterAsset);
		session.save(pemesananPengadaanMasterAssetDetail);
		loadData(null);
		this.barcode.focus();
		this.barcode.select();
	}

}
