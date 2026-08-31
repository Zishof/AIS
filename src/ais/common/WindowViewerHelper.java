package ais.common;

import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.jsoup.Jsoup;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Html;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Image;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.action.maintenance.MainAction;
import ais.common.gdrive.GDriveUtilPerPengguna;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.file.FileFoto;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.FotoAdmin;
import ais.database.model.file.FotoBiodataCalonMahasiswa;
import ais.database.model.file.FotoCalonPegawai;
import ais.database.model.file.FotoCalonSiswa;
import ais.database.model.file.FotoDosen;
import ais.database.model.file.FotoGambarProduk;
import ais.database.model.file.FotoMahasiswa;
import ais.database.model.file.FotoPegawai;
import ais.database.model.file.FotoSiswa;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.LampiranLainBiodataCalonMahasiswa;
import ais.database.model.file.LampiranLainMahasiswa;
import ais.database.model.file.PertemuanFileContent;
import ais.database.model.file.TugasFileContent;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Kumpulan helper statis untuk menampilkan konten (halaman ZUL, berkas lampiran/foto, iframe URL
 * eksternal) di dalam jendela modal ZK ({@link MyWindow}) pada aplikasi AIS. Kelas ini menjadi
 * titik sentral yang dipanggil dari banyak layar berbeda setiap kali aplikasi perlu menampilkan
 * "popup lihat data" — baik berupa halaman ZUL lain yang di-include ({@link #displayWindow(String,
 * boolean)} dan variannya), berkas lampiran/foto milik entitas domain seperti mahasiswa/siswa/
 * pegawai ({@link #display(FileFoto)}, {@link #displayWindow(Boolean, String, Boolean, String,
 * String, Boolean, FileFoto)}), maupun sekadar iframe URL murni tanpa logika deteksi berkas
 * ({@link #displayWindowIframe}).
 *
 * <h2>Pola umum jendela modal</h2>
 * <p>
 * Hampir seluruh method publik di kelas ini membangun struktur ZK yang sama: sebuah
 * {@link MyWindow} modal dilekatkan ke root halaman saat ini (lewat
 * {@link ExecutionsCtrl#getCurrentCtrl()}), berisi {@link Borderlayout} dengan area {@link Center}
 * (tempat konten utama — iframe, gambar, atau HTML info) dan area {@link South} berisi
 * {@link Toolbar} dengan tombol "Tutup" yang men-detach jendela. Jendela selalu diakhiri dengan
 * {@code window.onModal()} agar tampil sebagai dialog modal yang memblokir interaksi dengan
 * halaman di belakangnya sampai ditutup.
 * </p>
 *
 * <h2>Deteksi jenis konten dan strategi tampilan berkas</h2>
 * <p>
 * Untuk method yang menampilkan berkas ({@link #display(FileFoto)} dan
 * {@link #displayWindow(Boolean, String, Boolean, String, String, Boolean, FileFoto)}), kelas ini
 * menjalankan serangkaian pemeriksaan berjenjang untuk menentukan cara terbaik menampilkan suatu
 * berkas:
 * </p>
 * <ul>
 * <li><b>Berkas tersimpan di Google Drive</b> ({@code fileFoto.getGdrive() != null}) — ditampilkan
 * lewat iframe pratinjau bawaan Google Drive ({@code https://drive.google.com/file/d/.../preview}).</li>
 * <li><b>Berkas yang dapat dirender langsung oleh browser</b> (lihat {@link #isBrowserPreviewable}:
 * PDF, PNG, JPG/JPEG, WEBP, GIF, TXT) — ditampilkan langsung sebagai iframe/gambar tanpa perantara.</li>
 * <li><b>Dokumen Office (docx/xlsx/dsb.) yang berada di balik login eCampus</b> (URL cocok
 * {@link #isProtectedEcampusLampiranUrl}, mis. mengandung {@code "/al?d="} atau
 * {@code "ambillampiran"}) — TIDAK dikirim ke Google Docs Viewer, karena Google Viewer mengakses
 * URL tanpa membawa sesi login pengguna sehingga yang terbaca bisa jadi halaman login, bukan isi
 * dokumen. Sebagai gantinya ditampilkan kartu info penjelasan lewat
 * {@link #tampilkanInfoPreviewDokumenProtected} dengan tautan "Buka/unduh lewat eCampus".</li>
 * <li><b>Dokumen Office yang TIDAK berada di balik login</b> (URL publik) — diteruskan ke Google
 * Docs Viewer ({@code https://docs.google.com/gview?embedded=true&url=...}).</li>
 * <li><b>Berkas berjenis {@code .txt} yang sebenarnya berisi tautan URL</b> (konvensi
 * "berupa_link.txt": mahasiswa mengunggah tautan, bukan berkas fisik) — isi berkas dibaca sebagai
 * URL dan diteruskan ke {@link Common#displayUrlContent} alih-alih ditampilkan sebagai teks
 * mentah.</li>
 * <li><b>URL Google Drive/YouTube publik</b> — diteruskan ke {@link Common#displayUrlContent}
 * yang menangani perenderan embed masing-masing.</li>
 * <li><b>Berkas fisik hilang dari server</b> (record ada di database tapi file tidak ditemukan di
 * disk) — ditampilkan kartu peringatan merah "Berkas tidak ditemukan di server" alih-alih jendela
 * kosong tanpa penjelasan.</li>
 * </ul>
 *
 * <h2>Integrasi Google Drive sebagai penyimpanan sekunder</h2>
 * <p>
 * {@link #simpanKeDrive} menyediakan tombol toolbar opsional "Simpan ke Drive" yang, bila
 * diklik, mem-backup berkas lampiran ke Google Drive milik pengguna (lewat
 * {@link GDriveUtilPerPengguna}), memperbarui kolom {@code gdrive}/{@code gdriveUsername} pada
 * entitas berkas terkait, lalu menghapus salinan lokal lama. Proses backup berjalan asinkron dan
 * hasilnya dipantau lewat polling {@link Timer} 1 detik pada thread UI ZK (bukan callback
 * langsung), karena panggilan Google Drive API tidak dapat memblokir thread event ZK. <b>Perlu
 * dicatat</b>: tombol ini secara sengaja disembunyikan permanen di kode saat ini —
 * {@code save.setVisible(tampil && f != null && f.exists() && false)} — akibat operand
 * {@code && false} yang eksplisit, sehingga meski logika visibilitas lain terpenuhi, tombol tidak
 * akan pernah tampil sampai {@code && false} tersebut dihapus (lihat komentar pada kode
 * bersangkutan).
 * </p>
 *
 * <p>
 * Kelas ini murni statis (tidak ada instance state) dan tidak menyimpan kredensial atau rahasia
 * apa pun — seluruh URL dan path dibangun dari konfigurasi/host request saat runtime.
 * </p>
 */
public class WindowViewerHelper {

	/**
	 * Memeriksa apakah sebuah URL lampiran mengarah ke endpoint yang dilindungi login eCampus
	 * (memuat penanda {@code "/al?d="} atau {@code "ambillampiran"}), yang berarti URL tersebut
	 * TIDAK dapat diakses tanpa sesi login — sehingga tidak aman diteruskan ke Google Docs Viewer
	 * (lihat penjelasan pada Javadoc kelas).
	 *
	 * @param src URL/tautan yang diperiksa, boleh {@code null}
	 * @return {@code true} bila URL dianggap memerlukan sesi login eCampus
	 */
	private static boolean isProtectedEcampusLampiranUrl(String src) {
		if (src == null) {
			return false;
		}
		String lower = src.trim().toLowerCase();
		return lower.contains("/al?d=") || lower.contains("ambillampiran");
	}

	/**
	 * Memeriksa apakah nama berkas memiliki ekstensi yang umumnya dapat dirender langsung oleh
	 * browser modern tanpa perantara viewer eksternal (PDF, PNG, JPG/JPEG, WEBP, GIF, TXT).
	 *
	 * @param nama nama berkas (dengan ekstensi), boleh {@code null}
	 * @return {@code true} bila ekstensi berkas termasuk yang dapat dipratinjau langsung
	 */
	private static boolean isBrowserPreviewable(String nama) {
		if (nama == null) {
			return false;
		}
		String lower = nama.trim().toLowerCase();
		return lower.endsWith(".pdf") || lower.endsWith(".png") || lower.endsWith(".jpg")
				|| lower.endsWith(".jpeg") || lower.endsWith(".webp") || lower.endsWith(".gif")
				|| lower.endsWith(".txt");
	}

	/**
	 * Melakukan escaping karakter HTML dasar ({@code & " < >}) pada sebuah nilai agar aman
	 * disisipkan sebagai isi atribut HTML (mis. {@code href}) tanpa merusak markup atau membuka
	 * celah XSS sederhana.
	 *
	 * @param value nilai mentah yang akan di-escape, boleh {@code null}
	 * @return {@code value} dengan karakter {@code & " < >} sudah di-escape; string kosong bila
	 *         {@code value} adalah {@code null}
	 */
	private static String escapeAttr(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
	}

	/**
	 * Menambahkan kartu informasi HTML ke {@code center} yang menjelaskan bahwa pratinjau dokumen
	 * Office lewat Google Viewer tidak dapat ditampilkan karena berkas dilindungi login eCampus,
	 * beserta tautan tombol "Buka / unduh lewat eCampus" yang membuka {@code link} pada tab baru.
	 * Dipakai sebagai pengganti iframe Google Docs Viewer untuk berkas yang terdeteksi
	 * {@link #isProtectedEcampusLampiranUrl}.
	 *
	 * @param center komponen {@link Center} tempat kartu info ditambahkan sebagai anak
	 * @param link   URL unduh/akses langsung berkas lewat eCampus, di-escape lewat
	 *               {@link #escapeAttr} sebelum disisipkan ke atribut {@code href}
	 */
	private static void tampilkanInfoPreviewDokumenProtected(Center center, String link) {
		Html info = new ais.ui.util.MyHtml("<div style='margin:14px;padding:14px 16px;"
				+ "font-family:Arial,sans-serif;color:#334155;background:#f8fafc;border:1px solid #cbd5e1;"
				+ "border-radius:10px;line-height:1.5;'>"
				+ "<b>Preview dokumen Office tidak ditampilkan melalui Google.</b><br/>"
				+ "Berkas ini dilindungi login eCampus. Google Viewer tidak membawa sesi login pengguna, "
				+ "sehingga yang terbaca bisa halaman login, bukan isi dokumen."
				+ "<div style='margin-top:10px;'><a href='" + escapeAttr(link)
				+ "' target='_blank' rel='noopener noreferrer' "
				+ "style='display:inline-block;padding:7px 12px;border-radius:5px;background:#1d4ed8;"
				+ "color:#fff;text-decoration:none;font-weight:600;'>Buka / unduh lewat eCampus</a></div>"
				+ "</div>");
		center.appendChild(info);
	}

	/**
	 * Varian paling ringkas: menampilkan {@code src} (URL/path halaman ZUL) dalam jendela modal
	 * berukuran default 95% tinggi dan 95% lebar. Lihat varian paling lengkap
	 * {@link #displayWindow(String, boolean, String, String, EventListener, String, boolean)}
	 * untuk penjelasan penuh perilaku jendela.
	 *
	 * @param src           URL/path konten (biasanya halaman ZUL) yang di-include ke dalam jendela
	 * @param tampilToolbar tampilkan toolbar bawah berisi tombol "Tutup" bila {@code true}
	 * @return jendela {@link MyWindow} yang baru dibuat dan sudah ditampilkan
	 * @throws Exception diteruskan dari kegagalan membangun komponen ZK
	 */
	public static MyWindow displayWindow(String src, boolean tampilToolbar) throws Exception {
		return displayWindow(src, tampilToolbar, "95%", "95%");
	}

	/** Seperti {@link #displayWindow(String, boolean)}, dengan {@code lebar} kustom (tinggi tetap 95%). */
	public static MyWindow displayWindow(String src, boolean tampilToolbar, String lebar) throws Exception {
		return displayWindow(src, tampilToolbar, "95%", lebar);
	}

	/** Seperti {@link #displayWindow(String, boolean, String)}, dengan {@code tinggi} kustom juga, tanpa listener khusus penutupan (memakai listener kosong). */
	public static MyWindow displayWindow(String src, boolean tampilToolbar, String tinggi, String lebar)
			throws Exception {
		return displayWindow(src, tampilToolbar, tinggi, lebar, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				// Default empty listener
			}
		});
	}

	/** Seperti {@link #displayWindow(String, boolean, String, String)}, dengan {@code eventListener} kustom yang dipanggil saat jendela ditutup, judul default {@code "Tampilan Data"}. */
	public static MyWindow displayWindow(String src, boolean tampilToolbar, String tinggi, String lebar,
			final EventListener eventListener) throws Exception {
		return displayWindow(src, tampilToolbar, tinggi, lebar, eventListener, "Tampilan Data");
	}

	/** Seperti {@link #displayWindow(String, boolean, String, String, EventListener)}, dengan {@code judul} kustom, konten dapat di-scroll (default {@code scroll=true}). */
	public static MyWindow displayWindow(String src, boolean tampilToolbar, String tinggi, String lebar,
			final EventListener eventListener, String judul) throws Exception {
		return displayWindow(src, tampilToolbar, tinggi, lebar, eventListener, judul, true);
	}

	/**
	 * Implementasi kanonik seluruh keluarga overload {@code displayWindow(String, boolean, ...)}:
	 * membuka halaman ZUL/konten pada {@code src} di dalam jendela modal {@link MyWindow} yang
	 * dilekatkan ke root halaman saat ini. Konten dibungkus komponen {@link MyInclude} yang
	 * dipaksa memenuhi lebar penuh (100%, {@code display:block}) — tanpa pemaksaan ini, browser/ZK
	 * dapat mempertahankan lebar intrinsik halaman yang di-include (sering sekitar 50% modal)
	 * sehingga separuh kanan popup tampak kosong (lihat komentar penjelasan di badan method).
	 *
	 * <p>
	 * Bila {@code scroll=true}, tinggi {@link MyInclude} diatur sangat besar (dua kali tinggi
	 * layar pengguna yang tercatat di {@code MainAction#desktopHeights}, atau {@code 5000px} bila
	 * tidak diketahui) dan dibungkus {@link Div} dengan {@code overflow:auto} sebagai host scroll
	 * — sengaja TIDAK dibungkus {@link Grid}/{@link Row} karena pada ZUL bertingkat (mis. popup
	 * Pembayaran Mahasiswa), tabel {@link Grid} internal dapat menghitung lebar sel berdasarkan
	 * konten minimum sehingga Include 100% justru hanya mendapat sekitar setengah lebar
	 * {@link Center} (lihat komentar penjelasan di badan method).
	 * </p>
	 *
	 * <p>
	 * Bila {@code tampilToolbar=false}, jendela diberi judul eksplisit ({@code judul}) dan
	 * {@code eventListener} didaftarkan pada event {@link Events#ON_CLOSE} jendela (dipicu saat
	 * ditutup lewat tombol close bawaan ZK, bukan tombol "Tutup" kustom); bila
	 * {@code tampilToolbar=true}, jendela tanpa judul namun menampilkan toolbar berisi tombol
	 * "Tutup" yang memanggil {@code eventListener} lalu men-detach jendela.
	 * </p>
	 *
	 * @param src           URL/path konten yang di-include ke dalam jendela
	 * @param tampilToolbar tampilkan toolbar bawah berisi tombol "Tutup"; bila {@code false},
	 *                      judul jendela ditampilkan sebagai gantinya dan {@code eventListener}
	 *                      terpasang pada event tutup jendela
	 * @param tinggi        tinggi jendela (nilai CSS, mis. {@code "95%"}); {@code null} berarti
	 *                      {@code "97%"}
	 * @param lebar         lebar jendela (nilai CSS); {@code null} atau perangkat mobile berarti
	 *                      {@code "97%"}
	 * @param eventListener listener yang dipanggil saat jendela ditutup (lewat toolbar atau event
	 *                      close, tergantung {@code tampilToolbar}), boleh {@code null}
	 * @param judul         judul jendela, dipakai hanya saat {@code tampilToolbar=false}
	 * @param scroll        bungkus konten dengan host scroll bertinggi besar bila {@code true};
	 *                      bila {@code false}, konten ditambahkan langsung ke {@link Center} tanpa
	 *                      pengaturan tinggi/scroll khusus
	 * @return jendela {@link MyWindow} yang baru dibuat, sudah ditampilkan sebagai modal
	 * @throws Exception diteruskan dari kegagalan membangun komponen ZK atau resolusi pengguna
	 *                    saat ini
	 */
	public static MyWindow displayWindow(String src, boolean tampilToolbar, String tinggi, String lebar,
			final EventListener eventListener, String judul, boolean scroll) throws Exception {

		final MyWindow window = tampilToolbar ? new MyWindow("", "none", false) : new MyWindow(judul, "none", true);
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

		if (!tampilToolbar && eventListener != null) {
			window.addEventListener(Events.ON_CLOSE, eventListener);
		}

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(window);

		Center center = new Center();
		center.setBorder("none");
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyInclude c = new MyInclude(src);
		/*
		 * Include adalah isi utama Center. Tanpa width eksplisit, browser/ZK dapat
		 * mempertahankan lebar intrinsik halaman yang di-include (sering sekitar
		 * 50% modal), walaupun scrollHost dan ZUL anak sama-sama width="100%".
		 * Akibatnya separuh kanan popup kosong. Paksa Include menjadi blok penuh;
		 * aturan ini berlaku baik pada mode scroll maupun non-scroll.
		 */
		c.setWidth("100%");
		c.setStyle("display:block;max-width:100%;box-sizing:border-box;");
		if (scroll) {
			try {
				Tbmuser tbmuser = Common.getCurrentUser();
				if (tbmuser != null && tbmuser.getUserId() != null) {
					Integer desktopHeight = MainAction.desktopHeights.get(tbmuser.getUserId());
					if (desktopHeight != null) {
						c.setHeight((desktopHeight * 2) + "px");
					} else {
						c.setHeight("5000px");
					}
				} else {
					c.setHeight("5000px");
				}
			} catch (Exception e) {
				c.setHeight("5000px");
			}
			/*
			 * Jangan membungkus Include dengan Grid/Row. Pada ZUL bertingkat (misalnya
			 * popup Pembayaran Mahasiswa), tabel internal Grid dapat menghitung sel
			 * berdasarkan lebar minimum konten. Akibatnya Include 100% hanya mendapat
			 * sekitar setengah lebar Center dan menyisakan ruang kosong besar di kiri.
			 * Div adalah blok penuh dan tetap menyediakan perilaku scroll yang sama.
			 */
			Div scrollHost = new Div();
			scrollHost.setWidth("100%");
			scrollHost.setHeight("100%");
			scrollHost.setStyle("overflow:auto;box-sizing:border-box;");
			scrollHost.setParent(center);
			c.setParent(scrollHost);
		} else {
			center.appendChild(c);
		}

		South south = new South();
		south.setVisible(tampilToolbar);
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null) {
					eventListener.onEvent(event);
				}
				window.detach();
			}
		});
		cancel.setParent(toolbar);


		window.setVisible(true);
		window.setHeight(tinggi == null ? "97%" : tinggi);
		window.setWidth(lebar == null || Common.isMobile() ? "97%" : lebar);
		window.onModal();

		return window;
	}

	/**
	 * Menampilkan sebuah berkas ({@link FileFoto} atau turunannya, mis. {@link LampiranLain})
	 * dalam jendela modal, dijalankan secara asinkron lewat {@link Common#createDefaultTimer}
	 * (bukan langsung dalam thread event pemanggil) — pola yang umum dipakai AIS untuk operasi
	 * yang berpotensi memerlukan I/O (resolusi path/link berkas) tanpa memblokir UI.
	 *
	 * <p>
	 * Untuk berkas dengan {@code getGdrive() != null}, tautan diambil lewat
	 * {@link LampiranLain#downloadGDriveUrl()}. Untuk turunan {@link LampiranLain} lain, tautan
	 * dibangun lewat {@link LampiranLain#createLinkUri()}. Untuk berkas biasa, URL dibangun dari
	 * host request saat ini digabung path segmen folder berkas ({@code segmenFolderBerkas()} —
	 * dipakai agar primary key yang sama pada tabel entitas berbeda tidak berbagi folder fisik).
	 * Konten ditampilkan sebagai iframe langsung bila dapat dipratinjau browser
	 * ({@link #isBrowserPreviewable}) atau berasal dari Google Drive; bila berupa dokumen Office
	 * yang dilindungi login eCampus, ditampilkan kartu info penjelasan
	 * ({@link #tampilkanInfoPreviewDokumenProtected}) alih-alih iframe Google Docs Viewer; selain
	 * itu diteruskan ke Google Docs Viewer.
	 * </p>
	 *
	 * @param alurFile berkas yang akan ditampilkan; method langsung kembali tanpa efek apa pun
	 *                 bila {@code null}
	 * @throws Exception diteruskan dari kegagalan membangun timer/komponen ZK
	 */
	public static void display(final FileFoto alurFile) throws Exception {
		if (alurFile == null) {
			return;
		}

		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				boolean isLampiranLain = alurFile instanceof LampiranLain;
				boolean hasGdrive = isLampiranLain && ((LampiranLain) alurFile).getGdrive() != null;

				File f = hasGdrive ? null : alurFile.ambilFile();

				String link;
				if (hasGdrive) {
					link = ((LampiranLain) alurFile).downloadGDriveUrl();
				} else if (isLampiranLain) {
					link = ((LampiranLain) alurFile).createLinkUri();
				} else {
					String fileName = (f != null) ? f.getName() : "";
					// segmenFolderBerkas() = <NamaKelas>/<id>; berkas kini dipisah per entitas
					// supaya PK yang sama pada tabel berbeda tidak berbagi folder (lihat
					// FileFoto.segmenFolderBerkas()).
					link = Common.getRequestHostWithProtocol() + "/f" + CommonMedia.prefix + "/"
							+ alurFile.segmenFolderBerkas() + "/" + fileName;
				}

				System.out.println("link -> " + link);

				final MyWindow window = new MyWindow("Tampilan Data", "none", true);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

				Borderlayout borderlayout = new Borderlayout();
				borderlayout.setParent(window);

				Center center = new Center();
				center.setBorder("none");
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);

				String src;
				if (hasGdrive) {
					src = link;
				} else {
					boolean isViewableDirectly = false;
					boolean isDoc = false;
					if (f != null && f.getName() != null) {
						isViewableDirectly = isBrowserPreviewable(f.getName());
						isDoc = FileFoto.merupakanDokumen(f.getName());
					}

					if (isViewableDirectly) {
						src = link;
					} else if (isDoc && isProtectedEcampusLampiranUrl(link)) {
						tampilkanInfoPreviewDokumenProtected(center, link);
						src = null;
					} else {
						src = "https://docs.google.com/gview?embedded=true&url=" + URLEncoder.encode(link, "UTF-8");
					}
				}

				if (src != null && !src.trim().isEmpty()) {
					Iframe c = new Iframe(src);
					center.appendChild(c);
				}

				South south = new South();
				ais.ui.util.ZkCompat.setFlex(south, true);
				south.setParent(borderlayout);

				Toolbar toolbar = new Toolbar();
				toolbar.setParent(south);

				MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
				cancel.setTooltiptext("Tutup");
				cancel.addEventListener(Events.ON_CLICK, new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						window.detach();
					}
				});
				cancel.setParent(toolbar);

				window.setVisible(true);
				window.setHeight("97%");
				window.setWidth("97%");
				window.onModal();
			}
		});
	}

	/**
	 * Implementasi paling lengkap untuk menampilkan sebuah {@link FileFoto} (dan turunannya) dalam
	 * jendela modal, dengan kontrol eksplisit atas mode tampilan (gambar/iframe), sumber
	 * (Google Drive, berkas fisik lokal, atau URL yang sudah diberikan lewat {@code src}), serta
	 * tombol aksi tambahan pada toolbar (unduh, simpan ke Google Drive). Method ini dipanggil baik
	 * secara langsung oleh kode aplikasi maupun secara rekursif oleh dirinya sendiri (lewat
	 * callback {@link #simpanKeDrive}) setelah suatu berkas selesai dibackup ke Drive, untuk
	 * membuka ulang tampilan dengan data {@link FileFoto} yang sudah diperbarui.
	 *
	 * <p>
	 * Judul jendela diturunkan dari {@code ff.getKeterangan()} bila bukan berupa MIME type (mis.
	 * {@code "application/pdf"}), jika tidak memakai {@code ff.getNama()}; prefix {@code "id_"}
	 * pada nama berkas (pola penamaan berkas ber-primary-key AIS) dihilangkan dari judul.
	 * </p>
	 *
	 * <p>
	 * Alur penentuan konten mengikuti urutan pemeriksaan yang dijelaskan pada Javadoc kelas
	 * (PDF langsung, konten {@link PertemuanFileContent} dengan lokasi fisik, tautan Dropbox
	 * (dibuka lewat redirect di mobile atau popup JS di desktop), berkas {@code .txt} berisi
	 * tautan, berkas Google Drive, berkas fisik biasa lewat {@code CommonMedia}, dan fallback
	 * kartu "berkas tidak ditemukan"). Parameter {@code image} dan {@code iframe} mengontrol mode
	 * render akhir: {@code image=true} merender sebagai {@link Image} di dalam {@link Grid};
	 * {@code iframe=true} merender sebagai {@link Iframe} (dengan deteksi tambahan dokumen
	 * Office/Google Drive/YouTube); selain itu diteruskan ke
	 * {@link Common#displayUrlContent(String, Center)}.
	 * </p>
	 *
	 * <p>
	 * Toolbar bawah selalu memuat tombol "Tutup"; bila berkas fisik ditemukan, ditambahkan tombol
	 * "Download" (memakai {@link Filedownload#save}) dan tombol "Simpan ke Drive" (lewat
	 * {@link #simpanKeDrive}, walau saat ini selalu tersembunyi — lihat catatan pada Javadoc
	 * kelas); bila berkas hanya tersimpan di Google Drive, ditambahkan tombol "Download" yang
	 * mengarahkan (redirect) ke URL unduh Drive.
	 * </p>
	 *
	 * @param image         render konten sebagai gambar ({@link Image} dalam {@link Grid}) bila
	 *                      {@code true}; diabaikan bila {@code src} berakhiran {@code .pdf} atau
	 *                      berkas fisik tidak ditemukan
	 * @param src           URL/path konten; boleh kosong/{@code null} dan akan diturunkan otomatis
	 *                      dari {@code ff} bila memungkinkan
	 * @param tampilToolbar tampilkan toolbar bawah (tombol Tutup dan tombol aksi lain)
	 * @param lebar         lebar jendela (nilai CSS); diabaikan (dipaksa {@code "97%"}) pada
	 *                      perangkat mobile
	 * @param tinggi        tinggi jendela (nilai CSS); {@code null} berarti {@code "97%"}
	 * @param iframe        render konten sebagai {@link Iframe} bila {@code true} dan {@code image}
	 *                      tidak aktif; dapat diubah secara internal (mis. dipaksa {@code false}
	 *                      untuk berkas {@code .txt} berisi tautan)
	 * @param ff            entitas {@link FileFoto} (atau turunannya) yang berkasnya ditampilkan;
	 *                      boleh {@code null} bila hanya menampilkan {@code src} mentah tanpa
	 *                      konteks entitas
	 * @return jendela {@link MyWindow} yang baru dibuat, sudah ditampilkan sebagai modal
	 * @throws Exception diteruskan dari kegagalan I/O berkas, encoding URL, atau pembangunan
	 *                    komponen ZK
	 */
	public static MyWindow displayWindow(final Boolean image, String src, final Boolean tampilToolbar,
			final String lebar, final String tinggi, Boolean iframe, FileFoto ff) throws Exception {

		String windowTitle = "Tampilan Data";
		if (ff != null) {
			String ket = ff.getKeterangan();
			// Jika keterangan adalah MIME type (misal "application/pdf"), gunakan nama file
			boolean isMimeType = ket != null && ket.contains("/");
			windowTitle = (!isMimeType && ket != null && !ket.isEmpty()) ? ket : ff.getNama();
			if (windowTitle == null || windowTitle.trim().isEmpty())
				windowTitle = "Tampilan Data";
			// Hilangkan prefix id_ dari nama file
			if (windowTitle.contains("_") && windowTitle.matches("^\\d+_.*")) {
				windowTitle = windowTitle.substring(windowTitle.indexOf("_") + 1);
			}
		}

		final MyWindow window = new MyWindow(windowTitle, "none", true);
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

		final FileFoto fileFoto = ff;
		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(window);

		Center center = new Center();
		center.setBorder("none");
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		File file = null;

		String lowerSrc = (src != null) ? src.trim().toLowerCase() : "";

		if (lowerSrc.endsWith(".pdf") || lowerSrc.endsWith("pdf")) { // Menangani kasus ektensi pdf
			center.appendChild(new Iframe(src));
		} else {
			if (ff != null && ff instanceof PertemuanFileContent
					&& ((PertemuanFileContent) ff).getLokasiFisik() != null) {
				center.appendChild(new Iframe(src));
			} else {
				boolean textUpload = false;

				if (lowerSrc.contains("dropbox")) {
					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(src, "_blank");
					} else {
						Clients.evalJavaScript("popupCenter({url: '" + src + "', title: 'Dropbox', w: 1200, h: 600});");
					}
					return window;
				} else if (lowerSrc.endsWith(".txt")) {
					iframe = false;
					// "berupa_link.txt" = mahasiswa mengirim TAUTAN (URL), bukan berkas fisik.
					// Ambil URL-nya lalu BIARKAN jatuh ke perenderan di bawah (displayUrlContent)
					// agar tautan benar-benar tampil + toolbar "Tutup" tetap muncul. Sebelumnya di
					// sini langsung 'return window' dalam keadaan KOSONG sehingga popup tampil blank.
					String linkIsi = null;
					try {
						linkIsi = (ff != null) ? ff.ambilLink() : null;
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/WindowViewerHelper.java:298");
						// abaikan, coba baca isi berkas .txt langsung
					}
					if (linkIsi == null || linkIsi.trim().isEmpty()) {
						try {
							linkIsi = Jsoup.connect(src).userAgent("Mozilla").timeout(5000).ignoreContentType(true)
									.execute().body();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/WindowViewerHelper.java:305");
							// abaikan
						}
					}
					if (linkIsi != null && !linkIsi.trim().isEmpty()) {
						linkIsi = linkIsi.trim();
						src = linkIsi.toLowerCase().startsWith("http") ? linkIsi : ("http://" + linkIsi);
						ff = null;
						textUpload = true;
					}
				}

				if (fileFoto != null && fileFoto.getGdrive() != null) {
					Html html = new ais.ui.util.MyHtml("<iframe src=\"https://drive.google.com/file/d/"
							+ fileFoto.getGdrive() + "/preview\" style=\"width:100%;height:100%;\"></iframe>");
					html.setAttribute("lampiran_tambahan", true);
					html.setParent(center);
				} else {
					if (fileFoto != null) {
						try {
							if (fileFoto instanceof FileFotoLain) {
								file = fileFoto.ambilFile();
								if (fileFoto.getNama() != null && fileFoto.getNama().toLowerCase().endsWith(".txt")) {
									try {
										src = fileFoto.ambilLink().trim();
										iframe = false;
										textUpload = true;
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/WindowViewerHelper.java:333");
										src = ((FileFotoLain) fileFoto).createLinkUri();
									}
								} else if (src == null || src.trim().isEmpty()) {
									src = ((FileFotoLain) fileFoto).createLinkUri();
								}
							} else {
								file = CommonMedia.getFileFotoLangsungOld(fileFoto, false);
								if (file != null && file.exists()) {
									HttpServletRequest request = (HttpServletRequest) ExecutionsCtrl.getCurrent()
											.getNativeRequest();
									String baseUrl = "http" + (Common.isSecure(request) ? "s" : "") + "://"
											+ request.getServerName() + ":" + request.getServerPort() + "/media/";

									if (file.getName().toLowerCase().endsWith(".txt")) {
										try {
											src = fileFoto.ambilLink().trim();
											iframe = false;
											textUpload = true;
										} catch (Exception e) {
											src = baseUrl + URLEncoder.encode(file.getName(), "UTF-8");
										}
									} else {
										src = baseUrl + URLEncoder.encode(file.getName(), "UTF-8");
									}
								}
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/WindowViewerHelper.java:361");
						}
					}

					if ((src == null || src.trim().isEmpty()) && !textUpload && file != null && file.exists()) {
						src = LampiranLain.ambilLinkLampiranLain(file);
					}

					boolean fileHilang = (src == null || src.trim().isEmpty()) && !textUpload && fileFoto != null
							&& (file == null || !file.exists());
					if (fileHilang) {
						Html infoFileHilang = new Html();
						infoFileHilang.setContent("<div style='padding:18px;font-family:Arial,sans-serif;color:#7f1d1d;background:#fef2f2;border:1px solid #fecaca;border-radius:10px;'>"
								+ "<b>Berkas tidak ditemukan di server.</b><br/>"
								+ "Silakan unggah ulang berkas atau hubungi admin untuk restore file lampiran."
								+ "</div>");
						center.appendChild(infoFileHilang);
					}

					// Perbarui lowerSrc jika src dimodifikasi di atas
					lowerSrc = (src != null) ? src.trim().toLowerCase() : "";

					if (!fileHilang && lowerSrc.endsWith(".pdf") && !Common.isMobile()) {
						center.appendChild(new Iframe(src));
					} else if (!fileHilang && image != null && image) {
						Grid grid = new Grid();
						grid.setSclass("dgrid");
						center.appendChild(grid);

						Rows rows = new Rows();
						rows.setParent(grid);

						Row row = new Row();
						rows.appendChild(row);

						Image image2 = new Image(src);
						image2.setWidth("100%");
						row.appendChild(image2);
					} else if (!fileHilang && iframe != null && iframe) {
						boolean isDoc = (file != null && FileFoto.merupakanDokumen(file.getName()))
								|| FileFoto.merupakanDokumen(src);
						if (isDoc && isProtectedEcampusLampiranUrl(src)) {
							tampilkanInfoPreviewDokumenProtected(center, src);
						} else if (lowerSrc.startsWith("http") && isDoc) {
							String url = "https://docs.google.com/gview?embedded=true&url="
									+ URLEncoder.encode(src, "UTF-8");
							center.appendChild(new Iframe(url));
						} else {
							if (lowerSrc.startsWith("http")
									&& (lowerSrc.contains("drive.google.com") || lowerSrc.contains("youtu"))) {
								Common.displayUrlContent(src, center);
							} else {
								center.appendChild(new Iframe(src));
							}
						}
					} else if (!fileHilang) {
						Common.displayUrlContent(src, center);
					}
				}
			}
		}

		South south = new South();
		south.setVisible(tampilToolbar);
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		cancel.setParent(toolbar);

		if (fileFoto != null && file != null && file.exists()) {
			final File f = file;
			String fileNama = (fileFoto.getNama() != null && !fileFoto.getNama().trim().isEmpty())
					? " \"" + fileFoto.getNama() + "\""
					: "";

			MyToolbarbuttonConfig downloadBtn = new MyToolbarbuttonConfig("Download" + fileNama,
					fileFoto.iconDonwload());
			downloadBtn.setTooltiptext("Download" + fileNama);
			downloadBtn.addEventListener(Events.ON_CLICK, new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (f == null || !f.exists()) {
						MyMessageboxConfig.show(
								"Berkas tidak ditemukan di server. Silakan unggah ulang atau hubungi admin.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						return;
					}
					Filedownload.save(f, fileFoto.getKeterangan());
				}
			});
			downloadBtn.setParent(toolbar);

			final boolean frame = iframe != null ? iframe : false;
			WindowViewerHelper.simpanKeDrive(fileFoto, f, f.getParentFile().getName(), new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					window.detach();
					displayWindow(image, "", tampilToolbar, lebar, tinggi, frame, (FileFoto) arg0.getData());
				}
			}).setParent(toolbar);

		} else if (fileFoto != null && fileFoto.getGdrive() != null) {
			String fileNama = (fileFoto.getNama() != null && !fileFoto.getNama().trim().isEmpty())
					? " \"" + fileFoto.getNama() + "\""
					: "";

			MyToolbarbuttonConfig downloadGdriveBtn = new MyToolbarbuttonConfig("Download" + fileNama,
					fileFoto.iconDonwload());
			downloadGdriveBtn.setTooltiptext("Download" + fileNama);
			downloadGdriveBtn.addEventListener(Events.ON_CLICK, new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					ExecutionsCtrl.getCurrent().sendRedirect(fileFoto.downloadGDriveUrl(), "_blank");
				}
			});
			downloadGdriveBtn.setParent(toolbar);
		}

			if ((center.getChildren() == null || center.getChildren().size() == 0)
					&& fileFoto != null && (file == null || !file.exists())) {
				Html info = new Html("<div style='padding:18px;border:1px solid #fecaca;background:#fff7f7;border-radius:10px;color:#7f1d1d;'>"
						+ "<b>Berkas tidak ditemukan di server.</b><br/>"
						+ "Data lampiran masih tercatat, tetapi file fisiknya tidak ada di lokasi penyimpanan. "
						+ "Silakan unggah ulang berkas atau hubungi admin.</div>");
				info.setParent(center);
			}

		window.setVisible(true);
		window.setHeight(tinggi == null ? "97%" : tinggi);
		window.setWidth(Common.isMobile() ? "97%" : lebar);
		window.onModal();

		return window;
	}

	/** Seperti {@link #simpanKeDrive(FileFoto, File, String, String, EventListener)} tanpa sub-folder tambahan ({@code folderNameLagi=null}). */
	public static MyToolbarbuttonConfig simpanKeDrive(FileFoto fileFoto, File f, String folderName,
			EventListener eventListener) {
		return simpanKeDrive(fileFoto, f, folderName, null, eventListener);
	}

	/**
	 * Membangun tombol toolbar "Simpan ke Drive" yang, bila diklik, membackup berkas {@code f}
	 * ke Google Drive milik pengguna saat ini (lewat {@link GDriveUtilPerPengguna#prosesBackup}),
	 * lalu memperbarui kolom {@code gdrive}/{@code gdriveUsername} pada entitas {@code fileFoto}
	 * dan menghapus salinan lokal lama (lewat {@link FileFoto#hapusTotal}). Lihat penjelasan
	 * lengkap alur backup + polling status pada Javadoc kelas.
	 *
	 * <p>
	 * Tombol disembunyikan sepenuhnya (tidak pernah ditampilkan pada kondisi kode saat ini) untuk
	 * jenis foto entitas utama (mahasiswa, dosen, pegawai, siswa, dsb. — daftar {@code instanceof}
	 * pada badan method) dan untuk jenis {@link LampiranLain} bertanda tangan digital tertentu
	 * (TTD dosen/kartu anggota perpustakaan/dsb.), serta — akibat operand {@code && false} yang
	 * eksplisit pada penetapan {@code setVisible} — untuk SEMUA jenis berkas lainnya juga, terlepas
	 * dari nilai kondisi lain (lihat catatan pada Javadoc kelas).
	 * </p>
	 *
	 * <p>
	 * Setelah proses backup dimulai, sebuah {@link Timer} berulang (interval 1 detik) didaftarkan
	 * pada root halaman untuk memantau daftar hasil {@code s}; begitu berisi (backup selesai),
	 * timer dihentikan dan {@code eventListener} dipanggil dengan data {@link FileFoto} yang sudah
	 * diperbarui — pola polling ini diperlukan karena panggilan Google Drive API berjalan di luar
	 * thread event ZK dan tidak dapat memanggil balik komponen UI secara langsung.
	 * </p>
	 *
	 * @param fileFoto       entitas berkas yang akan dibackup dan diperbarui referensi Drive-nya
	 * @param f              berkas fisik lokal yang akan diunggah ke Drive
	 * @param folderName     nama folder Drive tujuan (biasanya nama folder lokal induk berkas)
	 * @param folderNameLagi sub-folder tambahan di dalam {@code folderName}, boleh {@code null}
	 * @param eventListener  dipanggil setelah backup selesai, menerima {@link FileFoto} terbaru
	 *                       sebagai data event
	 * @return tombol {@link MyToolbarbuttonConfig} "Simpan ke Drive" (visibilitasnya lihat catatan
	 *         di atas — pada kode saat ini selalu tersembunyi)
	 */
	public static MyToolbarbuttonConfig simpanKeDrive(final FileFoto fileFoto, final File f, final String folderName,
			final String folderNameLagi, final EventListener eventListener) {

		Tbmuser tbmuser = Common.getCurrentUser();
		String olehId = Common.generateOlehId(tbmuser);

		boolean tampil = Common.getApakahAdmin()
				|| (tbmuser != null && fileFoto.getOlehId() != null && olehId.equalsIgnoreCase(fileFoto.getOlehId()));

		if (fileFoto != null && fileFoto.ambilLink() != null && !fileFoto.ambilLink().trim().isEmpty()) {
			tampil = false;
		}

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan ke Drive", "/img/Google-Drive-icon.png");

		if (fileFoto instanceof FotoMahasiswa || fileFoto instanceof FotoBiodataCalonMahasiswa
				|| fileFoto instanceof FotoDosen || fileFoto instanceof FotoPegawai || fileFoto instanceof FotoSiswa
				|| fileFoto instanceof FotoCalonSiswa || fileFoto instanceof FotoCalonPegawai
				|| fileFoto instanceof FotoGambarProduk || fileFoto instanceof FotoAdmin
				|| fileFoto instanceof FileFoto) {
			save.setVisible(false);
			return save;
		}

		if (fileFoto instanceof LampiranLain) {
			LampiranLain lampiranLain = (LampiranLain) fileFoto;
			String jenis = lampiranLain.getJenis();
			if ((jenis != null && jenis.equalsIgnoreCase(LampiranLain.TTD_DOSEN))
					|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.TTD_KARTU_ALUMNI_PERPUSTAKAAN_STR))
					|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.TTD_KARTU_ANGGOTA_PERPUSTAKAAN_STR))
					|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.TTD_KARTU_MAHASISWA_PERPUSTAKAAN_STR))
					|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.TTD_KARTU_PEGAWAI_PERPUSTAKAAN_STR))
					|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.TTD_KARTU_SISWA_PERPUSTAKAAN_STR))
					|| (jenis != null && jenis.equalsIgnoreCase(LampiranLain.TTD_PEGAWAI))) {
				save.setVisible(false);
				return save;
			}
		}

		save.setTooltiptext("Simpan file ini ke Google Drive");
		save.setAttribute("janganDisabled", true);

		// Catatan: && false membuat button ini selalu tersembunyi sesuai kode asli
		// Anda.
		// Jika tujuannya agar aktif, hapus '&& false'.
		save.setVisible(tampil && f != null && f.exists() && false);

		save.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				final Tbmuser currentUser = Common.getCurrentUser();
				if (currentUser != null) {
					GDriveUtilPerPengguna driveUtilPerPengguna = new GDriveUtilPerPengguna(currentUser);
					final List<FileFoto> s = new ArrayList<FileFoto>();

					driveUtilPerPengguna.prosesBackup(f, folderName, folderNameLagi, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							com.google.api.services.drive.model.File fileUpload = (com.google.api.services.drive.model.File) arg0
									.getData();

							if (fileUpload != null && fileUpload.getId() != null) {
								Session session = null;
								try {
									session = StreamingHibernateUtil.getInstance().currentSession();

									String kolomName = "";
									String tableName = "";

									if (fileFoto instanceof LampiranLain) {
										kolomName = "foto";
										tableName = "lampiran_lain";
									} else if (fileFoto instanceof LampiranLainMahasiswa) {
										kolomName = "foto";
										tableName = "lampiran_lain_mahasiswa";
									} else if (fileFoto instanceof LampiranLainBiodataCalonMahasiswa) {
										kolomName = "foto";
										tableName = "lampiran_lain_biodata_calon_mahasiswa";
									} else if (fileFoto instanceof PertemuanFileContent) {
										kolomName = "filecontent";
										tableName = "pertemuan_file_content";
									} else if (fileFoto instanceof TugasFileContent) {
										kolomName = "filecontent";
										tableName = "tugas_file_content";
									} else if (fileFoto instanceof FotoMahasiswa) {
										kolomName = "foto";
										tableName = "foto_mahasiswa";
									}

									Object fotoId = null;
									if (!kolomName.trim().isEmpty()) {
										String sql = "select " + kolomName + " from " + tableName + " where id = "
												+ fileFoto.getId();
										fotoId = session.createSQLQuery(sql).uniqueResult();
									}

									session.refresh(fileFoto);
									fileFoto.setGdrive(fileUpload.getId());
									fileFoto.setGdriveUsername(currentUser == null ? Common.getCurrentSessionId()
											: currentUser.getUserId());

									session.getTransaction().begin();
									session.update(fileFoto);
									session.getTransaction().commit();

									fileFoto.write("Simpan ke drive " + kolomName + " " + tableName);

									if (fileFoto instanceof FileFotoLain) {
										FileFotoLain lampiranLain = (FileFotoLain) fileFoto;
										Boolean usingId = false;
										FileFotoLain.resetLokasi(usingId, lampiranLain.ambilRef(),
												lampiranLain.getJenis(), fileFoto.getClass());
									}

									if (fotoId != null) {
										FileFoto.hapusTotal(fotoId.toString(), session);
									}

									s.add(fileFoto);

								} catch (Exception e) {
									StreamingHibernateUtil.getInstance().rollbackTransaction();
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/WindowViewerHelper.java:633");
								} finally {
									// PENTING: Memastikan session selalu di-close untuk mencegah memory/connection
									// leak.
									StreamingHibernateUtil.getInstance().closeSession();
								}
							}
						}
					});

					final Timer timer = new Timer(1000);
					timer.setRepeats(true);
					timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					timer.addEventListener("onTimer", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							if (!s.isEmpty()) {
								timer.detach();
								eventListener.onEvent(new Event("", null, s.get(0)));
								s.clear(); // Bersihkan referensi list agar segera dibuang oleh GC
							}
						}
					});
					timer.start();
				}
			}
		});
		return save;
	}

	/** Seperti {@link #displayWindowIframe(String, Boolean, String, String, String)} dengan judul default {@code "Tampilan Data"}. */
	public static MyWindow displayWindowIframe(String src, Boolean tampilToolbar, String lebar, String tinggi)
			throws Exception {
		return displayWindowIframe(src, tampilToolbar, lebar, tinggi, "Tampilan Data");
	}

	/**
	 * Menampilkan {@code src} sebagai iframe murni di dalam jendela modal, tanpa logika deteksi
	 * jenis berkas/dokumen seperti pada {@link #displayWindow(Boolean, String, Boolean, String,
	 * String, Boolean, FileFoto)} — cocok dipakai saat pemanggil sudah tahu persis {@code src}
	 * adalah URL yang aman ditampilkan langsung dalam iframe (mis. laporan/report internal).
	 *
	 * @param src           URL yang ditampilkan sebagai iframe
	 * @param tampilToolbar tampilkan toolbar bawah berisi tombol "Tutup"; bila {@code false},
	 *                      {@code judul} ditampilkan sebagai judul jendela sebagai gantinya
	 * @param lebar         lebar jendela (nilai CSS); dipaksa {@code "97%"} pada perangkat mobile
	 * @param tinggi        tinggi jendela (nilai CSS); {@code null} berarti {@code "97%"}
	 * @param judul         judul jendela, dipakai hanya saat {@code tampilToolbar=false}
	 * @return jendela {@link MyWindow} yang baru dibuat, sudah ditampilkan sebagai modal
	 * @throws Exception diteruskan dari kegagalan membangun komponen ZK
	 */
	public static MyWindow displayWindowIframe(String src, Boolean tampilToolbar, String lebar, String tinggi,
			String judul) throws Exception {
		final MyWindow window = tampilToolbar ? new MyWindow("", "none", false) : new MyWindow(judul, "none", true);
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		Center center = new Center();
		center.setBorder("none");
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.appendChild(new Iframe(src));

		South south = new South();
		south.setVisible(tampilToolbar);
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		cancel.setParent(toolbar);

		window.setVisible(true);
		window.setHeight(tinggi == null ? "97%" : tinggi);
		window.setWidth(Common.isMobile() ? "97%" : lebar);
		window.onModal();

		return window;
	}
}
