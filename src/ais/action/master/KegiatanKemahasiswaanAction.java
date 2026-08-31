package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.dashboard.admin.DashboardRekapKegiatanKemahasiswaanBerdasarDetailKelompok;
import ais.action.master.dashboard.admin.DashboardRekapKegiatanKemahasiswaanBerdasarJabatan;
import ais.action.master.dashboard.admin.DashboardRekapKegiatanKemahasiswaanBerdasarKelompok;
import ais.action.master.dashboard.admin.DashboardRekapKegiatanKemahasiswaanBerdasarSkala;
import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.feeder.util.FeederExporter;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.KegiatanKemahasiswaanPunyaMahasiswaHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.report.format1.akademik.LaporanPendidikanLingkunganKampus;
import ais.action.report.format1.akademik.LaporanPerKegiatanMahasiswa;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.UploadReportHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.DetailKelompokKegiatanKemahasiswaan;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.JabatanKegiatanKemahasiswaan;
import ais.database.model.JenisAktfitasMahasiswa;
import ais.database.model.Jurusan;
import ais.database.model.KegiatanKemahasiswaan;
import ais.database.model.KegiatanKemahasiswaanPunyaMahasiswa;
import ais.database.model.KelompokKegiatanKemahasiswaan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.PrestasiMahasiswa;
import ais.database.model.Sertifikat;
import ais.database.model.SkalaKegiatanKemahasiswaan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyLabelKecilSekali;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk kegiatan kemahasiswaan. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Combobox searchjurusan}, {@code Combobox
 * searchfakultas}, {@code Combobox searchstatus}, {@code Textbox searchnamamhs}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian
 * ({@code onUploadData()}, {@code onSearchDefault()}); validasi/perhitungan ({@code checkNamaAgama()}); mutasi
 * data ({@code onSave()}, {@code setujuiPesertaTerfilter()}); operasi domain lain ({@code
 * onKelompokKegiatanKemahasiswaan()}, {@code onForm()}, {@code onRekapBerdasarJabatan()}, {@code
 * onRekapBerdasarSkala()}, {@code onRekapBerdasarKelompok()}, {@code loginSebagaiPesertaAtauPengajar()}). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class KegiatanKemahasiswaanAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchjurusan;
	private Combobox searchfakultas;
	private Combobox searchstatus;
	protected Textbox searchnamamhs;
	protected Textbox searchnim;
	protected AmbilDataDosenBanbox searchdosen;

	private Textbox nama;
	private MyDatebox mulai;
	private MyDatebox sampai;
	private Combobox jurusan; 
	private Combobox fakultas;

	private Combobox detailKelompokKegiatanKemahasiswaan;
	private Combobox kelompokKegiatanKemahasiswaan;

	private Textbox keterangan;

	// private boolean edit = false;
	// private boolean delete = false;

	private KegiatanKemahasiswaan kegiatanKemahasiswaan;
	private MyToolbarbuttonConfig add;

	private Tabpanel kelompokKegiatanKemahasiswaanTab;
	protected LampiranLain lainMahasiswa;
	private Tbmuser tbmuser;
	private EventListener eventListener;

	public void onKelompokKegiatanKemahasiswaan(Event event) {
		if (kelompokKegiatanKemahasiswaanTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(kelompokKegiatanKemahasiswaanTab);
			MyInclude iframe = new MyInclude("/pages/master/kelompok_kegiatan_kemahasiswaan.zul");
			iframe.setParent(window);
		}
	}

	private Tab rekap;

	private Tabpanel formTab;

	public void onForm(Event event) {
		if (formTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(formTab);
			MyInclude iframe = new MyInclude("/pages/master/formulir_kegiatan.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel rekapBerdasarJabatanTab;

	public void onRekapBerdasarJabatan(Event event) {
		if (rekapBerdasarJabatanTab.getChildren().size() == 0) {
			DashboardRekapKegiatanKemahasiswaanBerdasarJabatan window = new DashboardRekapKegiatanKemahasiswaanBerdasarJabatan();
			ais.ui.util.BaseDasbordPortal.mountWrapped(window, rekapBerdasarJabatanTab,
				"Rekap per Jabatan", "Sebaran kegiatan kemahasiswaan berdasarkan jabatan mahasiswa dalam organisasi.");
		}
	}

	private Tabpanel rekapBerdasarSkalaTab;

	public void onRekapBerdasarSkala(Event event) {
		if (rekapBerdasarSkalaTab.getChildren().size() == 0) {
			DashboardRekapKegiatanKemahasiswaanBerdasarSkala window = new DashboardRekapKegiatanKemahasiswaanBerdasarSkala();
			ais.ui.util.BaseDasbordPortal.mountWrapped(window, rekapBerdasarSkalaTab,
				"Rekap per Skala", "Sebaran kegiatan kemahasiswaan berdasarkan skala: lokal, nasional, internasional.");
		}
	}

	private Tabpanel rekapBerdasarKelompokTab;

	public void onRekapBerdasarKelompok(Event event) {
		if (rekapBerdasarKelompokTab.getChildren().size() == 0) {
			DashboardRekapKegiatanKemahasiswaanBerdasarKelompok window = new DashboardRekapKegiatanKemahasiswaanBerdasarKelompok();
			ais.ui.util.BaseDasbordPortal.mountWrapped(window, rekapBerdasarKelompokTab,
				"Rekap per Kelompok", "Sebaran kegiatan kemahasiswaan berdasarkan kelompok atau bidang kegiatan.");
		}
	}

	private Tabpanel rekapBerdasarDetailKelompokTab;
	private Textbox tempat;
	private Textbox url;

	private AmbilDataDosenBanbox dosenPembina1;
	private AmbilDataDosenBanbox dosenPembina2;

	private boolean loginSebagaiPesertaAtauPengajar() {
		return tbmuser != null && (tbmuser.getMahasiswa() != null || tbmuser.getSiswa() != null
				|| tbmuser.ambilDosen() != null || tbmuser.ambilGuru() != null);
	}
	private Combobox jabatanKegiatanKemahasiswaan;
	private Combobox skalaKegiatanKemahasiswaan;
	private Combobox tahunAkademik;
	private Combobox jenisSemester;
	private Combobox sertifikat;
	private MyCheckboxConfig bolehDipilih;
	private Textbox namaEn;
	private Combobox jenisAktfitasMahasiswa;
	private Textbox noSk;
	private MyDatebox tglSk;
	protected LampiranLain lainMahasiswaSK1;
	protected LampiranLain lainMahasiswaSK2;

	public void onRekapBerdasarDetailKelompok(Event event) {
		if (rekapBerdasarDetailKelompokTab.getChildren().size() == 0) {
			DashboardRekapKegiatanKemahasiswaanBerdasarDetailKelompok window = new DashboardRekapKegiatanKemahasiswaanBerdasarDetailKelompok();
			ais.ui.util.BaseDasbordPortal.mountWrapped(window, rekapBerdasarDetailKelompokTab,
				"Rekap Detail Kelompok", "Rincian kegiatan kemahasiswaan per sub-kelompok dan jenis aktivitas.");
		}
	}

	public void onUploadData(Event event) throws Exception {

		final Tbmuser tbmuser = Common.getCurrentUser();

		ForwardEvent forwardEvent = (ForwardEvent) event;
		Media media = ((UploadEvent) forwardEvent.getOrigin()).getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
		if (media.getName().toLowerCase().endsWith("xlsx")) {

			InputStream inputStream = media.getStreamData();
			// System.out.println("media = " + media);
			final File file = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
			// System.out.println("file = " + file.getAbsolutePath());
			file.getParentFile().mkdirs();
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			int c;
			while ((c = inputStream.read()) != -1) {
				fileOutputStream.write(c);
			}
			fileOutputStream.close();
			inputStream.close();

			final Label label = new Label(
					ais.common.Common.getBahasaConfig("Proses upload data kegiatan kemahasiswaan sedang berlangsung, harap menunggu.."));
			final UploadReportHelper report = new UploadReportHelper("Upload Kegiatan Kemahasiswaan");
			final Label downloadPath = new Label();
			new Thread(new Runnable() {

				@Override
				public void run() {
					try {

					XSSFWorkbook workbook;
					try {
						workbook = new XSSFWorkbook(file.getAbsolutePath());

						for (XSSFSheet sheet : Common.getAllXSSFSheet(workbook)) {
							Session session = HibernateUtil.currentNativeSession();

							KegiatanKemahasiswaan kegiatanKemahasiswaan = (KegiatanKemahasiswaan) session
									.createCriteria(KegiatanKemahasiswaan.class)
									.add(Restrictions.ilike("kode", sheet.getSheetName().trim(), MatchMode.EXACT))
									.setMaxResults(1).uniqueResult();
							if (kegiatanKemahasiswaan == null) {
								kegiatanKemahasiswaan = new KegiatanKemahasiswaan();
								kegiatanKemahasiswaan.setNama(sheet.getSheetName().trim());
								kegiatanKemahasiswaan.setKode(sheet.getSheetName().trim());
								kegiatanKemahasiswaan.setKeterangan(sheet.getSheetName().trim());
								session.getTransaction().begin();
								session.save(kegiatanKemahasiswaan);
								session.getTransaction().commit();
							}

							HibernateUtil.closeSession();

							int size = (sheet.getLastRowNum() + 1);
							for (int i = 0; i < (sheet.getLastRowNum() + 1); i++) {

								session = HibernateUtil.currentNativeSession();

								try {
									Mahasiswa mahasiswa = null;
									try {
										String nim = Common.getCellContent(Common.getCell(sheet, 0, i));
										mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
												.add(Restrictions.eq("nim", nim)).setMaxResults(1).uniqueResult();

										if (mahasiswa == null) {
											nim = Common.getCellContent(Common.getCell(sheet, 1, i));
											mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
													.add(Restrictions.eq("nim", nim)).setMaxResults(1).uniqueResult();
										}

										if (mahasiswa == null) {
											nim = Common.getCellContent(Common.getCell(sheet, 2, i));
											mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
													.add(Restrictions.eq("nim", nim)).setMaxResults(1).uniqueResult();
										}

										if (mahasiswa == null) {
											nim = Common.getCellContent(Common.getCell(sheet, 3, i));
											mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
													.add(Restrictions.eq("nim", nim)).setMaxResults(1).uniqueResult();
										}

									} catch (Exception e1) { ais.common.ErrorAuditUtil.record(e1, "auto-audit(empty-catch) src/ais/action/master/KegiatanKemahasiswaanAction.java:311");

									}

									if (mahasiswa == null) {
										continue;
									}

									Date mulai = Common.getSheetContentAsDate(sheet, 4, i);
									Date sampai = Common.getSheetContentAsDate(sheet, 5, i);

									JabatanKegiatanKemahasiswaan jabatanKegiatanKemahasiswaan = (JabatanKegiatanKemahasiswaan) Common
											.getSheetContentAsObject(sheet, 3, i, JabatanKegiatanKemahasiswaan.class);
									String keterangan = Common.getSheetContentAsString(sheet, 6, i);

									Boolean persetujuan = Common.getSheetContentAsBoolean(sheet, 8, i);

									KegiatanKemahasiswaanPunyaMahasiswa kegiatanKemahasiswaanPunyaMahasiswa = (KegiatanKemahasiswaanPunyaMahasiswa) session
											.createCriteria(KegiatanKemahasiswaanPunyaMahasiswa.class)
											.add(Restrictions.eq("mahasiswa", mahasiswa))
											.add(Restrictions.eq("kegiatanKemahasiswaan", kegiatanKemahasiswaan))
											.setMaxResults(1).uniqueResult();

									if (kegiatanKemahasiswaanPunyaMahasiswa == null) {
										kegiatanKemahasiswaanPunyaMahasiswa = new KegiatanKemahasiswaanPunyaMahasiswa();
									}
									kegiatanKemahasiswaanPunyaMahasiswa.setMulai(mulai);
									kegiatanKemahasiswaanPunyaMahasiswa.setSampai(sampai);
									kegiatanKemahasiswaanPunyaMahasiswa.setMahasiswa(mahasiswa);
									kegiatanKemahasiswaanPunyaMahasiswa.setKegiatanKemahasiswaan(kegiatanKemahasiswaan);
									kegiatanKemahasiswaanPunyaMahasiswa.setOleh(tbmuser.getUserId());
									kegiatanKemahasiswaanPunyaMahasiswa.setTbmuser(tbmuser);
									kegiatanKemahasiswaanPunyaMahasiswa
											.setDiubahDari(KegiatanKemahasiswaanAction.class.getSimpleName());

									kegiatanKemahasiswaanPunyaMahasiswa
											.setJabatanKegiatanKemahasiswaan(jabatanKegiatanKemahasiswaan);
									kegiatanKemahasiswaanPunyaMahasiswa.setKeterangan(keterangan);
									kegiatanKemahasiswaanPunyaMahasiswa.setPersetujuan(persetujuan);

									session.getTransaction().begin();
									session.saveOrUpdate(kegiatanKemahasiswaanPunyaMahasiswa);
									session.getTransaction().commit();

									HibernateUtil.closeSession();

									label.setValue("Upload mahasiswa " + mahasiswa + " di kegiatan kemahasiswaan "
											+ kegiatanKemahasiswaan.getNama() + ".. "
											+ Common.numberFormat.get().format(i * 100.0 / size) + " %");
									report.sukses(i, mahasiswa.getNim() + "@" + kegiatanKemahasiswaan.getNama(), "Kegiatan Kemahasiswaan berhasil diproses");

								} catch (Exception e1) {
									// TODO Auto-generated catch block

									HibernateUtil.closeSession();

									e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/KegiatanKemahasiswaanAction.java:366");
									report.gagal(i, "baris-" + i, e1, "Periksa data NIM/kegiatan pada baris ini");
								}
							}

						}

					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/KegiatanKemahasiswaanAction.java:374");
					}

					try {
						downloadPath.setValue(report.simpanLaporan().getAbsolutePath());
					} catch (java.io.IOException eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) src/ais/action/master/KegiatanKemahasiswaanAction.java:382"); }
					label.setValue("");
									} finally {
						ais.database.hibernate.HibernateUtil.closeSession();
					}
				}
			}).start();

			final Timer timer = new Timer(500);
			timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
			timer.setRepeats(true);
			timer.addEventListener("onTimer", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Clients.showBusy(label.getValue());
					if (label.getValue().isEmpty()) {
						Clients.clearBusy();
						try { Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain"); } catch (Exception ignored) {}
						MyMessageboxConfig.show("Update data organisasi berhasil dilakukan. " + report.getRingkasan(), "Pemberitahuan",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						timer.detach();
					}

				}
			});
			timer.start();

		} else {
			MyMessageboxConfig.show(
					"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
							+ media,
					"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
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

		tbmuser = Common.getCurrentUser();

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		if (add != null) { add.setVisible(tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null); }
		if (add != null) { add.setTooltiptext("Tambah"); }

		MyToolbarbuttonConfig ajukan = new MyToolbarbuttonConfig("Isi Form Pengajuan", "/img/print.png");
		ajukan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				LaporanPendidikanLingkunganKampus laporan = new LaporanPendidikanLingkunganKampus();
				laporan.setTitle("Pengajuan Form Kegiatan Kemahasiswaan");
				laporan.setClosable(true);
				laporan.setHeight("95%");
				laporan.setWidth("90%");
				laporan.setParent(page.getFirstRoot());
				laporan.onModal();
			}
		});
		if (ajukan != null) { ajukan.setParent(add.getParent()); }

		// edit = tbmuser.ambilDosen() == null
		// && tbmuser.getMahasiswa() == null;
		// delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE) &&
		// tbmuser.ambilDosen() == null
		// && tbmuser.getMahasiswa() == null;
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		searchdosen.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		if (tbmuser.getMahasiswa() != null || tbmuser.ambilDosen() != null) {
			kelompokKegiatanKemahasiswaanTab.getLinkedTab().setVisible(false);
			rekapBerdasarJabatanTab.getLinkedTab().setVisible(false);
			rekapBerdasarSkalaTab.getLinkedTab().setVisible(false);
			rekapBerdasarKelompokTab.getLinkedTab().setVisible(false);
			rekapBerdasarDetailKelompokTab.getLinkedTab().setVisible(false);
			formTab.getLinkedTab().setVisible(false);
			rekap.setVisible(false);
		}

		Comboitem comboitem = new Comboitem(KegiatanKemahasiswaan.BELUM_DIPROSES);
		if (comboitem != null) { comboitem.setValue(KegiatanKemahasiswaan.BELUM_DIPROSES); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem(KegiatanKemahasiswaan.SEDANG_DIPROSES);
		if (comboitem != null) { comboitem.setValue(KegiatanKemahasiswaan.SEDANG_DIPROSES); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem(KegiatanKemahasiswaan.DISETUJUI);
		if (comboitem != null) { comboitem.setValue(KegiatanKemahasiswaan.DISETUJUI); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem(KegiatanKemahasiswaan.DITOLAK);
		if (comboitem != null) { comboitem.setValue(KegiatanKemahasiswaan.DITOLAK); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem("Semua");
		if (comboitem != null) { comboitem.setValue(null); }
		searchstatus.appendChild(comboitem);
		if (searchstatus != null) { searchstatus.setReadonly(true); }
		if (searchstatus != null) { searchstatus.setSelectedItem(comboitem); }

		String[] contents = new String[] { "id", "mahasiswa", "kegiatanKemahasiswaan", "jabatanKegiatanKemahasiswaan",
				"skalaKegiatanKemahasiswaan", "mulai", "sampai", "keterangan", "persetujuan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common
				.cetakDataCustomButton(KegiatanKemahasiswaanPunyaMahasiswa.class, new DataCriteria() {

					@Override
					public Criteria initCriteria(boolean order) {

						Dosen dsn = (Dosen) (searchdosen != null ? null : searchdosen.getAttribute("dosen"));

						Session session = HibernateUtil.currentSession();
						// FIX (Excel tidak memuat semua peserta lintas-prodi): filter Fakultas/Prodi
						// diterapkan pada PRODI MAHASISWA (peserta), BUKAN prodi kegiatan. Dengan begitu,
						// memilih sebuah prodi akan mengekspor SELURUH partisipasi mahasiswa prodi tsb —
						// baik di kegiatan yang mereka ajukan sendiri MAUPUN di kegiatan milik mahasiswa
						// lain dari prodi berbeda. (Sebelumnya filter pada prodi KEGIATAN menyembunyikan
						// partisipasi lintas-prodi.)
						Criteria criteria = session.createCriteria(KegiatanKemahasiswaanPunyaMahasiswa.class)
								.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
								.createAlias("mahasiswa.jurusan", "mjurusan", Criteria.LEFT_JOIN);

						if (order)
							criteria.addOrder(Order.asc("id"));
						criteria.add(searchnim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("mahasiswa.nim", searchnim.getValue().trim(), MatchMode.ANYWHERE))

								.add(searchnamamhs.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.ilike("mahasiswa.nama", searchnamamhs.getValue().trim(),
												MatchMode.ANYWHERE))

								.add(dsn != null && dsn.getId() != null
										? Restrictions.eq("mahasiswa.dosen", dsn.getId())
										: Restrictions.sqlRestriction("true"))

								.add(CommonSearchFilterHelper.eqSelectedWithId("mahasiswa.jurusan", searchjurusan, false))
								.add(CommonSearchFilterHelper.eqSelectedWithId("mjurusan.fakultas", searchfakultas, false))

								.createCriteria("kegiatanKemahasiswaan")

								.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

								.add(searchstatus.getSelectedItem() == null
										|| searchstatus.getSelectedItem().getValue() == null
										|| searchstatus.getSelectedItem().getValue() == null
												? Restrictions.sqlRestriction("1=1")
												: Restrictions.eq("status", searchstatus.getSelectedItem().getValue()));

						return criteria;
					}
				}, "Download Persetujuan Mahasiswa", "/img/excel.png", contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KegiatanKemahasiswaanPunyaMahasiswa.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible())); }
		Common.appendKeToolbar(upload, add, comp);

		contents = loginSebagaiPesertaAtauPengajar()
				? new String[] { "id", "nama", "namaEn", "tempat", "detailKelompokKegiatanKemahasiswaan",
						"kelompokKegiatanKemahasiswaan", "mulai", "sampai", "fakultas", "jurusan", "keterangan",
						"status", "dosenPembina1", "dosenPembina2", "url", "jenisSemester", "tahunAkademik",
						"bolehDipilih" }
				: new String[] { "id", "nama", "namaEn", "tempat", "detailKelompokKegiatanKemahasiswaan",
						"kelompokKegiatanKemahasiswaan", "mulai", "sampai", "diajukanOleh", "fakultas", "jurusan",
						"keterangan", "status", "dosenPembina1", "dosenPembina2", "diajukanOleh", "url",
						"jenisSemester", "tahunAkademik", "bolehDipilih" };
		cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		upload = Common.uploadData(this, KegiatanKemahasiswaan.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible())); }
		Common.appendKeToolbar(upload, add, comp);

		MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("Cetak", "/img/print.png");
		cetak.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				LaporanPerKegiatanMahasiswa laporan = new LaporanPerKegiatanMahasiswa();
				laporan.setTitle("Cetak Kegiatan Mahasiswa");
				laporan.setClosable(true);
				laporan.setHeight("95%");
				laporan.setWidth("90%");
				laporan.setParent(page.getFirstRoot());
				laporan.onModal();
			}
		});
		if (cetak != null) { cetak.setParent(add.getParent()); }

		if (tbmuser.getMahasiswa() == null && tbmuser.getMahasiswa() == null) {
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Setujui Semua", "/img/svg/check2.svg");
			button.setVisible(
					Common.bolehKonfigurasi("aktifkan_tombol_setujui_semua_kegiatan_mahasiswa"));
			Common.appendKeToolbar(button, add, comp);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin melakukan persetujuan semua kegiatan mahasiswa ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										final Label label = new Label(
												ais.common.Common.getBahasaConfig("Proses verifikasi nilai sedang berlangsung, harap menunggu.."));

										new Thread(new Runnable() {

											@Override
											public void run() {
												double persenVeridikasi = 0.0;
												@SuppressWarnings("unchecked")
												List<KegiatanKemahasiswaan> kegiatanKemahasiswaans = initCriteria(true)
														.add(Restrictions.ne("status", KegiatanKemahasiswaan.DITOLAK))
														.list();
												int size = kegiatanKemahasiswaans.size();
												int iverifikasi = 0;
												// Sesi DEDIKASI untuk thread latar — bukan currentNativeSession() yang bisa
												// ditutup di tengah loop (akar "Session is closed!" saat commit). Ditutup
												// SEKALI di finally (clear/disconnect/close); L1 di-clear tiap iterasi agar
												// memori tetap rendah seperti perilaku closeSession() per-iterasi sebelumnya.
												Session session = HibernateUtil.getSessionFactory().openSession();
												try {
												boolean filterPeserta = (searchnim.getValue() != null && !searchnim.getValue().trim().isEmpty())
														|| (searchnamamhs.getValue() != null && !searchnamamhs.getValue().trim().isEmpty());
												if (filterPeserta) {
													// FIX: bila daftar disaring berdasar NIM/nama mahasiswa (di Lanjutan), tombol SETUJUI
													// menyetujui PESERTA yang cocok sebagai peserta (persetujuan=true) dan menjadikan
													// kegiatan terkait DISETUJUI agar persetujuan itu berlaku -- bukan menyetujui semua kegiatan.
													setujuiPesertaTerfilter(session, label);
												} else {
												for (KegiatanKemahasiswaan kegiatanKemahasiswaan : kegiatanKemahasiswaans) {
													iverifikasi++;
													try {
														persenVeridikasi = iverifikasi * 100.0 / size;
														if (label != null) {
															label.setValue(Common.numberFormat.get().format(persenVeridikasi)
																	+ "% .. Proses Persetujuan "
																	+ kegiatanKemahasiswaan.getNama());

														}

														kegiatanKemahasiswaan
																.setStatus(KegiatanKemahasiswaan.DISETUJUI);
														session.getTransaction().begin();
														Common.refreshUpdate(session, kegiatanKemahasiswaan);
														session.getTransaction().commit();

														@SuppressWarnings("unchecked")
														List<KegiatanKemahasiswaanPunyaMahasiswa> kegiatanKemahasiswaanPunyaMahasiswas = session
																.createCriteria(
																		KegiatanKemahasiswaanPunyaMahasiswa.class)
																.add(Restrictions.eq("kegiatanKemahasiswaan",
																		kegiatanKemahasiswaan))

																.list();
														for (KegiatanKemahasiswaanPunyaMahasiswa kegiatanKemahasiswaanPunyaMahasiswa : kegiatanKemahasiswaanPunyaMahasiswas) {
															kegiatanKemahasiswaanPunyaMahasiswa.setPersetujuan(true);
															session.getTransaction().begin();
															Common.refreshUpdate(session,
																	kegiatanKemahasiswaanPunyaMahasiswa);
															session.getTransaction().commit();
														}
													} catch (Exception e) {
														ais.common.Common.tampilErrorJikaAdmin(e);
													}
													try { session.clear(); } catch (Exception ignoreClear) { ais.common.ErrorAuditUtil.record(ignoreClear, "auto-audit(empty-catch) src/ais/action/master/KegiatanKemahasiswaanAction.java:649");}
												}
												}
												label.setValue("");
												} finally {
													try { session.clear(); } catch (Exception ignoreA) { ais.common.ErrorAuditUtil.record(ignoreA, "auto-audit(empty-catch) src/ais/action/master/KegiatanKemahasiswaanAction.java:653");}
													try { session.disconnect(); } catch (Exception ignoreB) { ais.common.ErrorAuditUtil.record(ignoreB, "auto-audit(empty-catch) src/ais/action/master/KegiatanKemahasiswaanAction.java:654");}
													try { session.close(); } catch (Exception ignoreC) { ais.common.ErrorAuditUtil.record(ignoreC, "auto-audit(empty-catch) src/ais/action/master/KegiatanKemahasiswaanAction.java:655");}
													try { HibernateUtil.closeSession(); } catch (Exception ignoreD) { ais.common.ErrorAuditUtil.record(ignoreD, "auto-audit(empty-catch) src/ais/action/master/KegiatanKemahasiswaanAction.java:656");}
												}
											}
										}).start();

										final Timer timer = new Timer(500);
										timer.setParent(
												ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
										timer.setRepeats(true);
										timer.addEventListener("onTimer", new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												Clients.showBusy(label.getValue());
												if (label.getValue().isEmpty()) {
													Clients.clearBusy();
													MyMessageboxConfig.show("Persetujuan kegiatan telah selesai",
															"Pemberitahuan", MyMessageboxConfig.OK,
															MyMessageboxConfig.INFORMATION);
													timer.detach();
													onSearchDefault(arg0);
												}

											}
										});
										timer.start();

									}

								}
							});
				}

			});
		}

	        FilterLanjutHelper.setup(comp);
}

	/**
	 * Renderer lokal untuk layar/komponen {@link KegiatanKemahasiswaanAction}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link KegiatanKemahasiswaanAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see KegiatanKemahasiswaanAction
	 */
	class KegiatanKemahasiswaanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KegiatanKemahasiswaan kegiatanKemahasiswaan = (KegiatanKemahasiswaan) arg1;

			final MyDetail detail = new MyDetail();
			final EventListener detailEventListener = new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					Common.clear(detail);
					if (detail.isOpen()) {
						KegiatanKemahasiswaanPunyaMahasiswaHelper detailperkuliahanHelper = new KegiatanKemahasiswaanPunyaMahasiswaHelper();
						detailperkuliahanHelper.display(kegiatanKemahasiswaan, detail, addWindow);
					}
				}
			};

			detail.setParent(arg0);
			detail.addEventListener("onOpen", detailEventListener);

			new Label(kegiatanKemahasiswaan.getKode()).setParent(arg0);

			Vbox a = RevisiHelper.createNewRevisi(KegiatanKemahasiswaan.class, kegiatanKemahasiswaan,
					kegiatanKemahasiswaan.getNama());
			new MyLabelKecilSekali(kegiatanKemahasiswaan.getNamaEn()).setParent(a);

			new MyLabelKecil(kegiatanKemahasiswaan.getJenisAktfitasMahasiswa() == null ? ""
					: kegiatanKemahasiswaan.getJenisAktfitasMahasiswa().getNama()).setParent(a);

			a.appendChild(new MyLabelAgakKecil(
					kegiatanKemahasiswaan.getTahunAkademik() + "/" + kegiatanKemahasiswaan.getJenisSemester()));
			new MyLabelAgakKecil(kegiatanKemahasiswaan.getTempat()).setParent(a);
			a.setParent(arg0);

			new Label(kegiatanKemahasiswaan.getFakultas() == null ? "Semua"
					: kegiatanKemahasiswaan.getFakultas().getNama()).setParent(arg0);
			new Label(
					kegiatanKemahasiswaan.getJurusan() == null ? "Semua" : kegiatanKemahasiswaan.getJurusan().getNama())
					.setParent(arg0);

			new Label((kegiatanKemahasiswaan.getDosenPembina1() == null ? ""
					: kegiatanKemahasiswaan.getDosenPembina1().getNama())
					+ (kegiatanKemahasiswaan.getDosenPembina2() == null ? ""
							: ", " + kegiatanKemahasiswaan.getDosenPembina2().getNama()))
					.setParent(arg0);

			new MyLabelKecil(kegiatanKemahasiswaan.getKelompokKegiatanKemahasiswaan().getNama()).setParent(arg0);
			a = new Vbox();
			a.setParent(arg0);
			new MyLabelKecil(kegiatanKemahasiswaan.getDetailKelompokKegiatanKemahasiswaan().getNama()).setParent(a);
			Vbox myvbox = new Vbox();
			myvbox.setParent(a);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, kegiatanKemahasiswaan.getId(),
					KegiatanKemahasiswaan.class.getName(), "Lampiran", false, null, null, false, false, false, false);

			new Label(kegiatanKemahasiswaan.getMulai() == null ? ""
					: Common.dateFormat1.get().format(kegiatanKemahasiswaan.getMulai())).setParent(arg0);
			new Label(kegiatanKemahasiswaan.getSampai() == null ? ""
					: Common.dateFormat1.get().format(kegiatanKemahasiswaan.getSampai())).setParent(arg0);

			new Label(kegiatanKemahasiswaan.getDiajukanOleh() == null ? "Admin"
					: kegiatanKemahasiswaan.getDiajukanOleh().getNama()).setParent(arg0);

			new Label(kegiatanKemahasiswaan.getSertifikat() == null ? "-"
					: kegiatanKemahasiswaan.getSertifikat().getNama()).setParent(arg0);

			final Hbox toolbar = new Hbox();
			final MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Krm ke feeder",
					"/img/Finance-Invoice-icon.png");
			buttonTagihan.setStyle("font-size:8px;");
			final Hbox myHbox = new Hbox();
			myHbox.setVisible(kegiatanKemahasiswaan.getStatus().equals(KegiatanKemahasiswaan.DISETUJUI));

			if (tbmuser != null && ((tbmuser.ambilFakultas() != null && kegiatanKemahasiswaan.getFakultas() == null)
					|| (tbmuser.ambilJurusan() != null && kegiatanKemahasiswaan.getJurusan() == null))) {
				new Label(kegiatanKemahasiswaan.getStatus()).setParent(arg0);
			} else if (tbmuser.getMahasiswa() == null) {
				final Combobox status = new Combobox();
				Comboitem comboitem = new Comboitem(KegiatanKemahasiswaan.BELUM_DIPROSES);
				comboitem.setValue(KegiatanKemahasiswaan.BELUM_DIPROSES);
				status.appendChild(comboitem);

				comboitem = new Comboitem(KegiatanKemahasiswaan.SEDANG_DIPROSES);
				comboitem.setValue(KegiatanKemahasiswaan.SEDANG_DIPROSES);
				status.appendChild(comboitem);

				comboitem = new Comboitem(KegiatanKemahasiswaan.DISETUJUI);
				comboitem.setValue(KegiatanKemahasiswaan.DISETUJUI);
				status.appendChild(comboitem);

				comboitem = new Comboitem(KegiatanKemahasiswaan.DITOLAK);
				comboitem.setValue(KegiatanKemahasiswaan.DITOLAK);
				status.appendChild(comboitem);

				Common.selectComboItem(status, kegiatanKemahasiswaan.getStatus());
				status.setParent(arg0);
				status.setReadonly(true);
				status.setWidth("97%");

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kegiatanKemahasiswaan.setStatus((String) (status.getSelectedItem() == null
								|| status.getSelectedItem().getValue() == null ? null
										: status.getSelectedItem().getValue()));
						if (arg0 != null) {
							Common.refreshUpdate(kegiatanKemahasiswaan);
						}
						toolbar.setVisible(!kegiatanKemahasiswaan.getStatus().equals(PrestasiMahasiswa.DISETUJUI));

						if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
								&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {
							buttonTagihan
									.setVisible(kegiatanKemahasiswaan.getStatus().equals(PrestasiMahasiswa.DISETUJUI));

						}
						myHbox.setVisible(kegiatanKemahasiswaan.getStatus().equals(PrestasiMahasiswa.DISETUJUI));
						Common.clear(detail);
						detail.setOpen(false);

						Common.createDefaultTimer(detailEventListener);
					}
				};
				status.addEventListener("onChange", eventListener);
				eventListener.onEvent(null);
			} else {
				new Label(kegiatanKemahasiswaan.getStatus()).setParent(arg0);
			}

			new Label(kegiatanKemahasiswaan.getKeterangan()).setParent(arg0);
			toolbar.setVisible(!kegiatanKemahasiswaan.getStatus().equals(PrestasiMahasiswa.DISETUJUI));

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			// button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(kegiatanKemahasiswaan);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			if (tbmuser != null && ((tbmuser.ambilFakultas() != null && kegiatanKemahasiswaan.getFakultas() == null)
					|| (tbmuser.ambilJurusan() != null && kegiatanKemahasiswaan.getJurusan() == null))) {
				button.setVisible(false);
			}

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			// button.setVisible(delete);
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
											// Hapus dulu baris anak di kegiatan_kemahasiswaan_punya_mahasiswa yang masih
											// mereferensikan kegiatan ini (FK tanpa cascade) agar refreshDelete tidak gagal
											// "violates foreign key constraint ... punya_mahasiswa" — kegagalan itu meng-abort
											// transaksi thread-local sehingga onSearchDefault berikutnya melempar
											// "createCriteria is not valid without active transaction".
											if (kegiatanKemahasiswaan.getId() != null) {
												Common.updateSql(
														"delete from kegiatan_kemahasiswaan_punya_mahasiswa where kegiatan_kemahasiswaan = "
																+ kegiatanKemahasiswaan.getId());
											}
											Common.refreshDelete(kegiatanKemahasiswaan);
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

			if (tbmuser != null && ((tbmuser.ambilFakultas() != null && kegiatanKemahasiswaan.getFakultas() == null)
					|| (tbmuser.ambilJurusan() != null && kegiatanKemahasiswaan.getJurusan() == null))) {
				button.setVisible(false);
			}

			Vbox vbox1 = new Vbox();
			vbox1.setParent(arg0);

			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(vbox1);

			myHbox.setParent(vbox1);

			if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
					&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {

				if (kegiatanKemahasiswaan.getFeeder() != null && !kegiatanKemahasiswaan.getFeeder().trim().isEmpty()) {
					myHbox.appendChild(new Image("/img/svg/check2-circle.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder valid"));
				} else {
					myHbox.appendChild(new Image("/img/svg/warning-outline.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder blm valid"));
				}

				buttonTagihan.setVisible(kegiatanKemahasiswaan.getStatus().equals(PrestasiMahasiswa.DISETUJUI));

				buttonTagihan.setStyle("font-size:8px;");
				buttonTagihan.setParent(vbox1);
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
														myLabelProsesDetail
																.setValue("Mengirim data " + kegiatanKemahasiswaan);

														feederImporter.aktivitasKegiatanMahasiswa(kegiatanKemahasiswaan,
																errorLog);

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

			}
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new KegiatanKemahasiswaan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public static void onAddExternal(EventListener eventListener, KegiatanKemahasiswaan kegiatanKemahasiswaan)
			throws Exception {
		KegiatanKemahasiswaanAction kegiatanKemahasiswaanAction = new KegiatanKemahasiswaanAction();
		kegiatanKemahasiswaanAction.eventListener = eventListener;
		kegiatanKemahasiswaanAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
				.appendChild(kegiatanKemahasiswaanAction.addWindow);
		kegiatanKemahasiswaanAction.addWindow.setHeight("95%");
		kegiatanKemahasiswaanAction.addWindow.setWidth("850px");

		kegiatanKemahasiswaanAction.init(kegiatanKemahasiswaan);

		kegiatanKemahasiswaanAction.addWindow.setVisible(true);
		kegiatanKemahasiswaanAction.addWindow.onModal();
	}

	private void init(final KegiatanKemahasiswaan kegiatanKemahasiswaan) throws Exception {
		this.kegiatanKemahasiswaan = kegiatanKemahasiswaan;
		addWindow.setTitle(kegiatanKemahasiswaan.getId() == null ? "Tambah Kegiatan Kemahasiswaan" : "Ubah Kegiatan Kemahasiswaan");
		Common.clear(addWindow);
		addWindow.setWidth("700px");
		final Tbmuser tbmuser = Common.getCurrentUser();
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
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kegiatan *"));
		row.appendChild(nama = new Textbox(kegiatanKemahasiswaan.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);
		nama.setMaxlength(255);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kegiatan (dalam bhs inggris)"));
		row.appendChild(namaEn = new Textbox(kegiatanKemahasiswaan.getNamaEn()));
		namaEn.setWidth("90%");
		namaEn.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Kegiatan"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		hbox.setWidth("90%");

		mulai = new MyDatebox(kegiatanKemahasiswaan.getMulai());
		sampai = new MyDatebox(kegiatanKemahasiswaan.getSampai());

		hbox.appendChild(mulai);
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));
		hbox.appendChild(sampai);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen Pembina I"));
		row.appendChild(dosenPembina1 = new AmbilDataDosenBanbox());
		dosenPembina1.setAttribute("myValue", kegiatanKemahasiswaan.getDosenPembina1());
		dosenPembina1.setAttribute("dosen", kegiatanKemahasiswaan.getDosenPembina1());
		dosenPembina1.setValue(kegiatanKemahasiswaan.getDosenPembina1() == null ? ""
				: kegiatanKemahasiswaan.getDosenPembina1().getNama());
		dosenPembina1.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("SK Dosen Pembina I"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, kegiatanKemahasiswaan.getId(),
				KegiatanKemahasiswaan.class.getName() + "_SK Dosen Pembina I", "SK Dosen Pembina I", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswaSK1 = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen Pembina II"));
		row.appendChild(dosenPembina2 = new AmbilDataDosenBanbox());
		dosenPembina2.setAttribute("myValue", kegiatanKemahasiswaan.getDosenPembina2());
		dosenPembina2.setAttribute("dosen", kegiatanKemahasiswaan.getDosenPembina2());
		dosenPembina2.setValue(kegiatanKemahasiswaan.getDosenPembina2() == null ? ""
				: kegiatanKemahasiswaan.getDosenPembina2().getNama());
		dosenPembina2.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("SK Dosen Pembina II"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, kegiatanKemahasiswaan.getId(),
				KegiatanKemahasiswaan.class.getName() + "_SK Dosen Pembina II", "SK Dosen Pembina II", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswaSK2 = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tempat / Alamat Kegiatan *"));
		row.appendChild(tempat = new Textbox(kegiatanKemahasiswaan.getTempat()));
		tempat.setWidth("90%");
		tempat.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aspek Kegiatan *"));
		row.appendChild(kelompokKegiatanKemahasiswaan = new Combobox());
		kelompokKegiatanKemahasiswaan.setWidth("90%");
		kelompokKegiatanKemahasiswaan.setReadonly(true);

		Common.insertCombo(kelompokKegiatanKemahasiswaan, "nama", "keterangan", KelompokKegiatanKemahasiswaan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				tbmuser != null && tbmuser.getMahasiswa() != null ? Restrictions
						.or(Restrictions.isNull("bisaDipilihMahasiswa"), Restrictions.eq("bisaDipilihMahasiswa", true))
						: Restrictions.sqlRestriction("true"));
		Common.selectComboItem(kelompokKegiatanKemahasiswaan, kegiatanKemahasiswaan.getKelompokKegiatanKemahasiswaan());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Rincian Aspek Kegiatan *"));
		row.appendChild(detailKelompokKegiatanKemahasiswaan = new Combobox());
		detailKelompokKegiatanKemahasiswaan.setWidth("90%");
		detailKelompokKegiatanKemahasiswaan.setReadonly(true);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(detailKelompokKegiatanKemahasiswaan);
				if (kelompokKegiatanKemahasiswaan.getSelectedItem() != null
						&& kelompokKegiatanKemahasiswaan.getSelectedItem().getValue() != null) {
					Common.insertCombo(detailKelompokKegiatanKemahasiswaan, "nama",
							DetailKelompokKegiatanKemahasiswaan.class,
							Restrictions.and(
									Restrictions.eq("kelompokKegiatanKemahasiswaan",
											kelompokKegiatanKemahasiswaan.getSelectedItem().getValue()),
									Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							tbmuser != null && tbmuser.getMahasiswa() != null
									? Restrictions.or(Restrictions.isNull("bisaDipilihMahasiswa"),
											Restrictions.eq("bisaDipilihMahasiswa", true))
									: Restrictions.sqlRestriction("true"));
					Common.selectComboItem(detailKelompokKegiatanKemahasiswaan,
							kegiatanKemahasiswaan.getDetailKelompokKegiatanKemahasiswaan());
				}

			}
		};

		kelompokKegiatanKemahasiswaan.addEventListener("onChange", eventListener);
		eventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(bolehDipilih = new MyCheckboxConfig("Kegiatan ini bisa dipilih oleh mahasiswa lainnya"));
		bolehDipilih.setChecked(kegiatanKemahasiswaan.getBolehDipilih());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jabatan/Status *"));
		row.appendChild(jabatanKegiatanKemahasiswaan = new Combobox());
		jabatanKegiatanKemahasiswaan.setWidth("90%");
		jabatanKegiatanKemahasiswaan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Skala *"));
		row.appendChild(skalaKegiatanKemahasiswaan = new Combobox());
		skalaKegiatanKemahasiswaan.setWidth("90%");
		skalaKegiatanKemahasiswaan.setReadonly(true);

		EventListener detail = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(jabatanKegiatanKemahasiswaan);
				Common.clear(skalaKegiatanKemahasiswaan);
				DetailKelompokKegiatanKemahasiswaan kedosenan = (DetailKelompokKegiatanKemahasiswaan) (detailKelompokKegiatanKemahasiswaan
						.getSelectedItem() == null ? null
								: detailKelompokKegiatanKemahasiswaan.getSelectedItem().getValue());
				if (kedosenan != null) {
					HibernateUtil.currentSession().refresh(kedosenan);
					List<JabatanKegiatanKemahasiswaan> jabatanKegiatanKemahasiswaans = new ArrayList<JabatanKegiatanKemahasiswaan>(
							kedosenan.getJabatanKegiatanKemahasiswaans());
					List<SkalaKegiatanKemahasiswaan> skalaKegiatanKemahasiswaans = new ArrayList<SkalaKegiatanKemahasiswaan>(
							kedosenan.getSkalaKegiatanKemahasiswaans());

					Collections.sort(jabatanKegiatanKemahasiswaans);
					Collections.sort(skalaKegiatanKemahasiswaans);

					Common.insertComboItems(jabatanKegiatanKemahasiswaan, "nama", jabatanKegiatanKemahasiswaans);
					Common.insertComboItems(skalaKegiatanKemahasiswaan, "nama", skalaKegiatanKemahasiswaans);

					Common.selectComboItem(jabatanKegiatanKemahasiswaan,
							kegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaan());
					Common.selectComboItem(skalaKegiatanKemahasiswaan,
							kegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaan());

				}

			}
		};

		detailKelompokKegiatanKemahasiswaan.addEventListener("onChange", detail);
		detail.onEvent(null);

		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		if (kegiatanKemahasiswaan.getFakultas() == null && tbmuser != null && tbmuser.ambilFakultas() != null) {
			kegiatanKemahasiswaan.setFakultas(tbmuser.ambilFakultas());
		}
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		Common.selectComboItem(fakultas, kegiatanKemahasiswaan.getFakultas());
		fakultas.setWidth("90%");

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		Common.pilihJurusan(jurusan, kegiatanKemahasiswaan.getJurusan());

		if (kegiatanKemahasiswaan.getJurusan() == null) {
			if (tbmuser.ambilJurusan() != null
					|| (tbmuser.getMahasiswa() != null && tbmuser.getMahasiswa().getJurusan() != null)) {
				Common.pilihJurusan(jurusan,
						tbmuser == null || tbmuser.ambilJurusan() == null ? tbmuser.getMahasiswa().getJurusan()
								: tbmuser.ambilJurusan());

			}
		}
		fakultas.setDisabled(false);
		jurusan.setDisabled(false);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("URL Kegiatan"));
		row.appendChild(url = new Textbox(kegiatanKemahasiswaan.getUrl()));
		url.setWidth("90%");

		Common.generateTahunAjaran(tahunAkademik = new Combobox());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		Common.selectComboItem(tahunAkademik, kegiatanKemahasiswaan.getTahunAkademik());

		jenisSemester = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		jenisSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		jenisSemester.appendChild(comboitem);
		jenisSemester.setSelectedIndex(1);
		row.appendChild(jenisSemester);
		jenisSemester.setWidth("90%");
		jenisSemester.setReadonly(true);

		Common.selectComboItem(jenisSemester, kegiatanKemahasiswaan.getJenisSemester());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester *"));
		row.appendChild(jenisSemester);
		jenisSemester.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis / Kampus Merdeka"));
		row.appendChild(jenisAktfitasMahasiswa = new Combobox());
		Common.insertCombo(jenisAktfitasMahasiswa, "nama", "merupakanKampusMerdeka", JenisAktfitasMahasiswa.class,
				Restrictions.eq("aktif", true));
		Common.selectComboItem(jenisAktfitasMahasiswa, kegiatanKemahasiswaan.getJenisAktfitasMahasiswa());
		jenisAktfitasMahasiswa.setWidth("90%");
		jenisAktfitasMahasiswa.setReadonly(true);

		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			jenisAktfitasMahasiswa.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor SK Kegiatan"));
		row.appendChild(noSk = new Textbox(kegiatanKemahasiswaan.getNoSk()));
		noSk.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal SK Kegiatan"));
		row.appendChild(tglSk = new MyDatebox(kegiatanKemahasiswaan.getTglSk()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(kegiatanKemahasiswaan.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sertifikat"));
		row.appendChild(sertifikat = new Combobox());
		Common.insertComboDanSemua(sertifikat, new String[] { "nama" }, "keterangan", Sertifikat.class,
				"== Tanpa Sertifikat ==");
		Common.selectComboItem(sertifikat, kegiatanKemahasiswaan.getSertifikat());
		sertifikat.setWidth("90%");
		sertifikat.setReadonly(true);

		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			sertifikat.setDisabled(true);
		}

		lainMahasiswa = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lampiran Kegiatan"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, kegiatanKemahasiswaan.getId(),
				KegiatanKemahasiswaan.class.getName(), "Lampiran Kegiatan", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		Common.initKeterangan(rows, "Jika file lampiran kegiatan lebih dari satu file, zip dulu semua file tersebut");

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

					if (KegiatanKemahasiswaanAction.this.eventListener != null) {
						KegiatanKemahasiswaanAction.this.eventListener.onEvent(event);
					}
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public Boolean checkNamaAgama() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(KegiatanKemahasiswaan.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.kegiatanKemahasiswaan.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.kegiatanKemahasiswaan.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	public boolean onSave(Event event) throws Exception {
		// Guard: entity yang sedang diedit (kegiatanKemahasiswaan) bisa saja SUDAH DIHAPUS dari DB
		// oleh user/proses lain di antara saat form dibuka dan saat tombol Simpan ditekan. Proxy
		// Hibernate lama pada field ini tidak tahu baris induknya sudah tidak ada -- bila dibiarkan
		// lanjut, ini memicu 3 error berantai: NPE getId() proxy rusak (checkNamaAgama), lalu
		// ObjectNotFoundException saat set field lain pada proxy yang sama, lalu FK violation saat
		// insert relasi KegiatanKemahasiswaanPunyaMahasiswa ke induk yang tak ada. Cek dulu di sini
		// dengan query ULANG by ID (bukan pakai proxy lama dari form) sebelum proses simpan mulai.
		boolean kegiatanSudahTerhapus = false;
		Long kegiatanIdUntukCek = null;
		if (kegiatanKemahasiswaan != null) {
			try {
				kegiatanIdUntukCek = kegiatanKemahasiswaan.getId();
			} catch (Exception e) {
				// proxy sudah rusak (mis. AbstractLazyInitializer NPE) -- anggap entity sudah terhapus
				kegiatanSudahTerhapus = true;
			}
		}
		if (!kegiatanSudahTerhapus && kegiatanIdUntukCek != null) {
			try {
				Session cekSession = HibernateUtil.currentNativeSession();
				if (cekSession.get(KegiatanKemahasiswaan.class, kegiatanIdUntukCek) == null) {
					kegiatanSudahTerhapus = true;
				}
			} catch (Exception e) {
				kegiatanSudahTerhapus = true;
			}
		}
		if (kegiatanSudahTerhapus) {
			MyMessageboxConfig.show(
					"Data kegiatan kemahasiswaan ini sudah dihapus oleh proses/user lain, silakan muat ulang halaman.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kegiatan Kemahasiswaan",
					"Kolom Nama Kegiatan Kemahasiswaan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Kegiatan Kemahasiswaan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (tempat.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tempat Kegiatan Kemahasiswaan",
					"Kolom Tempat Kegiatan Kemahasiswaan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tempat Kegiatan Kemahasiswaan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (kelompokKegiatanKemahasiswaan.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Aspek Kegiatan Kemahasiswaan",
					"Kolom Aspek Kegiatan Kemahasiswaan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Aspek Kegiatan Kemahasiswaan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (detailKelompokKegiatanKemahasiswaan.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Rincian Aspek Kegiatan Kemahasiswaan",
					"Kolom Rincian Aspek Kegiatan Kemahasiswaan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Rincian Aspek Kegiatan Kemahasiswaan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (jabatanKegiatanKemahasiswaan.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jabatan Kegiatan Dosen",
					"Kolom Jabatan Kegiatan Dosen belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jabatan Kegiatan Dosen.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (skalaKegiatanKemahasiswaan.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Skala Kegiatan Dosen",
					"Kolom Skala Kegiatan Dosen belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Skala Kegiatan Dosen.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
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
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Semester",
					"Kolom Jenis Semester belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jenis Semester.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (checkNamaAgama()) {
			MyMessageboxConfig.show(
					"Nama kegiatan \"" + nama.getValue() + "\" sudah ada, silahkan menagmbil kegiatan \""
							+ nama.getValue() + "\" di tombol \"Ambil Kegiatan Yang Ada\"",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		java.util.Date nilaiMulai;
		java.util.Date nilaiSampai;
		try {
			nilaiMulai = mulai.getValue();
			nilaiSampai = sampai.getValue();
		} catch (org.zkoss.zk.ui.WrongValueException e) {
			PesanFormalHelper.tampilkanGagal("penyimpanan tanggal kegiatan",
					"Tanggal mulai atau tanggal selesai belum valid. Gunakan format dd-MM-yyyy.",
					new String[] { "Periksa kembali kedua kolom tanggal.", "Isi tanggal yang valid lalu ulangi penyimpanan." });
			return false;
		}
		if (nilaiMulai == null || nilaiSampai == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan tanggal kegiatan",
					"Tanggal mulai dan tanggal selesai wajib diisi.",
					new String[] { "Lengkapi kedua tanggal lalu ulangi penyimpanan." });
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (kegiatanKemahasiswaan.getId() != null) {
			kegiatanKemahasiswaan = (KegiatanKemahasiswaan) session.load(KegiatanKemahasiswaan.class,
					kegiatanKemahasiswaan.getId());

		}
		kegiatanKemahasiswaan.setJabatanKegiatanKemahasiswaan(
				(JabatanKegiatanKemahasiswaan) jabatanKegiatanKemahasiswaan.getSelectedItem().getValue());
		kegiatanKemahasiswaan.setSkalaKegiatanKemahasiswaan(
				(SkalaKegiatanKemahasiswaan) skalaKegiatanKemahasiswaan.getSelectedItem().getValue());

		kegiatanKemahasiswaan.setDosenPembina1((Dosen) dosenPembina1.getAttribute("dosen"));
		kegiatanKemahasiswaan.setDosenPembina2((Dosen) dosenPembina2.getAttribute("dosen"));
		kegiatanKemahasiswaan.setTempat(tempat.getValue());
		kegiatanKemahasiswaan.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		kegiatanKemahasiswaan.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		kegiatanKemahasiswaan.setUrl(url.getValue());
		kegiatanKemahasiswaan.setMulai(nilaiMulai);
		kegiatanKemahasiswaan.setSampai(nilaiSampai);
		kegiatanKemahasiswaan.setNama(nama.getValue());
		kegiatanKemahasiswaan.setNamaEn(namaEn.getValue());

		kegiatanKemahasiswaan.setDetailKelompokKegiatanKemahasiswaan(
				(DetailKelompokKegiatanKemahasiswaan) detailKelompokKegiatanKemahasiswaan.getSelectedItem().getValue());
		kegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(
				(KelompokKegiatanKemahasiswaan) kelompokKegiatanKemahasiswaan.getSelectedItem().getValue());
		kegiatanKemahasiswaan.setKeterangan(keterangan.getValue());

		kegiatanKemahasiswaan.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		kegiatanKemahasiswaan.setJenisSemester((String) jenisSemester.getSelectedItem().getValue());

		kegiatanKemahasiswaan.setSertifikat(
				(Sertifikat) (sertifikat.getSelectedItem() == null ? null : sertifikat.getSelectedItem().getValue()));
		kegiatanKemahasiswaan.setBolehDipilih(bolehDipilih.isChecked());
		kegiatanKemahasiswaan.setJenisAktfitasMahasiswa(
				(JenisAktfitasMahasiswa) (jenisAktfitasMahasiswa.getSelectedItem() == null ? null
						: jenisAktfitasMahasiswa.getSelectedItem().getValue()));
		kegiatanKemahasiswaan.setNoSk(noSk.getValue());
		kegiatanKemahasiswaan.setTglSk(tglSk.getValue());

		// FIX (kegiatan yang diajukan mahasiswa via mobile tidak muncul): JANGAN menimpa diajukanOleh
		// yang SUDAH di-set. "Ajukan Kegiatan Baru" dari layar mahasiswa sudah mengisi diajukanOleh =
		// mahasiswa yang bersangkutan. Menimpanya dengan tbmuser.getMahasiswa() membuat nilainya jadi
		// null bila sesi login tidak memiliki mahasiswa (mis. akses mobile atau admin atas nama
		// mahasiswa), sehingga partisipasi (KegiatanKemahasiswaanPunyaMahasiswa) tidak terbentuk dan
		// kegiatan tidak pernah muncul di daftar mahasiswa. Isi dari pengguna login HANYA bila belum
		// ada pengaju -- ini juga mencegah pengaju terhapus saat kegiatan disunting oleh admin.
		if (kegiatanKemahasiswaan.getDiajukanOleh() == null && tbmuser != null && tbmuser.getMahasiswa() != null) {
			kegiatanKemahasiswaan.setDiajukanOleh(tbmuser.getMahasiswa());
		}
		// Simpan Kegiatan + (bila diajukan mahasiswa) buat partisipasi KegiatanKemahasiswaanPunyaMahasiswa
		// dalam SATU transaksi native eksplisit. BUG sebelumnya: jalur simpan berpindah-pindah antara
		// session ZK & native tanpa transaksi eksplisit -> (a) id kadang tak ter-assign -> masuk jalur
		// resave rapuh yang bisa rollback internal -> onSave return false -> form TIDAK menutup ("tidak
		// ada reaksi") & partisipasi TIDAK terbentuk; (b) save partisipasi tak di-commit -> kegiatan
		// tersimpan tapi tak pernah muncul di daftar mahasiswa. Versi ini atomik & pasti ter-commit.
		KegiatanKemahasiswaanPunyaMahasiswa kegiatanKemahasiswaanPunyaMahasiswa = null;
		{
			Session ns = HibernateUtil.currentNativeSession();
			org.hibernate.Transaction txSimpan = null;
			try {
				txSimpan = ns.beginTransaction();
				if (kegiatanKemahasiswaan.getId() == null) {
					ns.save(kegiatanKemahasiswaan);
				} else if (ns.contains(kegiatanKemahasiswaan)) {
					ns.saveOrUpdate(kegiatanKemahasiswaan);
				} else {
					kegiatanKemahasiswaan = (KegiatanKemahasiswaan) ns.merge(kegiatanKemahasiswaan);
				}
				ns.flush(); // pastikan id ter-assign (IDENTITY) sebelum membuat relasi partisipasi

				if (kegiatanKemahasiswaan.getDiajukanOleh() != null && kegiatanKemahasiswaan.getId() != null) {
					kegiatanKemahasiswaanPunyaMahasiswa = (KegiatanKemahasiswaanPunyaMahasiswa) ns
							.createCriteria(KegiatanKemahasiswaanPunyaMahasiswa.class)
							.add(Restrictions.eq("kegiatanKemahasiswaan", kegiatanKemahasiswaan))
							.add(Restrictions.eq("mahasiswa", kegiatanKemahasiswaan.getDiajukanOleh()))
							.setMaxResults(1).uniqueResult();
					if (kegiatanKemahasiswaanPunyaMahasiswa == null) {
						kegiatanKemahasiswaanPunyaMahasiswa = new KegiatanKemahasiswaanPunyaMahasiswa();
						kegiatanKemahasiswaanPunyaMahasiswa.setKegiatanKemahasiswaan(kegiatanKemahasiswaan);
						kegiatanKemahasiswaanPunyaMahasiswa.setOleh(tbmuser == null ? null : tbmuser.getUserId());
						kegiatanKemahasiswaanPunyaMahasiswa.setTbmuser(tbmuser);
						kegiatanKemahasiswaanPunyaMahasiswa.setMahasiswa(kegiatanKemahasiswaan.getDiajukanOleh());
						kegiatanKemahasiswaanPunyaMahasiswa.setDiubahDari(MahasiswaAction.class.getSimpleName());
						ns.save(kegiatanKemahasiswaanPunyaMahasiswa);
					}
				}

				txSimpan.commit();
			} catch (Exception eSimpan) {
				if (txSimpan != null && txSimpan.isActive()) {
					try {
						txSimpan.rollback();
					} catch (Exception ignore) {
						ais.common.ErrorAuditUtil.record(ignore, "KegiatanKemahasiswaanAction.onSave rollback");
					}
				}
				ais.common.ErrorAuditUtil.record(eSimpan,
						"KegiatanKemahasiswaanAction.onSave: gagal simpan Kegiatan + partisipasi");
				MyMessageboxConfig.show(
						"Gagal menyimpan Kegiatan Kemahasiswaan. Silakan periksa data & coba lagi. Detail: "
								+ (eSimpan.getMessage() == null ? eSimpan.getClass().getSimpleName() : eSimpan.getMessage()),
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}
			session = ns;
		}

		if (lainMahasiswaSK1 != null && lainMahasiswaSK1.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainMahasiswaSK1);
				lainMahasiswaSK1.setRef(kegiatanKemahasiswaan.getId());

				session.getTransaction().begin();
				session.update(lainMahasiswaSK1);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}
		}

		if (lainMahasiswaSK2 != null && lainMahasiswaSK2.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainMahasiswaSK2);
				lainMahasiswaSK2.setRef(kegiatanKemahasiswaan.getId());

				session.getTransaction().begin();
				session.update(lainMahasiswaSK2);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}
		}

		byte[] fotoBytesUntukCopy = null;
		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainMahasiswa);
				lainMahasiswa.setRef(kegiatanKemahasiswaan.getId());

				session.getTransaction().begin();
				session.update(lainMahasiswa);
				// Baca byte foto SELAGI transaksi streaming AKTIF (LO Postgres wajib non-autocommit).
				// Blob asli terikat ke koneksi streaming; setelah closeSession koneksi kembali ke mode
				// autocommit, sehingga menyalin Blob itu langsung ke 'copy' memicu "Large Objects may
				// not be used in auto-commit mode" saat blobSession.save(copy). Materialkan dulu ke
				// byte[] agar bisa dibungkus jadi Blob in-memory (bind LO terjadi pada blobConn).
				if (kegiatanKemahasiswaanPunyaMahasiswa != null) {
					try {
						fotoBytesUntukCopy = bacaBytesBlob(lainMahasiswa.getFoto());
					} catch (Exception exFoto) {
						fotoBytesUntukCopy = null;
					}
				}
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

			if (kegiatanKemahasiswaanPunyaMahasiswa != null) {
				LampiranLain copy = (LampiranLain) lainMahasiswa.clone();
				// LampiranLain HANYA dipetakan di SessionFactory STREAMING; menyimpannya lewat
				// HibernateUtil (factory utama) memicu "Unknown entity: LampiranLain". Namun kolom
				// foto adalah Large Object (oid) yang WAJIB ditulis pada koneksi NON-autocommit
				// ("Large Objects may not be used in auto-commit mode"). Karena itu ambil SATU
				// koneksi langsung dari ConnectionProvider factory streaming, kendalikan autocommit
				// sendiri, lalu buka session di atas koneksi tsb (pola sama dgn FileFotoLain/DoUpload).
				Session blobSession = null;
				java.sql.Connection blobConn = null;
				try {
					org.hibernate.engine.SessionFactoryImplementor sfStreaming = (org.hibernate.engine.SessionFactoryImplementor) StreamingHibernateUtil
							.getInstance().getSessionFactory();
					org.hibernate.connection.ConnectionProvider cp = sfStreaming.getConnectionProvider();
					blobConn = cp.getConnection();
					blobConn.setAutoCommit(false);
					blobSession = StreamingHibernateUtil.getInstance().getSessionFactory().openSession(blobConn);

					copy.setId(null);
					copy.setJenis(KegiatanKemahasiswaanPunyaMahasiswa.class.getName());
					copy.setRef(kegiatanKemahasiswaanPunyaMahasiswa.getId());
					// Ganti Blob yang terikat koneksi streaming (autocommit) dengan Blob in-memory dari
					// byte[] yang telah dibaca di atas, supaya bind LO terjadi pada blobConn (non-autocommit).
					copy.setFoto(fotoBytesUntukCopy == null ? null
							: org.hibernate.Hibernate.createBlob(fotoBytesUntukCopy));

					blobSession.save(copy);
					blobSession.flush();
					blobConn.commit();
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				} finally {
					if (blobSession != null) {
						try { blobSession.clear(); } catch (Exception eignore) { ais.common.ErrorAuditUtil.record(eignore, "auto-audit(empty-catch) src/ais/action/master/KegiatanKemahasiswaanAction.java:1701");}
						try { blobSession.close(); } catch (Exception eignore) { ais.common.ErrorAuditUtil.record(eignore, "auto-audit(empty-catch) src/ais/action/master/KegiatanKemahasiswaanAction.java:1702");}
					}
					if (blobConn != null) {
						try { blobConn.setAutoCommit(true); } catch (Exception eignore) { ais.common.ErrorAuditUtil.record(eignore, "auto-audit(empty-catch) src/ais/action/master/KegiatanKemahasiswaanAction.java:1705");}
						try {
							((org.hibernate.engine.SessionFactoryImplementor) StreamingHibernateUtil.getInstance()
									.getSessionFactory()).getConnectionProvider().closeConnection(blobConn);
						} catch (Exception eignore) { ais.common.ErrorAuditUtil.record(eignore, "auto-audit(empty-catch) src/ais/action/master/KegiatanKemahasiswaanAction.java:1709");}
					}
				}
			}

		}

		return true;
	}

	/**
	 * Baca isi Blob (Large Object PostgreSQL) menjadi byte[]. WAJIB dipanggil selagi transaksi
	 * pada koneksi sumber masih AKTIF (non-autocommit), karena LO tidak boleh dibaca dalam mode
	 * auto-commit. Mengembalikan null bila blob null.
	 */
	private byte[] bacaBytesBlob(java.sql.Blob blob) throws Exception {
		if (blob == null) {
			return null;
		}
		java.io.InputStream in = null;
		java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
		try {
			in = blob.getBinaryStream();
			byte[] buf = new byte[8192];
			int n;
			while ((n = in.read(buf)) != -1) {
				bos.write(buf, 0, n);
			}
			return bos.toByteArray();
		} finally {
			if (in != null) {
				try { in.close(); } catch (Exception eignore) { ais.common.ErrorAuditUtil.record(eignore, "auto-audit(empty-catch) src/ais/action/master/KegiatanKemahasiswaanAction.java:1740");}
			}
		}
	}

	/**
	 * Menyetujui PESERTA (bukan kegiatan) yang cocok dengan penyaring NIM/nama mahasiswa di baris
	 * pencarian "Lanjutan". Dipakai oleh tombol <b>Setujui Semua</b> ketika pengguna sedang menyaring
	 * berdasarkan mahasiswa tertentu: yang disetujui adalah keikutsertaan mahasiswa tsb sebagai
	 * peserta ({@code persetujuan=true}), dan kegiatan tempat ia menjadi peserta dijadikan
	 * {@code DISETUJUI} agar persetujuan peserta itu benar-benar berlaku (karena
	 * {@code getPersetujuan()} memaksa {@code false} bila kegiatan induk belum {@code DISETUJUI}).
	 *
	 * <p>Berjalan pada {@code session} dedikasi thread latar (di-commit per baris; {@code session.clear()}
	 * tiap baris agar hemat memori). Setiap baris dibungkus try/catch (rollback bila commit gagal)
	 * supaya satu kegagalan tidak menghentikan sisanya.</p>
	 *
	 * @param session sesi Hibernate thread latar (ditutup oleh pemanggil di blok finally).
	 * @param label   label progres (boleh {@code null}).
	 */
	@SuppressWarnings("unchecked")
	private void setujuiPesertaTerfilter(Session session, Label label) {
		String nim = searchnim.getValue() == null ? "" : searchnim.getValue().trim();
		String namaMhs = searchnamamhs.getValue() == null ? "" : searchnamamhs.getValue().trim();

		List<KegiatanKemahasiswaanPunyaMahasiswa> peserta = session
				.createCriteria(KegiatanKemahasiswaanPunyaMahasiswa.class)
				.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.add(nim.isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("mahasiswa.nim", nim, MatchMode.ANYWHERE))
				.add(namaMhs.isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("mahasiswa.nama", namaMhs, MatchMode.ANYWHERE))
				.list();

		int size = peserta.size();
		int ke = 0;
		for (KegiatanKemahasiswaanPunyaMahasiswa p : peserta) {
			ke++;
			try {
				if (label != null && size > 0) {
					label.setValue(Common.numberFormat.get().format(ke * 100.0 / size) + "% .. Menyetujui peserta "
							+ (p.getMahasiswa() == null ? "" : p.getMahasiswa().getNama()));
				}

				// Pastikan kegiatan induk DISETUJUI agar persetujuan peserta berlaku.
				KegiatanKemahasiswaan keg = p.getKegiatanKemahasiswaan();
				if (keg != null && !KegiatanKemahasiswaan.DITOLAK.equals(keg.getStatus())
						&& !KegiatanKemahasiswaan.DISETUJUI.equals(keg.getStatus())) {
					keg.setStatus(KegiatanKemahasiswaan.DISETUJUI);
					session.getTransaction().begin();
					Common.refreshUpdate(session, keg);
					session.getTransaction().commit();
				}

				p.setPersetujuan(true);
				session.getTransaction().begin();
				Common.refreshUpdate(session, p);
				session.getTransaction().commit();
			} catch (Exception e) {
				try {
					if (session.getTransaction() != null && session.getTransaction().isActive()) {
						session.getTransaction().rollback();
					}
				} catch (Exception er) {
					ais.common.ErrorAuditUtil.record(er,
							"rollback-gagal src/ais/action/master/KegiatanKemahasiswaanAction.java:setujuiPeserta");
				}
				Common.tampilErrorJikaAdmin(e);
			}
			try {
				session.clear();
			} catch (Exception ig) {
				ais.common.ErrorAuditUtil.record(ig,
						"auto-audit(empty-catch) src/ais/action/master/KegiatanKemahasiswaanAction.java:setujuiPesertaClear");
			}
		}
	}

	public Criteria initCriteria(boolean order) {

		Criterion criterionMhs = Restrictions.sqlRestriction("true");
		if (!searchnim.getValue().trim().isEmpty() || !searchnamamhs.getValue().trim().isEmpty()) {
			String sql = "this_.id in (select kegiatan_kemahasiswaan from kegiatan_kemahasiswaan_punya_mahasiswa a inner join mahasiswa b on (a.mahasiswa = b.id) where kegiatan_kemahasiswaan is not null and b.nama ilike '%"
					+ searchnamamhs.getValue().trim() + "%' and b.nim ilike '%" + searchnim.getValue().trim()
					+ "%' group by kegiatan_kemahasiswaan)";
			criterionMhs = Restrictions.sqlRestriction(sql);
		}

		Criterion criterionDosenPa = Restrictions.sqlRestriction("true");
		if (searchdosen != null && searchdosen.getAttribute("dosen") != null) {
			Dosen dsn = (Dosen) searchdosen.getAttribute("dosen");
			String sql = "this_.id in (select kegiatan_kemahasiswaan from kegiatan_kemahasiswaan_punya_mahasiswa a inner join mahasiswa b on (a.mahasiswa = b.id) where kegiatan_kemahasiswaan is not null and b.dosen = "
					+ dsn.getId() + " group by kegiatan_kemahasiswaan)";
			criterionDosenPa = Restrictions.sqlRestriction(sql);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KegiatanKemahasiswaan.class);

		if (order)
			criteria.addOrder(Order.desc("id")); // pengajuan terkini di atas
		criteria.add(criterionMhs).add(criterionDosenPa)
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("jurusan"),
								CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				)
				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("jurusan"),
								CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)))

				.add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
						|| searchstatus.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("status", searchstatus.getSelectedItem().getValue()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		if (searchnama == null) {
			return;
		}
		Common.initPaging(initCriteria(false), paging);

		List<KegiatanKemahasiswaan> kegiatanKemahasiswaan = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kegiatanKemahasiswaan);
		grid.setRowRenderer(new KegiatanKemahasiswaanRenderer());
		grid.setModelCheckMobile(strset);

	}

}
