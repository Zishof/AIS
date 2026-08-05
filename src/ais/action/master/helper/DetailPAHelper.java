package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
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
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
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
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Kelas;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DetailPAHelper implements DataLoader, DataCriteria {

	private MyGrid grid;
	// private Mahasiswa mahasiswa;
	private Dosen dosen;
	private boolean delete = false;

	private Textbox nama;
	private Intbox angkatan;
	private boolean create;
	private boolean update;

	private Paging paging;
	private AmbilDataKelasBanbox kelas;

	public DetailPAHelper() {
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		create = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		update = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});
	}

	class DetailPARenderer extends ais.ui.util.MyRowRenderer {

		public DetailPARenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");
			final Mahasiswa mahasiswa = (Mahasiswa) data;

			Vbox vbox = new Vbox();
			vbox.setParent(row);
			CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(vbox);

			RevisiHelper.createNewRevisi(Mahasiswa.class, mahasiswa, mahasiswa.getNim()).setParent(vbox);

			new Label(mahasiswa.getNama()).setParent(row);
			new Label(mahasiswa.getTahunangkatan() + "").setParent(row);
			StatusMahasiswa statusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa).getStatusMahasiswa();
			new Label(statusMahasiswa.getNama()).setParent(row);

			KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa);

			new Label(krsMahasiswa.getKelas()).setParent(row);
			new ais.ui.util.MyHtml(mahasiswa.rubahKeteranganPengambilanKRS(krsMahasiswa.getSemester(),
					krsMahasiswa.getTahapan(), krsMahasiswa.getSemesterPendek(), krsMahasiswa, false)).setParent(row);

			final Html komentarshtml = new ais.ui.util.MyHtml("");
			komentarshtml.setParent(row);

			Integer komentars = krsMahasiswa.getKomentars();
			String kom = komentars == 0 ? "Tidak ada komentar" : "Terdapat " + komentars + " komentar";
			komentarshtml.setContent(kom);

			if (mahasiswa.getDosen() != null) {
				if (krsMahasiswa.getDosenPa() == null
						|| !krsMahasiswa.getDosenPa().getId().equals(mahasiswa.getDosen())) {
					krsMahasiswa.setDosenPa(new Dosen(mahasiswa.getDosen()));
					Common.refreshSaveOrUpdate(krsMahasiswa);

				}
			}

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setOrient("vertical");
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

											mahasiswa.setDosen(null);
											mahasiswa.put("", "dosen");
											Common.refreshUpdate(mahasiswa);

											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													KrsMahasiswa krsMahasiswa = Common
															.singkronkanKrsMahasiswa(mahasiswa);
													krsMahasiswa.setDosenPa(null);
													Common.refreshSaveOrUpdate(krsMahasiswa);

													loadData(null);
												}
											});

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig(Common.getBahasa("label_krs"), "/img/upload.gif");
			button.setOrient("vertical");
			button.setTooltiptext("Tampilkan kartu studi mahasiswa");
			button.addEventListener("onClick", new EventListener() {

				TampilStudiMahasiswaHelper tampilStudiMahasiswaHelper = new TampilStudiMahasiswaHelper(null, null,
						false, true);

				@Override
				public void onEvent(Event event) throws Exception {

					tampilStudiMahasiswaHelper.tampil(mahasiswa, getDataloader(), false);
				}

			});
			button.setParent(toolbar);
			toolbar.setParent(row);

		}

	}

	public Criteria initCriteria(boolean order) {

		Kelas kelas = (Kelas) (this.kelas.getAttribute("kelas"));

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Mahasiswa.class)
				.add(Restrictions.isNull("statusKeluar"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(Restrictions.isNull("statusKeluar"))

				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(kelas != null ? Restrictions.ilike("kelas", kelas.getNama()) : Restrictions.sqlRestriction("1=1"))

				.add(Restrictions.eq("dosen", dosen.getId()))
				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("nim", nama.getValue().trim(), MatchMode.ANYWHERE)))
				.add(angkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunangkatan", angkatan.getValue()));

		if (order) {
			criteria.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim")).addOrder(Order.desc("id"));
		}

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Common.initPaging(initCriteria(false), paging);
		List<Mahasiswa> mahasiswa = ConstantValues
				.simpleList(
						initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE).setFirstResult(
								Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
						Mahasiswa.class);

		ListModel strset = new SimpleListModel(mahasiswa);
		grid.setRowRenderer(new DetailPARenderer());
		grid.setModelCheckMobile(strset);

	}

	private DataLoader getDataloader() {
		return this;
	}

	public void displayDetailPA(final Dosen dosen, final Component component, final MyWindow window) {

		this.dosen = dosen;
		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);
		groupbox.appendChild(new MyCaptionStyled("Daftar mahasiswa yang dibimbing oleh " + dosen.getNama()));

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Mhs : ")));
		toolbar.appendChild(nama = new Textbox());
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Angkatan : ")));
		toolbar.appendChild(angkatan = new Intbox());
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Kelas : ")));
		toolbar.appendChild(kelas = new AmbilDataKelasBanbox());
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Ambil Mahasiswa", "/img/new.gif");
		button.setVisible(create || update);
		button.addEventListener("onClick", new EventListener() {

			private AmbilDataMahasiswaForDosenPAHelper dataMahasiswaHelper = new AmbilDataMahasiswaForDosenPAHelper();

			@Override
			public void onEvent(Event event) throws Exception {
				dataMahasiswaHelper.display(dosen, getDataloader(), window);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Bersihkan", "/img/svg/trash.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus semua data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {

										List<Mahasiswa> mahasiswas = ConstantValues.simpleList(initCriteria(true),
												Mahasiswa.class);
										for (Mahasiswa mahasiswa : mahasiswas) {
											mahasiswa.setDosen(null);
											mahasiswa.put("", "dosen");
										}
										mahasiswas = null;

										String sql = "update mahasiswa set dosen=null where dosen=" + dosen.getId();
										HibernateUtil.currentSession().createSQLQuery(sql).executeUpdate();

										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												loadData(null);
											}
										});

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
									}

								}

							}
						});

			}

		});
		button.setParent(toolbar);

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, "nim", "nama", "tahunangkatan",
				"jurusan.nama", "jurusan.fakultas", "program");
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload " + Common.ukuranLabelFileUpload(),
				"/img/excel.png");
		upload.setUpload(Common.ukuranFileUpload());
		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();
				if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
					return;
				if (media.getName().toLowerCase().endsWith("xlsx")) {

					InputStream inputStream = media.getStreamData();
					// System.out.println("media = " + media);
					final File file = new File(
							Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					// System.out.println("file = " + file.getAbsolutePath());
					file.getParentFile().mkdirs();
					FileOutputStream fileOutputStream = new FileOutputStream(file);
					int c;
					while ((c = inputStream.read()) != -1) {
						fileOutputStream.write(c);
					}
					fileOutputStream.close();
					inputStream.close();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							uploadDataMahasiswa(file, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									loadData(arg0);
									Clients.clearBusy();
								}
							});
						}
					}, "Harap tunggu.. sedang melakukan proses upload data..");

				} else {
					MyMessageboxConfig.show(
							"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
									+ media,
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}
		});
		toolbar.appendChild(upload);

		button = new MyToolbarbuttonConfig("Singkronkan", "/img/new.gif");
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
				for (int i = 1; i <= 30; i++) {
					comboitem = new MyComboitemConfig();
					comboitem.setLabel(i + "");
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
				for (int i = 1; i <= 30; i++) {
					comboitem = new MyComboitemConfig();
					comboitem.setLabel(i + "");
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

						final Label label = Common.displayLoadBar(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(null);
							}
						});

						@SuppressWarnings("unchecked")
						final List<Mahasiswa> mahasiswas = initCriteria(true).list();
						new Thread(new Runnable() {

							@Override
							public void run() {
								try {
								int rowIndex = 1;
								for (Mahasiswa mahasiswa : mahasiswas) {
									label.setValue("Sedang memproses data " + mahasiswa.toString() + " ("
											+ Common.numberFormat.get().format(rowIndex * 100.0 / mahasiswas.size()) + " %)");

									Integer mulai = (Integer) (formStartSemester.getSelectedItem() == null
											|| formStartSemester.getSelectedItem().getValue() == null
													? mahasiswa.currentSemester()
													: formStartSemester.getSelectedItem().getValue());
									Integer sampai = (Integer) (formEndSemester.getSelectedItem() == null
											|| formEndSemester.getSelectedItem().getValue() == null
													? mahasiswa.currentSemester()
													: formEndSemester.getSelectedItem().getValue());

									for (Integer smt = mulai; smt <= sampai; smt++) {
										KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, smt, null,
												null);
										KrsMahasiswa krsMahasiswaSp = Common.singkronkanKrsMahasiswa(mahasiswa, smt,
												null, Perkuliahan.SEMESTER_PENDEK);
										krsMahasiswa.setDosenPa(dosen);
										krsMahasiswaSp.setDosenPa(dosen);

										Session session = HibernateUtil.currentNativeSession();
										session.getTransaction().begin();
										Common.refreshUpdate(session, krsMahasiswa);
										Common.refreshUpdate(session, krsMahasiswaSp);
										session.getTransaction().commit();

										HibernateUtil.closeSession();

									}

									rowIndex++;
								}
								mahasiswas.clear();
								label.setValue("");
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
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		paging.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Angkatan");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Status");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kelas");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Komentar");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		loadData(null);
		// borderlayout.setParent(component);

	}

	public void uploadDataMahasiswa(final File file, final EventListener eventListener) throws Exception {

		// Laporan hasil per baris. Menggantikan Label "peringatan" yang disiapkan untuk
		// menampung keterangan baris bermasalah tetapi TIDAK PERNAH diisi, sehingga baris
		// yang tak cocok hilang tanpa kabar sementara notifikasi tetap berbunyi berhasil.
		final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload(
				"Upload Mahasiswa Dosen PA");
		laporan.setNamaBerkasSumber(file.getName());

		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data data .."));
		Clients.showBusy(label.getValue());
		final Timer timer = new Timer(200);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Clients.showBusy(label.getValue());
				if (label.getValue().isEmpty()) {
					Clients.clearBusy();
					timer.detach();
					laporan.selesaikan(eventListener);
				}

			}
		});
		timer.start();

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				try {

					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheet = workbook.getSheetAt(0);

					Session session = HibernateUtil.currentNativeSession();

					int rowCount = (sheet.getLastRowNum() + 1);
					for (int i = 1; i < rowCount; i++) {
						String nimBaris = "";
						try {

							nimBaris = Common.getSheetContentAsString(sheet, 0, i);

							Mahasiswa mahasiswa = (Mahasiswa) Common.getSheetContentAsObject(sheet, 0, i,
									Mahasiswa.class);
							if (mahasiswa == null) {
								// Fallback pencarian lewat NIM. Layar ini sebelumnya TIDAK punya fallback,
								// sehingga baris gagal dicocokkan begitu sel tak terbaca sebagai objek/ID.
								mahasiswa = ConstantValues.ambilByNim(nimBaris);
							}
							if (mahasiswa != null && mahasiswa.getId() != null) {

								mahasiswa.setDosen(dosen.getId());

								session.getTransaction().begin();
								try {
									Common.refreshUpdate(session, mahasiswa);
									session.getTransaction().commit();
								} catch (Exception eSimpan) {
									// WAJIB rollback: tanpa ini transaksi tetap AKTIF sehingga begin() berikutnya
									// melempar "Transaction already active" -- satu baris bermasalah membuat
									// SELURUH baris sesudahnya ikut gagal tanpa jejak.
									try {
										session.getTransaction().rollback();
									} catch (Exception eRoll) {
										ais.common.ErrorAuditUtil.record(eRoll, "rollback-gagal-upload");
									}
									throw eSimpan;
								}

								KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa);
								krsMahasiswa.setDosenPa(dosen);

								session.getTransaction().begin();
								try {
									Common.refreshUpdate(session, krsMahasiswa);
									session.getTransaction().commit();
								} catch (Exception eSimpan) {
									// WAJIB rollback: tanpa ini transaksi tetap AKTIF sehingga begin() berikutnya
									// melempar "Transaction already active" -- satu baris bermasalah membuat
									// SELURUH baris sesudahnya ikut gagal tanpa jejak.
									try {
										session.getTransaction().rollback();
									} catch (Exception eRoll) {
										ais.common.ErrorAuditUtil.record(eRoll, "rollback-gagal-upload");
									}
									throw eSimpan;
								}

								laporan.catatBerhasil(i, mahasiswa.getNim(), mahasiswa.getNama());

								label.setValue("Upload data \"" + mahasiswa.getNim() + " - " + mahasiswa.getNama()
										+ "\" (" + Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
							} else if (nimBaris == null || nimBaris.trim().isEmpty()) {
								laporan.catatDilewati(i, "", "Kolom NIM/NPM kosong");
							} else {
								laporan.catatDilewati(i, nimBaris,
										"NIM/NPM tidak ditemukan pada data mahasiswa -- periksa penulisannya, "
											+ "atau mahasiswa memang belum terdaftar");
							}

						} catch (Exception e) {
							laporan.catatGagal(i, nimBaris, e);
							Common.tampilErrorJikaAdmin(e);
						}

					}
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/DetailPAHelper.java:702");
				}

				HibernateUtil.closeSession();

				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();
	}

}
