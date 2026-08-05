package ais.action.master;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.feeder.util.FeederExporter;
import ais.action.master.feeder.util.FeederJSONImport;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DspaceInformation;
import ais.database.model.Konfigurasi;
import ais.database.model.PerguruanTinggiLain;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class PerguruanTinggiLainAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 3786091220301468178L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchalamat;
	private Textbox searchkodeyayasan;
	private Textbox searchkodepergururantinggi;
	private Textbox searchkota;

	private Textbox rektor;
	private Textbox kodeYayasan;
	private Textbox kodePerguruanTinggiLain;
	private Textbox nama;
	private Textbox alamat1;
	private Textbox alamat2;
	private Textbox dusun;
	private Textbox kelurahan;
	private Textbox rt;
	private Textbox rw;
	private Textbox kota;
	private Textbox kodePos;
	private Textbox telepon;
	private Textbox faksimili;
	private MyDatebox tanggalAkta;
	private MyDatebox tanggalAwalPendirian;
	private Textbox nomorAkta;
	private Textbox email;
	private Textbox website;

	private Textbox skIzinOperasi;
	private Textbox pejabatIzinOperasi;
	private MyDatebox tglSkIzinOperasi;
	private Intbox tahunPertamaMenerimaMahasiswa;
	private Textbox noRek;
	private Textbox nmBank;
	private Textbox unitCabang;
	private Textbox nmRek;

	private MyToolbarbuttonConfig add;
	private PerguruanTinggiLain perguruanTinggiLain;
	private boolean edit;
	private boolean delete;
	private Textbox rektorNip;
	private Textbox peringkatAkreditasi;
	private Textbox akreditasi;
	private Textbox noSkAkreditasi;
	private MyDatebox tanggalAkreditasi;
	private Textbox domain;
	private Textbox motto;
	private Textbox kodeSinta;
	private Textbox feeder;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
				&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {

			MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Syn. Semua PT Feeder",
					"/img/Button-Refresh-icon.png");
			buttonTagihan.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin singkronkan semua PT dari Feeder ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										String[] kon = EksporFromFeederAction.koneksi();
										final String ip = kon[0];
										final String port = kon[1];
										final String username = kon[2];
										final String password = kon[3];
										final String url = kon[4];

										if (!EksporFromFeederAction.exists(url)) {

											MyMessageboxConfig.show(
													ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalKoneksi(ip, port, Common.bolehKonfigurasi("aktifkan_https_ke_feeder", ais.database.model.Konfigurasi.TIDAK_AKTIF), "Pemeriksaan ketersediaan alamat " + url + " gagal (server Neo Feeder tidak merespons)."),
													"Peringatan", MyMessageboxConfig.OK,
													MyMessageboxConfig.EXCLAMATION);
											return;
										}

										final Label myLabelProsesDetail = Common.displayLoadBar(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												if (arg0 != null && !arg0.getName().isEmpty()) {
													EksporFromFeederAction.display();
													MyMessageboxConfig.show(arg0.getName(), "Info",
															MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
												}
												onSearchDefault(null);
											}
										});

										new Thread(new Runnable() {

											@Override
											public void run() {
												try {
													FeederConnector feederConnector = new FeederConnector(ip,
															Integer.parseInt(port), null);

													String token = feederConnector.getToken(username, password);
													System.out.println("TOKEN => " + token);

													if (token == null || token.trim().isEmpty()
															|| token.trim().toLowerCase().startsWith("error")) {
														myLabelProsesDetail
																.setValue("Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
														return;
													}

													FeederExporter feederImporter = new FeederExporter(feederConnector,
															token, null, null, myLabelProsesDetail);

													exportKeFeeder(username, feederImporter, token, feederConnector);

													myLabelProsesDetail.setValue("");
												} catch (Exception e) {
													// FIX "gagal diam-diam": sebelumnya exception di sini hanya
													// dicatat ke log admin lalu progres diset "" (=SUKSES palsu)
													// di luar try, menutupi kegagalan dari pengguna.
													ais.common.Common.tampilErrorJikaAdmin(e);
													myLabelProsesDetail.setValue(
															"Error: " + ais.common.PesanFormalHelper.pesanGagalException(
																	"sinkronisasi data Perguruan Tinggi Lain dari Neo Feeder",
																	null, e,
																	new String[] {
																			"Periksa kembali koneksi ke server Neo Feeder (Pengaturan Koneksi) dan coba ulangi.",
																			"Pastikan Username/Password Feeder pada Pengaturan Koneksi masih benar.",
																			"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
																	.replace("\n", " "));
												}
											}
										}).start();

									}

								}
							});

				}
			});
			Common.appendKeToolbar(buttonTagihan, add, comp);
		}
	        FilterLanjutHelper.setup(comp);
}

	private void exportKeFeeder(String username, FeederExporter feederImporter, String token,
			FeederConnector feederConnector) throws Exception {
		// FIX "gagal diam-diam": try-luar sebelumnya menelan total exception (mis.
		// gagal getCount/getRecordset) hanya ke log admin, sehingga pemanggil (thread
		// latar) menganggap proses SELESAI sukses. Sekarang dilempar ke pemanggil.
		// Per-item (di dalam loop) TETAP ditangkap lokal agar satu record gagal tidak
		// menghentikan sisa batch.
		String filter = "";

		String tabel = "satuan_pendidikan";
		Integer countInteger = feederConnector.getCount(token, tabel, "");
		System.out.println("countInteger -> " + countInteger);
		for (int index = 0; index <= countInteger; index += 100) {
			List<Node> results = feederConnector.getRecordset(token, "satuan_pendidikan", filter, "", 100, index);
			System.out.println("results -> " + results.size());

			for (Node result : results) {
				try {
					if (result.hasChildNodes()) {
						NodeList nodeList = result.getChildNodes();
						JSONObject jsonObject = new JSONObject();
						for (int i = 0; i < nodeList.getLength(); i++) {
							Node node = nodeList.item(i);
							if (node.getTextContent() == null) {
								continue;
							}
							jsonObject.put(node.getNodeName(), node.getTextContent());
						}

//							System.out.println("jsonObject -> " + jsonObject);

						FeederJSONImport.perguruanTinggiLain(jsonObject);

					}
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}
		}
	}

	class PerguruanTinggiLainRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PerguruanTinggiLain perguruanTinggiLain = (PerguruanTinggiLain) arg1;

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(perguruanTinggiLain.getKodeYayasan()).setParent(vbox);
			new MyLabelAgakKecil(perguruanTinggiLain.getFeeder()).setParent(vbox);

			new Label(perguruanTinggiLain.getKodePerguruanTinggi()).setParent(arg0);
			new Label(perguruanTinggiLain.getNama()).setParent(arg0);
			new Label(perguruanTinggiLain.getAlamat1()).setParent(arg0);
			new Label(perguruanTinggiLain.getKota()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(perguruanTinggiLain.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					perguruanTinggiLain.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(perguruanTinggiLain);
				}
			});

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(perguruanTinggiLain);
					addWindow.setVisible(true);
					addWindow.onModal();
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

											Common.refreshDelete(perguruanTinggiLain);

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
			toolbar.setParent(arg0);
		}

	}

	public static DspaceInformation getDspace(String cookie, PerguruanTinggiLain perguruanTinggiLain, boolean update)
			throws Exception {

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", perguruanTinggiLain.getNama());
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", perguruanTinggiLain.getDeskripsi());

		jsonPost.put("shortDescription", "Repositori milik perguruan tinggi " + perguruanTinggiLain.getNama());
		jsonPost.put("sidebarText",
				"Berisi semua repository Repositori milik perguruan tinggi " + perguruanTinggiLain.getNama());

		return DspaceInformation.dspaceProcess(cookie, perguruanTinggiLain, jsonPost.toString(), update, "communities",
				"communities");
	}

	private void init(final PerguruanTinggiLain perguruanTinggiLain) throws Exception {
		this.perguruanTinggiLain = perguruanTinggiLain;

		Common.clear(addWindow);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		center.appendChild(initMain(perguruanTinggiLain));

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
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public void onAdd(Event event) throws Exception {
		init(new PerguruanTinggiLain());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private Borderlayout initMain(PerguruanTinggiLain perguruanTinggiLain) {
		this.perguruanTinggiLain = perguruanTinggiLain;
		addWindow.setTitle(perguruanTinggiLain.getId() == null ? "Tambah Perguruan Tinggi" : "Ubah Perguruan Tinggi");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Yayasan"));
		row.appendChild(kodeYayasan = new Textbox(
				perguruanTinggiLain.getKodeYayasan() == null ? "" : perguruanTinggiLain.getKodeYayasan()));
		kodeYayasan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Perguruan Tinggi"));
		row.appendChild(kodePerguruanTinggiLain = new Textbox(perguruanTinggiLain.getKodePerguruanTinggi() == null ? ""
				: perguruanTinggiLain.getKodePerguruanTinggi()));
		kodePerguruanTinggiLain.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama *"));
		row.appendChild(nama = new Textbox(perguruanTinggiLain.getNama() == null ? "" : perguruanTinggiLain.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Rektor / Ketua"));
		row.appendChild(rektor = new Textbox(perguruanTinggiLain.getRektor()));
		rektor.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIP Rektor / Ketua"));
		row.appendChild(rektorNip = new Textbox(perguruanTinggiLain.getRektorNip()));
		rektorNip.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alamat1"));
		row.appendChild(alamat1 = new Textbox(
				perguruanTinggiLain.getAlamat1() == null ? "" : perguruanTinggiLain.getAlamat1()));
		alamat1.setWidth("90%");
		alamat1.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alamat2"));
		row.appendChild(alamat2 = new Textbox(
				perguruanTinggiLain.getAlamat2() == null ? "" : perguruanTinggiLain.getAlamat2()));
		alamat2.setWidth("90%");
		alamat2.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dusun / Kampung"));
		row.appendChild(dusun = new Textbox(perguruanTinggiLain.getDusun()));
		dusun.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelurahan"));
		row.appendChild(kelurahan = new Textbox(perguruanTinggiLain.getKelurahan()));
		kelurahan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("RT"));
		row.appendChild(rt = new Textbox(perguruanTinggiLain.getRt()));
		rt.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("RW"));
		row.appendChild(rw = new Textbox(perguruanTinggiLain.getRw()));
		rw.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kota"));
		row.appendChild(kota = new Textbox(perguruanTinggiLain.getKota() == null ? "" : perguruanTinggiLain.getKota()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Pos"));
		row.appendChild(kodePos = new Textbox(
				(perguruanTinggiLain.getKodePos() == null ? "" : perguruanTinggiLain.getKodePos())));
		kodePos.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Telepon"));
		row.appendChild(telepon = new Textbox(
				perguruanTinggiLain.getTelepon() == null ? "" : perguruanTinggiLain.getTelepon()));
		telepon.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Faksimili"));
		row.appendChild(faksimili = new Textbox(
				perguruanTinggiLain.getFaksimili() == null ? "" : perguruanTinggiLain.getFaksimili()));
		faksimili.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. SK Pendirian"));
		row.appendChild(nomorAkta = new Textbox(
				(perguruanTinggiLain.getNomorAkta() == null ? "" : perguruanTinggiLain.getNomorAkta())));
		nomorAkta.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tgl. SK Pendirian"));
		row.appendChild(tanggalAkta = new MyDatebox(perguruanTinggiLain.getTanggalAkta()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Awal Pendirian"));
		row.appendChild(tanggalAwalPendirian = new MyDatebox(perguruanTinggiLain.getTanggalAwalPendirian()));

		// private Textbox skIzinOperasi;
		// private MyDatebox tglSkIzinOperasi;
		// private Textbox noRek;
		// private Textbox nmBank;
		// private Textbox unitCabang;
		// private Textbox nmRek;

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. SK Izin"));
		row.appendChild(skIzinOperasi = new Textbox((perguruanTinggiLain.getSkIzinOperasi())));
		skIzinOperasi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tgl. SK Izin"));
		row.appendChild(tglSkIzinOperasi = new MyDatebox((perguruanTinggiLain.getTglSkIzinOperasi())));
		tglSkIzinOperasi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pejabat SK Izin"));
		row.appendChild(pejabatIzinOperasi = new Textbox((perguruanTinggiLain.getPejabatIzinOperasi())));
		pejabatIzinOperasi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Pertama Menerima Mahasiswa"));
		row.appendChild(
				tahunPertamaMenerimaMahasiswa = new Intbox(perguruanTinggiLain.getTahunPertamaMenerimaMahasiswa()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Peringkat Akreditasi Terakhir BAN-PT"));
		row.appendChild(peringkatAkreditasi = new Textbox(perguruanTinggiLain.getPeringkatAkreditasi()));
		peringkatAkreditasi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Akreditasi Terakhir BAN-PT"));
		row.appendChild(akreditasi = new Textbox(perguruanTinggiLain.getAkreditasi()));
		akreditasi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. SK Akreditasi Terakhir BAN-PT"));
		row.appendChild(noSkAkreditasi = new Textbox(perguruanTinggiLain.getNoSkAkreditasi()));
		noSkAkreditasi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Akreditasi Terakhir BAN-PT"));
		row.appendChild(tanggalAkreditasi = new MyDatebox(perguruanTinggiLain.getTanggalAkreditasi()));
		tanggalAkreditasi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Bank"));
		row.appendChild(nmBank = new Textbox((perguruanTinggiLain.getNmBank())));
		nmBank.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. Rekening Bank"));
		row.appendChild(noRek = new Textbox((perguruanTinggiLain.getNoRek())));
		noRek.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Rekening Bank"));
		row.appendChild(nmRek = new Textbox((perguruanTinggiLain.getNmRek())));
		nmRek.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Unit Cabang"));
		row.appendChild(unitCabang = new Textbox((perguruanTinggiLain.getUnitCabang())));
		unitCabang.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Email"));
		row.appendChild(
				email = new Textbox(perguruanTinggiLain.getEmail() == null ? "" : perguruanTinggiLain.getEmail()));
		email.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Website"));
		row.appendChild(website = new Textbox(
				perguruanTinggiLain.getWebsite() == null ? "" : perguruanTinggiLain.getWebsite()));
		website.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Domain PT"));
		row.appendChild(domain = new Textbox(perguruanTinggiLain.getDomain()));
		domain.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Motto PT"));
		row.appendChild(motto = new Textbox(perguruanTinggiLain.getMotto()));
		motto.setWidth("90%");
		motto.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Sinta"));
		row.appendChild(kodeSinta = new Textbox(perguruanTinggiLain.getKodeSinta()));
		kodeSinta.setWidth("90%");

		Common.initKeterangan(rows,
				"jika link sinta perguruan tinggi anda https://sinta.kemdikbud.go.id/affiliations/profile?id=xxxx, maka kode sinta perguruan tinggi adalah xxxx");

		row = new MyFormRow();
		row.setVisible(Common.getApakahAdminBolehAksesFeeder());
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Feeder"));
		row.appendChild(feeder = new Textbox(perguruanTinggiLain.getFeeder()));
		feeder.setWidth("90%");

		return borderlayout;

	}

	public boolean onSave(Event event) throws Exception {

		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Nama",
					"Kolom Nama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (perguruanTinggiLain.getId() != null) {
			perguruanTinggiLain = (PerguruanTinggiLain) session.load(PerguruanTinggiLain.class,
					perguruanTinggiLain.getId());
		}

		perguruanTinggiLain.setRektorNip(rektorNip.getValue());
		perguruanTinggiLain.setRektor(rektor.getValue());
		perguruanTinggiLain.setAkreditasi(akreditasi.getValue());
		perguruanTinggiLain.setNoSkAkreditasi(noSkAkreditasi.getValue());
		perguruanTinggiLain.setTanggalAkreditasi(tanggalAkreditasi.getValue());
		perguruanTinggiLain.setPeringkatAkreditasi(peringkatAkreditasi.getValue());

		perguruanTinggiLain.setTahunPertamaMenerimaMahasiswa(tahunPertamaMenerimaMahasiswa.getValue());
		perguruanTinggiLain.setPejabatIzinOperasi(pejabatIzinOperasi.getValue());
		perguruanTinggiLain.setDusun(dusun.getValue());
		perguruanTinggiLain.setKelurahan(kelurahan.getValue());
		perguruanTinggiLain.setRt(rt.getValue());
		perguruanTinggiLain.setRw(rw.getValue());
		perguruanTinggiLain.setSkIzinOperasi(skIzinOperasi.getValue());
		perguruanTinggiLain.setTglSkIzinOperasi(tglSkIzinOperasi.getValue());
		perguruanTinggiLain.setNoRek(noRek.getValue());
		perguruanTinggiLain.setNmBank(nmBank.getValue());
		perguruanTinggiLain.setUnitCabang(unitCabang.getValue());
		perguruanTinggiLain.setNmRek(nmRek.getValue());

		perguruanTinggiLain.setKodeYayasan(kodeYayasan.getValue());
		perguruanTinggiLain.setKodePerguruanTinggi(kodePerguruanTinggiLain.getValue());
		perguruanTinggiLain.setNama(nama.getValue());
		perguruanTinggiLain.setAlamat1(alamat1.getValue());
		perguruanTinggiLain.setAlamat2(alamat2.getValue());
		perguruanTinggiLain.setKota(kota.getValue());
		perguruanTinggiLain.setKodePos(kodePos.getValue() == null ? null : (kodePos.getValue().toString()));
		perguruanTinggiLain.setTelepon(telepon.getValue());
		perguruanTinggiLain.setFaksimili(faksimili.getValue());
		perguruanTinggiLain.setTanggalAkta(tanggalAkta.getValue());
		perguruanTinggiLain.setTanggalAwalPendirian(tanggalAwalPendirian.getValue());
		perguruanTinggiLain.setNomorAkta(nomorAkta.getValue() == null ? null : (nomorAkta.getValue().toString()));
		perguruanTinggiLain.setEmail(email.getValue());
		perguruanTinggiLain.setWebsite(website.getValue());

		perguruanTinggiLain.setFeeder(feeder.getValue().trim());

		perguruanTinggiLain.setDomain(domain.getValue().trim());
		perguruanTinggiLain.setMotto(motto.getValue().trim());
		perguruanTinggiLain.setKodeSinta(kodeSinta.getValue().trim());

		Common.refreshSaveOrUpdate(session, perguruanTinggiLain);

		return true;
	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PerguruanTinggiLain.class);
		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE))
				.add(searchalamat.getValue().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("alamat1", searchalamat.getValue(), MatchMode.ANYWHERE))
				.add(searchkodeyayasan.getValue().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kodeYayasan", searchkodeyayasan.getValue(), MatchMode.ANYWHERE))
				.add(searchkodepergururantinggi.getValue().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kodePerguruanTinggiLain", searchkodepergururantinggi.getValue(),
								MatchMode.ANYWHERE))
				.add(searchkota.getValue().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kota", searchkota.getValue(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PerguruanTinggiLain> perguruanTinggiLain = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(perguruanTinggiLain);
		grid.setRowRenderer(new PerguruanTinggiLainRenderer());
		grid.setModelCheckMobile(strset);

	}
}
