package ais.action.master.helper.profile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.TampilanELearningAction;
import ais.action.master.helper.PertemuanHelper;
import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.GeneralValueObject;
import ais.database.model.Mahasiswa;
import ais.database.model.NamaTugasKelompok;
import ais.database.model.NamaTugasKelompokPunyaMahasiswa;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.Tbmuser;
import ais.database.model.Tugas;
import ais.database.model.TugasKelompok;
import ais.database.model.TugasPertemuan;
import ais.database.model.Ujian;
import ais.database.model.VOPembelajaran;
import ais.database.model.file.FileFoto;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.PertemuanFileContent;
import ais.database.model.streaming.AudioPertemuan;
import ais.database.model.streaming.VideoPertemuan;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyLabelKecilSekali;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowStyled;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * <h3>ProfileUtil — Utilitas Rendering Materi, Tugas, dan Ujian pada Panel Profil e-Learning</h3>
 *
 * <p><b>Untuk apa:</b> Kelas ini menyediakan sekumpulan method statik yang merender daftar konten e-Learning
 * (materi/file pertemuan, tugas mandiri/kelompok, dan ujian) ke dalam komponen UI ZK (kolom grid, baris rows,
 * atau baris rows yang dapat dipilih dengan checkbox) serta memvalidasi apakah persyaratan akses konten sudah
 * terpenuhi oleh peserta. Kelas ini adalah "helper rendering" yang digunakan secara luas oleh semua kelas
 * profil (ProfileMahasiswa, ProfileDosen, ProfileGabungan, dll.) untuk menampilkan linimasa konten.
 *
 * <p><b>Dua mode penggunaan utama:</b>
 * <ol>
 *   <li><b>Mode tampil (read-only):</b> Render daftar materi/tugas/ujian sebagai tombol akses langsung.
 *       Klik tombol membuka file, link, atau panel pertemuan. Digunakan di panel profil mahasiswa.</li>
 *   <li><b>Mode konfigurasi syarat ({@code bisaDipilih=true}):</b> Render dengan checkbox di setiap item
 *       agar dosen/admin dapat memilih item mana saja yang menjadi "syarat akses" untuk item lain
 *       (misalnya: "peserta harus mengakses Materi 1 sebelum bisa mengikuti Ujian 2"). Konfigurasi
 *       disimpan sebagai JSON di field {@code syaratAkses} entitas. Mode ini digunakan di form pengaturan
 *       ujian/tugas.</li>
 * </ol>
 *
 * <p><b>Format kunci syarat:</b> Setiap item memiliki kunci unik berformat {@code "{id}_{SimpleClassName}"},
 * misalnya {@code "42_PertemuanFileContent"} atau {@code "7_PertemuanPunyaUjian"}. Kunci ini digunakan
 * sebagai key di {@link org.json.JSONObject} {@code syarat} untuk menandai item mana yang dipersyaratkan.
 *
 * <p><b>Jenis konten yang didukung (6 tipe):</b>
 * <ul>
 *   <li>{@link ais.database.model.file.PertemuanFileContent} — file/link/buku Google/GDrive (materi)</li>
 *   <li>{@link ais.database.model.Tugas} / {@link ais.database.model.TugasKelompok} — tugas individu/kelompok</li>
 *   <li>{@link ais.database.model.PertemuanPunyaUjian} — ujian online</li>
 *   <li>{@link ais.database.model.streaming.AudioPertemuan} — audio streaming/download</li>
 *   <li>{@link ais.database.model.streaming.VideoPertemuan} — video streaming/download</li>
 * </ul>
 *
 * <p><b>Mekanisme paging materi:</b> Method {@link #tampilkanMateri(Rows, Rows, Rows, Textbox, Textbox, Textbox,
 * TreeMap, boolean, boolean, EventListener, JSONObject, Set, GeneralValueObject, MyToolbarbutton)}
 * menggunakan atribut ZK pada komponen {@link Rows} ({@code "ditampilkan"}, {@code "index"}, {@code "selesai"})
 * sebagai state machine per-kategori. Bila jumlah item melebihi batas tampil awal, muncul tombol
 * "Tampilkan selanjutnya" yang menambah limit +3 dan merender ulang.
 *
 * <p><b>Penanganan syarat akses:</b> Method {@link #chekSyarat} membaca JSON {@code syaratAkses} dan
 * memverifikasi setiap item yang dipersyaratkan: apakah file sudah diakses ({@link ais.database.model.Tugas#apakahAkses}),
 * tugas sudah dikumpulkan (via count upload), atau ujian sudah diikuti (count hasil ujian). Bila ada yang
 * belum terpenuhi, muncul {@link ais.ui.util.MyMessageboxConfig} dengan daftar syarat yang belum dipenuhi
 * dan method mengembalikan {@code true} (akses ditolak).
 *
 * <p><b>Threading:</b> Semua method berjalan di thread ZK. Event listener (onClick tombol download) dieksekusi
 * dalam thread yang sama. Tidak ada operasi latar.
 *
 * <p><b>Pemeliharaan:</b> Bila menambahkan tipe konten baru, tambahkan cabang baru pada setiap method yang
 * mengenal tipe konten (tampilkanMateriColum, tampilkanMateri dengan 2 overload, dan chekSyarat). Urutan
 * cabang if-else penting karena beberapa tipe saling menurunkan (misalnya TugasKelompok extends Tugas).
 */
public class ProfileUtil {

	/**
	 * <b>Tujuan:</b> Merender setiap item konten e-Learning (materi, tugas, ujian, audio, video) sebagai
	 * {@link Column} yang mengandung tombol download/akses ke dalam komponen {@link Columns} header grid.
	 * Variasi ini digunakan pada tampilan tabel/kolom matriks (misalnya tabel pertemuan × konten) dimana
	 * setiap konten menjadi satu kolom.
	 *
	 * <p><b>Cara kerja:</b> Iterasi setiap entry di {@code dataMateri} (TreeMap terurut); untuk setiap
	 * entry, tentukan tipe item ({@code data[0]}) dan buat {@link Column} berisi
	 * {@link ais.ui.util.MyToolbarbuttonConfig} dengan label judul konten (dipotong di 150 karakter),
	 * ikon sesuai tipe file, dan listener {@code onClick} yang menangani pembukaan konten:
	 * <ul>
	 *   <li>{@link ais.database.model.file.PertemuanFileContent}: periksa syarat akses via
	 *       {@link #chekSyarat(VOPembelajaran, String)}, catat akses via {@link Pertemuan#masukkanData},
	 *       lalu buka sesuai tipe (Google Book via popup JS, GDrive, file lokal, atau link eksternal).
	 *       File yang bisa preview dibuka di {@link Common#displayWindow}; lainnya via popup JS atau
	 *       redirect mobile.</li>
	 *   <li>{@link ais.database.model.Tugas}: buka tab tugas di {@link PertemuanHelper#display} (tab index 3
	 *       untuk tugas, mendukung {@link ais.database.model.TugasKelompok} dan {@link ais.database.model.TugasPertemuan}).</li>
	 *   <li>{@link ais.database.model.PertemuanPunyaUjian}: hanya tampilkan bila ujian aktif ({@code getAktif()==true});
	 *       buka tab ujian di {@code PertemuanHelper.display} (tab index 6).</li>
	 *   <li>{@link ais.database.model.streaming.AudioPertemuan} / {@link ais.database.model.streaming.VideoPertemuan}:
	 *       periksa syarat, catat akses, buka GDrive atau link/file dengan cara yang sama seperti materi.</li>
	 * </ul>
	 * Semua tombol memiliki atribut {@code "janganDisabled"=true} agar tidak dinonaktifkan oleh ZK saat
	 * grid dalam mode loading.
	 *
	 * <p><b>Sifat/thread-safety:</b> Dieksekusi di thread ZK. Event listener onClick dipanggil pada thread
	 * event ZK yang sama.
	 *
	 * @param columns    komponen {@link Columns} header grid tempat kolom konten akan ditambahkan
	 * @param dataMateri peta terurut dari string kunci ke {@code Object[2]}: {@code [0]} = entitas konten,
	 *                   {@code [1]} = {@link Pertemuan} induk; dihasilkan oleh
	 *                   {@link ais.database.model.file.PertemuanFileContent#ambilMateri}
	 */
	public static void tampilkanMateriColum(Columns columns, TreeMap<String, Object[]> dataMateri) {
		for (String key : dataMateri.keySet()) {
			Object[] data = dataMateri.get(key);
			final Pertemuan pertemuan = (Pertemuan) data[1];
			if (data[0] instanceof PertemuanFileContent) {
				final PertemuanFileContent pertemuanFileContent = (PertemuanFileContent) data[0];

				String n = pertemuanFileContent.getNama() != null
						&& pertemuanFileContent.getNama().trim().equalsIgnoreCase("link")
								? pertemuanFileContent.getLink()
								: pertemuanFileContent.getNama();

				if (ProfileUiHelper.notBlank(pertemuanFileContent.getGoogleBook())) {
					n = pertemuanFileContent.getNama();
				}
				if (n == null) {
					n = "";
				}

				String isi = pertemuanFileContent.getKeterangan();
				if (isi != null && !isi.trim().isEmpty()) {
					n = isi;
				}

				Toolbarbutton downloadButton = new MyToolbarbuttonConfig(
						n.length() > 150 ? n.substring(0, 150) + "..." : n,
						ProfileUiHelper.notBlank(pertemuanFileContent.getGoogleBook()) ? "/img/Apps-Google-Play-Books-icon.png"
								: pertemuanFileContent.getLokasiFisik() != null ? "/img/svg/desktop-light.svg"
										: FileFoto.icon(pertemuanFileContent.getNama()));
				downloadButton.setTooltiptext("Download \"" + pertemuanFileContent.getNama() + "\"");
				downloadButton.setAttribute("janganDisabled", true);
				downloadButton.setStyle("font-size:9px;");

				Column column = new Column();
				column.setParent(columns);
				column.appendChild(downloadButton);

				downloadButton.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (ProfileUtil.chekSyarat(pertemuan.ambilVOPembelajaran(),
								pertemuanFileContent.getSyaratAkses())) {
							return;
						}

						pertemuan.masukkanData("bahan_perkulaiahan_" + pertemuanFileContent.getId());

						if (ProfileUiHelper.notBlank(pertemuanFileContent.getGoogleBook())) {
							if (Common.isMobile()) {
								ExecutionsCtrl.getCurrent().sendRedirect(pertemuanFileContent.getLink(), "_blank");
							} else {
								Clients.evalJavaScript("popupCenter({url: '" + ProfileUiHelper.js(pertemuanFileContent.getLink())
										+ "', title: 'Book', w: 1200, h: 600});");
							}
						} else if (pertemuanFileContent.getGdrive() != null) {
							pertemuanFileContent.tampilGDrive(null);
						} else {

							String link = pertemuanFileContent == null ? null
									: (pertemuanFileContent.getLink() == null
											|| pertemuanFileContent.getLink().isEmpty() ? null
													: pertemuanFileContent.getLink());

							if (pertemuanFileContent != null
									&& (link == null || link.trim().isEmpty() || !link.startsWith("http"))) {
								link = pertemuanFileContent.createLinkUri();
								if (link != null) {
								}
							}

							if (pertemuanFileContent != null && link != null && !link.trim().isEmpty()) {

								if (pertemuanFileContent.bisaPreview()) {
									Common.displayWindow(pertemuanFileContent.merupakanGambar(), link, true, "95%",
											"95%", true, pertemuanFileContent);
								} else {
									if (Common.isMobile()) {
										ExecutionsCtrl.getCurrent().sendRedirect(link, "_blank");
									} else {
										Clients.evalJavaScript(
												"popupCenter({url: '" + ProfileUiHelper.js(link) + "', title: 'data', w: 1200, h: 600});");
									}
								}
							} else {
								MyMessageboxConfig.show(
				"Mohon maaf, Bapak/Ibu, berkas yang Anda akses tidak dapat ditemukan pada sistem. Langkah yang dapat dilakukan: (1) pastikan berkas masih tersedia dan belum dihapus; (2) muat ulang halaman lalu coba akses kembali; (3) apabila kendala masih berlanjut, hubungi Admin atau bagian terkait untuk bantuan lebih lanjut.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							}
						}
					}
				});
			}

			else if (data[0] instanceof Tugas) {

				final Tugas tugas = (Tugas) data[0];
				if (tugas.getJudultugas() != null && !tugas.getJudultugas().trim().isEmpty()) {
					String n = tugas.getJudultugas();
					if (n == null) {
						n = "";
					}

					String icon = "Status-mail-task-icon.png";
					if (tugas instanceof TugasKelompok) {
						icon = "Healthcare-Groups-icon.png";
					}

					Toolbarbutton downloadButton = new MyToolbarbuttonConfig(
							n.length() > 150 ? n.substring(0, 150) + "..." : n, "/img/" + icon);

					downloadButton.setAttribute("janganDisabled", true);

					downloadButton.setStyle("font-size:9px;");

					Column column = new Column();
					column.setParent(columns);
					column.appendChild(downloadButton);

					downloadButton.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Tbmuser tbmuser = Common.getCurrentUser();
							if (tugas instanceof Pertemuan) {

								new PertemuanHelper(tbmuser.getMahasiswa(), tbmuser.getBiodataCalonMahasiswa())
										.display(pertemuan, new DataLoader() {

											@Override
											public void loadData(Object value) {

											}
										}, 3);
							} else if (tugas instanceof TugasKelompok) {
								TugasKelompok tugasKelompok = (TugasKelompok) tugas;
								new PertemuanHelper(tbmuser.getMahasiswa(), tbmuser.getBiodataCalonMahasiswa())
										.display(pertemuan, new DataLoader() {

											@Override
											public void loadData(Object value) {

											}
										}, 3, null, tugasKelompok, null, null, null);
							} else {
								TugasPertemuan tugasPertemuan = (TugasPertemuan) tugas;
								new PertemuanHelper(tbmuser.getMahasiswa(), tbmuser.getBiodataCalonMahasiswa())
										.display(pertemuan, new DataLoader() {

											@Override
											public void loadData(Object value) {

											}
										}, 3, tugasPertemuan, null, null, null, null);
							}

						}
					});

				}

			} else if (data[0] instanceof PertemuanPunyaUjian) {
				PertemuanPunyaUjian pertemuanPunyaUjian = (PertemuanPunyaUjian) data[0];
				if (pertemuanPunyaUjian.getUjian() != null && Boolean.TRUE.equals(pertemuanPunyaUjian.getUjian().getAktif())) {
					String n = pertemuanPunyaUjian.getNama();
					if (n == null) {
						n = "";
					}

					Toolbarbutton downloadButton = new MyToolbarbuttonConfig(
							n.length() > 150 ? n.substring(0, 150) + "..." : n, "/img/Text-Edit-icon.png");

					downloadButton.setAttribute("janganDisabled", true);
					downloadButton.setStyle("font-size:9px;");

					Column column = new Column();
					column.setParent(columns);
					column.appendChild(downloadButton);

					downloadButton.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Tbmuser tbmuser = Common.getCurrentUser();
							new PertemuanHelper(tbmuser.getMahasiswa(), tbmuser.getBiodataCalonMahasiswa())
									.display(pertemuan, new DataLoader() {

										@Override
										public void loadData(Object value) {

										}
									}, 6);

						}
					});
				}
			} else if (data[0] instanceof AudioPertemuan) {
				final AudioPertemuan audioPertemuan = (AudioPertemuan) data[0];

				String n = audioPertemuan == null ? ""
						: (audioPertemuan.getLink() == null || audioPertemuan.getLink().isEmpty()
								? audioPertemuan.getNama()
								: audioPertemuan.getLink());
				if (n == null) {
					n = "";
				}
				String isi = audioPertemuan.getKeteranganTambahan();
				if (isi != null && !isi.trim().isEmpty()) {
					n = isi;
				}

				Toolbarbutton downloadButton = new MyToolbarbuttonConfig(
						n.length() > 150 ? n.substring(0, 150) + "..." : n, FileFoto.icon("mp3"));
				downloadButton.setTooltiptext("Download \"" + audioPertemuan.getNama() + "\"");
				downloadButton.setAttribute("janganDisabled", true);
				downloadButton.setStyle("font-size:9px;");

				Column column = new Column();
				column.setParent(columns);
				column.appendChild(downloadButton);

				downloadButton.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (ProfileUtil.chekSyarat(pertemuan.ambilVOPembelajaran(), audioPertemuan.getSyaratAkses())) {
							return;
						}

						pertemuan.masukkanData("audio_" + audioPertemuan.getId());

						if (audioPertemuan.getGdrive() != null) {
							audioPertemuan.tampilGDrive(null);
						} else {

							String link = audioPertemuan == null ? null
									: (audioPertemuan.getLink() == null || audioPertemuan.getLink().isEmpty() ? null
											: audioPertemuan.getLink());

							if (audioPertemuan != null
									&& (link == null || link.trim().isEmpty() || !link.startsWith("http"))) {
								link = audioPertemuan.createLinkUri();
								if (link != null) {
								}
							}

							if (audioPertemuan != null && link != null && !link.trim().isEmpty()) {

								if (Common.isMobile()) {
									ExecutionsCtrl.getCurrent().sendRedirect(link, "_blank");
								} else {
									Clients.evalJavaScript(
											"popupCenter({url: '" + ProfileUiHelper.js(link) + "', title: 'data', w: 1200, h: 600});");
								}
							} else {
								MyMessageboxConfig.show(
				"Mohon maaf, Bapak/Ibu, berkas yang Anda akses tidak dapat ditemukan pada sistem. Langkah yang dapat dilakukan: (1) pastikan berkas masih tersedia dan belum dihapus; (2) muat ulang halaman lalu coba akses kembali; (3) apabila kendala masih berlanjut, hubungi Admin atau bagian terkait untuk bantuan lebih lanjut.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							}
						}
					}
				});

			} else if (data[0] instanceof VideoPertemuan) {

				final VideoPertemuan videoPertemuan = (VideoPertemuan) data[0];

				String n = videoPertemuan == null ? ""
						: (videoPertemuan.getLink() == null || videoPertemuan.getLink().isEmpty()
								? videoPertemuan.getNama()
								: videoPertemuan.getLink());
				if (n == null) {
					n = "";
				}
				String isi = videoPertemuan.getKeteranganTambahan();
				if (isi != null && !isi.trim().isEmpty()) {
					n = isi;
				}

				Toolbarbutton downloadButton = new MyToolbarbuttonConfig(
						n.length() > 150 ? n.substring(0, 150) + "..." : n, FileFoto.icon("mp4"));
				downloadButton.setTooltiptext("Download \"" + videoPertemuan.getNama() + "\"");
				downloadButton.setAttribute("janganDisabled", true);
				downloadButton.setStyle("font-size:9px;");

				Column column = new Column();
				column.setParent(columns);
				column.appendChild(downloadButton);

				downloadButton.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (ProfileUtil.chekSyarat(pertemuan.ambilVOPembelajaran(), videoPertemuan.getSyaratAkses())) {
							return;
						}

						pertemuan.masukkanData("video_" + videoPertemuan.getId());

						if (videoPertemuan.getGdrive() != null) {
							videoPertemuan.tampilGDrive(null);
						} else {

							String link = videoPertemuan == null ? null
									: (videoPertemuan.getLink() == null || videoPertemuan.getLink().isEmpty() ? null
											: videoPertemuan.getLink());

							if (videoPertemuan != null
									&& (link == null || link.trim().isEmpty() || !link.startsWith("http"))) {
								link = videoPertemuan.createLinkUri();
								if (link != null) {
								}
							}

							if (videoPertemuan != null && link != null && !link.trim().isEmpty()) {

								if (Common.isMobile()) {
									ExecutionsCtrl.getCurrent().sendRedirect(link, "_blank");
								} else {
									Clients.evalJavaScript(
											"popupCenter({url: '" + ProfileUiHelper.js(link) + "', title: 'data', w: 1200, h: 600});");
								}
							} else {
								MyMessageboxConfig.show(
				"Mohon maaf, Bapak/Ibu, berkas yang Anda akses tidak dapat ditemukan pada sistem. Langkah yang dapat dilakukan: (1) pastikan berkas masih tersedia dan belum dihapus; (2) muat ulang halaman lalu coba akses kembali; (3) apabila kendala masih berlanjut, hubungi Admin atau bagian terkait untuk bantuan lebih lanjut.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							}
						}
					}
				});

			}

		}
	}

	/**
	 * <b>Tujuan:</b> Merender daftar konten e-Learning (materi, tugas, ujian, audio, video) sebagai baris
	 * {@link ais.ui.util.MyRowStyled} ke dalam komponen {@link Rows} yang diberikan, membuat tombol
	 * download/akses per item dalam layout Hbox. Versi sederhana tanpa checkbox, filter teks, atau syarat —
	 * digunakan untuk tampilan profil yang hanya menampilkan semua konten dari serangkaian pertemuan.
	 *
	 * <p><b>Cara kerja:</b> Panggil {@link ais.database.model.file.PertemuanFileContent#ambilMateri} untuk
	 * mengumpulkan semua item dari daftar pertemuan, lalu iterasi dan buat baris Hbox per item. Setiap item
	 * mendapatkan tombol {@link ais.ui.util.MyToolbarbuttonConfig} dan listener {@code onClick} yang membuka
	 * konten dengan cara yang identik dengan {@link #tampilkanMateriColum}. Perbedaan layout: di sini item
	 * menggunakan {@link ais.ui.util.MyRowStyled} (baris grid/listbox) bukan {@link Column}, sehingga item
	 * tampil secara vertikal berurutan.
	 *
	 * <p><b>Perbedaan dari tampilkanMateriColum:</b> Tidak menggunakan {@code dataMateri} yang sudah
	 * ter-precompute sebagai parameter; method ini sendiri memanggil {@code ambilMateri(pertemuans, false, null, tbmuser)}.
	 * Tidak ada syarat akses (syarat tidak diperiksa), tidak ada checkbox, tidak ada pagination.
	 *
	 * <p><b>Sifat/thread-safety:</b> Dieksekusi di thread ZK.
	 *
	 * @param rowsPerkuliahan komponen {@link Rows} tempat baris konten akan ditambahkan
	 * @param pertemuans      peta terurut dari string kunci ke ID {@link Pertemuan}; semua pertemuan dalam
	 *                        peta ini akan diambil kontennya
	 */
	public static void tampilkanMateri(Rows rowsPerkuliahan, TreeMap<String, Long> pertemuans) {
		Tbmuser tbmuser = Common.getCurrentUser();
		TreeMap<String, Object[]> dataMateri = PertemuanFileContent.ambilMateri(pertemuans, false, null, tbmuser);
		for (String key : dataMateri.keySet()) {
			Object[] data = dataMateri.get(key);
			final Pertemuan pertemuan = (Pertemuan) data[1];
			if (data[0] instanceof PertemuanFileContent) {
				final PertemuanFileContent pertemuanFileContent = (PertemuanFileContent) data[0];

				String n = pertemuanFileContent.getNama() != null
						&& pertemuanFileContent.getNama().trim().equalsIgnoreCase("link")
								? pertemuanFileContent.getLink()
								: pertemuanFileContent.getNama();

				if (ProfileUiHelper.notBlank(pertemuanFileContent.getGoogleBook())) {
					n = pertemuanFileContent.getNama();
				}
				if (n == null) {
					n = "";
				}

				String isi = pertemuanFileContent.getKeterangan();
				if (isi != null && !isi.trim().isEmpty()) {
					n = isi;
				}

				Toolbarbutton downloadButton = new MyToolbarbuttonConfig(
						n.length() > 150 ? n.substring(0, 150) + "..." : n,
						ProfileUiHelper.notBlank(pertemuanFileContent.getGoogleBook()) ? "/img/Apps-Google-Play-Books-icon.png"
								: pertemuanFileContent.getLokasiFisik() != null ? "/img/svg/desktop-light.svg"
										: FileFoto.icon(pertemuanFileContent.getNama()));
				downloadButton.setTooltiptext("Download \"" + pertemuanFileContent.getNama() + "\"");
				downloadButton.setAttribute("janganDisabled", true);
				downloadButton.setStyle("font-size:9px;");

				MyRowStyled row = new MyRowStyled();
				row.setParent(rowsPerkuliahan);
				Hbox hbox = new Hbox();
				row.appendChild(hbox);

				hbox.appendChild(downloadButton);

				downloadButton.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (ProfileUtil.chekSyarat(pertemuan.ambilVOPembelajaran(),
								pertemuanFileContent.getSyaratAkses())) {
							return;
						}

						pertemuan.masukkanData("bahan_perkulaiahan_" + pertemuanFileContent.getId());

						if (ProfileUiHelper.notBlank(pertemuanFileContent.getGoogleBook())) {
							if (Common.isMobile()) {
								ExecutionsCtrl.getCurrent().sendRedirect(pertemuanFileContent.getLink(), "_blank");
							} else {
								Clients.evalJavaScript("popupCenter({url: '" + ProfileUiHelper.js(pertemuanFileContent.getLink())
										+ "', title: 'Book', w: 1200, h: 600});");
							}
						} else if (pertemuanFileContent.getGdrive() != null) {
							pertemuanFileContent.tampilGDrive(null);
						} else {

							String link = pertemuanFileContent == null ? null
									: (pertemuanFileContent.getLink() == null
											|| pertemuanFileContent.getLink().isEmpty() ? null
													: pertemuanFileContent.getLink());

							if (pertemuanFileContent != null
									&& (link == null || link.trim().isEmpty() || !link.startsWith("http"))) {
								link = pertemuanFileContent.createLinkUri();
								if (link != null) {
								}
							}

							if (pertemuanFileContent != null && link != null && !link.trim().isEmpty()) {

								if (pertemuanFileContent.bisaPreview()) {
									Common.displayWindow(pertemuanFileContent.merupakanGambar(), link, true, "95%",
											"95%", true, pertemuanFileContent);
								} else {
									if (Common.isMobile()) {
										ExecutionsCtrl.getCurrent().sendRedirect(link, "_blank");
									} else {
										Clients.evalJavaScript(
												"popupCenter({url: '" + ProfileUiHelper.js(link) + "', title: 'data', w: 1200, h: 600});");
									}
								}
							} else {
								MyMessageboxConfig.show(
				"Mohon maaf, Bapak/Ibu, berkas yang Anda akses tidak dapat ditemukan pada sistem. Langkah yang dapat dilakukan: (1) pastikan berkas masih tersedia dan belum dihapus; (2) muat ulang halaman lalu coba akses kembali; (3) apabila kendala masih berlanjut, hubungi Admin atau bagian terkait untuk bantuan lebih lanjut.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							}
						}
					}
				});
			}

			else if (data[0] instanceof Tugas) {

				final Tugas tugas = (Tugas) data[0];
				if (tugas.getJudultugas() != null && !tugas.getJudultugas().trim().isEmpty()) {
					String n = tugas.getJudultugas();
					if (n == null) {
						n = "";
					}

					String icon = "Status-mail-task-icon.png";
					if (tugas instanceof TugasKelompok) {
						icon = "Healthcare-Groups-icon.png";
					}

					Toolbarbutton downloadButton = new MyToolbarbuttonConfig(
							n.length() > 150 ? n.substring(0, 150) + "..." : n, "/img/" + icon);

					downloadButton.setAttribute("janganDisabled", true);

					downloadButton.setStyle("font-size:9px;");

					MyRowStyled row = new MyRowStyled();
					row.setParent(rowsPerkuliahan);
					Hbox hbox = new Hbox();
					row.appendChild(hbox);

					hbox.appendChild(downloadButton);

					downloadButton.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Tbmuser tbmuser = Common.getCurrentUser();
							if (tugas instanceof Pertemuan) {

								new PertemuanHelper(tbmuser.getMahasiswa(), tbmuser.getBiodataCalonMahasiswa())
										.display(pertemuan, new DataLoader() {

											@Override
											public void loadData(Object value) {

											}
										}, 3);
							} else if (tugas instanceof TugasKelompok) {
								TugasKelompok tugasKelompok = (TugasKelompok) tugas;
								new PertemuanHelper(tbmuser.getMahasiswa(), tbmuser.getBiodataCalonMahasiswa())
										.display(pertemuan, new DataLoader() {

											@Override
											public void loadData(Object value) {

											}
										}, 3, null, tugasKelompok, null, null, null);
							} else {
								TugasPertemuan tugasPertemuan = (TugasPertemuan) tugas;
								new PertemuanHelper(tbmuser.getMahasiswa(), tbmuser.getBiodataCalonMahasiswa())
										.display(pertemuan, new DataLoader() {

											@Override
											public void loadData(Object value) {

											}
										}, 3, tugasPertemuan, null, null, null, null);
							}

						}
					});

				}

			} else if (data[0] instanceof PertemuanPunyaUjian) {
				PertemuanPunyaUjian pertemuanPunyaUjian = (PertemuanPunyaUjian) data[0];

				String n = pertemuanPunyaUjian.getNama();
				if (n == null) {
					n = "";
				}

				Toolbarbutton downloadButton = new MyToolbarbuttonConfig(
						n.length() > 150 ? n.substring(0, 150) + "..." : n, "/img/Text-Edit-icon.png");

				downloadButton.setAttribute("janganDisabled", true);
				downloadButton.setStyle("font-size:9px;");

				MyRowStyled row = new MyRowStyled();
				row.setParent(rowsPerkuliahan);
				Hbox hbox = new Hbox();
				row.appendChild(hbox);

				hbox.appendChild(downloadButton);

				downloadButton.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Tbmuser tbmuser = Common.getCurrentUser();
						new PertemuanHelper(tbmuser.getMahasiswa(), tbmuser.getBiodataCalonMahasiswa())
								.display(pertemuan, new DataLoader() {

									@Override
									public void loadData(Object value) {

									}
								}, 6);

					}
				});

			} else if (data[0] instanceof AudioPertemuan) {
				final AudioPertemuan audioPertemuan = (AudioPertemuan) data[0];

				String n = audioPertemuan == null ? ""
						: (audioPertemuan.getLink() == null || audioPertemuan.getLink().isEmpty()
								? audioPertemuan.getNama()
								: audioPertemuan.getLink());
				if (n == null) {
					n = "";
				}
				String isi = audioPertemuan.getKeteranganTambahan();
				if (isi != null && !isi.trim().isEmpty()) {
					n = isi;
				}

				Toolbarbutton downloadButton = new MyToolbarbuttonConfig(
						n.length() > 150 ? n.substring(0, 150) + "..." : n, FileFoto.icon("mp3"));
				downloadButton.setTooltiptext("Download \"" + audioPertemuan.getNama() + "\"");
				downloadButton.setAttribute("janganDisabled", true);
				downloadButton.setStyle("font-size:9px;");

				MyRowStyled row = new MyRowStyled();
				row.setParent(rowsPerkuliahan);
				Hbox hbox = new Hbox();
				row.appendChild(hbox);

				hbox.appendChild(downloadButton);

				downloadButton.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (ProfileUtil.chekSyarat(pertemuan.ambilVOPembelajaran(), audioPertemuan.getSyaratAkses())) {
							return;
						}

						pertemuan.masukkanData("audio_" + audioPertemuan.getId());

						if (audioPertemuan.getGdrive() != null) {
							audioPertemuan.tampilGDrive(null);
						} else {

							String link = audioPertemuan == null ? null
									: (audioPertemuan.getLink() == null || audioPertemuan.getLink().isEmpty() ? null
											: audioPertemuan.getLink());

							if (audioPertemuan != null
									&& (link == null || link.trim().isEmpty() || !link.startsWith("http"))) {
								link = audioPertemuan.createLinkUri();
								if (link != null) {
								}
							}

							if (audioPertemuan != null && link != null && !link.trim().isEmpty()) {

								if (Common.isMobile()) {
									ExecutionsCtrl.getCurrent().sendRedirect(link, "_blank");
								} else {
									Clients.evalJavaScript(
											"popupCenter({url: '" + ProfileUiHelper.js(link) + "', title: 'data', w: 1200, h: 600});");
								}
							} else {
								MyMessageboxConfig.show(
				"Mohon maaf, Bapak/Ibu, berkas yang Anda akses tidak dapat ditemukan pada sistem. Langkah yang dapat dilakukan: (1) pastikan berkas masih tersedia dan belum dihapus; (2) muat ulang halaman lalu coba akses kembali; (3) apabila kendala masih berlanjut, hubungi Admin atau bagian terkait untuk bantuan lebih lanjut.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							}
						}
					}
				});

			} else if (data[0] instanceof VideoPertemuan) {

				final VideoPertemuan videoPertemuan = (VideoPertemuan) data[0];

				String n = videoPertemuan == null ? ""
						: (videoPertemuan.getLink() == null || videoPertemuan.getLink().isEmpty()
								? videoPertemuan.getNama()
								: videoPertemuan.getLink());
				if (n == null) {
					n = "";
				}
				String isi = videoPertemuan.getKeteranganTambahan();
				if (isi != null && !isi.trim().isEmpty()) {
					n = isi;
				}

				Toolbarbutton downloadButton = new MyToolbarbuttonConfig(
						n.length() > 150 ? n.substring(0, 150) + "..." : n, FileFoto.icon("mp4"));
				downloadButton.setTooltiptext("Download \"" + videoPertemuan.getNama() + "\"");
				downloadButton.setAttribute("janganDisabled", true);
				downloadButton.setStyle("font-size:9px;");

				MyRowStyled row = new MyRowStyled();
				row.setParent(rowsPerkuliahan);
				Hbox hbox = new Hbox();
				row.appendChild(hbox);

				hbox.appendChild(downloadButton);

				downloadButton.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (ProfileUtil.chekSyarat(pertemuan.ambilVOPembelajaran(), videoPertemuan.getSyaratAkses())) {
							return;
						}

						pertemuan.masukkanData("video_" + videoPertemuan.getId());

						if (videoPertemuan.getGdrive() != null) {
							videoPertemuan.tampilGDrive(null);
						} else {

							String link = videoPertemuan == null ? null
									: (videoPertemuan.getLink() == null || videoPertemuan.getLink().isEmpty() ? null
											: videoPertemuan.getLink());

							if (videoPertemuan != null
									&& (link == null || link.trim().isEmpty() || !link.startsWith("http"))) {
								link = videoPertemuan.createLinkUri();
								if (link != null) {
								}
							}

							if (videoPertemuan != null && link != null && !link.trim().isEmpty()) {

								if (Common.isMobile()) {
									ExecutionsCtrl.getCurrent().sendRedirect(link, "_blank");
								} else {
									Clients.evalJavaScript(
											"popupCenter({url: '" + ProfileUiHelper.js(link) + "', title: 'data', w: 1200, h: 600});");
								}
							} else {
								MyMessageboxConfig.show(
				"Mohon maaf, Bapak/Ibu, berkas yang Anda akses tidak dapat ditemukan pada sistem. Langkah yang dapat dilakukan: (1) pastikan berkas masih tersedia dan belum dihapus; (2) muat ulang halaman lalu coba akses kembali; (3) apabila kendala masih berlanjut, hubungi Admin atau bagian terkait untuk bantuan lebih lanjut.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							}
						}
					}
				});

			}

		}
	}

	/**
	 * <b>Tujuan:</b> Delegator 1-baris ke {@link #chekSyarat(VOPembelajaran, Mahasiswa, BiodataCalonMahasiswa, String)}
	 * yang secara otomatis mengambil data peserta dari pengguna yang sedang login. Digunakan oleh listener
	 * {@code onClick} tombol download di seluruh kelas ini sebagai gerbang akses sebelum membuka konten.
	 *
	 * <p><b>Cara kerja:</b> Ambil {@link Tbmuser} via {@link Common#getCurrentUser()}, ekstrak {@code mahasiswa}
	 * dan {@code biodataCalonMahasiswa} (bisa null bila pengguna bukan peserta), lalu delegasikan ke overload
	 * utama dengan empat parameter. Bila pengguna bukan mahasiswa maupun calon mahasiswa, overload utama
	 * menganggap tidak ada syarat yang perlu dicek dan mengembalikan {@code false} (akses diizinkan).
	 *
	 * <p><b>Nilai kembali:</b> {@code true} berarti syarat BELUM terpenuhi (akses ditolak); {@code false}
	 * berarti syarat sudah terpenuhi atau tidak ada syarat (akses diizinkan). Caller harus memeriksa nilai
	 * kembali: jika {@code true}, hentikan aksi (sudah ada pesan error ke pengguna dari overload utama).
	 *
	 * @param pembelajaran   objek pembelajaran ({@link VOPembelajaran}) yang dimiliki item konten
	 * @param syaratAkses    string JSON syarat akses milik item konten yang akan dibuka; {@code null} atau
	 *                       JSON kosong berarti tidak ada syarat
	 * @return {@code true} bila akses ditolak (syarat belum terpenuhi), {@code false} bila boleh diakses
	 * @throws Exception bila terjadi kesalahan akses database saat memeriksa tugas/ujian yang dipersyaratkan
	 */
	public static boolean chekSyarat(VOPembelajaran pembelajaran, String syaratAkses) throws Exception {
		Tbmuser tbmuser = Common.getCurrentUser();
		return chekSyarat(pembelajaran, tbmuser == null ? null : tbmuser.getMahasiswa(),
				tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa(), syaratAkses);
	}

	/**
	 * <b>Tujuan:</b> Memeriksa apakah peserta sudah memenuhi semua persyaratan akses yang dikonfigurasi untuk
	 * suatu item konten e-Learning (misalnya: "harus sudah mengakses Materi 1 dan mengumpulkan Tugas 2 sebelum
	 * bisa membuka Ujian 3"). Bila ada syarat yang belum terpenuhi, menampilkan pesan peringatan dan
	 * mengembalikan {@code true} untuk menghentikan aksi.
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Parse {@code syaratAkses} sebagai {@link org.json.JSONObject}. Bila null/kosong atau peserta
	 *       tidak terdefinisi, kembalikan {@code false} langsung (tidak ada syarat, akses bebas).</li>
	 *   <li>Muat semua item konten dari objek pembelajaran via {@link ais.database.model.file.PertemuanFileContent#ambilMateri}.</li>
	 *   <li>Untuk setiap item, hitung kunci unik {@code "{id}_{SimpleClassName}"} dan periksa apakah kunci
	 *       tersebut ada di {@code syarat} JSON (artinya item ini dipersyaratkan). Bila ya, verifikasi status
	 *       peserta:
	 *       <ul>
	 *         <li>{@link ais.database.model.file.PertemuanFileContent}: cek akses via
	 *             {@link ais.database.model.Tugas#apakahAkses} (file sudah dibuka oleh peserta).</li>
	 *         <li>{@link ais.database.model.Tugas} individu: cek count upload via
	 *             {@link ais.database.model.Tugas#ambilJumlahTugasFileContent}; support Mahasiswa dan
	 *             BiodataCalonMahasiswa; bagi yang ada di {@code mhsYgTidakIkut} dilewati.</li>
	 *         <li>{@link ais.database.model.TugasKelompok}: cek via query Hibernate apakah kelompok sudah
	 *             mengumpulkan (NamaTugasKelompokPunyaMahasiswa + FileFotoLain).</li>
	 *         <li>{@link ais.database.model.PertemuanPunyaUjian}: cek count {@link ais.database.model.HasilUjianMahasiswa}
	 *             via {@link ais.database.model.PertemuanPunyaUjian#ambilHasilUjianMahasiswa}.</li>
	 *         <li>{@link ais.database.model.streaming.AudioPertemuan} /
	 *             {@link ais.database.model.streaming.VideoPertemuan}: cek akses via {@code apakahAkses}.</li>
	 *       </ul></li>
	 *   <li>Kumpulkan semua syarat yang belum terpenuhi dalam {@code syaratAlert} (Set<String>).</li>
	 *   <li>Bila ada syarat yang belum terpenuhi, tampilkan {@link ais.ui.util.MyMessageboxConfig} dengan
	 *       daftar lengkap dan kembalikan {@code true}.</li>
	 *   <li>Bila semua terpenuhi, kembalikan {@code false}.</li>
	 * </ol>
	 *
	 * <p><b>Catatan penting:</b> Method ini menggunakan {@link HibernateUtil#currentSession()} untuk query
	 * TugasKelompok (harus dipanggil dari thread ZK yang sudah memiliki sesi aktif). Semua query lain
	 * menggunakan cache in-memory (GeneralValueObject, dll.).
	 *
	 * <p><b>Nama label syarat:</b> Setiap item yang dipersyaratkan mendapatkan label yang menyebut nama item
	 * dan pertemuan-ke (bila mode konfigurasi, format "Pert.ke-N") atau nama perkuliahan (bila mode global).
	 * Ini membantu peserta mengetahui persis materi/tugas/ujian mana yang belum diselesaikan.
	 *
	 * @param pembelajaran             objek pembelajaran yang dimiliki item konten yang ingin diakses
	 * @param mahasiswa                peserta mahasiswa yang sedang login; boleh {@code null} (calonMhs dicek)
	 * @param biodataCalonMahasiswa    peserta calon mahasiswa yang sedang login; boleh {@code null}
	 * @param syaratAkses              string JSON syarat akses; boleh {@code null} atau JSON kosong (bebas akses)
	 * @return {@code true} bila ada syarat yang belum terpenuhi (akses ditolak, pesan sudah ditampilkan);
	 *         {@code false} bila semua syarat terpenuhi atau tidak ada syarat
	 * @throws Exception bila terjadi kesalahan akses database selama pemeriksaan
	 */
	public static boolean chekSyarat(VOPembelajaran pembelajaran, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, String syaratAkses) throws Exception {

		TreeMap<String, Long> pertemuans = pembelajaran.ambilPertemuan();
		JSONObject syarat = new JSONObject(syaratAkses);
		if (syarat == null || syarat.length() == 0 || pertemuans == null || pertemuans.isEmpty()
				|| (mahasiswa == null && biodataCalonMahasiswa == null)) {
			return false;
		}

		Set<String> syaratAlert = new HashSet<String>();
		Tbmuser tbmuser = Common.getCurrentUser();
		TreeMap<String, Object[]> dataMateri = PertemuanFileContent.ambilMateri(pertemuans, false, null, tbmuser);
		for (String key : dataMateri.keySet()) {
			Object[] data = dataMateri.get(key);
			Pertemuan pertemuan = (Pertemuan) data[1];

			GeneralValueObject o = (GeneralValueObject) data[0];
			String keyData = o.getId() + "_" + o.getClass().getSimpleName();

			if (o instanceof PertemuanFileContent) {
				PertemuanFileContent pertemuanFileContent = (PertemuanFileContent) o;
				String n = pertemuanFileContent.getNama() != null
						&& pertemuanFileContent.getNama().trim().equalsIgnoreCase("link")
								? pertemuanFileContent.getLink()
								: pertemuanFileContent.getNama();

				if (ProfileUiHelper.notBlank(pertemuanFileContent.getGoogleBook())) {
					n = pertemuanFileContent.getNama();
				}
				if (n == null) {
					n = "";
				}

				String isi = pertemuanFileContent.getKeterangan();
				if (isi != null && !isi.trim().isEmpty()) {
					n = isi;
				}

				if (syarat == null)
					n += " (" + pertemuan.ambilVOPembelajaran().infoSangatSimple() + ")";
				else
					n += " (Pert.ke-" + pertemuan.getPertemuanKe() + " - "
							+ (pertemuan.getTanggal() == null ? "" : Common.dateFormat11.get().format(pertemuan.getTanggal()))
							+ ")";

				if (syarat != null && !syarat.isNull(keyData)) {
					if (mahasiswa != null) {

						boolean akses = Tugas.apakahAkses(pertemuan,
								"bahan_perkulaiahan_" + pertemuanFileContent.getId(), mahasiswa.getId());

						if (!akses) {
							syaratAlert.add("Anda belum akses materi \"" + n + "\"");
						}
					}
				}
			} else if (o instanceof Tugas) {
				Tugas tugas = (Tugas) o;
				if (tugas.getJudultugas() != null && !tugas.getJudultugas().trim().isEmpty()) {
					String n = tugas.getJudultugas();
					if (n == null) {
						n = "";
					}

					if (syarat == null)
						n += " (" + pertemuan.ambilVOPembelajaran().infoSangatSimple() + ")";
					else
						n += " (Pert.ke-" + pertemuan.getPertemuanKe() + " - " + (pertemuan.getTanggal() == null ? ""
								: Common.dateFormat11.get().format(pertemuan.getTanggal())) + ")";

					if (syarat != null && !syarat.isNull(keyData)) {
						if (mahasiswa != null) {

							if (tugas.getMhsYgTidakIkut().contains("," + mahasiswa.getId() + ",")) {

							} else {
								if (tugas instanceof TugasKelompok) {

									Session session = HibernateUtil.currentSession();
									Long namaTugasKelompokId = (Long) session
											.createCriteria(NamaTugasKelompokPunyaMahasiswa.class)
											.setProjection(Projections.property("namaTugasKelompok.id"))
											.createAlias("namaTugasKelompok", "namaTugasKelompok")
											.add(Restrictions.eq("namaTugasKelompok.tugasKelompok", tugas))
											.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1)
											.uniqueResult();

									if (namaTugasKelompokId == null) {
										syaratAlert.add("Kelompok Anda belum mengumpulkan tugas \""
												+ tugas.getJudultugas() + "\"");
									} else {
										FileFotoLain fileFotoLain = FileFotoLain.ambil(false, namaTugasKelompokId,
												NamaTugasKelompok.class.getName(), LampiranLain.class);
										if (fileFotoLain == null) {
											syaratAlert.add("Kelompok Anda belum mengumpulkan tugas \""
													+ tugas.getJudultugas() + "\"");
										}
									}

								} else {
									int jml = tugas.ambilJumlahTugasFileContent(mahasiswa);
									if (jml == 0) {
										syaratAlert
												.add("Anda belum mengumpulkan tugas \"" + tugas.getJudultugas() + "\"");

									}
								}
							}
						} else if (biodataCalonMahasiswa != null) {
							if (tugas.getMhsYgTidakIkut().contains("," + biodataCalonMahasiswa.getId() + ",")) {

							} else {
								int jml = tugas.ambilJumlahTugasFileContent(biodataCalonMahasiswa);
								if (jml == 0) {
									syaratAlert.add("Anda belum mengumpulkan tugas \"" + tugas.getJudultugas() + "\"");
								}
							}
						}
					}
				}
			} else if (o instanceof PertemuanPunyaUjian) {
				final PertemuanPunyaUjian pertemuanPunyaUjian = (PertemuanPunyaUjian) o;

				String n = pertemuanPunyaUjian.getNama();
				if (n == null) {
					n = "";
				}

				if (syarat == null)
					n += " (" + pertemuan.ambilVOPembelajaran().infoSangatSimple() + ")";
				else
					n += " (Pert.ke-" + pertemuan.getPertemuanKe() + " - "
							+ (pertemuan.getTanggal() == null ? "" : Common.dateFormat11.get().format(pertemuan.getTanggal()))
							+ ")";

				if (syarat != null && !syarat.isNull(keyData)) {

					if (mahasiswa != null) {

						if (pertemuanPunyaUjian.getMhsYgTidakIkut().contains("," + mahasiswa.getId() + ",")) {

						} else {
							List<Long> d = pertemuanPunyaUjian.ambilHasilUjianMahasiswa(mahasiswa, null, true);
							int jml = d.size();

							if (jml == 0) {

								syaratAlert.add("Anda belum mengikuti ujian \"" + pertemuanPunyaUjian.getNama() + "\"");

							}
						}
					} else if (biodataCalonMahasiswa != null) {
						if (pertemuanPunyaUjian.getMhsYgTidakIkut()
								.contains("," + biodataCalonMahasiswa.getId() + ",")) {

						} else {
							List<Long> d = pertemuanPunyaUjian.ambilHasilUjianMahasiswa(null, biodataCalonMahasiswa,
									true);
							int jml = d.size();
							if (jml == 0) {

								syaratAlert.add("Anda belum mengikuti ujian \"" + pertemuanPunyaUjian.getNama() + "\"");

							}
						}
					}
				}
			} else if (o instanceof AudioPertemuan) {
				final AudioPertemuan audioPertemuan = (AudioPertemuan) o;
				String n = audioPertemuan == null ? ""
						: (audioPertemuan.getLink() == null || audioPertemuan.getLink().isEmpty()
								? audioPertemuan.getNama()
								: audioPertemuan.getLink());
				if (n == null) {
					n = "";
				}
				String isi = audioPertemuan.getKeteranganTambahan();
				if (isi != null && !isi.trim().isEmpty()) {
					n = isi;
				}

				if (syarat == null)
					n += " (" + pertemuan.ambilVOPembelajaran().infoSangatSimple() + ")";
				else
					n += " (Pert.ke-" + pertemuan.getPertemuanKe() + " - "
							+ (pertemuan.getTanggal() == null ? "" : Common.dateFormat11.get().format(pertemuan.getTanggal()))
							+ ")";

				if (syarat != null && !syarat.isNull(keyData)) {

					if (mahasiswa != null) {

						boolean akses = Tugas.apakahAkses(pertemuan, "audio_" + audioPertemuan.getId(),
								mahasiswa.getId());

						if (!akses) {

							syaratAlert.add("Anda belum akses audio \"" + n + "\"");

						}
					} else if (biodataCalonMahasiswa != null) {
						boolean akses = Tugas.apakahAkses(pertemuan, "audio_" + audioPertemuan.getId(),
								biodataCalonMahasiswa.getId());

						if (!akses) {

							syaratAlert.add("Anda belum akses audio \"" + n + "\"");

						}
					}
				}
			} else if (o instanceof VideoPertemuan) {
				final VideoPertemuan videoPertemuan = (VideoPertemuan) o;
				String n = videoPertemuan == null ? ""
						: (videoPertemuan.getLink() == null || videoPertemuan.getLink().isEmpty()
								? videoPertemuan.getNama()
								: videoPertemuan.getLink());
				if (n == null) {
					n = "";
				}
				String isi = videoPertemuan.getKeteranganTambahan();
				if (isi != null && !isi.trim().isEmpty()) {
					n = isi;
				}

				if (syarat == null)
					n += " (" + pertemuan.ambilVOPembelajaran().infoSangatSimple() + ")";
				else
					n += " (Pert.ke-" + pertemuan.getPertemuanKe() + " - "
							+ (pertemuan.getTanggal() == null ? "" : Common.dateFormat11.get().format(pertemuan.getTanggal()))
							+ ")";

				if (syarat != null && !syarat.isNull(keyData)) {

					if (mahasiswa != null) {

						boolean akses = Tugas.apakahAkses(pertemuan, "video_" + videoPertemuan.getId(),
								mahasiswa.getId());

						if (!akses) {

							syaratAlert.add("Anda belum akses video \"" + n + "\"");

						}
					} else if (biodataCalonMahasiswa != null) {
						boolean akses = Tugas.apakahAkses(pertemuan, "video_" + videoPertemuan.getId(),
								biodataCalonMahasiswa.getId());

						if (!akses) {

							syaratAlert.add("Anda belum akses video \"" + n + "\"");

						}
					}
				}
			}
		}

		if (!syaratAlert.isEmpty()) {
			String s = "";
			for (String dd : syaratAlert) {
				s += s.isEmpty() ? dd : "\n\n" + dd;
			}
			try {
				MyMessageboxConfig.show(s, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/profile/ProfileUtil.java:1205");
			}

		}

		return !syaratAlert.isEmpty();
	}

	/**
	 * <b>Tujuan:</b> Merender semua konten e-Learning (materi, tugas, ujian, audio, video) dari serangkaian
	 * pertemuan ke dalam tiga komponen {@link Rows} terpisah (materi, tugas, ujian) dengan dukungan:
	 * filter teks pencarian, checkbox untuk konfigurasi syarat akses, indikator status (sudah/belum diakses),
	 * dan pagination bertahap (+3 per klik). Ini adalah method rendering terlengkap di kelas ini.
	 *
	 * <p><b>Parameter penting:</b>
	 * <ul>
	 *   <li>{@code bisaDipilih}: {@code true} = mode editor syarat (checkbox aktif, bisa ditandai);
	 *       {@code false} = mode tampil peserta (checkbox read-only atau hidden bila tidak dipersyaratkan).</li>
	 *   <li>{@code tampilCheckbox}: bila {@code false}, tidak ada checkbox sama sekali (mode tampil bersih).</li>
	 *   <li>{@code syarat}: JSON berisi kunci item yang dipersyaratkan. Bila {@code null}, tidak ada filter
	 *       syarat. Bila kosong ({@code length()==0}) dan {@code !bisaDipilih}, seluruh konten disembunyikan.</li>
	 *   <li>{@code syaratAlert}: Set yang diisi oleh method ini dengan pesan syarat belum terpenuhi (untuk
	 *       ditampilkan kemudian oleh caller). Bisa {@code null}.</li>
	 *   <li>{@code janganTampilkanYgini}: item konten yang tidak boleh ditampilkan (misalnya item target
	 *       dari syarat yang sedang dikonfigurasi — jangan jadikan syarat untuk dirinya sendiri).</li>
	 *   <li>{@code buttonRefreshSyarat}: tombol yang disembunyikan bila tidak ada konten yang ditampilkan.</li>
	 * </ul>
	 *
	 * <p><b>Cara kerja (overview):</b>
	 * <ol>
	 *   <li>Bila {@code !bisaDipilih && syarat.length()==0}: sembunyikan semua Rows dan tombol, return.</li>
	 *   <li>Muat semua item via {@code PertemuanFileContent.ambilMateri}.</li>
	 *   <li>Untuk setiap item, periksa atribut state pada Rows ({@code "selesai"}, {@code "index"},
	 *       {@code "ditampilkan"}). Bila sudah mencapai batas tampil, tambahkan tombol
	 *       "Tampilkan selanjutnya" (+3 limit, render ulang), dan tandai {@code "selesai"=true}.</li>
	 *   <li>Bila belum selesai dan lolos filter teks ({@code cariMateri/cariTugas/cariUjian}), render item:
	 *       <ul>
	 *         <li>Checkbox (bila {@code tampilCheckbox && bisaDipilih}): tanda centang sesuai keadaan di JSON
	 *             syarat; listener kirim event ke {@code chekboxEventListener} dengan entitas sebagai data.</li>
	 *         <li>Tombol akses konten: buka file/pertemuan sesuai tipe.</li>
	 *         <li>Indikator akses (ikon ✓/⚠ + label "Telah/Belum"): ditampilkan bila {@code !bisaDipilih &&
	 *             syarat != null && item di JSON} — memperlihatkan status penyelesaian peserta.</li>
	 *         <li>Label statistik (jumlah upload/peserta ujian) untuk tugas dan ujian.</li>
	 *         <li>Widget "dilihat" ({@link TampilanELearningAction#dilihat}) menampilkan jumlah akses.</li>
	 *       </ul></li>
	 *   <li>Setelah semua item diproses, atur visibilitas Rows sesuai ada/tidaknya item ({@code adaMateri},
	 *       {@code adaTugas}, {@code adaUjian}) dan sesuaikan visibilitas {@code buttonRefreshSyarat}.</li>
	 * </ol>
	 *
	 * <p><b>Catatan pagination:</b> Atribut {@code "ditampilkan"}, {@code "index"}, {@code "selesai"} pada
	 * komponen Rows harus sudah diinisialisasi oleh caller sebelum memanggil method ini (biasanya di
	 * {@code display()} kelas profil). Tombol "Tampilkan selanjutnya" me-reset atribut dan memanggil method
	 * ini kembali secara rekursif.
	 *
	 * <p><b>Penanganan tugas kelompok:</b> Saat mengecek status pengumpulan tugas kelompok mahasiswa, method
	 * ini menggunakan {@code HibernateUtil.currentSession()} untuk query
	 * {@code NamaTugasKelompokPunyaMahasiswa} dan memeriksa {@code FileFotoLain} lampiran kelompok.
	 *
	 * @param rowTugas               Rows ZK untuk daftar tugas
	 * @param rowUjian               Rows ZK untuk daftar ujian
	 * @param rowMateri              Rows ZK untuk daftar materi/audio/video
	 * @param cariTugas              Textbox filter pencarian tugas (dicocokkan case-insensitive)
	 * @param cariUjian              Textbox filter pencarian ujian
	 * @param cariMateri             Textbox filter pencarian materi
	 * @param pertemuans             peta terurut ID pertemuan (dari VOPembelajaran atau subset tertentu)
	 * @param bisaDipilih            {@code true} untuk mode editor syarat (checkbox aktif)
	 * @param tampilCheckbox         {@code true} untuk menampilkan checkbox di setiap item
	 * @param chekboxEventListener   listener yang dipanggil saat checkbox diklik (parameter: entitas item)
	 * @param syarat                 JSON konfigurasi syarat yang sudah tersimpan; boleh {@code null}
	 * @param syaratAlert            Set yang diisi dengan pesan syarat belum terpenuhi; boleh {@code null}
	 * @param janganTampilkanYgini   item yang dikecualikan dari daftar (jangan tampilkan); boleh {@code null}
	 * @param buttonRefreshSyarat    tombol yang disembunyikan bila tidak ada konten; boleh {@code null}
	 */
	public static void tampilkanMateri(final Rows rowTugas, final Rows rowUjian, final Rows rowMateri,
			final Textbox cariTugas, final Textbox cariUjian, final Textbox cariMateri,
			final TreeMap<String, Long> pertemuans, final boolean bisaDipilih, final boolean tampilCheckbox,
			final EventListener chekboxEventListener, final JSONObject syarat, final Set<String> syaratAlert,
			final GeneralValueObject janganTampilkanYgini, final MyToolbarbutton buttonRefreshSyarat) {
		if (syaratAlert != null) {
			syaratAlert.clear();
		}

		if (!bisaDipilih) {
			if (syarat != null && syarat.length() == 0) {

				rowMateri.setVisible(false);
				rowTugas.setVisible(false);
				rowUjian.setVisible(false);

				if (buttonRefreshSyarat != null) {
					buttonRefreshSyarat.setVisible(false);

					if (buttonRefreshSyarat.getParent() != null) {
						buttonRefreshSyarat.getParent().setVisible(false);
					}
				}

				return;
			}
		}

		Tbmuser tbmuser = Common.getCurrentUser();
		boolean adaMateri = false;
		boolean adaTugas = false;
		boolean adaUjian = false;

		TreeMap<String, Object[]> dataMateri = PertemuanFileContent.ambilMateri(pertemuans, false, null, tbmuser);
		for (String key : dataMateri.keySet()) {

			Boolean tugasSelesai = (Boolean) rowTugas.getAttribute("selesai");
			Boolean ujianSelesai = (Boolean) rowUjian.getAttribute("selesai");
			Boolean materiSelesai = (Boolean) rowMateri.getAttribute("selesai");

			Object[] data = dataMateri.get(key);
			final Pertemuan pertemuan = (Pertemuan) data[1];
			if (!materiSelesai && data[0] instanceof PertemuanFileContent) {

				final PertemuanFileContent pertemuanFileContent = (PertemuanFileContent) data[0];

				if (janganTampilkanYgini != null && janganTampilkanYgini instanceof PertemuanFileContent
						&& janganTampilkanYgini.getId() != null
						&& janganTampilkanYgini.getId().equals(pertemuanFileContent.getId())) {
					continue;
				}

				Integer ditampilkan = (Integer) rowMateri.getAttribute("ditampilkan");
				Integer index = (Integer) rowMateri.getAttribute("index");

				if (ditampilkan <= index) {
					rowMateri.setAttribute("selesai", true);
					MyRowStyled row = new MyRowStyled();
					rowMateri.appendChild(row);

					MyToolbarbuttonConfig a = new MyToolbarbuttonConfig("Tampilkan materi selanjutnya..",
							"/img/Button-Next-icon.png");
					a.setStyle("font-size:11px;");
					row.appendChild(a);
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							rowTugas.setAttribute("selesai", false);
							rowUjian.setAttribute("selesai", false);
							rowMateri.setAttribute("selesai", false);
							rowTugas.setAttribute("index", 0);
							rowUjian.setAttribute("index", 0);
							rowMateri.setAttribute("index", 0);
							arg0.getTarget().getParent().setVisible(false);

							Common.clear(rowUjian);
							Common.clear(rowTugas);
							Common.clear(rowMateri);

							try {
								Integer ditampilkan = (Integer) rowMateri.getAttribute("ditampilkan");
								ditampilkan += 3;
								rowMateri.setAttribute("ditampilkan", ditampilkan);
								tampilkanMateri(rowTugas, rowUjian, rowMateri, cariTugas, cariUjian, cariMateri,
										pertemuans, bisaDipilih, tampilCheckbox, chekboxEventListener, syarat,
										syaratAlert, janganTampilkanYgini, buttonRefreshSyarat);
							} catch (Exception e) {
								MyMessageboxConfig.show(
				"Mohon maaf, tidak terdapat data selanjutnya yang dapat ditampilkan. Seluruh data yang tersedia telah selesai dimuat.",
				"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							}
						}
					});

				}

				else {

					String n = pertemuanFileContent.getNama() != null
							&& pertemuanFileContent.getNama().trim().equalsIgnoreCase("link")
									? pertemuanFileContent.getLink()
									: pertemuanFileContent.getNama();

					if (ProfileUiHelper.notBlank(pertemuanFileContent.getGoogleBook())) {
						n = pertemuanFileContent.getNama();
					}
					if (n == null) {
						n = "";
					}

					String isi = pertemuanFileContent.getKeterangan();
					if (isi != null && !isi.trim().isEmpty()) {
						n = isi;
					}

					if (syarat == null)
						n += " (" + pertemuan.ambilVOPembelajaran().infoSangatSimple() + ")";
					else
						n += " (Pert.ke-" + pertemuan.getPertemuanKe() + " - " + (pertemuan.getTanggal() == null ? ""
								: Common.dateFormat11.get().format(pertemuan.getTanggal())) + ")";

					if (cariMateri.getValue().trim().isEmpty()
							|| n.toLowerCase().contains(cariMateri.getValue().trim().toLowerCase())) {

						index++;
						rowMateri.setAttribute("index", index);

						Toolbarbutton downloadButton = new MyToolbarbuttonConfig(
								n.length() > 150 ? n.substring(0, 150) + "..." : n,
								ProfileUiHelper.notBlank(pertemuanFileContent.getGoogleBook()) ? "/img/Apps-Google-Play-Books-icon.png"
										: pertemuanFileContent.getLokasiFisik() != null ? "/img/svg/desktop-light.svg"
												: FileFoto.icon(pertemuanFileContent.getNama()));
						downloadButton.setTooltiptext("Download \"" + pertemuanFileContent.getNama() + "\"");
						downloadButton.setAttribute("janganDisabled", true);
						downloadButton.setStyle("font-size:9px;");

						MyRowStyled row = new MyRowStyled();
						row.setParent(rowMateri);
						Hbox hbox = new Hbox();
						row.appendChild(hbox);

						String keyData = pertemuanFileContent.getId() + "_"
								+ pertemuanFileContent.getClass().getSimpleName();

						if (tampilCheckbox) {
							if (bisaDipilih) {
								Checkbox checkbox = new Checkbox();
								checkbox.setChecked(syarat != null && !syarat.isNull(keyData));
								hbox.appendChild(checkbox);
								checkbox.addEventListener("onClick", new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										chekboxEventListener
												.onEvent(new Event("", arg0.getTarget(), pertemuanFileContent));
									}
								});
							} else if (syarat != null && syarat.isNull(keyData)) {
								row.setVisible(false);
							}
						}

						hbox.appendChild(downloadButton);

						TampilanELearningAction.dilihat(pertemuan, "bahan_perkulaiahan_" + pertemuanFileContent.getId(),
								"Akses", false).setParent(hbox);

						adaMateri |= row.isVisible();

						if (!bisaDipilih && syarat != null && !syarat.isNull(keyData)) {

							if (tbmuser != null && tbmuser.getMahasiswa() != null) {

								boolean akses = Tugas.apakahAkses(pertemuan,
										"bahan_perkulaiahan_" + pertemuanFileContent.getId(),
										tbmuser.getMahasiswa().getId());

								if (!akses) {
									hbox.appendChild(new Space());
									hbox.appendChild(new Image("/img/svg/warning-outline.svg"));
									hbox.appendChild(new MyLabelKecilSekali("Belum akses materi"));
									if (syaratAlert != null)
										syaratAlert.add("Anda belum akses materi \"" + n + "\"");

								} else {
									hbox.appendChild(new Space());
									hbox.appendChild(new Image("/img/svg/check2-circle.svg"));
									hbox.appendChild(new MyLabelKecilSekali("Telah akses materi"));
								}
							} else if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null) {
								boolean akses = Tugas.apakahAkses(pertemuan,
										"bahan_perkulaiahan_" + pertemuanFileContent.getId(),
										tbmuser.getBiodataCalonMahasiswa().getId());

								if (!akses) {
									hbox.appendChild(new Space());
									hbox.appendChild(new Image("/img/svg/warning-outline.svg"));
									hbox.appendChild(new MyLabelKecilSekali("Belum akses materi"));
									if (syaratAlert != null)
										syaratAlert.add("Anda belum akses materi \"" + n + "\"");

								} else {
									hbox.appendChild(new Space());
									hbox.appendChild(new Image("/img/svg/check2-circle.svg"));
									hbox.appendChild(new MyLabelKecilSekali("Telah akses materi"));
								}
							}
						}

						downloadButton.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								if (ProfileUtil.chekSyarat(pertemuan.ambilVOPembelajaran(),
										pertemuanFileContent.getSyaratAkses())) {
									return;
								}

								pertemuan.masukkanData("bahan_perkulaiahan_" + pertemuanFileContent.getId());

								if (ProfileUiHelper.notBlank(pertemuanFileContent.getGoogleBook())) {
									if (Common.isMobile()) {
										ExecutionsCtrl.getCurrent().sendRedirect(pertemuanFileContent.getLink(),
												"_blank");
									} else {
										Clients.evalJavaScript("popupCenter({url: '" + ProfileUiHelper.js(pertemuanFileContent.getLink())
												+ "', title: 'Book', w: 1200, h: 600});");
									}
								} else if (pertemuanFileContent.getGdrive() != null) {
									pertemuanFileContent.tampilGDrive(null);
								} else {

									String link = pertemuanFileContent == null ? null
											: (pertemuanFileContent.getLink() == null
													|| pertemuanFileContent.getLink().isEmpty() ? null
															: pertemuanFileContent.getLink());

									if (pertemuanFileContent != null
											&& (link == null || link.trim().isEmpty() || !link.startsWith("http"))) {
										link = pertemuanFileContent.createLinkUri();
										if (link != null) {
										}
									}

									if (pertemuanFileContent != null && link != null && !link.trim().isEmpty()) {

										if (pertemuanFileContent.bisaPreview()) {
											Common.displayWindow(pertemuanFileContent.merupakanGambar(), link, true,
													"95%", "95%", true, pertemuanFileContent);
										} else {
											if (Common.isMobile()) {
												ExecutionsCtrl.getCurrent().sendRedirect(link, "_blank");
											} else {
												Clients.evalJavaScript("popupCenter({url: '" + ProfileUiHelper.js(link)
														+ "', title: 'data', w: 1200, h: 600});");
											}
										}
									} else {
										MyMessageboxConfig.show(
				"Mohon maaf, Bapak/Ibu, berkas yang Anda akses tidak dapat ditemukan pada sistem. Langkah yang dapat dilakukan: (1) pastikan berkas masih tersedia dan belum dihapus; (2) muat ulang halaman lalu coba akses kembali; (3) apabila kendala masih berlanjut, hubungi Admin atau bagian terkait untuk bantuan lebih lanjut.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
									}
								}
							}
						});
					}
				}
			}

			else if (!tugasSelesai && data[0] instanceof Tugas) {

				final Tugas tugas = (Tugas) data[0];

				if (janganTampilkanYgini != null && janganTampilkanYgini instanceof Tugas
						&& janganTampilkanYgini.getId() != null
						&& janganTampilkanYgini.getId().equals(tugas.getId())) {
					continue;
				}

				if (tugas.getJudultugas() != null && !tugas.getJudultugas().trim().isEmpty()) {
					String n = tugas.getJudultugas();
					if (n == null) {
						n = "";
					}

					if (syarat == null)
						n += " (" + pertemuan.ambilVOPembelajaran().infoSangatSimple() + ")";
					else {

						if (tugas.getMulai() != null && tugas.getSelesai() != null) {
							n += " (Pert.ke-" + pertemuan.getPertemuanKe() + " - "
									+ (Common.dateFormat33.get().format(tugas.getMulai())) + " s.d "
									+ (Common.dateFormat33.get().format(tugas.getSelesai())) + ")";
						} else if (tugas.getMulai() != null) {
							n += " (Pert.ke-" + pertemuan.getPertemuanKe() + " - "
									+ (Common.dateFormat33.get().format(tugas.getMulai())) + ")";
						} else if (tugas.getSelesai() != null) {
							n += " (Pert.ke-" + pertemuan.getPertemuanKe() + " - s.d "
									+ (Common.dateFormat33.get().format(tugas.getSelesai())) + ")";
						} else {
							n += " (Pert.ke-" + pertemuan.getPertemuanKe() + " - "
									+ (pertemuan.getTanggal() == null ? ""
											: Common.dateFormat11.get().format(pertemuan.getTanggal()))
									+ ")";
						}
					}

					Integer ditampilkan = (Integer) rowTugas.getAttribute("ditampilkan");
					Integer index = (Integer) rowTugas.getAttribute("index");

					if (ditampilkan <= index) {
						rowTugas.setAttribute("selesai", true);
						MyRowStyled row = new MyRowStyled();
						rowTugas.appendChild(row);

						MyToolbarbuttonConfig a = new MyToolbarbuttonConfig("Tampilkan tugas selanjutnya..",
								"/img/Button-Next-icon.png");
						a.setStyle("font-size:11px;");
						row.appendChild(a);
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								arg0.getTarget().getParent().setVisible(false);

								rowTugas.setAttribute("selesai", false);
								rowUjian.setAttribute("selesai", false);
								rowMateri.setAttribute("selesai", false);
								rowTugas.setAttribute("index", 0);
								rowUjian.setAttribute("index", 0);
								rowMateri.setAttribute("index", 0);

								Common.clear(rowUjian);
								Common.clear(rowTugas);
								Common.clear(rowMateri);

								try {
									Integer ditampilkan = (Integer) rowTugas.getAttribute("ditampilkan");
									ditampilkan += 3;
									rowTugas.setAttribute("ditampilkan", ditampilkan);
									tampilkanMateri(rowTugas, rowUjian, rowMateri, cariTugas, cariUjian, cariMateri,
											pertemuans, bisaDipilih, tampilCheckbox, chekboxEventListener, syarat,
											syaratAlert, janganTampilkanYgini, buttonRefreshSyarat);
								} catch (Exception e) {
									MyMessageboxConfig.show(
				"Mohon maaf, tidak terdapat data selanjutnya yang dapat ditampilkan. Seluruh data yang tersedia telah selesai dimuat.",
				"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
								}
							}
						});

					}

					else {

						if (cariTugas.getValue().trim().isEmpty()
								|| n.toLowerCase().contains(cariTugas.getValue().trim().toLowerCase())) {

							index++;
							rowTugas.setAttribute("index", index);

							Toolbarbutton downloadButton = new MyToolbarbuttonConfig(
									n.length() > 150 ? n.substring(0, 150) + "..." : n,
									"/img/Status-mail-task-icon.png");

							downloadButton.setAttribute("janganDisabled", true);

							downloadButton.setStyle("font-size:9px;");

							MyRowStyled row = new MyRowStyled();
							row.setParent(rowTugas);
							Hbox hbox = new Hbox();
							row.appendChild(hbox);
							String keyData = tugas.getId() + "_" + tugas.getClass().getSimpleName();
							if (tampilCheckbox) {
								if (bisaDipilih) {

									Checkbox checkbox = new Checkbox();
									checkbox.setChecked(syarat != null && !syarat.isNull(keyData));

									hbox.appendChild(checkbox);
									checkbox.addEventListener("onClick", new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											chekboxEventListener.onEvent(new Event("", arg0.getTarget(), tugas));
										}
									});
								} else if (syarat != null && syarat.isNull(keyData)) {
									row.setVisible(false);
								}
							}

							adaTugas |= row.isVisible();

							hbox.appendChild(downloadButton);

							Hbox myHbox = new Hbox();
							myHbox.setParent(hbox);

							if (tugas instanceof TugasKelompok) {

							} else if (tugas.getJudultugas() != null && !tugas.getJudultugas().trim().equals("")) {
								Number tg = tugas.ambilJumlahTugasFileContent();
								MyLabelKecil labelKecil = new MyLabelKecil(
										"Upload : " + Common.numberFormat.get().format(tg.intValue()) + " peserta");
								labelKecil.setStyle("font-size:8px;color:blue;");
								myHbox.appendChild(labelKecil);
							}
							TampilanELearningAction.dilihat(tugas, "tugas", "Akses", false).setParent(myHbox);

							if (!bisaDipilih && syarat != null && !syarat.isNull(keyData)) {

								if (tbmuser != null && tbmuser.getMahasiswa() != null) {

									if (tugas.getMhsYgTidakIkut()
											.contains("," + tbmuser.getMahasiswa().getId() + ",")) {
										hbox.appendChild(new Space());
										hbox.appendChild(new Image("/img/Info-icon.png"));
										hbox.appendChild(new MyLabelKecilSekali("Tidak perlu mengumpulkan tugas ini"));
									} else {

										if (tugas instanceof TugasKelompok) {

											Session session = HibernateUtil.currentSession();
											Long namaTugasKelompokId = (Long) session
													.createCriteria(NamaTugasKelompokPunyaMahasiswa.class)
													.setProjection(Projections.property("namaTugasKelompok.id"))
													.createAlias("namaTugasKelompok", "namaTugasKelompok")
													.add(Restrictions.eq("namaTugasKelompok.tugasKelompok", tugas))
													.add(Restrictions.eq("mahasiswa", tbmuser.getMahasiswa()))
													.setMaxResults(1).uniqueResult();

											if (namaTugasKelompokId == null) {
												hbox.appendChild(new Space());
												hbox.appendChild(new Image("/img/svg/warning-outline.svg"));
												hbox.appendChild(new MyLabelKecilSekali("Belum mengumpulkan"));
												if (syaratAlert != null)
													syaratAlert.add("Kelompok Anda belum mengumpulkan tugas \""
															+ tugas.getJudultugas() + "\"");
											} else {
												FileFotoLain fileFotoLain = FileFotoLain.ambil(false,
														namaTugasKelompokId, NamaTugasKelompok.class.getName(),
														LampiranLain.class);
												if (fileFotoLain == null) {
													hbox.appendChild(new Space());
													hbox.appendChild(new Image("/img/svg/warning-outline.svg"));
													hbox.appendChild(new MyLabelKecilSekali("Belum mengumpulkan"));
													if (syaratAlert != null)
														syaratAlert.add("Kelompok Anda belum mengumpulkan tugas \""
																+ tugas.getJudultugas() + "\"");
												} else {
													hbox.appendChild(new Space());
													hbox.appendChild(new Image("/img/svg/check2-circle.svg"));
													hbox.appendChild(new MyLabelKecilSekali(
															"Telah mengumpulkan tugas kelompok"));
												}
											}

										} else {
											int jml = tugas.ambilJumlahTugasFileContent(tbmuser.getMahasiswa());
											if (jml == 0) {
												hbox.appendChild(new Space());
												hbox.appendChild(new Image("/img/svg/warning-outline.svg"));
												hbox.appendChild(new MyLabelKecilSekali("Belum mengumpulkan"));
												if (syaratAlert != null)
													syaratAlert.add("Anda belum mengumpulkan tugas \""
															+ tugas.getJudultugas() + "\"");

											} else {
												hbox.appendChild(new Space());
												hbox.appendChild(new Image("/img/svg/check2-circle.svg"));
												hbox.appendChild(new MyLabelKecilSekali("Telah mengumpulkan tugas"));
											}
										}
									}
								} else if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null) {
									if (tugas.getMhsYgTidakIkut()
											.contains("," + tbmuser.getBiodataCalonMahasiswa().getId() + ",")) {
										hbox.appendChild(new Space());
										hbox.appendChild(new Image("/img/Info-icon.png"));
										hbox.appendChild(new MyLabelKecilSekali("Tidak perlu mengumpulkan tugas ini"));
									} else {
										int jml = tugas.ambilJumlahTugasFileContent(tbmuser.getBiodataCalonMahasiswa());
										if (jml == 0) {
											hbox.appendChild(new Space());
											hbox.appendChild(new Image("/img/svg/warning-outline.svg"));
											hbox.appendChild(new MyLabelKecilSekali("Belum mengumpulkan"));
											if (syaratAlert != null)
												syaratAlert.add("Anda belum mengumpulkan tugas \""
														+ tugas.getJudultugas() + "\"");

										} else {
											hbox.appendChild(new Space());
											hbox.appendChild(new Image("/img/svg/check2-circle.svg"));
											hbox.appendChild(new MyLabelKecilSekali("Telah mengumpulkan tugas"));
										}
									}
								}
							}

							downloadButton.addEventListener("onClick", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									Tbmuser tbmuser = Common.getCurrentUser();
									if (tugas instanceof Pertemuan) {

										new PertemuanHelper(tbmuser.getMahasiswa(), tbmuser.getBiodataCalonMahasiswa())
												.display(pertemuan, new DataLoader() {

													@Override
													public void loadData(Object value) {

													}
												}, 3);
									} else if (tugas instanceof TugasKelompok) {
										TugasKelompok tugasKelompok = (TugasKelompok) tugas;
										new PertemuanHelper(tbmuser.getMahasiswa(), tbmuser.getBiodataCalonMahasiswa())
												.display(pertemuan, new DataLoader() {

													@Override
													public void loadData(Object value) {

													}
												}, 3, null, tugasKelompok, null, null, null);
									} else {
										TugasPertemuan tugasPertemuan = (TugasPertemuan) tugas;
										new PertemuanHelper(tbmuser.getMahasiswa(), tbmuser.getBiodataCalonMahasiswa())
												.display(pertemuan, new DataLoader() {

													@Override
													public void loadData(Object value) {

													}
												}, 3, tugasPertemuan, null, null, null, null);
									}

								}
							});
						}
					}
				}

			} else if (!ujianSelesai && data[0] instanceof PertemuanPunyaUjian) {
				final PertemuanPunyaUjian pertemuanPunyaUjian = (PertemuanPunyaUjian) data[0];

				if (janganTampilkanYgini != null && janganTampilkanYgini instanceof Ujian
						&& janganTampilkanYgini.getId() != null
						&& pertemuanPunyaUjian.getUjian() != null
						&& janganTampilkanYgini.getId().equals(pertemuanPunyaUjian.getUjian().getId())) {
					continue;
				}

				String n = pertemuanPunyaUjian.getNama();
				if (n == null) {
					n = "";
				}

				if (syarat == null)
					n += " (" + pertemuan.ambilVOPembelajaran().infoSangatSimple() + ")";
				else {

					if (pertemuanPunyaUjian.getMulaiUjian() != null && pertemuanPunyaUjian.getSampaiUjian() != null) {
						n += " (Pert.ke-" + pertemuan.getPertemuanKe() + " - "
								+ (Common.dateFormat33.get().format(pertemuanPunyaUjian.getMulaiUjian())) + " s.d "
								+ (Common.dateFormat33.get().format(pertemuanPunyaUjian.getSampaiUjian())) + ")";
					} else if (pertemuanPunyaUjian.getMulaiUjian() != null) {
						n += " (Pert.ke-" + pertemuan.getPertemuanKe() + " - "
								+ (Common.dateFormat33.get().format(pertemuanPunyaUjian.getMulaiUjian())) + ")";
					} else if (pertemuanPunyaUjian.getSampaiUjian() != null) {
						n += " (Pert.ke-" + pertemuan.getPertemuanKe() + " - s.d "
								+ (Common.dateFormat33.get().format(pertemuanPunyaUjian.getSampaiUjian())) + ")";
					} else {
						n += " (Pert.ke-" + pertemuan.getPertemuanKe() + " - " + (pertemuan.getTanggal() == null ? ""
								: Common.dateFormat11.get().format(pertemuan.getTanggal())) + ")";
					}
				}

				Integer ditampilkan = (Integer) rowUjian.getAttribute("ditampilkan");
				Integer index = (Integer) rowUjian.getAttribute("index");

				if (ditampilkan <= index) {
					rowUjian.setAttribute("selesai", true);
					MyRowStyled row = new MyRowStyled();
					rowUjian.appendChild(row);

					MyToolbarbuttonConfig a = new MyToolbarbuttonConfig("Tampilkan ujian selanjutnya..",
							"/img/Button-Next-icon.png");
					a.setStyle("font-size:11px;");
					row.appendChild(a);
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							rowTugas.setAttribute("selesai", false);
							rowUjian.setAttribute("selesai", false);
							rowMateri.setAttribute("selesai", false);
							rowTugas.setAttribute("index", 0);
							rowUjian.setAttribute("index", 0);
							rowMateri.setAttribute("index", 0);

							arg0.getTarget().getParent().setVisible(false);

							Common.clear(rowUjian);
							Common.clear(rowTugas);
							Common.clear(rowMateri);

							try {
								Integer ditampilkan = (Integer) rowUjian.getAttribute("ditampilkan");
								ditampilkan += 3;
								rowUjian.setAttribute("ditampilkan", ditampilkan);
								tampilkanMateri(rowTugas, rowUjian, rowMateri, cariTugas, cariUjian, cariMateri,
										pertemuans, bisaDipilih, tampilCheckbox, chekboxEventListener, syarat,
										syaratAlert, janganTampilkanYgini, buttonRefreshSyarat);
							} catch (Exception e) {
								MyMessageboxConfig.show(
				"Mohon maaf, tidak terdapat data selanjutnya yang dapat ditampilkan. Seluruh data yang tersedia telah selesai dimuat.",
				"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							}
						}
					});

				}

				else {

					if (cariUjian.getValue().trim().isEmpty()
							|| n.toLowerCase().contains(cariUjian.getValue().trim().toLowerCase())) {

						index++;
						rowUjian.setAttribute("index", index);

						Toolbarbutton downloadButton = new MyToolbarbuttonConfig(
								n.length() > 150 ? n.substring(0, 150) + "..." : n, "/img/Text-Edit-icon.png");

						downloadButton.setAttribute("janganDisabled", true);
						downloadButton.setStyle("font-size:9px;");

						MyRowStyled row = new MyRowStyled();
						row.setParent(rowUjian);
						Hbox hbox = new Hbox();
						row.appendChild(hbox);

						String keyData = pertemuanPunyaUjian.getId() + "_"
								+ pertemuanPunyaUjian.getClass().getSimpleName();
						if (tampilCheckbox) {
							if (bisaDipilih) {

								Checkbox checkbox = new Checkbox();
								checkbox.setChecked(syarat != null && !syarat.isNull(keyData));

								hbox.appendChild(checkbox);
								checkbox.addEventListener("onClick", new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										chekboxEventListener
												.onEvent(new Event("", arg0.getTarget(), pertemuanPunyaUjian));
									}
								});
							} else if (syarat != null && syarat.isNull(keyData)) {
								row.setVisible(false);
							}
						}

						adaUjian |= row.isVisible();
						hbox.appendChild(downloadButton);

						Hbox myHbox = new Hbox();
						myHbox.setParent(hbox);
						Number tg = pertemuanPunyaUjian.ambilJumlahHasilUjianMahasiswaTelahIkut(false);
						MyLabelKecil labelKecil = new MyLabelKecil(
								"Ikut Ujian : " + Common.numberFormat.get().format(tg.intValue()) + " peserta");
						labelKecil.setStyle("font-size:8px;color:blue;");

						myHbox.appendChild(labelKecil);
						TampilanELearningAction
								.dilihat(pertemuan, "ujian_" + pertemuanPunyaUjian.getId(), "Akses", false)
								.setParent(myHbox);

						if (!bisaDipilih && syarat != null && !syarat.isNull(keyData)) {

							if (tbmuser != null && tbmuser.getMahasiswa() != null) {

								if (pertemuanPunyaUjian.getMhsYgTidakIkut()
										.contains("," + tbmuser.getMahasiswa().getId() + ",")) {
									hbox.appendChild(new Space());
									hbox.appendChild(new Image("/img/Info-icon.png"));
									hbox.appendChild(new MyLabelKecilSekali("Tidak perlu mengikuti ujian ini"));
								} else {
									List<Long> d = pertemuanPunyaUjian.ambilHasilUjianMahasiswa(tbmuser.getMahasiswa(),
											null, true);
									int jml = d.size();

									if (jml == 0) {
										hbox.appendChild(new Space());
										hbox.appendChild(new Image("/img/svg/warning-outline.svg"));
										hbox.appendChild(new MyLabelKecilSekali("Belum mengikuti ujian"));
										if (syaratAlert != null)
											syaratAlert.add("Anda belum mengikuti ujian \""
													+ pertemuanPunyaUjian.getNama() + "\"");

									} else {
										hbox.appendChild(new Space());
										hbox.appendChild(new Image("/img/svg/check2-circle.svg"));
										hbox.appendChild(new MyLabelKecilSekali("Telah mengikuti ujian"));
									}
								}
							} else if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null) {
								if (pertemuanPunyaUjian.getMhsYgTidakIkut()
										.contains("," + tbmuser.getBiodataCalonMahasiswa().getId() + ",")) {
									hbox.appendChild(new Space());
									hbox.appendChild(new Image("/img/Info-icon.png"));
									hbox.appendChild(new MyLabelKecilSekali("Tidak perlu mengikuti ujian ini"));
								} else {
									List<Long> d = pertemuanPunyaUjian.ambilHasilUjianMahasiswa(null,
											tbmuser.getBiodataCalonMahasiswa(), true);
									int jml = d.size();
									if (jml == 0) {
										hbox.appendChild(new Space());
										hbox.appendChild(new Image("/img/svg/warning-outline.svg"));
										hbox.appendChild(new MyLabelKecilSekali("Belum mengikuti ujian"));
										if (syaratAlert != null)
											syaratAlert.add("Anda belum mengikuti ujian \""
													+ pertemuanPunyaUjian.getNama() + "\"");

									} else {
										hbox.appendChild(new Space());
										hbox.appendChild(new Image("/img/svg/check2-circle.svg"));
										hbox.appendChild(new MyLabelKecilSekali("Telah mengikuti ujian"));
									}
								}
							}
						}

						downloadButton.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Tbmuser tbmuser = Common.getCurrentUser();
								new PertemuanHelper(tbmuser.getMahasiswa(), tbmuser.getBiodataCalonMahasiswa())
										.display(pertemuan, new DataLoader() {

											@Override
											public void loadData(Object value) {

											}
										}, 6);

							}
						});
					}
				}
			} else if (!materiSelesai && data[0] instanceof AudioPertemuan) {
				final AudioPertemuan audioPertemuan = (AudioPertemuan) data[0];

				if (janganTampilkanYgini != null && janganTampilkanYgini instanceof AudioPertemuan
						&& janganTampilkanYgini.getId() != null
						&& janganTampilkanYgini.getId().equals(audioPertemuan.getId())) {
					continue;
				}

				Integer ditampilkan = (Integer) rowMateri.getAttribute("ditampilkan");
				Integer index = (Integer) rowMateri.getAttribute("index");

				if (ditampilkan <= index) {
					rowMateri.setAttribute("selesai", true);
					MyRowStyled row = new MyRowStyled();
					rowMateri.appendChild(row);

					MyToolbarbuttonConfig a = new MyToolbarbuttonConfig("Tampilkan materi selanjutnya..",
							"/img/Button-Next-icon.png");
					a.setStyle("font-size:11px;");
					row.appendChild(a);
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							rowTugas.setAttribute("selesai", false);
							rowUjian.setAttribute("selesai", false);
							rowMateri.setAttribute("selesai", false);
							rowTugas.setAttribute("index", 0);
							rowUjian.setAttribute("index", 0);
							rowMateri.setAttribute("index", 0);

							arg0.getTarget().getParent().setVisible(false);

							Common.clear(rowUjian);
							Common.clear(rowTugas);
							Common.clear(rowMateri);

							try {
								Integer ditampilkan = (Integer) rowMateri.getAttribute("ditampilkan");
								ditampilkan += 3;
								rowMateri.setAttribute("ditampilkan", ditampilkan);
								tampilkanMateri(rowTugas, rowUjian, rowMateri, cariTugas, cariUjian, cariMateri,
										pertemuans, bisaDipilih, tampilCheckbox, chekboxEventListener, syarat,
										syaratAlert, janganTampilkanYgini, buttonRefreshSyarat);
							} catch (Exception e) {
								MyMessageboxConfig.show(
				"Mohon maaf, tidak terdapat data selanjutnya yang dapat ditampilkan. Seluruh data yang tersedia telah selesai dimuat.",
				"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							}
						}
					});

				}

				else {

					String n = audioPertemuan == null ? ""
							: (audioPertemuan.getLink() == null || audioPertemuan.getLink().isEmpty()
									? audioPertemuan.getNama()
									: audioPertemuan.getLink());
					if (n == null) {
						n = "";
					}
					String isi = audioPertemuan.getKeteranganTambahan();
					if (isi != null && !isi.trim().isEmpty()) {
						n = isi;
					}

					if (syarat == null)
						n += " (" + pertemuan.ambilVOPembelajaran().infoSangatSimple() + ")";
					else
						n += " (Pert.ke-" + pertemuan.getPertemuanKe() + " - " + (pertemuan.getTanggal() == null ? ""
								: Common.dateFormat11.get().format(pertemuan.getTanggal())) + ")";

					if (cariMateri.getValue().trim().isEmpty()
							|| n.toLowerCase().contains(cariMateri.getValue().trim().toLowerCase())) {

						index++;
						rowMateri.setAttribute("index", index);

						Toolbarbutton downloadButton = new MyToolbarbuttonConfig(
								n.length() > 150 ? n.substring(0, 150) + "..." : n, FileFoto.icon("mp3"));
						downloadButton.setTooltiptext("Download \"" + audioPertemuan.getNama() + "\"");
						downloadButton.setAttribute("janganDisabled", true);
						downloadButton.setStyle("font-size:9px;");

						MyRowStyled row = new MyRowStyled();
						row.setParent(rowMateri);
						Hbox hbox = new Hbox();
						row.appendChild(hbox);
						String keyData = audioPertemuan.getId() + "_" + audioPertemuan.getClass().getSimpleName();
						if (tampilCheckbox) {
							if (bisaDipilih) {
								Checkbox checkbox = new Checkbox();
								checkbox.setChecked(syarat != null && !syarat.isNull(keyData));
								hbox.appendChild(checkbox);
								checkbox.addEventListener("onClick", new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										chekboxEventListener.onEvent(new Event("", arg0.getTarget(), audioPertemuan));
									}
								});
							} else if (syarat != null && syarat.isNull(keyData)) {
								row.setVisible(false);
							}
						}
						adaMateri |= row.isVisible();
						hbox.appendChild(downloadButton);

						TampilanELearningAction.dilihat(pertemuan, "audio_" + audioPertemuan.getId(), "Akses", false)
								.setParent(hbox);

						if (!bisaDipilih && syarat != null && !syarat.isNull(keyData)) {

							if (tbmuser != null && tbmuser.getMahasiswa() != null) {

								boolean akses = Tugas.apakahAkses(pertemuan, "audio_" + audioPertemuan.getId(),
										tbmuser.getMahasiswa().getId());

								if (!akses) {
									hbox.appendChild(new Space());
									hbox.appendChild(new Image("/img/svg/warning-outline.svg"));
									hbox.appendChild(new MyLabelKecilSekali("Belum akses audio"));
									if (syaratAlert != null)
										syaratAlert.add("Anda belum akses audio \"" + n + "\"");

								} else {
									hbox.appendChild(new Space());
									hbox.appendChild(new Image("/img/svg/check2-circle.svg"));
									hbox.appendChild(new MyLabelKecilSekali("Telah akses audio"));
								}
							} else if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null) {
								boolean akses = Tugas.apakahAkses(pertemuan, "audio_" + audioPertemuan.getId(),
										tbmuser.getBiodataCalonMahasiswa().getId());

								if (!akses) {
									hbox.appendChild(new Space());
									hbox.appendChild(new Image("/img/svg/warning-outline.svg"));
									hbox.appendChild(new MyLabelKecilSekali("Belum akses audio"));
									if (syaratAlert != null)
										syaratAlert.add("Anda belum akses audio \"" + n + "\"");

								} else {
									hbox.appendChild(new Space());
									hbox.appendChild(new Image("/img/svg/check2-circle.svg"));
									hbox.appendChild(new MyLabelKecilSekali("Telah akses audio"));
								}
							}
						}

						downloadButton.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								if (ProfileUtil.chekSyarat(pertemuan.ambilVOPembelajaran(),
										audioPertemuan.getSyaratAkses())) {
									return;
								}

								pertemuan.masukkanData("audio_" + audioPertemuan.getId());

								if (audioPertemuan.getGdrive() != null) {
									audioPertemuan.tampilGDrive(null);
								} else {

									String link = audioPertemuan == null ? null
											: (audioPertemuan.getLink() == null || audioPertemuan.getLink().isEmpty()
													? null
													: audioPertemuan.getLink());

									if (audioPertemuan != null
											&& (link == null || link.trim().isEmpty() || !link.startsWith("http"))) {
										link = audioPertemuan.createLinkUri();
										if (link != null) {
										}
									}

									if (audioPertemuan != null && link != null && !link.trim().isEmpty()) {

										if (Common.isMobile()) {
											ExecutionsCtrl.getCurrent().sendRedirect(link, "_blank");
										} else {
											Clients.evalJavaScript("popupCenter({url: '" + ProfileUiHelper.js(link)
													+ "', title: 'data', w: 1200, h: 600});");
										}
									} else {
										MyMessageboxConfig.show(
				"Mohon maaf, Bapak/Ibu, berkas yang Anda akses tidak dapat ditemukan pada sistem. Langkah yang dapat dilakukan: (1) pastikan berkas masih tersedia dan belum dihapus; (2) muat ulang halaman lalu coba akses kembali; (3) apabila kendala masih berlanjut, hubungi Admin atau bagian terkait untuk bantuan lebih lanjut.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
									}
								}
							}
						});
					}
				}
			} else if (!materiSelesai && data[0] instanceof VideoPertemuan) {

				final VideoPertemuan videoPertemuan = (VideoPertemuan) data[0];

				if (janganTampilkanYgini != null && janganTampilkanYgini instanceof VideoPertemuan
						&& janganTampilkanYgini.getId() != null
						&& janganTampilkanYgini.getId().equals(videoPertemuan.getId())) {
					continue;
				}

				Integer index = (Integer) rowMateri.getAttribute("index");
				Integer ditampilkan = (Integer) rowMateri.getAttribute("ditampilkan");

				if (ditampilkan <= index) {
					rowMateri.setAttribute("selesai", true);
					MyRowStyled row = new MyRowStyled();
					rowMateri.appendChild(row);

					MyToolbarbuttonConfig a = new MyToolbarbuttonConfig("Tampilkan materi selanjutnya..",
							"/img/Button-Next-icon.png");
					a.setStyle("font-size:11px;");
					row.appendChild(a);
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							rowTugas.setAttribute("selesai", false);
							rowUjian.setAttribute("selesai", false);
							rowMateri.setAttribute("selesai", false);
							rowTugas.setAttribute("index", 0);
							rowUjian.setAttribute("index", 0);
							rowMateri.setAttribute("index", 0);

							arg0.getTarget().getParent().setVisible(false);

							Common.clear(rowUjian);
							Common.clear(rowTugas);
							Common.clear(rowMateri);

							try {
								Integer ditampilkan = (Integer) rowMateri.getAttribute("ditampilkan");
								ditampilkan += 3;
								rowMateri.setAttribute("ditampilkan", ditampilkan);
								tampilkanMateri(rowTugas, rowUjian, rowMateri, cariTugas, cariUjian, cariMateri,
										pertemuans, bisaDipilih, tampilCheckbox, chekboxEventListener, syarat,
										syaratAlert, janganTampilkanYgini, buttonRefreshSyarat);
							} catch (Exception e) {
								MyMessageboxConfig.show(
				"Mohon maaf, tidak terdapat data selanjutnya yang dapat ditampilkan. Seluruh data yang tersedia telah selesai dimuat.",
				"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							}
						}
					});

				}

				else {

					String n = videoPertemuan == null ? ""
							: (videoPertemuan.getLink() == null || videoPertemuan.getLink().isEmpty()
									? videoPertemuan.getNama()
									: videoPertemuan.getLink());
					if (n == null) {
						n = "";
					}
					String isi = videoPertemuan.getKeteranganTambahan();
					if (isi != null && !isi.trim().isEmpty()) {
						n = isi;
					}

					if (syarat == null)
						n += " (" + pertemuan.ambilVOPembelajaran().infoSangatSimple() + ")";
					else
						n += " (Pert.ke-" + pertemuan.getPertemuanKe() + " - " + (pertemuan.getTanggal() == null ? ""
								: Common.dateFormat11.get().format(pertemuan.getTanggal())) + ")";

					if (cariMateri.getValue().trim().isEmpty()
							|| n.toLowerCase().contains(cariMateri.getValue().trim().toLowerCase())) {

						index++;
						rowMateri.setAttribute("index", index);

						Toolbarbutton downloadButton = new MyToolbarbuttonConfig(
								n.length() > 150 ? n.substring(0, 150) + "..." : n, FileFoto.icon("mp4"));
						downloadButton.setTooltiptext("Download \"" + videoPertemuan.getNama() + "\"");
						downloadButton.setAttribute("janganDisabled", true);
						downloadButton.setStyle("font-size:9px;");

						MyRowStyled row = new MyRowStyled();
						row.setParent(rowMateri);
						Hbox hbox = new Hbox();
						row.appendChild(hbox);
						String keyData = videoPertemuan.getId() + "_" + videoPertemuan.getClass().getSimpleName();
						if (tampilCheckbox) {
							if (bisaDipilih) {
								Checkbox checkbox = new Checkbox();
								checkbox.setChecked(syarat != null && !syarat.isNull(keyData));
								hbox.appendChild(checkbox);
								checkbox.addEventListener("onClick", new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										chekboxEventListener.onEvent(new Event("", arg0.getTarget(), videoPertemuan));
									}
								});
							} else if (syarat != null && syarat.isNull(keyData)) {
								row.setVisible(false);
							}
						}
						adaMateri |= row.isVisible();
						hbox.appendChild(downloadButton);

						TampilanELearningAction.dilihat(pertemuan, "video_" + videoPertemuan.getId(), "Akses", false)
								.setParent(hbox);

						if (!bisaDipilih && syarat != null && !syarat.isNull(keyData)) {

							if (tbmuser != null && tbmuser.getMahasiswa() != null) {

								boolean akses = Tugas.apakahAkses(pertemuan, "video_" + videoPertemuan.getId(),
										tbmuser.getMahasiswa().getId());

								if (!akses) {
									hbox.appendChild(new Space());
									hbox.appendChild(new Image("/img/svg/warning-outline.svg"));
									hbox.appendChild(new MyLabelKecilSekali("Belum akses video"));
									if (syaratAlert != null)
										syaratAlert.add("Anda belum akses video \"" + n + "\"");

								} else {
									hbox.appendChild(new Space());
									hbox.appendChild(new Image("/img/svg/check2-circle.svg"));
									hbox.appendChild(new MyLabelKecilSekali("Telah akses video"));
								}
							} else if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null) {
								boolean akses = Tugas.apakahAkses(pertemuan, "video_" + videoPertemuan.getId(),
										tbmuser.getBiodataCalonMahasiswa().getId());

								if (!akses) {
									hbox.appendChild(new Space());
									hbox.appendChild(new Image("/img/svg/warning-outline.svg"));
									hbox.appendChild(new MyLabelKecilSekali("Belum akses video"));
									if (syaratAlert != null)
										syaratAlert.add("Anda belum akses video \"" + n + "\"");

								} else {
									hbox.appendChild(new Space());
									hbox.appendChild(new Image("/img/svg/check2-circle.svg"));
									hbox.appendChild(new MyLabelKecilSekali("Telah akses video"));
								}
							}
						}

						downloadButton.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								if (ProfileUtil.chekSyarat(pertemuan.ambilVOPembelajaran(),
										videoPertemuan.getSyaratAkses())) {
									return;
								}

								pertemuan.masukkanData("video_" + videoPertemuan.getId());

								if (videoPertemuan.getGdrive() != null) {
									videoPertemuan.tampilGDrive(null);
								} else {

									String link = videoPertemuan == null ? null
											: (videoPertemuan.getLink() == null || videoPertemuan.getLink().isEmpty()
													? null
													: videoPertemuan.getLink());

									if (videoPertemuan != null
											&& (link == null || link.trim().isEmpty() || !link.startsWith("http"))) {
										link = videoPertemuan.createLinkUri();
										if (link != null) {
										}
									}

									if (videoPertemuan != null && link != null && !link.trim().isEmpty()) {

										if (Common.isMobile()) {
											ExecutionsCtrl.getCurrent().sendRedirect(link, "_blank");
										} else {
											Clients.evalJavaScript("popupCenter({url: '" + ProfileUiHelper.js(link)
													+ "', title: 'data', w: 1200, h: 600});");
										}
									} else {
										MyMessageboxConfig.show(
				"Mohon maaf, Bapak/Ibu, berkas yang Anda akses tidak dapat ditemukan pada sistem. Langkah yang dapat dilakukan: (1) pastikan berkas masih tersedia dan belum dihapus; (2) muat ulang halaman lalu coba akses kembali; (3) apabila kendala masih berlanjut, hubungi Admin atau bagian terkait untuk bantuan lebih lanjut.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
									}
								}
							}
						});
					}
				}
			}

		}
		dataMateri = null;
		rowMateri.setVisible(adaMateri);
		rowTugas.setVisible(adaTugas);
		rowUjian.setVisible(adaUjian);

		if (buttonRefreshSyarat != null) {
			buttonRefreshSyarat.setVisible(adaMateri || adaTugas || adaUjian);

			if (buttonRefreshSyarat.getParent() != null) {
				buttonRefreshSyarat.getParent().setVisible(adaMateri || adaTugas || adaUjian);
			}
		}
	}

}
