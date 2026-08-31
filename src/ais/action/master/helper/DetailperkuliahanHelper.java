package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
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
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.EksporFromFeederAction;
import ais.action.master.MahasiswaAction;
import ais.action.master.PerkuliahanAction;
import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.feeder.util.FeederExporter;
import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Tbmuser;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelKecilSekali;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper composer ZK untuk halaman admin/dosen "Daftar Mahasiswa" pada satu {@link Perkuliahan}:
 * menampilkan seluruh {@link Detailperkuliahan} (mahasiswa peserta KRS) dari sudut pandang
 * perkuliahan (kebalikan {@link KrsHelper}/{@link KrsPaketHelper} yang berpusat pada mahasiswa).
 * Setiap baris menampilkan foto, riwayat revisi, status validitas data Feeder Dikti, nama,
 * angkatan, status kemahasiswaan, total nilai, status persetujuan (editable via textbox keterangan
 * + intbox semester/tahap), dan tombol Pindah Data/Ubah Persetujuan/Hapus per baris — hak tampil
 * tombol dikontrol lewat flag {@code delete}/{@code edit}/{@code approve}/{@code reject}/{@code create}
 * yang diberikan lewat konstruktor.
 *
 * <p>
 * Toolbar menyediakan: pencarian NIM/nama; Refresh; "Singkronkan" (menjalankan
 * {@code perkuliahan.singkronkan()} di thread terpisah dengan polling timer); "Ambil Mhs" (buka
 * {@code AmbilDataMahasiswaHelper}); "Transfer"/"Copy mhs" (pindah/salin mahasiswa ke perkuliahan
 * lain); "Setujui"/"Tolak"/"Hapus" massal untuk seluruh mahasiswa (dibatasi role
 * Akademik/AdminFakultas/AdminJurusan/Admin); cetak laporan Absensi/UTS/UAS; unduh/unggah data
 * Excel (format kolom {@code mahasiswa, semester, tahap, persetujuan}, diproses baris-per-baris
 * asinkron via {@link #uploadDataMahasiswa} dengan laporan hasil per baris
 * {@link ais.common.LaporanUpload}); dan "History" (buka {@code RevisiDetailPerkuliahanHelper}).
 * </p>
 *
 * <p>
 * Method statis {@link #kirimKeFeeder} menyediakan tombol pengiriman satu
 * {@link Detailperkuliahan} ke Feeder Dikti (Neo Feeder) — dipakai baik oleh perender baris kelas
 * ini maupun dipanggil dari konteks lain — yang menjalankan proses ekspor di thread terpisah
 * dengan progress bar dan log error yang dapat diunduh sebagai file teks bila gagal.
 * </p>
 *
 * <p>
 * Mengimplementasikan {@link DataCriteria} ({@link #initCriteria(boolean)}, dipakai fitur cetak
 * data) dan {@link DataLoader} ({@link #loadData(Object)}).
 * </p>
 */
public class DetailperkuliahanHelper implements DataCriteria, DataLoader {

	private MyGrid grid;
	private Perkuliahan perkuliahan;
	// private Textbox nim;
	private Textbox nama;

	// private Paging paging;

	private Integer semesterPendek;

	protected boolean delete;
	private boolean edit;
	private boolean approve;
	private boolean reject;
	private boolean create;

	/**
	 * @param semesterPendek status semester pendek konteks perkuliahan ({@code null} untuk reguler)
	 * @param delete         izinkan tombol hapus per baris dan tombol "Hapus" massal
	 * @param edit           izinkan tombol Pindah Data, Transfer, Copy mhs, dan History
	 * @param approve        izinkan tombol "Setujui" massal
	 * @param reject         izinkan tombol "Tolak" massal
	 * @param create         izinkan tombol "Ambil Mhs"
	 */
	public DetailperkuliahanHelper(Integer semesterPendek, boolean delete, boolean edit, boolean approve,
			boolean reject, boolean create) {
		this.semesterPendek = semesterPendek;
		this.delete = delete;
		this.edit = edit;
		this.approve = approve;
		this.reject = reject;
		this.create = create;
	}

	private List<Long> detailperkuliahan = null;

	/**
	 * Menambahkan tombol "Kirim ke feeder" ke {@code vbox} (hanya tampil bila user login, admin
	 * berhak akses Feeder, fitur {@code aktifkan_terhubung_langsung_ke_feeder} aktif, dan mahasiswa
	 * sudah punya {@code idRegPd}). Saat diklik: memeriksa ketersediaan server Neo Feeder, lalu
	 * login dan mengirim data perkuliahan (via {@code PerkuliahanAction.kirimKeFeeder}) atau nilai
	 * transfer/konversi (via {@code feederImporter.nilaiTransfer}) di thread terpisah dengan
	 * progress bar; kegagalan (koneksi, kredensial, parsing) ditampilkan sebagai pesan error yang
	 * terlihat pada progress bar, bukan gagal diam-diam.
	 *
	 * @param tbmuser           user yang sedang login
	 * @param detailperkuliahan baris KRS yang akan dikirim ke feeder
	 * @param dataLoader        callback penyegar tampilan setelah proses selesai
	 * @param vbox              komponen tujuan penambahan tombol
	 * @param verical           bila {@code true}, tombol dirender dengan orientasi vertikal
	 */
	public static void kirimKeFeeder(Tbmuser tbmuser, final Detailperkuliahan detailperkuliahan,
			final DataLoader dataLoader, Component vbox, boolean verical) {
		Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();

		if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
				&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")
				&& (mahasiswa != null && mahasiswa.getIdRegPd() != null && !mahasiswa.getIdRegPd().isEmpty())) {

			MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Kirim ke feeder",
					"/img/Finance-Invoice-icon.png");
			buttonTagihan.setStyle("font-size:8px;");
			if (verical) {
				buttonTagihan.setOrient("vertical");
			}
			buttonTagihan.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin mengambil data dari feeder ?", "Pertanyaan",
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

													MyMessageboxConfig.show(err, "Error Terjadi", MyMessageboxConfig.OK,
															MyMessageboxConfig.EXCLAMATION);

													File file = new File(
															"/opt/ecampus/error_" + Common.randLong() + ".txt");

													if (!file.getParentFile().exists()) {
														file.getParentFile().mkdirs();
													}
													FileUtils.writeStringToFile(file, err);
													Filedownload.save(file, "text/plain");

												}

												dataLoader.loadData(true);
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

													FeederExporter feederImporter = new FeederExporter(feederConnector,
															token, null, null, null);

													myLabelProsesDetail.setValue("Mengirim data " + detailperkuliahan);

													Perkuliahan perkuliahan = detailperkuliahan.getPerkuliahan();
													if (perkuliahan != null) {
														Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
														PerkuliahanAction.kirimKeFeeder(feederImporter, perkuliahan,
																feederConnector, token, mahasiswa, errorLog);
													} else if (detailperkuliahan.getMatakuliahKonversi() != null) {

														feederImporter.nilaiTransfer(detailperkuliahan, errorLog);
													}

													myLabelProsesDetail.setValue("");
												} catch (Exception e) {
													// FIX "gagal diam-diam": sebelumnya exception di sini (mis. gagal
													// konek/parse port/JSON) hanya dicatat ke log admin lalu progres
													// diset "" (=SUKSES palsu) di luar try, menutupi kegagalan dari
													// pengguna. Sekarang progres diisi pesan error yang terlihat.
													ais.common.Common.tampilErrorJikaAdmin(e);
													myLabelProsesDetail.setValue("Error: "
															+ ais.common.PesanFormalHelper.pesanGagalException(
																	"pengiriman data Detail Perkuliahan \""
																			+ detailperkuliahan + "\" ke Neo Feeder",
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
			buttonTagihan.setParent(vbox);
		}
	}

	/**
	 * Perender baris grid: menampilkan foto, riwayat revisi, indikator validitas data Feeder
	 * (ikon check/warning bila fitur Feeder aktif), tombol "Kirim ke feeder" ({@link #kirimKeFeeder}),
	 * nama, angkatan, status kemahasiswaan, total nilai, status persetujuan, textbox keterangan
	 * nilai tambahan (auto-save on change), intbox semester dan tahap (auto-save on change), serta
	 * tombol Pindah Data (buka {@code TransferDataMahasiswaHelper} untuk satu mahasiswa), Ubah
	 * Persetujuan (toggle disetujui/belum, dengan pengecekan opsional "nilai harus nol"), dan Hapus
	 * (dengan pengecekan tidak bisa hapus bila sudah disetujui, masih dipakai pengajuan tugas akhir,
	 * atau nilai tidak nol).
	 */
	class DetailPerkuliahanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, data.toString());
			if (detailperkuliahan == null || detailperkuliahan.getMahasiswa() == null) {
				new Label("Data KRS/perkuliahan tidak ditemukan atau mahasiswa sudah tidak terhubung.").setParent(row);
				return;
			}

			CommonMedia.tampilkanGambarKecil(detailperkuliahan.getMahasiswa()).setParent(row);
			Vbox vbox = new Vbox();
			vbox.setParent(row);
			RevisiHelper.createNewRevisi(Detailperkuliahan.class, detailperkuliahan,
					detailperkuliahan.getMahasiswa().getNim()).setParent(vbox);
//			new MyLabelKecil((detailperkuliahan.getFeeder() == null ? "" : "Feeder:" + detailperkuliahan.getFeeder()))
//					.setParent(vbox);
//			new MyLabelKecil((detailperkuliahan.getId_kls() == null ? ""
//					: "Feeder:" + detailperkuliahan.getId_kls() + ";" + detailperkuliahan.getId_reg_pd()))
//							.setParent(vbox);

			Hbox myHbox = new Hbox();
			myHbox.setParent(vbox);
			Tbmuser tbmuser = Common.getCurrentUser();
			if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
					&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {
				if (detailperkuliahan.getFeeder() != null && !detailperkuliahan.getFeeder().trim().isEmpty()) {
					myHbox.appendChild(new Image("/img/svg/check2-circle.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder valid"));
				} else {
					myHbox.appendChild(new Image("/img/svg/warning-outline.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder blm valid"));
				}
			}
			DetailperkuliahanHelper.kirimKeFeeder(tbmuser, detailperkuliahan, DetailperkuliahanHelper.this, myHbox,
					false);

			new Label(detailperkuliahan.getMahasiswa().getNama()).setParent(row);
			new Label(detailperkuliahan.getMahasiswa().getTahunangkatan() + " / "
					+ detailperkuliahan.getMahasiswa().getSemesterMulai()).setParent(row);

			KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(detailperkuliahan.getMahasiswa(),
					detailperkuliahan.getSemester(), detailperkuliahan.getTahap(),
					detailperkuliahan.getPerkuliahan() == null ? null
							: detailperkuliahan.getPerkuliahan().getStatusSemesterPendek());

			ais.database.model.HistoryStatusMahasiswa historyStatusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.getHistoryStatusMahasiswa(krsMahasiswa);
			StatusMahasiswa statusMahasiswa = historyStatusMahasiswa == null ? null : historyStatusMahasiswa.getStatusMahasiswa();
			new Label((detailperkuliahan.getMahasiswa().getStatusAwalMahasiswa() == null ? ""
					: detailperkuliahan.getMahasiswa().getStatusAwalMahasiswa().getNama()) + " / "
					+ (statusMahasiswa == null ? "" : statusMahasiswa.getNama())).setParent(row);

			new Label(detailperkuliahan.getTotalNilai() == null ? "0.0 (Belum dinilai)"
					: Common.numberFormat.get().format(detailperkuliahan.getTotalNilai()) + " ("
							+ (detailperkuliahan.getNilaiHuruf() == null
									|| detailperkuliahan.getNilaiHuruf().trim().equals("") ? "Belum dinilai"
											: detailperkuliahan.getNilaiHuruf())
							+ ")")
					.setParent(row);

			final Label label;
			(label = new Label(detailperkuliahan.getPersetujuan() == null
					|| detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.BELUM_DISETUJUI) ? "Tidak" : "Ya"))
					.setParent(row);
			label.setStyle(label.getValue().equals("Tidak") ? "color:red;" : "color:blue");

			final Textbox keterangan = new Textbox(detailperkuliahan.getDetailNilaiTambahan());
			keterangan.setRows(3);
			keterangan.setWidth("90%");
			keterangan.setParent(row);
//			keterangan.setDisabled(tbmuser == null || tbmuser.getRoot() == null || !tbmuser.getRoot());
			keterangan.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					detailperkuliahan.setDetailNilaiTambahan(keterangan.getValue());
					Common.refreshUpdate(session, (detailperkuliahan));
				}
			});

			final Intbox semester = new Intbox(detailperkuliahan.getSemester());
			semester.setParent(row);
			semester.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					detailperkuliahan
							.setSemester(semester.getValue() == null ? perkuliahan.getSemester() : semester.getValue());
					Common.refreshUpdate(session, (detailperkuliahan));
					semester.setValue(detailperkuliahan.getSemester());
				}
			});

			final Intbox tahap = new Intbox(detailperkuliahan == null || detailperkuliahan.getTahap() == null ? 0
					: detailperkuliahan.getTahap());
			tahap.setParent(row);
