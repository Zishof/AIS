package ais.action.master.sekolah;


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
import org.zkoss.zul.Hbox;
import ais.ui.util.MyInclude;
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

import ais.action.master.dashboard.admin.DashboardRekapKegiatanKesiswaanBerdasarDetailKelompok;
import ais.action.master.dashboard.admin.DashboardRekapKegiatanKesiswaanBerdasarJabatan;
import ais.action.master.dashboard.admin.DashboardRekapKegiatanKesiswaanBerdasarKelompok;
import ais.action.master.dashboard.admin.DashboardRekapKegiatanKesiswaanBerdasarSkala;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sekolah.helper.AmbilDataGuruBanbox;
import ais.action.master.sekolah.helper.KegiatanKesiswaanPunyaSiswaHelper;
import ais.action.report.format1.akademik.LaporanPendidikanLingkunganKampus;
import ais.action.report.format1.akademik.LaporanPerKegiatanSiswa;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Perkuliahan;
import ais.database.model.Sertifikat;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.DetailKelompokKegiatanKesiswaan;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JabatanKegiatanKesiswaan;
import ais.database.model.sekolah.KegiatanKesiswaan;
import ais.database.model.sekolah.KegiatanKesiswaanPunyaSiswa;
import ais.database.model.sekolah.KelompokKegiatanKesiswaan;
import ais.database.model.sekolah.PrestasiSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.SkalaKegiatanKesiswaan;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class KegiatanKesiswaanAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchsekolah;
	private Combobox searchyayasan;
	private Combobox searchstatus;
	protected Textbox searchnamamhs;
	protected Textbox searchnim;
	protected AmbilDataGuruBanbox searchguru;

	private Textbox nama;
	private MyDatebox mulai;
	private MyDatebox sampai;
	private Combobox sekolah;
	private Combobox yayasan;

	private Combobox detailKelompokKegiatanKesiswaan;
	private Combobox kelompokKegiatanKesiswaan;

	private Textbox keterangan;

	// private boolean edit = false;
	// private boolean delete = false;

	private KegiatanKesiswaan kegiatanKesiswaan;
	private MyToolbarbuttonConfig add;

	private Tabpanel kelompokKegiatanKesiswaanTab;
	protected LampiranLain lainSiswa;
	private Tbmuser tbmuser;
	private EventListener eventListener;

	private boolean loginSebagaiPesertaAtauPengajar() {
		return tbmuser != null && (tbmuser.getMahasiswa() != null || tbmuser.getSiswa() != null
				|| tbmuser.ambilDosen() != null || tbmuser.ambilGuru() != null);
	}

	public void onKelompokKegiatanKesiswaan(Event event) {
		if (kelompokKegiatanKesiswaanTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(kelompokKegiatanKesiswaanTab);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/kelompok_kegiatan_kesiswaan.zul");
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
			DashboardRekapKegiatanKesiswaanBerdasarJabatan window = new DashboardRekapKegiatanKesiswaanBerdasarJabatan();
			ais.ui.util.BaseDasbordPortal.mountWrapped(window, rekapBerdasarJabatanTab,
				"Rekap per Jabatan", "Sebaran kegiatan kesiswaan berdasarkan jabatan siswa dalam organisasi.");
		}
	}

	private Tabpanel rekapBerdasarSkalaTab;

	public void onRekapBerdasarSkala(Event event) {
		if (rekapBerdasarSkalaTab.getChildren().size() == 0) {
			DashboardRekapKegiatanKesiswaanBerdasarSkala window = new DashboardRekapKegiatanKesiswaanBerdasarSkala();
			ais.ui.util.BaseDasbordPortal.mountWrapped(window, rekapBerdasarSkalaTab,
				"Rekap per Skala", "Sebaran kegiatan kesiswaan berdasarkan skala: sekolah, kota, nasional.");
		}
	}

	private Tabpanel rekapBerdasarKelompokTab;

	public void onRekapBerdasarKelompok(Event event) {
		if (rekapBerdasarKelompokTab.getChildren().size() == 0) {
			DashboardRekapKegiatanKesiswaanBerdasarKelompok window = new DashboardRekapKegiatanKesiswaanBerdasarKelompok();
			ais.ui.util.BaseDasbordPortal.mountWrapped(window, rekapBerdasarKelompokTab,
				"Rekap per Kelompok", "Sebaran kegiatan kesiswaan berdasarkan kelompok atau bidang ekstrakurikuler.");
		}
	}

	private Tabpanel rekapBerdasarDetailKelompokTab;
	private Textbox tempat;
	private Textbox url;

	private AmbilDataGuruBanbox guruPembina1;
	private AmbilDataGuruBanbox guruPembina2;
	private Combobox jabatanKegiatanKesiswaan;
	private Combobox skalaKegiatanKesiswaan;
	private Combobox tahunAkademik;
	private Combobox jenisSemester;
	private Combobox sertifikat;
	private MyCheckboxConfig bolehDipilih;
	private Textbox namaEn;
	private Textbox noSk;
	private MyDatebox tglSk;

	public void onRekapBerdasarDetailKelompok(Event event) {
		if (rekapBerdasarDetailKelompokTab.getChildren().size() == 0) {
			DashboardRekapKegiatanKesiswaanBerdasarDetailKelompok window = new DashboardRekapKegiatanKesiswaanBerdasarDetailKelompok();
			ais.ui.util.BaseDasbordPortal.mountWrapped(window, rekapBerdasarDetailKelompokTab,
				"Rekap Detail Kelompok", "Rincian kegiatan kesiswaan per sub-kelompok dan jenis aktivitas.");
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

			final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data kegiatan kesiswaan sedang berlangsung, harap menunggu.."));

			new Thread(new Runnable() {

				@Override
				public void run() {
					try {

					XSSFWorkbook workbook;
					try {
						workbook = new XSSFWorkbook(file.getAbsolutePath());

						for (XSSFSheet sheet : Common.getAllXSSFSheet(workbook)) {
							Session session = HibernateUtil.currentNativeSession();

							KegiatanKesiswaan kegiatanKesiswaan = (KegiatanKesiswaan) session
									.createCriteria(KegiatanKesiswaan.class)
									.add(Restrictions.ilike("kode", sheet.getSheetName().trim(), MatchMode.EXACT))
									.setMaxResults(1).uniqueResult();
							if (kegiatanKesiswaan == null) {
								kegiatanKesiswaan = new KegiatanKesiswaan();
								kegiatanKesiswaan.setNama(sheet.getSheetName().trim());
								kegiatanKesiswaan.setKode(sheet.getSheetName().trim());
								kegiatanKesiswaan.setKeterangan(sheet.getSheetName().trim());
								session.getTransaction().begin();
								session.save(kegiatanKesiswaan);
								session.getTransaction().commit();
							}

							HibernateUtil.closeSession();

							int size = (sheet.getLastRowNum() + 1);
							for (int i = 0; i < (sheet.getLastRowNum() + 1); i++) {

								session = HibernateUtil.currentNativeSession();

								try {
									Siswa siswa = null;
									try {

										String nim = Common.getCellContent(Common.getCell(sheet, 0, i));
										siswa = (Siswa) session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa","")).add(Restrictions.isNotNull("sekolah"))
												.add(Restrictions.eq("nim", nim)).setMaxResults(1).uniqueResult();

										if (siswa == null) {
											nim = Common.getCellContent(Common.getCell(sheet, 1, i));
											siswa = (Siswa) session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa","")).add(Restrictions.isNotNull("sekolah"))
													.add(Restrictions.eq("nim", nim)).setMaxResults(1).uniqueResult();
										}

										if (siswa == null) {
											nim = Common.getCellContent(Common.getCell(sheet, 2, i));
											siswa = (Siswa) session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa","")).add(Restrictions.isNotNull("sekolah"))
													.add(Restrictions.eq("nim", nim)).setMaxResults(1).uniqueResult();
										}

										if (siswa == null) {
											nim = Common.getCellContent(Common.getCell(sheet, 3, i));
											siswa = (Siswa) session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa","")).add(Restrictions.isNotNull("sekolah"))
													.add(Restrictions.eq("nim", nim)).setMaxResults(1).uniqueResult();
										}

									} catch (Exception e1) { ais.common.ErrorAuditUtil.record(e1, "auto-audit(empty-catch) src/ais/action/master/sekolah/KegiatanKesiswaanAction.java:299");

									}

									if (siswa == null) {
										continue;
									}

									Date mulai = Common.getSheetContentAsDate(sheet, 4, i);
									Date sampai = Common.getSheetContentAsDate(sheet, 5, i);

									JabatanKegiatanKesiswaan jabatanKegiatanKesiswaan = (JabatanKegiatanKesiswaan) Common
											.getSheetContentAsObject(sheet, 3, i, JabatanKegiatanKesiswaan.class);
									String keterangan = Common.getSheetContentAsString(sheet, 6, i);

									Boolean persetujuan = Common.getSheetContentAsBoolean(sheet, 8, i);

									KegiatanKesiswaanPunyaSiswa kegiatanKesiswaanPunyaSiswa = (KegiatanKesiswaanPunyaSiswa) session
											.createCriteria(KegiatanKesiswaanPunyaSiswa.class)
											.add(Restrictions.eq("siswa", siswa))
											.add(Restrictions.eq("kegiatanKesiswaan", kegiatanKesiswaan))
											.setMaxResults(1).uniqueResult();

									if (kegiatanKesiswaanPunyaSiswa == null) {
										kegiatanKesiswaanPunyaSiswa = new KegiatanKesiswaanPunyaSiswa();
									}
									kegiatanKesiswaanPunyaSiswa.setMulai(mulai);
									kegiatanKesiswaanPunyaSiswa.setSampai(sampai);
									kegiatanKesiswaanPunyaSiswa.setSiswa(siswa);
									kegiatanKesiswaanPunyaSiswa.setKegiatanKesiswaan(kegiatanKesiswaan);
									kegiatanKesiswaanPunyaSiswa.setOleh(tbmuser.getUserId());
									kegiatanKesiswaanPunyaSiswa.setTbmuser(tbmuser);
									kegiatanKesiswaanPunyaSiswa
											.setDiubahDari(KegiatanKesiswaanAction.class.getSimpleName());

									kegiatanKesiswaanPunyaSiswa.setJabatanKegiatanKesiswaan(jabatanKegiatanKesiswaan);
									kegiatanKesiswaanPunyaSiswa.setKeterangan(keterangan);
									kegiatanKesiswaanPunyaSiswa.setPersetujuan(persetujuan);

									session.getTransaction().begin();
									session.saveOrUpdate(kegiatanKesiswaanPunyaSiswa);
									session.getTransaction().commit();

									HibernateUtil.closeSession();

									label.setValue("Upload siswa " + siswa + " di kegiatan kesiswaan "
											+ kegiatanKesiswaan.getNama() + ".. "
											+ Common.numberFormat.get().format(i * 100.0 / size) + " %");

								} catch (Exception e1) {
									// TODO Auto-generated catch block

									HibernateUtil.closeSession();

									e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/sekolah/KegiatanKesiswaanAction.java:353");
								}
							}

						}

					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/sekolah/KegiatanKesiswaanAction.java:361");
					}

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
						MyMessageboxConfig.show("Update data organisasi berhasil dilakukan", "Pemberitahuan",
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

	private static void setTabDanPanelVisible(Tabpanel tabpanel, boolean visible) {
		if (tabpanel == null) {
			return;
		}
		tabpanel.setVisible(visible);
		Tab tab = tabpanel.getLinkedTab();
		if (tab != null) {
			tab.setVisible(visible);
		}
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();

		tbmuser = Common.getCurrentUser();

		try {
			if (tbmuser != null && (tbmuser.ambilGuru() != null || tbmuser.getSiswa() != null)) {
				setTabDanPanelVisible(formTab, false);
				setTabDanPanelVisible(kelompokKegiatanKesiswaanTab, false);

			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/KegiatanKesiswaanAction.java:420");
//			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		if (add != null) { add.setVisible(tbmuser != null && tbmuser.ambilGuru() == null && tbmuser.getSiswa() == null); }
		if (add != null) { add.setTooltiptext("Tambah"); }

		MyToolbarbuttonConfig ajukan = new MyToolbarbuttonConfig("Isi Form Pengajuan", "/img/print.png");
		ajukan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				LaporanPendidikanLingkunganKampus laporan = new LaporanPendidikanLingkunganKampus();
				laporan.setTitle("Pengajuan Form Kegiatan Kesiswaan");
				laporan.setClosable(true);
				laporan.setHeight("95%");
				laporan.setWidth("90%");
				laporan.setParent(page.getFirstRoot());
				laporan.onModal();
			}
		});
		if (add != null && add.getParent() != null) { ajukan.setParent(add.getParent()); }

		// edit = tbmuser.ambilGuru() == null
		// && tbmuser.getSiswa() == null;
		// delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE) &&
		// tbmuser.ambilGuru() == null
		// && tbmuser.getSiswa() == null;
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		searchguru.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		try {
			if (tbmuser != null && (tbmuser.getSiswa() != null || tbmuser.ambilGuru() != null)) {
				setTabDanPanelVisible(kelompokKegiatanKesiswaanTab, false);
				setTabDanPanelVisible(rekapBerdasarJabatanTab, false);
				setTabDanPanelVisible(rekapBerdasarSkalaTab, false);
				setTabDanPanelVisible(rekapBerdasarKelompokTab, false);
				setTabDanPanelVisible(rekapBerdasarDetailKelompokTab, false);
				setTabDanPanelVisible(formTab, false);
				if (rekap != null) { rekap.setVisible(false); }
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/KegiatanKesiswaanAction.java:477");
			// TODO: handle exception
		}

		Comboitem comboitem = new Comboitem(KegiatanKesiswaan.BELUM_DIPROSES);
		if (comboitem != null) { comboitem.setValue(KegiatanKesiswaan.BELUM_DIPROSES); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem(KegiatanKesiswaan.SEDANG_DIPROSES);
		if (comboitem != null) { comboitem.setValue(KegiatanKesiswaan.SEDANG_DIPROSES); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem(KegiatanKesiswaan.DISETUJUI);
		if (comboitem != null) { comboitem.setValue(KegiatanKesiswaan.DISETUJUI); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem(KegiatanKesiswaan.DITOLAK);
		if (comboitem != null) { comboitem.setValue(KegiatanKesiswaan.DITOLAK); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem("Semua");
		if (comboitem != null) { comboitem.setValue(null); }
		searchstatus.appendChild(comboitem);
		if (searchstatus != null) { searchstatus.setReadonly(true); }
		if (searchstatus != null) { searchstatus.setSelectedItem(comboitem); }

		String[] contents = new String[] { "id", "siswa", "kegiatanKesiswaan", "jabatanKegiatanKesiswaan",
				"skalaKegiatanKesiswaan", "mulai", "sampai", "keterangan", "persetujuan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(KegiatanKesiswaanPunyaSiswa.class,
				new DataCriteria() {

					@Override
					public Criteria initCriteria(boolean order) {

						Guru dsn = (Guru) (searchguru != null ? null : searchguru.getAttribute("guru"));

						Session session = HibernateUtil.currentSession();
						Criteria criteria = session.createCriteria(KegiatanKesiswaanPunyaSiswa.class)
								.createAlias("siswa", "siswa", Criteria.LEFT_JOIN);

						if (order)
							criteria.addOrder(Order.asc("id"));
						criteria.add(searchnim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("siswa.nim", searchnim.getValue().trim(), MatchMode.ANYWHERE))

								.add(searchnamamhs.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.ilike("siswa.nama", searchnamamhs.getValue().trim(),
												MatchMode.ANYWHERE))

								.add(dsn != null && dsn.getId() != null ? Restrictions.eq("siswa.guru", dsn.getId())
										: Restrictions.sqlRestriction("true"))

								.createCriteria("kegiatanKesiswaan")

								.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
								.add(CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))
								.add(CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false))

								.add(searchstatus.getSelectedItem() == null
										|| searchstatus.getSelectedItem().getValue() == null
										|| searchstatus.getSelectedItem().getValue() == null
												? Restrictions.sqlRestriction("1=1")
												: Restrictions.eq("status", searchstatus.getSelectedItem().getValue()));

						return criteria;
					}
				}, "Download Persetujuan Siswa", "/img/excel.png", contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KegiatanKesiswaanPunyaSiswa.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible())); }
		Common.appendKeToolbar(upload, add, comp);

		contents = loginSebagaiPesertaAtauPengajar()
				? new String[] { "id", "nama", "namaEn", "tempat", "detailKelompokKegiatanKesiswaan",
						"kelompokKegiatanKesiswaan", "mulai", "sampai", "yayasan", "sekolah", "keterangan",
						"status", "guruPembina1", "guruPembina2", "url", "jenisSemester", "tahunAkademik",
						"bolehDipilih" }
				: new String[] { "id", "nama", "namaEn", "tempat", "detailKelompokKegiatanKesiswaan",
						"kelompokKegiatanKesiswaan", "mulai", "sampai", "diajukanOleh", "yayasan", "sekolah",
						"keterangan", "status", "guruPembina1", "guruPembina2", "diajukanOleh", "url",
						"jenisSemester", "tahunAkademik", "bolehDipilih" };
		cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		upload = Common.uploadData(this, KegiatanKesiswaan.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible())); }
		Common.appendKeToolbar(upload, add, comp);

		MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("Cetak", "/img/print.png");
		cetak.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				LaporanPerKegiatanSiswa laporan = new LaporanPerKegiatanSiswa();
				laporan.setTitle("Cetak Kegiatan Siswa");
				laporan.setClosable(true);
				laporan.setHeight("95%");
				laporan.setWidth("90%");
				laporan.setParent(page.getFirstRoot());
				laporan.onModal();
			}
		});
		if (add != null && add.getParent() != null) { cetak.setParent(add.getParent()); }

		if (tbmuser.getSiswa() == null && tbmuser.getSiswa() == null) {
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Setujui Semua", "/img/svg/check2.svg");
			button.setVisible(Common.bolehKonfigurasi("aktifkan_tombol_setujui_semua_kegiatan_siswa"));
			Common.appendKeToolbar(button, add, comp);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin melakukan persetujuan semua kegiatan siswa ini ?",
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
												List<KegiatanKesiswaan> kegiatanKesiswaans = initCriteria(true)
														.add(Restrictions.ne("status", KegiatanKesiswaan.DITOLAK))
														.list();
												int size = kegiatanKesiswaans.size();
												int iverifikasi = 0;
												for (KegiatanKesiswaan kegiatanKesiswaan : kegiatanKesiswaans) {
													iverifikasi++;
													try {
														persenVeridikasi = iverifikasi * 100.0 / size;
														if (label != null) {
															label.setValue(Common.numberFormat.get().format(persenVeridikasi)
																	+ "% .. Proses Persetujuan "
																	+ kegiatanKesiswaan.getNama());

														}

														Session session = HibernateUtil.currentNativeSession();

														kegiatanKesiswaan.setStatus(KegiatanKesiswaan.DISETUJUI);
														session.getTransaction().begin();
														Common.refreshUpdate(session, kegiatanKesiswaan);
														session.getTransaction().commit();

														@SuppressWarnings("unchecked")
														List<KegiatanKesiswaanPunyaSiswa> kegiatanKesiswaanPunyaSiswas = session
																.createCriteria(KegiatanKesiswaanPunyaSiswa.class)
																.add(Restrictions.eq("kegiatanKesiswaan",
																		kegiatanKesiswaan))

																.list();
														for (KegiatanKesiswaanPunyaSiswa kegiatanKesiswaanPunyaSiswa : kegiatanKesiswaanPunyaSiswas) {
															kegiatanKesiswaanPunyaSiswa.setPersetujuan(true);
															session.getTransaction().begin();
															Common.refreshUpdate(session, kegiatanKesiswaanPunyaSiswa);
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

	        FilterLanjutHelper.setup(comp);
}

	class KegiatanKesiswaanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KegiatanKesiswaan kegiatanKesiswaan = (KegiatanKesiswaan) arg1;

			final MyDetail detail = new MyDetail();
			final EventListener detailEventListener = new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					Common.clear(detail);
					if (detail.isOpen()) {
						KegiatanKesiswaanPunyaSiswaHelper detailperkuliahanHelper = new KegiatanKesiswaanPunyaSiswaHelper();
						detailperkuliahanHelper.display(kegiatanKesiswaan, detail, addWindow);
					}
				}
			};

			detail.setParent(arg0);
			detail.addEventListener("onOpen", detailEventListener);

			new Label(kegiatanKesiswaan.getKode()).setParent(arg0);

			Vbox a = RevisiHelper.createNewRevisi(KegiatanKesiswaan.class, kegiatanKesiswaan,
					kegiatanKesiswaan.getNama());
			new Label(kegiatanKesiswaan.getNamaEn()).setParent(a);

			a.appendChild(new MyLabelAgakKecil(
					kegiatanKesiswaan.getTahunAkademik() + "/" + kegiatanKesiswaan.getJenisSemester()));
			new MyLabelAgakKecil(kegiatanKesiswaan.getTempat()).setParent(a);
			a.setParent(arg0);

			new Label(kegiatanKesiswaan.getYayasan() == null ? "Semua" : kegiatanKesiswaan.getYayasan().getNama())
					.setParent(arg0);
			new Label(kegiatanKesiswaan.getSekolah() == null ? "Semua" : kegiatanKesiswaan.getSekolah().getNama())
					.setParent(arg0);

			new Label((kegiatanKesiswaan.getGuruPembina1() == null ? "" : kegiatanKesiswaan.getGuruPembina1().getNama())
					+ (kegiatanKesiswaan.getGuruPembina2() == null ? ""
							: ", " + kegiatanKesiswaan.getGuruPembina2().getNama()))
					.setParent(arg0);

			new Label(kegiatanKesiswaan.getKelompokKegiatanKesiswaan().getNama()).setParent(arg0);
			a = new Vbox();
			a.setParent(arg0);
			new Label(kegiatanKesiswaan.getDetailKelompokKegiatanKesiswaan().getNama()).setParent(a);
			Vbox myvbox = new Vbox();
			myvbox.setParent(a);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, kegiatanKesiswaan.getId(),
					KegiatanKesiswaan.class.getName(), "Lampiran", false, null, null, false, false, false, false);

			new Label(
					kegiatanKesiswaan.getMulai() == null ? "" : Common.dateFormat1.get().format(kegiatanKesiswaan.getMulai()))
					.setParent(arg0);
			new Label(kegiatanKesiswaan.getSampai() == null ? ""
					: Common.dateFormat1.get().format(kegiatanKesiswaan.getSampai())).setParent(arg0);

			new Label(kegiatanKesiswaan.getDiajukanOleh() == null ? "Admin"
					: kegiatanKesiswaan.getDiajukanOleh().getNama()).setParent(arg0);

			new Label(kegiatanKesiswaan.getSertifikat() == null ? "-" : kegiatanKesiswaan.getSertifikat().getNama())
					.setParent(arg0);

			final Hbox toolbar = new Hbox();
			final MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Krm ke feeder",
					"/img/Finance-Invoice-icon.png");
			buttonTagihan.setStyle("font-size:8px;");
			final Hbox myHbox = new Hbox();
			myHbox.setVisible(kegiatanKesiswaan.getStatus().equals(KegiatanKesiswaan.DISETUJUI));

			if (tbmuser.getSiswa() == null) {
				final Combobox status = new Combobox();
				Comboitem comboitem = new Comboitem(KegiatanKesiswaan.BELUM_DIPROSES);
				comboitem.setValue(KegiatanKesiswaan.BELUM_DIPROSES);
				status.appendChild(comboitem);

				comboitem = new Comboitem(KegiatanKesiswaan.SEDANG_DIPROSES);
				comboitem.setValue(KegiatanKesiswaan.SEDANG_DIPROSES);
				status.appendChild(comboitem);

				comboitem = new Comboitem(KegiatanKesiswaan.DISETUJUI);
				comboitem.setValue(KegiatanKesiswaan.DISETUJUI);
				status.appendChild(comboitem);

				comboitem = new Comboitem(KegiatanKesiswaan.DITOLAK);
				comboitem.setValue(KegiatanKesiswaan.DITOLAK);
				status.appendChild(comboitem);

				Common.selectComboItem(status, kegiatanKesiswaan.getStatus());
				status.setParent(arg0);
				status.setReadonly(true);
				status.setWidth("97%");

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kegiatanKesiswaan.setStatus((String) (status.getSelectedItem() == null
								|| status.getSelectedItem().getValue() == null ? null
										: status.getSelectedItem().getValue()));
						if (arg0 != null) {
							Common.refreshUpdate(kegiatanKesiswaan);
						}
						toolbar.setVisible(!kegiatanKesiswaan.getStatus().equals(PrestasiSiswa.DISETUJUI));

						if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
								&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {
							buttonTagihan.setVisible(kegiatanKesiswaan.getStatus().equals(PrestasiSiswa.DISETUJUI));

						}
						myHbox.setVisible(kegiatanKesiswaan.getStatus().equals(PrestasiSiswa.DISETUJUI));
						Common.clear(detail);
						detail.setOpen(false);

						Common.createDefaultTimer(detailEventListener);
					}
				};
				status.addEventListener("onChange", eventListener);
				eventListener.onEvent(null);
			} else {
				new Label(kegiatanKesiswaan.getStatus()).setParent(arg0);
			}

			new Label(kegiatanKesiswaan.getKeterangan()).setParent(arg0);
			toolbar.setVisible(!kegiatanKesiswaan.getStatus().equals(PrestasiSiswa.DISETUJUI));

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			// button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(kegiatanKesiswaan);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

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
											Common.refreshDelete(kegiatanKesiswaan);
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
			Vbox vbox1 = new Vbox();
			vbox1.setParent(arg0);

			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(vbox1);

			myHbox.setParent(vbox1);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new KegiatanKesiswaan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public static void onAddExternal(EventListener eventListener, KegiatanKesiswaan kegiatanKesiswaan)
			throws Exception {
		KegiatanKesiswaanAction kegiatanKesiswaanAction = new KegiatanKesiswaanAction();
		kegiatanKesiswaanAction.eventListener = eventListener;
		kegiatanKesiswaanAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(kegiatanKesiswaanAction.addWindow);
		kegiatanKesiswaanAction.addWindow.setHeight("95%");
		kegiatanKesiswaanAction.addWindow.setWidth("850px");

		kegiatanKesiswaanAction.init(kegiatanKesiswaan);

		kegiatanKesiswaanAction.addWindow.setVisible(true);
		kegiatanKesiswaanAction.addWindow.onModal();
	}

	private void init(final KegiatanKesiswaan kegiatanKesiswaan) throws Exception {
		this.kegiatanKesiswaan = kegiatanKesiswaan;
		addWindow.setTitle(kegiatanKesiswaan.getId() == null ? "Tambah Kegiatan Kesiswaan" : "Ubah Kegiatan Kesiswaan");
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
		row.appendChild(nama = new Textbox(kegiatanKesiswaan.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kegiatan (dalam bhs inggris)"));
		row.appendChild(namaEn = new Textbox(kegiatanKesiswaan.getNamaEn()));
		namaEn.setWidth("90%");
		namaEn.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Kegiatan"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		hbox.setWidth("90%");

		mulai = new MyDatebox(kegiatanKesiswaan.getMulai());
		sampai = new MyDatebox(kegiatanKesiswaan.getSampai());

		hbox.appendChild(mulai);
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));
		hbox.appendChild(sampai);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Guru Pembina I"));
		row.appendChild(guruPembina1 = new AmbilDataGuruBanbox());
		guruPembina1.setAttribute("myValue", kegiatanKesiswaan.getGuruPembina1());
		guruPembina1.setAttribute("guru", kegiatanKesiswaan.getGuruPembina1());
		guruPembina1.setValue(
				kegiatanKesiswaan.getGuruPembina1() == null ? "" : kegiatanKesiswaan.getGuruPembina1().getNama());
		guruPembina1.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Guru Pembina II"));
		row.appendChild(guruPembina2 = new AmbilDataGuruBanbox());
		guruPembina2.setAttribute("myValue", kegiatanKesiswaan.getGuruPembina2());
		guruPembina2.setAttribute("guru", kegiatanKesiswaan.getGuruPembina2());
		guruPembina2.setValue(
				kegiatanKesiswaan.getGuruPembina2() == null ? "" : kegiatanKesiswaan.getGuruPembina2().getNama());
		guruPembina2.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tempat / Alamat Kegiatan *"));
		row.appendChild(tempat = new Textbox(kegiatanKesiswaan.getTempat()));
		tempat.setWidth("90%");
		tempat.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aspek Kegiatan *"));
		row.appendChild(kelompokKegiatanKesiswaan = new Combobox());
		kelompokKegiatanKesiswaan.setWidth("90%");
		kelompokKegiatanKesiswaan.setReadonly(true);

		Common.insertCombo(kelompokKegiatanKesiswaan, "nama", "keterangan", KelompokKegiatanKesiswaan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				tbmuser != null && tbmuser.getSiswa() != null ? Restrictions.or(Restrictions.isNull("bisaDipilihSiswa"),
						Restrictions.eq("bisaDipilihSiswa", true)) : Restrictions.sqlRestriction("true"));
		Common.selectComboItem(kelompokKegiatanKesiswaan, kegiatanKesiswaan.getKelompokKegiatanKesiswaan());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Rincian Aspek Kegiatan *"));
		row.appendChild(detailKelompokKegiatanKesiswaan = new Combobox());
		detailKelompokKegiatanKesiswaan.setWidth("90%");
		detailKelompokKegiatanKesiswaan.setReadonly(true);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(detailKelompokKegiatanKesiswaan);
				if (kelompokKegiatanKesiswaan.getSelectedItem() != null
						&& kelompokKegiatanKesiswaan.getSelectedItem().getValue() != null) {
					Common.insertCombo(detailKelompokKegiatanKesiswaan, "nama", DetailKelompokKegiatanKesiswaan.class,
							Restrictions.and(
									Restrictions.eq("kelompokKegiatanKesiswaan",
											kelompokKegiatanKesiswaan.getSelectedItem().getValue()),
									Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							tbmuser != null && tbmuser.getSiswa() != null
									? Restrictions.or(Restrictions.isNull("bisaDipilihSiswa"),
											Restrictions.eq("bisaDipilihSiswa", true))
									: Restrictions.sqlRestriction("true"));
					Common.selectComboItem(detailKelompokKegiatanKesiswaan,
							kegiatanKesiswaan.getDetailKelompokKegiatanKesiswaan());
				}

			}
		};

		kelompokKegiatanKesiswaan.addEventListener("onChange", eventListener);
		eventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(bolehDipilih = new MyCheckboxConfig("Kegiatan ini bisa dipilih oleh siswa lainnya"));
		bolehDipilih.setChecked(kegiatanKesiswaan.getBolehDipilih());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jabatan/Status *"));
		row.appendChild(jabatanKegiatanKesiswaan = new Combobox());
		jabatanKegiatanKesiswaan.setWidth("90%");
		jabatanKegiatanKesiswaan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Skala *"));
		row.appendChild(skalaKegiatanKesiswaan = new Combobox());
		skalaKegiatanKesiswaan.setWidth("90%");
		skalaKegiatanKesiswaan.setReadonly(true);

		EventListener detail = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(jabatanKegiatanKesiswaan);
				Common.clear(skalaKegiatanKesiswaan);
				DetailKelompokKegiatanKesiswaan keguruan = (DetailKelompokKegiatanKesiswaan) (detailKelompokKegiatanKesiswaan
						.getSelectedItem() == null ? null
								: detailKelompokKegiatanKesiswaan.getSelectedItem().getValue());
				if (keguruan != null) {
					HibernateUtil.currentSession().refresh(keguruan);
					List<JabatanKegiatanKesiswaan> jabatanKegiatanKesiswaans = new ArrayList<JabatanKegiatanKesiswaan>(
							keguruan.getJabatanKegiatanKesiswaans());
					List<SkalaKegiatanKesiswaan> skalaKegiatanKesiswaans = new ArrayList<SkalaKegiatanKesiswaan>(
							keguruan.getSkalaKegiatanKesiswaans());

					Collections.sort(jabatanKegiatanKesiswaans);
					Collections.sort(skalaKegiatanKesiswaans);

					Common.insertComboItems(jabatanKegiatanKesiswaan, "nama", jabatanKegiatanKesiswaans);
					Common.insertComboItems(skalaKegiatanKesiswaan, "nama", skalaKegiatanKesiswaans);

					Common.selectComboItem(jabatanKegiatanKesiswaan, kegiatanKesiswaan.getJabatanKegiatanKesiswaan());
					Common.selectComboItem(skalaKegiatanKesiswaan, kegiatanKesiswaan.getSkalaKegiatanKesiswaan());

				}

			}
		};

		detailKelompokKegiatanKesiswaan.addEventListener("onChange", detail);
		detail.onEvent(null);

		Common.initYayasanDanSekolahDanSemua(yayasan = new Combobox(), sekolah = new Combobox(), null, null);
		if (kegiatanKesiswaan.getYayasan() == null && tbmuser != null && tbmuser.ambilYayasan() != null) {
			kegiatanKesiswaan.setYayasan(tbmuser.ambilYayasan());
		}
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		row.appendChild(yayasan);
		Common.selectComboItem(yayasan, kegiatanKesiswaan.getYayasan());
		yayasan.setWidth("90%");

		if (yayasan.getSelectedItem() != null && yayasan.getSelectedItem().getValue() != null) {
			Common.insertComboDanSemua(sekolah, new String[] { "nama", "kodeEpsbed" }, "jenjang", Sekolah.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("yayasan", yayasan, false));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(sekolah);
		sekolah.setWidth("90%");
		Common.pilihSekolah(sekolah, kegiatanKesiswaan.getSekolah());

		if (kegiatanKesiswaan.getSekolah() == null) {
			if (tbmuser.ambilSekolah() != null
					|| (tbmuser.getSiswa() != null && tbmuser.getSiswa().getSekolah() != null)) {
				Common.pilihSekolah(sekolah,
						tbmuser == null || tbmuser.ambilSekolah() == null ? tbmuser.getSiswa().getSekolah()
								: tbmuser.ambilSekolah());
				sekolah.setDisabled(true);
			} else {
				sekolah.setDisabled(false);
			}
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("URL Kegiatan"));
		row.appendChild(url = new Textbox(kegiatanKesiswaan.getUrl()));
		url.setWidth("90%");

		Common.generateTahunAjaran(tahunAkademik = new Combobox());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Ajaran *"));
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		Common.selectComboItem(tahunAkademik, kegiatanKesiswaan.getTahunAkademik());

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

		Common.selectComboItem(jenisSemester, kegiatanKesiswaan.getJenisSemester());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester *"));
		row.appendChild(jenisSemester);
		jenisSemester.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor SK Kegiatan"));
		row.appendChild(noSk = new Textbox(kegiatanKesiswaan.getNoSk()));
		noSk.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal SK Kegiatan"));
		row.appendChild(tglSk = new MyDatebox(kegiatanKesiswaan.getTglSk()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(kegiatanKesiswaan.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sertifikat"));
		row.appendChild(sertifikat = new Combobox());
		Common.insertComboDanSemua(sertifikat, new String[] { "nama" }, "keterangan", Sertifikat.class,
				"== Tanpa Sertifikat ==");
		Common.selectComboItem(sertifikat, kegiatanKesiswaan.getSertifikat());
		sertifikat.setWidth("90%");
		sertifikat.setReadonly(true);

		if (tbmuser != null && tbmuser.getSiswa() != null) {
			sertifikat.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lampiran Kegiatan"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, kegiatanKesiswaan.getId(), KegiatanKesiswaan.class.getName(),
				"Lampiran Kegiatan", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainSiswa = (LampiranLain) arg0.getData();
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

					if (KegiatanKesiswaanAction.this.eventListener != null) {
						KegiatanKesiswaanAction.this.eventListener.onEvent(event);
					}
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Kegiatan Kesiswaan  harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (tempat.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Tempat Kegiatan Kesiswaan  harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (kelompokKegiatanKesiswaan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Aspek Kegiatan Kesiswaan  harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (detailKelompokKegiatanKesiswaan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Rincian Aspek Kegiatan Kesiswaan  harus diisi", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (jabatanKegiatanKesiswaan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Jabatan Kegiatan Guru  harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (skalaKegiatanKesiswaan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Skala Kegiatan Guru  harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (tahunAkademik.getSelectedItem() == null) {
			MyMessageboxConfig.show("Tahun Ajaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (jenisSemester.getSelectedItem() == null) {
			MyMessageboxConfig.show("Jenis Semester harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (kegiatanKesiswaan.getId() != null) {
			kegiatanKesiswaan = (KegiatanKesiswaan) session.load(KegiatanKesiswaan.class, kegiatanKesiswaan.getId());

		}
		kegiatanKesiswaan.setJabatanKegiatanKesiswaan(
				(JabatanKegiatanKesiswaan) jabatanKegiatanKesiswaan.getSelectedItem().getValue());
		kegiatanKesiswaan.setSkalaKegiatanKesiswaan(
				(SkalaKegiatanKesiswaan) skalaKegiatanKesiswaan.getSelectedItem().getValue());

		kegiatanKesiswaan.setGuruPembina1((Guru) guruPembina1.getAttribute("guru"));
		kegiatanKesiswaan.setGuruPembina2((Guru) guruPembina2.getAttribute("guru"));
		kegiatanKesiswaan.setTempat(tempat.getValue());
		kegiatanKesiswaan.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null ? null
						: yayasan.getSelectedItem().getValue()));
		kegiatanKesiswaan.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null ? null
						: sekolah.getSelectedItem().getValue()));
		kegiatanKesiswaan.setUrl(url.getValue());
		kegiatanKesiswaan.setMulai(mulai.getValue());
		kegiatanKesiswaan.setSampai(sampai.getValue());
		kegiatanKesiswaan.setNama(nama.getValue());
		kegiatanKesiswaan.setNamaEn(namaEn.getValue());

		kegiatanKesiswaan.setDetailKelompokKegiatanKesiswaan(
				(DetailKelompokKegiatanKesiswaan) detailKelompokKegiatanKesiswaan.getSelectedItem().getValue());
		kegiatanKesiswaan.setKelompokKegiatanKesiswaan(
				(KelompokKegiatanKesiswaan) kelompokKegiatanKesiswaan.getSelectedItem().getValue());
		kegiatanKesiswaan.setKeterangan(keterangan.getValue());

		kegiatanKesiswaan.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		kegiatanKesiswaan.setJenisSemester((String) jenisSemester.getSelectedItem().getValue());

		kegiatanKesiswaan.setSertifikat(
				(Sertifikat) (sertifikat.getSelectedItem() == null ? null : sertifikat.getSelectedItem().getValue()));
		kegiatanKesiswaan.setBolehDipilih(bolehDipilih.isChecked());
		kegiatanKesiswaan.setNoSk(noSk.getValue());
		kegiatanKesiswaan.setTglSk(tglSk.getValue());

		if (tbmuser != null) {
			kegiatanKesiswaan.setDiajukanOleh(tbmuser.getSiswa());
		}
		Common.refreshSaveOrUpdate(session, kegiatanKesiswaan);
		KegiatanKesiswaanPunyaSiswa kegiatanKesiswaanPunyaSiswa = null;
		if (kegiatanKesiswaan.getDiajukanOleh() != null) {
			kegiatanKesiswaanPunyaSiswa = (KegiatanKesiswaanPunyaSiswa) session
					.createCriteria(KegiatanKesiswaanPunyaSiswa.class)
					.add(Restrictions.eq("kegiatanKesiswaan", kegiatanKesiswaan))
					.add(Restrictions.eq("siswa", kegiatanKesiswaan.getDiajukanOleh())).setMaxResults(1).uniqueResult();
			if (kegiatanKesiswaanPunyaSiswa == null) {
				kegiatanKesiswaanPunyaSiswa = new KegiatanKesiswaanPunyaSiswa();
				kegiatanKesiswaanPunyaSiswa.setKegiatanKesiswaan(kegiatanKesiswaan);
				kegiatanKesiswaanPunyaSiswa.setOleh(tbmuser == null ? null : tbmuser.getUserId());
				kegiatanKesiswaanPunyaSiswa.setTbmuser(tbmuser);
				kegiatanKesiswaanPunyaSiswa.setSiswa(kegiatanKesiswaan.getDiajukanOleh());
				kegiatanKesiswaanPunyaSiswa.setDiubahDari(SiswaAction.class.getSimpleName());
				Common.refreshSaveOrUpdate(session, kegiatanKesiswaanPunyaSiswa);

			}
		}

		if (lainSiswa != null && lainSiswa.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainSiswa);
				lainSiswa.setRef(kegiatanKesiswaan.getId());

				session.getTransaction().begin();
				session.update(lainSiswa);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

			if (kegiatanKesiswaanPunyaSiswa != null) {
				LampiranLain copy = (LampiranLain) lainSiswa.clone();
				try {
					session = StreamingHibernateUtil.getInstance().currentSession();

					copy.setId(null);
					copy.setJenis(KegiatanKesiswaanPunyaSiswa.class.getName());
					copy.setRef(kegiatanKesiswaanPunyaSiswa.getId());

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
		if (!searchnim.getValue().trim().isEmpty() || !searchnamamhs.getValue().trim().isEmpty()) {
			String sql = "this_.id in (select kegiatan_kesiswaan from sekolah.kegiatan_kesiswaan_punya_siswa a inner join sekolah.siswa b on (a.siswa = b.id) where kegiatan_kesiswaan is not null and b.nama_siswa ilike '%"
					+ searchnamamhs.getValue().trim() + "%' and b.nim ilike '%" + searchnim.getValue().trim()
					+ "%' group by kegiatan_kesiswaan)";
			criterionMhs = Restrictions.sqlRestriction(sql);
		}

		Criterion criterionGuruPa = Restrictions.sqlRestriction("true");
		if (searchguru != null && searchguru.getAttribute("guru") != null) {
			Guru dsn = (Guru) searchguru.getAttribute("guru");
			String sql = "this_.id in (select kegiatan_kesiswaan from sekolah.kegiatan_kesiswaan_punya_siswa a inner join sekolah.siswa b on (a.siswa = b.id) where kegiatan_kesiswaan is not null and (b.guru_pembina = "
					+ dsn.getId() + " or b.guru_bk = " + dsn.getId() + ") group by kegiatan_kesiswaan)";
			criterionGuruPa = Restrictions.sqlRestriction(sql);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KegiatanKesiswaan.class);

		if (order)
			criteria.addOrder(Order.desc("id")); // pengajuan terkini di atas
		criteria.add(criterionMhs).add(criterionGuruPa)
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))
				.add(CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false))

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

		List<KegiatanKesiswaan> kegiatanKesiswaan = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kegiatanKesiswaan);
		grid.setRowRenderer(new KegiatanKesiswaanRenderer());
		grid.setModelCheckMobile(strset);

	}

}
