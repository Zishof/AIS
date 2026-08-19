package ais.action.master.alumni;


import ais.common.CommonSearchFilterHelper;
import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.master.EksporFromFeederAction;
import ais.action.master.SkripsiAction;
import ais.action.master.TampilanELearningAction;
import ais.action.master.dashboard.helper.DashboardRekapMahasiswa;
import ais.action.master.dashboard.helper.DashboardRekapParameterTambahanAlumni;
import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.feeder.util.FeederExporter;
import ais.action.master.feeder.util.FeederJSONImport;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.TampilStudiMahasiswaHelper;
import ais.action.report.format1.akademik.LaporanKartuAlumni;
import ais.action.report.format1.akademik.LaporanRekapitulasiAlumniJurusan;
import ais.action.report.format1.akademik.LaporanRekapitulasiAlumniJurusanBerdasarkanTahunLulus;
import ais.action.report.format1.akademik.LaporanTranskipAkademik;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.MahasiswaDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.JenisSeleksi;
import ais.database.model.Jenjang;
import ais.database.model.Judisium;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Skripsi;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusDomisiliSetelahLulus;
import ais.database.model.StatusMahasiswa;
import ais.database.model.StatusPekerjaanSetelahLulus;
import ais.database.model.StatusSetelahLulus;
import ais.database.model.Tbmuser;
import ais.delivery.email.sender.MailSender;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelKecilSekali;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyVboxStyled;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class MahasiswaAction extends GenericAutowireComposer implements DataLoader, DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;

	private Paging paging;

	private MyGrid grid;
	private Textbox searchnama;
	private Textbox searchta;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Decimalbox searchtahun;
	private Decimalbox searchtahunLulus;
	private Decimalbox searchMasa;
	private Decimalbox searchsemesterLulus;
	private Combobox searchstatus;
	private Combobox searchprogram;
	private Combobox searchStatusAwalMahasiswa;
	private Combobox searchjenjang;

	private Combobox searchkewarganegaraan;

	private MyCheckboxConfig searchTelahMengisi;
	private MyCheckboxConfig searchUrutkanTelahMengisi;

	private Combobox searchJenisSeleksi;

	private Combobox searchpredikatKelulusan;
	private Combobox searchstatusSetelahLulus;
	private Combobox searchstatusPekerjaanSetelahLulus;
	private Combobox searchstatusDomisiliSetelahLulus;
	private Combobox searchsemesterawal;

	private MyToolbarbuttonConfig add;

	private boolean edit = false;
	private boolean delete = false;

	private Tabpanel transkripAkademik;
	private Tabpanel rekapJumlahAlumni;
	private Tabpanel grafikJumlahAlumni;

	private Tabpanel parameterAlumni;

	private Tabpanel galeriFoto;

	private Tabpanel manajemenKartuMahasiswa;

	private Tabpanel dashboardAlumni;

	/**
	 * Tab "Dashboard" (Tracer Study): menampilkan sekaligus <b>statistik data alumni</b>
	 * ({@link DashboardStatistikJumlahAlumni}) dan <b>rekap jawaban parameter</b> yang diisi alumni
	 * ({@link DashboardRekapParameterTambahanAlumni}) dalam satu tempat, sesuai permintaan. Dibangun
	 * lazy sekali (guard {@code getChildren().size()==0}); memakai kembali komponen dasbor yang sudah ada.
	 */
	public void onDashboardAlumni(Event event) {
		if (dashboardAlumni.getChildren().size() == 0) {
			// 1) Statistik dari DATA alumni (jumlah lulus per tahun/prodi/jenjang).
			DashboardStatistikJumlahAlumni statistik = new DashboardStatistikJumlahAlumni();
			ais.ui.util.BaseDasbordPortal.mountWrapped(statistik, dashboardAlumni, "Statistik Data Alumni",
					"Ringkasan dan tren jumlah alumni: per tahun lulus, program studi, dan jenjang.");

			// 2) Statistik dari PARAMETER yang diisi alumni (survei Tracer Study).
			DashboardRekapParameterTambahanAlumni rekapParameter = new DashboardRekapParameterTambahanAlumni();
			ais.ui.util.BaseDasbordPortal.mountWrapped(rekapParameter, dashboardAlumni,
					"Ringkasan Jawaban Tracer Study",
					"Rekap isian survei/parameter tambahan yang diisi alumni setelah kelulusan.");
		}
	}

	public void onKartuMahasiswa(Event event) {
		if (manajemenKartuMahasiswa.getChildren().size() == 0) {
			LaporanKartuAlumni laporan = new LaporanKartuAlumni();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(manajemenKartuMahasiswa);
		}
	}

	public void onGaleriFoto(Event event) {
		if (galeriFoto.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(galeriFoto);
			MyInclude iframe = new MyInclude("/pages/master/galeri_foto.zul");
			iframe.setParent(window);
		}
	}

	public void onParameterAlumni(Event event) {
		if (parameterAlumni.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(parameterAlumni);
			MyInclude iframe = new MyInclude("/pages/master/parameter_tambahan_mahasiswa_alumni.zul");
			iframe.setParent(window);
		}
	}

	public void onRekapJumlahAlumni(Event event) {

		if (rekapJumlahAlumni.getChildren().size() == 0) {

			ais.ui.util.MyButtonTabbox btnTabAlumniStat = ais.ui.util.MyButtonTabbox.buat(rekapJumlahAlumni, "100%", new int[] { 0 });

			// Tab 0: Tracer Study - load immediately
			{
				org.zkoss.zul.Div panelTracer = btnTabAlumniStat.tambahTab(0, "Berdasarkan Tracer Study", "/img/svg/person-lines-fill.svg");
				DashboardRekapParameterTambahanAlumni rekapParameterTambahanAlumni = new DashboardRekapParameterTambahanAlumni();
				ais.ui.util.BaseDasbordPortal.mountWrapped(rekapParameterTambahanAlumni, panelTracer,
						"Parameter Tambahan Alumni", "Rekap isian survei tambahan yang diisi alumni setelah kelulusan.");
			}
			btnTabAlumniStat.tambahTabLazy(1, "Berdasarkan Data Lulusan", "/img/svg/user-graduate.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
					DashboardRekapMahasiswa laporan = new DashboardRekapMahasiswa(false, ConstantValues.LULUS);
					ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, panel,
							"Rekap Alumni", "Gambaran jumlah dan sebaran alumni berdasarkan prodi dan tahun lulus.");
				}
			});
			btnTabAlumniStat.tambahTabLazy(2, "Alumni Berdasar Tahun Angkatan", "/img/svg/dashboard-chart.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
					LaporanRekapitulasiAlumniJurusan laporan = new LaporanRekapitulasiAlumniJurusan();
					laporan.setHeight("100%");
					laporan.setWidth("100%");
					laporan.setParent(panel);
				}
			});
			btnTabAlumniStat.tambahTabLazy(3, "Alumni Berdasar Tahun Lulus", "/img/svg/chart-line-light.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
					LaporanRekapitulasiAlumniJurusanBerdasarkanTahunLulus laporan = new LaporanRekapitulasiAlumniJurusanBerdasarkanTahunLulus();
					laporan.setHeight("100%");
					laporan.setWidth("100%");
					laporan.setParent(panel);
				}
			});

		}
	}

	public void onGrafikJumlahAlumni(Event event) {

		if (grafikJumlahAlumni.getChildren().size() == 0) {
			DashboardStatistikJumlahAlumni laporan = new DashboardStatistikJumlahAlumni();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, grafikJumlahAlumni,
				"Statistik Alumni", "Tren jumlah alumni yang lulus setiap tahun per program studi.");
		}
	}

	public void onTampilTranskripAkademik(Event event) {

		if (transkripAkademik.getChildren().size() == 0) {
			LaporanTranskipAkademik laporanTranskipAkademik = new LaporanTranskipAkademik();
			laporanTranskipAkademik.setHeight("100%");
			laporanTranskipAkademik.setWidth("100%");
			laporanTranskipAkademik.setParent(transkripAkademik);
		}
	}

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

		if (add != null) { add.setVisible(false); }
		if (add != null) { add.setTooltiptext("Tambah"); }

		Common.initPrograms(searchprogram);
		Common.insertCombo(searchStatusAwalMahasiswa, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.insertCombo(searchstatus, new String[] { "nama", "kodeEpsbed" }, StatusMahasiswa.class);

		Common.insertComboDanSemua(searchJenisSeleksi, "nama", JenisSeleksi.class, Restrictions.eq("aktif", true));

		Common.insertComboDanSemua(searchpredikatKelulusan, "nama", Judisium.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertComboDanSemua(searchstatusSetelahLulus, "nama", StatusSetelahLulus.class);
		Common.insertComboDanSemua(searchstatusPekerjaanSetelahLulus, "nama", StatusPekerjaanSetelahLulus.class);
		Common.insertComboDanSemua(searchstatusDomisiliSetelahLulus, "nama", StatusDomisiliSetelahLulus.class);

		MyComboitemConfig comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GANJIL); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		searchsemesterawal.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GENAP); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		searchsemesterawal.appendChild(comboitem);

		searchkewarganegaraan = new Combobox();
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(ais.database.model.Mahasiswa.WNI); }
		if (comboitem != null) { comboitem.setValue(ais.database.model.Mahasiswa.WNI); }
		searchkewarganegaraan.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(ais.database.model.Mahasiswa.WNA); }
		if (comboitem != null) { comboitem.setValue(ais.database.model.Mahasiswa.WNA); }
		searchkewarganegaraan.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("Semua"); }
		if (comboitem != null) { comboitem.setValue(null); }
		searchkewarganegaraan.appendChild(comboitem);

		if (searchkewarganegaraan != null) { searchkewarganegaraan.setSelectedItem(comboitem); }
		if (searchkewarganegaraan != null) { searchkewarganegaraan.setReadonly(true); }

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("Semua"); }
		if (comboitem != null) { comboitem.setValue(null); }
		searchsemesterawal.appendChild(comboitem);
		if (searchsemesterawal != null) { searchsemesterawal.setSelectedItem(comboitem); }
		if (searchsemesterawal != null) { searchsemesterawal.setReadonly(true); }

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		Common.insertComboDanSemua(searchjenjang, "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		ais.action.master.MahasiswaAction.createUploadDanDownloadData(add.getParent(), new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		}, this, true, true);
		Tbmuser tbmuser = Common.getCurrentUser();

		if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
				&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {

			MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Kirim ke Feeder",
					"/img/Finance-Invoice-icon.png");
			buttonTagihan.setStyle("font-size:8px;");
			buttonTagihan.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin mengirim ke feeder ?", "Pertanyaan",
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

										final List<String> errorLog = new ArrayList<String>();
										final Label myLabelProsesDetail = Common.displayLoadBar(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												if (arg0 != null && !arg0.getName().isEmpty()) {
													EksporFromFeederAction.display();
													MyMessageboxConfig.show(arg0.getName(), "Info",
															MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
												}

												if (!errorLog.isEmpty()) {
													String err = "";
													for (String s : errorLog) {
														err += err.isEmpty() ? s
																: "\n----------------------------------------------------------------------------------------------------------\n"
																		+ s;
													}

													MyMessageboxConfig.show(
															"Error Terjadi, catatan error akan otomatis ter-download",
															"Error Terjadi", MyMessageboxConfig.OK,
															MyMessageboxConfig.EXCLAMATION);

													File file = new File(
															"/opt/ecampus/error_" + Common.randLong() + ".txt");
													if (!file.getParentFile().exists()) {
														file.getParentFile().mkdirs();
													}
													FileUtils.writeStringToFile(file, err);
													Filedownload.save(file, "text/plain");
												}

												onSearchDefault(null);
											}
										});

										new Thread(new Runnable() {

											@SuppressWarnings("unchecked")
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
															token, null, null, null);

													List<Mahasiswa> tbmusers = ConstantValues
															.simpleList(initCriteria(true), Mahasiswa.class);
													int size = tbmusers.size();
													int index = 1;
													for (Mahasiswa mahasiswa : tbmusers) {
														myLabelProsesDetail.setValue("Memproses " + mahasiswa.getNim()
																+ " " + mahasiswa.getNama() + " ("
																+ Common.numberFormat.get().format((index * 100.0) / size)
																+ "%");
														index++;
														ais.action.master.MahasiswaAction.exportKeFeeder(mahasiswa,
																feederImporter, token, feederConnector, errorLog);
													}
													tbmusers.clear();
													tbmusers = null;

													myLabelProsesDetail.setValue("");
												} catch (Exception e) {
													// FIX "gagal diam-diam": sebelumnya exception di sini hanya
													// dicatat ke log admin lalu progres diset "" (=SUKSES palsu)
													// di luar try, menutupi kegagalan (mis. gagal konek/parse port).
													ais.common.Common.tampilErrorJikaAdmin(e);
													myLabelProsesDetail.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
															"pengiriman data Mahasiswa ke Neo Feeder",
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

			buttonTagihan = new MyToolbarbuttonConfig("Ambil dari feeder", "/img/Button-Refresh-icon.png");
			buttonTagihan.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin mengambil dari feeder ?", "Pertanyaan",
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

											@SuppressWarnings("unchecked")
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

													String filter = "";

													if (!searchnama.getValue().trim().isEmpty()) {
														for (String o : searchnama.getValue().trim().split(";")) {
															String s = "nama_mahasiswa like '%" + o.trim() + "%'";
															filter += filter.isEmpty() ? s : " or " + s;
														}
													}

													Integer countInteger = feederConnector.getCount(token,
															"GetCountMahasiswa", "");

													for (int index = 0; index <= countInteger; index += 500) {

														JSONArray dataMhsPt = feederConnector.getData(
																"GetListMahasiswa", token, filter, "", "500",
																index + "");

														for (int i = 0; i < dataMhsPt.length(); i++) {
															JSONObject jsonObject = dataMhsPt.getJSONObject(i);

															JSONArray dataMhsLulusDo = feederConnector
																	.getData("GetListMahasiswaLulusDO", token,
																			"id_mahasiswa='" + jsonObject
																					.getString("id_mahasiswa") + "'",
																			"", "1", index + "");

															if (dataMhsLulusDo != null) {

																JSONObject jsonObjectMhsLulsuDo = dataMhsLulusDo
																		.length() > 0 ? dataMhsLulusDo.getJSONObject(0)
																				: null;
																if (jsonObjectMhsLulsuDo != null) {
																	Iterator<String> it = jsonObjectMhsLulsuDo.keys();
																	while (it.hasNext()) {
																		String key = it.next();
																		jsonObject.put(key,
																				jsonObjectMhsLulsuDo.get(key));
																	}
																}

																System.out.println(
																		"results mahasiswa_pt -> " + jsonObject);

																JSONArray dataMhs = feederConnector
																		.getData("GetBiodataMahasiswa", token,
																				"id_mahasiswa='" + jsonObject.getString(
																						"id_mahasiswa") + "'",
																				"", "1", index + "");

																if (dataMhs.length() > 0) {
																	JSONObject jsonObjectMhs = dataMhs.getJSONObject(0);

																	System.out.println(
																			"results mahasiswa -> " + jsonObjectMhs);

																	FeederJSONImport.mahasiswa(jsonObject);
																	FeederJSONImport.mahasiswa_aja(jsonObjectMhs);
																	String key = jsonObject.getString("nipd").trim();
																	String nama = jsonObjectMhs
																			.getString("nama_mahasiswa").trim();
																	myLabelProsesDetail.setValue("Memproses " + key
																			+ " " + nama + " ("
																			+ Common.numberFormat.get()
																					.format(((index + i) * 100.0)
																							/ countInteger)
																			+ "%");
																}
															}
														}
													}

												myLabelProsesDetail.setValue("");
											} catch (Exception e) {
												// FIX "gagal diam-diam": sebelumnya exception di sini hanya
												// dicatat ke log admin lalu progres diset "" (=SUKSES palsu)
												// di luar try, menutupi kegagalan (mis. gagal konek/parse data).
												ais.common.Common.tampilErrorJikaAdmin(e);
												myLabelProsesDetail.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
														"pengambilan data Mahasiswa dari Neo Feeder",
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

		if (Common.bolehKonfigurasi("upload_download_email_mahasiswa", Konfigurasi.TIDAK_AKTIF)) {

			String[] contents1 = new String[] { "id", "mahasiswa.nim", "mahasiswa.nama", "email" };
			MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(BiodataCalonMahasiswa.class,
					new DataCriteria() {

						@SuppressWarnings("unchecked")
						@Override
						public Criteria initCriteria(boolean order) {

							List<Long> mhs = MahasiswaAction.this.initCriteria(true)
									.setProjection(Projections.property("id")).list();

							return HibernateUtil.currentSession().createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(mhs.isEmpty() ? Restrictions.sqlRestriction("false")
											: Restrictions.in("mahasiswa.id", mhs));
						}
					}, contents1);
			cetakToolbarbutton.setLabel("Download Email");
			Common.appendKeToolbar(cetakToolbarbutton, add, comp);

			MyToolbarbuttonConfig upload = Common.uploadData(this, BiodataCalonMahasiswa.class, contents1);
			upload.setLabel("Upload Email");
			upload.setVisible((add != null && add.isVisible()) && edit && delete);
			Common.appendKeToolbar(upload, add, comp);

		}

		if (Common.bolehKonfigurasi("broadcast_email_alumni_mahasiswa", Konfigurasi.TIDAK_AKTIF)) {

			MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Broadcast Email Tracer Study",
					"/img/Button-Refresh-icon.png");
			buttonTagihan.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (searchtahunLulus.getValue() == null && searchtahunLulus.getValue().intValue() < 1900) {
						MyMessageboxConfig.show(
								"Sebelum broadcast email tracer study, pencarian tahun lulus harus diisi dengan benar",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}

					MyMessageboxConfig.show("Apakah yakin ingin Broadcast Email Tracer Study ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										final String filenameexcel = Sessions.getCurrent().getWebApp()
												.getRealPath("/tmp/pembayaran_mahasiswa_"
														+ URLEncoder.encode(Common.dateFormat7.get()
																.format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
														+ ".xlsx");

										final Label myLabelProsesDetail = Common.displayLoadBar(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												if (arg0 != null && !arg0.getName().isEmpty()) {
													MyMessageboxConfig.show(arg0.getName(), "Info",
															MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
												}
												File file = new File(filenameexcel);
												try {
													Filedownload.save(new FileInputStream(file),
															"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
															file.getName());
												} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

												onSearchDefault(null);
											}
										});

										new Thread(new Runnable() {

											@SuppressWarnings("unchecked")
											@Override
											public void run() {
												try {

													XSSFWorkbook workbook = new XSSFWorkbook();
													XSSFSheet sheet = workbook.createSheet("DATA SURAT TAGIHAN");
													sheet.setDefaultColumnWidth(20);
													XSSFCellStyle lockedNumericStyle = workbook.createCellStyle();
													lockedNumericStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
													lockedNumericStyle.setFillForegroundColor(new XSSFColor(Color.RED));
													lockedNumericStyle.setLocked(true);

													XSSFRow rowhead = sheet.createRow((short) 0);
													rowhead.createCell(0).setCellValue("NIM");
													rowhead.createCell(1).setCellValue("NAMA");
													rowhead.createCell(2)
															.setCellValue(Common.getBahasaConfig("FAKULTAS"));
													rowhead.createCell(3)
															.setCellValue(Common.getBahasaConfig("JURUSAN"));
													rowhead.createCell(4).setCellValue("STATUS AWAL");
													rowhead.createCell(5).setCellValue("ANGKATAN");
													rowhead.createCell(6).setCellValue("EMAIL");
													rowhead.createCell(7).setCellValue("LINK");

													List<Long> mhs = MahasiswaAction.this.initCriteria(true)
															.setProjection(Projections.property("id"))
															.add(Restrictions.isNotNull("email"))
															.add(Restrictions.ne("email", "")).list();
													int rowIndex = 1;
													int size = mhs.size();
													int index = 0;
													for (Long idMhs : mhs) {
														try {
															index++;
															Mahasiswa mahasiswa = (Mahasiswa) ConstantValues
																	.ambil(Mahasiswa.class.getName(), idMhs);
															if (mahasiswa != null) {

																myLabelProsesDetail
																		.setValue(
																				"Proses kirim email ke "
																						+ mahasiswa.getNama() + " "
																						+ Common.numberFormat.get().format(
																								(index * 100.0) / size)
																						+ "%");

																String subject = "Informasi Pengisian Tracer Study atas nama "
																		+ mahasiswa.getNama() + " ("
																		+ mahasiswa.getNim() + ")";

																String code = Common.getRequestHostWithProtocol()
																		+ "/m?q="
																		+ URLEncoder.encode(Common.desEncrypter.get()
																				.encrypt(mahasiswa.getId()
																						+ "-Alumni-abcdefghijklmnopqrstuvwxyz"),
																				"UTF-8");

																String body = "Anda mendapatkan informasi Pengisian Tracer Study untuk mahasiswa atas nama "
																		+ mahasiswa.getNama() + " ("
																		+ mahasiswa.getNim() + ") "
																		+ "<br>Pengisian Tracer Study dapat di bukan pada link berikut <a target='_blank' href='"
																		+ code + "'>" + code
																		+ "</a>.<br><br>Terima Kasih";
																String sender = Common.getKonfigurasi("default_email",
																		"info@zishof.com").getNilai();
																JSONArray userIds = new JSONArray();
																userIds.put(mahasiswa.getNim());
																MailSender.sendMailLampiranTagihan(userIds, subject,
																		body, sender, mahasiswa.getEmail(), null, false,
																		mahasiswa);
																Jurusan jurusan = mahasiswa.getJurusan();

																XSSFRow row = sheet.createRow(rowIndex);
																XSSFCell cell = row.createCell(0);
																cell.setCellValue(mahasiswa.getNim());

																cell = row.createCell(1);
																cell.setCellValue(mahasiswa.getNama());

																cell = row.createCell(2);
																cell.setCellValue(jurusan == null ? ""
																		: jurusan.getFakultas().getNama());

																cell = row.createCell(3);
																cell.setCellValue(
																		jurusan == null ? "" : jurusan.getNama());

																cell = row.createCell(4);
																cell.setCellValue(
																		mahasiswa.getStatusAwalMahasiswa() == null ? ""
																				: mahasiswa.getStatusAwalMahasiswa()
																						.getNama());

																cell = row.createCell(5);
																cell.setCellValue(mahasiswa.getTahunangkatan());

																cell = row.createCell(6);
																cell.setCellValue(mahasiswa.getEmail());

																cell = row.createCell(7);
																cell.setCellValue(code);

																rowIndex++;
															}
														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/alumni/MahasiswaAction.java:872");
															// TODO: handle exception
														}
													}
													try {
														FileOutputStream fileOut = new FileOutputStream(filenameexcel);
														workbook.write(fileOut);
														fileOut.close();
													} catch (IOException e) {
														Common.tampilErrorJikaAdmin(e);
													}
													System.out.println("Your excel file has been generated! ");
												} catch (Exception e) {
													ais.common.Common.tampilErrorJikaAdmin(e);
												}

												myLabelProsesDetail.setValue("");
											}
										}).start();

									}

								}
							});

				}
			});
			Common.appendKeToolbar(buttonTagihan, add, comp);

			// ================= BROADCAST EMAIL KE ATASAN ALUMNI (Penilaian Pengguna Lulusan) =================
			// Kirim surat undangan formal ke setiap ATASAN (email dari Mahasiswa.atasans JSON) alumni yang lulus,
			// beserta tautan kuesioner pengguna-lulusan yang LANGSUNG membuka halaman kuesioner tanpa login.
			MyToolbarbuttonConfig buttonAtasan = new MyToolbarbuttonConfig("Broadcast Email ke Atasan Alumni",
					"/img/Button-Refresh-icon.png");
			buttonAtasan.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (searchtahunLulus.getValue() == null || searchtahunLulus.getValue().intValue() < 1900) {
						MyMessageboxConfig.show(
								"Sebelum broadcast email ke atasan alumni, pencarian tahun lulus harus diisi dengan benar",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}

					MyMessageboxConfig.show(
							"Apakah yakin ingin Broadcast Email ke Atasan Alumni (Penilaian Pengguna Lulusan) ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int konfirmasi = Integer.parseInt(event.getData().toString());
									if (konfirmasi != MyMessageboxConfig.OK) {
										return;
									}

									final int[] totalHolder = { 0 };
									final Label myLabelProsesDetail = Common.displayLoadBar(new EventListener() {
										@Override
										public void onEvent(Event arg0) throws Exception {
											MyMessageboxConfig.show(
													"Broadcast email ke atasan alumni selesai. Total " + totalHolder[0]
															+ " email berhasil diproses/diantrikan.",
													"Info", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
											onSearchDefault(null);
										}
									});

									new Thread(new Runnable() {

										@SuppressWarnings("unchecked")
										@Override
										public void run() {
											try {
												List<Long> mhs = MahasiswaAction.this.initCriteria(true)
														.setProjection(Projections.property("id"))
														.add(Restrictions.isNotNull("atasans"))
														.add(Restrictions.ne("atasans", "")).list();
												int size = mhs.size();
												int index = 0;
												String sender = Common.getKonfigurasi("default_email", "info@zishof.com")
														.getNilai();

												for (Long idMhs : mhs) {
													index++;
													try {
														Mahasiswa mahasiswa = (Mahasiswa) ConstantValues
																.ambil(Mahasiswa.class.getName(), idMhs);
														if (mahasiswa == null) {
															continue;
														}

														myLabelProsesDetail.setValue("Proses kirim email ke atasan "
																+ mahasiswa.getNama() + " " + Common.numberFormat.get()
																		.format((index * 100.0) / (size == 0 ? 1 : size))
																+ "%");

														// Tautan kuesioner PENGGUNA LULUSAN (atasan) -> auto-login tanpa layar login.
														String code = Common.getRequestHostWithProtocol() + "/m?q="
																+ URLEncoder.encode(Common.desEncrypter.get().encrypt(
																		mahasiswa.getId()
																				+ "-PenggunaLulusan-abcdefghijklmnopqrstuvwxyz"),
																		"UTF-8");

														List<ais.action.master.helper.AtasanMahasiswaHelper.Atasan> daftarAtasan = ais.action.master.helper.AtasanMahasiswaHelper
																.parseList(mahasiswa.getAtasans());

														for (ais.action.master.helper.AtasanMahasiswaHelper.Atasan atasan : daftarAtasan) {
															if (atasan == null || atasan.email == null
																	|| atasan.email.trim().isEmpty()
																	|| !atasan.email.contains("@")) {
																continue;
															}
															String subject = ais.action.master.helper.BroadcastAtasanEmailHelper
																	.buildSubject(mahasiswa);
															String body = ais.action.master.helper.BroadcastAtasanEmailHelper
																	.buildBody(mahasiswa, atasan, code);
															JSONArray userIds = new JSONArray();
															userIds.put(mahasiswa.getNim());
															MailSender.sendMailLampiranTagihan(userIds, subject, body,
																	sender, atasan.email.trim(), null, false, mahasiswa);
															totalHolder[0]++;
														}
													} catch (Exception e) {
														ais.common.Common.tampilErrorJikaAdmin(e);
													}
												}
											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/alumni/MahasiswaAction.java:1002");
											} finally {
												// Sinyal SELESAI ke load bar (memicu listener penyelesaian).
												myLabelProsesDetail.setValue("");
											}
										}
									}).start();

								}
							});

				}
			});
			Common.appendKeToolbar(buttonAtasan, add, comp);

		}
	        FilterLanjutHelper.setup(comp);
}

	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		String tahunAkademik = null;
		String jenisSemester = null;
		String hr = null;
		String keyword = "";
		boolean merupakanPraPerkuliahan = false;
		Integer ekstrakurikuler = null;
		boolean merupakanRemedial = false;

		private Tbmuser user = Common.getCurrentUser();

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub

			MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.setOpen(true);

			final Mahasiswa mahasiswa = (Mahasiswa) arg1;
			arg0.setValign("top");
			CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(arg0);

			Vbox aaa;
			(aaa = RevisiHelper.createNewRevisi(Mahasiswa.class, mahasiswa, mahasiswa.getNim())).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(aaa);

			Hbox myHbox = new Hbox();
			myHbox.setParent(vbox);

			if (user != null && Common.getApakahAdminBolehAksesFeeder()
					&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {

				if (mahasiswa.getIdRegPd() != null && !mahasiswa.getIdRegPd().trim().isEmpty()) {
					myHbox.appendChild(new Image("/img/svg/check2-circle.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder valid"));
				} else {
					myHbox.appendChild(new Image("/img/svg/warning-outline.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder blm valid"));
				}

				MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Krm ke feeder",
						"/img/Finance-Invoice-icon.png");
				buttonTagihan.setStyle("font-size:8px;");
				buttonTagihan.setParent(vbox);
				buttonTagihan.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						MyMessageboxConfig.show("Apakah yakin ingin mengirim ke feeder ?", "Pertanyaan",
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

											final List<String> errorLog = new ArrayList<String>();

											final Label myLabelProsesDetail = Common
													.displayLoadBar(new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															if (arg0 != null && !arg0.getName().isEmpty()) {
																EksporFromFeederAction.display();
																MyMessageboxConfig.show(arg0.getName(), "Info",
																		MyMessageboxConfig.OK,
																		MyMessageboxConfig.EXCLAMATION);
															}

															if (!errorLog.isEmpty()) {
																String err = "";
																for (String s : errorLog) {
																	err += err.isEmpty() ? s
																			: "\n----------------------------------------------------------------------------------------------------------\n"
																					+ s;
																}

																MyMessageboxConfig.show(err, "Error Terjadi",
																		MyMessageboxConfig.OK,
																		MyMessageboxConfig.EXCLAMATION);

																File file = new File(Common.REAL_PATH + "/tmp/error_"
																		+ Common.randLong() + ".txt");

																if (!file.getParentFile().exists()) {
																	file.getParentFile().mkdirs();
																}
																FileUtils.writeStringToFile(file, err);
																Filedownload.save(file, "text/plain");
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

														FeederExporter feederImporter = new FeederExporter(
																feederConnector, token, null, null, null);
														myLabelProsesDetail.setValue("Mengirim data " + mahasiswa);
														ais.action.master.MahasiswaAction.exportKeFeeder(mahasiswa,
																feederImporter, token, feederConnector, errorLog);

														myLabelProsesDetail.setValue("");
													} catch (Exception e) {
														// FIX "gagal diam-diam": sebelumnya exception di sini hanya
														// dicatat ke log admin lalu progres diset "" (=SUKSES palsu)
														// di luar try, menutupi kegagalan (mis. gagal konek/parse data).
														ais.common.Common.tampilErrorJikaAdmin(e);
														myLabelProsesDetail.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
																"pengiriman data Mahasiswa \"" + mahasiswa.getNim() + " " + mahasiswa.getNama() + "\" ke Neo Feeder",
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

			}

			new Label(mahasiswa.getNama()).setParent(arg0);
			new Label(mahasiswa.getTahunangkatan() + "").setParent(arg0);
			new Label(mahasiswa.getTahunLulus() == null ? "" : mahasiswa.getTahunLulus() + "").setParent(arg0);

			new Label(mahasiswa.getWarganegara() == null ? "" : mahasiswa.getWarganegara()).setParent(arg0);

			new Label(mahasiswa.getNegara() == null ? "" : mahasiswa.getNegara().getNamaNegara()).setParent(arg0);

			KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa);

			Object[] objects = mahasiswa.ambilPerkuliahanDanParalel(tahunAkademik, jenisSemester, hr, keyword.trim(),
					"", merupakanPraPerkuliahan, ekstrakurikuler, true, merupakanRemedial, false,
					TampilanELearningAction.SKRIPSI, 0, 100);
			List<Skripsi> skripsis = (List<Skripsi>) objects[0];

			MyGroupboxStyled myGroupboxStyled = new MyGroupboxStyled();
			detail.appendChild(myGroupboxStyled);
			myGroupboxStyled.appendChild(new MyCaptionStyled(
					"Judul TA/Skripsi/Thesis \"" + mahasiswa.getNim() + " " + mahasiswa.getNama() + "\""));

			if (skripsis == null || skripsis.isEmpty()) {
				Label label = new Label();
				label.setParent(myGroupboxStyled);
				label.setValue(mahasiswa.getJudulSkripsi());
			} else {
				SkripsiAction.tampilkanJudul(skripsis.get(0)).setParent(myGroupboxStyled);
			}

			if (user != null && user.getMahasiswa() == null && user.getBiodataCalonMahasiswa() == null) {

				MyVboxStyled myVboxStyled = new MyVboxStyled();
				myVboxStyled.setParent(myGroupboxStyled);

				final A a = new A("Tampilkan Link Untuk Mengisi Tracer Study");
				a.setHref("");
				myVboxStyled.appendChild(a);

				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						String code = Common.getRequestHostWithProtocol() + "/m?q=" + URLEncoder.encode(
								Common.desEncrypter.get().encrypt(mahasiswa.getId() + "-Alumni-abcdefghijklmnopqrstuvwxyz"),
								"UTF-8");
						a.setLabel(code);
						a.setHref(Common.getRequestHostWithProtocol() + "/logoff?param="
								+ URLEncoder.encode(code, "UTF-8"));
					}
				});

				final A aa = new A("Tampilkan Link Untuk Pengguna Alumni");
				aa.setHref("");
				myVboxStyled.appendChild(aa);

				aa.setStyle("font-size:9px;");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						String code = Common.getRequestHostWithProtocol() + "/m?q="
								+ URLEncoder.encode(
										Common.desEncrypter.get().encrypt(
												mahasiswa.getId() + "-PenggunaLulusan-abcdefghijklmnopqrstuvwxyz"),
										"UTF-8");
						aa.setLabel(code);
						aa.setHref(Common.getRequestHostWithProtocol() + "/logoff?param="
								+ URLEncoder.encode(code, "UTF-8"));
					}
				});
			}

			objects = null;
			skripsis = null;

			vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getNama()).setParent(vbox);
			new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()).setParent(vbox);

			StatusMahasiswa statusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa).getStatusMahasiswa();
			new Label((statusMahasiswa.getNama()) + "/"
					+ (mahasiswa.getStatusAwalMahasiswa() == null ? "" : mahasiswa.getStatusAwalMahasiswa().getNama()))
					.setParent(arg0);

			new ais.ui.util.MyHtml(mahasiswa.getStatusKeluar() == null
					? mahasiswa.rubahKeteranganPengambilanKRS(krsMahasiswa.getSemester(), krsMahasiswa.getTahapan(),
							krsMahasiswa.getSemesterPendek(), krsMahasiswa, false)
					: (mahasiswa.getStatusKeluar().getNama()
							+ (mahasiswa.getPredikatKelulusan() == null ? ""
									: " / " + mahasiswa.getPredikatKelulusan().getNama())

							+ (mahasiswa.getStatusSetelahLulus() == null ? ""
									: " / " + mahasiswa.getStatusSetelahLulus().getNama())

							+ (mahasiswa.getStatusPekerjaanSetelahLulus() == null ? ""
									: " / " + mahasiswa.getStatusPekerjaanSetelahLulus().getNama())

							+ (mahasiswa.getStatusDomisiliSetelahLulus() == null ? ""
									: " / " + mahasiswa.getStatusDomisiliSetelahLulus().getNama())

					)).setParent(arg0);

			Tbmuser tbmuser = Common.getCurrentUser();

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
			// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/upload.gif");
			button.setVisible(edit && tbmuser != null);
			try {
				button.setVisible(Common.getApakahAdmin() || (tbmuser != null && tbmuser.hakAkses() != null
						&& ((ConstantValues.Akademik != null
								&& tbmuser.hakAkses().getRoleId().equals(ConstantValues.Akademik.getRoleId()))
								|| tbmuser.hakAkses().getRoleId().equals(ConstantValues.roleAdminFakultas.getRoleId())
								|| tbmuser.hakAkses().getRoleId()
										.equals(ConstantValues.roleAdminJurusan.getRoleId()))));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/alumni/MahasiswaAction.java:1299");
				// TODO: handle exception
			}

			button.setTooltiptext("Tampilan kartu studi mahasiswa");
			button.addEventListener("onClick", new EventListener() {

				TampilStudiMahasiswaHelper tampilStudiMahasiswaHelper = new TampilStudiMahasiswaHelper(null, null,
						false, edit);

				@Override
				public void onEvent(Event event) throws Exception {
					tampilStudiMahasiswaHelper.tampil(mahasiswa, MahasiswaAction.this, false);
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit && tbmuser != null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					ais.action.master.MahasiswaAction.onAddExternal(event, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
						}
					}, mahasiswa);
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete && tbmuser != null);
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
											MahasiswaDao mahasiswaDao = DaoFactory.getInstance().getMahasiswaDao();

											Session session = mahasiswaDao.getCurrentSession();
											List<BiodataMahasiswa> biodataMahasiswas = session
													.createCriteria(BiodataMahasiswa.class)
													.add(Restrictions.eq("mahasiswa", mahasiswa)).list();

											for (BiodataMahasiswa biodataMahasiswa : biodataMahasiswas) {
												session.delete(biodataMahasiswa);
											}

											Common.refreshDelete(mahasiswa);

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
			aksiButtons.add(button);

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(Mahasiswa.class)
				.add(searchkewarganegaraan == null || searchkewarganegaraan.getSelectedItem() == null
						|| searchkewarganegaraan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("warganegara", searchkewarganegaraan.getSelectedItem().getValue()))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (searchTelahMengisi.isChecked() || searchUrutkanTelahMengisi.isChecked()) {
			criteria = session.createCriteria(BiodataMahasiswa.class)
					.add(Restrictions.isNotNull("parameterTambahanIndsAlumni"))
					.add(Restrictions.ne("parameterTambahanIndsAlumni", ""))
					.setProjection(Projections.property("mahasiswa"));

			if (searchUrutkanTelahMengisi.isChecked()) {
				criteria.addOrder(Order.desc("tanggal_dirubah"));
			}

			criteria = criteria.createCriteria("mahasiswa");
		}

		if (order) {
			if (!searchUrutkanTelahMengisi.isChecked()) {
				criteria.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"));
			}
		}

		criteria.add(Restrictions.eq("statusKeluar.id", 1L))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("nim", searchnama.getValue().trim(), MatchMode.ANYWHERE)))

				.add(searchta.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("judulSkripsi", searchta.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(searchtahun.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunangkatan", searchtahun.getValue().intValue()))

				.add(searchtahunLulus.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tahunLulus", searchtahunLulus.getValue().intValue()))

				.add(searchsemesterLulus.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("semesterLulus", searchsemesterLulus.getValue().intValue()))

				.add(searchsemesterawal.getSelectedItem() == null
						|| searchsemesterawal.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("semesterMulai", searchsemesterawal.getSelectedItem().getValue()))

				.add(searchJenisSeleksi.getSelectedItem() == null
						|| searchJenisSeleksi.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("jenisSeleksi", searchJenisSeleksi.getSelectedItem().getValue()))

				.add(searchpredikatKelulusan.getSelectedItem() == null
						|| searchpredikatKelulusan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("predikatKelulusan",
										searchpredikatKelulusan.getSelectedItem().getValue()))

				.add(searchstatusSetelahLulus.getSelectedItem() == null
						|| searchstatusSetelahLulus.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("statusSetelahLulus",
										searchstatusSetelahLulus.getSelectedItem().getValue()))

				.add(searchstatusPekerjaanSetelahLulus.getSelectedItem() == null
						|| searchstatusPekerjaanSetelahLulus.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("statusPekerjaanSetelahLulus",
										searchstatusPekerjaanSetelahLulus.getSelectedItem().getValue()))

				.add(searchstatusDomisiliSetelahLulus.getSelectedItem() == null
						|| searchstatusDomisiliSetelahLulus.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("statusDomisiliSetelahLulus",
										searchstatusDomisiliSetelahLulus.getSelectedItem().getValue()))

				.createCriteria("jurusan", Criteria.LEFT_JOIN)

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))

				.add(searchjenjang.getSelectedItem() == null || searchjenjang.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenjang", searchjenjang.getSelectedItem().getValue()))

				.add(searchMasa.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.sqlRestriction(
								"this_.tahunlulus is not null and (this_.tahunlulus-this_.tahunangkatan)="
										+ searchMasa.getValue().intValue()));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		if (searchnama == null) {
			return;
		}

		List<Mahasiswa> mahasiswa = ConstantValues
				.simpleList(
						initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE).setFirstResult(
								Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
						Mahasiswa.class);

		ListModel strset = new SimpleListModel(mahasiswa);
		grid.setRowRenderer(new MahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	@Override
	public void loadData(Object value) {
		onSearchDefault(null);
	}

}
