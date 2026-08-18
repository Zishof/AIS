package ais.action.master.asset.helper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.asset.LokasiAction;
import ais.action.master.asset.util.AssetUtil;
import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.report.Report;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Ruang;
import ais.database.model.Tbmuser;
import ais.database.model.asset.Asset;
import ais.database.model.asset.AssetDetail;
import ais.database.model.asset.Lokasi;
import ais.database.model.asset.PemilikAsset;
import ais.database.model.asset.StatusAsset;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeFactory;
import net.sourceforge.barbecue.BarcodeImageHandler;

public class AssetDetailAction extends MyDetail implements DataCriteria, DataSearchDefault {

	/** 
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private Asset asset;
	private MyGrid grid;

	private boolean add = false;
	private boolean edit = false;
	private boolean delete = false;

	private EventListener eventListener;
	private List<AssetDetail> assetDetails;

	private Combobox searchlokasi;
	private AmbilDataRuangBanbox searchruang;
	private AmbilDataSatuanKerjaBanbox searchparent;

	private PemilikAsset selectedPemilikAsset;
	private Combobox searchpemilikAsset;
	private LampiranLain gambar = null;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	private Lokasi selectedLokasi = null;

	private Ruang selectedRuang = null;

	private Textbox nama;

	private SatuanKerja satuanKerjaData = null;
	private Lokasi lokasiData = null;
	private Ruang ruangData = null;

	public AssetDetailAction(Asset asset, SatuanKerja satuanKerjaData, Lokasi lokasiData, Ruang ruangData,
			EventListener eventListener) {
		super();
		this.satuanKerjaData = satuanKerjaData;
		this.lokasiData = lokasiData;
		this.ruangData = ruangData;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		this.asset = asset;

		if (Sessions.getCurrent().getAttribute("PemilikAsset") != null) {
			selectedPemilikAsset = (PemilikAsset) Sessions.getCurrent().getAttribute("PemilikAsset");
			Sessions.getCurrent().removeAttribute("PemilikAsset");
		}

		if (Sessions.getCurrent().getAttribute("Lokasi") != null) {
			selectedLokasi = (Lokasi) Sessions.getCurrent().getAttribute("Lokasi");
			Sessions.getCurrent().removeAttribute("Lokasi");
		}

		if (Sessions.getCurrent().getAttribute("Ruang") != null) {
			selectedRuang = (Ruang) Sessions.getCurrent().getAttribute("Ruang");
			Sessions.getCurrent().removeAttribute("Ruang");
		}

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		gambar = LampiranLain.ambil(asset.getMasterAsset().getId(), LampiranLain.GAMBAR_MASTER_ASSET);
		this.eventListener = eventListener;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (getChildren().isEmpty()) {
					display();
				}
			}
		});
	}

	class AssetDetailRenderer extends ais.ui.util.MyRowRenderer {

		public AssetDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");
			final AssetDetail assetDetail = (AssetDetail) data;

			final AmbilDataSatuanKerjaBanbox satuanKerjaBanbox = new AmbilDataSatuanKerjaBanbox();
			satuanKerjaBanbox.setAttribute("satuanKerja", assetDetail.getSatuanKerja());
			satuanKerjaBanbox.setAttribute("myValue", assetDetail.getSatuanKerja());
			satuanKerjaBanbox
					.setValue(assetDetail.getSatuanKerja() == null ? "" : assetDetail.getSatuanKerja().getNama());

			satuanKerjaBanbox.setDisabled(!edit);
			satuanKerjaBanbox.setWidth("90%");
			satuanKerjaBanbox.setParent(row);
			satuanKerjaBanbox.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					assetDetail.setSatuanKerja((SatuanKerja) satuanKerjaBanbox.getAttribute("satuanKerja"));
					Common.refreshUpdate(session, (assetDetail));
				}
			});

			final Combobox lokasi = new Combobox();
			Common.insertComboDanSemua(lokasi, new String[] { "nama" }, "alamat", Lokasi.class, "Tanpa Lokasi",
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			Common.selectComboItem(lokasi, assetDetail.getLokasi());
			lokasi.setDisabled(!edit);
			lokasi.setWidth("90%");
			lokasi.setParent(row);
			lokasi.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					assetDetail.setLokasi(
							(Lokasi) (lokasi.getSelectedItem() == null ? null : lokasi.getSelectedItem().getValue()));
					Common.refreshUpdate(session, (assetDetail));
				}
			});

			final Combobox pemilikAsset = new Combobox();
			Common.insertComboDanSemua(pemilikAsset, new String[] { "nama" }, "keterangan", PemilikAsset.class,
					"Tanpa Pemilik", Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			Common.selectComboItem(pemilikAsset, assetDetail.getPemilikAsset());
			pemilikAsset.setDisabled(!edit);
			pemilikAsset.setWidth("90%");
			pemilikAsset.setParent(row);
			pemilikAsset.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					assetDetail.setPemilikAsset((PemilikAsset) (pemilikAsset.getSelectedItem() == null ? null
							: pemilikAsset.getSelectedItem().getValue()));
					Common.refreshUpdate(session, (assetDetail));
				}
			});

			final AmbilDataRuangBanbox ruangBanbox = new AmbilDataRuangBanbox();
			ruangBanbox.setAttribute("ruang", assetDetail.getRuang());
			ruangBanbox.setAttribute("myValue", assetDetail.getRuang());
			ruangBanbox.setValue(assetDetail.getRuang() == null ? "" : assetDetail.getRuang().getNama());

			ruangBanbox.setDisabled(!edit);
			ruangBanbox.setWidth("90%");
			ruangBanbox.setParent(row);
			ruangBanbox.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					assetDetail.setRuang((Ruang) ruangBanbox.getAttribute("ruang"));
					Common.refreshUpdate(session, (assetDetail));
				}
			});

			final Textbox barcode = new Textbox(assetDetail.getBarcode());
			barcode.setDisabled(!edit);
			barcode.setWidth("90%");
			barcode.setParent(row);
			barcode.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					assetDetail.setBarcode(barcode.getValue().trim());
					Common.refreshUpdate(session, (assetDetail));
				}
			});

			final MyTextbox nama = new MyTextbox(assetDetail.getNama() == null ? "" : assetDetail.getNama());
			nama.setWidth("90%");
			nama.setHeight("95%");
			nama.setParent(row);
			nama.setDisabled(!edit);
			nama.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					assetDetail.setNama(nama.getValue());
					Common.refreshUpdate(session, (assetDetail));
				}
			});

			final Combobox status = new Combobox();
			Common.insertCombo(status, "nama", StatusAsset.class);
			Common.selectComboItem(status, assetDetail.getStatusAsset());

			if (assetDetail.getPenghapusanMasterAssetDetail() != null) {
				status.setDisabled(true);
			}

			status.setWidth("90%");
			status.setHeight("95%");
			status.setParent(row);
			status.setDisabled(!edit);
			status.setReadonly(true);
			status.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					assetDetail.setStatusAsset((StatusAsset) (status.getSelectedItem() == null
							|| status.getSelectedItem().getValue() == null ? null
									: status.getSelectedItem().getValue()));
					Common.refreshUpdate(session, (assetDetail));
				}
			});

			Date tglB = null;
			if (assetDetail.getAsset() != null
					&& assetDetail.getAsset().getPermintaanPengadaanMasterAssetDetail() != null
					&& assetDetail.getAsset().getPermintaanPengadaanMasterAssetDetail().getUangMuka() != null
					&& assetDetail.getAsset().getPermintaanPengadaanMasterAssetDetail().getUangMuka()
							.getPertangungjawaban() != null
					&& assetDetail.getAsset().getPermintaanPengadaanMasterAssetDetail().getUangMuka()
							.getPertangungjawaban().getTanggalPersetujuan() != null) {
				tglB = assetDetail.getAsset().getPermintaanPengadaanMasterAssetDetail().getUangMuka()
						.getPertangungjawaban().getTanggalPersetujuan();
			} else if (assetDetail.getAsset() != null && assetDetail.getAsset().getSaldoAwalMasterAssetDetail() != null
					&& assetDetail.getAsset().getSaldoAwalMasterAssetDetail()
							.getPenerimaanPengadaanMasterAssetDetail() != null
					&& assetDetail.getAsset().getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail()
							.getPenerimaanPengadaanMasterAsset() != null
					&& assetDetail.getAsset().getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail()
							.getPenerimaanPengadaanMasterAsset().getTanggalPersetujuan() != null) {
				tglB = assetDetail.getAsset().getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail()
						.getPenerimaanPengadaanMasterAsset().getTanggalPersetujuan();
			}

			if (tglB != null) {
				new MyLabelAgakKecil(Common.numberFormat.get().format(assetDetail.getHargaBeli())).setParent(row);
			} else {

				final MyDoublebox hargaBeli = new MyDoublebox(assetDetail.getHargaBeli());
				hargaBeli.setWidth("90%");
				hargaBeli.setHeight("95%");
				hargaBeli.setParent(row);
				hargaBeli.setDisabled(!edit);
				hargaBeli.addEventListener(Events.ON_CHANGE, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						assetDetail.setHargaBeli(hargaBeli.getValue());
						Common.refreshUpdate(session, (assetDetail));
					}
				});
			}

			if (tglB != null) {
				new MyLabelAgakKecil(Common.dateFormat2.get().format(tglB)).setParent(row);
			} else {
				final MyDatebox tanggalBeli = new MyDatebox(assetDetail.getTanggalBeli());
				tanggalBeli.setWidth("90%");
				tanggalBeli.setHeight("95%");
				tanggalBeli.setParent(row);
				tanggalBeli.setDisabled(!edit);
				tanggalBeli.addEventListener(Events.ON_CHANGE, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						assetDetail.setTanggalBeli(tanggalBeli.getValue());
						Common.refreshUpdate(session, (assetDetail));
					}
				});
				tanggalBeli.setReadonly(true);
			}

			final MyDoublebox nilaiMinimal = new MyDoublebox(assetDetail.getNilaiMinimal());
			nilaiMinimal.setWidth("90%");
			nilaiMinimal.setHeight("95%");
			nilaiMinimal.setParent(row);
			nilaiMinimal.setDisabled(!edit);
			nilaiMinimal.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					assetDetail.setNilaiMinimal(nilaiMinimal.getValue());
					Common.refreshUpdate(session, (assetDetail));
				}
			});

			final MyDoublebox umurEkonomis = new MyDoublebox(assetDetail.getUmurEkonomis());
			umurEkonomis.setWidth("90%");
			umurEkonomis.setHeight("95%");
			umurEkonomis.setParent(row);
			umurEkonomis.setDisabled(!edit);
			umurEkonomis.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					assetDetail.setUmurEkonomis(umurEkonomis.getValue());
					Common.refreshUpdate(session, (assetDetail));
				}
			});

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Sarana Bersama");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(assetDetail.getSaranaBersama());
			checkbox.setParent(row);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					assetDetail.setSaranaBersama(checkbox.isChecked());
					Common.refreshSaveOrUpdate(assetDetail);
				}
			});

			final MyTextbox keterangan = new MyTextbox(
					assetDetail.getKeterangan() == null ? "" : assetDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setParent(row);
			keterangan.setDisabled(!edit);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					assetDetail.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (assetDetail));
				}
			});

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setDisabled(!delete);
			button.setVisible(assetDetail.getAsset().getSaldoAwalMasterAssetDetail() == null);
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
											Common.refreshDelete(assetDetail);
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
	public void loadData(Object value) throws Exception {
		assetDetails = asset == null ? new ArrayList<AssetDetail>() : initCriteria(true).list();

		ListModel strset = new SimpleListModel(assetDetails);
		grid.setRowRenderer(new AssetDetailRenderer());
		grid.setModelCheckMobile(strset);

		eventListener.onEvent(new Event("", grid, assetDetails));
	}

	public void display() throws Exception {

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(this);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(groupbox);
		new MyLabelAgakKecil("Kode/Nama").setParent(toolbar);
		nama = new Textbox();
		nama.setCols(5);
		nama.setParent(toolbar);
		nama.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		Common.insertComboDanSemua(searchpemilikAsset = new Combobox(), new String[] { "nama", "id" }, "keterangan",
				PemilikAsset.class, "Tanpa Pemilik",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		searchpemilikAsset.setCols(5);
		searchpemilikAsset.setParent(toolbar);
		searchpemilikAsset.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		if (lokasiData != null) {
			Common.selectComboItem(true, searchlokasi, lokasiData);
			searchlokasi.setDisabled(true);
		}

		searchlokasi = new Combobox();
		Common.insertComboDanSemua(searchlokasi, new String[] { "nama" }, "alamat", Lokasi.class, "Lokasi",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		searchlokasi.setCols(5);
		searchlokasi.setParent(toolbar);
		searchlokasi.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		if (lokasiData != null) {
			Common.selectComboItem(true, searchlokasi, lokasiData);
			searchlokasi.setDisabled(true);
		}

		searchparent = new AmbilDataSatuanKerjaBanbox();
		searchparent.setValue("Satuan Kerja");
		searchparent.setCols(5);
		searchparent.setParent(toolbar);
		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		Tbmuser tbmuser = Common.getCurrentUser();
		if (satuanKerjaData != null && tbmuser != null && tbmuser.hakAkses() != null
				&& !tbmuser.hakAkses().getMelihatDataSatkerLain()) {
			searchparent.setValue(satuanKerjaData.getNama());
			searchparent.setAttribute("satuanKerja", satuanKerjaData);
			searchparent.setAttribute("myValue", satuanKerjaData);
			searchparent.setDisabled(true);
		}

		searchruang = new AmbilDataRuangBanbox();
		searchruang.setValue("Ruang");
		searchruang.setCols(5);
		searchruang.setParent(toolbar);

		LokasiAction.kunciLokasi(searchlokasi);

		searchruang.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		if (selectedPemilikAsset != null) {
			Common.selectComboItem(searchpemilikAsset, Sessions.getCurrent().getAttribute("PemilikAsset"));
			searchpemilikAsset.setDisabled(true);
		}
		if (selectedLokasi != null) {
			Common.selectComboItem(searchlokasi, selectedLokasi);
			searchlokasi.setDisabled(true);
		}
		if (selectedLokasi != null) {
			searchruang.setValue(selectedRuang == null ? "" : (selectedRuang.getKodeRuangan()));
			searchruang.setAttribute("ruang", selectedRuang);
			searchruang.setDisabled(true);
		}

		if (ruangData != null) {
			searchruang.setValue(ruangData == null ? "" : (ruangData.getKodeRuangan()));
			searchruang.setAttribute("ruang", ruangData);
			searchruang.setValue(ruangData.getNama());
			searchruang.setDisabled(true);
		}

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah", "/img/add_item.png");
		button.setDisabled(asset.getDisetujuiOleh() != null && add);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				final MyWindow window = new MyWindow("Tambah Barang/Sarpras", "none", true);
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				window.setHeight("240px");
				window.setWidth("390px");

				final Intbox jumlahAsset = new Intbox(0);

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);
				Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);

				Columns columns = new Columns();
				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("40%");

				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Barang/Sarpras"));
				row.appendChild(jumlahAsset);
				jumlahAsset.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pengadaan / Pembelian"));
				final MyDatebox tanggalBeli;
				row.appendChild(tanggalBeli = new MyDatebox(ais.ui.util.WaktuUtil.getDate()));

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Harga Pengadaan / Pembelian"));
				final MyDoublebox hargaBeli;
				row.appendChild(hargaBeli = new MyDoublebox(0.0));
				hargaBeli.setWidth("90%");

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
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
				save.setTooltiptext("Simpan");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						for (int i = 0; i < jumlahAsset.getValue(); i++) {
							try {
								AssetDetail assetDetail = new AssetDetail();
								assetDetail.setNama(asset.getNama());
								assetDetail.setKeterangan(asset.getKeterangan());
								assetDetail.setStatusAsset(AssetUtil.AKTIF);
								assetDetail.setAsset(asset);
								assetDetail.setHargaBeli(hargaBeli.getValue());
								assetDetail.setTanggalBeli(tanggalBeli.getValue());

								if (assetDetail.getLat() == null) {
									if (assetDetail.getLokasi() != null) {
										assetDetail.setLat(assetDetail.getLokasi().getLat());
										assetDetail.setAlamat(assetDetail.getLokasi().getAlamat());
										assetDetail.setDetailAlamat(assetDetail.getLokasi().getDetailAlamat());
									} else {
										String strLat = Common.getKonfigurasi("default_lat", Konfigurasi.AKTIF,
												"" + -6.195168, "", "").getInfo1();
										assetDetail.setLat(Double.parseDouble(strLat));
									}
								}

								if (assetDetail.getLng() == null) {
									if (assetDetail.getLokasi() != null) {
										assetDetail.setLng(assetDetail.getLokasi().getLng());
										assetDetail.setAlamat(assetDetail.getLokasi().getAlamat());
										assetDetail.setDetailAlamat(assetDetail.getLokasi().getDetailAlamat());
									} else {
										String strLng = Common.getKonfigurasi("default_lng", Konfigurasi.AKTIF,
												"" + 106.846046, "", "").getInfo1();
										assetDetail.setLng(Double.parseDouble(strLng));
									}
								}
								assetDetail.setBarcode(AssetDetail.generateBarcode(assetDetail, null, true));

								Session session = HibernateUtil.currentNativeSession();
								session.getTransaction().begin();
								session.save(assetDetail);
								session.getTransaction().commit();
								// session.disconnect();
								if (session.isOpen()) {session.disconnect();session.close();}
								HibernateUtil.closeSession();
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/asset/helper/AssetDetailAction.java:699");
							}
						}

						loadData(null);
						window.detach();
					}
				});
				save.setParent(toolbar);

				window.onModal();
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
		button.setParent(toolbar);
		button.setDisabled(asset.getDisetujuiOleh() != null && delete);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									String sql = "delete from asset.asset_detail where asset = " + asset.getId();

									HibernateUtil.currentSession().createSQLQuery(sql).executeUpdate();
									loadData(null);

								}

							}
						});
			}
		});

		String[] contents = new String[] { "id", "barcode", "nama", "lokasi", "ruang", "satuanKerja", "hargaBeli",
				"tanggalBeli", "nilaiMinimal", "umurEkonomis", "statusAsset", "lat", "lng", "alamat", "detailAlamat",
				"keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = Common.uploadData(this, AssetDetail.class, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				AssetDetail d = (AssetDetail) ((Object[]) arg0.getData())[0];
				d.setAsset(asset);

				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.update(d);
				session.getTransaction().commit();
				HibernateUtil.closeSession();
			}
		}, contents);
		upload.setVisible(add && edit && delete);
		toolbar.appendChild(upload);

		button = new MyToolbarbuttonConfig("Barcode", "/img/print.png");
		button.setParent(toolbar);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				@SuppressWarnings("rawtypes")
				final Map parameters = ais.common.HashMapGenerator.getRand();
				List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();
				for (AssetDetail assetDetail : assetDetails) {
					Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();
					map.put("status",
							assetDetail.getStatusAsset() == null ? "" : assetDetail.getStatusAsset().getNama());
					map.put("judul", assetDetail.getNama());

					final File myfilebarcode = new File(
							Common.ambilREAL_PATH_REPORT() + "/barcode_" + assetDetail.getBarcode() + ".png");

					Barcode mybarcode = BarcodeFactory.createCode128B(assetDetail.getBarcode());
					BarcodeImageHandler.savePNG(mybarcode, myfilebarcode);
					map.put("barcode", myfilebarcode.getAbsolutePath());
					map.put("barcode_data", assetDetail.getBarcode());
					maps.add(map);
				}
				Report.generatePDFReport(Report.PDF, parameters, "asset/barcode_asset", ais.ui.util.WaktuUtil.getDate(),
						maps);
			}
		});

		button = new MyToolbarbuttonConfig("QRcode", "/img/print.png");
		button.setParent(toolbar);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void onEvent(Event arg0) throws Exception {

				String lokasigambar = null;
				if (gambar != null) {
					lokasigambar = FileFotoLain.ambilLinkLampiranLain(gambar, false, false, LampiranLain.class);
				}

				Session session = HibernateUtil.currentSession();
				assetDetails = asset == null ? new ArrayList<AssetDetail>()
						: session.createCriteria(AssetDetail.class).addOrder(Order.desc("id"))
								.add(Restrictions.eq("asset", asset)).list();

				Map parameters = ais.common.HashMapGenerator.getRand();
				List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();
				for (AssetDetail assetDetail : assetDetails) {
					Map map = new java.util.HashMap();
					map.put("status",
							assetDetail.getStatusAsset() == null ? "" : assetDetail.getStatusAsset().getNama());
					map.put("judul", assetDetail.getNama());

					File myfilebarcode = new File(
							Common.ambilREAL_PATH_REPORT() + "/barcode_" + assetDetail.getBarcode() + ".png");

					Barcode mybarcode = BarcodeFactory.createCode128B(assetDetail.getBarcode());
					BarcodeImageHandler.savePNG(mybarcode, myfilebarcode);
					map.put("barcode", myfilebarcode.getAbsolutePath());
					map.put("barcode_data", assetDetail.getBarcode());
					map.put("ruang", assetDetail.getRuang() == null ? ""
							: assetDetail.getRuang().getKodeRuangan() + "-" + assetDetail.getRuang().getNama());
					map.put("tanggal", assetDetail.getTanggalBeli());
					map.put("tanggal_format", Common.dateFormat1.get().format(assetDetail.getTanggalBeli()));

					map.put("gambar", lokasigambar);

					Common.insertProperty(AssetDetail.class, assetDetail, map, "");

					String code = assetDetail.getBarcode() + "\n" + assetDetail.getNama() + "\n"
							+ (assetDetail.getRuang() == null ? ""
									: assetDetail.getRuang().getKodeRuangan() + "-" + assetDetail.getRuang().getNama()
											+ "\n")
							+ Common.dateFormat1.get().format(assetDetail.getTanggalBeli()) + "\n"
							+ (assetDetail.getSatuanKerja() == null ? "" : assetDetail.getSatuanKerja().getNama());

					myfilebarcode = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_" + assetDetail.getBarcode() + ".png");

					BarcodeCommon.generateCRCode(code, myfilebarcode);
					map.put("cr_code", myfilebarcode.getAbsolutePath());

					maps.add(map);
				}
				Common.insertProperty(Asset.class, asset, parameters, "", 3);
				
				Report.generatePDFReport(Report.PDF, parameters, "asset/crcode_asset", ais.ui.util.WaktuUtil.getDate(),
						maps);
			}
		});

		button = new MyToolbarbuttonConfig("Re-code", "/img/svg/process.svg");
		button.setParent(toolbar);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings({ "unchecked" })
			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin membuatkan kode ulang  ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									Common.createDefaultTimer(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											Session sessionData = HibernateUtil.currentNativeSession();
											List<AssetDetail> assetDetails = asset == null
													? new ArrayList<AssetDetail>()
													: sessionData.createCriteria(AssetDetail.class)
															.addOrder(Order.asc("id"))
															.add(Restrictions.eq("asset", asset)).list();
											sessionData.disconnect();
											sessionData.close();
											HibernateUtil.closeSession();

											int index = 1;
											for (AssetDetail assetDetail : assetDetails) {

												assetDetail.setBarcode(
														AssetDetail.generateBarcode(assetDetail, index, true));

												sessionData = HibernateUtil.currentNativeSession();
												sessionData.getTransaction().begin();
												Common.refreshUpdate(sessionData, assetDetail);
												sessionData.getTransaction().commit();
												sessionData.disconnect();
												sessionData.close();
												HibernateUtil.closeSession();

												index++;
											}
											assetDetails.clear();
											assetDetails = null;
											loadData(null);

										}
									});

								}

							}
						});

			}
		});

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Satker");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Lokasi");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pemilik");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Ruang");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Barcode");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("12%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Status");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Harga");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tanggal");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai Buku");
		column.setWidth("6%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Masa Pakai");
		column.setWidth("6%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sarpras?");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("4%");

		loadData(null);
	}

	@Override
	public Criteria initCriteria(boolean order) {

		Ruang ruang = (Ruang) searchruang.getAttribute("ruang");

		Lokasi lokasi = (Lokasi) (searchlokasi.getSelectedItem() == null ? null
				: searchlokasi.getSelectedItem().getValue());

		PemilikAsset pemilikAsset = (PemilikAsset) (searchpemilikAsset.getSelectedItem() == null ? null
				: searchpemilikAsset.getSelectedItem().getValue());

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		return session.createCriteria(AssetDetail.class)

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("barcode", nama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE)))

				.add(ruang != null ? Restrictions.eq("ruang", ruang) : Restrictions.sqlRestriction("true"))

				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								parent == null ? Restrictions.isNull("satuanKerja")
										: Restrictions.sqlRestriction("false"),
								Restrictions.in("satuanKerja", satuanKerjas)))

				.add(lokasi != null ? Restrictions.eq("lokasi", lokasi) : Restrictions.sqlRestriction("true"))
				.add(pemilikAsset != null ? Restrictions.eq("pemilikAsset", pemilikAsset)
						: Restrictions.sqlRestriction("true"))

				.addOrder(Order.desc("id")).add(Restrictions.eq("asset", asset));
	}

	@Override
	public void onSearchDefault(Event event) {
		try {
			loadData(null);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

}
