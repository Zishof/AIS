package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.dashboard.admin.DashboardRekapKegiatanKedosenanBerdasarDetailKelompok;
import ais.action.master.dashboard.admin.DashboardRekapKegiatanKedosenanBerdasarJabatan;
import ais.action.master.dashboard.admin.DashboardRekapKegiatanKedosenanBerdasarKelompok;
import ais.action.master.dashboard.admin.DashboardRekapKegiatanKedosenanBerdasarSkala;
import ais.action.master.helper.KegiatanKedosenanPunyaDosenHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.report.format1.akademik.LaporanPerKegiatanDosen;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.UploadReportHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.DetailKelompokKegiatanKedosenan;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.JabatanKegiatanKedosenan;
import ais.database.model.Jurusan;
import ais.database.model.KegiatanKedosenan;
import ais.database.model.KegiatanKedosenanPunyaDosen;
import ais.database.model.KelompokKegiatanKedosenan;
import ais.database.model.Konfigurasi;
import ais.database.model.Perkuliahan;
import ais.database.model.PrestasiDosen;
import ais.database.model.Sertifikat;
import ais.database.model.SkalaKegiatanKedosenan;
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
import ais.ui.util.MyLabelKecilSekali;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class KegiatanKedosenanAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

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

	private Textbox nama;
	private Textbox tempat;
	private MyDatebox mulai;
	private MyDatebox sampai;
	private Combobox jurusan;
	private Combobox fakultas;

	private Combobox detailKelompokKegiatanKedosenan;
	private Combobox kelompokKegiatanKedosenan;

	private Textbox keterangan;

	// private boolean edit = false;
	// private boolean delete = false;

	private KegiatanKedosenan kegiatanKedosenan;
	private MyToolbarbuttonConfig add;

	private Tabpanel kelompokKegiatanKedosenanTab;
	protected LampiranLain lainDosen;
	private Tbmuser tbmuser;
	private EventListener eventListener;

	private boolean loginSebagaiPesertaAtauPengajar() {
		return tbmuser != null && (tbmuser.getMahasiswa() != null || tbmuser.getSiswa() != null
				|| tbmuser.ambilDosen() != null || tbmuser.ambilGuru() != null);
	}

	private Tabpanel tabDasbor;

	public void onDasbor(Event event) {
		if (tabDasbor != null && tabDasbor.getChildren().size() == 0) {
			ais.action.master.prestasi.DasbordKegiatanKedosenan dasbor = new ais.action.master.prestasi.DasbordKegiatanKedosenan();
			ais.ui.util.BaseDasbordPortal.mountWrapped(dasbor, tabDasbor,
				"Kegiatan Kedosenan",
				"Ringkasan dan tren kegiatan yang dilakukan dosen.");
		}
	}

	public void onKelompokKegiatanKedosenan(Event event) {
		if (kelompokKegiatanKedosenanTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(kelompokKegiatanKedosenanTab);
			MyInclude iframe = new MyInclude("/pages/master/kelompok_kegiatan_kedosenan.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel rekapBerdasarJabatanTab;

	public void onRekapBerdasarJabatan(Event event) {
		if (rekapBerdasarJabatanTab.getChildren().size() == 0) {
			DashboardRekapKegiatanKedosenanBerdasarJabatan window = new DashboardRekapKegiatanKedosenanBerdasarJabatan();
			ais.ui.util.BaseDasbordPortal.mountWrapped(window, rekapBerdasarJabatanTab,
				"Rekap per Jabatan", "Sebaran kegiatan kedosenan berdasarkan jabatan fungsional dosen.");
		}
	}

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

	private Tabpanel rekapBerdasarSkalaTab;

	public void onRekapBerdasarSkala(Event event) {
		if (rekapBerdasarSkalaTab.getChildren().size() == 0) {
			DashboardRekapKegiatanKedosenanBerdasarSkala window = new DashboardRekapKegiatanKedosenanBerdasarSkala();
			ais.ui.util.BaseDasbordPortal.mountWrapped(window, rekapBerdasarSkalaTab,
				"Rekap per Skala", "Sebaran kegiatan kedosenan berdasarkan skala: lokal, nasional, internasional.");
		}
	}

	private Tabpanel rekapBerdasarKelompokTab;

	public void onRekapBerdasarKelompok(Event event) {
		if (rekapBerdasarKelompokTab.getChildren().size() == 0) {
			DashboardRekapKegiatanKedosenanBerdasarKelompok window = new DashboardRekapKegiatanKedosenanBerdasarKelompok();
			ais.ui.util.BaseDasbordPortal.mountWrapped(window, rekapBerdasarKelompokTab,
				"Rekap per Kelompok", "Sebaran kegiatan kedosenan berdasarkan kelompok bidang tridharma.");
		}
	}

	private Tabpanel rekapBerdasarDetailKelompokTab;
	private Textbox url;
	private Combobox jabatanKegiatanKedosenan;
	private Combobox skalaKegiatanKedosenan;
	private Combobox tahunAkademik;
	private Combobox jenisSemester;
	private Combobox sertifikat;
	private MyCheckboxConfig bolehDipilih;
	private Textbox namaEn;

	public void onRekapBerdasarDetailKelompok(Event event) {
		if (rekapBerdasarDetailKelompokTab.getChildren().size() == 0) {
			DashboardRekapKegiatanKedosenanBerdasarDetailKelompok window = new DashboardRekapKegiatanKedosenanBerdasarDetailKelompok();
			ais.ui.util.BaseDasbordPortal.mountWrapped(window, rekapBerdasarDetailKelompokTab,
				"Rekap Detail Kelompok", "Rincian kegiatan kedosenan per sub-kelompok dan jenis aktivitas.");
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

			final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data kegiatan kedosenan sedang berlangsung, harap menunggu.."));
			final UploadReportHelper report = new UploadReportHelper("Upload Kegiatan Kedosenan");
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

							KegiatanKedosenan kegiatanKedosenan = (KegiatanKedosenan) session
									.createCriteria(KegiatanKedosenan.class)
									.add(Restrictions.ilike("kode", sheet.getSheetName().trim(), MatchMode.EXACT))
									.setMaxResults(1).uniqueResult();
							if (kegiatanKedosenan == null) {
								kegiatanKedosenan = new KegiatanKedosenan();
								kegiatanKedosenan.setNama(sheet.getSheetName().trim());
								kegiatanKedosenan.setKode(sheet.getSheetName().trim());
								kegiatanKedosenan.setKeterangan(sheet.getSheetName().trim());
								session.getTransaction().begin();
								session.save(kegiatanKedosenan);
								session.getTransaction().commit();
							}

							HibernateUtil.closeSession();

							int size = (sheet.getLastRowNum() + 1);
							for (int i = 0; i < (sheet.getLastRowNum() + 1); i++) {

								session = HibernateUtil.currentNativeSession();

								try {
									Dosen dosen = null;
									try {
										String nim = Common.getCellContent(Common.getCell(sheet, 0, i));
										dosen = (Dosen) session.createCriteria(Dosen.class)
												.add(Restrictions.eq("nim", nim)).setMaxResults(1).uniqueResult();

										if (dosen == null) {
											nim = Common.getCellContent(Common.getCell(sheet, 1, i));
											dosen = (Dosen) session.createCriteria(Dosen.class)
													.add(Restrictions.eq("nim", nim)).setMaxResults(1).uniqueResult();
										}

										if (dosen == null) {
											nim = Common.getCellContent(Common.getCell(sheet, 2, i));
											dosen = (Dosen) session.createCriteria(Dosen.class)
													.add(Restrictions.eq("nim", nim)).setMaxResults(1).uniqueResult();
										}

										if (dosen == null) {
											nim = Common.getCellContent(Common.getCell(sheet, 3, i));
											dosen = (Dosen) session.createCriteria(Dosen.class)
													.add(Restrictions.eq("nim", nim)).setMaxResults(1).uniqueResult();
										}

									} catch (Exception e1) { ais.common.ErrorAuditUtil.record(e1, "auto-audit(empty-catch) src/ais/action/master/KegiatanKedosenanAction.java:298");

									}

									if (dosen == null) {
										continue;
									}

									Date mulai = Common.getSheetContentAsDate(sheet, 4, i);
									Date sampai = Common.getSheetContentAsDate(sheet, 5, i);

									JabatanKegiatanKedosenan jabatanKegiatanKedosenan = (JabatanKegiatanKedosenan) Common
											.getSheetContentAsObject(sheet, 3, i, JabatanKegiatanKedosenan.class);
									String keterangan = Common.getSheetContentAsString(sheet, 6, i);

									Boolean persetujuan = Common.getSheetContentAsBoolean(sheet, 8, i);

									KegiatanKedosenanPunyaDosen kegiatanKedosenanPunyaDosen = (KegiatanKedosenanPunyaDosen) session
											.createCriteria(KegiatanKedosenanPunyaDosen.class)
											.add(Restrictions.eq("dosen", dosen))
											.add(Restrictions.eq("kegiatanKedosenan", kegiatanKedosenan))
											.setMaxResults(1).uniqueResult();

									if (kegiatanKedosenanPunyaDosen == null) {
										kegiatanKedosenanPunyaDosen = new KegiatanKedosenanPunyaDosen();
									}
									kegiatanKedosenanPunyaDosen.setMulai(mulai);
									kegiatanKedosenanPunyaDosen.setSampai(sampai);
									kegiatanKedosenanPunyaDosen.setDosen(dosen);
									kegiatanKedosenanPunyaDosen.setKegiatanKedosenan(kegiatanKedosenan);
									kegiatanKedosenanPunyaDosen.setOleh(tbmuser.getUserId());
									kegiatanKedosenanPunyaDosen.setTbmuser(tbmuser);
									kegiatanKedosenanPunyaDosen
											.setDiubahDari(KegiatanKedosenanAction.class.getSimpleName());

									kegiatanKedosenanPunyaDosen.setJabatanKegiatanKedosenan(jabatanKegiatanKedosenan);
									kegiatanKedosenanPunyaDosen.setKeterangan(keterangan);
									kegiatanKedosenanPunyaDosen.setPersetujuan(persetujuan);

									session.getTransaction().begin();
									session.saveOrUpdate(kegiatanKedosenanPunyaDosen);
									session.getTransaction().commit();

									HibernateUtil.closeSession();

									label.setValue("Upload dosen " + dosen + " di kegiatan kedosenan "
											+ kegiatanKedosenan.getNama() + ".. "
											+ Common.numberFormat.get().format(i * 100.0 / size) + " %");
									report.sukses(i, dosen.getNidn() + "@" + kegiatanKedosenan.getNama(), "Kegiatan Kedosenan berhasil diproses");

								} catch (Exception e1) {
									// TODO Auto-generated catch block

									HibernateUtil.closeSession();

									e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/KegiatanKedosenanAction.java:352");
									report.gagal(i, "baris-" + i, e1, "Periksa data NIDN/kegiatan pada baris ini");
								}
							}

						}

					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/KegiatanKedosenanAction.java:360");
					}

					try {
						downloadPath.setValue(report.simpanLaporan().getAbsolutePath());
					} catch (java.io.IOException eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) src/ais/action/master/KegiatanKedosenanAction.java:369"); }
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

		// add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		// add.setTooltiptext("Tambah");

		// edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		// delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onDasbor(null);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		Comboitem comboitem = new Comboitem(KegiatanKedosenan.BELUM_DIPROSES);
		if (comboitem != null) { comboitem.setValue(KegiatanKedosenan.BELUM_DIPROSES); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem(KegiatanKedosenan.SEDANG_DIPROSES);
		if (comboitem != null) { comboitem.setValue(KegiatanKedosenan.SEDANG_DIPROSES); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem(KegiatanKedosenan.DISETUJUI);
		if (comboitem != null) { comboitem.setValue(KegiatanKedosenan.DISETUJUI); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem(KegiatanKedosenan.DITOLAK);
		if (comboitem != null) { comboitem.setValue(KegiatanKedosenan.DITOLAK); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem("Semua");
		if (comboitem != null) { comboitem.setValue(null); }
		searchstatus.appendChild(comboitem);
		if (searchstatus != null) { searchstatus.setReadonly(true); }
		if (searchstatus != null) { searchstatus.setSelectedItem(comboitem); }

		String[] contents = new String[] { "id", "dosen", "kegiatanKedosenan", "jabatanKegiatanKedosenan",
				"skalaKegiatanKedosenan", "mulai", "sampai", "keterangan", "persetujuan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(KegiatanKedosenanPunyaDosen.class,
				new DataCriteria() {

					@Override
					public Criteria initCriteria(boolean order) {

						Session session = HibernateUtil.currentSession();
						Criteria criteria = session.createCriteria(KegiatanKedosenanPunyaDosen.class)
								.createAlias("dosen", "dosen", Criteria.LEFT_JOIN);

						if (order)
							criteria.addOrder(Order.asc("id"));
						criteria.add(searchnim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.or(
										Restrictions.ilike("dosen.code", searchnim.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.or(
												Restrictions.ilike("dosen.mycode", searchnim.getValue().trim(),
														MatchMode.ANYWHERE),
												Restrictions.ilike("dosen.nidn", searchnim.getValue().trim(),
														MatchMode.ANYWHERE))))

								.add(searchnamamhs.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.ilike("dosen.nama", searchnamamhs.getValue().trim(),
												MatchMode.ANYWHERE))

								.createCriteria("kegiatanKedosenan")

								.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
								.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))
								.add(CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))

								.add(searchstatus.getSelectedItem() == null
										|| searchstatus.getSelectedItem().getValue() == null
										|| searchstatus.getSelectedItem().getValue() == null
												? Restrictions.sqlRestriction("1=1")
												: Restrictions.eq("status", searchstatus.getSelectedItem().getValue()));

						return criteria;
					}
				}, "Download Persetujuan Dosen", "/img/excel.png", contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KegiatanKedosenanPunyaDosen.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible())); }
		Common.appendKeToolbar(upload, add, comp);

		contents = loginSebagaiPesertaAtauPengajar()
				? new String[] { "id", "nama", "namaEn", "tempat", "detailKelompokKegiatanKedosenan",
						"kelompokKegiatanKedosenan", "mulai", "sampai", "fakultas", "jurusan", "keterangan",
						"status", "url", "tahunAkademik", "jenisSemester", "bolehDipilih" }
				: new String[] { "id", "nama", "namaEn", "tempat", "detailKelompokKegiatanKedosenan",
						"kelompokKegiatanKedosenan", "mulai", "sampai", "diajukanOleh", "fakultas", "jurusan",
						"keterangan", "status", "diajukanOleh", "url", "tahunAkademik", "jenisSemester",
						"bolehDipilih" };
		cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		upload = Common.uploadData(this, KegiatanKedosenan.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible())); }
		Common.appendKeToolbar(upload, add, comp);

		MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("Cetak Kegiatan Dosen", "/img/print.png");
		cetak.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				LaporanPerKegiatanDosen laporan = new LaporanPerKegiatanDosen();
				laporan.setTitle("Cetak Kegiatan Dosen");
				laporan.setClosable(true);
				laporan.setHeight("95%");
				laporan.setWidth("90%");
				laporan.setParent(page.getFirstRoot());
				laporan.onModal();
			}
		});
		if (cetak != null) { cetak.setParent(add.getParent()); }

		if (tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null) {
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Setujui Semua", "/img/svg/check2.svg");
			button.setVisible(Common.bolehKonfigurasi("aktifkan_tombol_setujui_semua_kegiatan_dosen"));
			Common.appendKeToolbar(button, add, comp);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin melakukan persetujuan semua kegiatan dosen ini ?",
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
												try {
												double persenVeridikasi = 0.0;
												@SuppressWarnings("unchecked")
												List<KegiatanKedosenan> kegiatanKedosenans = initCriteria(true)
														.add(Restrictions.ne("status", KegiatanKedosenan.DITOLAK))
														.list();
												int size = kegiatanKedosenans.size();
												int iverifikasi = 0;
												for (KegiatanKedosenan kegiatanKedosenan : kegiatanKedosenans) {
													iverifikasi++;
													try {
														persenVeridikasi = iverifikasi * 100.0 / size;
														if (label != null) {
															label.setValue(Common.numberFormat.get().format(persenVeridikasi)
																	+ "% .. Proses Persetujuan "
																	+ kegiatanKedosenan.getNama());

														}

														Session session = HibernateUtil.currentNativeSession();

														kegiatanKedosenan.setStatus(KegiatanKedosenan.DISETUJUI);
														session.getTransaction().begin();
														Common.refreshUpdate(session, kegiatanKedosenan);
														session.getTransaction().commit();

														@SuppressWarnings("unchecked")
														List<KegiatanKedosenanPunyaDosen> kegiatanKedosenanPunyaDosens = session
																.createCriteria(KegiatanKedosenanPunyaDosen.class)
																.add(Restrictions.eq("kegiatanKedosenan",
																		kegiatanKedosenan))

																.list();
														for (KegiatanKedosenanPunyaDosen kegiatanKedosenanPunyaDosen : kegiatanKedosenanPunyaDosens) {
															kegiatanKedosenanPunyaDosen.setPersetujuan(true);
															session.getTransaction().begin();
															Common.refreshUpdate(session, kegiatanKedosenanPunyaDosen);
															session.getTransaction().commit();
														}
													} catch (Exception e) {
														ais.common.Common.tampilErrorJikaAdmin(e);
													}
													HibernateUtil.closeSession();
												}
												label.setValue("");
																							} finally {
													ais.database.hibernate.HibernateUtil.closeSession();
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

	}

	class KegiatanKedosenanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KegiatanKedosenan kegiatanKedosenan = (KegiatanKedosenan) arg1;

			final MyDetail detail = new MyDetail();
			final EventListener detailEventListener = new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					Common.clear(detail);
					if (detail.isOpen()) {
						KegiatanKedosenanPunyaDosenHelper detailperkuliahanHelper = new KegiatanKedosenanPunyaDosenHelper();
						detailperkuliahanHelper.display(kegiatanKedosenan, detail, addWindow);
					}
				}
			};

			detail.setParent(arg0);
			detail.addEventListener("onOpen", detailEventListener);

			new Label(kegiatanKedosenan.getKode()).setParent(arg0);

			Vbox a = RevisiHelper.createNewRevisi(KegiatanKedosenan.class, kegiatanKedosenan,
					kegiatanKedosenan.getNama());

			new MyLabelKecilSekali(kegiatanKedosenan.getNamaEn()).setParent(a);

			a.appendChild(new MyLabelAgakKecil(
					kegiatanKedosenan.getTahunAkademik() + "/" + kegiatanKedosenan.getJenisSemester()));
			new MyLabelAgakKecil(kegiatanKedosenan.getTempat()).setParent(a);
			a.setParent(arg0);

			new Label(kegiatanKedosenan.getFakultas() == null ? "Semua" : kegiatanKedosenan.getFakultas().getNama())
					.setParent(arg0);
			new Label(kegiatanKedosenan.getJurusan() == null ? "Semua" : kegiatanKedosenan.getJurusan().getNama())
					.setParent(arg0);

			new MyLabelAgakKecil(kegiatanKedosenan.getKelompokKegiatanKedosenan().getNama()).setParent(arg0);
			a = new Vbox();
			a.setParent(arg0);
			new MyLabelAgakKecil(kegiatanKedosenan.getDetailKelompokKegiatanKedosenan().getNama()).setParent(a);
			Vbox myvbox = new Vbox();
			myvbox.setParent(a);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, kegiatanKedosenan.getId(),
					KegiatanKedosenan.class.getName(), "Lampiran", false, null, null, false, false, false, false);

			new Label(
					kegiatanKedosenan.getMulai() == null ? "" : Common.dateFormat1.get().format(kegiatanKedosenan.getMulai()))
					.setParent(arg0);
			new Label(kegiatanKedosenan.getSampai() == null ? ""
					: Common.dateFormat1.get().format(kegiatanKedosenan.getSampai())).setParent(arg0);

			new Label(kegiatanKedosenan.getDiajukanOleh() == null ? "Admin"
					: kegiatanKedosenan.getDiajukanOleh().getNama()).setParent(arg0);

			new Label(kegiatanKedosenan.getSertifikat() == null ? "-" : kegiatanKedosenan.getSertifikat().getNama())
					.setParent(arg0);

			final Hbox toolbar = new Hbox();

			boolean bolehMensetujui = false;
			if (tbmuser.ambilDosen() != null) {
				bolehMensetujui = ((Number) HibernateUtil.currentSession()
						.createCriteria(KegiatanKedosenanPunyaDosen.class).createAlias("dosen", "dosen")
						.add(Restrictions.eq("kegiatanKedosenan", kegiatanKedosenan))
						.add(Restrictions.eq("dosen.atasanlangsung", tbmuser.getDosen().getId()))
						.setProjection(Projections.rowCount()).uniqueResult()).intValue() > 0;

				System.out.println("Dosen " + tbmuser.ambilDosen() + ", kegiatan " + kegiatanKedosenan
						+ ", bolehMensetujui " + bolehMensetujui);
			}

			if (tbmuser != null && ((tbmuser.ambilFakultas() != null && kegiatanKedosenan.getFakultas() == null)
					|| (tbmuser.ambilJurusan() != null && kegiatanKedosenan.getJurusan() == null))) {
				new Label(kegiatanKedosenan.getStatus()).setParent(arg0);
			} else if (tbmuser.ambilDosen() == null || bolehMensetujui) {
				final Combobox status = new Combobox();
				Comboitem comboitem = new Comboitem(KegiatanKedosenan.BELUM_DIPROSES);
				comboitem.setValue(KegiatanKedosenan.BELUM_DIPROSES);
				status.appendChild(comboitem);

				comboitem = new Comboitem(KegiatanKedosenan.SEDANG_DIPROSES);
				comboitem.setValue(KegiatanKedosenan.SEDANG_DIPROSES);
				status.appendChild(comboitem);

				comboitem = new Comboitem(KegiatanKedosenan.DISETUJUI);
				comboitem.setValue(KegiatanKedosenan.DISETUJUI);
				status.appendChild(comboitem);

				comboitem = new Comboitem(KegiatanKedosenan.DITOLAK);
				comboitem.setValue(KegiatanKedosenan.DITOLAK);
				status.appendChild(comboitem);

				Common.selectComboItem(status, kegiatanKedosenan.getStatus());
				status.setParent(arg0);
				status.setReadonly(true);
				status.setWidth("97%");

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kegiatanKedosenan.setStatus((String) (status.getSelectedItem() == null
								|| status.getSelectedItem().getValue() == null ? null
										: status.getSelectedItem().getValue()));
						if (arg0 != null) {
							Common.refreshUpdate(kegiatanKedosenan);
						}
						toolbar.setVisible(!kegiatanKedosenan.getStatus().equals(PrestasiDosen.DISETUJUI));

						Common.clear(detail);
						detail.setOpen(false);

						Common.createDefaultTimer(detailEventListener);
					}
				};
				status.addEventListener("onChange", eventListener);
				eventListener.onEvent(null);
			} else {
				new Label(kegiatanKedosenan.getStatus()).setParent(arg0);
			}

			new Label(kegiatanKedosenan.getKeterangan()).setParent(arg0);
			toolbar.setVisible(!kegiatanKedosenan.getStatus().equals(PrestasiDosen.DISETUJUI));

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			// button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(kegiatanKedosenan);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			if (tbmuser != null && ((tbmuser.ambilFakultas() != null && kegiatanKedosenan.getFakultas() == null)
					|| (tbmuser.ambilJurusan() != null && kegiatanKedosenan.getJurusan() == null))) {
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
											Common.refreshDelete(kegiatanKedosenan);
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

			if (tbmuser != null && ((tbmuser.ambilFakultas() != null && kegiatanKedosenan.getFakultas() == null)
					|| (tbmuser.ambilJurusan() != null && kegiatanKedosenan.getJurusan() == null))) {
				button.setVisible(false);
			}
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new KegiatanKedosenan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public static void onAddExternal(EventListener eventListener, KegiatanKedosenan kegiatanKedosenan)
			throws Exception {
		KegiatanKedosenanAction kegiatanKedosenanAction = new KegiatanKedosenanAction();
		kegiatanKedosenanAction.eventListener = eventListener;
		kegiatanKedosenanAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(kegiatanKedosenanAction.addWindow);
		kegiatanKedosenanAction.addWindow.setHeight("95%");
		kegiatanKedosenanAction.addWindow.setWidth("850px");

		kegiatanKedosenanAction.init(kegiatanKedosenan);

		kegiatanKedosenanAction.addWindow.setVisible(true);
		kegiatanKedosenanAction.addWindow.onModal();
	}

	private void init(final KegiatanKedosenan kegiatanKedosenan) throws Exception {
		this.kegiatanKedosenan = kegiatanKedosenan;
		addWindow.setTitle(kegiatanKedosenan.getId() == null ? "Tambah Kegiatan Dosen" : "Ubah Kegiatan Dosen");
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
		row.appendChild(nama = new Textbox(kegiatanKedosenan.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kegiatan (dalam bhs inggris)"));
		row.appendChild(namaEn = new Textbox(kegiatanKedosenan.getNamaEn()));
		namaEn.setWidth("90%");
		namaEn.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Kegiatan"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		hbox.setWidth("90%");

		mulai = new MyDatebox(kegiatanKedosenan.getMulai());
		sampai = new MyDatebox(kegiatanKedosenan.getSampai());

		hbox.appendChild(mulai);
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));
		hbox.appendChild(sampai);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tempat Kegiatan *"));
		row.appendChild(tempat = new Textbox(kegiatanKedosenan.getTempat()));
		tempat.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Kegiatan *"));
		final Combobox jenisKelompokKegiatanKedosenan;
		row.appendChild(jenisKelompokKegiatanKedosenan = new Combobox());
		jenisKelompokKegiatanKedosenan.setWidth("90%");
		jenisKelompokKegiatanKedosenan.setReadonly(true);

		for (String s : new String[] { KelompokKegiatanKedosenan.BIDANG_PENDIDIKAN,
				KelompokKegiatanKedosenan.BIDANG_PENELITIAN, KelompokKegiatanKedosenan.BIDANG_PENGABDIAN,
				KelompokKegiatanKedosenan.BIDANG_PENUNJANG }) {
			Comboitem comboitem = new Comboitem(s);
			comboitem.setValue(s);
			jenisKelompokKegiatanKedosenan.appendChild(comboitem);
		}

		Comboitem comboitem = new Comboitem("Semua");
		comboitem.setValue(null);
		jenisKelompokKegiatanKedosenan.appendChild(comboitem);
		jenisKelompokKegiatanKedosenan.setReadonly(true);

		Common.selectComboItem(jenisKelompokKegiatanKedosenan,
				kegiatanKedosenan.getKelompokKegiatanKedosenan() == null ? null
						: kegiatanKedosenan.getKelompokKegiatanKedosenan().getJenis());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aspek Kegiatan *"));
		row.appendChild(kelompokKegiatanKedosenan = new Combobox());
		kelompokKegiatanKedosenan.setWidth("90%");
		kelompokKegiatanKedosenan.setReadonly(true);

		EventListener myEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				String j = (String) (jenisKelompokKegiatanKedosenan.getSelectedItem() == null ? null
						: jenisKelompokKegiatanKedosenan.getSelectedItem().getValue());
				Common.insertCombo(kelompokKegiatanKedosenan, new String[] { "nama", "jenis" }, "keterangan",
						KelompokKegiatanKedosenan.class,
						j == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("jenis", j),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
								? Restrictions.or(Restrictions.isNull("bisaDipilihDosen"),
										Restrictions.eq("bisaDipilihDosen", true))
								: Restrictions.sqlRestriction("true"));
				Common.selectComboItem(kelompokKegiatanKedosenan, kegiatanKedosenan.getKelompokKegiatanKedosenan());
			}
		};

		myEventListener.onEvent(null);

		jenisKelompokKegiatanKedosenan.addEventListener("onChange", myEventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Rincian Aspek Kegiatan *"));
		row.appendChild(detailKelompokKegiatanKedosenan = new Combobox());
		detailKelompokKegiatanKedosenan.setWidth("90%");
		detailKelompokKegiatanKedosenan.setReadonly(true);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(detailKelompokKegiatanKedosenan);
				if (kelompokKegiatanKedosenan.getSelectedItem() != null
						&& kelompokKegiatanKedosenan.getSelectedItem().getValue() != null) {
					Common.insertCombo(detailKelompokKegiatanKedosenan, "nama", DetailKelompokKegiatanKedosenan.class,
							Restrictions.and(
									Restrictions.eq("kelompokKegiatanKedosenan",
											kelompokKegiatanKedosenan.getSelectedItem().getValue()),
									Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
									? Restrictions.or(Restrictions.isNull("bisaDipilihDosen"),
											Restrictions.eq("bisaDipilihDosen", true))
									: Restrictions.sqlRestriction("true"));
					Common.selectComboItem(detailKelompokKegiatanKedosenan,
							kegiatanKedosenan.getDetailKelompokKegiatanKedosenan());

				}

			}
		};

		kelompokKegiatanKedosenan.addEventListener("onChange", eventListener);
		eventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(bolehDipilih = new MyCheckboxConfig("Kegiatan ini bisa dipilih oleh dosen lainnya"));
		bolehDipilih.setChecked(kegiatanKedosenan.getBolehDipilih());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jabatan/Status *"));
		row.appendChild(jabatanKegiatanKedosenan = new Combobox());
		jabatanKegiatanKedosenan.setWidth("90%");
		jabatanKegiatanKedosenan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Skala *"));
		row.appendChild(skalaKegiatanKedosenan = new Combobox());
		skalaKegiatanKedosenan.setWidth("90%");
		skalaKegiatanKedosenan.setReadonly(true);

		EventListener detail = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(jabatanKegiatanKedosenan);
				Common.clear(skalaKegiatanKedosenan);
				DetailKelompokKegiatanKedosenan kedosenan = (DetailKelompokKegiatanKedosenan) (detailKelompokKegiatanKedosenan
						.getSelectedItem() == null ? null
								: detailKelompokKegiatanKedosenan.getSelectedItem().getValue());
				if (kedosenan != null) {
					HibernateUtil.currentSession().refresh(kedosenan);
					List<JabatanKegiatanKedosenan> jabatanKegiatanKedosenans = new ArrayList<JabatanKegiatanKedosenan>(
							kedosenan.getJabatanKegiatanKedosenans());
					List<SkalaKegiatanKedosenan> skalaKegiatanKedosenans = new ArrayList<SkalaKegiatanKedosenan>(
							kedosenan.getSkalaKegiatanKedosenans());

					Collections.sort(jabatanKegiatanKedosenans);
					Collections.sort(skalaKegiatanKedosenans);

					Common.insertComboItems(jabatanKegiatanKedosenan, "nama", jabatanKegiatanKedosenans);
					Common.insertComboItems(skalaKegiatanKedosenan, "nama", skalaKegiatanKedosenans);

					Common.selectComboItem(jabatanKegiatanKedosenan, kegiatanKedosenan.getJabatanKegiatanKedosenan());
					Common.selectComboItem(skalaKegiatanKedosenan, kegiatanKedosenan.getSkalaKegiatanKedosenan());
				}

			}
		};

		detailKelompokKegiatanKedosenan.addEventListener("onChange", detail);
		detail.onEvent(null);

		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		if (kegiatanKedosenan.getFakultas() == null && tbmuser.ambilFakultas() != null) {
			kegiatanKedosenan.setFakultas(tbmuser.ambilFakultas());
		}
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		Common.selectComboItem(fakultas, kegiatanKedosenan.getFakultas());
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
		Common.pilihJurusan(jurusan, kegiatanKedosenan.getJurusan());

		if (kegiatanKedosenan.getJurusan() == null) {
			if (tbmuser.ambilJurusan() != null
					|| (tbmuser.ambilDosen() != null && tbmuser.ambilDosen().getJurusan() != null)) {
				Common.pilihJurusan(jurusan,
						tbmuser == null || tbmuser.ambilJurusan() == null ? tbmuser.ambilDosen().getJurusan()
								: tbmuser.ambilJurusan());
				jurusan.setDisabled(true);
			} else {
				jurusan.setDisabled(false);
			}
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("URL Kegiatan"));
		row.appendChild(url = new Textbox(kegiatanKedosenan.getUrl()));
		url.setWidth("90%");

		Common.generateTahunAjaran(tahunAkademik = new Combobox());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		Common.selectComboItem(tahunAkademik, kegiatanKedosenan.getTahunAkademik());

		jenisSemester = new Combobox();
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		jenisSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		jenisSemester.appendChild(comboitem);
		jenisSemester.setSelectedIndex(1);
		row.appendChild(jenisSemester);
		jenisSemester.setWidth("90%");
		jenisSemester.setReadonly(true);

		Common.selectComboItem(jenisSemester, kegiatanKedosenan.getJenisSemester());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester *"));
		row.appendChild(jenisSemester);
		jenisSemester.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(kegiatanKedosenan.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sertifikat"));
		row.appendChild(sertifikat = new Combobox());
		Common.insertComboDanSemua(sertifikat, new String[] { "nama" }, "keterangan", Sertifikat.class,
				"== Tanpa Sertifikat ==");
		Common.selectComboItem(sertifikat, kegiatanKedosenan.getSertifikat());
		sertifikat.setWidth("90%");
		sertifikat.setReadonly(true);

		if (tbmuser != null && (tbmuser.getMahasiswa() != null || tbmuser.ambilDosen() != null)) {
			sertifikat.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lampiran Kegiatan"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, kegiatanKedosenan.getId(), KegiatanKedosenan.class.getName(),
				"Lampiran Kegiatan", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainDosen = (LampiranLain) arg0.getData();
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

					if (KegiatanKedosenanAction.this.eventListener != null) {
						KegiatanKedosenanAction.this.eventListener.onEvent(event);
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
		kotaCount = ((Number) session.createCriteria(KegiatanKedosenan.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.kegiatanKedosenan.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.kegiatanKedosenan.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kegiatan Dosen",
					"Kolom Nama Kegiatan Dosen belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Kegiatan Dosen.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (tempat.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tempat Kegiatan Dosen",
					"Kolom Tempat Kegiatan Dosen belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tempat Kegiatan Dosen.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (kelompokKegiatanKedosenan.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Aspek Kegiatan Dosen",
					"Kolom Aspek Kegiatan Dosen belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Aspek Kegiatan Dosen.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (detailKelompokKegiatanKedosenan.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Rincian Aspek Kegiatan Dosen",
					"Kolom Rincian Aspek Kegiatan Dosen belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Rincian Aspek Kegiatan Dosen.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (jabatanKegiatanKedosenan.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jabatan Kegiatan Dosen",
					"Kolom Jabatan Kegiatan Dosen belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jabatan Kegiatan Dosen.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (skalaKegiatanKedosenan.getSelectedItem() == null) {
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

		Session session = HibernateUtil.currentSession();
		if (kegiatanKedosenan.getId() != null) {
			kegiatanKedosenan = (KegiatanKedosenan) session.load(KegiatanKedosenan.class, kegiatanKedosenan.getId());

		}
		kegiatanKedosenan.setJabatanKegiatanKedosenan(
				(JabatanKegiatanKedosenan) jabatanKegiatanKedosenan.getSelectedItem().getValue());
		kegiatanKedosenan.setSkalaKegiatanKedosenan(
				(SkalaKegiatanKedosenan) skalaKegiatanKedosenan.getSelectedItem().getValue());

		kegiatanKedosenan.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		kegiatanKedosenan.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		kegiatanKedosenan.setUrl(url.getValue());
		kegiatanKedosenan.setTempat(tempat.getValue());
		kegiatanKedosenan.setMulai(mulai.getValue());
		kegiatanKedosenan.setSampai(sampai.getValue());
		kegiatanKedosenan.setNama(nama.getValue());
		kegiatanKedosenan.setNamaEn(namaEn.getValue());
		kegiatanKedosenan.setDetailKelompokKegiatanKedosenan(
				(DetailKelompokKegiatanKedosenan) detailKelompokKegiatanKedosenan.getSelectedItem().getValue());
		kegiatanKedosenan.setKelompokKegiatanKedosenan(
				(KelompokKegiatanKedosenan) kelompokKegiatanKedosenan.getSelectedItem().getValue());
		kegiatanKedosenan.setKeterangan(keterangan.getValue());

		kegiatanKedosenan.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		kegiatanKedosenan.setJenisSemester((String) jenisSemester.getSelectedItem().getValue());
		kegiatanKedosenan.setSertifikat(
				(Sertifikat) (sertifikat.getSelectedItem() == null ? null : sertifikat.getSelectedItem().getValue()));
		kegiatanKedosenan.setBolehDipilih(bolehDipilih.isChecked());
		if (tbmuser != null) {
			kegiatanKedosenan.setDiajukanOleh(tbmuser.ambilDosen());
		}
		Common.refreshSaveOrUpdate(session, kegiatanKedosenan);
		KegiatanKedosenanPunyaDosen kegiatanKedosenanPunyaDosen = null;
		if (kegiatanKedosenan.getDiajukanOleh() != null) {
			kegiatanKedosenanPunyaDosen = (KegiatanKedosenanPunyaDosen) session
					.createCriteria(KegiatanKedosenanPunyaDosen.class)
					.add(Restrictions.eq("kegiatanKedosenan", kegiatanKedosenan))
					.add(Restrictions.eq("dosen", kegiatanKedosenan.getDiajukanOleh())).setMaxResults(1).uniqueResult();
			if (kegiatanKedosenanPunyaDosen == null) {
				kegiatanKedosenanPunyaDosen = new KegiatanKedosenanPunyaDosen();
				kegiatanKedosenanPunyaDosen.setKegiatanKedosenan(kegiatanKedosenan);
				kegiatanKedosenanPunyaDosen.setOleh(tbmuser == null ? null : tbmuser.getUserId());
				kegiatanKedosenanPunyaDosen.setTbmuser(tbmuser);
				kegiatanKedosenanPunyaDosen.setDosen(kegiatanKedosenan.getDiajukanOleh());
				kegiatanKedosenanPunyaDosen.setDiubahDari(DosenAction.class.getSimpleName());
				Common.refreshSaveOrUpdate(session, kegiatanKedosenanPunyaDosen);

			}
		}

		if (lainDosen != null && lainDosen.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainDosen);
				lainDosen.setRef(kegiatanKedosenan.getId());

				session.getTransaction().begin();
				session.update(lainDosen);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

			if (kegiatanKedosenanPunyaDosen != null) {
				LampiranLain copy = (LampiranLain) lainDosen.clone();
				try {
					session = StreamingHibernateUtil.getInstance().currentSession();

					copy.setId(null);
					copy.setJenis(KegiatanKedosenanPunyaDosen.class.getName());
					copy.setRef(kegiatanKedosenanPunyaDosen.getId());

					session.getTransaction().begin();
					session.save(copy);
					session.getTransaction().commit();

					StreamingHibernateUtil.getInstance().closeSession();
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}
			}

		}

		return true;
	}

	public Criteria initCriteria(boolean order) {

		Criterion criterionMhs = Restrictions.sqlRestriction("true");
		Criterion criterionDsn = Restrictions.sqlRestriction("true");
		Dosen currDsn = tbmuser == null ? null : tbmuser.ambilDosen();
		if (currDsn != null) {
			String sql = "this_.id in (select kegiatan_kedosenan from kegiatan_kedosenan_punya_dosen a inner join dosen b on (a.dosen = b.id) where kegiatan_kedosenan is not null and (a.dosen="
					+ currDsn.getId() + " or b.atasanlangsung =" + currDsn.getId() + ") group by kegiatan_kedosenan)";
			System.out.println("criterionDsn sql => " + sql);
			criterionDsn = Restrictions.sqlRestriction(sql);
		}

		else if (!searchnim.getValue().trim().isEmpty() || !searchnamamhs.getValue().trim().isEmpty()) {
			String sql = "this_.id in (select kegiatan_kedosenan from kegiatan_kedosenan_punya_dosen a inner join dosen b on (a.dosen = b.id) where kegiatan_kedosenan is not null and b.nama ilike '%"
					+ searchnamamhs.getValue().trim() + "%' and b.nidn ilike '%" + searchnim.getValue().trim()
					+ "%' group by kegiatan_kedosenan)";
			criterionMhs = Restrictions.sqlRestriction(sql);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KegiatanKedosenan.class);

		if (order)
			criteria.addOrder(Order.desc("id")); // pengajuan terkini di atas
		criteria.add(criterionMhs).add(criterionDsn)
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("jurusan"),
								CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)))

				.add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
						|| searchstatus.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("status", searchstatus.getSelectedItem().getValue()))

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

		List<KegiatanKedosenan> kegiatanKedosenan = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kegiatanKedosenan);
		grid.setRowRenderer(new KegiatanKedosenanRenderer());
		grid.setModelCheckMobile(strset);

	}

}
