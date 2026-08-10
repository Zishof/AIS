package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
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
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
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

import ais.action.master.bkd.helper.PenilaianAsesorHelper;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.DetailPAHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.report.format1.akademik.LaporanRekapitulasiPA;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.UploadReportHelper;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.PenilaianAsesor;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DosenPembimbingAkademikAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2201964045553345368L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;
	private AmbilDataDosenBanbox searchdosen;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	protected Textbox searchnamamhs;
	protected Textbox searchnim;
	private PerguruanTinggi perguruanTinggi;
	private MyToolbarbuttonConfig find;

	private Tabpanel laporanDosenPA;

	public void onTampilDosenPA(Event event) {
		if (laporanDosenPA.getChildren().size() == 0) {
			LaporanRekapitulasiPA laporanRekapitulasiPA = new LaporanRekapitulasiPA();
			laporanRekapitulasiPA.setHeight("100%");
			laporanRekapitulasiPA.setWidth("100%");
			laporanRekapitulasiPA.setParent(laporanDosenPA);
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}
		perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		if (execution.getParameter("dosen") != null) {
			Dosen dosen = (Dosen) HibernateUtil.currentSession().createCriteria(Dosen.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("dosen")))).uniqueResult();
			searchdosen.setValue(dosen.getNama());
			searchdosen.setAttribute("myValue", dosen);
			searchdosen.setAttribute("dosen", dosen);
			searchdosen.setDisabled(true);
		}

		searchdosen.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = cetakDataCustomButton("Download Data", "/img/print.png");
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		MyToolbarbuttonConfig masukkNKetuaProdi = new MyToolbarbuttonConfig(
				"Masukkan ketua " + Common.getBahasaConfig("Jurusan") + " untuk mhs yg belum punya dosen PA",
				"/img/user_male_add.png");
		Common.appendKeToolbar(masukkNKetuaProdi, find, comp);
		masukkNKetuaProdi.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub
						Session session = HibernateUtil.currentSession();
						@SuppressWarnings("unchecked")
						List<Mahasiswa> mahasiswas = session.createCriteria(Mahasiswa.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.isNull("dosen")).createAlias("jurusan", "jurusan")
								.add(Restrictions.isNotNull("jurusan.kaprodi")).list();
						System.out.println("mahasiswas => " + mahasiswas.size());
						for (Mahasiswa mahasiswa : mahasiswas) {
							Dosen prodi = mahasiswa.getJurusan().getKaprodi();
							if (prodi != null) {
								mahasiswa.setDosen(prodi.getId());
								session.update(mahasiswa);

								KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa);
								krsMahasiswa.setDosenPa(prodi);
								// KE-FIX (SessionException: Session is closed!): loop bulk ini memutar BANYAK
								// mahasiswa berturutan, membuka+menutup session native SATU per iterasi. Cek
								// "session1 == null/!isOpen()" sebelumnya tidak cukup -- currentNativeSession()
								// sendiri sudah menangani itu, sedangkan yang benar-benar terjadi adalah
								// session bisa tertutup DI ANTARA begin() dan commit() (mis. race pada thread
								// latar timer ZK). Bungkus satu baris ini dalam try/catch supaya SATU
								// mahasiswa yang gagal tidak menghentikan seluruh proses bulk untuk mahasiswa
								// lain -- konsisten dgn pola pemulihan yang sudah dipakai di RepositorySyncService.
								try {
									Session session1 = HibernateUtil.currentNativeSession();
									session1.getTransaction().begin();
									Common.refreshSaveOrUpdate(session1, krsMahasiswa);
									session1.getTransaction().commit();
								} catch (Exception exBulk) {
									ais.common.ErrorAuditUtil.record(exBulk,
											"auto-audit src/ais/action/master/DosenPembimbingAkademikAction.java:bulk-dosenpa-satu-mahasiswa");
								} finally {
									HibernateUtil.closeSession();
								}

							}
						}

						MyMessageboxConfig.show(
								"Memasukkan data ketua " + Common.getBahasaConfig("Jurusan")
										+ " untuk mhs yg belum punya dosen PA telah sukses dilakukan",
								"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
								new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										onSearchDefault(arg0);
									}
								});

					}
				});
			}
		});

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
					@Override
					public void onEvent(Event event) throws Exception {
						addWindow.detach();

						final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Dosen PA dengan KRS");
						// PENTING (fix NPE Filedownload.save, insiden 07-23): SEBELUMNYA
						// laporan.selesaikan(null) dipanggil LANGSUNG di dalam Thread latar
						// (DosenPembimbingAkademikAction$4$2$2.run) -- padahal Filedownload.save
						// & messagebox WAJIB berjalan di thread event ZK (butuh Executions
						// context yang tidak ada di Thread biasa) -> NullPointerException.
						// Fix: panggil laporan.selesaikan(...) dari sini, yaitu di dalam
						// EventListener yang sudah dijamin berjalan di thread event ZK (dipicu
						// oleh Timer milik Common.displayLoadBar ketika label.setValue("")
						// mendeteksi proses latar selesai). Thread latar di bawah HANYA
						// mengosongkan label; TIDAK lagi memanggil laporan.selesaikan sendiri.
						final Label label = Common.displayLoadBar(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								laporan.selesaikan(null);
								onSearchDefault(arg0);
							}
						});

						@SuppressWarnings("unchecked")
						final List<Dosen> dosens = initCriteria(true).list();
						// Laporan rinci per dosen (berhasil/gagal+penyebab teknis lengkap) -
						// sebelumnya tiap dosen sudah dibungkus try/catch (baik), tapi hasilnya
						// tak pernah dilaporkan ke user (cuma tampilErrorJikaAdmin internal,
						// tanpa popup akhir sama sekali).
						final java.util.concurrent.atomic.AtomicInteger nomorBarisLaporan = new java.util.concurrent.atomic.AtomicInteger(0);
						new Thread(new Runnable() {

							@SuppressWarnings("unchecked")
							@Override
							public void run() {
								// PENTING (fix Session-is-closed + NPE Filedownload, insiden 07-23):
								// SEBELUMNYA memakai HibernateUtil.currentNativeSession() (session
								// ThreadLocal PER-THREAD). Karena proses per-dosen di bawah memanggil
								// banyak helper (Common.singkronkanKrsMahasiswa -> KrsDanSkripsiHelper,
								// DataUtil.ambilData, dll) yang JUGA berjalan di THREAD LATAR YANG SAMA
								// dan masing-masing punya kontrak "buka native lalu tutup sendiri di
								// finally" (lihat COOKBOOK HibernateUtil), panggilan bersarang itu bisa
								// menutup ThreadLocal MAP yang SAMA dengan session milik loop luar ini
								// (sama-sama diambil via currentNativeSession() pada thread latar yang
								// sama) -> iterasi berikutnya "Session is closed!" di
								// session.getTransaction().begin() (Error A). Fix: pakai
								// HibernateUtil.openSession() -- session LEPAS yang TIDAK disimpan ke
								// ThreadLocal MAP -- sehingga tidak mungkin ditutup oleh helper lain yang
								// hanya menyentuh currentNativeSession()/closeSession(). Dideklarasikan DI
								// LUAR try agar tetap terlihat di blok finally, dan ditutup sendiri di sana
								// lewat closeSessionQuietly(session).
								Session session = HibernateUtil.openSession();
								try {
								for (Dosen dosen : dosens) {
									String kunciDosen = dosen == null || dosen.getNama() == null ? "-" : dosen.getNama();
									try {
										Criteria criteria = session.createCriteria(Mahasiswa.class)
												.add(Restrictions.or(Restrictions.isNull("aktif"),
														Restrictions.eq("aktif", true)))
												.add(Restrictions.eq("dosen", dosen.getId()));

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
												krsMahasiswa.setDosenPa(dosen);
												krsMahasiswaSp.setDosenPa(dosen);

												session.getTransaction().begin();
												Common.refreshUpdate(session, krsMahasiswa);
												Common.refreshUpdate(session, krsMahasiswaSp);
												session.getTransaction().commit();

											}

											rowIndex++;
										}
										mahasiswas.clear();
										laporan.catatBerhasil(nomorBarisLaporan.getAndIncrement(), kunciDosen, "Sinkronisasi berhasil");
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										laporan.catatGagalDetail(nomorBarisLaporan.getAndIncrement(), kunciDosen, e);
									}
								}
								HibernateUtil.closeSessionQuietly(session);
								// Selesai: HANYA kosongkan label di sini. laporan.selesaikan(...) TIDAK
								// dipanggil dari Thread latar ini (lihat catatan di deklarasi
								// EventListener displayLoadBar di atas) -- Timer ZK yang mendeteksi
								// label.setValue("") akan memanggilnya di thread event ZK yang benar.
								label.setValue("");
															} finally {
									ais.database.hibernate.HibernateUtil.closeSessionQuietly(session);
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
		Common.appendKeToolbar(button, find, comp);

	}

	class DosenPembimbingAkademikRenderer extends ais.ui.util.MyRowRenderer {

		private DetailPAHelper detailPAHelper = new DetailPAHelper();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final Dosen dosen = (Dosen) arg1;
			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					if (detail.getChildren().isEmpty() && detail.isOpen()) {
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								detailPAHelper.displayDetailPA(dosen, detail, addWindow);
							}
						});
					}

				}

			});

			if (searchdosen.getAttribute("dosen") != null) {
				detail.setOpen(true);
				detailPAHelper.displayDetailPA(dosen, detail, addWindow);
			}

			CommonMedia.tampilkanGambarKecil(dosen).setParent(arg0);

			new Label(dosen.getNama()).setParent(arg0);
			new Label(dosen.getFakultas() == null ? "" : dosen.getFakultas().getNama()).setParent(arg0);
			new Label(dosen.getJurusan() == null ? "" : dosen.getJurusan().getNama()).setParent(arg0);

			int count = ((Number) HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
					.add(Restrictions.isNull("statusKeluar"))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("dosen", dosen.getId())).setProjection(Projections.rowCount()).uniqueResult())
					.intValue();
			new Label(Common.numberFormat.get().format(count)).setParent(arg0);

		}
	}

	public Criteria initCriteria(boolean order) {

		Criterion criterionMhs = Restrictions.sqlRestriction("true");
		if (!searchnim.getValue().trim().isEmpty() || !searchnamamhs.getValue().trim().isEmpty()) {
			String sql = "this_.id in (select dosen from mahasiswa where dosen is not null and nama ilike '%"
					+ searchnamamhs.getValue().trim() + "%' and nim ilike '%" + searchnim.getValue().trim()
					+ "%' group by dosen)";
			criterionMhs = Restrictions.sqlRestriction(sql);
		}

		Session session = HibernateUtil.currentSession();
		Dosen d = (Dosen) searchdosen.getAttribute("myValue");
		Criteria criteria = session.createCriteria(Dosen.class)
				.add(perguruanTinggi == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("perguruanTinggi", perguruanTinggi))

				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(criterionMhs);
		if (order)
			criteria.addOrder(Order.asc("nama"));

		criteria.add(Restrictions.or(
				searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false),
				Restrictions.eq("milikUniversitas", true)))

				.add(Restrictions.or(Restrictions.eq("milikUniversitas", true),
						searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
								|| searchfakultas.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)))

				.add(d == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("id", d.getId()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Dosen> dosen = ConstantValues
				.simpleList(
						initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE).setFirstResult(
								Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
						Dosen.class);

		ListModel strset = new SimpleListModel(dosen);
		grid.setRowRenderer(new DosenPembimbingAkademikRenderer());
		grid.setModelCheckMobile(strset);

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
					final List<Dosen> dosenes = initCriteria(true).list();
					new Thread(new Runnable() {

						@Override
						public void run() {

							try {

								Session session = HibernateUtil.currentSession();

								XSSFWorkbook workbook = new XSSFWorkbook();
								for (Dosen dosen : dosenes) {
									List<Mahasiswa> data = session.createCriteria(Mahasiswa.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(Restrictions.eq("dosen", dosen.getId())).addOrder(Order.asc("nim"))
											.setMaxResults(1048576).list();

									if (!data.isEmpty()) {
										intbox.setValue(data.size());
										System.out.println("data = " + data.size());
										try {
											XSSFSheet sheet = workbook.createSheet(dosen.getId() + "");
											sheet.setDefaultColumnWidth(20);
											int rowIndex = 0;

											XSSFRow rowhead = sheet.createRow((short) 0);
											rowhead.createCell(0).setCellValue("No.");
											rowhead.createCell(1).setCellValue("NIM");
											rowhead.createCell(2).setCellValue("Nama");
											rowhead.createCell(3).setCellValue("Dosen");

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
													row.createCell(3).setCellValue(dosen.getNama());

												} catch (Exception e) {
													Common.tampilErrorJikaAdmin(e);
												}
											}
										} catch (Exception e) {
											// TODO Auto-generated catch block
											Common.tampilErrorJikaAdmin(e);
										}
										data.clear();
										data = null;

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

								label.setValue("");
							} catch (Exception e) {
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

			final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data dosen sedang berlangsung, harap menunggu.."));
			final UploadReportHelper report = new UploadReportHelper("Upload Dosen Pembimbing Akademik");
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
							Dosen dosen = (Dosen) session.createCriteria(Dosen.class)
									.add(Restrictions.idEq(Long.parseLong(sheet.getSheetName().trim())))
									.setMaxResults(1).uniqueResult();

							if (dosen == null) {
								HibernateUtil.closeSession();
								continue;
							}

							session.createSQLQuery("delete from dosen_pembimbing_akademik where dosen=" + dosen.getId())
									.executeUpdate();

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

									} catch (Exception e1) { ais.common.ErrorAuditUtil.record(e1, "auto-audit(empty-catch) src/ais/action/master/DosenPembimbingAkademikAction.java:787");

									}

									if (mahasiswa == null) {
										continue;
									}

									String namaDosen = Common.getCellContent(Common.getCell(sheet, 3, i));
									Dosen dsn = (Dosen) session.createCriteria(Dosen.class)
											.add(Restrictions.eq("nama", namaDosen)).setMaxResults(1).uniqueResult();

									if (dsn != null) {
										dosen = dsn;
									}

									mahasiswa.setDosen(dosen == null ? null : dosen.getId());

									session.getTransaction().begin();
									session.saveOrUpdate(mahasiswa);
									session.getTransaction().commit();

									KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa);
									krsMahasiswa.setDosenPa(dosen);

									session.getTransaction().begin();
									Common.refreshSaveOrUpdate(session, krsMahasiswa);
									session.getTransaction().commit();

									// session.disconnect();
									ais.common.Common.closeOpenedSession(session);

									label.setValue("Upload mahasiswa " + mahasiswa + " di dosen " + dosen.getNama()
											+ ".. " + Common.numberFormat.get().format(i * 100.0 / size) + " %");
									report.sukses(i, mahasiswa.getNim(), "Dosen PA: " + dosen.getNama());

								} catch (Exception e1) {
									// TODO Auto-generated catch block

									HibernateUtil.closeSession();

									e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/DosenPembimbingAkademikAction.java:827");
									report.gagal(i, "baris-" + i, e1, "Periksa data NIM pada baris ini");
								}
							}

						}

					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/DosenPembimbingAkademikAction.java:835");
					}

					// FIX compile "unreported exception IOException": simpanLaporan() checked exception.
					try {
						downloadPath.setValue(report.simpanLaporan().getAbsolutePath());
					} catch (java.io.IOException eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) src/ais/action/master/DosenPembimbingAkademikAction.java:882"); }
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
						MyMessageboxConfig.show("Update data dosen berhasil dilakukan. " + report.getRingkasan(), "Pemberitahuan",
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

	public static void displayRow(Center center, final PenilaianAsesor penilaianAsesor) throws Exception {
		ais.ui.util.MyButtonTabbox btnTab = ais.ui.util.MyButtonTabbox.buat(center, "100%", new int[] { 0 });

		{ org.zkoss.zul.Div panel = btnTab.tambahTab(0, "Penilaian Asesor", "/img/svg/award.svg");
		  panel.setStyle("min-height: 300px;");
		  PenilaianAsesorHelper.formNilai(penilaianAsesor.getAsesemenPenilaian().getPegawai(),
				penilaianAsesor.getAsesemenPenilaian().getJenjang(),
				penilaianAsesor.getAsesemenPenilaian().getTahunAkademik(),
				penilaianAsesor.getAsesemenPenilaian().getSemester(), "SK Pembimbing Akademik",
				penilaianAsesor.getAsesemenPenilaian().getSpesifikasi(), new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
					}
				}).setParent(panel); }

		btnTab.tambahTabLazy(1, "Rincian Pembimbing Akademik", "/img/svg/chalkboard-user.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
			@Override
			public void muat(org.zkoss.zul.Div panel) throws Exception {
				Dosen dosen = penilaianAsesor.getAsesemenPenilaian().getPegawai().getDosen();
				if (dosen != null) {
					MyWindow window = new MyWindow("", "none", false);
					window.setHeight("100%"); window.setWidth("100%"); window.setParent(panel);
					new MyInclude("/pages/master/dosen_pembimbing_akademik.zul?dosen=" + dosen.getId()).setParent(window);
				}
			}
		});
	}

}
