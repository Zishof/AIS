package ais.action.master.helper;

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
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.generic.AmbilDataMahasiswaBanyak;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.ProgramMahasiswa;
import ais.database.model.Mahasiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Panel detail ZK ({@link MyDetail}, lazy-load saat dibuka via {@code Events.ON_OPEN}) yang
 * menampilkan dan mengelola keanggotaan mahasiswa dalam satu {@link ProgramMahasiswa} (program/
 * kelompok penugasan mahasiswa non-akademik, mis. beasiswa, kelas unggulan, atau program khusus
 * lain — relasi disimpan langsung sebagai field {@code programMahasiswa} pada {@link Mahasiswa},
 * BUKAN tabel pivot terpisah).
 *
 * <p><b>Isi grid</b> (dibangun {@link #loadData(Object)}, dirender {@link MahasiswaRenderer}):
 * foto, NIM, nama (tautan riwayat revisi), tahun angkatan, jurusan, dan tombol hapus yang
 * meng-null-kan {@code mahasiswa.setProgramMahasiswa(null)} (bukan menghapus data mahasiswa,
 * hanya melepas keanggotaan dari program ini).</p>
 *
 * <p><b>Toolbar</b> menyediakan lima aksi:</p>
 * <ul>
 * <li><b>Ambil Data Mahasiswa</b> — membuka picker {@code AmbilDataMahasiswaBanyak} berisi
 * mahasiswa yang SUDAH menjadi anggota program ini ({@link #initCriteria(boolean)} dengan
 * {@code order=false}, dipakai sebagai daftar pre-selected), hasil pilihan pengguna di-assign
 * {@code programMahasiswa} lalu disimpan satu per satu via {@code Common.refreshUpdate}.</li>
 * <li><b>Pencarian</b> — textbox yang memfilter grid berdasar nama/NIM (ilike, via
 * {@link #initCriteria(boolean)}) saat Enter ditekan.</li>
 * <li><b>Refresh</b>, <b>Cetak</b> (lewat {@code Common.cetakData} dengan kolom nim/nama/
 * jurusan.nama/tahunangkatan).</li>
 * <li><b>Upload (xlsx)</b> — lihat {@link #uploadDataMahasiswa(File, EventListener)}.</li>
 * <li><b>Hapus Semua</b> — melepas keanggotaan SELURUH mahasiswa yang cocok filter pencarian
 * saat ini (maks. 5000 baris) dari program ini, satu per satu, tanpa transaksi batch.</li>
 * </ul>
 *
 * <p><b>Efek samping:</b> seluruh mutasi keanggotaan program (assign/lepas) memakai
 * {@code Common.refreshUpdate}/{@code refreshSaveOrUpdate} langsung tanpa transaksi eksplisit
 * per-batch — kegagalan di tengah proses "Hapus Semua"/upload massal bisa meninggalkan sebagian
 * baris sudah berubah dan sebagian belum (lihat catatan rollback di
 * {@link #uploadDataMahasiswa(File, EventListener)}, satu-satunya jalur yang menangani ini
 * secara eksplisit per baris).</p>
 *
 * @see MyDetail
 * @see DataCriteria
 */
public class ProgramDataMahasiswaDetailAction extends MyDetail implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private ProgramMahasiswa programMahasiswa;
	private MyGrid grid;

	private Textbox pencarian;

	/**
	 * Menyimpan {@code programMahasiswa} target dan mendaftarkan listener {@code onOpen}: setiap
	 * kali panel ini DIBUKA (mis. dari accordion/detail lazy MyDetail), isinya dibersihkan
	 * ({@code Common.clear}) lalu dibangun ulang dari nol via {@link #display()} — memastikan
	 * data selalu segar setiap kali panel dibuka, bukan hanya sekali saat konstruksi.
	 *
	 * @param programMahasiswa program yang keanggotaannya akan ditampilkan/dikelola panel ini.
	 */
	public ProgramDataMahasiswaDetailAction(ProgramMahasiswa programMahasiswa) {
		super();
		this.programMahasiswa = programMahasiswa;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(ProgramDataMahasiswaDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	/**
	 * Renderer baris grid untuk satu {@link Mahasiswa} anggota program: foto, NIM, tautan nama +
	 * riwayat revisi, tahun angkatan, nama jurusan, dan tombol hapus (hanya tampak bila
	 * {@code mahasiswa.getProgramMahasiswa() != null}) yang melepas keanggotaan.
	 *
	 * @see ProgramDataMahasiswaDetailAction
	 */
	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		/** Constructor kosong — tidak ada state renderer sendiri. */
		public MahasiswaRenderer() {

		}

		/**
		 * Merender satu baris {@link Mahasiswa}. Tombol hapus meminta konfirmasi lewat
		 * {@link MyMessageboxConfig}, lalu meng-null-kan {@code programMahasiswa} pada mahasiswa
		 * tsb dan menyimpan via {@code Common.refreshSaveOrUpdate} (dibungkus
		 * {@code Common.createDefaultTimer} agar refresh grid berjalan pada siklus event ZK
		 * berikutnya). Kegagalan (mis. relasi terkunci) ditangani lewat
		 * {@code PesanFormalHelper.tampilkanGagalException} dengan saran perbaikan untuk pengguna.
		 *
		 * @param arg0 baris grid tujuan.
		 * @param data instance {@link Mahasiswa} untuk baris ini.
		 */
		@Override
		public void render(final Row arg0, Object data) throws Exception {
			// TODO Auto-generated method stub
			final Mahasiswa mahasiswa = (Mahasiswa) data;

			CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(arg0);
			new Label(mahasiswa.getNim()).setParent(arg0);

			RevisiHelper.createNewRevisi(Mahasiswa.class, mahasiswa, mahasiswa.getNama()).setParent(arg0);

			new Label(mahasiswa.getTahunangkatan() + "").setParent(arg0);

			new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()).setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			button.setVisible(mahasiswa.getProgramMahasiswa() != null);
			button.setOrient("vertical");
			button.setTooltiptext("Hapus Data");
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
											mahasiswa.setProgramMahasiswa(null);
											Common.refreshSaveOrUpdate(mahasiswa);
											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													loadData(null);
												}
											});
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException(
													"menghapus program mahasiswa untuk data ini",
													e,
													new String[] {
															"Periksa apakah data program mahasiswa ini masih berelasi dengan data lain sehingga tidak dapat dihapus.",
															"Muat ulang halaman kemudian ulangi proses penghapusan.",
															"Jika data tetap tidak dapat dihapus, konfirmasikan kebutuhan ini kepada Administrator." });
										}

									}

								}
							});

				}
			});
			button.setParent(arg0);
		}
	}

	/**
	 * Memuat ulang grid: mengambil hingga 500 {@link Mahasiswa} anggota program lewat
	 * {@link #initCriteria(boolean)} (dengan filter pencarian aktif), memasang
	 * {@link MahasiswaRenderer} baru, dan meng-update model grid.
	 *
	 * @param value tidak dipakai (parameter standar callback pemuatan ulang).
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		List<Mahasiswa> mahasiswas = ConstantValues.simpleList(initCriteria(true).setMaxResults(500), Mahasiswa.class);

		ListModel strset = new SimpleListModel(mahasiswas);
		grid.setRowRenderer(new MahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Membangun seluruh UI panel: caption "Daftar mahasiswa yang masuk program &lt;nama&gt;",
	 * toolbar lengkap (lihat daftar aksi di Javadoc kelas), dan grid paging (50 baris/halaman)
	 * dengan kolom Foto/NIM/Nama/Angkatan/Prodi/aksi. Diakhiri dengan {@link #loadData(Object)}
	 * untuk mengisi grid pertama kali. Dipanggil dari constructor DAN setiap kali panel dibuka
	 * ulang (listener {@code onOpen}), sehingga bisa berjalan lebih dari sekali per instance.
	 */
	public void display() {

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(this);
		groupbox.appendChild(new MyCaptionStyled("Daftar mahasiswa yang masuk program " + programMahasiswa.getNama()));
		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Data Mahasiswa", "/img/add_item.png");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<Mahasiswa> mahasiswas = initCriteria(false).list();

				AmbilDataMahasiswaBanyak window = new AmbilDataMahasiswaBanyak(mahasiswas);

				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
				window.setWidth("90%");
				window.setHeight("90%");

				window.setEventListener(new EventListener() {

					@Override
					public void onEvent(final Event dataCalonMhs) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								List<Mahasiswa> mahasiswas = (List<Mahasiswa>) dataCalonMhs.getData();

								if (mahasiswas != null) {
									Session session = HibernateUtil.currentSession();
									for (Mahasiswa mahasiswa : mahasiswas) {
										mahasiswa.setProgramMahasiswa(programMahasiswa);
										Common.refreshUpdate(session, mahasiswa);
									}

									loadData(null);
								}
							}
						});

					}
				});

				window.onModal();

			}

		});
		button.setParent(toolbar);

		pencarian = new Textbox();
		pencarian.setCols(8);
		pencarian.setParent(toolbar);
		pencarian.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(toolbar);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		String[] contents = new String[] { "nim", "nama", "jurusan.nama", "tahunangkatan" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
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

		button = new MyToolbarbuttonConfig("Hapus Semua", "/img/svg/trash.svg");
		button.setTooltiptext("Hapus Data");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {
										List<Mahasiswa> mahasiswas = ConstantValues
												.simpleList(initCriteria(true).setMaxResults(5000), Mahasiswa.class);
										for (Mahasiswa mahasiswa : mahasiswas) {
											mahasiswa.setProgramMahasiswa(null);
											Common.refreshSaveOrUpdate(mahasiswa);
										}
										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												loadData(null);
											}
										});
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException(
												"menghapus program mahasiswa untuk seluruh data yang ditampilkan",
												e,
												new String[] {
														"Periksa apakah sebagian data program mahasiswa ini masih berelasi dengan data lain sehingga tidak dapat dihapus secara massal.",
														"Muat ulang halaman kemudian ulangi proses penghapusan, atau coba hapus data satu per satu untuk mengetahui data yang bermasalah.",
														"Jika data tetap tidak dapat dihapus, konfirmasikan kebutuhan ini kepada Administrator." });
									}

								}

							}
						});

			}
		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Foto");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");
		column.setWidth("12%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Angkatan");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Prodi");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);
	}

	/**
	 * Implementasi {@link DataCriteria}: membangun {@link Criteria} {@link Mahasiswa} yang
	 * menjadi anggota {@code programMahasiswa} ini, aktif (field {@code aktif} null ATAU
	 * {@code true} — mahasiswa lama tanpa nilai eksplisit dianggap aktif), dan cocok filter
	 * {@code pencarian} (ilike nama ATAU NIM, "anywhere") bila diisi, diurutkan id menurun.
	 *
	 * <p><b>Kuirk:</b> parameter {@code order} DIDEKLARASIKAN sesuai kontrak {@link DataCriteria}
	 * tapi TIDAK PERNAH dipakai untuk mengubah perilaku — {@code addOrder(Order.desc("id"))}
	 * selalu dipasang terlepas dari nilai {@code order}. Pemanggil dengan {@code order=false}
	 * (mis. saat menyiapkan daftar pre-selected untuk picker "Ambil Data Mahasiswa") tetap
	 * mendapat hasil terurut.</p>
	 *
	 * @param order tidak berpengaruh pada hasil (lihat kuirk di atas); tetap wajib diisi sesuai
	 *              kontrak {@link DataCriteria}.
	 * @return criteria siap dieksekusi ({@code .list()} atau dibungkus
	 *         {@code ConstantValues.simpleList(...)}).
	 */
	@Override
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		return session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(pencarian.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("nama", pencarian.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("nim", pencarian.getValue().trim(), MatchMode.ANYWHERE)))

				.addOrder(Order.desc("id")).add(Restrictions.eq("programMahasiswa", programMahasiswa));
	}

	/**
	 * Mengimpor keanggotaan program dari berkas Excel (.xlsx): kolom pertama tiap baris (mulai
	 * baris ke-2, header dilewati) dibaca sebagai NIM/NPM (lewat
	 * {@code Common.getSheetContentAsString}/{@code getSheetContentAsObject}, dengan fallback
	 * {@code ConstantValues.ambilByNim} bila konversi objek langsung gagal), lalu mahasiswa yang
	 * cocok di-assign {@code programMahasiswa = this.programMahasiswa} dan disimpan.
	 *
	 * <p><b>Berjalan di thread terpisah</b> (bukan event thread ZK) agar tidak memblokir UI;
	 * progres ditampilkan lewat {@link Label} yang di-poll oleh {@link Timer} 200ms
	 * ({@code Clients.showBusy}), dan proses dianggap selesai ketika label progres kembali
	 * kosong. Setiap baris diproses dalam transaksi Hibernate SENDIRI-SENDIRI
	 * ({@code session.getTransaction().begin()/commit()}) dengan rollback eksplisit saat gagal —
	 * catatan pada kode: WAJIB rollback per-baris, karena tanpa itu transaksi tetap aktif dan
	 * {@code begin()} pada baris berikutnya melempar "Transaction already active", membuat
	 * seluruh baris SESUDAHNYA ikut gagal tanpa jejak. Baris dengan NIM kosong atau tidak
	 * ditemukan dicatat sebagai "dilewati" (bukan "gagal") ke {@link ais.common.LaporanUpload}.</p>
	 *
	 * @param file          berkas .xlsx sementara hasil unggahan (sudah divalidasi ekstensi oleh
	 *                      pemanggil di {@link #display()}).
	 * @param eventListener dipanggil oleh {@code laporan.selesaikan(...)} setelah proses selesai,
	 *                      biasanya untuk memuat ulang grid dan membersihkan indikator sibuk.
	 */
	public void uploadDataMahasiswa(final File file, final EventListener eventListener) throws Exception {

		// Laporan hasil per baris. Menggantikan Label "peringatan" yang disiapkan untuk
		// menampung keterangan baris bermasalah tetapi TIDAK PERNAH diisi, sehingga baris
		// yang tak cocok hilang tanpa kabar sementara notifikasi tetap berbunyi berhasil.
		final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload(
				"Upload Data Mahasiswa Program");
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
								mahasiswa = ConstantValues.ambilByNim(nimBaris);
							}
							if (mahasiswa != null && mahasiswa.getId() != null) {

								mahasiswa.setProgramMahasiswa(programMahasiswa);

								session.getTransaction().begin();
								try {
									Common.refreshUpdate(session, mahasiswa);
									session.getTransaction().commit();
								} catch (Exception eSimpan) {
									// WAJIB rollback: tanpa ini transaksi tetap AKTIF sehingga begin() pada baris
									// berikutnya melempar "Transaction already active" -- satu baris bermasalah
									// membuat SELURUH baris sesudahnya ikut gagal tanpa jejak.
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
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/ProgramDataMahasiswaDetailAction.java:449");
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
