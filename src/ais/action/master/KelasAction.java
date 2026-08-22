package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
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
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
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
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.KelasPunyaMahasiswaHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.report.format1.akademik.LaporanRekapitulasiKelas;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.UploadReportHelper;
import ais.database.dao.DaoFactory;
import ais.database.dao.KelasDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Kelas;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class KelasAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Decimalbox searchtahun;
	private AmbilDataDosenBanbox searchdosen;
	private Checkbox searchaktif;

	private Textbox nama;
	private Combobox fakultas;
	private Combobox jurusan;
	private Textbox keterangan;
	private AmbilDataDosenBanbox dosenPaDefault;
	private MyCheckboxConfig updateDosenPaSekarang;

	private boolean edit = false;
	private boolean delete = false;

	private Kelas kelas;
	private MyToolbarbuttonConfig add;
	private Decimalbox tahunAngkatan;

	private MyToolbarbuttonConfig uploadData;

	private Tabpanel laporanKelas;

	public void onTampilKelas(Event event) {
		if (laporanKelas.getChildren().size() == 0) {
			LaporanRekapitulasiKelas laporanRekapitulasiKelas = new LaporanRekapitulasiKelas();
			laporanRekapitulasiKelas.setHeight("100%");
			laporanRekapitulasiKelas.setWidth("100%");
			laporanRekapitulasiKelas.setParent(laporanKelas);
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	@SuppressWarnings("rawtypes")
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (searchdosen != null) {
			searchdosen.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(null);
				}
			});
		}

		Session session = HibernateUtil.currentSession();

		int count = ((Number) session.createCriteria(Kelas.class).setProjection(Projections.rowCount()).uniqueResult())
				.intValue();
		if (count == 0) {

			List kelases = session.createCriteria(Perkuliahan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.isNotNull("kelas")).add(Restrictions.ne("kelas", ""))
					.setProjection(Projections.groupProperty("kelas")).list();

			for (Object k : kelases) {
				if (k != null) {
					Kelas kelas = new Kelas();
					kelas.setNama(k.toString());
					kelas.setKeterangan("Kelas " + k.toString());
					session.save(kelas);
				}
			}

		}

		count = ((Number) session.createCriteria(Kelas.class).setProjection(Projections.rowCount()).uniqueResult())
				.intValue();
		if (count == 0) {
			Kelas kelas = new Kelas();
			kelas.setNama("A");
			kelas.setKeterangan("Kelas A");
			session.save(kelas);
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

		MyToolbarbuttonConfig cetakToolbarbutton = cetakDataCustomButton("Download Kelas", "/img/print.png");
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		if (uploadData != null) {
			ais.database.model.Konfigurasi konfigUpload = Common.getKonfigurasi("boleh_upload_data_kelas", Konfigurasi.AKTIF);
			uploadData.setVisible(konfigUpload != null && Konfigurasi.AKTIF.equals(konfigUpload.getNilai()));
		}

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Singkronkan", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				final MyWindow addWindow = new MyWindow();
				addWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				addWindow.setTitle("Simpan Dosen PA");
				addWindow.setWidth("500px");
				addWindow.setHeight("300px");
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
				row.appendChild(new ais.ui.util.MyLabelConfig("Mulai dari semester :"));
				final Combobox formStartSemester = new Combobox();
				MyComboitemConfig comboitem;
				for (int i = 1; i <= 8; i++) {
					comboitem = new MyComboitemConfig();
					comboitem.setLabel("semester saat ini - " + i);
					comboitem.setValue(i);
					formStartSemester.appendChild(comboitem);
				}
				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Semester saat ini");
				comboitem.setValue(null);
				formStartSemester.appendChild(comboitem);
				Common.selectComboItem(formStartSemester, null);

				row.appendChild(formStartSemester);
				formStartSemester.setReadonly(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Sampai dengan semester :"));
				final Combobox formEndSemester = new Combobox();
				for (int i = 1; i <= 8; i++) {
					comboitem = new MyComboitemConfig();
					comboitem.setLabel("semester saat ini + " + i);
					comboitem.setValue(i);
					formEndSemester.appendChild(comboitem);
				}
				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Semester saat ini");
				comboitem.setValue(null);
				formEndSemester.appendChild(comboitem);
				row.appendChild(formEndSemester);
				formEndSemester.setReadonly(true);
				Common.selectComboItem(formEndSemester, null);

				row = new MyFormRow();
				row.setParent(rows);
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
						addWindow.detach();
					}
				});
				cancel.setTooltiptext("keluar");
				cancel.setParent(toolbar);
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
				save.setTooltiptext("Simpan");
				save.addEventListener("onClick", new EventListener() {
					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						addWindow.detach();

						final Label label = Common.displayLoadBar(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						});

						final List<Kelas> kelases = initCriteria(true).list();
						// Laporan rinci per kelas (berhasil/gagal+penyebab teknis lengkap) - dulu
						// hasil tiap kelas hanya dicatat ke tampilErrorJikaAdmin internal, tanpa
						// popup akhir sama sekali.
						final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Kelas dengan KRS");
						final java.util.concurrent.atomic.AtomicInteger nomorBarisLaporan = new java.util.concurrent.atomic.AtomicInteger(0);
						new Thread(new Runnable() {

							@Override
							public void run() {
								try {

								Session session = HibernateUtil.currentNativeSession();
								for (Kelas kelas : kelases) {
									String kunciKelas = kelas == null || kelas.getNama() == null ? "-" : kelas.getNama();
									try {
										Criteria criteria = session.createCriteria(Mahasiswa.class)
												.add(Restrictions.or(Restrictions.isNull("aktif"),
														Restrictions.eq("aktif", true)))
												.add(kelas != null && !kelas.getNama().trim().isEmpty()
														? Restrictions.ilike("kelas", kelas.getNama().trim(),
																MatchMode.EXACT)
														: Restrictions.sqlRestriction("false"));

										List<Mahasiswa> mahasiswas = criteria.list();

										int rowIndex = 1;
										for (Mahasiswa mahasiswa : mahasiswas) {
											label.setValue("Sedang memproses data " + mahasiswa.toString() + " ("
													+ Common.numberFormat.get().format(rowIndex * 100.0 / mahasiswas.size())
													+ " %)");

											Integer mulai = (Integer) (formStartSemester.getSelectedItem() == null
													|| formStartSemester.getSelectedItem().getValue() == null
															? mahasiswa.currentSemester()
															: mahasiswa.currentSemester() - ((Integer) formStartSemester
																	.getSelectedItem().getValue()));
											Integer sampai = (Integer) (formEndSemester.getSelectedItem() == null
													|| formEndSemester.getSelectedItem().getValue() == null
															? mahasiswa.currentSemester()
															: mahasiswa.currentSemester() + ((Integer) formEndSemester
																	.getSelectedItem().getValue()));

											if (mulai < 1) {
												mulai = 1;
											}

											for (Integer smt = mulai; smt <= sampai; smt++) {
												KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa,
														smt, null, null);
												KrsMahasiswa krsMahasiswaSp = Common.singkronkanKrsMahasiswa(mahasiswa,
														smt, null, Perkuliahan.SEMESTER_PENDEK);
												krsMahasiswa.setKelas(kelas.getNama());
												krsMahasiswaSp.setKelas(kelas.getNama());

												session.getTransaction().begin();
												Common.refreshUpdate(session, krsMahasiswa);
												Common.refreshUpdate(session, krsMahasiswaSp);
												session.getTransaction().commit();

											}

											rowIndex++;
										}
										mahasiswas.clear();
										laporan.catatBerhasil(nomorBarisLaporan.getAndIncrement(), kunciKelas, "Sinkronisasi berhasil");
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										laporan.catatGagalDetail(nomorBarisLaporan.getAndIncrement(), kunciKelas, e);
									}
								}
								HibernateUtil.closeSession();
								label.setValue("");
								laporan.selesaikan(null);
															} finally {
									ais.database.hibernate.HibernateUtil.closeSession();
								}
							}
						}).start();

					}
				});
				save.setTooltiptext("simpan");
				save.setParent(toolbar);
				borderlayout.setParent(addWindow);

				addWindow.setVisible(true);
				addWindow.onModal();

			}

		});
		Common.appendKeToolbar(button, add, comp);
	        FilterLanjutHelper.setup(comp);
}

	public void onUploadData(Event event) throws Exception {

		ForwardEvent forwardEvent = (ForwardEvent) event;
		Media media = ((UploadEvent) forwardEvent.getOrigin()).getMedia();
		if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
			return;
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

			final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data kelas sedang berlangsung, harap menunggu.."));
			final UploadReportHelper report = new UploadReportHelper("Upload Kelas");
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

							Kelas kelas = (Kelas) session.createCriteria(Kelas.class)
									.add(Restrictions.ilike("nama", sheet.getSheetName().trim(), MatchMode.EXACT))
									.setMaxResults(1).uniqueResult();
							if (kelas == null) {
								kelas = new Kelas();
								kelas.setNama(sheet.getSheetName().trim());
								kelas.setKeterangan(sheet.getSheetName().trim());
								session.getTransaction().begin();
								session.save(kelas);
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
										mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class)
												.add(Restrictions.or(Restrictions.isNull("aktif"),
														Restrictions.eq("aktif", true)))
												.add(Restrictions.eq("nim", nim)).setMaxResults(1).uniqueResult();

										if (mahasiswa == null) {
											nim = Common.getCellContent(Common.getCell(sheet, 1, i));
											mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class)
													.add(Restrictions.or(Restrictions.isNull("aktif"),
															Restrictions.eq("aktif", true)))
													.add(Restrictions.eq("nim", nim)).setMaxResults(1).uniqueResult();
										}

										if (mahasiswa == null) {
											nim = Common.getCellContent(Common.getCell(sheet, 2, i));
											mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class)
													.add(Restrictions.or(Restrictions.isNull("aktif"),
															Restrictions.eq("aktif", true)))
													.add(Restrictions.eq("nim", nim)).setMaxResults(1).uniqueResult();
										}

										if (mahasiswa == null) {
											nim = Common.getCellContent(Common.getCell(sheet, 3, i));
											mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class)
													.add(Restrictions.or(Restrictions.isNull("aktif"),
															Restrictions.eq("aktif", true)))
													.add(Restrictions.eq("nim", nim)).setMaxResults(1).uniqueResult();
										}

									} catch (Exception e1) { ais.common.ErrorAuditUtil.record(e1, "auto-audit(empty-catch) src/ais/action/master/KelasAction.java:489");

									}

									if (mahasiswa == null) {
										continue;
									}

									mahasiswa.setKelas(kelas.getNama());

									session.getTransaction().begin();
									session.save(mahasiswa);
									session.getTransaction().commit();

									HibernateUtil.closeSession();

									label.setValue("Upload mahasiswa " + mahasiswa + " di kelas " + kelas.getNama()
											+ ".. " + Common.numberFormat.get().format(i * 100.0 / size) + " %");
									report.sukses(i, mahasiswa.getNim(), "Kelas " + kelas.getNama());

								} catch (Exception e1) {
									// TODO Auto-generated catch block

									HibernateUtil.closeSession();

									e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/KelasAction.java:513");
									report.gagal(i, "baris-" + i, e1, "Periksa data NIM pada baris ini");
								}
							}

							session = HibernateUtil.currentNativeSession();
							HibernateUtil.closeSession();
						}

					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/KelasAction.java:523");
					}

					try {
						downloadPath.setValue(report.simpanLaporan().getAbsolutePath());
					} catch (java.io.IOException eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) src/ais/action/master/KelasAction.java:540"); }
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
						MyMessageboxConfig.show("Update data kelas berhasil dilakukan. " + report.getRingkasan(), "Pemberitahuan",
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

	class KelasRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Kelas kelas = (Kelas) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					Common.clear(detail);
					if (detail.isOpen()) {
						KelasPunyaMahasiswaHelper detailperkuliahanHelper = new KelasPunyaMahasiswaHelper();
						detailperkuliahanHelper.display(kelas, detail, addWindow);
					}
				}
			});

			RevisiHelper.createNewRevisi(Kelas.class, kelas, kelas.getNama()).setParent(arg0);
			new Label(kelas.getFakultas() == null ? "Semua" : kelas.getFakultas().getNama()).setParent(arg0);
			new Label(kelas.getJurusan() == null ? "Semua" : kelas.getJurusan().getNama()).setParent(arg0);
			new Label(kelas.getTahunAngkatan() == null ? "Semua" : kelas.getTahunAngkatan() + "").setParent(arg0);

			new Label(kelas.getDosenPaDefault() == null ? "" : kelas.getDosenPaDefault().getNama()).setParent(arg0);

			new Label(kelas.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(kelas.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kelas.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(kelas);
				}
			});

			int count = ((Number) HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.ilike("kelas", kelas.getNama(), MatchMode.EXACT))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			new Label(Common.numberFormat.get().format(count)).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(kelas);
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

											Common.refreshDelete(kelas);

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
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new Kelas());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(Kelas kelas) {
		this.kelas = kelas;

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

		addWindow.setTitle(kelas.getId() == null ? "Tambah Kelas" : "Ubah Kelas");
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
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kelas"));
		row.appendChild(nama = new Textbox(kelas.getNama() == null ? "" : kelas.getNama()));
		nama.setWidth("90%");

		Tbmuser tbmuser = Common.getCurrentUser();

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.selectComboItem(fakultas, kelas.getFakultas() == null ? tbmuser.ambilFakultas() : kelas.getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.pilihJurusan(jurusan, kelas.getJurusan() == null ? tbmuser.ambilJurusan() : kelas.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		Common.initKeterangan(rows, "(Kosongkan " + Common.getBahasaConfig("Jurusan")
				+ " jika kelas ini berlaku untuk semua " + Common.getBahasaConfig("Jurusan") + ")");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan"));
		row.appendChild(tahunAngkatan = new Decimalbox(
				kelas.getTahunAngkatan() == null ? null : new BigDecimal(kelas.getTahunAngkatan())));
		tahunAngkatan.setWidth("90%");

		Common.initKeterangan(rows, "(Kosongkan tahun angkatan jika kelas ini berlaku untuk semua tahun angkatan)");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen PA Default"));
		row.appendChild(dosenPaDefault = new AmbilDataDosenBanbox());
		dosenPaDefault.setAttribute("myValue", kelas.getDosenPaDefault());
		dosenPaDefault.setAttribute("dosen", kelas.getDosenPaDefault());
		dosenPaDefault.setValue(kelas.getDosenPaDefault() == null ? "" : kelas.getDosenPaDefault().getNama());
		dosenPaDefault.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(updateDosenPaSekarang = new MyCheckboxConfig("Update dosen PA sekarang"));
		updateDosenPaSekarang.setChecked(kelas.getUpdateDosenPaSekarang());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(kelas.getKeterangan() == null ? "" : kelas.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

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

					if (KelasAction.this.kelas.getUpdateDosenPaSekarang() && KelasAction.this.kelas != null
							&& KelasAction.this.kelas.getNama() != null
							&& !KelasAction.this.kelas.getNama().trim().isEmpty()) {

						final Label label = Common.displayLoadBar(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(null);
								addWindow.setVisible(false);

							}
						});

						new Thread(new Runnable() {

							@SuppressWarnings("unchecked")
							@Override
							public void run() {
								try {

								Session session = HibernateUtil.currentNativeSession();
								List<Mahasiswa> myKelasPunyaMahasiswas = session.createCriteria(Mahasiswa.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))

										.add(KelasAction.this.kelas.getJurusan() == null
												? Restrictions.sqlRestriction("true")
												: Restrictions.eq("jurusan", KelasAction.this.kelas.getJurusan()))

										.createAlias("jurusan", "jurusan")
										.add(KelasAction.this.kelas.getTahunAngkatan() == null
												? Restrictions.sqlRestriction("true")
												: Restrictions.eq("tahunangkatan",
														KelasAction.this.kelas.getTahunAngkatan()))

										.add(KelasAction.this.kelas.getFakultas() == null
												? Restrictions.sqlRestriction("true")
												: Restrictions.eq("jurusan.fakultas",
														KelasAction.this.kelas.getFakultas()))

										.add(Restrictions.eq("kelas", KelasAction.this.kelas.getNama())).list();
								HibernateUtil.closeSession();
								int i = 1;
								for (Mahasiswa mahasiswa : myKelasPunyaMahasiswas) {
									session = HibernateUtil.currentNativeSession();
									Long idDosen = KelasAction.this.kelas.getDosenPaDefault() == null ? null
											: KelasAction.this.kelas.getDosenPaDefault().getId();
									String query = "update mahasiswa set dosen=" + idDosen + " where id = "
											+ mahasiswa.getId() + "";
									int hasil = session.createSQLQuery(query).executeUpdate();
									System.out.println("query => " + query + ", hasil => " + hasil);

									label.setValue("Singkronkan dosen PA mahasiswa " + mahasiswa + " ("
											+ Common.numberFormat.get().format((i * 100.0 / myKelasPunyaMahasiswas.size()))
											+ "%)");

									KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa);
									krsMahasiswa.setDosenPa(KelasAction.this.kelas.getDosenPaDefault());

									session.getTransaction().begin();
									session.update(krsMahasiswa);
									session.getTransaction().commit();

									HibernateUtil.closeSession();

									i++;
								}
								label.setValue("");
															} finally {
									ais.database.hibernate.HibernateUtil.closeSession();
								}
							}
						}).start();

					} else {
						onSearchDefault(null);
						addWindow.setVisible(false);
					}

				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kelas",
					"Kolom Nama Kelas belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Kelas.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		boolean i = checkNamaKelas();
		if (i) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kelas",
					"Nama Kelas sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
					new String[] {
							"Gunakan nama kelas yang berbeda dari data yang sudah ada.",
							"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
					});
			return false;
		}

		KelasDao kelasDao = DaoFactory.getInstance().getKelasDao();
		if (kelas.getId() != null) {
			kelas = kelasDao.load(kelas.getId());

		}

		String kelLama = kelas.getNama();

		kelas.setNama(nama.getValue());
		kelas.setKeterangan(keterangan.getValue());
		kelas.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		kelas.setTahunAngkatan(tahunAngkatan.getValue() == null ? null : tahunAngkatan.getValue().intValue());
		kelas.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));

		kelas.setDosenPaDefault((Dosen) dosenPaDefault.getAttribute("dosen"));
		kelas.setUpdateDosenPaSekarang(updateDosenPaSekarang.isChecked());

		Common.refreshSaveOrUpdate(kelas);

		String query = "update mahasiswa set kelas='" + kelas.getNama() + "' where kelas ilike '" + kelLama + "'";
		kelasDao.getCurrentSession().createSQLQuery(query).executeUpdate();
		System.out.println("query => " + query);

		return true;
	}

	public Criteria initCriteria(boolean order) {

		Dosen dosen = (Dosen) searchdosen.getAttribute("myValue");

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Kelas.class)

				// FIX QueryException "could not resolve property: dosen of ais.database.model.Kelas":
				// Kelas tidak punya properti "dosen" -- field sebenarnya adalah "dosenPaDefault" (lihat
				// Kelas.java). Bug ini membuat pencarian Kelas SELALU gagal (crash onSearchDefault)
				// begitu filter Dosen diisi.
				.add(dosen != null ? Restrictions.eq("dosenPaDefault", dosen) : Restrictions.sqlRestriction("1=1"))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE))

				.add(searchtahun.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunAngkatan", searchtahun.getValue().intValue()))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("jurusan"),
										CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false)))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("fakultas"),
										CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Kelas> kelas = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kelas);
		grid.setRowRenderer(new KelasRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaKelas() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(Kelas.class).setProjection(Projections.rowCount())
				.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.EXACT))
				.add(this.kelas.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.kelas.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	public MyToolbarbuttonConfig cetakDataCustomButton(String buttonLabel, String buttonImage) {

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(buttonLabel, buttonImage);

		toolbarbutton.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

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
								spreadsheet.setMaxcolumns(3);
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
					final List<Kelas> kelases = initCriteria(true)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
					new Thread(new Runnable() {

						@Override
						public void run() {

							try {

								Session session = HibernateUtil.currentSession();

								XSSFWorkbook workbook = new XSSFWorkbook();
								// FIX IllegalArgumentException "The workbook already contains a sheet of this
								// name" (KE-12): dua atau lebih Kelas bisa punya nama yang sama persis (mis.
								// data lama yang belum dirapikan) -- createSheet() dengan nama duplikat
								// melempar exception yang menggagalkan SELURUH proses export, bukan cuma baris
								// yang bentrok. Lacak nama yang sudah dipakai & tambahkan sufiks unik bila
								// bentrok, supaya seluruh kelas tetap ter-export.
								java.util.Set<String> namaSheetTerpakai = new java.util.HashSet<String>();
								for (Kelas kelas : kelases) {
									List<Mahasiswa> data = session.createCriteria(Mahasiswa.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(Restrictions.ilike("kelas", kelas.getNama(), MatchMode.EXACT))
											.addOrder(Order.asc("nim")).setMaxResults(1048576).list();

									intbox.setValue(data.size());
									System.out.println("data = " + data.size());

									String namaAsli = kelas.getNama() == null ? "Kelas" : kelas.getNama().trim();
									// KE-FIX (IllegalArgumentException: Invalid char found in sheet name): Excel
									// melarang \ / ? * [ ] : pada nama sheet (mis. nama kelas "17-KPI/BC" gagal
									// dibuat). Ganti karakter terlarang dgn "-" SEBELUM pemotongan panjang/
									// disambiguasi di bawah supaya nama kelas apa pun tetap bisa jadi nama sheet.
									namaAsli = namaAsli.replaceAll("[\\\\/\\?\\*\\[\\]:]", "-");
									if (namaAsli.isEmpty()) {
										namaAsli = "Kelas";
									}
									String namaSheet = namaAsli.length() > 31 ? namaAsli.substring(0, 31) : namaAsli;
									String namaSheetUnik = namaSheet;
									int sufiks = 2;
									while (namaSheetTerpakai.contains(namaSheetUnik)) {
										String tambahan = " (" + sufiks + ")";
										int maks = 31 - tambahan.length();
										namaSheetUnik = (namaSheet.length() > maks ? namaSheet.substring(0, maks) : namaSheet)
												+ tambahan;
										sufiks++;
									}
									namaSheetTerpakai.add(namaSheetUnik);

									XSSFSheet sheet = workbook.createSheet(namaSheetUnik);
									sheet.setDefaultColumnWidth(20);
									int rowIndex = 0;

									XSSFRow rowhead = sheet.createRow((short) 0);
									rowhead.createCell(0).setCellValue("No.");
									rowhead.createCell(1).setCellValue("NIM");
									rowhead.createCell(2).setCellValue("Nama");

									for (Mahasiswa o : data) {
										try {
											rowIndex++;
											if (o == null) {
												continue;
											}
											label.setValue("Sedang memproses data " + o.toString() + " ("
													+ Common.numberFormat.get().format(rowIndex * 100.0 / data.size())
													+ " %)");

											XSSFRow row = sheet.createRow(rowIndex);

											row.createCell(0).setCellValue(rowIndex);
											row.createCell(1).setCellValue(o.getNim());
											row.createCell(2).setCellValue(o.getNama());

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										}
									}

									data.clear();
									data = null;
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

								label.setValue("");
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
								label.setValue("-");
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
}
