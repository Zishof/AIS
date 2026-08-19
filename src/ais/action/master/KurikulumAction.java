package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
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
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Image;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.dashboard.admin.DashboardKurikulum;
import ais.action.master.dashboard.admin.DashboardRekapSilabusPerkuliahan;
import ais.action.master.dashboard.admin.DashboardStatistikJadwalKurikulum;
import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.feeder.util.FeederExporter;
import ais.action.master.feeder.util.FeederJSONImport;
import ais.action.master.helper.DetailSemesterKurikulumHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.obe.MatakuliahVsKurikulumAction;
import ais.action.report.format1.akademik.LaporanKurikulum;
import ais.action.report.format1.akademik.LaporanKurikulumMahasiswa;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.DataPunyaArtikel;
import ais.database.model.Fakultas;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Kurikulum;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.KurikulumPunyaMatakuliahDetail;
import ais.database.model.KurikulumPunyaMatakuliahPunyaItem;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Program;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.PertemuanFileContent;
import ais.database.model.streaming.AudioPertemuan;
import ais.database.model.streaming.VideoPertemuan;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelKecilSekali;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class KurikulumAction extends GenericAutowireComposer implements DataCriteria {
	private static final long serialVersionUID = 3786091220301468178L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;
	private Intbox tahun;
	private Textbox keterangan;

	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchjenjang;
	private Combobox searchprogram;
	private Intbox searchtahun;
	private Checkbox searchaktif;
	private Textbox searchmk;
	private Textbox searchnama;

	private Textbox nama;
	private Combobox jurusan;
	private Combobox program;
	private Combobox fakultas;

	private Combobox tahunAkademik;
	private Combobox jenisSemester;

	private Combobox tahunAkademikObe;
	private Combobox semesterObe;

	private Intbox jumlahAturanSksWajib;
	private Intbox jumlahAturanSksPilihan;
	private Intbox jumlahAturanSksLulus;

	private Decimalbox tahunAngkatanMulai;
	private Decimalbox tahunAngkatanSampai;

	private Checkbox nonAktifkanYgTerlanjurMengambilTidakSesuaiTahunAngkatan;

	private Kurikulum kurikulum;
	private Kurikulum asliKurikulum;
	private MyToolbarbuttonConfig add;
	private boolean edit;
	private boolean delete;

	private Label jenjang;
	private PerguruanTinggi perguruanTinggi;
	/**
	 * Tabbox utama halaman Kurikulum. Konten tiap tab dibangun LAZY oleh handler {@code onXxx} yang di
	 * zul disambungkan lewat {@code forward="onClick=..."} pada masing-masing Tab. Pada ZK 5.5, memilih
	 * tab memicu event <b>onSelect pada TABBOX</b> — {@code onClick} pada Tab tidak selalu terkirim —
	 * sehingga konten tab (mis. <b>Kesiapan OBE / Kurikulum OBE</b>) bisa TIDAK PERNAH terbangun dan tab
	 * tampak KOSONG walau datanya ada. Dispatcher {@link #onPilihTabKurikulum(Event)} menjembatani hal
	 * ini: saat tab dipilih, handler yang sesuai dipanggil. Semua handler idempoten (memeriksa
	 * {@code getChildren().size()==0}) sehingga aman bila terpanggil dua kali (onClick + onSelect).
	 */
	private org.zkoss.zul.Tabbox tabboxKurikulum;

	/** Muat konten tab sesuai tab yang DIPILIH (dipanggil dari forward onSelect pada tabbox). */
	public void onPilihTabKurikulum(Event event) throws Exception {
		if (tabboxKurikulum == null) {
			return;
		}
		int idx = tabboxKurikulum.getSelectedIndex();
		if (idx == 1) {
			onRps(event);
		} else if (idx == 2) {
			onKurikulum(event);
		} else if (idx == 3) {
			onTampilKurikulum(event);
		} else if (idx == 4) {
			onTampilKurikulumMahasiswa(event);
		} else if (idx == 5) {
			onTampilRekapKurikulum(event);
		} else if (idx == 6) {
			onRekapPertemuanPerkuliahan(event);
		} else if (idx == 7) {
			onStatistik(event);
		} else if (idx == 8) {
			onPikobe(event);
		}
	}

	private Tabpanel manajemenKurikulum;

	public void onKurikulum(Event event) {
		if (manajemenKurikulum.getChildren().size() == 0) {
			MatakuliahVsKurikulumAction laporan = new MatakuliahVsKurikulumAction();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(manajemenKurikulum);
		}
	}

	private Tabpanel manajemenRps;

	public void onRps(Event event) {
		if (manajemenRps.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenRps);
			MyInclude iframe = new MyInclude("/pages/master/rps_obe.zul");
			iframe.setParent(window);
		}
	}

	protected Tabpanel statistik;

	public void onStatistik(Event event) {

		if (statistik.getChildren().size() == 0) {
			DashboardKurikulum include = new DashboardKurikulum();
			ais.ui.util.BaseDasbordPortal.mountWrapped(include, statistik,
				"Statistik Kurikulum", "Gambaran sebaran mata kuliah, SKS total, dan kelengkapan silabus per kurikulum.");
		}
	}

	private Tabpanel pikobe;

	public void onPikobe(Event event) {
		if (pikobe.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(pikobe);
			MyInclude iframe = new MyInclude("/pages/master/obe/pikobe.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel laporanKurikulum;

	public void onTampilKurikulum(Event event) {
		if (this.laporanKurikulum.getChildren().size() == 0) {
			LaporanKurikulum laporanKurikulum = new LaporanKurikulum();
			laporanKurikulum.setHeight("100%");
			laporanKurikulum.setWidth("100%");
			laporanKurikulum.setParent(this.laporanKurikulum);
		}

	}

	private Tabpanel laporanKurikulumMahasiswa;

	public void onTampilKurikulumMahasiswa(Event event) {
		if (this.laporanKurikulumMahasiswa.getChildren().size() == 0) {
			LaporanKurikulumMahasiswa laporanKurikulum = new LaporanKurikulumMahasiswa();
			laporanKurikulum.setHeight("100%");
			laporanKurikulum.setWidth("100%");
			laporanKurikulum.setParent(this.laporanKurikulumMahasiswa);
		}

	}

	private Tabpanel rekapKurikulum;

	public void onTampilRekapKurikulum(Event event) {
		if (this.rekapKurikulum.getChildren().size() == 0) {
			DashboardStatistikJadwalKurikulum laporanKurikulum = new DashboardStatistikJadwalKurikulum();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporanKurikulum, rekapKurikulum,
				"Statistik Jadwal", "Ringkasan persebaran jadwal kuliah berdasarkan kurikulum yang aktif.");
		}

	}

	protected Tabpanel rekapitulasiPertemuanPerkuliahan;
	private Long selectedJurusan = null;
	private Integer selectedTahun = null;
	private Tbmuser tbmuser;
	private Textbox feeder;
	private MyCheckboxConfig obe;

	public void onRekapPertemuanPerkuliahan(Event event) {

		if (rekapitulasiPertemuanPerkuliahan.getChildren().size() == 0) {
			DashboardRekapSilabusPerkuliahan laporan = new DashboardRekapSilabusPerkuliahan();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, rekapitulasiPertemuanPerkuliahan,
				"Rekap Silabus", "Tingkat kelengkapan silabus perkuliahan untuk seluruh mata kuliah aktif.");
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	@SuppressWarnings("unchecked")
	public void doAfterCompose(Component comp) throws Exception {

		super.doAfterCompose(comp);
		Common.initLaguage();

		tbmuser = Common.getCurrentUser();

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE)
				&& Common.getCurrentUser().getMahasiswa() == null);
		add.setTooltiptext("Tambah");
		}

		if (execution.getParameter("jurusan") != null) {
			selectedJurusan = Long.parseLong(execution.getParameter("jurusan"));
		}
		if (execution.getParameter("tahun") != null) {
			selectedTahun = Integer.parseInt(execution.getParameter("tahun"));
		} else {
			if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
				Common.goLogoff();
				return;
			}
		}

		perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		program = new Combobox();
		List<Program> programs = HibernateUtil.currentSession().createCriteria(Program.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

		for (Program myProgram : programs) {
			MyComboitemConfig comboitem = new MyComboitemConfig(myProgram.getNamaBaru());
			comboitem.setValue(myProgram);
			program.appendChild(comboitem);

			comboitem = new MyComboitemConfig(myProgram.getNamaBaru());
			comboitem.setValue(myProgram);
			searchprogram.appendChild(comboitem);
		}

		MyComboitemConfig comboitem = new MyComboitemConfig("Semua");
		if (comboitem != null) { comboitem.setValue(null); }
		program.appendChild(comboitem);
		if (program != null) { program.setReadonly(true); }

		Common.checkProgramString(searchprogram, true);

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusan(fakultas, jurusan, searchfakultas, searchjurusan);

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.insertComboDanSemua(searchjenjang, "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		if (tbmuser != null && tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.ambilDosen() == null) {

			MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, "id", "nama", "tahun", "jurusan",
					"program", "tahunAkademik", "jenisSemester", "jumlahAturanSksWajib", "jumlahAturanSksPilihan",
					"jumlahAturanSksLulus", "tahunAngkatanMulai", "tahunAngkatanSampai",
					"nonAktifkanYgTerlanjurMengambilTidakSesuaiTahunAngkatan", "keterangan");
			Common.appendKeToolbar(cetakToolbarbutton, add, comp);

			if (ConstantValues.aktifkanTahapan) {
				MyToolbarbuttonConfig simkronkanTahap = new MyToolbarbuttonConfig("Singkronkan Tahap dengan KRS",
						"/img/svg/check2.svg");
				Common.appendKeToolbar(simkronkanTahap, add, comp);
				simkronkanTahap.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();
								String sql = "update detailperkuliahan a set tahap = (select aa.tahap from kurikulum_punya_matakuliah aa inner join perkuliahan bb on (aa.id=bb.kurikulum_punya_matakuliah) where bb.id=a.perkuliahan) "
										+ " where a.perkuliahan is not null and a.tahap is null;";
								int status = session.createSQLQuery(sql).executeUpdate();

								System.out.println("sql " + sql + ",  status " + status);
								MyMessageboxConfig.show("Singkronkan Tahap dengan KRS berhasil dilakukan", "Informasi",
										MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);

							}
						});
					}
				});
			}

			MyToolbarbuttonConfig simkronkanTahap = new MyToolbarbuttonConfig("Singkronkan Silabus",
					"/img/svg/check2.svg");
			Common.appendKeToolbar(simkronkanTahap, add, comp);
			simkronkanTahap.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					copySilabusDariPerkuliahan(null, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(arg0);
						}
					});
				}
			});

			MyToolbarbuttonConfig prosesUlang = prosesUlangTagihan("Gabungkan Kurikulum", "/img/excel.png");
			Common.appendKeToolbar(prosesUlang, add, comp);
		} else {
			edit = false;
			delete = false;

			if (add != null)
				add.setVisible(false);

			if (manajemenKurikulum != null) {
				manajemenKurikulum.setVisible(false);
				manajemenKurikulum.getLinkedTab().setVisible(false);
			}

			if (statistik != null) {
				statistik.setVisible(false);
				statistik.getLinkedTab().setVisible(false);
			}

			if (laporanKurikulum != null) {
				laporanKurikulum.setVisible(false);
				laporanKurikulum.getLinkedTab().setVisible(false);
			}

			if (laporanKurikulumMahasiswa != null) {
				laporanKurikulumMahasiswa.setVisible(false);
				laporanKurikulumMahasiswa.getLinkedTab().setVisible(false);
			}

			if (rekapKurikulum != null) {
				rekapKurikulum.setVisible(false);
				rekapKurikulum.getLinkedTab().setVisible(false);
			}

			if (rekapitulasiPertemuanPerkuliahan != null) {
				rekapitulasiPertemuanPerkuliahan.setVisible(false);
				rekapitulasiPertemuanPerkuliahan.getLinkedTab().setVisible(false);
			}
		}

		if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
				&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {

			MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Kirim ke Feeder",
					"/img/Finance-Invoice-icon.png");
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

											@Override
											public void run() {
												try {
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

													List<Kurikulum> tbmusers = ConstantValues
															.simpleList(initCriteria(true), Kurikulum.class);
													int size = tbmusers.size();
													int index = 1;
													for (Kurikulum kurikulum : tbmusers) {
														myLabelProsesDetail.setValue("Memproses " + kurikulum.getNama()
																+ " ("
																+ Common.numberFormat.get().format((index * 100.0) / size)
																+ "%");
														index++;
														feederImporter.kurikulum(kurikulum, errorLog);

														Session session = HibernateUtil.currentNativeSession();
														List<KurikulumPunyaMatakuliah> kurikulumPunyaMatakuliahs = ConstantValues
																.simpleList(session
																		.createCriteria(KurikulumPunyaMatakuliah.class)
																		.add(Restrictions.eq("kurikulum", kurikulum))
																		.add(Restrictions.or(
																				Restrictions.isNull("aktif"),
																				Restrictions.eq("aktif", true))),
																		KurikulumPunyaMatakuliah.class);
														// session.disconnect();
														ais.common.Common.closeOpenedSession(session);

														for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kurikulumPunyaMatakuliahs) {
															feederImporter.kurikulumPunyaMatakuliah(
																	kurikulumPunyaMatakuliah, errorLog);
														}
													}
													tbmusers.clear();
													tbmusers = null;
												} catch (Exception e) {
													ais.common.Common.tampilErrorJikaAdmin(e);
												}

												myLabelProsesDetail.setValue("");
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
			Common.appendKeToolbar(buttonTagihan, add, comp);

			buttonTagihan = new MyToolbarbuttonConfig("Ambil dari feeder", "/img/Button-Refresh-icon.png");
			buttonTagihan.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin mengambil data dari feeder ?", "Pertanyaan",
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

													String filter = "";

													if (!searchnama.getValue().trim().isEmpty()) {
														for (String o : searchnama.getValue().trim().split(";")) {
															String s = "nama_kurikulum like '%" + o + "%'";
															filter += filter.isEmpty() ? s : " or " + s;
														}
													}

													if (searchtahun.getValue() != null) {
														String s = "(id_semester='" + searchtahun.getValue().intValue()
																+ "1' or id_semester='"
																+ searchtahun.getValue().intValue() + "2')";
														filter += filter.isEmpty() ? s : " and " + s;
													}

													Jurusan jur = (Jurusan) (searchjurusan.getSelectedItem() == null
															|| searchjurusan.getSelectedItem().getValue() == null ? null
																	: searchjurusan.getSelectedItem().getValue());
													if (jur != null && jur.getFeeder() != null
															&& !jur.getFeeder().isEmpty()) {
														String s = "id_prodi='" + jur.getFeeder() + "'";
														filter += filter.isEmpty() ? s : " and " + s;
													}

													Integer countInteger = feederConnector.getCount(token,
															"GetCountKurikulum", filter);

													String or = "id_semester desc";

													for (int index = 0; index <= countInteger; index += 500) {

														JSONArray dataMataKuliah = feederConnector.getData(
																"GetListKurikulum", token, filter, or, "500",
																index + "");

														for (int i = 0; i < dataMataKuliah.length(); i++) {
															JSONObject kur = dataMataKuliah.getJSONObject(i);
															FeederJSONImport.kurikulum(kur);

															JSONArray dataMatkulKurikulum = feederConnector.getData(
																	"GetMatkulKurikulum", token, "id_kurikulum='"
																			+ kur.getString("id_kurikulum") + "'",
																	"", "5000", "0");

															for (int j = 0; j < dataMatkulKurikulum.length(); j++) {

																JSONObject kurSub = dataMatkulKurikulum
																		.getJSONObject(j);
																FeederJSONImport.kurikulumPunyaMatakuliah(kurSub);
															}

														}

													}

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
		}
	        FilterLanjutHelper.setup(comp);
}

	public MyToolbarbuttonConfig prosesUlangTagihan(String buttonLabel, String buttonImage) {

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(buttonLabel, buttonImage);

		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final MyWindow window = new MyWindow("Pilih Tahun Kurikulum", "none", true);
				window.setParent(page.getFirstRoot());
				window.setHeight("300px");
				window.setWidth("600px");
				final Combobox tahunAkademik = new Combobox();
				Common.generateTahunAjaran(tahunAkademik);

				final Combobox tahunAkademikSampai = new Combobox();
				Common.generateTahunAjaran(tahunAkademikSampai);

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
				column.setWidth("20%");
				column.setParent(columns);
				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				final Combobox fakultas;
				final Combobox jurusan;
				fakultas = new Combobox();
				jurusan = new Combobox();
				Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas *"));
				row.appendChild(fakultas);
				fakultas.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Prodi *"));
				row.appendChild(jurusan);
				jurusan.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Program *"));
				final Combobox program;
				row.appendChild(program = new Combobox());
				Common.initPrograms(program);
				program.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tahun *"));
				final Intbox tahun;
				row.appendChild(tahun = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)));
				jurusan.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kurikulum Baru *"));
				final MyTextbox nama;
				row.appendChild(nama = new MyTextbox());
				nama.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				final MyCheckboxConfig nonaktifkan;
				row.appendChild(
						nonaktifkan = new MyCheckboxConfig("Non aktifkan kurikulum yang lama setelah digabung"));
				jurusan.setWidth("90%");
				nonaktifkan.setChecked(true);

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
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Gabungkan Kurikulum", "/img/save.gif");
				save.setTooltiptext("Proses");
				save.addEventListener("onClick", new EventListener() {
					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {

						final Fakultas fak = (Fakultas) (fakultas.getSelectedItem() == null ? null
								: fakultas.getSelectedItem().getValue());
						final Jurusan jur = (Jurusan) (jurusan.getSelectedItem() == null ? null
								: jurusan.getSelectedItem().getValue());

						final String prog = (String) (program.getSelectedItem() == null ? null
								: program.getSelectedItem().getValue());
						final Integer thn = tahun.getValue();
						final String nm = nama.getValue().trim();
						if (fak == null) {
							PesanFormalHelper.tampilkanGagal("penyimpanan data Fakultas",
									"Kolom Fakultas belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
									new String[] {
											"Isi/pilih terlebih dahulu Fakultas.",
											"Ulangi proses penyimpanan setelah kolom tersebut terisi."
									});
							return;
						}
						if (jur == null) {
							PesanFormalHelper.tampilkanGagal("penyimpanan data Prodi",
									"Kolom Prodi belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
									new String[] {
											"Isi/pilih terlebih dahulu Prodi.",
											"Ulangi proses penyimpanan setelah kolom tersebut terisi."
									});
							return;
						}
						if (prog == null) {
							PesanFormalHelper.tampilkanGagal("penyimpanan data Program",
									"Kolom Program belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
									new String[] {
											"Isi/pilih terlebih dahulu Program.",
											"Ulangi proses penyimpanan setelah kolom tersebut terisi."
									});
							return;
						}
						if (thn == null) {
							PesanFormalHelper.tampilkanGagal("penyimpanan data Tahun",
									"Kolom Tahun belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
									new String[] {
											"Isi/pilih terlebih dahulu Tahun.",
											"Ulangi proses penyimpanan setelah kolom tersebut terisi."
									});
							return;
						}
						if (nm.isEmpty()) {
							PesanFormalHelper.tampilkanGagal("penyimpanan data Nama",
									"Kolom Nama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
									new String[] {
											"Isi/pilih terlebih dahulu Nama.",
											"Ulangi proses penyimpanan setelah kolom tersebut terisi."
									});
							return;
						}

						Session session = HibernateUtil.currentSession();
						List<Kurikulum> kurikulums = session.createCriteria(Kurikulum.class)
								.createAlias("program", "program").add(Restrictions.eq("jurusan", jur))
								.add(Restrictions.ilike("program.nama", prog, MatchMode.EXACT))
								.add(Restrictions.eq("tahun", thn))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.list();

						if (kurikulums.isEmpty()) {
							MyMessageboxConfig.show("Kurikulum yang akan digabung tidak ditemukan", "Peringatan",
									MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}

						Kurikulum kurikulumBaru = (Kurikulum) kurikulums.get(0).clone();
						kurikulumBaru.setNama(nm);
						kurikulumBaru.setId(null);
						session.save(kurikulumBaru);

						for (Kurikulum kurikulum : kurikulums) {

							List<KurikulumPunyaMatakuliah> kurikulumPunyaMatakuliahs = ConstantValues.simpleList(session
									.createCriteria(KurikulumPunyaMatakuliah.class)
									.createAlias("matakuliah", "matakuliah")
									.add(Restrictions.eq("kurikulum", kurikulum))
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.or(Restrictions.isNull("matakuliah.aktif"),
											Restrictions.eq("matakuliah.aktif", true)))
									.addOrder(Order.asc("semester")), KurikulumPunyaMatakuliah.class);

							for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kurikulumPunyaMatakuliahs) {
								KurikulumPunyaMatakuliah baru = (KurikulumPunyaMatakuliah) kurikulumPunyaMatakuliah
										.clone();
								baru.setId(null);
								baru.setKurikulum(kurikulumBaru);
								session.save(baru);
							}

							if (nonaktifkan.isChecked()) {
								kurikulum.setAktif(false);
								Common.refreshUpdate(session, kurikulum);
							}
						}

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
								window.detach();
							}
						});

					}
				});
				save.setParent(toolbar);

				window.onModal();

			}
		});

		return toolbarbutton;
	}

	class KurikulumRenderer extends ais.ui.util.MyRowRenderer {
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");

			final Kurikulum kurikulum = (Kurikulum) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						DetailSemesterKurikulumHelper detailSemesterKurikulumHelper = new DetailSemesterKurikulumHelper();
						detailSemesterKurikulumHelper.display(kurikulum, detail, addWindow);
					}
				}
			});

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			RevisiHelper.createNewRevisi(Kurikulum.class, kurikulum, kurikulum.getTahun() + " ").setParent(vbox);
			new Label((kurikulum.getId() == null ? "" : "ID:" + kurikulum.getId())).setParent(vbox);

			new Label(kurikulum.getNama()).setParent(arg0);

			new Label(kurikulum.getJurusan() == null ? "" : kurikulum.getJurusan().getNama()).setParent(arg0);
			new Label(kurikulum.getJurusan() == null || kurikulum.getJurusan().getJenjang() == null ? ""
					: kurikulum.getJurusan().getJenjang().getNama()).setParent(arg0);
			new Label(kurikulum.getProgram() == null ? "Semua" : kurikulum.getProgram().getNama()).setParent(arg0);

			String aturan = "<font style='font-size:9px;'>" + "<ol>";
			aturan += "<li>Pilihan: " + Common.numberFormat.get().format(kurikulum.getJumlahAturanSksPilihan()) + "</li>";
			aturan += "<li>Wajib: " + Common.numberFormat.get().format(kurikulum.getJumlahAturanSksWajib()) + "</li>";
			aturan += "<li>Jml. SKS : " + Common.numberFormat.get().format(kurikulum.getJumlahAturanSksLulus()) + "</li>";
			aturan += "</ol>" + "</font>";

			new ais.ui.util.MyHtml(aturan).setParent(arg0);

			final Html realisasi = new ais.ui.util.MyHtml();
			realisasi.setParent(arg0);

			new Label(kurikulum.getObe() ? "Ya" : "Tidak").setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(
					!edit || tbmuser == null || tbmuser.getMahasiswa() != null || tbmuser.ambilDosen() != null);
			checkbox.setChecked(kurikulum.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kurikulum.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(kurikulum);
				}
			});

			final MyToolbarbuttonConfig buttonDelete = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");

			Common.createDefaultTimer(new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event arg0) throws Exception {

					Session session = HibernateUtil.currentSession();
					Number sks = ((Number) session.createCriteria(KurikulumPunyaMatakuliah.class)
							.createAlias("matakuliah", "matakuliah").add(Restrictions.eq("kurikulum", kurikulum))
							.add(Restrictions.isNotNull("semester")).setProjection(Projections.sum("matakuliah.sks"))
							.uniqueResult());

					Number wajib = ((Number) session.createCriteria(KurikulumPunyaMatakuliah.class)
							.createAlias("matakuliah", "matakuliah")
							.add(Restrictions.ilike("matakuliah.status", "Wajib"))
							.add(Restrictions.eq("kurikulum", kurikulum)).add(Restrictions.isNotNull("semester"))
							.setProjection(Projections.sum("matakuliah.sks")).uniqueResult());

					Number pilihan = ((Number) session.createCriteria(KurikulumPunyaMatakuliah.class)
							.createAlias("matakuliah", "matakuliah")
							.add(Restrictions.ilike("matakuliah.status", "Pilihan"))
							.add(Restrictions.eq("kurikulum", kurikulum)).add(Restrictions.isNotNull("semester"))
							.setProjection(Projections.sum("matakuliah.sks")).uniqueResult());

					String real = "<font style='font-size:9px;'>" + "<ol>";
					real += "<li>Pilihan: " + (pilihan == null ? "0" : Common.numberFormat.get().format(pilihan.intValue()))
							+ "</li>";
					real += "<li>Wajib: " + (wajib == null ? "0" : Common.numberFormat.get().format(wajib.intValue()))
							+ "</li>";
					real += "<li>Jml. SKS : " + (sks == null ? "0" : Common.numberFormat.get().format(sks.intValue()))
							+ "</li>";

					if (ConstantValues.aktifkanTahapanKurikulum) {

						List<Object[]> tahaps = HibernateUtil.currentSession()
								.createSQLQuery("select tahap,count(*) as jumlah from kurikulum_punya_matakuliah "
										+ " where kurikulum = " + kurikulum.getId() + " group by tahap order by tahap")
								.list();

						for (Object[] objects : tahaps) {
							Integer tahap = objects[0] == null ? null : ((Number) objects[0]).intValue();
							Integer jumlah = objects[1] == null ? 0 : ((Number) objects[1]).intValue();
							String s = (tahap == null ? "Tanpa tahap" : "Tahap " + tahap) + " terdapat " + jumlah
									+ " matakuliah";
							real += "<li>" + s + "</li>";
						}
					}

					int count = ((Number) session.createCriteria(Perkuliahan.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.createAlias("kurikulumPunyaMatakuliah", "kurikulumPunyaMatakuliah")
							.add(Restrictions.eq("kurikulumPunyaMatakuliah.kurikulum", kurikulum))
							.setProjection(Projections.rowCount()).uniqueResult()).intValue();
					if (Common.bolehKonfigurasi("kurikulum_yang_sudah_dijadwal_tidak_bisa_dihapus")) {
						buttonDelete.setDisabled(count > 0);
					}
					real += "<li>Jml. Jadwal : " + (Common.numberFormat.get().format(count)) + "</li>";

					real += "</ol>" + "</font>";

					realisasi.setContent(real);

				}
			});

			new Label(kurikulum.getTahunAkademik() + " " + kurikulum.getJenisSemester()).setParent(arg0);

			new Label((kurikulum.getTahunAngkatanMulai() == null ? "" : kurikulum.getTahunAngkatanMulai())
					+ (kurikulum.getTahunAngkatanSampai() == null ? "" : " s.d " + kurikulum.getTahunAngkatanSampai()))
					.setParent(arg0);

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
			// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit && Common.getCurrentUser().getMahasiswa() == null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(kurikulum, false);
					addWindow.setVisible(true);
					addWindow.onModal();
				}
			});
			aksiButtons.add(button);

			MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("", "/img/svg/edit-copy.svg");
			save.setTooltiptext("Copy Data");
			save.setVisible(edit && Common.getCurrentUser().getMahasiswa() == null);
			save.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					init(kurikulum, true);
					addWindow.setVisible(true);
					addWindow.onModal();

				}
			});

			aksiButtons.add(save);

			buttonDelete.setTooltiptext("Hapus Data");
			buttonDelete.setVisible(delete && Common.getCurrentUser().getMahasiswa() == null);

			buttonDelete.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Session session = HibernateUtil.currentSession();

											List<KurikulumPunyaMatakuliah> kurikulumPunyaMatakuliahs = session
													.createCriteria(KurikulumPunyaMatakuliah.class)
													.add(Restrictions.eq("kurikulum", kurikulum)).list();

											for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kurikulumPunyaMatakuliahs) {
												session.delete(kurikulumPunyaMatakuliah);
											}

											Common.refreshDelete(kurikulum);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show("Data ini tidak dapat dihapus");
										}
									}

								}
							});

				}
			});
			aksiButtons.add(buttonDelete);

			Vbox vbox3 = new Vbox();
			vbox3.setParent(arg0);
			// Sel aksi juga memuat konten non-tombol (status feeder), jadi kebab dipasang
			// ke vbox3 (bukan ke Row) agar jumlah kolom grid tidak bertambah.
			ais.ui.util.UIHelper.buatBarisAksi(vbox3, 3, aksiButtons);

			Hbox myHbox = new Hbox();
			myHbox.setParent(vbox3);

			if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
					&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {

				if (kurikulum.getFeeder() != null && !kurikulum.getFeeder().trim().isEmpty()) {
					myHbox.appendChild(new Image("/img/svg/check2-circle.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder valid"));
				} else {
					myHbox.appendChild(new Image("/img/svg/warning-outline.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder blm valid"));
				}

				MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Krm ke feeder",
						"/img/Finance-Invoice-icon.png");
				buttonTagihan.setStyle("font-size:8px;");
				buttonTagihan.setParent(vbox3);
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

												@SuppressWarnings("unchecked")
												@Override
												public void run() {
													try {
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
														myLabelProsesDetail.setValue("Mengirim data " + kurikulum);

														feederImporter.kurikulum(kurikulum, errorLog);

														Session session = HibernateUtil.currentNativeSession();
														List<KurikulumPunyaMatakuliah> kurikulumPunyaMatakuliahs = ConstantValues
																.simpleList(session
																		.createCriteria(KurikulumPunyaMatakuliah.class)
																		.add(Restrictions.eq("kurikulum", kurikulum))
																		.add(Restrictions.or(
																				Restrictions.isNull("aktif"),
																				Restrictions.eq("aktif", true))),
																		KurikulumPunyaMatakuliah.class);
														// session.disconnect();
														ais.common.Common.closeOpenedSession(session);

														for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kurikulumPunyaMatakuliahs) {
															feederImporter.kurikulumPunyaMatakuliah(
																	kurikulumPunyaMatakuliah, errorLog);
														}

													} catch (Exception e) {
														ais.common.Common.tampilErrorJikaAdmin(e);
													}

													myLabelProsesDetail.setValue("");
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

			}
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new Kurikulum(), false);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(final Kurikulum kurikulum, Boolean copy) throws Exception {
		this.kurikulum = kurikulum;
		if (copy) {
			addWindow.setTitle("Copy Kurikulum");
			this.asliKurikulum = new Kurikulum();
			this.asliKurikulum.setId(kurikulum.getId());
		} else {
			addWindow.setTitle(kurikulum.getId() == null ? "Tambah Kurikulum" : "Ubah Kurikulum");
		}
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(addWindow);
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kurikulum *"));
		row.appendChild(nama = new Textbox(kurikulum.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun *"));
		row.appendChild(
				tahun = new Intbox(kurikulum.getTahun() == null ? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)
						: kurikulum.getTahun()));
		tahun.setWidth("90%");

		Tbmuser tbmuser = Common.getCurrentUser();

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas *"));
		Common.selectComboItem(fakultas,
				kurikulum.getJurusan() == null ? tbmuser.ambilFakultas() : kurikulum.getJurusan().getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi *"));
		Common.pilihJurusan(jurusan,
				kurikulum.getJurusan() == null ? tbmuser.ambilJurusan() : kurikulum.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenjang *"));
		row.appendChild(jenjang = new Label());
		jenjang.setWidth("90%");

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Jurusan myJurusan = (Jurusan) (jurusan.getSelectedItem() == null
						|| jurusan.getSelectedItem().getValue() == null ? null : jurusan.getSelectedItem().getValue());
				if (myJurusan != null && myJurusan.getJenjang() != null) {
					jenjang.setValue(myJurusan.getJenjang().getNama());
				}
			}
		};

		jurusan.addEventListener("onChange", eventListener);
		eventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		Common.selectComboItem(program, kurikulum.getProgram());
		row.appendChild(program);
		program.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai Berlaku Tahun Akademik"));
		row.appendChild(tahunAkademik = new Combobox());
		Common.generateTahunAjaranDanSemua(tahunAkademik);
		Common.selectComboItem(tahunAkademik, kurikulum.getTahunAkademik());
		tahunAkademik.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai Berlaku Semester"));
		row.appendChild(jenisSemester = new Combobox());

		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setValue(Perkuliahan.GANJIL);
		comboitem.setLabel(Perkuliahan.GANJIL);
		jenisSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setValue(Perkuliahan.GENAP);
		comboitem.setLabel(Perkuliahan.GENAP);
		jenisSemester.appendChild(comboitem);
		Common.selectComboItem(jenisSemester, kurikulum.getJenisSemester());
		jenisSemester.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Apakah kurikulum OBE ?"));
		obe = new MyCheckboxConfig("OBE");
		obe.setDisabled(!edit || tbmuser == null || tbmuser.getMahasiswa() != null || tbmuser.ambilDosen() != null);
		obe.setChecked(kurikulum.getObe());
		obe.setParent(row);

		tahunAkademikObe = new Combobox();
		comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		tahunAkademikObe.appendChild(comboitem);
		tahunAkademikObe = Common.generateTahunAjaranDanSemua(tahunAkademikObe);

		semesterObe = new Combobox();
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		semesterObe.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semesterObe.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semesterObe.appendChild(comboitem);

		row = new MyFormRow();
		row.setParent(rows);
		final ais.ui.util.MyLabelConfig labelTahunAkademikObe = new ais.ui.util.MyLabelConfig(
				obe.isChecked() ? "Berlaku Mulai Tahun Akademik OBE *" : "Berlaku Mulai Tahun Akademik OBE");
		row.appendChild(labelTahunAkademikObe);
		row.appendChild(tahunAkademikObe);
		tahunAkademikObe.setWidth("90%");
		Common.selectComboItem(tahunAkademikObe, kurikulum.getTahunAkademikObe());

		obe.addEventListener("onCheck", new org.zkoss.zk.ui.event.EventListener() {
			@Override
			public void onEvent(org.zkoss.zk.ui.event.Event event) throws Exception {
				if (obe.isChecked()) {
					labelTahunAkademikObe.setValueData("Berlaku Mulai Tahun Akademik OBE *");
				} else {
					labelTahunAkademikObe.setValueData("Berlaku Mulai Tahun Akademik OBE");
				}
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Berlaku Mulai Semester OBE"));
		row.appendChild(semesterObe);
		semesterObe.setWidth("90%");
		Common.selectComboItem(semesterObe, kurikulum.getSemesterObe());
		semesterObe.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Aturan SKS Wajib"));
		row.appendChild(jumlahAturanSksWajib = new Intbox(kurikulum.getJumlahAturanSksWajib()));
		jumlahAturanSksWajib.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Aturan SKS Pilihan"));
		row.appendChild(jumlahAturanSksPilihan = new Intbox(kurikulum.getJumlahAturanSksPilihan()));
		jumlahAturanSksPilihan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Aturan SKS Lulus"));
		row.appendChild(jumlahAturanSksLulus = new Intbox(kurikulum.getJumlahAturanSksLulus()));
		jumlahAturanSksLulus.setWidth("90%");
		jumlahAturanSksLulus.setDisabled(true);

		EventListener eventListenerTahun = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				kurikulum.setTahun(tahun.getValue());
				tahunAkademik.setValue(kurikulum.getTahunAkademik());

				int wajib = jumlahAturanSksWajib.getValue() == null ? 0 : jumlahAturanSksWajib.getValue();
				int pilihan = jumlahAturanSksPilihan.getValue() == null ? 0 : jumlahAturanSksPilihan.getValue();

				jumlahAturanSksLulus.setValue(wajib + pilihan);
			}
		};

		tahun.addEventListener("onChange", eventListenerTahun);
		jumlahAturanSksWajib.addEventListener("onChange", eventListenerTahun);
		jumlahAturanSksPilihan.addEventListener("onChange", eventListenerTahun);
		eventListenerTahun.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan Mulai"));
		row.appendChild(tahunAngkatanMulai = new Decimalbox(
				kurikulum.getTahunAngkatanMulai() == null ? null : new BigDecimal(kurikulum.getTahunAngkatanMulai())));

		Common.initKeterangan(rows,
				"Jika tahun angkatan mulai dikosongkan, artinya kurikulum ini berlaku mulai tahun angkatan kapan pun");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan Sampai"));
		row.appendChild(tahunAngkatanSampai = new Decimalbox(kurikulum.getTahunAngkatanSampai() == null ? null
				: new BigDecimal(kurikulum.getTahunAngkatanSampai())));

		Common.initKeterangan(rows,
				"Jika tahun angkatan sampai dikosongkan, artinya kurikulum ini berlaku sampai tahun angkatan kapan pun");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(nonAktifkanYgTerlanjurMengambilTidakSesuaiTahunAngkatan = new MyCheckboxConfig(
				"Batalkan persetujuan KRS mahasiswa yang telah terlanjur mengambil tidak sesuai tahun angkatan"));
		nonAktifkanYgTerlanjurMengambilTidakSesuaiTahunAngkatan
				.setChecked(kurikulum.getNonAktifkanYgTerlanjurMengambilTidakSesuaiTahunAngkatan());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(kurikulum.getKeterangan() == null ? "" : kurikulum.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		keterangan.setRows(4);

		row = new MyFormRow();
		row.setVisible(Common.getApakahAdminBolehAksesFeeder());
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Feeder"));
		row.appendChild(feeder = new Textbox(kurikulum.getFeeder()));
		feeder.setWidth("90%");

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

		if (copy) {
			MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Copy", "/img/save.gif");
			save.setTooltiptext("Simpan");
			save.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							KurikulumAction.this.kurikulum.setId(null);
							if (onSaveCopy(arg0)) {
								onSearchDefault(null);
								addWindow.setVisible(false);
							}
						}
					});
				}
			});
			save.setParent(toolbar);
		} else {
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
		}

	}

	public boolean onSave(Event event) throws Exception {
		if (tahun.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tahun",
					"Kolom Tahun belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tahun.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show(Common.getBahasaConfig("Jurusan") + " harus diisi", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (tahunAkademik.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tahun Akademik",
					"Kolom Tahun Akademik belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tahun Akademik.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (jenisSemester.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis semester",
					"Kolom Jenis semester belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jenis semester.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (obe.isChecked()
				&& (tahunAkademikObe.getSelectedItem() == null
						|| tahunAkademikObe.getSelectedItem().getValue() == null)) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kurikulum OBE",
					"Kolom \"Berlaku Mulai Tahun Akademik OBE\" wajib diisi jika kurikulum OBE diaktifkan.",
					new String[] {
							"Pilih Tahun Akademik OBE terlebih dahulu.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (kurikulum.getId() != null) {
			kurikulum = (Kurikulum) session.load(Kurikulum.class, kurikulum.getId());
		}
		kurikulum.setNama(nama.getValue().trim());
		kurikulum.setProgram(
				(Program) (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null ? null
						: program.getSelectedItem().getValue()));
		kurikulum.setTahun(tahun.getValue());
		kurikulum.setJurusan((Jurusan) jurusan.getSelectedItem().getValue());
		kurikulum.setKeterangan(keterangan.getValue());
		// kurikulum.setNama("Kurikulum " + jurusan.getSelectedItem().getLabel()
		// + " " + tahun.getValue());
		kurikulum.setJenisSemester((String) jenisSemester.getSelectedItem().getValue());
		kurikulum.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		kurikulum.setJumlahAturanSksLulus(jumlahAturanSksLulus.getValue());
		kurikulum.setJumlahAturanSksPilihan(jumlahAturanSksPilihan.getValue());
		kurikulum.setJumlahAturanSksWajib(jumlahAturanSksWajib.getValue());
		kurikulum.setTahunAngkatanMulai(
				tahunAngkatanMulai.getValue() == null ? null : tahunAngkatanMulai.getValue().intValue());
		kurikulum.setTahunAngkatanSampai(
				tahunAngkatanSampai.getValue() == null ? null : tahunAngkatanSampai.getValue().intValue());
		kurikulum.setNonAktifkanYgTerlanjurMengambilTidakSesuaiTahunAngkatan(
				nonAktifkanYgTerlanjurMengambilTidakSesuaiTahunAngkatan.isChecked());
		kurikulum.setFeeder(feeder.getValue());
		kurikulum.setObe(obe.isChecked());

		kurikulum.setTahunAkademikObe((String) (tahunAkademikObe.getSelectedItem() == null
				|| tahunAkademikObe.getSelectedItem().getValue() == null ? null
						: tahunAkademikObe.getSelectedItem().getValue()));
		kurikulum.setSemesterObe(
				(String) (semesterObe.getSelectedItem() == null ? null : semesterObe.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(kurikulum);

		return true;
	}

	@SuppressWarnings("unchecked")
	public boolean onSaveCopy(Event event) throws Exception {
		if (tahun.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tahun",
					"Kolom Tahun belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tahun.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show(Common.getBahasaConfig("Jurusan") + " harus diisi", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (kurikulum.getId() != null) {
			kurikulum = (Kurikulum) session.load(Kurikulum.class, kurikulum.getId());
		}
		kurikulum.setNama(nama.getValue().trim());
		kurikulum.setProgram(
				(Program) (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null ? null
						: program.getSelectedItem().getValue()));
		kurikulum.setTahun(tahun.getValue());
		kurikulum.setJurusan((Jurusan) jurusan.getSelectedItem().getValue());
		kurikulum.setKeterangan(keterangan.getValue());
		kurikulum.setJenisSemester((String) jenisSemester.getSelectedItem().getValue());
		kurikulum.setTahunAkademik(tahunAkademik.getValue());
		kurikulum.setJumlahAturanSksLulus(jumlahAturanSksLulus.getValue());
		kurikulum.setJumlahAturanSksPilihan(jumlahAturanSksPilihan.getValue());
		kurikulum.setJumlahAturanSksWajib(jumlahAturanSksWajib.getValue());

		kurikulum.setTahunAngkatanMulai(
				tahunAngkatanMulai.getValue() == null ? null : tahunAngkatanMulai.getValue().intValue());
		kurikulum.setTahunAngkatanSampai(
				tahunAngkatanSampai.getValue() == null ? null : tahunAngkatanSampai.getValue().intValue());
		kurikulum.setNonAktifkanYgTerlanjurMengambilTidakSesuaiTahunAngkatan(
				nonAktifkanYgTerlanjurMengambilTidakSesuaiTahunAngkatan.isChecked());
		kurikulum.setFeeder("");
		kurikulum.setObe(obe.isChecked());

		kurikulum.setTahunAkademikObe((String) (tahunAkademikObe.getSelectedItem() == null
				|| tahunAkademikObe.getSelectedItem().getValue() == null ? null
						: tahunAkademikObe.getSelectedItem().getValue()));
		kurikulum.setSemesterObe(
				(String) (semesterObe.getSelectedItem() == null ? null : semesterObe.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(session, kurikulum);
		session.flush();

		List<KurikulumPunyaMatakuliah> kurikulumPunyaMatakuliahs = session
				.createCriteria(KurikulumPunyaMatakuliah.class)
				.add(Restrictions.eq("kurikulum.id", asliKurikulum.getId())).list();

		System.out.println("kurikulumPunyaMatakuliahs.size() = " + kurikulumPunyaMatakuliahs.size());

		for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kurikulumPunyaMatakuliahs) {
			KurikulumPunyaMatakuliah kurikulumPunyaMatakuliahCopy = new KurikulumPunyaMatakuliah();
			kurikulumPunyaMatakuliahCopy.setKurikulum(kurikulum);
			kurikulumPunyaMatakuliahCopy.setMatakuliah(kurikulumPunyaMatakuliah.getMatakuliah());
			kurikulumPunyaMatakuliahCopy.setSemester(kurikulumPunyaMatakuliah.getSemester());
			kurikulumPunyaMatakuliahCopy.setTanggalDitambahkan(ais.ui.util.WaktuUtil.getDate());
			session.save(kurikulumPunyaMatakuliahCopy);

			List<KurikulumPunyaMatakuliahDetail> kurikulumPunyaMatakuliahDetails = session
					.createCriteria(KurikulumPunyaMatakuliahDetail.class)
					.add(Restrictions.eq("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah)).list();
			for (KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail : kurikulumPunyaMatakuliahDetails) {
				KurikulumPunyaMatakuliahDetail coPunyaMatakuliahDetail = (KurikulumPunyaMatakuliahDetail) kurikulumPunyaMatakuliahDetail
						.clone();
				coPunyaMatakuliahDetail.setKurikulumPunyaMatakuliah(kurikulumPunyaMatakuliahCopy);
				coPunyaMatakuliahDetail.setId(null);
				session.save(coPunyaMatakuliahDetail);

				copyLampiran(kurikulumPunyaMatakuliahDetail, coPunyaMatakuliahDetail);
			}

			List<KurikulumPunyaMatakuliahPunyaItem> kurikulumPunyaMatakuliahPunyaItems = session
					.createCriteria(KurikulumPunyaMatakuliahPunyaItem.class)
					.add(Restrictions.eq("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah)).list();
			for (KurikulumPunyaMatakuliahPunyaItem kurikulumPunyaMatakuliahPunyaItem : kurikulumPunyaMatakuliahPunyaItems) {
				KurikulumPunyaMatakuliahPunyaItem coPunyaMatakuliahDetail = (KurikulumPunyaMatakuliahPunyaItem) kurikulumPunyaMatakuliahPunyaItem
						.clone();
				coPunyaMatakuliahDetail.setKurikulumPunyaMatakuliah(kurikulumPunyaMatakuliahCopy);
				coPunyaMatakuliahDetail.setId(null);
				session.save(coPunyaMatakuliahDetail);
			}

			List<DataPunyaArtikel> dataPunyaArtikels = session.createCriteria(DataPunyaArtikel.class)
					.add(Restrictions.eq("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah)).list();
			for (DataPunyaArtikel dataPunyaArtikel : dataPunyaArtikels) {
				DataPunyaArtikel coPunyaMatakuliahDetail = (DataPunyaArtikel) dataPunyaArtikel.clone();
				coPunyaMatakuliahDetail.setKurikulumPunyaMatakuliah(kurikulumPunyaMatakuliahCopy);
				coPunyaMatakuliahDetail.setId(null);
				session.save(coPunyaMatakuliahDetail);
			}

			copyLampiran(kurikulumPunyaMatakuliah, kurikulumPunyaMatakuliahCopy);
		}

		return true;
	}

	@SuppressWarnings("unchecked")
	public static void copyLampiran(KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah,
			KurikulumPunyaMatakuliah kurikulumPunyaMatakuliahBaru) {

		try {
			Session session = StreamingHibernateUtil.getInstance().currentSession();

			List<LampiranLain> lampiranLains = session.createCriteria(LampiranLain.class)
					.add(Restrictions.eq("ref", kurikulumPunyaMatakuliah.getId())).addOrder(Order.desc("id"))
					.add(Restrictions.eq("jenis", KurikulumPunyaMatakuliah.class.getName())).list();
			for (LampiranLain c : lampiranLains) {
				LampiranLain copy = (LampiranLain) c.clone();
				copy.setRef(kurikulumPunyaMatakuliahBaru.getId());
				session.getTransaction().begin();
				session.save(copy);
				session.getTransaction().commit();
			}

			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e1) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/KurikulumAction.java:1762");
		}

	}

	@SuppressWarnings("unchecked")
	public static void copyLampiran(KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail,
			KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetailBaru) {

		try {
			Session session = StreamingHibernateUtil.getInstance().currentSession();

			List<PertemuanFileContent> pertemuanFileContents = session.createCriteria(PertemuanFileContent.class)
					.addOrder(Order.desc("id"))
					.add(Restrictions.eq("kurikulumPunyaMatakuliahDetail", kurikulumPunyaMatakuliahDetail.getId()))
					.list();
			for (PertemuanFileContent c : pertemuanFileContents) {
				PertemuanFileContent pertemuanFileContent = new PertemuanFileContent();
				pertemuanFileContent.setCopyDari(c);
				pertemuanFileContent.setLokasiFisik(c.getLokasiFisik());
				pertemuanFileContent.setFoto(c.getFoto());
				pertemuanFileContent.setNama(c.getNama());
				pertemuanFileContent.setFileMimeType(c.getFileMimeType());
				pertemuanFileContent.setKurikulumPunyaMatakuliah(
						kurikulumPunyaMatakuliahDetailBaru.getKurikulumPunyaMatakuliah().getId());
				pertemuanFileContent.setKurikulumPunyaMatakuliahDetail(kurikulumPunyaMatakuliahDetailBaru.getId());
				pertemuanFileContent.setUploadDate(ais.ui.util.WaktuUtil.getDate());
				session.getTransaction().begin();
				session.save(pertemuanFileContent);
				session.getTransaction().commit();
			}

			List<VideoPertemuan> videoPertemuans = session.createCriteria(VideoPertemuan.class)
					.addOrder(Order.desc("id"))
					.add(Restrictions.eq("kurikulumPunyaMatakuliahDetail", kurikulumPunyaMatakuliahDetail.getId()))
					.list();
			for (VideoPertemuan c : videoPertemuans) {
				VideoPertemuan videoPertemuan = new VideoPertemuan();
				videoPertemuan.setFoto(c.getFoto());
				videoPertemuan.setNama(c.getNama());
				videoPertemuan.setJurusan(c.getJurusan());
				videoPertemuan.setKeterangan(c.getKeterangan());
				videoPertemuan.setKeteranganTambahan(c.getKeteranganTambahan());
				videoPertemuan.setTahunAkademik(c.getTahunAkademik());
				videoPertemuan.setType(c.getType());
				videoPertemuan.setUkuran(c.getUkuran());

				videoPertemuan.setKurikulumPunyaMatakuliah(
						kurikulumPunyaMatakuliahDetailBaru.getKurikulumPunyaMatakuliah().getId());
				videoPertemuan.setKurikulumPunyaMatakuliahDetail(kurikulumPunyaMatakuliahDetailBaru.getId());
				session.getTransaction().begin();
				session.save(videoPertemuan);
				session.getTransaction().commit();
			}

			List<AudioPertemuan> audioPertemuans = session.createCriteria(AudioPertemuan.class)
					.addOrder(Order.desc("id"))
					.add(Restrictions.eq("kurikulumPunyaMatakuliahDetail", kurikulumPunyaMatakuliahDetail.getId()))
					.list();
			for (AudioPertemuan c : audioPertemuans) {
				AudioPertemuan audioPertemuan = new AudioPertemuan();
				audioPertemuan.setFoto(c.getFoto());
				audioPertemuan.setNama(c.getNama());
				audioPertemuan.setJurusan(c.getJurusan());
				audioPertemuan.setKeterangan(c.getKeterangan());
				audioPertemuan.setKeteranganTambahan(c.getKeteranganTambahan());
				audioPertemuan.setTahunAkademik(c.getTahunAkademik());
				audioPertemuan.setType(c.getType());
				audioPertemuan.setUkuran(c.getUkuran());

				audioPertemuan.setKurikulumPunyaMatakuliah(
						kurikulumPunyaMatakuliahDetailBaru.getKurikulumPunyaMatakuliah().getId());
				audioPertemuan.setKurikulumPunyaMatakuliahDetail(kurikulumPunyaMatakuliahDetailBaru.getId());
				session.getTransaction().begin();
				session.save(audioPertemuan);
				session.getTransaction().commit();
			}

			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e1) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/KurikulumAction.java:1843");
		}

	}

	@SuppressWarnings("unchecked")
	public static void copyLampiran(Perkuliahan perkuliahan, KurikulumPunyaMatakuliah kurikulumPunyaMatakuliahBaru) {

		try {
			Session session = StreamingHibernateUtil.getInstance().currentSession();

			List<LampiranLain> lampiranLains = session.createCriteria(LampiranLain.class)
					.add(Restrictions.eq("ref", perkuliahan.getId())).addOrder(Order.desc("id"))
					.add(Restrictions.eq("jenis", LampiranLain.SILABUS)).list();
			for (LampiranLain c : lampiranLains) {
				LampiranLain copy = (LampiranLain) c.clone();
				copy.setJenis(KurikulumPunyaMatakuliah.class.getName());
				copy.setRef(kurikulumPunyaMatakuliahBaru.getId());
				session.getTransaction().begin();
				session.save(copy);
				session.getTransaction().commit();
			}

			lampiranLains = session.createCriteria(LampiranLain.class).add(Restrictions.eq("ref", perkuliahan.getId()))
					.addOrder(Order.desc("id")).add(Restrictions.eq("jenis", LampiranLain.SAP)).list();
			for (LampiranLain c : lampiranLains) {
				LampiranLain copy = (LampiranLain) c.clone();
				copy.setJenis(KurikulumPunyaMatakuliah.class.getName());
				copy.setRef(kurikulumPunyaMatakuliahBaru.getId());
				session.getTransaction().begin();
				session.save(copy);
				session.getTransaction().commit();
			}

			lampiranLains = session.createCriteria(LampiranLain.class).add(Restrictions.eq("ref", perkuliahan.getId()))
					.addOrder(Order.desc("id")).add(Restrictions.eq("jenis", LampiranLain.LHP)).list();
			for (LampiranLain c : lampiranLains) {
				LampiranLain copy = (LampiranLain) c.clone();
				copy.setJenis(KurikulumPunyaMatakuliah.class.getName());
				copy.setRef(kurikulumPunyaMatakuliahBaru.getId());
				session.getTransaction().begin();
				session.save(copy);
				session.getTransaction().commit();
			}

			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e1) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/KurikulumAction.java:1891");
		}

	}

	@SuppressWarnings("unchecked")
	public static void copyLampiran(Pertemuan pertemuan,
			KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetailBaru) {

		try {
			Session session = StreamingHibernateUtil.getInstance().currentSession();

			List<PertemuanFileContent> pertemuanFileContents = session.createCriteria(PertemuanFileContent.class)
					.addOrder(Order.desc("id")).add(Restrictions.eq("pertemuan", pertemuan.getId())).list();
			for (PertemuanFileContent c : pertemuanFileContents) {
				PertemuanFileContent pertemuanFileContent = (PertemuanFileContent) session
						.createCriteria(PertemuanFileContent.class)
						.add(Restrictions.eq("kurikulumPunyaMatakuliahDetail",
								kurikulumPunyaMatakuliahDetailBaru.getId()))
						.add(Restrictions.eq("nama", c.getNama())).setMaxResults(1).uniqueResult();
				if (pertemuanFileContent == null) {
					pertemuanFileContent = new PertemuanFileContent();
				}
				pertemuanFileContent.setCopyDari(c);
				pertemuanFileContent.setLokasiFisik(c.getLokasiFisik());
				pertemuanFileContent.setFoto(c.getFoto());
				pertemuanFileContent.setNama(c.getNama());
				pertemuanFileContent.setFileMimeType(c.getFileMimeType());
				pertemuanFileContent.setKurikulumPunyaMatakuliah(
						kurikulumPunyaMatakuliahDetailBaru.getKurikulumPunyaMatakuliah().getId());
				pertemuanFileContent.setKurikulumPunyaMatakuliahDetail(kurikulumPunyaMatakuliahDetailBaru.getId());
				pertemuanFileContent.setUploadDate(ais.ui.util.WaktuUtil.getDate());
				session.getTransaction().begin();
				session.saveOrUpdate(pertemuanFileContent);
				session.getTransaction().commit();
			}

			List<VideoPertemuan> videoPertemuans = session.createCriteria(VideoPertemuan.class)
					.addOrder(Order.desc("id")).add(Restrictions.eq("pertemuan", pertemuan.getId())).list();
			for (VideoPertemuan c : videoPertemuans) {
				VideoPertemuan videoPertemuan = (VideoPertemuan) session.createCriteria(VideoPertemuan.class)
						.add(Restrictions.eq("kurikulumPunyaMatakuliahDetail",
								kurikulumPunyaMatakuliahDetailBaru.getId()))
						.add(Restrictions.eq("nama", c.getNama())).setMaxResults(1).uniqueResult();
				if (videoPertemuan == null) {
					videoPertemuan = new VideoPertemuan();
				}
				videoPertemuan.setFoto(c.getFoto());
				videoPertemuan.setNama(c.getNama());
				videoPertemuan.setJurusan(c.getJurusan());
				videoPertemuan.setKeterangan(c.getKeterangan());
				videoPertemuan.setKeteranganTambahan(c.getKeteranganTambahan());
				videoPertemuan.setTahunAkademik(c.getTahunAkademik());
				videoPertemuan.setType(c.getType());
				videoPertemuan.setUkuran(c.getUkuran());

				videoPertemuan.setKurikulumPunyaMatakuliah(
						kurikulumPunyaMatakuliahDetailBaru.getKurikulumPunyaMatakuliah().getId());
				videoPertemuan.setKurikulumPunyaMatakuliahDetail(kurikulumPunyaMatakuliahDetailBaru.getId());
				session.getTransaction().begin();
				session.saveOrUpdate(videoPertemuan);
				session.getTransaction().commit();
			}

			List<AudioPertemuan> audioPertemuans = session.createCriteria(AudioPertemuan.class)
					.addOrder(Order.desc("id")).add(Restrictions.eq("pertemuan", pertemuan.getId())).list();
			for (AudioPertemuan c : audioPertemuans) {
				AudioPertemuan audioPertemuan = (AudioPertemuan) session.createCriteria(AudioPertemuan.class)
						.add(Restrictions.eq("kurikulumPunyaMatakuliahDetail",
								kurikulumPunyaMatakuliahDetailBaru.getId()))
						.add(Restrictions.eq("nama", c.getNama())).setMaxResults(1).uniqueResult();
				if (audioPertemuan == null) {
					audioPertemuan = new AudioPertemuan();
				}
				audioPertemuan.setFoto(c.getFoto());
				audioPertemuan.setNama(c.getNama());
				audioPertemuan.setJurusan(c.getJurusan());
				audioPertemuan.setKeterangan(c.getKeterangan());
				audioPertemuan.setKeteranganTambahan(c.getKeteranganTambahan());
				audioPertemuan.setTahunAkademik(c.getTahunAkademik());
				audioPertemuan.setType(c.getType());
				audioPertemuan.setUkuran(c.getUkuran());

				audioPertemuan.setKurikulumPunyaMatakuliah(
						kurikulumPunyaMatakuliahDetailBaru.getKurikulumPunyaMatakuliah().getId());
				audioPertemuan.setKurikulumPunyaMatakuliahDetail(kurikulumPunyaMatakuliahDetailBaru.getId());
				session.getTransaction().begin();
				session.saveOrUpdate(audioPertemuan);
				session.getTransaction().commit();
			}

			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e1) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/KurikulumAction.java:1985");
		}

	}

	@SuppressWarnings("unchecked")
	public static void copySilabusDariPerkuliahan(final Kurikulum kurikulum, final EventListener eventListener)
			throws Exception {

		final MyWindow window = new MyWindow("Pilih Tahun Akademik dan Semester", "none", true);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight("300px");
		window.setWidth("600px");
		final Combobox tahunAkademik = new Combobox();
		Common.generateTahunAjaranDanSemua(tahunAkademik);
		final Combobox genapGanjil = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		genapGanjil.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		genapGanjil.appendChild(comboitem);

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
		column.setWidth("20%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(genapGanjil);
		genapGanjil.setWidth("90%");
		genapGanjil.setReadonly(true);

		Common.selectComboItem(genapGanjil, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah pertemuan minimal yang diambil"));
		final MyIntbox minim;
		row.appendChild(minim = new MyIntbox(10));

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

			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Singkronkan Silabus", "/img/save.gif");
		save.setTooltiptext("Proses");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if (minim.getValue() == null) {
					MyMessageboxConfig.show("Jumlah pertemuan minimal yang diambil harus diisi", "Error",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				window.detach();

				final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Silabus dari Perkuliahan");

				final Label label = Common.displayLoadBar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						laporan.selesaikan(eventListener);
					}
				});

				new Thread(new Runnable() {

					@Override
					public void run() {
						try {

						Session session = HibernateUtil.currentNativeSession();

						try {

							List<Perkuliahan> perkuliahans = session.createCriteria(Perkuliahan.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.eq("tahunAjaran", tahunAkademik.getSelectedItem().getValue()))
									.add(Restrictions.eq("ganjilGenap", genapGanjil.getSelectedItem().getValue()))
									.add(kurikulum == null ? Restrictions.sqlRestriction("true")
											: Restrictions.eq("kurikulum", kurikulum))
									.add(Restrictions.isNotNull("kurikulumPunyaMatakuliah")).addOrder(Order.asc("id"))
									.list();

							int index = 1;
							for (Perkuliahan perkuliahan : perkuliahans) {

								int nomorBaris = index - 1;
								String kunciPerkuliahan = perkuliahan.infoSimple();
								try {

								List<Pertemuan> pertemuans = session.createCriteria(Pertemuan.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(Restrictions.eq("perkuliahan", perkuliahan))
										.addOrder(Order.asc("pertemuanKe")).list();

								label.setValue(perkuliahan.infoSimple() + ", qty : " + pertemuans.size() + " ("
										+ Common.numberFormat.get().format(index * 100.0 / perkuliahans.size()) + "%)");
								index++;

								if (pertemuans.size() >= minim.getValue()) {
									for (Pertemuan pertemuan : pertemuans) {
										if (pertemuan.getAktif()) {
											KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail = (KurikulumPunyaMatakuliahDetail) session
													.createCriteria(KurikulumPunyaMatakuliahDetail.class)
													.add(Restrictions.eq("kurikulumPunyaMatakuliah",
															perkuliahan.getKurikulumPunyaMatakuliah()))
													.add(Restrictions.eq("nomorUrut", pertemuan.getPertemuanKe()))
													.setMaxResults(1).uniqueResult();
											if (kurikulumPunyaMatakuliahDetail == null) {
												kurikulumPunyaMatakuliahDetail = new KurikulumPunyaMatakuliahDetail();
											}
											kurikulumPunyaMatakuliahDetail.setNomorUrut(pertemuan.getPertemuanKe());
											kurikulumPunyaMatakuliahDetail.setTopik(pertemuan.getTopik());
											kurikulumPunyaMatakuliahDetail.setIndikator(pertemuan.getIndikator());
											kurikulumPunyaMatakuliahDetail
													.setWaktupembelajaran(pertemuan.getWaktupembelajaran());
											kurikulumPunyaMatakuliahDetail
													.setPengalamanBelajar(pertemuan.getPengalamanBelajar());
											kurikulumPunyaMatakuliahDetail
													.setTugasDanPenilaian(pertemuan.getTugasDanPenilaian());
											kurikulumPunyaMatakuliahDetail.setBukuRujukan1(pertemuan.getBukuRujukan1());
											kurikulumPunyaMatakuliahDetail
													.setStatusPertemuan(pertemuan.getStatusPertemuan());
											kurikulumPunyaMatakuliahDetail
													.setMetodePembelajaran(pertemuan.getMetodePembelajaran());
											kurikulumPunyaMatakuliahDetail.setKurikulumPunyaMatakuliah(
													perkuliahan.getKurikulumPunyaMatakuliah());

											session.getTransaction().begin();
											Common.refreshSaveOrUpdate(session, kurikulumPunyaMatakuliahDetail);
											session.getTransaction().commit();

											copyLampiran(pertemuan, kurikulumPunyaMatakuliahDetail);
										}
									}

									copyLampiran(perkuliahan, perkuliahan.getKurikulumPunyaMatakuliah());
								}

								pertemuans = null;
								laporan.catatBerhasil(nomorBaris, kunciPerkuliahan, "Sinkronisasi silabus berhasil");
								} catch (Exception ePerItem) {
									Common.tampilErrorJikaAdmin(ePerItem);
									laporan.catatGagalDetail(nomorBaris, kunciPerkuliahan, ePerItem);
								}
							}
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
							laporan.tambahCatatan("Proses sinkronisasi silabus terhenti total (di luar per-perkuliahan): "
									+ ais.common.LaporanUpload.detailTeknisException(e));
						}

						label.setValue("");
						HibernateUtil.closeSession();
											} finally {
							ais.database.hibernate.HibernateUtil.closeSession();
						}
					}
				}).start();

			}
		});
		save.setParent(toolbar);

		window.onModal();

	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Kurikulum.class);

		if (searchmk != null && !searchmk.getValue().trim().isEmpty()) {
			criteria = session.createCriteria(KurikulumPunyaMatakuliah.class).createAlias("matakuliah", "matakuliah")
					.add(Restrictions.or(
							Restrictions.ilike("matakuliah.kode", searchmk.getValue().trim(), MatchMode.ANYWHERE),
							Restrictions.ilike("matakuliah.nama", searchmk.getValue().trim(), MatchMode.ANYWHERE)))
					.setProjection(Projections.groupProperty("kurikulum")).createCriteria("kurikulum");
		}

		criteria.add(selectedJurusan != null ? Restrictions.eq("jurusan.id", selectedJurusan)
				: Restrictions.sqlRestriction("1=1"))
				.add(selectedTahun != null ? Restrictions.eq("tahun", selectedTahun)
						: Restrictions.sqlRestriction("1=1"))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));
		if (searchmk.getValue().trim().isEmpty() && order)
			criteria.addOrder(Order.desc("tahun"));

		criteria.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))

				.add(searchtahun.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahun", searchtahun.getValue()))

				.add(searchnama == null || searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.createAlias("jurusan", "jurusan")

				.add(searchfakultas == null || searchfakultas.getSelectedItem() == null
						|| searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

				.add(searchjenjang == null || searchjenjang.getSelectedItem() == null
						|| searchjenjang.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan.jenjang", searchjenjang, false))

				.createAlias("jurusan.fakultas", "fakultas")

				.add(perguruanTinggi == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("fakultas.perguruanTinggi", perguruanTinggi));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Kurikulum> kurikulum = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(kurikulum);
		grid.setRowRenderer(new KurikulumRenderer());
		grid.setModelCheckMobile(strset);

	}

}
