package ais.action.master.kkn;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.poi.ss.usermodel.Hyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.poi.xssf.usermodel.XSSFHyperlink;
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
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
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
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.EksporFromFeederAction;
import ais.action.master.JurusanAction;
import ais.action.master.bkd.helper.PenilaianAsesorHelper;
import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.feeder.util.FeederExporter;
import ais.action.master.helper.AktifitasKknHelper;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.DspaceInformation;
import ais.database.model.FormatNilai;
import ais.database.model.Jurusan;
import ais.database.model.Kkn;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaDapatKelompokKkn;
import ais.database.model.Matakuliah;
import ais.database.model.NilaiHuruf;
import ais.database.model.Pegawai;
import ais.database.model.PenilaianAsesor;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Sertifikat;
import ais.database.model.Tbmuser;
import ais.database.model.asset.Lokasi;
import ais.database.model.file.LampiranKknMahasiswa;
import ais.database.model.file.LampiranLain;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.kkn.KknPunyaPersyaratan;
import ais.database.model.kkn.MahasiswaDaftarKkn;
import ais.database.model.kkn.MahasiswaKknPersyaratan;
import ais.database.model.kkn.PersyaratanKkn;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelKecilSekali;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class KelompokKknAction extends GenericAutowireComposer implements DataSearchDefault, DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;
	private Textbox searchnama;
	private Textbox searchketerangan;
	private Combobox searchkkn;
	private AmbilDataDosenBanbox searchdosen1;
	private AmbilDataMahasiswaBanbox searchmahasiswa;

	private Textbox nama_kelompok;
	private MyDatebox tanggal_mulai;
	private MyDatebox tanggal_selesai;
	private Textbox alamat;
	private Textbox keterangan;
	private Combobox kkn;
	private AmbilDataDosenBanbox dosen_pembimbing1;
	private AmbilDataDosenBanbox dosen_pembimbing2;
	private AmbilDataDosenBanbox dosen_pembimbing3;
	private AmbilDataDosenBanbox dosen_pembimbing4;
	private AmbilDataDosenBanbox dosen_pembimbing5;
	private AmbilDataDosenBanbox dosen_pembimbing6;
	private AmbilDataDosenBanbox dosen_pembimbing7;
	private AmbilDataDosenBanbox dosen_pembimbing8;
	private AmbilDataDosenBanbox dosen_pembimbing9;
	private AmbilDataDosenBanbox dosen_pembimbing10;

	private MyCheckboxConfig searchBelumMasukFeeder;
	private MyCheckboxConfig searchMasukFeeder;

	private KelompokKkn kelompokKkn;

	private MyToolbarbuttonConfig add;
	private boolean edit;
	private boolean delete;

	private Tbmuser tbmuser;
	private Dosen dosenTerpilih;
	private boolean ases = false;
	private MyColumnConfig colEdit;
	private MyCheckboxConfig mahasiswaBisaMemilih;
	private MyIntbox kuota;
	private Combobox sertifikat;
	private Textbox noSk;
	private MyDatebox tglSk;
	protected LampiranLain lainMahasiswa;
	private Textbox feeder;

	private static AktifitasKknHelper aktifitasKknHelper = new AktifitasKknHelper();

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

		tbmuser = Common.getCurrentUser();

		searchmahasiswa.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		if (execution.getParameter("dosen") != null) {
			dosenTerpilih = (Dosen) HibernateUtil.currentSession().createCriteria(Dosen.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("dosen").trim()))).uniqueResult();
		}

		if (execution.getParameter("ases") != null) {
			ases = Boolean.parseBoolean(execution.getParameter("ases"));
		}

		if (dosenTerpilih != null) {
			searchdosen1.setAttribute("dosen", dosenTerpilih);
			searchdosen1.setAttribute("myValue", dosenTerpilih);
			searchdosen1.setValue(dosenTerpilih.getNama());
			searchdosen1.setDisabled(true);
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE) && tbmuser != null
				&& tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null);
		add.setTooltiptext("Tambah");
		}

		if (ases || tbmuser.ambilDosen() != null) {
			colEdit.setVisible(false);
			add.setVisible(false);
		}

		Common.insertComboDanSemua(searchkkn, "nama", Kkn.class);
		if (searchkkn != null) { searchkkn.setReadonly(true); }

		String[] contents = new String[] { "id", "kkn", "nama_kelompok", "tanggal_mulai", "tanggal_selesai", "tglSk",
				"noSk", "mahasiswaBisaMemilih", "kuota", "alamat", "dosen_pembimbing1", "dosen_pembimbing2",
				"dosen_pembimbing3", "dosen_pembimbing4", "dosen_pembimbing5", "dosen_pembimbing6", "dosen_pembimbing7",
				"dosen_pembimbing8", "dosen_pembimbing9", "dosen_pembimbing10", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KelompokKkn.class, contents);
		upload.setVisible((add != null && add.isVisible()) && edit && delete && tbmuser != null && tbmuser.getMahasiswa() == null
				&& tbmuser.getSiswa() == null && tbmuser.ambilDosen() == null);
		Common.appendKeToolbar(upload, add, comp);

		contents = new String[] { "id", "namaDosen", "kelompokKkn", "mahasiswa.nim", "mahasiswa.nama",
				"mahasiswa.kelamin", "mahasiswa.jurusan", "mahasiswa.jurusan.fakultas", "diterima", "detailperkuliahan",
				"totalNilai", "nilaiHuruf", "totalIP", "lulus", "hasil", "keterangan" };
		cetakToolbarbutton = Common.cetakDataCustomButton(MahasiswaDapatKelompokKkn.class, new DataCriteria() {

			@Override
			public Criteria initCriteria(boolean order) {

				Mahasiswa mahasiswa = (Mahasiswa) searchmahasiswa.getAttribute("mahasiswa");

				Dosen dosenPemimbing = (Dosen) searchdosen1.getAttribute("myValue");

				Criterion criterion = Restrictions.eq("dosen_pembimbing1", dosenPemimbing);
				criterion = Restrictions.or(criterion, Restrictions.eq("dosen_pembimbing2", dosenPemimbing));
				criterion = Restrictions.or(criterion, Restrictions.eq("dosen_pembimbing3", dosenPemimbing));
				criterion = Restrictions.or(criterion, Restrictions.eq("dosen_pembimbing4", dosenPemimbing));
				criterion = Restrictions.or(criterion, Restrictions.eq("dosen_pembimbing5", dosenPemimbing));
				criterion = Restrictions.or(criterion, Restrictions.eq("dosen_pembimbing6", dosenPemimbing));
				criterion = Restrictions.or(criterion, Restrictions.eq("dosen_pembimbing7", dosenPemimbing));
				criterion = Restrictions.or(criterion, Restrictions.eq("dosen_pembimbing8", dosenPemimbing));
				criterion = Restrictions.or(criterion, Restrictions.eq("dosen_pembimbing9", dosenPemimbing));
				criterion = Restrictions.or(criterion, Restrictions.eq("dosen_pembimbing10", dosenPemimbing));

				Criteria criteria = HibernateUtil.currentSession().createCriteria(MahasiswaDapatKelompokKkn.class)
						.add(mahasiswa == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("mahasiswa", mahasiswa))
						.createCriteria("kelompokKkn")
						.add(dosenPemimbing == null ? Restrictions.sqlRestriction("1=1") : criterion)
						.add(searchkkn.getSelectedItem() == null || searchkkn.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("kkn", searchkkn.getSelectedItem().getValue()));
				if (order)
					criteria.addOrder(Order.desc("nama_kelompok")).addOrder(Order.desc("tanggal_mulai"));

				criteria.add(Restrictions.ilike("nama_kelompok", searchnama.getValue(), MatchMode.ANYWHERE))
						.add(Restrictions.ilike("keterangan", searchketerangan.getValue(), MatchMode.ANYWHERE));
				return criteria;
			}
		}, "Download Data Peserta", "/img/print.png", contents);
		cetakToolbarbutton.setVisible((add != null && add.isVisible()) && edit && delete && tbmuser != null
				&& tbmuser.getMahasiswa() == null && tbmuser.ambilDosen() == null);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		upload = Common.uploadData(this, MahasiswaDapatKelompokKkn.class, contents);
		if (upload != null) { upload.setLabel("Upload Data Peserta"); }
		upload.setVisible((add != null && add.isVisible()) && edit && delete && tbmuser != null && tbmuser.getMahasiswa() == null
				&& tbmuser.getSiswa() == null && tbmuser.ambilDosen() == null);
		Common.appendKeToolbar(upload, add, comp);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		tbmuser = Common.getCurrentUser();
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Singkronkan Nilai", "/img/Configure.gif");
		if (button != null) { button.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null); }
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings({ "unchecked" })
			@Override
			public void onEvent(Event arg0) throws Exception {

				final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Nilai KKN");

				final Label label = Common.displayLoadBar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						laporan.selesaikan(new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								onSearchDefault(null);
							}
						});
					}
				});

				new Thread(new Runnable() {

					@Override
					public void run() {
						// PENTING (fix Session-is-closed, insiden 07-23, Error C/D): SEBELUMNYA
						// session dibuka via HibernateUtil.currentNativeSession() (ThreadLocal
						// PER-THREAD) lalu di-CLOSE (HibernateUtil.closeSession()) SEGERA setelah
						// memuat daftar mahasiswaDapatKelompokKkns -- padahal entity yang baru saja
						// dimuat itu MASIH dipakai sepanjang loop di bawah (lazy-load relasi seperti
						// getMahasiswa()/getKelompokKkn()), dan session ThreadLocal yang sama juga
						// dipakai ulang oleh banyak helper lain di thread latar ini (Common.refreshUpdate,
						// Common.getFormatNilais, dll) yang masing-masing bisa menutupnya lagi via
        				// currentNativeSession()/closeSession() sesuai kontrak COOKBOOK HibernateUtil,
						// sehingga session tertutup DI TENGAH loop -> "Session is closed!" pada
						// session.getTransaction().begin()/commit() (Error C/D). Fix: buka SATU session
						// TERISOLASI (HibernateUtil.openSession(), bukan ThreadLocal) yang dipakai
						// konsisten sepanjang seluruh proses (list + loop), tidak pernah ditutup di
						// tengah, dan ditutup TUNTAS sekali saja di finally lewat closeSessionQuietly.
						Session session = HibernateUtil.openSession();
						try {

						try {
							List<MahasiswaDapatKelompokKkn> mahasiswaDapatKelompokKkns = session
									.createCriteria(MahasiswaDapatKelompokKkn.class)
									.add(Restrictions.eq("diterima", true)).addOrder(Order.asc("id")).list();
							int size = mahasiswaDapatKelompokKkns.size();
							int index = 0;
							for (MahasiswaDapatKelompokKkn mahasiswaDapatKelompokKkn : mahasiswaDapatKelompokKkns) {
								String kunciMhs = String.valueOf(mahasiswaDapatKelompokKkn);
								try {
									Mahasiswa mahasiswa = mahasiswaDapatKelompokKkn.getMahasiswa();
									index++;
									// FIX akar masalah NullPointerException (KE-3) + ConstraintViolationException
									// "null value in column kelompok_kkn" (KE-4): baris MahasiswaDapatKelompokKkn
									// ini kelompokKkn-nya sudah null di database (data yatim, mis. kelompok KKN-nya
									// sudah dihapus tapi baris penugasan mahasiswanya belum ikut dibersihkan).
									// Sebelumnya baris label progres di bawah memanggil getKelompokKkn().getNama()
									// TANPA null-check (beda dari baris kunciMhs tepat di atasnya yang sudah benar)
									// -> NPE. Selain itu memproses baris rusak ini lebih lanjut (refreshUpdate)
									// memicu flush yang menabrak constraint NOT NULL kolom kelompok_kkn. Lewati
									// baris ini sepenuhnya di sumbernya -- tidak ada kelompok KKN utk diproses.
									if (mahasiswaDapatKelompokKkn.getKelompokKkn() == null) {
										continue;
									}
									kunciMhs = String.valueOf(mahasiswa) + " / "
											+ mahasiswaDapatKelompokKkn.getKelompokKkn().getNama();
									label.setValue("Sedang memproses nilai " + mahasiswa + " kelompok "
											+ mahasiswaDapatKelompokKkn.getKelompokKkn().getNama() + " ("
											+ Common.numberFormat.get().format((index * 100.0) / size) + " %)");

									Detailperkuliahan detailperkuliahan = mahasiswaDapatKelompokKkn
											.getDetailperkuliahan();

									if (detailperkuliahan == null) {
										detailperkuliahan = (Detailperkuliahan) session
												.createCriteria(Detailperkuliahan.class)
												.add(Restrictions.eq("mahasiswa", mahasiswa))
												.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
												.createAlias("perkuliahan", "perkuliahan", Criteria.LEFT_JOIN)
												.createAlias("perkuliahan.matakuliah", "matakuliah", Criteria.LEFT_JOIN)
												.createAlias("matakuliahKonversi", "matakuliahKonversi",
														Criteria.LEFT_JOIN)
												.add(Restrictions.or(
														Restrictions.ilike("matakuliah.nama",
																Common.getBahasaConfig("kkn"), MatchMode.ANYWHERE),
														Restrictions.ilike("matakuliahKonversi.nama",
																Common.getBahasaConfig("kkn"), MatchMode.ANYWHERE)))
												.addOrder(Order.desc("semester")).setMaxResults(1).uniqueResult();
										mahasiswaDapatKelompokKkn.setDetailperkuliahan(detailperkuliahan);

										session.getTransaction().begin();
										Common.refreshUpdate(session, mahasiswaDapatKelompokKkn);
										session.getTransaction().commit();
									}

									if (detailperkuliahan != null) {

										session.refresh(detailperkuliahan);

										List<FormatNilai> formatNilais = Common.getFormatNilais(session,
												detailperkuliahan.getPerkuliahan());
										for (FormatNilai formatNilai : formatNilais) {
											detailperkuliahan.populateDetailNilai(formatNilai, null,
													mahasiswaDapatKelompokKkn.getTotalNilai(), true, tbmuser);
										}

										detailperkuliahan.setTotalNilai(mahasiswaDapatKelompokKkn.getTotalNilai());
										detailperkuliahan.setTotalIP(mahasiswaDapatKelompokKkn.getTotalIP());
										detailperkuliahan.setNilaiHuruf(mahasiswaDapatKelompokKkn.getNilaiHuruf());
										detailperkuliahan.setLulus(mahasiswaDapatKelompokKkn.getLulus());
										Matakuliah matakuliah = detailperkuliahan == null ? null
												: detailperkuliahan.getPerkuliahan() != null
														? detailperkuliahan.getPerkuliahan().getMatakuliah()
														: detailperkuliahan.getMatakuliahKonversi();
										Double totalSementara = mahasiswaDapatKelompokKkn.getTotalNilai();
										NilaiHuruf nilaiHuruf = Common.getNilaiHuruf(totalSementara,
												detailperkuliahan.getMahasiswa().getTahunangkatan(),
												detailperkuliahan.getMahasiswa().getJurusan(),
												detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
												detailperkuliahan.getTahunAkademik(),
												detailperkuliahan.getSemester() % 2 == 0 ? Perkuliahan.GENAP
														: Perkuliahan.GANJIL,
												matakuliah == null ? "" : matakuliah.getKode(),
												matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

										detailperkuliahan.setTotalNilaiSementara(totalSementara);
										detailperkuliahan.setNilaiHurufSementara(
												nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
										detailperkuliahan.setTotalIPSementara(
												nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());

										session.getTransaction().begin();
										Common.refreshUpdate(session, detailperkuliahan);
										session.getTransaction().commit();

									}
									laporan.catatBerhasil(index - 1, kunciMhs, "Sinkronisasi nilai berhasil");
								} catch (Exception e) {
									ais.common.Common.tampilErrorJikaAdmin(e);
									laporan.catatGagalDetail(index - 1, kunciMhs, e);
								}
							}
						} catch (Exception e) {
							ais.common.Common.tampilErrorJikaAdmin(e);
							laporan.tambahCatatan("Proses sinkronisasi terhenti total (di luar per-mahasiswa): "
									+ ais.common.LaporanUpload.detailTeknisException(e));
						}
											} finally {
							label.setValue("");
							ais.database.hibernate.HibernateUtil.closeSessionQuietly(session);
						}
					}
				}).start();

			}

		});
		if (button != null) { button.setParent(add.getParent()); }

		cetakToolbarbutton = cetakDataCustomButton("Download Persyaratan", "/img/excel.png");
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

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

													List<KelompokKkn> tbmusers = ConstantValues
															.simpleList(initCriteria(true), KelompokKkn.class);
													int size = tbmusers.size();
													int index = 1;
													for (KelompokKkn kelompokKkn : tbmusers) {
														myLabelProsesDetail.setValue("Memproses "
																+ kelompokKkn.getNama_kelompok() + " ("
																+ Common.numberFormat.get().format((index * 100.0) / size)
																+ "%");
														index++;
														feederImporter.aktivitasMahasiswaKkn(kelompokKkn, errorLog);

													}
													tbmusers.clear();
													tbmusers = null;
													// FIX "gagal diam-diam": sebelumnya exception di sini hanya dicatat ke log admin lalu progres diset "" (=SUKSES palsu) di luar try.
													myLabelProsesDetail.setValue("");
												} catch (Exception e) {
													ais.common.Common.tampilErrorJikaAdmin(e);
													myLabelProsesDetail.setValue(
															"Error: " + ais.common.PesanFormalHelper.pesanGagalException(
																	"pengiriman data Kelompok KKN ke Neo Feeder",
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

	}

	public static Hbox tampilkanInfoDosen(KelompokKkn kelompokKkn, boolean tampilkanAsesor, boolean rinci)
			throws Exception {
		List<Dosen> dosens = kelompokKkn.populateDosenBuNama();
		return tampilkanInfoDosen(kelompokKkn, tampilkanAsesor, rinci, dosens);
	}

	public static Hbox tampilkanInfoDosen(KelompokKkn kelompokKkn, boolean tampilkanAsesor, boolean rinci,
			List<Dosen> dosens) throws Exception {

		if (!rinci) {
			int tampilPerRow = Common.isMobile() ? 2 : 6;
			Hbox hbox = new Hbox();

			Vbox vboxBaru = new Vbox();
			vboxBaru.setParent(hbox);

			Hbox hboxBaru = new Hbox();
			hboxBaru.setParent(vboxBaru);
			int size = 0;
			for (Dosen dosen : dosens) {
				if (size % tampilPerRow == 0) {
					hboxBaru = new Hbox();
					hboxBaru.setParent(vboxBaru);
				}
				size++;
				CommonMedia.tampilkanGambarKecil(dosen).setParent(hboxBaru);
			}

			return hbox;
		} else {
			Hbox hbox = new Hbox();
			for (Dosen dosen : dosens) {
				Vbox vbox = new Vbox();
				vbox.setParent(hbox);
				CommonMedia.tampilkanGambarKecil(dosen).setParent(vbox);
				new Label(dosen.getNama()).setParent(vbox);
			}
			return hbox;
		}
	}

	@SuppressWarnings("unchecked")
	public MyToolbarbuttonConfig cetakDataCustomButton(String buttonLabel, String buttonImage) {

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(buttonLabel, buttonImage);
		toolbarbutton.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final Kkn kkn = (Kkn) (searchkkn.getSelectedItem() == null ? null
						: searchkkn.getSelectedItem().getValue());
				if (kkn == null) {
					MyMessageboxConfig.show("Mohon maaf, data KKN belum dipilih pada filter pencarian. Langkah yang dapat dilakukan: (1) buka filter pencarian di bagian atas halaman; (2) pilih salah satu data KKN dari daftar yang tersedia; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}

				Session session = HibernateUtil.currentSession();
				final List<PersyaratanKkn> persyaratanKkns = session.createCriteria(KknPunyaPersyaratan.class)
						.createAlias("persyaratanKkn", "persyaratanKkn").add(Restrictions.eq("kkn", kkn))
						.setProjection(Projections.property("persyaratanKkn"))
						.addOrder(Order.asc("persyaratanKkn.nama")).addOrder(Order.asc("persyaratanKkn.labelInputan"))
						.list();

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
								spreadsheet.setMaxcolumns(persyaratanKkns.size() + 15);
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

						@Override
						public void run() {
							try {

							try {

								Session session = HibernateUtil.currentNativeSession();
								Criteria criteria = session.createCriteria(MahasiswaDapatKelompokKkn.class)
										.createCriteria("kelompokKkn").add(Restrictions.eq("kkn", kkn));

								criteria.addOrder(Order.desc("nama_kelompok")).addOrder(Order.desc("tanggal_mulai"));
								criteria.add(
										searchnama == null || searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
												: Restrictions.ilike("nama_kelompok", searchnama.getValue().trim(),
														MatchMode.ANYWHERE))
										.add(searchketerangan.getValue().trim().isEmpty()
												? Restrictions.sqlRestriction("true")
												: Restrictions.ilike("keterangan", searchketerangan.getValue().trim(),
														MatchMode.ANYWHERE));

								List<MahasiswaDapatKelompokKkn> data = criteria.setMaxResults(1048576).list();
								HibernateUtil.closeSession();
								intbox.setValue(data.size());
								System.out.println("data = " + data.size());

								XSSFWorkbook workbook = new XSSFWorkbook();

								XSSFCellStyle lockedNumericStyle = workbook.createCellStyle();
								lockedNumericStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
								lockedNumericStyle.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
								// lockedNumericStyle.setLocked(true);

								XSSFCellStyle hlink_style = workbook.createCellStyle();
								XSSFFont hlink_font = workbook.createFont();
								hlink_font.setUnderline(XSSFFont.U_SINGLE);
								hlink_font.setColor(new XSSFColor(Color.BLUE));
								hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
								hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
								hlink_style.setFont(hlink_font);

								XSSFCellStyle notLocked = workbook.createCellStyle();
								notLocked.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
								notLocked.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
								// notLocked.setLocked(false);

								XSSFSheet sheet = workbook.createSheet("CETAK DATA");
								// sheet.protectSheet("passwordrahasia");
								sheet.setDefaultColumnWidth(20);
								int rowIndex = 0;

								XSSFRow rowhead = sheet.createRow((short) 0);

								rowhead.createCell(0).setCellValue("Kelompok");
								rowhead.createCell(1).setCellValue("Dosen");
								rowhead.createCell(2).setCellValue("NIM");
								rowhead.createCell(3).setCellValue("Nama");
								rowhead.createCell(4).setCellValue("Fakultas");
								rowhead.createCell(5).setCellValue("Jurusan");
								rowhead.createCell(6).setCellValue("Program");
								rowhead.createCell(7).setCellValue("Diterima");
								rowhead.createCell(8).setCellValue("Nilai masuk ke matakuliah");
								rowhead.createCell(9).setCellValue("Nilai Angka");
								rowhead.createCell(10).setCellValue("Nilai Huruf");
								rowhead.createCell(11).setCellValue("Nilai IP");
								rowhead.createCell(12).setCellValue("Lulus");
								rowhead.createCell(13).setCellValue("Hasil");
								rowhead.createCell(14).setCellValue("Keterangan");

								for (int i = 15; i < persyaratanKkns.size() + 15; i++) {
									PersyaratanKkn persyaratanKkn = persyaratanKkns.get(i - 15);
									if (persyaratanKkn.getLabelInputan() == null
											|| persyaratanKkn.getLabelInputan().trim().isEmpty()) {
										rowhead.createCell(i).setCellValue(persyaratanKkn.getNama());
									} else {
										rowhead.createCell(i).setCellValue(persyaratanKkn.getLabelInputan());

									}
								}

								for (MahasiswaDapatKelompokKkn o : data) {

									try {
										rowIndex++;
										if (o == null) {
											continue;
										}
										Mahasiswa mahasiswa = o.getMahasiswa();
										label.setValue("Sedang memproses data " + o.toString() + " ("
												+ Common.numberFormat.get().format(rowIndex * 100.0 / data.size()) + " %)");

										XSSFRow row = sheet.createRow(rowIndex);
										XSSFCell cell = row.createCell(0);
										cell.setCellStyle(notLocked);
										cell.setCellValue(o.getKelompokKkn() == null ? ""
												: o.getKelompokKkn().getNama_kelompok());

										cell = row.createCell(1);
										cell.setCellStyle(notLocked);
										cell.setCellValue(o.getKelompokKkn() == null ? ""
												: o.getKelompokKkn().populateDosenBuNama().toString()
														.replaceAll("\\[", "").replaceAll("\\]", ""));

										cell = row.createCell(2);
										cell.setCellStyle(notLocked);
										cell.setCellValue(mahasiswa.getNim());

										cell = row.createCell(3);
										cell.setCellStyle(notLocked);
										cell.setCellValue(mahasiswa.getNama());

										cell = row.createCell(4);
										cell.setCellStyle(notLocked);
										cell.setCellValue(mahasiswa.getJurusan().getFakultas().getNama());

										cell = row.createCell(5);
										cell.setCellStyle(notLocked);
										cell.setCellValue(mahasiswa.getJurusan().getNama());

										cell = row.createCell(6);
										cell.setCellStyle(notLocked);
										cell.setCellValue(mahasiswa.getProgram());

										cell = row.createCell(7);
										cell.setCellStyle(notLocked);
										cell.setCellValue(o.getDiterima() ? "Ya" : "Tidak");

										cell = row.createCell(8);
										cell.setCellStyle(notLocked);
										try {
											cell.setCellValue(o.getDetailperkuliahan() == null ? ""
													: (o.getDetailperkuliahan().getMatakuliahKonversi() == null
															? o.getDetailperkuliahan().getPerkuliahan().getMatakuliah()
																	.getNama()
															: o.getDetailperkuliahan().getMatakuliahKonversi()
																	.getNama()));
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/kkn/KelompokKknAction.java:865");
											// TODO: handle exception
										}

										cell = row.createCell(9);
										cell.setCellStyle(notLocked);
										cell.setCellValue(o.getTotalNilai());

										cell = row.createCell(10);
										cell.setCellStyle(notLocked);
										cell.setCellValue(o.getNilaiHuruf());

										cell = row.createCell(11);
										cell.setCellStyle(notLocked);
										cell.setCellValue(o.getTotalIP());

										cell = row.createCell(12);
										cell.setCellStyle(notLocked);
										cell.setCellValue(o.getLulus() ? "Ya" : "Tidak");

										cell = row.createCell(13);
										cell.setCellStyle(notLocked);
										cell.setCellValue(o.getHasil());

										cell = row.createCell(14);
										cell.setCellStyle(notLocked);
										cell.setCellValue(o.getKeterangan());

										for (int i = 15; i < persyaratanKkns.size() + 15; i++) {

											try {
												PersyaratanKkn persyaratanKkn = persyaratanKkns.get(i - 15);
												session = HibernateUtil.currentNativeSession();
												MahasiswaKknPersyaratan mahasiswaKknPersyaratan = (MahasiswaKknPersyaratan) session
														.createCriteria(MahasiswaKknPersyaratan.class)
														.add(Restrictions.eq("mahasiswa", mahasiswa))
														.add(Restrictions.eq("kkn", kkn)).addOrder(Order.desc("id"))
														.setMaxResults(1)
														.add(Restrictions.eq("persyaratanKkn", persyaratanKkn))
														.uniqueResult();
												if (mahasiswaKknPersyaratan == null) {
													mahasiswaKknPersyaratan = new MahasiswaKknPersyaratan();
													mahasiswaKknPersyaratan.setMahasiswa(mahasiswa);
													mahasiswaKknPersyaratan.setKkn(kkn);
													mahasiswaKknPersyaratan.setPersyaratanKkn(persyaratanKkn);
													session.getTransaction().begin();
													session.save(mahasiswaKknPersyaratan);
													session.getTransaction().commit();
												}
												HibernateUtil.closeSession();

												if (persyaratanKkn.getTipeDataInputan().equals(PersyaratanKkn.TEXT)
														|| persyaratanKkn.getTipeDataInputan()
																.equals(PersyaratanKkn.TEXT_ANGKA)) {
													cell = row.createCell(i);
													cell.setCellStyle(lockedNumericStyle);
													if (mahasiswaKknPersyaratan.getNilaiString() != null) {
														cell.setCellValue(mahasiswaKknPersyaratan.getNilaiString());
													}
												} else if (persyaratanKkn.getTipeDataInputan()
														.equals(PersyaratanKkn.TANGGAL)) {
													cell = row.createCell(i);
													cell.setCellStyle(lockedNumericStyle);
													cell.setCellValue(
															mahasiswaKknPersyaratan.getNilaiTanggal() == null ? ""
																	: Common.dateFormat1.get().format(
																			mahasiswaKknPersyaratan.getNilaiTanggal()));

												} else if (persyaratanKkn.getTipeDataInputan()
														.equals(PersyaratanKkn.ANGKA)) {
													cell = row.createCell(i);
													cell.setCellStyle(lockedNumericStyle);
													if (mahasiswaKknPersyaratan.getNilaiNumber() != null) {
														cell.setCellValue(mahasiswaKknPersyaratan.getNilaiNumber());
													}

												} else if (persyaratanKkn.getTipeDataInputan()
														.equals(PersyaratanKkn.PILIHAN_YA_TIDAK)) {
													cell = row.createCell(i);
													cell.setCellStyle(lockedNumericStyle);
													if (mahasiswaKknPersyaratan.getNilaiBoolean() != null) {
														cell.setCellValue(mahasiswaKknPersyaratan.getNilaiBoolean());
													}

												} else if (persyaratanKkn.getTipeDataInputan()
														.equals(PersyaratanKkn.PILIHAN_CUSTOM)) {
													cell = row.createCell(i);
													cell.setCellStyle(lockedNumericStyle);
													if (mahasiswaKknPersyaratan.getNilaiString() != null) {
														cell.setCellValue(mahasiswaKknPersyaratan.getNilaiString());
													}

												} else {
													cell = row.createCell(i);
													if (persyaratanKkn.getLabelInputan() == null
															|| persyaratanKkn.getLabelInputan().trim().isEmpty()) {
														cell.setCellValue(persyaratanKkn.getNama());
													} else {
														cell.setCellValue(persyaratanKkn.getLabelInputan());

													}
												}

												if (persyaratanKkn.getHarusMenyertakanLampiran()) {
													cell.setCellStyle(hlink_style);

													try {
														Session streamingSession = StreamingHibernateUtil.getInstance()
																.currentSession();
														int jumlah = ((Number) streamingSession
																.createCriteria(LampiranKknMahasiswa.class)
																.setProjection(Projections.rowCount())
																.add(Restrictions.eq("persyaratanKkn",
																		mahasiswaKknPersyaratan.getId()))
																.setMaxResults(1).uniqueResult()).intValue();

														Long ids = (Long) (streamingSession
																.createCriteria(LampiranKknMahasiswa.class)
																.setProjection(Projections.property("id"))
																.add(Restrictions.eq("persyaratanKkn",
																		mahasiswaKknPersyaratan.getId()))
																.setMaxResults(1).uniqueResult());

														String url = CommonMedia.getFile(ids,
																LampiranKknMahasiswa.class.getName());

														if (jumlah > 0) {
															XSSFHyperlink link = row.getSheet().getWorkbook()
																	.getCreationHelper()
																	.createHyperlink(Hyperlink.LINK_URL);
															link.setAddress(url);
															cell.setHyperlink(link);
														}
													} catch (Exception e) {
														StreamingHibernateUtil.getInstance().rollbackTransaction();
													}

													StreamingHibernateUtil.getInstance().closeSession();

												}
											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
											}
										}

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}
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
								ais.common.Common.tampilErrorJikaAdmin(e);
								label.setValue("-");
							}

													} finally {
								ais.database.hibernate.HibernateUtil.closeSession();
							}
						}
					}).start();

				} catch (Exception e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});

		return toolbarbutton;
	}

	public static void displayRow(Center center, final PenilaianAsesor penilaianAsesor) throws Exception {
		Tabbox tabbox = new Tabbox();
		tabbox.setParent(center);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabSoal = new MyTabConfig("Penilaian Asesor");
		tabSoal.setParent(tabs);

		final MyTabConfig tabPengajaran = new MyTabConfig("Rincian Pembimbing");
		tabPengajaran.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
		tabpanelUtama.setStyle("min-height: 300px;");
		tabpanelUtama.setParent(tabpanels);

		PenilaianAsesorHelper.formNilai(penilaianAsesor.getAsesemenPenilaian().getPegawai(),
				penilaianAsesor.getAsesemenPenilaian().getJenjang(),
				penilaianAsesor.getAsesemenPenilaian().getTahunAkademik(),
				penilaianAsesor.getAsesemenPenilaian().getSemester(), "SK Pembimbing",
				penilaianAsesor.getAsesemenPenilaian().getSpesifikasi(), new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub

					}
				}).setParent(tabpanelUtama);

		final Tabpanel jurusanTabpanel = new ais.ui.util.MyTabpanel();
		jurusanTabpanel.setParent(tabpanels);
		jurusanTabpanel.setWidth("100%");

		tabPengajaran.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				Dosen dosen = penilaianAsesor.getAsesemenPenilaian().getPegawai().getDosen();
				if (dosen != null && jurusanTabpanel.getChildren().isEmpty()) {

					Session session = HibernateUtil.currentSession();

					Criterion criterion = Restrictions.eq("kelompokKkn.dosen_pembimbing1", dosen);

					String[] cols = new String[] { "kelompokKkn.dosen_pembimbing2", "kelompokKkn.dosen_pembimbing3",
							"kelompokKkn.dosen_pembimbing4", "kelompokKkn.dosen_pembimbing5",
							"kelompokKkn.dosen_pembimbing6", "kelompokKkn.dosen_pembimbing7",
							"kelompokKkn.dosen_pembimbing8", "kelompokKkn.dosen_pembimbing9",
							"kelompokKkn.dosen_pembimbing10" };
					for (String c : cols) {
						criterion = Restrictions.or(criterion, Restrictions.eq(c, dosen));
					}

					List<KelompokKkn> kelompokKkns = session.createCriteria(MahasiswaDapatKelompokKkn.class)
							.add(Restrictions.eq("diterima", true)).createAlias("kelompokKkn", "kelompokKkn")
							.createAlias("kelompokKkn.kkn", "kkn").createAlias("mahasiswa", "mahasiswa")
							.createAlias("mahasiswa.jurusan", "jurusan")
							.add(Restrictions.eq("jurusan.jenjang",
									penilaianAsesor.getAsesemenPenilaian().getJenjang()))
							.setProjection(Projections.groupProperty("kelompokKkn")).add(criterion)
							.add(Restrictions.eq("kkn.tahunAkademik",
									penilaianAsesor.getAsesemenPenilaian().getTahunAkademik()))
							.add(Restrictions.eq("kkn.semester", penilaianAsesor.getAsesemenPenilaian().getSemester()))
							.list();

					Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
					borderlayout.setParent(jurusanTabpanel);
					Center center = new Center();
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);

					MyGrid grid = new MyGrid();
					grid.setMold("paging");
					grid.setPageSize(50);
					grid.getPagingChild().setMold("os");
					grid.setParent(center);
					Rows rows = new Rows();
					rows.setParent(grid);
					for (KelompokKkn kelompokKkn : kelompokKkns) {
						MyFormRow row = new MyFormRow();
						row.setValign("top");
						row.setParent(rows);

						KelompokKknAction.displayRow(row, kelompokKkn,
								penilaianAsesor.getAsesemenPenilaian().getPegawai(), false);
					}

				}
			}
		});
	}

	public static void displayRow(Row arg0, final KelompokKkn kelompokKkn, final Pegawai pegawai, final Boolean ases)
			throws Exception {

		final Vbox vboxKeterangan = new Vbox();
		final EventListener keteranganEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(vboxKeterangan);

				KelompokKknAction.tampilkanInfoDosen(kelompokKkn, ases, true).setParent(vboxKeterangan);

			}
		};

		final MyDetail detail = new MyDetail();
		detail.setParent(arg0);
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(detail);
				if (detail.isOpen()) {

					Set<Long> treeMap = new HashSet<Long>();
					if (kelompokKkn.getDosen_pembimbing1() != null) {
						treeMap.add(kelompokKkn.getDosen_pembimbing1().getId());
					}
					if (kelompokKkn.getDosen_pembimbing2() != null) {
						treeMap.add(kelompokKkn.getDosen_pembimbing2().getId());
					}
					if (kelompokKkn.getDosen_pembimbing3() != null) {
						treeMap.add(kelompokKkn.getDosen_pembimbing3().getId());
					}
					if (kelompokKkn.getDosen_pembimbing4() != null) {
						treeMap.add(kelompokKkn.getDosen_pembimbing4().getId());
					}
					if (kelompokKkn.getDosen_pembimbing5() != null) {
						treeMap.add(kelompokKkn.getDosen_pembimbing5().getId());
					}
					if (kelompokKkn.getDosen_pembimbing6() != null) {
						treeMap.add(kelompokKkn.getDosen_pembimbing6().getId());
					}
					if (kelompokKkn.getDosen_pembimbing7() != null) {
						treeMap.add(kelompokKkn.getDosen_pembimbing7().getId());
					}
					if (kelompokKkn.getDosen_pembimbing8() != null) {
						treeMap.add(kelompokKkn.getDosen_pembimbing8().getId());
					}
					if (kelompokKkn.getDosen_pembimbing9() != null) {
						treeMap.add(kelompokKkn.getDosen_pembimbing9().getId());
					}
					if (kelompokKkn.getDosen_pembimbing10() != null) {
						treeMap.add(kelompokKkn.getDosen_pembimbing10().getId());
					}

					if (ases && pegawai != null && pegawai.getDosen() != null
							&& treeMap.contains(pegawai.getDosen().getId())) {
						Tabbox tabbox = new Tabbox();
						tabbox.setParent(detail);
						tabbox.setHeight("100%");
						tabbox.setWidth("100%");

						Tabs tabs = new Tabs();
						tabs.setParent(tabbox);

						final MyTabConfig tabSoal = new MyTabConfig("Penilaian Asesor");
						tabSoal.setParent(tabs);

						final MyTabConfig tabPengajaran = new MyTabConfig("Rincian Bimbingan");
						tabPengajaran.setParent(tabs);

						Tabpanels tabpanels = new Tabpanels();
						tabpanels.setParent(tabbox);

						Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
						tabpanelUtama.setStyle("min-height: 300px;");
						tabpanelUtama.setParent(tabpanels);

						PenilaianAsesorHelper
								.formNilai(pegawai, null,
										kelompokKkn.getKkn() == null ? "" : kelompokKkn.getKkn().getTahunAkademik(),
										kelompokKkn.getKkn() == null ? "" : kelompokKkn.getKkn().getSemester(),
										"SK pembimbing KKN kelompok \"" + kelompokKkn.getNama() + "\"",
										PenilaianAsesor.PEMBIMBING_KKN, keteranganEventListener)
								.setParent(tabpanelUtama);

						final Tabpanel jurusanTabpanel = new ais.ui.util.MyTabpanel();
						jurusanTabpanel.setParent(tabpanels);
						jurusanTabpanel.setStyle("min-height: 700px;");
						jurusanTabpanel.setWidth("100%");

						tabPengajaran.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (jurusanTabpanel.getChildren().isEmpty()) {

									ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
									groupbox.setStyle("min-height: 200px;");
									aktifitasKknHelper.initDetail(kelompokKkn, groupbox);
									jurusanTabpanel.appendChild(groupbox);

								}
							}
						});
					} else {
						ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
						groupbox.setStyle("min-height: 200px;");
						aktifitasKknHelper.initDetail(kelompokKkn, groupbox);
						detail.appendChild(groupbox);
					}
				}
			}
		};
		detail.addEventListener("onOpen", eventListener);
		if (ases) {
			detail.setOpen(true);
			eventListener.onEvent(null);
		}

		Vbox a;
		(a = RevisiHelper.createNewRevisi(KelompokKkn.class, kelompokKkn,
				kelompokKkn.getNama_kelompok() + " ("
						+ (kelompokKkn.getKkn() == null ? "" : kelompokKkn.getKkn().getNama_kelompok()) + ")"))
				.setParent(arg0);

		if (kelompokKkn.getNoSk() != null && !kelompokKkn.getNoSk().trim().isEmpty()) {
			a.appendChild(new MyLabelAgakKecil("No. SK : " + kelompokKkn.getNoSk()));
		}
		if (kelompokKkn.getTglSk() != null) {
			a.appendChild(new MyLabelAgakKecil("Tgl. SK : " + Common.dateFormat4.get().format(kelompokKkn.getTglSk())));
		}

		Vbox myvbox = new Vbox();
		myvbox.setParent(a);

		Hbox hbox = new Hbox();
		ais.ui.util.MenuAksiBaris.pasang(hbox);
		hbox.setParent(myvbox);
		LampiranLain.createDownloadUploadFileLain(hbox, kelompokKkn.getId(), KelompokKkn.class.getName(), "Lampiran SK",
				false, null, null, false, false, false, false);

		Vbox vbox = new Vbox();
		vbox.setParent(arg0);
		new Label((kelompokKkn.getTanggal_mulai() == null ? ""
				: Common.dateFormat4.get().format(kelompokKkn.getTanggal_mulai()))
				+ (kelompokKkn.getTanggal_selesai() == null ? ""
						: " s.d " + Common.dateFormat4.get().format(kelompokKkn.getTanggal_selesai())))
				.setParent(vbox);
		new Label(kelompokKkn.getKkn() == null ? ""
				: (kelompokKkn.getKkn().getTahunAkademik() + " / " + kelompokKkn.getKkn().getSemester()))
				.setParent(vbox);

		vboxKeterangan.setParent(arg0);
		keteranganEventListener.onEvent(null);

		new Label(kelompokKkn.getAlamat()).setParent(arg0);

		new Label(kelompokKkn.getSertifikat() == null ? "-" : kelompokKkn.getSertifikat().getNama()).setParent(arg0);

		int count = ((Number) HibernateUtil.currentSession().createCriteria(MahasiswaDapatKelompokKkn.class)
				.add(Restrictions.eq("diterima", true)).add(Restrictions.eq("kelompokKkn", kelompokKkn))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();

		new Label((kelompokKkn.getMahasiswaBisaMemilih() ? "Ya / " + kelompokKkn.getKuota() : "Tidak") + " / " + count
				+ " mhs").setParent(arg0);
	}

	class KelompokKknRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KelompokKkn kelompokKkn = (KelompokKkn) arg1;
			Dosen dosenPemimbing = (Dosen) searchdosen1.getAttribute("myValue");
			KelompokKknAction.displayRow(arg0, kelompokKkn, dosenPemimbing == null ? null : new Pegawai(dosenPemimbing),
					ases);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
			// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(kelompokKkn);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(button);

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
											Long jumlahPertemuan = (Long) HibernateUtil.currentSession()
													.createCriteria(Pertemuan.class)
													.add(Restrictions.eq("kelompokKkn", kelompokKkn))
													.setProjection(Projections.rowCount()).uniqueResult();
											if (jumlahPertemuan != null && jumlahPertemuan.longValue() > 0L) {
												MyMessageboxConfig.show(
														"Kelompok KKN ini masih dipakai oleh "
																+ jumlahPertemuan
																+ " data pertemuan. Hapus atau pindahkan pertemuan terkait terlebih dahulu.",
														"Peringatan", MyMessageboxConfig.OK,
														MyMessageboxConfig.EXCLAMATION);
												return;
											}

											Common.refreshDelete(kelompokKkn);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Mohon maaf, data kelompok KKN ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Langkah yang dapat dilakukan: (1) pastikan tidak ada mahasiswa atau data lain yang masih terhubung dengan kelompok KKN ini; (2) hapus terlebih dahulu data yang berelasi tersebut; (3) ulangi proses penghapusan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis. Detail error: "
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			aksiButtons.add(button);

			Vbox aksiBox = ais.ui.util.UIHelper.buatBarisAksi(vbox, 3, aksiButtons);
			aksiBox.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.ambilDosen() == null);

			Hbox myHbox = new Hbox();
			/* UBAH 21-08-2026: status feeder dahulu menumpang di sel AKSI yang hanya selebar
			 * tombol kebab, sehingga labelnya terpenggal menurun satu kata per baris. Kini
			 * ditempelkan di bawah sel identitas baris. Bila sel itu tidak ditemukan,
			 * wadah lama tetap dipakai agar keterangannya tidak hilang. */
			org.zkoss.zul.Vbox wadahFeeder = ais.ui.util.UIHelper.selIdentitas(arg0);
			if (wadahFeeder == null) {
				wadahFeeder = vbox;
			}
			myHbox.setParent(wadahFeeder);

			if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
					&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {

				if (kelompokKkn.getFeeder() != null && !kelompokKkn.getFeeder().trim().isEmpty()) {
					myHbox.appendChild(new Image("/img/svg/check2-circle.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder valid"));
				} else {
					myHbox.appendChild(new Image("/img/svg/warning-outline.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder blm valid"));
				}

				MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Krm ke feeder",
						"/img/Finance-Invoice-icon.png");
				buttonTagihan.setStyle("font-size:10px; white-space:nowrap;");
				buttonTagihan.setParent(wadahFeeder);
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
														myLabelProsesDetail.setValue("Mengirim data " + kelompokKkn);

														feederImporter.aktivitasMahasiswaKkn(kelompokKkn, errorLog);

														// FIX "gagal diam-diam": sebelumnya exception di sini hanya dicatat ke log admin lalu progres diset "" (=SUKSES palsu) di luar try.
														myLabelProsesDetail.setValue("");
													} catch (Exception e) {
														ais.common.Common.tampilErrorJikaAdmin(e);
														myLabelProsesDetail.setValue(
																"Error: " + ais.common.PesanFormalHelper.pesanGagalException(
																		"pengiriman data Kelompok KKN (" + kelompokKkn + ") ke Neo Feeder",
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
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new KelompokKkn());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public Combobox jumlahDosen;
	private Combobox lokasi;
	private MyDoublebox jarak;

	private void init(KelompokKkn kelompokKkn) throws Exception {
		this.kelompokKkn = kelompokKkn;
		Common.clear(addWindow);
		addWindow.setTitle("Kelompok Kkn");
		addWindow.setWidth("550px");
		addWindow.setHeight("95%");
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kelompok *"));
		row.appendChild(nama_kelompok = new Textbox(
				kelompokKkn.getNama_kelompok() == null ? "" : kelompokKkn.getNama_kelompok()));
		nama_kelompok.setWidth("90%");
		// nama_kelompok.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai"));
		row.appendChild(
				tanggal_mulai = new MyDatebox(kelompokKkn.getTanggal_mulai() == null ? ais.ui.util.WaktuUtil.getDate()
						: kelompokKkn.getTanggal_mulai()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Selesai"));
		row.appendChild(tanggal_selesai = new MyDatebox(
				kelompokKkn.getTanggal_selesai() == null ? ais.ui.util.WaktuUtil.getDate()
						: kelompokKkn.getTanggal_selesai()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. SK"));
		row.appendChild(noSk = new Textbox(kelompokKkn.getNoSk()));
		noSk.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal SK"));
		row.appendChild(tglSk = new MyDatebox(kelompokKkn.getTglSk()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lampiran SK"));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, kelompokKkn.getId(), KelompokKkn.class.getName(), "Lampiran SK",
				false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		Common.initKeterangan(rows, "Jika file lampiran SK lebih dari satu file, zip dulu semua file tersebut");

		row = new MyFormRow();
		row.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(
				mahasiswaBisaMemilih = new MyCheckboxConfig("Mahasiswa boleh memilih sendiri pilihan kelompok"));
		mahasiswaBisaMemilih.setChecked(kelompokKkn.getMahasiswaBisaMemilih());

		row = new MyFormRow();
		row.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kuota"));
		row.appendChild(kuota = new MyIntbox(kelompokKkn.getKuota()));

		Tbmuser tbmuser = Common.getCurrentUser();

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("KKN"));
		row.appendChild(kkn = new Combobox());
		Common.insertCombo(kkn, "nama", Kkn.class);
		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			MahasiswaDaftarKkn mahasiswaDaftarKkn = (MahasiswaDaftarKkn) HibernateUtil.currentSession()
					.createCriteria(MahasiswaDaftarKkn.class).add(Restrictions.eq("mahasiswa", tbmuser.getMahasiswa()))
					.addOrder(Order.desc("id")).uniqueResult();

			if (mahasiswaDaftarKkn == null) {
				Common.initKeterangan(rows, "Anda belum terdaftar");
			} else {
				Common.selectComboItem(true, kkn, mahasiswaDaftarKkn.getKkn());
			}
			kkn.setDisabled(true);
		} else {

			Common.selectComboItem(kkn, kelompokKkn.getKkn());
		}
		kkn.setWidth("90%");
		kkn.setReadonly(true);

		jumlahDosen = new Combobox();
		for (int i = 1; i <= 10; i++) {
			MyComboitemConfig comboitem = new MyComboitemConfig();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			jumlahDosen.appendChild(comboitem);
		}

		int jml = kelompokKkn.getJumlahDosen();

		MyFormRow rowJumlahDosen = new MyFormRow();
		if (Common.bolehKonfigurasi("mahasiswa_tidak_boleh_memilih_dosen_pembimbing_sendiri"))
			rowJumlahDosen.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
		rowJumlahDosen.setParent(rows);
		rowJumlahDosen.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Dosen Pembimbing"));
		rowJumlahDosen.appendChild(jumlahDosen);
		Common.selectComboItem(jumlahDosen, jml < 1 ? 1 : jml);
		jumlahDosen.setWidth("90%");
		jumlahDosen.setReadonly(true);

		row = new MyFormRow();
		if (Common.bolehKonfigurasi("mahasiswa_tidak_boleh_memilih_dosen_pembimbing_sendiri"))
			row.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(("Dosen Pembimbing I")));
		row.appendChild(dosen_pembimbing1 = new AmbilDataDosenBanbox());
		dosen_pembimbing1.setValue(
				kelompokKkn.getDosen_pembimbing1() == null ? "" : (kelompokKkn.getDosen_pembimbing1().getNama()));
		if (tbmuser != null && tbmuser.ambilDosen() != null
				&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
			Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
			dosen_pembimbing1.setValue(dosen.getNama());
			dosen_pembimbing1.setAttribute("myValue", dosen);
			dosen_pembimbing1.setDisabled(true);
		}

		dosen_pembimbing1.setAttribute("myValue", kelompokKkn.getDosen_pembimbing1());
		dosen_pembimbing1.setWidth("90%");

		row = new MyFormRow();
		if (Common.bolehKonfigurasi("mahasiswa_tidak_boleh_memilih_dosen_pembimbing_sendiri"))
			row.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(("Dosen Pembimbing II")));
		row.appendChild(dosen_pembimbing2 = new AmbilDataDosenBanbox());
		dosen_pembimbing2.setValue(
				kelompokKkn.getDosen_pembimbing2() == null ? "" : (kelompokKkn.getDosen_pembimbing2().getNama()));

		dosen_pembimbing2.setAttribute("myValue", kelompokKkn.getDosen_pembimbing2());
		dosen_pembimbing2.setWidth("90%");

		row = new MyFormRow();
		if (Common.bolehKonfigurasi("mahasiswa_tidak_boleh_memilih_dosen_pembimbing_sendiri"))
			row.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen Pembimbing III"));
		row.appendChild(dosen_pembimbing3 = new AmbilDataDosenBanbox());
		dosen_pembimbing3.setValue(
				kelompokKkn.getDosen_pembimbing3() == null ? "" : (kelompokKkn.getDosen_pembimbing3().getNama()));

		dosen_pembimbing3.setAttribute("myValue", kelompokKkn.getDosen_pembimbing3());
		dosen_pembimbing3.setWidth("90%");

		row = new MyFormRow();
		if (Common.bolehKonfigurasi("mahasiswa_tidak_boleh_memilih_dosen_pembimbing_sendiri"))
			row.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen Pembimbing IV"));
		row.appendChild(dosen_pembimbing4 = new AmbilDataDosenBanbox());
		dosen_pembimbing4.setValue(
				kelompokKkn.getDosen_pembimbing4() == null ? "" : (kelompokKkn.getDosen_pembimbing4().getNama()));

		dosen_pembimbing4.setAttribute("myValue", kelompokKkn.getDosen_pembimbing4());
		dosen_pembimbing4.setWidth("90%");

		row = new MyFormRow();
		if (Common.bolehKonfigurasi("mahasiswa_tidak_boleh_memilih_dosen_pembimbing_sendiri"))
			row.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen Pembimbing V"));
		row.appendChild(dosen_pembimbing5 = new AmbilDataDosenBanbox());
		dosen_pembimbing5.setValue(
				kelompokKkn.getDosen_pembimbing5() == null ? "" : (kelompokKkn.getDosen_pembimbing5().getNama()));

		dosen_pembimbing5.setAttribute("myValue", kelompokKkn.getDosen_pembimbing5());
		dosen_pembimbing5.setWidth("90%");

		row = new MyFormRow();
		if (Common.bolehKonfigurasi("mahasiswa_tidak_boleh_memilih_dosen_pembimbing_sendiri"))
			row.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen Pembimbing VI"));
		row.appendChild(dosen_pembimbing6 = new AmbilDataDosenBanbox());
		dosen_pembimbing6.setValue(
				kelompokKkn.getDosen_pembimbing6() == null ? "" : (kelompokKkn.getDosen_pembimbing6().getNama()));
		dosen_pembimbing6.setAttribute("myValue", kelompokKkn.getDosen_pembimbing6());
		dosen_pembimbing6.setWidth("90%");

		row = new MyFormRow();
		if (Common.bolehKonfigurasi("mahasiswa_tidak_boleh_memilih_dosen_pembimbing_sendiri"))
			row.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen Pembimbing VII"));
		row.appendChild(dosen_pembimbing7 = new AmbilDataDosenBanbox());
		dosen_pembimbing7.setValue(
				kelompokKkn.getDosen_pembimbing7() == null ? "" : (kelompokKkn.getDosen_pembimbing7().getNama()));
		dosen_pembimbing7.setAttribute("myValue", kelompokKkn.getDosen_pembimbing7());
		dosen_pembimbing7.setWidth("90%");

		row = new MyFormRow();
		if (Common.bolehKonfigurasi("mahasiswa_tidak_boleh_memilih_dosen_pembimbing_sendiri"))
			row.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen Pembimbing VIII"));
		row.appendChild(dosen_pembimbing8 = new AmbilDataDosenBanbox());
		dosen_pembimbing8.setValue(
				kelompokKkn.getDosen_pembimbing8() == null ? "" : (kelompokKkn.getDosen_pembimbing8().getNama()));
		dosen_pembimbing8.setAttribute("myValue", kelompokKkn.getDosen_pembimbing8());
		dosen_pembimbing8.setWidth("90%");

		row = new MyFormRow();
		if (Common.bolehKonfigurasi("mahasiswa_tidak_boleh_memilih_dosen_pembimbing_sendiri"))
			row.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen Pembimbing IX"));
		row.appendChild(dosen_pembimbing9 = new AmbilDataDosenBanbox());
		dosen_pembimbing9.setValue(
				kelompokKkn.getDosen_pembimbing9() == null ? "" : (kelompokKkn.getDosen_pembimbing9().getNama()));
		dosen_pembimbing9.setAttribute("myValue", kelompokKkn.getDosen_pembimbing9());
		dosen_pembimbing9.setWidth("90%");

		row = new MyFormRow();
		if (Common.bolehKonfigurasi("mahasiswa_tidak_boleh_memilih_dosen_pembimbing_sendiri"))
			row.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen Pembimbing X"));
		row.appendChild(dosen_pembimbing10 = new AmbilDataDosenBanbox());
		dosen_pembimbing10.setValue(
				kelompokKkn.getDosen_pembimbing10() == null ? "" : (kelompokKkn.getDosen_pembimbing10().getNama()));
		dosen_pembimbing10.setAttribute("myValue", kelompokKkn.getDosen_pembimbing10());
		dosen_pembimbing10.setWidth("100%");

		EventListener jumlahDosenEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Integer jml = (Integer) (jumlahDosen.getSelectedItem() == null ? 1
						: jumlahDosen.getSelectedItem().getValue());

				dosen_pembimbing1.getParent().setVisible(jml >= 1);

				dosen_pembimbing2.getParent().setVisible(jml >= 2);
				dosen_pembimbing3.getParent().setVisible(jml >= 3);
				dosen_pembimbing4.getParent().setVisible(jml >= 4);
				dosen_pembimbing5.getParent().setVisible(jml >= 5);
				dosen_pembimbing6.getParent().setVisible(jml >= 6);
				dosen_pembimbing7.getParent().setVisible(jml >= 7);
				dosen_pembimbing8.getParent().setVisible(jml >= 8);
				dosen_pembimbing9.getParent().setVisible(jml >= 9);
				dosen_pembimbing10.getParent().setVisible(jml >= 10);

			}
		};

		jumlahDosenEventListener.onEvent(null);
		jumlahDosen.addEventListener("onChange", jumlahDosenEventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alamat KKN"));
		row.appendChild(alamat = new Textbox(kelompokKkn.getAlamat() == null ? "" : kelompokKkn.getAlamat()));
		alamat.setWidth("90%");
		alamat.setRows(3);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi Pertemuan"));
		row.appendChild(lokasi = new Combobox());
		lokasi.setWidth("90%");
		Common.insertComboDanSemua(lokasi, new String[] { "nama", "lat", "lng" }, "alamat", Lokasi.class,
				"Semua Lokasi", Restrictions.eq("aktif", true));
		Common.selectComboItem(lokasi, kelompokKkn.getLokasi());

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig("Radius posisi kehadiran titik dari lokasi (km)"));
		row.appendChild(jarak = new MyDoublebox(kelompokKkn.getJarak()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ketarangan"));
		row.appendChild(
				keterangan = new Textbox(kelompokKkn.getKeterangan() == null ? "" : kelompokKkn.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sertifikat"));
		row.appendChild(sertifikat = new Combobox());
		Common.insertComboDanSemua(sertifikat, new String[] { "nama" }, "keterangan", Sertifikat.class,
				"== Tanpa Sertifikat ==");
		Common.selectComboItem(sertifikat, kelompokKkn.getSertifikat());
		sertifikat.setWidth("90%");
		sertifikat.setReadonly(true);

		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			sertifikat.setDisabled(true);
		}

		row = new MyFormRow();
		row.setVisible(Common.getApakahAdminBolehAksesFeeder());
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Feeder"));
		row.appendChild(feeder = new Textbox(kelompokKkn.getFeeder()));
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
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);
					// loadKurikulum();
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {

		if (nama_kelompok.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, nama kelompok KKN belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Kelompok dengan nama kelompok KKN yang akan disimpan; (2) pastikan nama kelompok tidak dikosongkan; (3) ulangi proses penyimpanan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (kkn.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, data KKN belum dipilih. Langkah yang dapat dilakukan: (1) pilih data KKN dari daftar yang tersedia pada kolom KKN; (2) pastikan KKN telah dipilih sebelum menyimpan data kelompok; (3) ulangi proses penyimpanan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (kelompokKkn.getId() != null) {
			kelompokKkn = (KelompokKkn) session.load(KelompokKkn.class, kelompokKkn.getId());
		}

		kelompokKkn.setMahasiswaBisaMemilih(mahasiswaBisaMemilih.isChecked());
		kelompokKkn.setKuota(kuota.getValue());
		kelompokKkn.setAlamat(alamat.getValue());
		kelompokKkn.setDosen_pembimbing1((Dosen) dosen_pembimbing1.getAttribute("myValue"));
		kelompokKkn.setDosen_pembimbing2((Dosen) dosen_pembimbing2.getAttribute("myValue"));

		kelompokKkn.setDosen_pembimbing3((Dosen) dosen_pembimbing3.getAttribute("myValue"));
		kelompokKkn.setDosen_pembimbing4((Dosen) dosen_pembimbing4.getAttribute("myValue"));
		kelompokKkn.setDosen_pembimbing5((Dosen) dosen_pembimbing5.getAttribute("myValue"));

		kelompokKkn.setDosen_pembimbing6((Dosen) dosen_pembimbing6.getAttribute("myValue"));
		kelompokKkn.setDosen_pembimbing7((Dosen) dosen_pembimbing7.getAttribute("myValue"));
		kelompokKkn.setDosen_pembimbing8((Dosen) dosen_pembimbing8.getAttribute("myValue"));
		kelompokKkn.setDosen_pembimbing9((Dosen) dosen_pembimbing9.getAttribute("myValue"));
		kelompokKkn.setDosen_pembimbing10((Dosen) dosen_pembimbing10.getAttribute("myValue"));

		kelompokKkn.setKkn((Kkn) (kkn.getSelectedItem() == null ? null : kkn.getSelectedItem().getValue()));

		kelompokKkn.setKeterangan(keterangan.getValue());
		kelompokKkn.setNama_kelompok(nama_kelompok.getValue());
		kelompokKkn.setTanggal_mulai(tanggal_mulai.getValue());
		kelompokKkn.setTanggal_selesai(tanggal_selesai.getValue());

		kelompokKkn.setSertifikat(
				(Sertifikat) (sertifikat.getSelectedItem() == null ? null : sertifikat.getSelectedItem().getValue()));
		kelompokKkn.setNoSk(noSk.getValue());
		kelompokKkn.setTglSk(tglSk.getValue());
		kelompokKkn.setFeeder(feeder.getValue().trim());
		kelompokKkn.setLokasi((Lokasi) (lokasi == null || lokasi.getSelectedItem() == null ? null
				: lokasi.getSelectedItem().getValue()));
		kelompokKkn.setJarak(jarak == null ? null : jarak.getValue());
		Common.refreshSaveOrUpdate(session, kelompokKkn);

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			MahasiswaDapatKelompokKkn mahasiswaDapatKelompokKkn = (MahasiswaDapatKelompokKkn) session
					.createCriteria(MahasiswaDapatKelompokKkn.class).add(Restrictions.eq("kelompokKkn", kelompokKkn))
					.add(Restrictions.eq("mahasiswa", tbmuser.getMahasiswa())).setMaxResults(1).uniqueResult();
			if (mahasiswaDapatKelompokKkn == null) {
				mahasiswaDapatKelompokKkn = new MahasiswaDapatKelompokKkn();
				mahasiswaDapatKelompokKkn.setKelompokKkn(kelompokKkn);
				mahasiswaDapatKelompokKkn.setMahasiswa(tbmuser.getMahasiswa());
				Common.refreshSaveOrUpdate(session, mahasiswaDapatKelompokKkn);
			}
		}

		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainMahasiswa);
				lainMahasiswa.setRef(kelompokKkn.getId());

				session.getTransaction().begin();
				session.update(lainMahasiswa);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		// Guard: komponen autowire bisa null saat dipanggil dari konteks non-UI
		// (background download CommonDownloadUpload, banbox renderer, varian ZUL).
		Mahasiswa mahasiswa = searchmahasiswa == null ? null : (Mahasiswa) searchmahasiswa.getAttribute("mahasiswa");

		Dosen dosenPemimbing = searchdosen1 == null ? null : (Dosen) searchdosen1.getAttribute("myValue");

		Criterion criterion = Restrictions.eq("dosen_pembimbing1", dosenPemimbing);
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen_pembimbing2", dosenPemimbing));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen_pembimbing3", dosenPemimbing));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen_pembimbing4", dosenPemimbing));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen_pembimbing5", dosenPemimbing));

		criterion = Restrictions.or(criterion, Restrictions.eq("dosen_pembimbing6", dosenPemimbing));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen_pembimbing7", dosenPemimbing));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen_pembimbing8", dosenPemimbing));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen_pembimbing9", dosenPemimbing));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen_pembimbing10", dosenPemimbing));

		if (mahasiswa != null) {
			Criteria criteria = session.createCriteria(MahasiswaDapatKelompokKkn.class)
					.add(Restrictions.eq("diterima", true)).add(Restrictions.eq("mahasiswa", mahasiswa))
					.setProjection(Projections.property("kelompokKkn")).createCriteria("kelompokKkn")
					.add(dosenPemimbing == null ? Restrictions.sqlRestriction("1=1") : criterion)
					.add(searchkkn == null || searchkkn.getSelectedItem() == null || searchkkn.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("kkn", searchkkn.getSelectedItem().getValue()));
			if (order)
				criteria.addOrder(Order.desc("nama_kelompok")).addOrder(Order.desc("tanggal_mulai"));
			criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
					: Restrictions.ilike("nama_kelompok", searchnama.getValue().trim(), MatchMode.ANYWHERE))
					.add(searchketerangan == null || searchketerangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("keterangan", searchketerangan.getValue().trim(), MatchMode.ANYWHERE))
					.add(searchBelumMasukFeeder != null && searchBelumMasukFeeder.isChecked()
							? Restrictions.or(Restrictions.isNull("feeder"), Restrictions.eq("feeder", ""))

							: Restrictions.sqlRestriction("true"))

					.add(searchMasukFeeder != null && searchMasukFeeder.isChecked()
							? Restrictions.or(Restrictions.isNotNull("feeder"), Restrictions.ne("feeder", ""))

							: Restrictions.sqlRestriction("true"));
			return criteria;
		} else {
			Criteria criteria = session.createCriteria(KelompokKkn.class)
					.add(dosenPemimbing == null ? Restrictions.sqlRestriction("1=1") : criterion)
					.add(searchkkn == null || searchkkn.getSelectedItem() == null || searchkkn.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("kkn", searchkkn.getSelectedItem().getValue()));
			if (order)
				criteria.addOrder(Order.desc("nama_kelompok")).addOrder(Order.desc("tanggal_mulai"));

			criteria

					.add(searchBelumMasukFeeder != null && searchBelumMasukFeeder.isChecked()
							? Restrictions.or(Restrictions.isNull("feeder"), Restrictions.eq("feeder", ""))

							: Restrictions.sqlRestriction("true"))

					.add(searchMasukFeeder != null && searchMasukFeeder.isChecked()
							? Restrictions.or(Restrictions.isNotNull("feeder"), Restrictions.ne("feeder", ""))

							: Restrictions.sqlRestriction("true"))

					.add(searchnama == null || searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("nama_kelompok", searchnama.getValue().trim(), MatchMode.ANYWHERE))
					.add(searchketerangan == null || searchketerangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("keterangan", searchketerangan.getValue().trim(), MatchMode.ANYWHERE));
			;
			return criteria;
		}
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KelompokKkn> kelompokKkn = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(kelompokKkn);
		grid.setRowRenderer(new KelompokKknRenderer());
		grid.setModelCheckMobile(strset);

	}

	public static DspaceInformation getDspaceKkn(String cookie, KelompokKkn kelompokKkn, Jurusan jurusan)
			throws Exception {

		String description = "KKN untuk " + Common.getBahasaConfig("Jurusan") + " " + jurusan.getNama();

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", "KKN");
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", description);
		jsonPost.put("shortDescription", "KKN " + jurusan.getNama() + " Repository");
		jsonPost.put("sidebarText", description);

		Konfigurasi uuidKonfigurasi = Common.getKonfigurasi("dspace_label_collection_kkn_" + jurusan.getId(), "");
		DspaceInformation dsParent = DspaceInformation.dspaceProcess(cookie, uuidKonfigurasi, jsonPost.toString(),
				false, "communities",
				"communities/" + JurusanAction.getDspace(cookie, jurusan, false) + "/communities");

		description = kelompokKkn.getKkn().getNama() + " pada " + Common.getBahasaConfig("Jurusan") + " "
				+ jurusan.getNama();

		jsonPost = new JSONObject();
		jsonPost.put("name", kelompokKkn.getKkn().getNama());
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", description);
		jsonPost.put("shortDescription", kelompokKkn.getKkn().getNama() + " Repository");
		jsonPost.put("sidebarText", description);

		uuidKonfigurasi = Common.getKonfigurasi("dspace_label_collection_kelompok_kkn_" + jurusan.getId(), "");
		return DspaceInformation.dspaceProcess(cookie, uuidKonfigurasi, jsonPost.toString(), false, "communities",
				"communities/" + dsParent + "/communities");

	}

	public static DspaceInformation getDspace(String cookie, KelompokKkn kelompokKkn, Jurusan jurusan)
			throws Exception {

		String info = kelompokKkn.getKkn().getNama() + " - " + kelompokKkn.getNama() + " - "
				+ kelompokKkn.getKkn().getTahunAkademik() + "/" + kelompokKkn.getKkn().getSemester();
		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", info);
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", "Berisi semua artefak " + info);
		jsonPost.put("shortDescription", "Artefak " + info);
		jsonPost.put("sidebarText", "Artefak " + info);
		return DspaceInformation.dspaceProcess(cookie, kelompokKkn, jsonPost.toString(), true, "collections",
				"communities/" + getDspaceKkn(cookie, kelompokKkn, jurusan) + "/collections");
	}

}