//			tahap.setDisabled(tbmuser == null || tbmuser.getRoot() == null || !tbmuser.getRoot());
			tahap.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					detailperkuliahan.setTahap(
							tahap.getValue() == null
									? (perkuliahan.getKurikulumPunyaMatakuliah() == null ? 0
											: perkuliahan.getKurikulumPunyaMatakuliah().getTahap())
									: tahap.getValue());
					Common.refreshUpdate(session, (detailperkuliahan));
					tahap.setValue(detailperkuliahan.getTahap());
				}
			});

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/stock_data_edit_table.png");
			button.setTooltiptext("Pindah Data");
			button.setVisible(
					edit && detailperkuliahan.getPerkuliahan() != null && Common.getCurrentUser().getDosen() == null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show(
							"Apakah yakin ingin memindahkan krs mahasiswa " + detailperkuliahan.getMahasiswa().getNama()
									+ " matakuliah " + detailperkuliahan.getPerkuliahan().getMatakuliah().getNama()
									+ " ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											TransferDataMahasiswaHelper transferDataMahasiswaHelper = new TransferDataMahasiswaHelper(
													detailperkuliahan.getPerkuliahan(),
													detailperkuliahan.getMahasiswa());
											MyWindow window = new MyWindow();
											window.setParent(
													ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
											transferDataMahasiswaHelper.display(new DataLoader() {

												@Override
												public void loadData(Object value) {
													DetailperkuliahanHelper.this.loadData(true);
												}
											}, window);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);

										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setOrient("vertical");
			button.setVisible(edit);
			button.setTooltiptext("Ubah Persetujuan");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					if (Common.bolehKonfigurasi("batalkan_persetujuan_harus_memiliki_nilai_nol")) {
						if (detailperkuliahan.getPersetujuan() != null
								&& detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)
								&& detailperkuliahan.getTotalNilai() > 1.0) {
							MyMessageboxConfig.show("Jika nilai tidak nol, anda tidak bisa mengubah persetujuan",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							return;
						}
					}

					String pertanyaan = "Apakah anda ingin mengubah mahasiswa dengan NIM "
							+ detailperkuliahan.getMahasiswa().getNim() + " dan nama "
							+ detailperkuliahan.getMahasiswa().getNama() + " yang mengikuti perkulihaan "
							+ detailperkuliahan.getPerkuliahan().getMatakuliah().getNama()
							+ (detailperkuliahan.getPersetujuan() == null
									|| detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.BELUM_DISETUJUI)
											? " dari tidak disetujui menjadi disetujui ?"
											: " dari disetujui menjadi tidak disetujui ?");

					MyMessageboxConfig.show(pertanyaan, "Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {
								public void onEvent(Event event) throws Exception {

									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										Session session = HibernateUtil.currentSession();
										detailperkuliahan.setPersetujuan(
												detailperkuliahan.getPersetujuan() == null || detailperkuliahan
														.getPersetujuan().equals(Detailperkuliahan.BELUM_DISETUJUI)
																? Detailperkuliahan.DISETUJUI
																: Detailperkuliahan.BELUM_DISETUJUI);

										Common.refreshUpdate(session, detailperkuliahan);

										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												DetailperkuliahanHelper.this.loadData(true);

											}
										});
									}

								}
							});
				}
			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setOrient("vertical");
			button.setVisible(delete);
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					if (detailperkuliahan.getPersetujuan() != null
							&& detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)) {
						MyMessageboxConfig.show("Mahasiswa yang sudah disetujui tidak bisa dihapus", "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						return;
					}

					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Number jumlahRequestTugasAkhir = (Number) HibernateUtil.currentSession()
													.createCriteria(MahasiswaRequestTugasAkhir.class)
													.setProjection(org.hibernate.criterion.Projections.rowCount())
													.add(Restrictions.eq("detailperkuliahan", detailperkuliahan))
													.uniqueResult();
											if (jumlahRequestTugasAkhir != null
													&& jumlahRequestTugasAkhir.longValue() > 0L) {
												MyMessageboxConfig.show(
														"Data perkuliahan tidak dapat dihapus karena masih digunakan pada pengajuan tugas akhir mahasiswa. Batalkan atau pindahkan pengajuan tugas akhir tersebut terlebih dahulu.",
														"Peringatan", MyMessageboxConfig.OK,
														MyMessageboxConfig.EXCLAMATION);
												return;
											}

											if (Common.bolehKonfigurasi("batalkan_persetujuan_harus_memiliki_nilai_nol")) {
												if (detailperkuliahan.getPersetujuan() != null
														&& detailperkuliahan.getPersetujuan()
																.equals(Detailperkuliahan.DISETUJUI)
														&& detailperkuliahan.getTotalNilai() > 1.0) {
													MyMessageboxConfig.show(
															"Jika nilai tidak nol, anda tidak bisa menghapus mahasiswa ini",
															"Peringatan", MyMessageboxConfig.OK,
															MyMessageboxConfig.EXCLAMATION);
													return;
												}
											}

											Common.refreshDelete(detailperkuliahan);

											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {

													DetailperkuliahanHelper.this.loadData(true);

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

			toolbar.setParent(row);

		}

	}

	/**
	 * Membangun kriteria {@link Detailperkuliahan} yang mengikuti {@link Perkuliahan} tertentu
	 * (bukan konversi, {@code ikutiPerkuliahan} kosong), disaring pula berdasarkan NIM/nama
	 * mahasiswa (ilike) bila kotak pencarian diisi.
	 *
	 * @param order bila {@code true}, tambahkan pengurutan menaik berdasarkan NIM mahasiswa
	 * @return kriteria Hibernate siap dieksekusi
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Detailperkuliahan.class);

		criteria.add(Restrictions.isNull("ikutiPerkuliahan")).createAlias("mahasiswa", "mahasiswa")
				.add(nama == null || nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("mahasiswa.nim", nama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("mahasiswa.nama", nama.getValue().trim(), MatchMode.ANYWHERE)))
				.add(Restrictions.eq("perkuliahan", perkuliahan));

		if (order)
			criteria.addOrder(Order.asc("mahasiswa.nim"));

		return criteria;
	}

	/**
	 * Memuat ulang grid dengan seluruh {@link Detailperkuliahan} milik {@link #perkuliahan} saat
	 * ini, sesuai teks pencarian pada kotak nama.
	 *
	 * @param value bila {@code true} (sebagai {@link Boolean}), paksa cache {@code Detailperkuliahan}
	 *              milik {@link #perkuliahan} dibangun ulang dari database via
	 *              {@code reInitDetailperkuliahan} sebelum diambil
	 */
	public void loadData(Object value) {
		if (value != null && value.equals(true)) {
			perkuliahan.reInitDetailperkuliahan(HibernateUtil.currentSession());
		}
		detailperkuliahan = new ArrayList<Long>(perkuliahan.ambilDetailperkuliahan(null, null, nama.getValue().trim()));
		ListModel strset = new SimpleListModel(detailperkuliahan);
		grid.setRowRenderer(new DetailPerkuliahanRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Mengatur konteks {@link Perkuliahan} helper tanpa membangun ulang tampilan.
	 *
	 * @param perkuliahan      perkuliahan konteks baru
	 * @param perkuliahanAsli  tidak dipakai dalam badan method; diterima untuk kompatibilitas signature
	 */
	public void setPerkuliahan(Perkuliahan perkuliahan, Perkuliahan perkuliahanAsli) {
		this.perkuliahan = perkuliahan;
	}

	/**
	 * Membangun dan menampilkan seluruh UI "Daftar Mahasiswa" untuk {@code perkuliahan} ke dalam
	 * {@code component}: toolbar pencarian dan aksi (lihat javadoc kelas untuk daftar lengkap
	 * tombol dan hak yang mengaturnya) serta grid berisi seluruh peserta perkuliahan (page size
	 * besar — 10000 — sehingga efektif menampilkan semua baris sekaligus).
	 *
	 * @param perkuliahan      perkuliahan yang daftar mahasiswanya ditampilkan
	 * @param perkuliahanAsli  perkuliahan asli (sebelum kemungkinan substitusi/redirect), dipakai
	 *                         untuk cetak laporan absensi
	 * @param component        komponen ZK tujuan tampilan (dibersihkan lebih dulu)
	 * @param window           window pemanggil, diteruskan ke helper Ambil Mhs/Transfer/Copy mhs
	 */
	public void display(final Perkuliahan perkuliahan, final Perkuliahan perkuliahanAsli, final Component component,
			final MyWindow window) {
		this.perkuliahan = perkuliahan;
		Common.clear(component);

		Tbmuser tbmuser = Common.getCurrentUser();

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);
		groupbox.appendChild(
				new MyCaptionStyled("Daftar mahasiswa yang mengikuti perkuliahan " + perkuliahan.toString()));
		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		toolbar.setVisible(tbmuser != null);
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Mhs : ")));
		toolbar.appendChild(nama = new Textbox());
		nama.setCols(10);
		nama.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Refresh", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				DetailperkuliahanHelper.this.loadData(true);
			}

		});
		button.setParent(toolbar);

		MyToolbarbuttonConfig cetakSksDosen = new MyToolbarbuttonConfig("Singkronkan", "/img/svg/check2.svg");
		cetakSksDosen.setVisible(Common.bolehKonfigurasi("aktifkan_tombol_sinkronkan_semua"));
		toolbar.appendChild(cetakSksDosen);
		cetakSksDosen.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						final Label label = new Label(ais.common.Common.getBahasaConfig("Proses singkronisasi perkuliahan"));

						new Thread(new Runnable() {

							@Override
							public void run() {
								try {
								// currentNativeSession() ditutup sekali saja di finally (hindari close ganda).
								Session session = HibernateUtil.currentNativeSession();
								perkuliahan.singkronkan(session);
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

								// System.out.println("process = " +
								// label.getValue());
								Clients.showBusy(label.getValue());
								if (label.getValue().isEmpty()) {

									DetailperkuliahanHelper.this.loadData(true);
									Clients.clearBusy();
									MyMessageboxConfig.show("Singkronisasi perkuliahan berhasil dilakukan",
											"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
									timer.detach();

								}

							}
						});
						timer.start();

					}
				});
			}
		});

		button = new MyToolbarbuttonConfig("Ambil Mhs", "/img/new.gif");
		button.setDisabled(!create);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataMahasiswaHelper dataMahasiswaHelper = new AmbilDataMahasiswaHelper(perkuliahan,
						semesterPendek);
				dataMahasiswaHelper.display(new DataLoader() {

					@Override
					public void loadData(Object value) {

						// currentNativeSession() WAJIB ditutup di finally agar tidak bocor bila singkronkan() gagal.
						Session session = HibernateUtil.currentNativeSession();
						try {
							perkuliahan.singkronkan(session);
						} finally {
							HibernateUtil.closeSession();
						}

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								DetailperkuliahanHelper.this.loadData(true);
							}
						});
					}
				}, window);
			}

		});
		button.setParent(toolbar);
		button = new MyToolbarbuttonConfig("Transfer", "/img/group.gif");
		button.setDisabled(!edit);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				TransferDataMahasiswaHelper dataMahasiswaHelper = new TransferDataMahasiswaHelper(perkuliahan);
				dataMahasiswaHelper.display(new DataLoader() {

					@Override
					public void loadData(Object value) {
						DetailperkuliahanHelper.this.loadData(true);
					}
				}, window);
			}

		});
		button.setParent(toolbar);
		button = new MyToolbarbuttonConfig("Copy mhs", "/img/group.gif");
		button.setDisabled(!edit);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CopyDataMahasiswaHelper dataMahasiswaHelper = new CopyDataMahasiswaHelper(perkuliahan);
				dataMahasiswaHelper.display(new DataLoader() {

					@Override
					public void loadData(Object value) {
						DetailperkuliahanHelper.this.loadData(true);
					}
				}, window);
			}

		});
		button.setParent(toolbar);
		button = new MyToolbarbuttonConfig("Setujui", "/img/svg/edit-box-line.svg");
		button.setDisabled(!approve);

		try {
			button.setVisible(Common.getApakahAdmin() || (tbmuser != null && tbmuser.hakAkses() != null
					&& ((ConstantValues.Akademik != null && ConstantValues.Akademik.getRoleId() != null
							&& ConstantValues.Akademik.getRoleId().equals(tbmuser.hakAkses().getRoleId()))
							|| (ConstantValues.roleAdminFakultas != null
									&& ConstantValues.roleAdminFakultas.getRoleId() != null
									&& ConstantValues.roleAdminFakultas.getRoleId().equals(tbmuser.hakAkses().getRoleId()))
							|| (ConstantValues.roleAdminJurusan != null
									&& ConstantValues.roleAdminJurusan.getRoleId() != null
									&& ConstantValues.roleAdminJurusan.getRoleId().equals(tbmuser.hakAkses().getRoleId())))));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanHelper.java:727");
			// TODO: handle exception
		}
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				if (detailperkuliahan != null) {

					MyMessageboxConfig.show("Apakah anda ingin men-setujui semua mahasiswa di dalam perkuliahan ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {
								public void onEvent(Event event) throws Exception {

									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										for (Long detailperkuliahanid : detailperkuliahan) {
											Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
													.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
											if (detailperkuliahan != null) {
												detailperkuliahan.setPersetujuan(Detailperkuliahan.DISETUJUI);
												Common.refreshUpdate(detailperkuliahan);
											}
										}
										perkuliahan.belum("detailperkulaiahan");
										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												DetailperkuliahanHelper.this.loadData(true);
											}
										});

									}
								}
							});
				}
			}

		});
		button.setParent(toolbar);
		button = new MyToolbarbuttonConfig("Tolak", "/img/svg/warning-outline.svg");
		button.setDisabled(!reject);
		try {
			button.setVisible(Common.getApakahAdmin() || (tbmuser != null && tbmuser.hakAkses() != null
					&& ((ConstantValues.Akademik != null && ConstantValues.Akademik.getRoleId() != null
							&& ConstantValues.Akademik.getRoleId().equals(tbmuser.hakAkses().getRoleId()))
							|| (ConstantValues.roleAdminFakultas != null
									&& ConstantValues.roleAdminFakultas.getRoleId() != null
									&& ConstantValues.roleAdminFakultas.getRoleId().equals(tbmuser.hakAkses().getRoleId()))
							|| (ConstantValues.roleAdminJurusan != null
									&& ConstantValues.roleAdminJurusan.getRoleId() != null
									&& ConstantValues.roleAdminJurusan.getRoleId().equals(tbmuser.hakAkses().getRoleId())))));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanHelper.java:777");
			// TODO: handle exception
		}

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				if (detailperkuliahan != null) {

					MyMessageboxConfig.show("Apakah anda ingin menolak semua mahasiswa di dalam perkuliahan ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {
								public void onEvent(Event event) throws Exception {

									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										for (Long detailperkuliahanid : detailperkuliahan) {
											Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
													.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
											if (detailperkuliahan != null) {
												detailperkuliahan.setPersetujuan(Detailperkuliahan.BELUM_DISETUJUI);
												Common.refreshUpdate(detailperkuliahan);
											}
										}
										perkuliahan.belum("detailperkulaiahan");
										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												DetailperkuliahanHelper.this.loadData(true);
											}
										});

									}
								}
							});

				}
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
		button.setDisabled(!delete);
		try {
			button.setVisible(Common.getApakahAdmin() || (tbmuser != null && tbmuser.hakAkses() != null
					&& ((ConstantValues.Akademik != null && ConstantValues.Akademik.getRoleId() != null
							&& ConstantValues.Akademik.getRoleId().equals(tbmuser.hakAkses().getRoleId()))
							|| (ConstantValues.roleAdminFakultas != null
									&& ConstantValues.roleAdminFakultas.getRoleId() != null
									&& ConstantValues.roleAdminFakultas.getRoleId().equals(tbmuser.hakAkses().getRoleId()))
							|| (ConstantValues.roleAdminJurusan != null
									&& ConstantValues.roleAdminJurusan.getRoleId() != null
									&& ConstantValues.roleAdminJurusan.getRoleId().equals(tbmuser.hakAkses().getRoleId())))));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanHelper.java:830");
			// TODO: handle exception
		}
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				if (detailperkuliahan != null) {

					MyMessageboxConfig.show(
							"Apakah anda ingin menghapus semua mahasiswa yang belum disetujui di dalam perkuliahan ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {
								public void onEvent(Event event) throws Exception {

									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										for (Long detailperkuliahanid : detailperkuliahan) {
											Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
													.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
											if (detailperkuliahan != null) {
												if (detailperkuliahan.getPersetujuan()
														.equals(Detailperkuliahan.BELUM_DISETUJUI)) {
													// currentNativeSession() ditutup di finally + rollback bila commit gagal,
													// supaya sesi tak bocor & transaksi tak tertinggal aktif.
													Session session = HibernateUtil.currentNativeSession();
													try {
														session.refresh(detailperkuliahan);
														session.getTransaction().begin();
														session.delete(detailperkuliahan);
														session.getTransaction().commit();
													} catch (Exception e) {
														try {
															session.getTransaction().rollback();
														} catch (Exception er) {
															ais.common.ErrorAuditUtil.record(er,
																	"rollback-gagal src/ais/action/master/helper/DetailperkuliahanHelper.java:hapusBelumDisetujui");
														}
														ais.common.ErrorAuditUtil.record(e,
																"auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanHelper.java:862");
													} finally {
														HibernateUtil.closeSession();
													}
												}
											}
										}
										perkuliahan.belum("detailperkulaiahan");
										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												DetailperkuliahanHelper.this.loadData(true);
											}
										});
									}
								}
							});

				}
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Absensi", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.onLaporanAbsensi(perkuliahanAsli, false);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("UTS", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.onLaporanAbsensi(perkuliahan, "UTS");

			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("UAS", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.onLaporanAbsensi(perkuliahan, "UAS");
			}

		});
		button.setParent(toolbar);

		final String[] contents = new String[] { "mahasiswa", "semester", "tahap", "persetujuan" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload Data" + Common.ukuranLabelFileUpload(),
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
							}, contents);
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

		MahasiswaAction.createUploadDanDownloadData(toolbar, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

			}
		}, new DataCriteria() {

			@Override
			public Object initCriteria(boolean order) {
				return perkuliahan.ambilMahasiswa();
			}
		}, false, false);

		button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		button.setDisabled(!edit);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				RevisiDetailPerkuliahanHelper revisiHelper = new RevisiDetailPerkuliahanHelper(perkuliahan,
						new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								perkuliahan.belum("detailperkulaiahan");
								Common.createDefaultTimer(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										DetailperkuliahanHelper.this.loadData(true);
									}
								});
							}
						});
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();

			}

		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10000);
		grid.setParent(groupbox);
		grid.setSclass("fgrid");
		// paging.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Foto");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		// column.setWidth("25%");

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
		column.setLabel("Total Nilai");
		column.setWidth(tbmuser == null ? "0%" : "10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Persetujuan");
		column.setWidth(tbmuser == null ? "0%" : "10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth(tbmuser == null ? "0%" : "10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Smt");
		column.setWidth(tbmuser == null ? "0%" : (perkuliahan.getMerupakanPraPerkuliahan() ? "0%" : "5%"));

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahap");
		column.setWidth(tbmuser == null ? "0%" : (ConstantValues.aktifkanTahapanKurikulum ? "5%" : "0%"));

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth(tbmuser == null ? "0%" : "12%");

		loadData(null);

	}

	/**
	 * Memproses berkas Excel (.xlsx) unggahan berisi kolom mahasiswa/semester/tahap/persetujuan:
	 * untuk setiap baris, mencari mahasiswa (via objek sel atau fallback NIM), memeriksa status
	 * pembayaran semester terkait (baris dilewati dengan catatan bila belum bayar), lalu
	 * membuat/menemukan baris {@link Detailperkuliahan} yang sesuai. Dijalankan di thread terpisah
	 * dengan sesi Hibernate dedikasi (bukan sesi thread-local, karena thread ini berjalan setelah
	 * request asal selesai) yang ditutup rapi di {@code finally}; kegagalan simpan per baris di-
	 * rollback agar tidak menggagalkan baris berikutnya. Hasil akhir dilaporkan per baris via
	 * {@link ais.common.LaporanUpload}, lalu {@code eventListener} dipanggil.
	 *
	 * @param file          berkas .xlsx yang diunggah
	 * @param eventListener callback dipanggil setelah laporan hasil selesai disusun
	 * @param contents      nama-nama kolom (tidak dipakai langsung; bagian dari signature yang
	 *                      dibagi dengan pemanggil unduh data)
	 */
	public void uploadDataMahasiswa(final File file, final EventListener eventListener, final String[] contents)
			throws Exception {

		// Laporan hasil per baris. Menggantikan pemakaian Label "peringatan" untuk pesan akhir,
		// sekaligus mencatat baris yang tak cocok / belum bayar yang sebelumnya hanya jadi teks
		// gabungan tanpa rincian per baris.
		final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload(
				"Upload Peserta Perkuliahan");
		laporan.setNamaBerkasSumber(file.getName());

		final Tbmuser tbmuser = Common.getCurrentUser();

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

				Session session = null;
				try {

					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheet = workbook.getSheetAt(0);

					// Thread latar: JANGAN pakai currentNativeSession (session thread-cache bisa sudah
					// DITUTUP saat request selesai → "Session is closed!" ketika createCriteria). Buka
					// session dedikasi utk thread ini lalu tutup di finally (clear/disconnect/close).
					session = HibernateUtil.getSessionFactory().openSession();

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

								Integer semester = Common.getSheetContentAsInteger(sheet, 1, i);
								Integer tahap = Common.getSheetContentAsInteger(sheet, 2, i);
								Integer persetujuan = Common.getSheetContentAsInteger(sheet, 3, i);
								if (persetujuan == null) {
									persetujuan = Detailperkuliahan.BELUM_DISETUJUI;
								}

								if (semester == null) {
									semester = perkuliahan.getSemester();
								}

								try {
									if (!Common.checkStatusPembayaranMahasiswa(semester, tahap, mahasiswa, false,
											semesterPendek != null)) {
										laporan.catatDilewati(i, mahasiswa.getNim(),
												"Belum melakukan pembayaran di semester " + semester
														+ (semesterPendek != null ? " (semester pendek)" : ""));
										continue;
									}
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanHelper.java:1162");
									// TODO: handle exception
								}

								Detailperkuliahan detailperkuliahan = (Detailperkuliahan) session
										.createCriteria(Detailperkuliahan.class)
										.add(Restrictions.eq("mahasiswa", mahasiswa))
										.add(Restrictions.eq("perkuliahan", perkuliahan)).addOrder(Order.desc("id"))
										.setMaxResults(1).uniqueResult();

								if (detailperkuliahan == null) {
									detailperkuliahan = new Detailperkuliahan(tbmuser, DetailperkuliahanHelper.class);
									detailperkuliahan.setMahasiswa(mahasiswa);
									detailperkuliahan.setSemester(semester);
									detailperkuliahan.setTahap(tahap);
									detailperkuliahan.setPersetujuan(persetujuan);
									detailperkuliahan.setPerkuliahan(perkuliahan);

									session.getTransaction().begin();
									try {
										session.saveOrUpdate(detailperkuliahan);
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
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/DetailperkuliahanHelper.java:1196");
				} finally {
					if (session != null) {
						try { if (session.isOpen()) session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanHelper.java:1199");}
						try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanHelper.java:1200");}
						try { if (session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanHelper.java:1201");}
					}
				}

				label.setValue("");
			}
		}).start();
	}
}
