package ais.action.master.sekolah.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
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
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataSiswaBanyak;
import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.AsramaSiswa;
import ais.database.model.sekolah.AsramaSiswaPunyaSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper UI untuk layar detail penghuni asrama siswa ({@link AsramaSiswa}/
 * {@link AsramaSiswaPunyaSiswa}). Menampilkan daftar siswa penghuni berpaginasi dengan filter
 * nama/NISN dan angkatan, mendukung penambahan penghuni lewat picker banyak-pilih
 * ({@code AmbilDataSiswaBanyak}) atau unggah massal dari berkas Excel
 * ({@link #uploadDataSiswa}, dijalankan di thread terpisah dengan progres dan laporan unduhan
 * teks di akhir), penghapusan penghuni satu per satu, pembersihan seluruh penghuni sekaligus lewat
 * SQL native ({@code UPDATE sekolah.siswa SET asrama=null}), pencetakan data, dan pencetakan
 * laporan absensi lewat {@code CommonReportHelper}. Kolom {@code asrama} pada entitas
 * {@link Siswa} selalu disinkronkan mengikuti keanggotaan {@link AsramaSiswaPunyaSiswa} (baik
 * saat baris dirender pertama kali, ditambahkan, maupun dihapus).
 */
public class DetailAsramaSiswaHelper implements DataLoader, DataCriteria {

	private MyGrid grid;
	// private Siswa siswa;
	private AsramaSiswa asramaSiswa;
	private boolean delete = false;

	private Textbox nama;
	private Intbox angkatan;
	private boolean create;

	private Paging paging;

	/** Membuat helper, menentukan hak hapus/tambah dari hak akses pengguna saat ini, dan menginisialisasi komponen paging yang memicu {@link #loadData} saat halaman berganti. */
	public DetailAsramaSiswaHelper() {
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		create = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});
	}

	/** Renderer baris grid: foto, label revisi+NIS, nama, tahun masuk, status siswa, dan tombol hapus (dengan dialog konfirmasi; melepas kaitan {@code siswa.asrama} sebelum menghapus baris relasi). Menyinkronkan {@code siswa.getAsrama()} ke asrama ini bila belum tersinkron saat baris dirender. */
	class DetailPARenderer extends ais.ui.util.MyRowRenderer {

		public DetailPARenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final AsramaSiswaPunyaSiswa asramaSiswaPunyaSiswa = (AsramaSiswaPunyaSiswa) data;
			final Siswa siswa = asramaSiswaPunyaSiswa.getSiswa();

			if (siswa.getAsrama() == null) {
				siswa.setAsrama(asramaSiswaPunyaSiswa.getAsramaSiswa());
				Common.refreshUpdate(siswa);
			}

			CommonMedia.tampilkanGambarKecil(siswa).setParent(row);
			RevisiHelper.createNewRevisi(Siswa.class, siswa, siswa.getNomorInduk()).setParent(row);

			new Label(siswa.getNama()).setParent(row);
			new Label(siswa.getTahunMasuk() + "").setParent(row);
			new Label(siswa.getStatusSiswa()).setParent(row);

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

											siswa.setAsrama(null);

											Common.refreshUpdate(siswa);

											Common.refreshDelete(asramaSiswaPunyaSiswa);

											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													loadData(null);
												}
											});

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e); 
											MyMessageboxConfig
													.show("Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);

			toolbar.setParent(row);

		}

	}

	/** Membangun kriteria pencarian {@link AsramaSiswaPunyaSiswa} milik {@code asramaSiswa}, opsional difilter nama/NISN siswa (ILIKE sebagian) dan tahun masuk, diurutkan menurut tahun masuk menurun lalu NISN dan id bila {@code order} true. */
	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(AsramaSiswaPunyaSiswa.class)

				.add(Restrictions.eq("asramaSiswa", asramaSiswa))

				.createCriteria("siswa")

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("namaSiswa", nama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("nomorIndukNasional", nama.getValue().trim(), MatchMode.ANYWHERE)))
				.add(angkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunMasuk", angkatan.getValue()));

		if (order) {
			criteria.addOrder(Order.desc("tahunMasuk")).addOrder(Order.asc("nomorIndukNasional"))
					.addOrder(Order.desc("id"));
		}

		return criteria;
	}

	/** Memuat satu halaman penghuni sesuai kriteria/paging saat ini dan menyegarkan grid dengan {@link DetailPARenderer}. */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Common.initPaging(initCriteria(false), paging);
		List<AsramaSiswaPunyaSiswa> siswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(siswa);
		grid.setRowRenderer(new DetailPARenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Membangun tampilan lengkap detail penghuni asrama ke dalam {@code component}: toolbar
	 * pencarian nama/angkatan, tombol Absensi (mencetak laporan absensi asrama), "Ambil Siswa"
	 * (picker banyak-pilih, hanya bila punya hak buat), "Bersihkan" (menghapus seluruh keanggotaan
	 * lewat SQL native, dengan dialog konfirmasi), cetak data, dan unggah Excel; diikuti grid
	 * berpaginasi (10 baris/halaman) daftar penghuni.
	 *
	 * @param asramaSiswa asrama yang penghuninya dikelola
	 * @param component   komponen ZK tempat tata letak dibangun (dibersihkan lebih dulu)
	 * @param window      jendela induk (tidak dipakai langsung, diteruskan untuk konteks pemanggil)
	 */
	public void displayDetailPA(final AsramaSiswa asramaSiswa, final Component component, final MyWindow window) {

		this.asramaSiswa = asramaSiswa;
		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Siswa : ")));
		toolbar.appendChild(nama = new Textbox());
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Angkatan : ")));
		toolbar.appendChild(angkatan = new Intbox());
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Absensi", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.onLaporanAbsensi(asramaSiswa, false);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Ambil Siswa", "/img/new.gif");
		button.setVisible(create);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				Session session = HibernateUtil.currentSession();
				List<Siswa> siswas = session.createCriteria(AsramaSiswaPunyaSiswa.class)
						.setProjection(Projections.groupProperty("siswa"))
						.add(Restrictions.eq("asramaSiswa", asramaSiswa)).list();

				AmbilDataSiswaBanyak ambilDataSiswaBanyak = new AmbilDataSiswaBanyak(siswas);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataSiswaBanyak);
				ambilDataSiswaBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Siswa> siswas = (List<Siswa>) arg0.getData();

						Session session = HibernateUtil.currentSession();
						for (Siswa siswa : siswas) {

							AsramaSiswaPunyaSiswa asramaSiswaPunyaSiswa = (AsramaSiswaPunyaSiswa) session
									.createCriteria(AsramaSiswaPunyaSiswa.class).add(Restrictions.eq("siswa", siswa))
									.setMaxResults(1).uniqueResult();
							if (asramaSiswaPunyaSiswa == null) {
								asramaSiswaPunyaSiswa = new AsramaSiswaPunyaSiswa();
							}

							asramaSiswaPunyaSiswa.setAsramaSiswa(asramaSiswa);
							asramaSiswaPunyaSiswa.setSiswa(siswa);
							Common.refreshSaveOrUpdate(asramaSiswaPunyaSiswa);

							siswa.setAsrama(asramaSiswa);
							Common.refreshSaveOrUpdate(siswa);
						}

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(null);
							}
						});
					}
				});
				ambilDataSiswaBanyak.setWidth("850px");
				ambilDataSiswaBanyak.setHeight("97%");
				ambilDataSiswaBanyak.setVisible(true);
				ambilDataSiswaBanyak.onModal();

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

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {

										Session session = HibernateUtil.currentSession();

										session.createSQLQuery("update sekolah.siswa set asrama=null where asrama= "
												+ asramaSiswa.getId()).executeUpdate();

										loadData(null);

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e); 
										MyMessageboxConfig
												.show("Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
														+ e.getMessage());
									}

								}

							}
						});

			}

		});
		button.setParent(toolbar);

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, "siswa.nomorInduk",
				"siswa.nomorIndukNasional", "siswa.namaSiswa", "siswa.tahunMasuk", "siswa.sekolah.nama",
				"siswa.sekolah.yayasan", "siswa.statusSiswa");
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload " + Common.ukuranLabelFileUpload(),
				"/img/excel.png");
		upload.setUpload(Common.ukuranFileUpload());
		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
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
							uploadDataSiswa(file, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									loadData(arg0);
									Clients.clearBusy();
								}
							});
						}
					}, "Harap tunggu.. sedang melakukan proses upload data..");

				} else {
					MyMessageboxConfig
							.show("File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
									+ media, "Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}
		});
		toolbar.appendChild(upload);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		paging.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Foto");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIS");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Angkatan");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Status");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		loadData(null);

	}

	/**
	 * Memproses berkas Excel (.xlsx) berisi daftar siswa untuk ditambahkan sebagai penghuni asrama
	 * ini, dijalankan di thread terpisah dengan indikator sibuk dan timer polling progres (setiap
	 * 200ms). Untuk tiap baris, siswa diresolusi dari kolom pertama ({@code Common.getSheetContentAsObject}
	 * dengan tipe {@link Siswa}); baris yang siswanya tidak ditemukan atau kolom kosong dilewati/
	 * dicatat gagal, baris valid membuat/memperbarui {@link AsramaSiswaPunyaSiswa} dan menyetel
	 * {@code siswa.asrama}, masing-masing dalam transaksi terpisah. Setelah selesai, laporan rinci
	 * (jumlah berhasil/gagal per baris) diunduh sebagai berkas teks dan ringkasan ditampilkan lewat
	 * dialog, lalu {@code eventListener} dipanggil.
	 *
	 * @param file          berkas Excel sementara yang sudah tersimpan di server
	 * @param eventListener callback yang dipanggil setelah proses dan dialog ringkasan selesai
	 * @throws Exception diteruskan dari kegagalan pembangunan komponen UI progres
	 */
	public void uploadDataSiswa(final File file, final EventListener eventListener) throws Exception {

		final StringBuilder laporan = new StringBuilder();
		final int[] jumlah = {0, 0}; // [0]=berhasil, [1]=gagal/dilewati

		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data siswa.."));
		Clients.showBusy(label.getValue());
		final Timer timer = new Timer(200);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Clients.showBusy(label.getValue());
				if (label.getValue().isEmpty()) {
					timer.detach();
					Clients.clearBusy();
					String isi = "Laporan Upload Siswa ke Asrama\n"
							+ "==============================\n"
							+ "Berhasil : " + jumlah[0] + " siswa\n"
							+ "Gagal    : " + jumlah[1] + " baris\n\n"
							+ laporan.toString();
					try {
						org.zkoss.zul.Filedownload.save(isi.getBytes("UTF-8"), "text/plain", "laporan_upload_siswa_asrama.txt");
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					MyMessageboxConfig.show(
							"Upload selesai.\nBerhasil: " + jumlah[0] + " siswa, Gagal/Dilewati: " + jumlah[1]
									+ " baris.\nLaporan rinci telah diunduh.",
							"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, eventListener);
				}
			}
		});
		timer.start();

		new Thread(new Runnable() {

			@Override
			public void run() {
				Session session = null;
				try {

					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheet = workbook.getSheetAt(0);

					session = HibernateUtil.openSession();

					int rowCount = (sheet.getLastRowNum() + 1);
					for (int i = 1; i < rowCount; i++) {
						try {

							Siswa siswa = (Siswa) Common.getSheetContentAsObject(sheet, 0, i, Siswa.class);
							if (siswa != null && siswa.getId() != null) {
								Siswa siswaSafe = (Siswa) session.get(Siswa.class, siswa.getId());
								if (siswaSafe == null) {
									laporan.append("Baris ").append(i).append(": GAGAL - siswa tidak ditemukan di DB\n");
									jumlah[1]++;
									continue;
								}

								AsramaSiswaPunyaSiswa asps = (AsramaSiswaPunyaSiswa) session
										.createCriteria(AsramaSiswaPunyaSiswa.class)
										.add(Restrictions.eq("siswa", siswaSafe)).setMaxResults(1).uniqueResult();
								if (asps == null) {
									asps = new AsramaSiswaPunyaSiswa();
								}
								asps.setAsramaSiswa(asramaSiswa);
								asps.setSiswa(siswaSafe);
								session.getTransaction().begin();
								Common.refreshSaveOrUpdate(session, asps);
								session.getTransaction().commit();

								siswaSafe.setAsrama(asramaSiswa);
								session.getTransaction().begin();
								Common.refreshUpdate(session, siswaSafe);
								session.getTransaction().commit();

								laporan.append("Baris ").append(i).append(": OK - ")
										.append(siswaSafe.getNim()).append(" ").append(siswaSafe.getNama()).append("\n");
								jumlah[0]++;
								label.setValue("Upload \"" + siswaSafe.getNim() + " - " + siswaSafe.getNama() + "\" ("
										+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
							} else {
								String cellVal = Common.getCellContent(Common.getCell(sheet, 0, i));
								if (cellVal != null && !cellVal.trim().isEmpty()) {
									laporan.append("Baris ").append(i).append(": DILEWATI - NIS '")
											.append(cellVal).append("' tidak ditemukan\n");
									jumlah[1]++;
								}
							}

						} catch (Exception e) {
							laporan.append("Baris ").append(i).append(": ERROR - ").append(e.getMessage()).append("\n");
							jumlah[1]++;
							Common.tampilErrorJikaAdmin(e);
						}

					}
				} catch (Exception e1) {
					laporan.append("ERROR FATAL: ").append(e1.getMessage()).append("\n");
					e1.printStackTrace();
					ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/sekolah/helper/DetailAsramaSiswaHelper.java:uploadDataSiswa");
				} finally {
					HibernateUtil.closeSessionQuietly(session);
					HibernateUtil.closeSession();
				}

				label.setValue("");
			}
		}).start();
	}

}
