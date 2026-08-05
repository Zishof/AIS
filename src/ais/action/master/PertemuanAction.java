package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.util.PDFMergerUtility;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
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
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.dashboard.admin.DashboardRekapPertemuanPerkuliahan;
import ais.action.master.dashboard.admin.RekapHasilTugas;
import ais.action.master.dashboard.admin.RekapHasilUjian;
import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.feeder.util.FeederExporter;
import ais.action.master.helper.AktifitasPerkuliahanHelper;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataKurikulumBanbox;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.AmbilDataMasaPerkuliahanBanbox;
import ais.action.master.helper.AmbilDataMatakuliahBanbox;
import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.master.helper.DspaceHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.report.CommonReportHelper;
import ais.action.report.Report;
import ais.action.report.format1.akademik.LaporanMonitorPerkuliahanKbm;
import ais.action.report.format1.akademik.LaporanPendidikanLingkunganKampus;
import ais.action.report.helper.CommonReport;
import ais.common.AsyncTaskManager;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.Html2Text;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.DspaceInformation;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaDapatKelompokKkn;
import ais.database.model.MahasiswaDapatKelompokPkl;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.ui.dspace.DspaceCommon;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIframe;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelBoldConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class PertemuanAction extends GenericAutowireComposer implements DataLoader, DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	protected static final long serialVersionUID = -5779730267402400328L;
	protected MyGrid grid;
	protected Paging paging;

	protected AmbilDataMatakuliahBanbox searchmatakuliah;
	protected AmbilDataDosenBanbox searchdosen;

	protected Textbox searchkelas;
	protected MyCheckboxConfig searchparalel;
	protected MyCheckboxConfig searchtanpakelas;
	protected AmbilDataRuangBanbox searchruang;

	protected Combobox searchhari;
	protected Combobox searchTahunAjaran;
	protected Combobox searchsemester;
	protected Combobox searchJenisSemester;
	protected AmbilDataMasaPerkuliahanBanbox searchmasaperkulaiahan;
	protected Combobox searchprogram;
	protected Combobox searchObe;
	protected Combobox searchfakultas;
	protected Combobox searchjurusan;
	protected AmbilDataKurikulumBanbox searchkurikulum;
	protected AmbilDataMahasiswaBanbox searchasisten;
	protected Textbox searchnamamhs;
	protected Textbox searchnim;

	protected Textbox searchnamadsn;
	protected Textbox searchnamamk;
	protected Textbox searchKeterangan;
	protected Textbox searchnamaasisten;

	protected Mahasiswa mahasiswa;
	protected Dosen dosen;

	// protected North mynorth;
	protected Tbmuser tbmuser;

	protected Integer semesterPendek;
	protected Boolean merupakanPraPerkuliahan = false;
	protected Boolean merupakanRemedial = false;

	protected AktifitasPerkuliahanHelper aktifitasPerkuliahanHelper;

	protected Tabpanel panelIkut;
	protected MyTabConfig tab1;
	protected MyTabConfig tab2;

	protected MyToolbarbuttonConfig find;
	private List<Perkuliahan> perkuliahans = null;

	public void onTampilPanelIkut(Event event) {
		if (panelIkut.getChildren().size() == 0) {
			MyIframe include = new MyIframe("/pages/master/ikut_pertemuan.zul");
			panelIkut.appendChild(include);
		}
	}

	protected Tabpanel rekapitulasiPertemuanPerkuliahan;

	public void onRekapPertemuanPerkuliahan(Event event) {

		if (rekapitulasiPertemuanPerkuliahan.getChildren().size() == 0) {
			DashboardRekapPertemuanPerkuliahan laporan = new DashboardRekapPertemuanPerkuliahan();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, rekapitulasiPertemuanPerkuliahan,
				"Rekap Pertemuan", "Ringkasan jumlah pertemuan yang sudah terlaksana vs target per mata kuliah.");
		}
	}

	protected Tabpanel kegiatanMahasiswa;

	protected Tab tabkegiatanMahasiswa;

	public void onKegiatanMahasiswa(Event event) {

		if (kegiatanMahasiswa.getChildren().size() == 0) {
			LaporanPendidikanLingkunganKampus laporan = new LaporanPendidikanLingkunganKampus();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.kegiatanMahasiswa);
		}
	}

	protected Tabpanel perkuliahanSp;
	// private List<Statusabsensi> statusabsensis;

	public void onPerkuliahanSp(Event event) {

		if (perkuliahanSp.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(perkuliahanSp);
			include.setSrc("/pages/master/pertemuan_sp.zul");
		}
	}

	protected Tabpanel ekstrakurikulerTab;
	protected Integer ekstrakurikuler = null;

	public void onEkstrakurikuler(Event event) {

		if (ekstrakurikulerTab.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(ekstrakurikulerTab);
			include.setSrc("/pages/master/pertemuan_ekstrakulikuler.zul");
		}
	}

	protected Tabpanel parameterPertemuanPerkuliahan;

	public void onParameterPertemuanPerkuliahan(Event event) {
		if (parameterPertemuanPerkuliahan.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(parameterPertemuanPerkuliahan);
			include.setSrc("/pages/master/parameter_tambahan_pertemuan.zul");
		}
	}

	private Tabpanel praPerkuliahanTab;

	public void onPraPerkuliahan(Event event) {

		if (praPerkuliahanTab.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(praPerkuliahanTab);
			include.setSrc("/pages/master/pertemuan_pra_perkuliahan.zul");
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	@SuppressWarnings("unchecked")
	public void onDownloadLampiran(Event event) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Long> calonMahasiswa = initCriteria(true).setProjection(Projections.property("id")).list();
				File fileFolderLampiran = new File(
						"/opt/ecampus/lampiran_" + ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis());
				fileFolderLampiran.mkdirs();
				System.out.println("fileFolderLampiran => " + fileFolderLampiran.getAbsolutePath());

				class FileDownloadHelper {
					public File download(String jenis, Long ref, File fileFolderCalon) {
						File fileCopy = null;

						Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
						try {

							int jumlah = ((Number) streamingSession.createCriteria(LampiranLain.class)
									.setProjection(Projections.rowCount()).add(Restrictions.eq("jenis", jenis))
									.add(Restrictions.eq("ref", ref)).setMaxResults(1).uniqueResult()).intValue();
							if (jumlah > 0) {
								LampiranLain lampiranLainMahasiswa = (LampiranLain) streamingSession
										.createCriteria(LampiranLain.class).add(Restrictions.eq("ref", ref))
										.add(Restrictions.eq("jenis", jenis)).setMaxResults(1).uniqueResult();
								if (lampiranLainMahasiswa.getGdrive() != null) {
									fileCopy = new File(fileFolderCalon.getAbsolutePath() + "/" + jenis + "_"
											+ mahasiswa.getNim() + ".txt");
									FileUtils.writeStringToFile(fileCopy, lampiranLainMahasiswa.forwardGDriveUrl());
								} else if (lampiranLainMahasiswa.getLink() != null
										&& !lampiranLainMahasiswa.getLink().isEmpty()) {
									fileCopy = new File(fileFolderCalon.getAbsolutePath() + "/" + jenis + "_"
											+ mahasiswa.getNim() + ".txt");
									FileUtils.writeStringToFile(fileCopy, lampiranLainMahasiswa.getLink());
								} else {
									File file = lampiranLainMahasiswa.ambilFile();
									fileCopy = new File(
											fileFolderCalon.getAbsolutePath() + "/" + jenis + "_" + file.getName());
									System.out.println("fileCopy => " + fileCopy.getAbsolutePath());
									FileOutputStream fileOutputStream = new FileOutputStream(fileCopy);
									FileInputStream fileInputStream = new FileInputStream(file);
									IOUtils.copyLarge(fileInputStream, fileOutputStream);
									fileInputStream.close();
									fileOutputStream.close();
								}
							}

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}

						StreamingHibernateUtil.getInstance().closeSession();

						return fileCopy;
					}
				}

				FileDownloadHelper downloadHelper = new FileDownloadHelper();

				for (Long ref : calonMahasiswa) {
					Perkuliahan perkuliahan = (Perkuliahan) ConstantValues.ambil(Perkuliahan.class.getName(), ref,
							true);
					if (perkuliahan != null) {
						File fileFolderCalon = new File(fileFolderLampiran.getAbsolutePath() + "/"
								+ URLEncoder.encode(perkuliahan.infoSimple(), "UTF-8"));
						fileFolderCalon.mkdirs();
						System.out.println("fileFolderCalon => " + fileFolderCalon.getAbsolutePath());

						downloadHelper.download(LampiranLain.SILABUS, ref, fileFolderCalon);
						downloadHelper.download(LampiranLain.SAP, ref, fileFolderCalon);
						downloadHelper.download("Absen Manual", ref, fileFolderCalon);

					}
				}

				File fileFolderLampiranZip = new File(fileFolderLampiran.getAbsolutePath() + ".zip");
				Common.zipDir(fileFolderLampiranZip.getAbsolutePath(), fileFolderLampiran.getAbsolutePath());
				Filedownload.save(fileFolderLampiranZip, "application/zip");

			}
		}, "Harap tunggu.. sedang melakukan proses download lampiran..");

	}

	private Tabpanel jadwalRemedial;

	public void onJadwalRemedial(Event event) {

		if (jadwalRemedial.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(jadwalRemedial);
			include.setSrc("/pages/master/pertemuan_remedial.zul");
		}
	}

	private North mynorth;
	private Combobox ta;
	private Combobox smt;
	private Combobox hari;
	private Textbox keyword;
	private List<Perkuliahan> semuaperkuliahans = new ArrayList<Perkuliahan>();
	private PerguruanTinggi perguruanTinggi;

	public static void tampilanExportKeFeeder(Component parent, Tbmuser tbmuser, final DataSearchDefault dataSearch,
			final DataCriteria criteria) {
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

												dataSearch.onSearchDefault(null);
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
															token, null, null, myLabelProsesDetail);

													List<Perkuliahan> tbmusers = ConstantValues.simpleList(
															(Criteria) criteria.initCriteria(true), Perkuliahan.class);
													int size = tbmusers.size();
													int index = 1;
													for (Perkuliahan perkuliahan : tbmusers) {
														myLabelProsesDetail.setValue("Memproses " + perkuliahan.info()
																+ " ("
																+ Common.numberFormat.get().format((index * 100.0) / size)
																+ "%");
														index++;
														kirimKeFeeder(feederImporter, perkuliahan, feederConnector,
																token, errorLog);
													}
													tbmusers.clear();
													tbmusers = null;

													myLabelProsesDetail.setValue("");
												} catch (Exception e) {
													// FIX "gagal diam-diam": sebelumnya exception di sini hanya
													// dicatat ke log admin lalu progres diset "" (=SUKSES palsu)
													// di luar try, menutupi kegagalan dari pengguna.
													ais.common.Common.tampilErrorJikaAdmin(e);
													myLabelProsesDetail.setValue(
															"Error: " + ais.common.PesanFormalHelper.pesanGagalException(
																	"pengiriman data Rencana Pembelajaran (Pertemuan) ke Neo Feeder",
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
			parent.appendChild(buttonTagihan);

		}
	}

	public static void kirimKeFeeder(FeederExporter feederImporter, Perkuliahan perkuliahan,
			FeederConnector feederConnector, String token, List<String> errorLog) {

		try {

			String id_matkul = perkuliahan.getMatakuliah().getFeeder();

			List<Pertemuan> pertemuans = perkuliahan.ambilPertemuanList();
			for (Pertemuan pertemuan : pertemuans) {
				if (pertemuan.getAktif()) {
					String filter = "id_matkul='" + id_matkul + "' AND pertemuan=" + pertemuan.getPertemuanKe();

					JSONArray dataGetListRencanaPembelajaran = feederConnector.getData("GetListRencanaPembelajaran",
							token, filter, "", "1", "");
					System.out.println("results dataGetListRencanaPembelajaran -> " + dataGetListRencanaPembelajaran);

					if (dataGetListRencanaPembelajaran.length() == 0) {
						JSONObject record = new JSONObject();
						record.put("id_matkul", id_matkul);
						record.put("pertemuan", pertemuan.getPertemuanKe());
						record.put("materi_indonesia", pertemuan.getBukuRujukan1().isEmpty() ? "Belum ada bahan kajian"
								: pertemuan.getBukuRujukan1());
						record.put("materi_inggris", pertemuan.getBukuRujukan1().isEmpty() ? "Belum ada bahan kajian"
								: pertemuan.getBukuRujukan1());

						List<String> errorLogLocal = new ArrayList<String>();
						feederConnector.insertOrUpdateRecordBaru(token, null, "InsertRencanaPembelajaran", record,
								errorLogLocal, pertemuan);

					}

					else {

						JSONObject jsonObject = dataGetListRencanaPembelajaran.getJSONObject(0);

						JSONObject idMhs = new JSONObject();
						idMhs.put("id_rencana_ajar", jsonObject.get("id_rencana_ajar"));

						JSONObject record = new JSONObject();
						record.put("id_matkul", id_matkul);
						record.put("pertemuan", pertemuan.getPertemuanKe());
						record.put("materi_indonesia", pertemuan.getBukuRujukan1().isEmpty() ? "Belum ada bahan kajian"
								: pertemuan.getBukuRujukan1());

						record.put("materi_inggris", pertemuan.getBukuRujukan1().isEmpty() ? "Belum ada bahan kajian"
								: pertemuan.getBukuRujukan1());

						feederConnector.insertOrUpdateRecordBaru(token, idMhs, "UpdateRencanaPembelajaran", record,
								errorLog, pertemuan);
					}
				}
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

	}

	@SuppressWarnings("unchecked")
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		tbmuser = Common.getCurrentUser();
		mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		dosen = tbmuser == null ? null : tbmuser.ambilDosen();
		if (mahasiswa == null && tab1 != null && tab2 != null) {
			tab1.setHeight("0px");
			tab2.setHeight("0px");

		}
		perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		if (tbmuser == null || mahasiswa != null || dosen != null) {
			parameterPertemuanPerkuliahan.setVisible(false);
			parameterPertemuanPerkuliahan.getLinkedTab().setVisible(false);
		}

		MyToolbarbuttonConfig kbb = new MyToolbarbuttonConfig("KBM", "/img/group.gif");
		kbb.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				PertemuanAction.dataKBM.onEvent(new Event("", null, perkuliahans));
			}
		});

		MyToolbarbuttonConfig rekap = new MyToolbarbuttonConfig("Rekap Pemb.", "/img/group.gif");
		rekap.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				PertemuanAction.datarekapPembelajaran.onEvent(new Event("", null, perkuliahans));
			}
		});
		boolean edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		aktifitasPerkuliahanHelper = new AktifitasPerkuliahanHelper(mahasiswa, null, edit);

		if (mynorth != null && tbmuser != null && (tbmuser.getMahasiswa() != null || tbmuser.ambilDosen() != null)) {

			Common.clear(mynorth);

			ta = new Combobox();
			smt = new Combobox();
			hari = new Combobox();

			Comboitem comboitem;
			for (String h : Common.haris) {
				comboitem = new MyComboitemConfig();
				comboitem.setLabel(h);
				comboitem.setValue(h);
				hari.appendChild(comboitem);
			}
			comboitem = new Comboitem();
			comboitem.setLabel("=hari=");
			comboitem.setValue(null);
			hari.appendChild(comboitem);
			hari.setReadonly(true);
			hari.setSelectedItem(comboitem);

			comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(Perkuliahan.GANJIL);
			comboitem.setValue(Perkuliahan.GANJIL);
			smt.appendChild(comboitem);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel(Perkuliahan.GENAP);
			comboitem.setValue(Perkuliahan.GENAP);
			smt.appendChild(comboitem);

			MyComboitemConfig comboitemSp = new MyComboitemConfig();
			comboitemSp.setLabel(Perkuliahan.SP);
			comboitemSp.setValue(Perkuliahan.SP);
			smt.appendChild(comboitemSp);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel("=smt=");
			comboitem.setValue(null);
			smt.appendChild(comboitem);
			smt.setReadonly(true);

			if (merupakanPraPerkuliahan) {
				smt.setVisible(false);
			}

			if (semesterPendek != null) {
				smt.setSelectedItem(comboitemSp);
				smt.setDisabled(true);
			} else {
				smt.setSelectedItem(comboitem);
			}
			Common.generateTahunAjaranDanSemua(ta);
			Common.selectComboItem(ta, Common.getCurrentTahunAkademik());

			keyword = new Textbox();
			keyword.setCols(Common.isMobile() ? 5 : 10);

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(new Event("cari"));
				}
			};

			ta.addEventListener("onChange", eventListener);
			smt.addEventListener("onChange", eventListener);
			keyword.addEventListener("onOK", eventListener);
			hari.addEventListener("onChange", eventListener);

			int jumlahDataDalamSatuHalamanElearning = 10;
			Common.initPagingCustom(paging, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(null);
				}
			}, jumlahDataDalamSatuHalamanElearning);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");

			Toolbar toolbar = new Toolbar();
			if (!Common.isMobile())
				toolbar.appendChild(new MyLabelBoldConfig("TA :"));
			else
				ta.setCols(3);
			toolbar.appendChild(ta);

			if (!Common.isMobile() && !merupakanPraPerkuliahan) {
				toolbar.appendChild(new Space());
				toolbar.appendChild(new MyLabelBoldConfig("Smt :"));
			} else
				smt.setCols(2);
			toolbar.appendChild(smt);
			if (!Common.isMobile()) {
				toolbar.appendChild(new Space());
				toolbar.appendChild(new MyLabelBoldConfig("Hari :"));
			} else
				hari.setCols(3);
			toolbar.appendChild(hari);

			if (!Common.isMobile()) {
				toolbar.appendChild(new Space());
				toolbar.appendChild(new MyLabelBoldConfig("Dosen/Mk :"));
			}
			toolbar.appendChild(keyword);
			toolbar.appendChild(button);
			toolbar.appendChild(new Space());
			toolbar.appendChild(kbb);
			toolbar.appendChild(rekap);
			toolbar.setParent(mynorth);

			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(new Event("cari"));
				}
			});
			onSearchDefault(null);

			MyToolbarbuttonConfig buttonFormatNilai = new MyToolbarbuttonConfig("Rekap Hasil Ujian",
					"/img/svg/edit-box-line.svg");
			buttonFormatNilai.setParent(toolbar);
			buttonFormatNilai
					.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
			buttonFormatNilai.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {

					List<Pertemuan> pertemuans = HibernateUtil.currentSession().createCriteria(Pertemuan.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(semuaperkuliahans.isEmpty() ? Restrictions.sqlRestriction("false")
									: Restrictions.in("perkuliahan", semuaperkuliahans))
							.list();

					RekapHasilUjian addWindow = new RekapHasilUjian(pertemuans.toArray(new Pertemuan[] {}));
					addWindow.setClosable(true);
					addWindow.setTitle("Rekap Hasil Ujian");
					addWindow.setHeight("95%");
					addWindow.setWidth("90%");
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
					addWindow.onModal();
				}

			});

			buttonFormatNilai = new MyToolbarbuttonConfig("Rekap Hasil Tugas", "/img/svg/edit-box-line.svg");
			buttonFormatNilai
					.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
			buttonFormatNilai.setParent(toolbar);
			buttonFormatNilai.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {

					List<Pertemuan> pertemuans = HibernateUtil.currentSession().createCriteria(Pertemuan.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(semuaperkuliahans.isEmpty() ? Restrictions.sqlRestriction("false")
									: Restrictions.in("perkuliahan", semuaperkuliahans))
							.list();

					RekapHasilTugas addWindow = new RekapHasilTugas(false, pertemuans.toArray(new Pertemuan[] {}));
					addWindow.setClosable(true);
					addWindow.setTitle("Rekap Hasil Tugas");
					addWindow.setHeight("95%");
					addWindow.setWidth("90%");
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
					addWindow.onModal();
				}

			});

		} else {

			if (searchtanpakelas != null) {
				searchtanpakelas.setVisible(
						Common.bolehKonfigurasi("tampilkan_search_kelas_di_penjadwalan", Konfigurasi.TIDAK_AKTIF));
			}

			searchmatakuliah.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);

				}
			});
			searchdosen.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);

				}
			});

			if (kegiatanMahasiswa != null) {
				kegiatanMahasiswa.setVisible(mahasiswa != null);
				tabkegiatanMahasiswa.setVisible(mahasiswa != null);
			}

			if (dosen != null) {
				searchdosen.setValue(dosen.getNama());
				searchdosen.setAttribute("myValue", dosen);
				searchdosen.setDisabled(true);
			}

			searchJenisSemester.setReadonly(true);

			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(Perkuliahan.GANJIL);
			comboitem.setValue(Perkuliahan.GANJIL);
			searchJenisSemester.appendChild(comboitem);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel(Perkuliahan.GENAP);
			comboitem.setValue(Perkuliahan.GENAP);
			searchJenisSemester.appendChild(comboitem);

			comboitem = new MyComboitemConfig();
			if (comboitem != null) { comboitem.setLabel("Semester Pendek (SP)"); }
			if (comboitem != null) { comboitem.setValue(Perkuliahan.SP); }
			searchJenisSemester.appendChild(comboitem);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Semua");
			comboitem.setValue(null);
			searchJenisSemester.appendChild(comboitem);

			// Default Jenis Semester = SEMESTER SAAT INI (abaikan konfigurasi
			// 'pilihan_semester_di_perkuliahan_dibuat_default_semua_aja'; selalu pakai isNowSemensterGanjil()).
			Common.selectComboItem(searchJenisSemester, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

			Common.initPrograms(searchprogram);

			for (String h : Common.haris) {
				comboitem = new MyComboitemConfig();
				comboitem.setLabel(h);
				comboitem.setValue(h);
				searchhari.appendChild(comboitem);
			}

			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Semua");
			comboitem.setValue(null);
			searchhari.appendChild(comboitem);
			searchhari.setSelectedItem(comboitem);

			if (searchasisten != null) {

				if (mahasiswa != null) {
					searchasisten.setValue("");
					searchasisten.setAttribute("mahasiswa", null);
					searchasisten.setVisible(false);
				}

				searchasisten.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onSearchDefault(arg0);
					}
				});
			}

			searchmatakuliah.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);

				}
			});
			searchdosen.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);

				}
			});

			searchruang.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);

				}
			});

			searchkurikulum.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);
				}
			});

			searchmasaperkulaiahan.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);
				}
			});

			final EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(searchsemester);
					searchsemester.setSelectedItem(null);

					if (searchJenisSemester.getSelectedItem() == null) {
						return;
					}
					if (searchJenisSemester.getSelectedItem().getValue() == null
							|| Perkuliahan.SP.equals(searchJenisSemester.getSelectedItem().getValue())) {
						org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
						comboitem.setLabel("Semua");
						comboitem.setValue(null);
						searchsemester.appendChild(comboitem);
						for (int i = 1; i < 30; i++) {
							comboitem = new MyComboitemConfig();
							comboitem.setLabel(i + "");
							comboitem.setValue(i);
							searchsemester.appendChild(comboitem);
						}
					} else {
						Boolean genap = searchJenisSemester.getSelectedItem().getValue().equals(Perkuliahan.GENAP);
						org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
						comboitem.setLabel("Semua");
						comboitem.setValue(null);
						searchsemester.appendChild(comboitem);
						if (genap) {
							for (int i : Common.genap) {
								if (i == 0)
									continue;
								comboitem = new MyComboitemConfig();
								comboitem.setLabel(i + "");
								comboitem.setValue(i);
								searchsemester.appendChild(comboitem);
							}
						} else {
							for (int i : Common.ganjil) {
								comboitem = new MyComboitemConfig();
								comboitem.setLabel(i + "");
								comboitem.setValue(i);
								searchsemester.appendChild(comboitem);
							}
						}
					}

					searchsemester.setSelectedIndex(0);
					searchsemester.setReadonly(true);
				}
			};

			Common.generateTahunAjaranDanSemua(searchTahunAjaran);
			Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());

			Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

			searchJenisSemester.addEventListener("onChange", eventListener);
			eventListener.onEvent(null);
			if ((tbmuser != null && tbmuser.getMahasiswa() != null)
					|| searchasisten != null && searchasisten.getAttribute("mahasiswa") != null) {
				searchfakultas.setDisabled(false);
				searchjurusan.setDisabled(false);

				searchfakultas.setSelectedIndex(-1);
				searchjurusan.setSelectedIndex(-1);
			}

			// Default filter "Tampilan Kurikulum" = Tampilkan Semua
			if (searchObe != null && searchObe.getItemCount() > 0) {
				searchObe.setSelectedIndex(0);
			}

			onSearchDefault(null);

			Common.initPaging(paging, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(null);

				}
			});

			MyToolbarbuttonConfig exportKeOjs = new MyToolbarbuttonConfig("Ekspor", "/img/corner.gif");
			Common.appendKeToolbar(exportKeOjs, find, comp);
			exportKeOjs.setVisible(mahasiswa == null
					&& Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF)
					&& Common.bolehKonfigurasi("pertemuan_elearning_terhubung_ke_dspace"));
			exportKeOjs.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					List<Perkuliahan> perkuliahans = initCriteria(true).list();
					String cookie = DspaceCommon.login();
					DspaceHelper.exportDisplayPilihan(cookie, perkuliahans, null, null, null, null, null,
							new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									onSearchDefault(arg0);
									LogLoginAction.tampilDpsaceLog();
								}
							});
				}
			});

			MyToolbarbuttonConfig batalExport = new MyToolbarbuttonConfig("Batalkan Ekspor", "/img/svg/trash.svg");
			Common.appendKeToolbar(batalExport, find, comp);
			batalExport.setVisible(mahasiswa == null
					&& Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF)
					&& Common.bolehKonfigurasi("pertemuan_elearning_terhubung_ke_dspace"));
			batalExport.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin membatalkan ekspor data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										final Label label = Common.displayLoadBar(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												onSearchDefault(arg0);
												LogLoginAction.tampilDpsaceLog();
											}
										});

										new Thread(new Runnable() {

											@Override
											public void run() {
												try {
												try {
													String cookie = DspaceCommon.login();
													List<Perkuliahan> perkuliahans = initCriteria(true).list();

													int rowIndex = 1;
													for (Perkuliahan perkuliahan : perkuliahans) {
														label.setValue("Sedang memproses data " + perkuliahan.toString()
																+ " ("
																+ Common.numberFormat.get().format(
																		(rowIndex++) * 100.0 / perkuliahans.size())
																+ " %)");
														DspaceInformation dspaceInformation = DspaceInformation
																.getDspaceInformation(Perkuliahan.class.getName(),
																		perkuliahan.getId());
														if (dspaceInformation != null) {
															int i = DspaceInformation.delete(cookie,
																	"items/" + dspaceInformation.getUuid(),
																	dspaceInformation.getPostInfo());
															if (i == 200) {

																Session session = HibernateUtil.currentNativeSession();
																session.getTransaction().begin();
																session.delete(dspaceInformation);
																session.getTransaction().commit();
																HibernateUtil.closeSession();
															}
														}
													}
												} catch (Exception e) {
													// TODO Auto-generated catch
													// block
													Common.tampilErrorJikaAdmin(e);
												}
												label.setValue("");
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

			Common.appendKeToolbar(kbb, find, comp);
			Common.appendKeToolbar(rekap, find, comp);

			MyToolbarbuttonConfig buttonFormatNilai = new MyToolbarbuttonConfig("Rekap Hasil Ujian",
					"/img/svg/edit-box-line.svg");
			buttonFormatNilai.setParent(find.getParent());
			buttonFormatNilai.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {

					List<Long> perkuliahan = initCriteria(false).setProjection(Projections.property("id")).list();

					List<Pertemuan> pertemuans = HibernateUtil.currentSession().createCriteria(Pertemuan.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(perkuliahan.isEmpty() ? Restrictions.sqlRestriction("false")
									: Restrictions.in("perkuliahan.id", perkuliahan))
							.list();

					RekapHasilUjian addWindow = new RekapHasilUjian(pertemuans.toArray(new Pertemuan[] {}));
					addWindow.setClosable(true);
					addWindow.setTitle("Rekap Hasil Ujian");
					addWindow.setHeight("95%");
					addWindow.setWidth("90%");
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
					addWindow.onModal();
				}

			});

			buttonFormatNilai = new MyToolbarbuttonConfig("Rekap Hasil Tugas", "/img/svg/edit-box-line.svg");
			buttonFormatNilai.setParent(find.getParent());
			buttonFormatNilai.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {

					List<Long> perkuliahan = initCriteria(false).setProjection(Projections.property("id")).list();

					List<Pertemuan> pertemuans = HibernateUtil.currentSession().createCriteria(Pertemuan.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(perkuliahan.isEmpty() ? Restrictions.sqlRestriction("false")
									: Restrictions.in("perkuliahan.id", perkuliahan))
							.list();

					RekapHasilTugas addWindow = new RekapHasilTugas(false, pertemuans.toArray(new Pertemuan[] {}));
					addWindow.setClosable(true);
					addWindow.setTitle("Rekap Hasil Tugas");
					addWindow.setHeight("95%");
					addWindow.setWidth("90%");
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
					addWindow.onModal();
				}

			});
		}

		MyToolbarbuttonConfig downloadLampiran = new MyToolbarbuttonConfig("Lampiran", "/img/attachment-icon.png");
		if (downloadLampiran != null) { downloadLampiran.setVisible(edit); }
		downloadLampiran.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onDownloadLampiran(arg0);
			}
		});
		Common.appendKeToolbar(downloadLampiran, find, comp);

		downloadLampiran = new MyToolbarbuttonConfig("Laporan KBM", "/img/svg/printer.svg");
		if (downloadLampiran != null) { downloadLampiran.setVisible(edit); }
		downloadLampiran.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Long> perkuliahans = initCriteria(true).setProjection(Projections.property("id")).list();
				LaporanMonitorPerkuliahanKbm laporanMonitorPerkuliahan = new LaporanMonitorPerkuliahanKbm(perkuliahans);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(laporanMonitorPerkuliahan);
				laporanMonitorPerkuliahan.setBorder("none");
				laporanMonitorPerkuliahan.setHeight("650px");
				laporanMonitorPerkuliahan.setTitle("Laporan KBM");
				laporanMonitorPerkuliahan.setHeight("95%");
				laporanMonitorPerkuliahan.setWidth("90%");
				laporanMonitorPerkuliahan.onModal();
			}
		});
		Common.appendKeToolbar(downloadLampiran, find, comp);

		downloadLampiran = new MyToolbarbuttonConfig("Laporan Absensi", "/img/svg/printer.svg");
		if (downloadLampiran != null) { downloadLampiran.setVisible(edit); }
		downloadLampiran.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyWindow laporanMonitorPerkuliahan = new MyWindow();
				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(laporanMonitorPerkuliahan);

				final Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);

				Common.createDefaultTimer(new EventListener() {

					@SuppressWarnings("rawtypes")
					@Override
					public void onEvent(Event arg0) throws Exception {
						PDFMergerUtility ut = new PDFMergerUtility();
						// Session dedicated agar koneksi OSIV basi tidak sebabkan JDBCConnectionException
						Session session = HibernateUtil.getSessionFactory().openSession();
						File fileD = null;
						try {
						List<Long> perkuliahans = initCriteria(true).setProjection(Projections.property("id")).list();

						Toolbar toolbar = new Toolbar();

						for (Long id : perkuliahans) {
							Perkuliahan perkuliahan = (Perkuliahan) session.createCriteria(Perkuliahan.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.idEq(id)).uniqueResult();
							Map parameters = CommonReportHelper.parameterKehadiran(perkuliahan, true, 1);
							fileD = Report.generateFileReport(Report.PDF, parameters, "LaporanAbsensi",
									ais.ui.util.WaktuUtil.getDate(), toolbar);
							ut.addSource(fileD);
						}

						if (fileD != null) {
							File filePdfBaru = new File(fileD.getParentFile().getAbsolutePath() + "/"
									+ Common.getGeneratedBarCode() + ".pdf");
							ut.setDestinationStream(new FileOutputStream(filePdfBaru));
							ut.mergeDocuments();

							CommonReport.tampilkanReportPDF(center, filePdfBaru);
						}
						} finally {
							try { session.clear(); session.close(); } catch (Exception eignore) { ais.common.ErrorAuditUtil.record(eignore, "auto-audit(empty-catch) src/ais/action/master/PertemuanAction.java:1224");}
						}
					}
				});

				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(laporanMonitorPerkuliahan);
				laporanMonitorPerkuliahan.setBorder("none");
				laporanMonitorPerkuliahan.setHeight("650px");
				laporanMonitorPerkuliahan.setTitle("Laporan Absensi");
				laporanMonitorPerkuliahan.setHeight("95%");
				laporanMonitorPerkuliahan.setWidth("90%");
				laporanMonitorPerkuliahan.setClosable(true);
				laporanMonitorPerkuliahan.onModal();
			}
		});
		Common.appendKeToolbar(downloadLampiran, find, comp);

		PertemuanAction.tampilanExportKeFeeder(find.getParent(), tbmuser, this, this);

	        FilterLanjutHelper.setup(comp);
}

	public static EventListener dataKBM = new EventListener() {

		@SuppressWarnings("unchecked")
		@Override
		public void onEvent(Event arg0) throws Exception {

			final List<Perkuliahan> perkuliahan = (List<Perkuliahan>) arg0.getData();

			if (perkuliahan == null || perkuliahan.isEmpty()) {
				MyMessageboxConfig.show("Perkuliahan tidak ditemukan", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return;
			}

			final MyWindow window = new MyWindow("Kegiatan Belajar Mengajar", "none", true);
			ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(window);

			Center center = new Center();
			center.setBorder("none");
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			Grid grid = new Grid();
			grid.setSclass("dgrid");
			grid.setWidth("100%");
			grid.setParent(center);
			grid.setHeight("100%");
			grid.setStyle("min-height: 400px;");

			Columns columns = new Columns();
			columns.setParent(grid);
			Column column = new Column();
			column.setParent(columns);
			column.setWidth("100%");

			Rows rows = new Rows();
			rows.setParent(grid);

			final MyFormRow finalRowMahasiswa = new MyFormRow();
			finalRowMahasiswa.setValign("top");
			finalRowMahasiswa.setParent(rows);

			final MyFormRow finalRowDosen = new MyFormRow();
			finalRowDosen.setValign("top");
			finalRowDosen.setParent(rows);

			South south = new South();
			ais.ui.util.ZkCompat.setFlex(south, true);
			south.setParent(borderlayout);

			final Toolbar toolbar = new Toolbar();
			// toolbar.setHeight("25px");
			toolbar.setParent(south);

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					TampilanELearningAction.tampilkanStatistikAktifitasMahasiswa(perkuliahan, finalRowMahasiswa,
							finalRowDosen, toolbar);
				}
			});

			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					window.detach();
				}
			});
			cancel.setParent(toolbar);

			window.setVisible(true);
			window.setHeight("97%");
			window.setWidth("97%");
			window.onModal();

		}
	};

	public static EventListener datarekapPembelajaran = new EventListener() {

		@SuppressWarnings("unchecked")
		@Override
		public void onEvent(Event arg0) throws Exception {

			final List<Perkuliahan> perkuliahan = (List<Perkuliahan>) arg0.getData();

			if (perkuliahan == null || perkuliahan.isEmpty()) {
				MyMessageboxConfig.show("Perkuliahan tidak ditemukan", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return;
			}

			final MyWindow window = new MyWindow("Rekapitulasi Pembelajaran", "none", true);
			ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(window);

			Center center = new Center();
			center.setBorder("none");
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			Grid grid = new Grid();
			grid.setSclass("dgrid");
			grid.setWidth("100%");
			grid.setParent(center);
			grid.setHeight("100%");
			grid.setStyle("min-height: 400px;");

			Columns columns = new Columns();
			columns.setParent(grid);
			Column column = new Column();
			column.setParent(columns);
			column.setWidth("100%");

			Rows rows = new Rows();
			rows.setParent(grid);

			final MyFormRow finalRowMahasiswa = new MyFormRow();
			finalRowMahasiswa.setValign("top");
			finalRowMahasiswa.setParent(rows);

			South south = new South();
			ais.ui.util.ZkCompat.setFlex(south, true);
			south.setParent(borderlayout);

			final Toolbar toolbar = new Toolbar();
			// toolbar.setHeight("25px");
			toolbar.setParent(south);

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					TampilanELearningAction.tampilkanRekapPerkuliahan(perkuliahan, finalRowMahasiswa, toolbar);
				}
			});

			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					window.detach();
				}
			});
			cancel.setParent(toolbar);

			window.setVisible(true);
			window.setHeight("97%");
			window.setWidth("97%");
			window.onModal();

		}
	};

	@SuppressWarnings("unchecked")
	public static DspaceInformation getDspace(String cookie, Pertemuan pertemuan, String uuidParent, boolean update)
			throws Exception {

		JSONArray jsonArray = new JSONArray();

		String penulis = "dc.contributor.advisor";
		Map<String, Dosen> map = new java.util.HashMap<String, Dosen>();
		if (pertemuan.getPerkuliahan() != null) {
			map = pertemuan.getPerkuliahan().populateDosen();
			penulis = "dc.contributor.author";

			Collection<Long> detailperkuliahans = pertemuan.getPerkuliahan().ambilDetailperkuliahan();
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)) {
						JSONObject jsonMetadata = new JSONObject();
						jsonMetadata.put("key", "dc.contributor.other");
						jsonMetadata.put("value", detailperkuliahan.getMahasiswa().getNama());
						jsonArray.put(jsonMetadata);
					}
				}
			}

		} else if (pertemuan.getKelompokKkn() != null) {
			map = pertemuan.getKelompokKkn().populateDosen();

			List<String> mhs = HibernateUtil.currentSession().createCriteria(MahasiswaDapatKelompokKkn.class)
					.add(Restrictions.eq("diterima", true))
					.add(Restrictions.eq("kelompokKkn", pertemuan.getKelompokKkn()))
					.createAlias("mahasiswa", "mahasiswa").setProjection(Projections.groupProperty("mahasiswa.nama"))
					.list();
			for (String n : mhs) {
				JSONObject jsonMetadata = new JSONObject();
				jsonMetadata.put("key", "dc.contributor.other");
				jsonMetadata.put("value", n);
				jsonArray.put(jsonMetadata);
			}

		} else if (pertemuan.getKelompokPkl() != null) {
			map = pertemuan.getKelompokPkl().populateDosen();

			List<String> mhs = HibernateUtil.currentSession().createCriteria(MahasiswaDapatKelompokPkl.class)
					.add(Restrictions.eq("diterima", true))
					.add(Restrictions.eq("kelompokPkl", pertemuan.getKelompokPkl()))
					.createAlias("mahasiswa", "mahasiswa").setProjection(Projections.groupProperty("mahasiswa.nama"))
					.list();
			for (String n : mhs) {
				JSONObject jsonMetadata = new JSONObject();
				jsonMetadata.put("key", "dc.contributor.other");
				jsonMetadata.put("value", n);
				jsonArray.put(jsonMetadata);
			}

		} else if (pertemuan.getMahasiswaRequestTugasAkhir() != null) {
			map = pertemuan.getMahasiswaRequestTugasAkhir().populateDosen();
			JSONObject jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.contributor.author");
			jsonMetadata.put("value", pertemuan.getMahasiswaRequestTugasAkhir().getMahasiswa().getNama());
			jsonArray.put(jsonMetadata);
		} else if (pertemuan.getSkripsi() != null) {
			map = pertemuan.getSkripsi().populateDosen();
			JSONObject jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.contributor.author");
			jsonMetadata.put("value", pertemuan.getSkripsi().getMahasiswa().getNama());
			jsonArray.put(jsonMetadata);
		}
		for (Dosen dosen : map.values()) {
			String nama = dosen.getNama();
			JSONObject jsonMetadata = new JSONObject();
			jsonMetadata.put("key", penulis);
			jsonMetadata.put("value", nama);
			jsonArray.put(jsonMetadata);
		}
		Html2Text parser = new Html2Text();
		parser.parse(new StringReader(pertemuan.getCatatan()));

		JSONObject jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.description");
		jsonMetadata.put("value", parser.getText());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.title");
		jsonMetadata.put("value", pertemuan.getTopik());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.subject");
		jsonMetadata.put("value", pertemuan.getTopik());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.date.copyright");
		jsonMetadata.put("value",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonArray.put(jsonMetadata);

		if (pertemuan.getTanggal() != null) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.date.issued");
			jsonMetadata.put("value", Common.databaseDateFormat.get().format(pertemuan.getTanggal()));
			jsonArray.put(jsonMetadata);
		}

		LampiranLain lam = LampiranLain.ambil(pertemuan.getId(), LampiranLain.CATATAN_PERKULIAHAN);
		if (lam != null) {
			String uri = lam.createLinkUri();
			if (uri != null && !uri.trim().isEmpty()) {
				jsonMetadata = new JSONObject();
				jsonMetadata.put("key", "dc.identifier.uri");
				jsonMetadata.put("value", uri);
				jsonArray.put(jsonMetadata);
			}
		}

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("metadata", jsonArray);

		DspaceInformation dspaceInformation = DspaceInformation.dspaceProcess(cookie, pertemuan, jsonPost.toString(),
				jsonArray.toString(), update, "items", "collections/" + uuidParent + "/items", "items/{uuid}/metadata");

		if (lam == null) {
			DspaceInformation.upload(cookie, dspaceInformation.getUuid(), lam,
					"Lampiran Catatan " + pertemuan.getTopik());
		}

		return dspaceInformation;

	}

	class PertemuanRenderer extends ais.ui.util.MyRowRenderer {

		boolean mobile = Common.isMobile();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub

			final Perkuliahan perkuliahan = (Perkuliahan) arg1;

			// Kolom "Detail": dulu expander MyDetail (inline). Kini diganti tombol ikon "mata"
			// dengan label vertikal "Lihat". Saat diklik, detail perkuliahan yang SAMA
			// (aktifitasPerkuliahanHelper.initDetail) ditampilkan dalam MyWindow popup — mirip
			// tampilan pada TampilanELearningAction.
			final MyToolbarbuttonConfig btnLihat = new MyToolbarbuttonConfig("Lihat", "/img/svg/eye.svg");
			btnLihat.setOrient("vertical");
			btnLihat.setTooltiptext("Lihat detail perkuliahan");
			btnLihat.setParent(arg0);

			btnLihat.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event ev) throws Exception {
					MyWindow window = new MyWindow(perkuliahan.info(), "normal", true);
					window.setClosable(true);
					window.setSizable(true);
					window.setMaximizable(true);
					window.setWidth("99%");
					window.setHeight("99%");
					window.setContentStyle("overflow:auto;");

					ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
					groupbox.setStyle("min-height: 200px;");
					groupbox.setParent(window);

					// Lampirkan ke halaman lebih dulu agar konteks komponen sudah ter-attach
					// (sama seperti perilaku expander lama) sebelum initDetail membangun isi.
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

					int banyak = 1;
					try {
						banyak = Integer.parseInt(Common
								.getKonfigurasi("tampilan_jumlah_agenda_perkuliahan", banyak + "").getNilai());
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
					aktifitasPerkuliahanHelper.initDetail(perkuliahan, groupbox, 0, banyak);

					window.onModal();
				}
			});

			Common.getDeskripsiPerkuliahanHbox(perkuliahan, true, !mobile, arg0, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);
				}
			}, false);
		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		return initCriteria(session, order);
	}

	public Criteria initCriteria(Session session, boolean order) {

		Criterion criterionMhs = Restrictions.sqlRestriction("true");
		if (!searchnim.getValue().trim().isEmpty() || !searchnamamhs.getValue().trim().isEmpty()) {
			String sql = "this_.id in (select perkuliahan from detailperkuliahan a inner join mahasiswa b on (a.mahasiswa = b.id) where perkuliahan is not null and b.nama ilike '%"
					+ searchnamamhs.getValue().trim() + "%' and b.nim ilike '%" + searchnim.getValue().trim()
					+ "%' group by perkuliahan)";
			criterionMhs = Restrictions.sqlRestriction(sql);
		}

		if ((searchasisten != null && searchasisten.getAttribute("mahasiswa") != null)
				|| !searchnamaasisten.getValue().trim().isEmpty()) {
			Mahasiswa mahasiswa = (Mahasiswa) searchasisten.getAttribute("mahasiswa");
			String sql = "this_.id in (select perkuliahan from mahasiswa_jadi_asisten a inner join mahasiswa b on (a.mahasiswa=b.id) where perkuliahan is not null and a.aktif=true "
					+ (mahasiswa == null ? "" : "and a.mahasiswa=" + mahasiswa.getId())
					+ (searchnamaasisten.getValue().trim().isEmpty() ? ""
							: "and (b.nama ilike '%" + searchnamaasisten.getValue().trim() + "%' or b.nim ilike '%"
									+ searchnamaasisten.getValue().trim() + "%')")
					+ " group by perkuliahan)";
			criterionMhs = Restrictions.and(criterionMhs, Restrictions.sqlRestriction(sql));
		}

		// KEAMANAN: bila yang login adalah MAHASISWA, batasi daftar perkuliahan HANYA milik
		// dirinya sendiri (perkuliahan yang ia ambil), apa pun nilai filter lainnya. Digabung ke
		// criterionMhs yang ditambahkan ke ROOT criteria (alias this_) — sehalaman dengan pola
		// enrollment yang sudah ada — sehingga ikut membatasi grid maupun seluruh tombol yang
		// memakai initCriteria (Rekap, Laporan, Lampiran, dsb). Mencegah mahasiswa melihat atau
		// mengunduh perkuliahan milik orang lain. (id numerik → aman dari injeksi.)
		if (mahasiswa != null && mahasiswa.getId() != null) {
			criterionMhs = Restrictions.and(criterionMhs, Restrictions.sqlRestriction(
					"this_.id in (select perkuliahan from detailperkuliahan where mahasiswa = "
							+ mahasiswa.getId() + " and perkuliahan is not null group by perkuliahan)"));
		}

		Criteria criteria = session.createCriteria(Perkuliahan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(merupakanRemedial ? Restrictions.eq("merupakanRemedial", true)
						: Restrictions.or(Restrictions.isNull("merupakanRemedial"),
								Restrictions.eq("merupakanRemedial", false)))

				.add(searchtanpakelas.isChecked()
						? Restrictions.or(Restrictions.eq("kelas", ""), Restrictions.isNull("kelas"))
						: Restrictions.sqlRestriction("1=1"))
				.add(searchKeterangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("keterangan", searchKeterangan.getValue().trim(), MatchMode.ANYWHERE));

		if (ekstrakurikuler != null && ekstrakurikuler.equals(Perkuliahan.EKSTRA)) {
			criteria.createAlias("matakuliah", "matakuliah").add(Restrictions.eq("matakuliah.extraKulikuler", true));
		} else {
			criteria.createAlias("matakuliah", "matakuliah")
					.add(Restrictions.or(Restrictions.isNull("matakuliah.extraKulikuler"),
							Restrictions.eq("matakuliah.extraKulikuler", false)));
		}

		Criterion criterion = searchdosen.getAttribute("myValue") == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(Restrictions.eq("dosen1", searchdosen.getAttribute("myValue")),
						Restrictions.eq("dosen2", searchdosen.getAttribute("myValue")));

		criterion = Restrictions.or(criterion, Restrictions.eq("dosen3", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen4", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen5", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen6", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen7", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen8", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen9", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen10", searchdosen.getAttribute("myValue")));

		Criterion criterionNamaDosn = Restrictions.sqlRestriction("1=1");
		if (!searchnamadsn.getValue().trim().isEmpty()) {
			criteria.createAlias("dosen1", "dosen1", Criteria.LEFT_JOIN)
					.createAlias("dosen2", "dosen2", Criteria.LEFT_JOIN)
					.createAlias("dosen3", "dosen3", Criteria.LEFT_JOIN)
					.createAlias("dosen4", "dosen4", Criteria.LEFT_JOIN)
					.createAlias("dosen5", "dosen5", Criteria.LEFT_JOIN)
					.createAlias("dosen6", "dosen6", Criteria.LEFT_JOIN)
					.createAlias("dosen7", "dosen7", Criteria.LEFT_JOIN)
					.createAlias("dosen8", "dosen8", Criteria.LEFT_JOIN)
					.createAlias("dosen9", "dosen9", Criteria.LEFT_JOIN)
					.createAlias("dosen10", "dosen10", Criteria.LEFT_JOIN);

			criterionNamaDosn = Restrictions.ilike("dosen1.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE);

			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen2.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen3.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen4.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen5.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen6.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen7.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen8.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen9.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen10.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
		}

		criteria

				.add(criterionNamaDosn)

				.add(criterionMhs)

				.add(searchnamamk.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("matakuliah.kode", searchnamamk.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.ilike("matakuliah.nama", searchnamamk.getValue().trim(),
										MatchMode.ANYWHERE)))

				.add((searchkurikulum == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchkurikulum.getAttribute("kurikulum") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("kurikulum", searchkurikulum.getAttribute("kurikulum"))))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(Restrictions.ilike("kelas", searchkelas.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchparalel.isChecked() ? Restrictions.or(Restrictions.sqlRestriction(
						"this_.id in (select perkuliahan_paralel from perkuliahan where perkuliahan_paralel is not null)"),
						Restrictions.eq("merupakan_paralel", true)) : Restrictions.sqlRestriction("1=1"))
				.add((searchruang == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchruang.getAttribute("ruang") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("ruang", searchruang.getAttribute("ruang"))))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))

				.add((searchmasaperkulaiahan == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchmasaperkulaiahan.getAttribute("masaPerkuliahan") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("masaPerkuliahan", searchmasaperkulaiahan.getAttribute("masaPerkuliahan"))))

				.add(criterion)

				.add((searchmatakuliah == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchmatakuliah.getAttribute("matakuliah") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("matakuliah", searchmatakuliah.getAttribute("matakuliah"))))

				.add(searchhari.getSelectedItem() == null || searchhari.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("hari", searchhari.getSelectedItem().getValue()))

				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAjaran", searchTahunAjaran.getSelectedItem().getValue()))

				.add(merupakanPraPerkuliahan || searchsemester.getSelectedItem() == null
						|| searchsemester.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("semester", searchsemester.getSelectedItem().getValue()))

				.add(merupakanPraPerkuliahan || searchJenisSemester.getSelectedItem() == null
						|| searchJenisSemester.getSelectedItem().getValue() == null
						|| Perkuliahan.SP.equals(searchJenisSemester.getSelectedItem().getValue())
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("ganjilGenap", searchJenisSemester.getSelectedItem().getValue()))

				.add(merupakanPraPerkuliahan ? Restrictions.eq("merupakanPraPerkuliahan", true)
						: Restrictions.or(Restrictions.eq("merupakanPraPerkuliahan", false),
								Restrictions.isNull("merupakanPraPerkuliahan")))

				.add((searchJenisSemester.getSelectedItem() != null
						&& Perkuliahan.SP.equals(searchJenisSemester.getSelectedItem().getValue()))
								? Restrictions.eq("statusSemesterPendek", Perkuliahan.SEMESTER_PENDEK)
								: merupakanRemedial ? Restrictions.sqlRestriction("true")
										: semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
												: Restrictions.eq("statusSemesterPendek", semesterPendek))

				.createCriteria("jurusan", Criteria.LEFT_JOIN)

				.add(merupakanPraPerkuliahan || searchfakultas.getSelectedItem() == null
						|| searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));

		if (perguruanTinggi != null) {
			criteria.createAlias("jurusan.fakultas", "fakultas", Criteria.LEFT_JOIN)
					.add(Restrictions.eq("fakultas.perguruanTinggi", perguruanTinggi));
		}

		// Filter "Tampilan Kurikulum": OBE / Bukan OBE / Semua. Kurikulum.obe (Boolean,
		// null dianggap bukan OBE). Pakai LEFT JOIN alias agar perkuliahan tanpa kurikulum
		// tetap terhitung sebagai "bukan OBE".
		String filterObe = (searchObe == null || searchObe.getSelectedItem() == null) ? null
				: (String) searchObe.getSelectedItem().getValue();
		if (filterObe != null && !filterObe.trim().isEmpty()) {
			criteria.createAlias("kurikulum", "kurikulumObe", Criteria.LEFT_JOIN);
			if ("OBE".equals(filterObe)) {
				criteria.add(Restrictions.eq("kurikulumObe.obe", true));
			} else if ("NONOBE".equals(filterObe)) {
				criteria.add(Restrictions.or(Restrictions.isNull("kurikulumObe.obe"),
						Restrictions.eq("kurikulumObe.obe", false)));
			}
		}

		if (order)
			criteria.add(Restrictions.sqlRestriction(
					"1=1 order by case hari when 'Senin' then 1 when 'Selasa' then 2 when 'Rabu' then 3 when 'Kamis' then 4 when 'Jumat' then 5  when 'Sabtu' then 6 when 'Minggu' then 7 else 5 end, waktu_mulai_d"));

		return criteria;
	}

	@Override
	public void loadData(Object value) {
		onSearchDefault(null);

	}

	@SuppressWarnings("unchecked")
	/**
	 * Mengambil nilai (value) item terpilih dari sebuah {@link Combobox} secara aman.
	 *
	 * @param combo combobox sumber; boleh {@code null}.
	 * @return nilai {@code toString()} dari item terpilih, atau {@code null} bila combo/nilai kosong.
	 */
	private static String nilaiCombo(Combobox combo) {
		if (combo == null || combo.getSelectedItem() == null || combo.getSelectedItem().getValue() == null) {
			return null;
		}
		return combo.getSelectedItem().getValue().toString();
	}

	public void onSearchDefault(final Event event) {
		final boolean isEventCari = event != null && event.getName() != null
				&& event.getName().equalsIgnoreCase("cari");
		// Nilai filter dibaca dari toolbar ringkas mahasiswa/dosen (ta/smt/hari/keyword) BILA ada;
		// bila tampilan memakai KARTU FILTER (FilterCard, tanpa toolbar ringkas mynorth) maka
		// jatuh-balik ke field FilterCard (searchTahunAjaran/searchJenisSemester/searchhari/
		// searchnamamk) agar pencarian TETAP berfungsi untuk mahasiswa & dosen.
		final String tahunAkademik = nilaiCombo(ta != null ? ta : searchTahunAjaran);
		final String jenisSemester = nilaiCombo(smt != null ? smt : searchJenisSemester);
		final String hr = nilaiCombo(hari != null ? hari : searchhari);
		final String keywordVal = (keyword != null && keyword.getValue() != null
				&& !keyword.getValue().trim().isEmpty()) ? keyword.getValue().trim()
						: (searchnamamk != null && searchnamamk.getValue() != null ? searchnamamk.getValue().trim() : "");
		final int activePage = paging == null ? 0 : paging.getActivePage();
		final int jumlahDataDalamSatuHalamanElearning = 10;
		// Cabang ambilPerkuliahanDanParalel HANYA memuat perkuliahan milik user TANPA menerapkan
		// filter lanjutan (MK/Kelas/Fakultas/Prodi/Ruang/Kurikulum), sehingga hanya cocok untuk
		// tampilan toolbar ringkas (mynorth != null). Pada tampilan KARTU FILTER (FilterCard,
		// mynorth == null — mis. pertemuan.zul yang tak punya <north id="mynorth">) baik MAHASISWA
		// maupun DOSEN HARUS memakai cabang else → initCriteria, agar:
		//   (1) filter lanjutan (MK/Kelas/Fakultas/dst.) BENAR-BENAR diterapkan, DAN
		//   (2) pembatasan "hanya milik sendiri" tetap berlaku: DOSEN lewat searchdosen (preset +
		//       disabled di cabang else doAfterCompose ~796) dan MAHASISWA lewat criterionMhs di
		//       initCriteria (~1629).
		// BUG sebelumnya: syarat "mynorth != null" dibuang, sehingga di FilterCard DOSEN masuk ke
		// ambilPerkuliahanDanParalel dan filter lanjutan DIABAIKAN (selalu tampil semua). Syarat
		// mynorth dikembalikan di sini untuk memperbaikinya.
		final boolean isMahasiswaArea = mynorth != null && tbmuser != null && tbmuser.getMahasiswa() != null;
		final boolean isDosenArea = !isMahasiswaArea && mynorth != null && tbmuser != null
				&& tbmuser.ambilDosen() != null && tbmuser.hakAkses() != null && tbmuser.hakAkses().getRoleId() != null
				&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen");

		try {
			if (isMahasiswaArea) {
				if (isEventCari) {
					try {
						mahasiswa.reInitDetailperkuliahan(HibernateUtil.currentSession());
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
				Object[] objects = mahasiswa.ambilPerkuliahanDanParalel(tahunAkademik, jenisSemester, hr, keywordVal,
						"", merupakanPraPerkuliahan, ekstrakurikuler, true, merupakanRemedial, false,
						TampilanELearningAction.PERKULIAHAN, jumlahDataDalamSatuHalamanElearning * activePage,
						jumlahDataDalamSatuHalamanElearning);
				perkuliahans = (List<Perkuliahan>) objects[0];
				semuaperkuliahans = (List<Perkuliahan>) objects[2];
				int totalSize = (Integer) objects[1];
				if (paging != null) {
					paging.setPageSize(jumlahDataDalamSatuHalamanElearning);
					paging.setMold("os");
					paging.setTotalSize(totalSize);
					paging.setVisible(totalSize > jumlahDataDalamSatuHalamanElearning);
					try {
						if (paging.getParent() instanceof org.zkoss.zul.South) {
							((org.zkoss.zul.South) paging.getParent()).setHeight(paging.isVisible() ? "30px" : "0px");
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			} else if (isDosenArea) {
				if (isEventCari) {
					try {
						dosen.reInitPerkuliahan(HibernateUtil.currentSession());
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
				Object[] objects = dosen.ambilPerkuliahanDanParalel(HibernateUtil.currentSession(), tahunAkademik,
						jenisSemester, hr, keywordVal, "", merupakanPraPerkuliahan, ekstrakurikuler, true,
						merupakanRemedial, false, true, true, true, true, true, true, true, true, true,
						TampilanELearningAction.PERKULIAHAN, jumlahDataDalamSatuHalamanElearning * activePage,
						jumlahDataDalamSatuHalamanElearning);
				perkuliahans = (List<Perkuliahan>) objects[0];
				semuaperkuliahans = (List<Perkuliahan>) objects[2];
				int totalSize = (Integer) objects[1];
				if (paging != null) {
					paging.setPageSize(jumlahDataDalamSatuHalamanElearning);
					paging.setMold("os");
					paging.setTotalSize(totalSize);
					paging.setVisible(totalSize > jumlahDataDalamSatuHalamanElearning);
					try {
						if (paging.getParent() instanceof org.zkoss.zul.South) {
							((org.zkoss.zul.South) paging.getParent()).setHeight(paging.isVisible() ? "30px" : "0px");
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			} else {
				Common.initPaging(initCriteria(false), paging);
				perkuliahans = ConstantValues.simpleList(
						initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
								.setFirstResult(Common.ROWS_COUNT_ON_PAGE * activePage),
						Perkuliahan.class);
			}
			if (grid != null) {
				org.zkoss.zul.ListModel strset = new org.zkoss.zul.SimpleListModel(
						perkuliahans != null ? perkuliahans : new java.util.ArrayList<Perkuliahan>());
				grid.setRowRenderer(new PertemuanRenderer());
				grid.setModelCheckMobile(strset);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

}
