package ais.action.master.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.lang.ref.SoftReference;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Bantuan;
import ais.database.model.Menu;
import ais.database.model.Tbmuser;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * Helper bantuan/panduan halaman yang dapat dipakai ulang oleh ZUL mana pun.
 *
 * <p><b>Tujuan.</b> Menyediakan satu tombol "Bantuan" standar yang, saat diklik,
 * menampilkan panduan penggunaan halaman dalam sebuah jendela modal yang dapat
 * digulir (scroll). Panduan ditulis dalam berkas HTML terpisah agar mudah dirawat
 * dan dapat memuat penjelasan panjang (mis. arti setiap field/kolom serta fungsi
 * tiap tombol) tanpa membebani berkas ZUL.</p>
 *
 * <p><b>Cara pakai dari ZUL</b> (zscript inline pada atribut {@code onClick}):</p>
 * <pre>
 * &lt;toolbarbutton label="Bantuan" image="/img/info.gif"
 *     tooltiptext="Panduan penggunaan halaman ini"
 *     onClick="ais.action.master.helper.BantuanHelper.tampilkanDariResource(
 *                  self, 'matakuliah', 'Bantuan — Matakuliah');"/&gt;
 * </pre>
 *
 * <p>Argumen kedua adalah <i>key</i>: nama berkas panduan tanpa ekstensi yang
 * terletak di {@code webapp/WEB-INF/bantuan/&lt;key&gt;.html}. Bila berkas itu
 * belum ada, ditampilkan panduan umum bawaan agar tombol tetap berguna.</p>
 *
 * <p>Komponen sengaja dibangun di Java (bukan popup ZUL) supaya satu definisi
 * dipakai bersama oleh banyak halaman dan konsisten tampilannya.</p>
 */
public class BantuanHelper {

	private BantuanHelper() {
	}

	/**
	 * Tampilkan panduan dari berkas {@code /WEB-INF/bantuan/<key>.html}.
	 *
	 * @param self  komponen pemicu (mis. {@code self} pada onClick ZUL) untuk
	 *              mendapatkan konteks halaman; boleh null
	 * @param key   nama berkas panduan (tanpa ekstensi) di folder WEB-INF/bantuan
	 * @param judul judul jendela bantuan
	 */
	public static void tampilkanDariResource(Component self, String key, String judul) {
		catatBuka(key);
		String html = muatKontenFile(key);
		if (html == null || html.trim().isEmpty()) {
			// Belum ada berkas panduan khusus → bangun panduan otomatis dari isi
			// halaman (tab, kolom, dan tombol) ditambah panduan umum.
			html = bangunOtomatis(self);
		}
		tampilkan(self, judul, html, key);
	}

	/**
	 * Tampilkan tanya jawab kontekstual untuk halaman aktif. Materi khusus halaman
	 * ditempatkan sebagai jawaban pertama, kemudian dilengkapi FAQ operasional umum
	 * yang panjangnya minimal 2.000 kata dan dapat dipakai tanpa pengetahuan teknis.
	 */
	public static void tampilkanTanyaJawabDariResource(Component self, String key, String judul) {
		String khusus = muatKontenFile(key);
		if (khusus == null || khusus.trim().isEmpty()) {
			khusus = bangunOtomatis(self);
		}
		// FAQ khusus per halaman (<key>_qa.html); bila belum ada, pakai FAQ umum bersama.
		String faq = key == null ? null : muatKontenFile(key.trim() + "_qa");
		if (faq == null || faq.trim().isEmpty()) {
			faq = muatKontenFile("_qa_umum");
		}
		if (faq == null) {
			faq = "";
		}
		String html = "<details open style='border:1px solid #bbf7d0;border-radius:10px;padding:0 14px;margin-bottom:10px;'>"
				+ "<summary style='cursor:pointer;color:#166534;font-weight:700;padding:12px 0;'>"
				+ "Apa fungsi dan petunjuk khusus halaman ini?</summary><div style='padding-bottom:12px;'>"
				+ khusus + "</div></details>" + faq;
		// Buka dengan KEY FAQ (<key>_qa) agar tombol Ubah/Share/Terjemah tersedia seperti pada
		// panduan biasa — sehingga FAQ juga dapat disunting dan diterjemahkan ke 4 bahasa.
		String keyFaq = key == null || key.trim().isEmpty() ? null : key.trim() + "_qa";
		tampilkan(self, judul == null || judul.trim().isEmpty() ? "Tanya Jawab" : judul, html, keyFaq);
	}

	/**
	 * Tampilkan panduan dengan konten HTML yang sudah disiapkan.
	 *
	 * @param self        komponen pemicu untuk konteks halaman; boleh null
	 * @param judul       judul jendela
	 * @param htmlKonten  isi panduan dalam HTML
	 */
	public static void tampilkan(Component self, String judul, String htmlKonten) {
		tampilkan(self, judul, htmlKonten, null);
	}

	/**
	 * Varian dengan {@code key} berkas panduan. Bila {@code key} tersedia dan pengguna
	 * berhak (administrator), ditambahkan tombol <b>Ubah</b> untuk menyunting isi panduan
	 * langsung dari aplikasi dan menyimpannya ke {@code WEB-INF/bantuan/<key>.html} tanpa
	 * perlu deploy ulang (bantuan bersifat dinamis).
	 */
	public static void tampilkan(final Component self, final String judul, String htmlKonten, final String key) {
		try {
			final MyWindow window = new MyWindow(judul == null || judul.trim().isEmpty() ? "Bantuan" : judul,
					"normal", true);
			window.setWidth("74%");
			window.setHeight("84%");
			window.setMaximizable(true);
			window.setStyle("overflow:hidden;");
			window.setContentStyle("overflow:hidden;padding:0;background:#f6f8fb;");

			window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

			// Gunakan Div (bukan Vbox). Vbox ZK dirender sebagai <table> sehingga display:flex
			// tidak berlaku dan area isi tidak pernah mendapat tinggi terbatas -> scrollbar tak
			// pernah muncul. Div adalah elemen blok biasa sehingga flexbox bekerja semestinya.
			Div box = new Div();
			box.setWidth("100%");
			box.setHeight("100%");
			box.setStyle("display:flex;flex-direction:column;height:100%;box-sizing:border-box;");
			box.setParent(window);

			// Referensi maju ke komponen Html isi & teks bawaan (Indonesia) untuk tombol Terjemah.
			final org.zkoss.zul.Html[] htmlHolder = new org.zkoss.zul.Html[1];
			final String[] baseHolder = new String[1];

			Toolbar toolbar = new Toolbar();
			toolbar.setStyle("flex:0 0 auto;background:#eef2f7;border-bottom:1px solid #d7dee8;");
			toolbar.setParent(box);
			final String judulCetak = judul == null || judul.trim().isEmpty() ? "Bantuan" : judul;
			final String isiCetak = htmlKonten == null ? "" : htmlKonten;

			if (key != null && !key.trim().isEmpty() && !key.trim().endsWith("_qa")) {
				final String qaKey = key;
				MyToolbarbuttonConfig qa = new MyToolbarbuttonConfig("Tanya Jawab", "/img/svg/info.svg");
				qa.setTooltiptext("Buka tanya jawab lengkap sesuai halaman ini");
				qa.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						tampilkanTanyaJawabDariResource(self, qaKey, "Tanya Jawab — " + judulCetak);
					}
				});
				qa.setParent(toolbar);
			}

			MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("Cetak", "/img/svg/printer.svg");
			cetak.setTooltiptext("Cetak panduan ini");
			cetak.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					cetakHtml(judulCetak, isiCetak);
				}
			});
			cetak.setParent(toolbar);

			// Tombol Pusat Panduan: kumpulan panduan menurut peran pengguna (petugas
			// perpustakaan, dosen, mahasiswa, admin/calon siswa PPDB). Sengaja diletakkan
			// pada SETIAP jendela bantuan agar dapat dicapai dari mana saja tanpa perlu
			// mengetik alamat, baik pada tampilan Desktop maupun Mobile.
			if (key == null || !key.trim().toLowerCase().startsWith("panduan")) {
				MyToolbarbuttonConfig pusat = new MyToolbarbuttonConfig("Pusat Panduan", "/img/svg/books-thin.svg");
				pusat.setTooltiptext("Kumpulan panduan menurut peran pengguna");
				pusat.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						tampilkanDariResource(self, "panduan", "Pusat Panduan");
					}
				});
				pusat.setParent(toolbar);
			}

			// Terjemah: alih-bahasakan isi panduan (Indonesia/English/Arab/Mandarin) DI jendela ini, tanpa
			// mengubah bahasa aplikasi. DUA tombol: "Terjemahkan AI" (Ollama, kualitas lebih baik) dan
			// "Terjemahkan Cepat" (kamus internal HTML-aware, instan/tanpa server AI).

			// --- Menu + tombol Terjemahkan AI ---
			final org.zkoss.zul.Menupopup popTerjemahAi = new org.zkoss.zul.Menupopup();
			tambahItemTerjemah(popTerjemahAi, "Indonesia", "indonesia", "id", htmlHolder, baseHolder, true);
			tambahItemTerjemah(popTerjemahAi, "English", "united-kingdom", "english", htmlHolder, baseHolder, true);
			tambahItemTerjemah(popTerjemahAi, "العربية", "saudi-arabia", "arab", htmlHolder, baseHolder, true);
			tambahItemTerjemah(popTerjemahAi, "中文", "china", "mandarin", htmlHolder, baseHolder, true);
			popTerjemahAi.setParent(box);

			final MyToolbarbuttonConfig terjemahAi = new MyToolbarbuttonConfig("Terjemahkan AI",
					"/img/svg/sparkles.svg");
			terjemahAi.setTooltiptext("Terjemahkan panduan via AI (Ollama). Bila AI tidak terkoneksi otomatis "
					+ "memakai kamus internal.");
			terjemahAi.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					popTerjemahAi.open(terjemahAi, "after_start");
				}
			});
			terjemahAi.setParent(toolbar);

			// --- Menu + tombol Terjemahkan Cepat (kamus internal) ---
			final org.zkoss.zul.Menupopup popTerjemahCepat = new org.zkoss.zul.Menupopup();
			tambahItemTerjemah(popTerjemahCepat, "Indonesia", "indonesia", "id", htmlHolder, baseHolder, false);
			tambahItemTerjemah(popTerjemahCepat, "English", "united-kingdom", "english", htmlHolder, baseHolder, false);
			tambahItemTerjemah(popTerjemahCepat, "العربية", "saudi-arabia", "arab", htmlHolder, baseHolder, false);
			tambahItemTerjemah(popTerjemahCepat, "中文", "china", "mandarin", htmlHolder, baseHolder, false);
			popTerjemahCepat.setParent(box);

			final MyToolbarbuttonConfig terjemahCepat = new MyToolbarbuttonConfig("Terjemahkan Cepat",
					"/img/svg/flag.svg");
			terjemahCepat.setTooltiptext("Terjemahkan cepat memakai kamus internal (tanpa server AI, instan)");
			terjemahCepat.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					popTerjemahCepat.open(terjemahCepat, "after_start");
				}
			});
			terjemahCepat.setParent(toolbar);

			// Tombol Share: menampilkan tautan publik panduan yang dapat dibuka TANPA login.
			if (key != null && !key.trim().isEmpty()) {
				final String judulShare = judulCetak;
				MyToolbarbuttonConfig share = new MyToolbarbuttonConfig("Share", "/img/svg/send.svg");
				share.setTooltiptext("Bagikan panduan ini — dapat dibuka semua orang tanpa login");
				share.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						bukaShare(key, judulShare);
					}
				});
				share.setParent(toolbar);
			}

			// Tombol Ubah (khusus administrator): sunting isi panduan langsung dari aplikasi.
			if (key != null && !key.trim().isEmpty() && bolehEditBantuan()) {
				MyToolbarbuttonConfig ubah = new MyToolbarbuttonConfig("Ubah", "/img/svg/edit.svg");
				ubah.setTooltiptext("Sunting isi panduan ini (administrator)");
				ubah.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						bukaEditor(self, judul, key, window);
					}
				});
				ubah.setParent(toolbar);
			}

			MyToolbarbuttonConfig tutup = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
			tutup.setTooltiptext("Tutup panduan");
			tutup.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					window.detach();
				}
			});
			tutup.setParent(toolbar);

			Div scroll = new Div();
			scroll.setWidth("100%");
			// min-height:0 WAJIB: tanpa ini, anak flex menolak mengecil di bawah tinggi kontennya
			// sehingga overflow:auto tidak pernah menghasilkan scroll. max-height berbasis viewport
			// menjadi pengaman bila perhitungan flex tidak diterapkan browser tertentu.
			scroll.setStyle("flex:1 1 auto;min-height:0;overflow-y:auto;overflow-x:hidden;background:#ffffff;"
					+ "max-height:calc(84vh - 58px);-webkit-overflow-scrolling:touch;");
			scroll.setParent(box);

			Div pad = new Div();
			pad.setStyle("max-width:980px;margin:0 auto;padding:18px 22px;font-size:13px;line-height:1.7;color:#0f172a;");
			pad.setParent(scroll);

			Html html = new Html(htmlKonten == null ? "" : htmlKonten);
			html.setParent(pad);

			// Sediakan teks sumber (Indonesia) untuk tombol Terjemah: pakai isi mentah dari
			// tabel/berkas bila ada key; jika tidak, pakai konten yang sedang ditampilkan.
			htmlHolder[0] = html;
			String baseTerjemah = isiCetak;
			if (key != null && !key.trim().isEmpty()) {
				String raw = muatKontenFile(key);
				if (raw != null && !raw.trim().isEmpty()) {
					baseTerjemah = raw;
				}
			}
			baseHolder[0] = baseTerjemah;

			window.setVisible(true);
			window.onModal();
		} catch (Throwable t) {
			try {
				Common.tampilErrorJikaAdmin(t instanceof Exception ? (Exception) t : new Exception(t));
				PesanFormalHelper.tampilkanGagalException("penampilan konten Bantuan",
						t,
						new String[] {
								"Muat ulang (refresh) halaman ini, lalu buka kembali menu Bantuan.",
								"Pastikan konten Bantuan untuk halaman ini sudah pernah diisi oleh Administrator.",
								"Bila kegagalan berulang, laporkan ke Administrator/pengembang disertai tangkapan layar (screenshot) pesan ini."
						});
			} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:199");
			}
		}
	}

	/**
	 * Muat isi panduan. Mengutamakan isi yang telah dimodifikasi di <b>tabel Bantuan</b>;
	 * bila belum pernah dimodifikasi, jatuh ke berkas bawaan
	 * {@code WEB-INF/bantuan/<key>.html}. Kembalikan {@code null} bila keduanya tak ada.
	 */
	private static String muatKontenFile(String key) {
		String dariTabel = ambilDariTabel(key);
		if (dariTabel != null && !dariTabel.trim().isEmpty()) {
			return terjemahKontenBantuanBilaPerlu(dariTabel);
		}
		FileInputStream in = null;
		try {
			if (key != null && !key.trim().isEmpty()) {
				String path = Sessions.getCurrent().getWebApp()
						.getRealPath("/WEB-INF/bantuan/" + key.trim() + ".html");
				if (path != null) {
					File f = new File(path);
					if (f.exists() && f.isFile()) {
						in = new FileInputStream(f);
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						byte[] buf = new byte[8192];
						int n;
						while ((n = in.read(buf)) != -1) {
							bos.write(buf, 0, n);
						}
						return terjemahKontenBantuanBilaPerlu(new String(bos.toByteArray(), "UTF-8"));
					}
				}
			}
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:233");
			// abaikan → kembalikan null agar dibangun panduan otomatis
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:239");
				}
			}
		}
		return null;
	}

	/**
	 * Bila bahasa aktif bukan Indonesia, terjemahkan otomatis konten panduan (HTML) via kamus lokal
	 * dengan menjaga tag/atribut. Berlaku untuk konten bawaan berkas {@code .html} maupun yang tersimpan
	 * di tabel Bantuan.
	 */
	private static String terjemahKontenBantuanBilaPerlu(String konten) {
		try {
			if (konten == null || konten.trim().isEmpty()) {
				return konten;
			}
			String lang = ais.common.Common.currentLang();
			if (lang == null || lang.equalsIgnoreCase(ais.database.model.Tbmuser.INDONESIA)) {
				return konten;
			}
			boolean arab = lang.equalsIgnoreCase(ais.database.model.Tbmuser.ARAB);
			return ais.common.KamusBahasaInternal.terjemahHtml(konten, arab ? "arab" : "english");
		} catch (Exception e) {
			return konten;
		}
	}

	// ==================== Penyimpanan panduan termodifikasi (tabel Bantuan) ====================

	private static String normalKunci(String key) {
		if (key == null) {
			return null;
		}
		String k = key.trim().toLowerCase();
		return (k.length() == 0 || !k.matches("[a-z0-9_\\-]+")) ? null : k;
	}

	/** Ambil isi panduan dari tabel Bantuan; {@code null} bila belum pernah dimodifikasi. */
	public static String ambilDariTabel(String key) {
		String k = normalKunci(key);
		if (k == null) {
			return null;
		}
		Session s = null;
		try {
			s = HibernateUtil.openSession();
			List<?> l = s.createCriteria(Bantuan.class)
					.add(Restrictions.eq("kunci", k))
					.setMaxResults(1)
					.list();
			if (l != null && !l.isEmpty()) {
				return ((Bantuan) l.get(0)).getIsi();
			}
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:293");
			// abaikan → anggap belum ada (jatuh ke berkas HTML)
		} finally {
			HibernateUtil.closeSessionQuietly(s);
		}
		return null;
	}

	/** Simpan/perbarui isi panduan ke tabel Bantuan (upsert berdasarkan kunci). */
	public static boolean simpanKeTabel(String key, String isi) {
		String k = normalKunci(key);
		if (k == null) {
			return false;
		}
		// Isi katalog ikut berubah, sementara berkas di disk tidak; buang indeks.
		invalidasiIndeksKatalog();
		Session s = null;
		Transaction tx = null;
		try {
			s = HibernateUtil.openSession();
			tx = s.beginTransaction();
			List<?> l = s.createCriteria(Bantuan.class)
					.add(Restrictions.eq("kunci", k))
					.setMaxResults(1)
					.list();
			Bantuan b = (l != null && !l.isEmpty()) ? (Bantuan) l.get(0) : new Bantuan();
			if (b.getId() == null) {
				b.setKunci(k);
			}
			b.setIsi(isi == null ? "" : isi);
			b.setWaktuUbah(new java.util.Date());
			try {
				Tbmuser u = Common.getCurrentUser();
				if (u != null) {
					b.setOlehId(u.getId() == null ? null : String.valueOf(u.getId()));
					b.setOleh(u.getNama());
				}
			} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:328");
			}
			s.saveOrUpdate(b);
			tx.commit();
			return true;
		} catch (Throwable t) {
			if (tx != null) {
				try {
					tx.rollback();
				} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:337");
				}
			}
			return false;
		} finally {
			HibernateUtil.closeSessionQuietly(s);
		}
	}

	/** Hapus baris tabel Bantuan untuk kunci ini → panduan kembali memakai berkas HTML bawaan. */
	public static boolean hapusDariTabel(String key) {
		String k = normalKunci(key);
		if (k == null) {
			return false;
		}
		// Panduan kembali ke isi bawaan; indeks lama tidak berlaku lagi.
		invalidasiIndeksKatalog();
		Session s = null;
		Transaction tx = null;
		try {
			s = HibernateUtil.openSession();
			tx = s.beginTransaction();
			int n = s.createQuery("delete from Bantuan where kunci = :k")
					.setString("k", k)
					.executeUpdate();
			tx.commit();
			return n >= 0;
		} catch (Throwable t) {
			if (tx != null) {
				try {
					tx.rollback();
				} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:366");
				}
			}
			return false;
		} finally {
			HibernateUtil.closeSessionQuietly(s);
		}
	}

	/** Apakah pengguna saat ini berhak menyunting panduan (administrator). */
	private static boolean bolehEditBantuan() {
		try {
			ais.database.model.Tbmuser u = Common.getCurrentUser();
			if (u == null) {
				return false;
			}
			ais.database.model.Tbmrole r = u.hakAkses();
			return r != null && r.getRoleId() != null
					&& r.getRoleId().equals(ais.database.model.Tbmrole.ADMINISTRATOR);
		} catch (Throwable t) {
			return false;
		}
	}

	/**
	 * Bagikan konten panduan <b>ad-hoc</b> yang dibangun langsung di Java (mis. panel
	 * e-Learning yang tidak memiliki berkas HTML). Isi diterbitkan ke tabel Bantuan dengan
	 * kunci turunan judul ({@code panel_<slug>}) agar dapat diakses publik tanpa login,
	 * lalu tautannya ditampilkan.
	 */
	public static void bagikanKonten(String judul, String htmlKonten) {
		try {
			String slug = judul == null ? "" : judul.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_");
			slug = slug.replaceAll("^_+", "").replaceAll("_+$", "");
			if (slug.length() == 0) {
				slug = "umum";
			}
			String key = "panel_" + slug;
			simpanKeTabel(key, htmlKonten);
			bukaShare(key, judul);
		} catch (Throwable t) {
			try {
				Common.tampilErrorJikaAdmin(t instanceof Exception ? (Exception) t : new Exception(t));
				PesanFormalHelper.tampilkanGagalException("berbagi (share) konten Bantuan",
						t,
						new String[] {
								"Coba ulangi proses berbagi konten beberapa saat lagi.",
								"Pastikan konten Bantuan yang akan dibagikan sudah tersimpan dengan benar.",
								"Bila kegagalan berulang, laporkan ke Administrator/pengembang disertai tangkapan layar (screenshot) pesan ini."
						});
			} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:409");
			}
		}
	}

	/**
	 * Tambah satu pilihan bahasa pada menu Terjemah. Saat diklik, isi panduan diterjemahkan
	 * dari teks sumber Indonesia ({@code baseHolder}) ke {@code token} lalu ditampilkan ulang
	 * pada komponen {@code htmlHolder}. Token: {@code id} (asli), {@code english}, {@code arab},
	 * {@code mandarin}. Memakai {@code KamusBahasaInternal.terjemahHtml} yang sadar-tag.
	 */
	private static void tambahItemTerjemah(org.zkoss.zul.Menupopup pop, String label, String flag,
			final String token, final org.zkoss.zul.Html[] htmlHolder, final String[] baseHolder,
			final boolean pakaiAi) {
		org.zkoss.zul.Menuitem it = new org.zkoss.zul.Menuitem(label);
		try {
			it.setImage("/component/assets/media/flags/" + flag + ".svg");
		} catch (Throwable ignore) {
		}
		it.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				if (htmlHolder[0] == null) {
					return;
				}
				final String base = baseHolder[0] == null ? "" : baseHolder[0];
				// Bahasa asal → tampilkan apa adanya (tanpa terjemah).
				if ("id".equals(token)) {
					htmlHolder[0].setContent(base);
					return;
				}
				final String namaBahasa = "english".equals(token) ? "English"
						: ("arab".equals(token) || (token != null && token.indexOf("arab") >= 0) ? "Arab (Arabic)"
								: "Mandarin (中文)");
				// "Terjemahkan Cepat" (pakaiAi=false) ATAU AI tidak terkoneksi → kamus internal (instan).
				if (!pakaiAi || !GenerateAiHelper.siap()) {
					String out;
					try {
						out = ais.common.KamusBahasaInternal.terjemahHtml(base, token);
					} catch (Throwable t) {
						out = base;
					}
					if (out == null) {
						out = "";
					}
					if (token != null && token.indexOf("arab") >= 0) {
						out = "<div dir='rtl' style='text-align:right;'>" + out + "</div>";
					}
					htmlHolder[0].setContent(out);
					return;
				}
				// Terjemah pakai AI Ollama (kualitas lebih baik, sama seperti LabelBahasaAction) dengan popup
				// progres streaming; bila AI gagal/kosong → fallback kamus internal HTML-aware.
				// PEMBELAJARAN: bila panduan ini sudah pernah diterjemahkan AI, pakai hasil tersimpan
				// (tanpa memanggil AI lagi; tetap ada setelah restart).
				try {
					String tjTersimpan = TerjemahAiHelper.ambilDoc(base, token);
					if (tjTersimpan != null && tjTersimpan.trim().length() > 0) {
						String out = tjTersimpan;
						if (token != null && token.indexOf("arab") >= 0) {
							out = "<div dir='rtl' style='text-align:right;'>" + out + "</div>";
						}
						htmlHolder[0].setContent(out);
						return;
					}
				} catch (Throwable ignore) {
				}

				StringBuilder prompt = new StringBuilder();
				prompt.append("Terjemahkan KONTEN HTML panduan berikut ke bahasa ").append(namaBahasa).append(". ");
				prompt.append("PERTAHANKAN seluruh tag & atribut HTML persis apa adanya; terjemahkan HANYA teks ");
				prompt.append("yang terlihat pengguna. Jangan tambahkan penjelasan/markdown/pembungkus. ");
				prompt.append("Kembalikan HANYA HTML hasil terjemahan.\n\n").append(base);
				GenerateAiHelper.jalankanAiStreaming(
						Common.getBahasaConfig("Terjemahkan Panduan") + " — " + namaBahasa, prompt.toString(),
						new GenerateAiHelper.HasilAi() {
							@Override
							public void selesai(String resp) throws Exception {
								String out = resp == null ? "" : resp.trim();
								// Buang pembungkus code-fence bila ada.
								out = out.replaceAll("^```[a-zA-Z]*", "").replaceAll("```$", "").trim();
								boolean dariAi = out.length() > 0;
								if (out.length() == 0) {
									try {
										out = ais.common.KamusBahasaInternal.terjemahHtml(base, token);
									} catch (Throwable t) {
										out = base;
									}
								}
								if (out == null) {
									out = "";
								}
								// PEMBELAJARAN: simpan hasil AI (natural) agar dipakai ulang & bertahan setelah restart.
								if (dariAi && out.length() > 0) {
									try {
										TerjemahAiHelper.simpanDoc(base, token, out);
									} catch (Throwable ignore) {
									}
								}
								if (token != null && token.indexOf("arab") >= 0) {
									out = "<div dir='rtl' style='text-align:right;'>" + out + "</div>";
								}
								htmlHolder[0].setContent(out);
							}
						}, 4096);
			}
		});
		it.setParent(pop);
	}

	/**
	 * Bangun URL <b>lengkap</b> (skema://host[:port]/context + pathQuery) dari request aktif,
	 * menghormati header proxy {@code X-Forwarded-Proto}/{@code X-Forwarded-Host}. Bila request
	 * tak tersedia, kembalikan bentuk relatif (context + pathQuery) sebagai cadangan.
	 */
	private static String urlPublik(String pathQuery) {
		String ctx = null;
		String base = "";
		try {
			org.zkoss.zk.ui.Execution ex = org.zkoss.zk.ui.Executions.getCurrent();
			if (ex != null) {
				try {
					ctx = ex.getContextPath();
				} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:427");
				}
				Object nr = ex.getNativeRequest();
				if (nr instanceof javax.servlet.http.HttpServletRequest) {
					javax.servlet.http.HttpServletRequest r = (javax.servlet.http.HttpServletRequest) nr;

					String proto = r.getHeader("X-Forwarded-Proto");
					if (proto == null || proto.trim().length() == 0) {
						proto = r.getScheme();
					}
					proto = proto.trim().toLowerCase();

					String host = r.getHeader("X-Forwarded-Host");
					if (host != null) {
						int comma = host.indexOf(',');
						if (comma >= 0) {
							host = host.substring(0, comma);
						}
						host = host.trim();
					}
					if (host == null || host.length() == 0) {
						host = r.getServerName();
						int port = r.getServerPort();
						boolean baku = ("http".equals(proto) && port == 80)
								|| ("https".equals(proto) && port == 443);
						if (port > 0 && !baku) {
							host = host + ":" + port;
						}
					}

					if (ctx == null) {
						ctx = r.getContextPath();
					}
					base = proto + "://" + host;
				}
			}
		} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:463");
		}
		return base + (ctx == null ? "" : ctx) + pathQuery;
	}

	/**
	 * Tampilkan tautan publik panduan (endpoint servlet {@code /bantuan?key=<kunci>}) yang
	 * dapat dibuka oleh siapa saja <b>tanpa login</b>. Menyediakan tombol Salin dan Buka.
	 */
	private static void bukaShare(String key, String judul) {
		try {
			String k = normalKunci(key);
			if (k == null) {
				return;
			}
			final String url = urlPublik("/bantuan?key=" + k);

			final MyWindow w = new MyWindow("Bagikan Panduan", "normal", true);
			w.setWidth("540px");
			w.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

			Div box = new Div();
			box.setStyle("padding:16px 18px;");
			box.setParent(w);

			new Html("<div style='font-size:13px;color:#0f172a;margin:0 0 4px;'>"
					+ (judul == null || judul.trim().isEmpty() ? "" : "<b>" + esc(judul) + "</b>")
					+ "</div><div style='font-size:12px;color:#475569;margin:6px 0 10px;'>"
					+ "Tautan berikut dapat dibuka oleh <b>siapa saja tanpa perlu login</b>. "
					+ "Salin lalu bagikan.</div>").setParent(box);

			final Textbox tb = new Textbox(url);
			tb.setReadonly(true);
			tb.setWidth("100%");
			tb.setStyle("padding:8px 10px;border:1px solid #cbd5e1;border-radius:8px;font-size:12px;"
					+ "box-sizing:border-box;");
			tb.setParent(box);

			Toolbar bar = new Toolbar();
			bar.setStyle("margin-top:12px;");
			bar.setParent(box);

			final String js = toJsString(url);

			MyToolbarbuttonConfig salin = new MyToolbarbuttonConfig("Salin", "/img/svg/copy.svg");
			salin.setTooltiptext("Salin tautan ke papan klip");
			salin.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					Clients.evalJavaScript("(function(){var t=" + js + ";"
							+ "if(navigator.clipboard&&navigator.clipboard.writeText){navigator.clipboard.writeText(t);}"
							+ "})();");
				}
			});
			salin.setParent(bar);

			MyToolbarbuttonConfig buka = new MyToolbarbuttonConfig("Buka Tautan", "/img/svg/send.svg");
			buka.setTooltiptext("Buka tautan panduan di tab baru");
			buka.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					Clients.evalJavaScript("window.open(" + js + ",'_blank');");
				}
			});
			buka.setParent(bar);

			MyToolbarbuttonConfig tutup = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
			tutup.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					w.detach();
				}
			});
			tutup.setParent(bar);

			w.setVisible(true);
			w.onModal();
		} catch (Throwable t) {
			try {
				Common.tampilErrorJikaAdmin(t instanceof Exception ? (Exception) t : new Exception(t));
				PesanFormalHelper.tampilkanGagalException("pembukaan jendela berbagi konten Bantuan",
						t,
						new String[] {
								"Coba ulangi proses berbagi konten beberapa saat lagi.",
								"Muat ulang (refresh) halaman ini, lalu ulangi.",
								"Bila kegagalan berulang, laporkan ke Administrator/pengembang disertai tangkapan layar (screenshot) pesan ini."
						});
			} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:543");
			}
		}
	}

	/**
	 * Buka penyunting isi panduan (HTML). Simpan menyimpan ke tabel Bantuan; Reset menghapusnya
	 * (kembali ke berkas HTML bawaan). Setelah aksi, jendela panduan dibuka ulang.
	 */
	private static void bukaEditor(final Component self, final String judul, final String key, final MyWindow parent) {
		try {
			final MyWindow ed = new MyWindow("Ubah Panduan — " + (judul == null || judul.trim().isEmpty() ? key : judul),
					"normal", true);
			ed.setWidth("70%");
			ed.setHeight("78%");
			ed.setMaximizable(true);
			ed.setStyle("overflow:hidden;");
			ed.setContentStyle("overflow:hidden;padding:0;background:#f6f8fb;");
			ed.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

			Div box = new Div();
			box.setWidth("100%");
			box.setHeight("100%");
			box.setStyle("display:flex;flex-direction:column;height:100%;box-sizing:border-box;");
			box.setParent(ed);

			Html note = new Html("<div style='flex:0 0 auto;padding:8px 14px;background:#fef9c3;"
					+ "border-bottom:1px solid #fde68a;font-size:12px;color:#713f12;'>"
					+ "Sunting panduan untuk kunci <b>" + esc(key) + "</b> memakai penyunting visual di bawah "
					+ "(atur teks, judul, daftar, tabel, dan tautan tanpa menulis HTML manual). "
					+ "<b>Simpan</b> menyimpan perubahan ke basis data (berlaku langsung tanpa deploy ulang). "
					+ "<b>Reset ke Bawaan</b> menghapus perubahan dan mengembalikan panduan ke berkas HTML asli.</div>");
			note.setParent(box);

			// Penyunting visual (WYSIWYG) MyCkEditor agar panduan mudah diolah tanpa menulis HTML manual.
			Div editWrap = new Div();
			editWrap.setStyle("flex:1 1 auto;min-height:0;overflow:auto;background:#ffffff;padding:8px;"
					+ "box-sizing:border-box;");
			editWrap.setParent(box);

			final ais.ui.util.MyCkEditor area = new ais.ui.util.MyCkEditor();
			area.setWidth("100%");
			area.setHeight("100%");
			String isi = muatKontenFile(key);
			area.setValue(isi == null ? "" : isi);
			area.setParent(editWrap);

			Toolbar bar = new Toolbar();
			bar.setStyle("flex:0 0 auto;background:#eef2f7;border-top:1px solid #d7dee8;");
			bar.setParent(box);

			MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
			simpan.setTooltiptext("Simpan perubahan panduan");
			simpan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					if (simpanKontenFile(key, area.getValue())) {
						ed.detach();
						if (parent != null) {
							try {
								parent.detach();
							} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:604");
							}
						}
						tampilkanDariResource(self, key, judul);
					} else {
						try {
							MyMessageboxConfig.show(
									"Mohon maaf, panduan gagal disimpan. Langkah yang dapat dilakukan: (1) periksa hak akses tulis pada berkas panduan; (2) pastikan Anda memiliki wewenang administrator; (3) coba simpan kembali atau hubungi administrator sistem apabila kendala berlanjut.",
									"Bantuan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:613");
						}
					}
				}
			});
			simpan.setParent(bar);

			MyToolbarbuttonConfig reset = new MyToolbarbuttonConfig("Reset ke Bawaan", "/img/svg/refresh.svg");
			reset.setTooltiptext("Hapus perubahan dan kembalikan ke panduan bawaan (berkas HTML)");
			reset.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					MyMessageboxConfig.show(
							"Apakah Anda yakin ingin mengembalikan panduan ini ke versi bawaan? Perlu diperhatikan, seluruh perubahan panduan yang telah tersimpan akan dihapus dan tidak dapat dikembalikan. Tekan Ya untuk melanjutkan atau Tidak untuk membatalkan.",
							"Reset Panduan", MyMessageboxConfig.YES | MyMessageboxConfig.NO,
							MyMessageboxConfig.QUESTION, new EventListener() {
								@Override
								public void onEvent(Event ev) throws Exception {
									if (new Integer(MyMessageboxConfig.YES).equals(ev.getData())) {
										resetKeBawaan(key);
										ed.detach();
										if (parent != null) {
											try {
												parent.detach();
											} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:637");
											}
										}
										tampilkanDariResource(self, key, judul);
									}
								}
							});
				}
			});
			reset.setParent(bar);

			MyToolbarbuttonConfig batal = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
			batal.setTooltiptext("Batalkan penyuntingan");
			batal.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					ed.detach();
				}
			});
			batal.setParent(bar);

			ed.setVisible(true);
			ed.onModal();
		} catch (Throwable t) {
			try {
				Common.tampilErrorJikaAdmin(t instanceof Exception ? (Exception) t : new Exception(t));
				PesanFormalHelper.tampilkanGagalException("pembukaan editor konten Bantuan",
						t,
						new String[] {
								"Coba ulangi proses membuka editor beberapa saat lagi.",
								"Pastikan tidak ada konten Bantuan yang sedang diedit bersamaan oleh Administrator lain.",
								"Bila kegagalan berulang, laporkan ke Administrator/pengembang disertai tangkapan layar (screenshot) pesan ini."
						});
			} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:663");
			}
		}
	}

	/**
	 * Simpan hasil suntingan panduan ke <b>tabel Bantuan</b> (bukan menimpa berkas HTML
	 * bawaan). Berkas HTML dipertahankan sebagai versi bawaan yang dapat dipulihkan lewat
	 * tombol Reset. Khusus administrator.
	 */
	private static boolean simpanKontenFile(String key, String isi) {
		if (!bolehEditBantuan()) {
			return false;
		}
		return simpanKeTabel(key, isi);
	}

	/** Reset: hapus versi termodifikasi (tabel) → panduan kembali ke berkas HTML bawaan. Admin. */
	private static boolean resetKeBawaan(String key) {
		if (!bolehEditBantuan()) {
			return false;
		}
		return hapusDariTabel(key);
	}

	/**
	 * Bangun panduan otomatis dengan menelusuri komponen halaman: nama bagian/menu
	 * (tab), kolom data (Column), dan tombol/aksi (Toolbarbutton/Button), lalu
	 * ditutup dengan panduan umum cara kerja aplikasi. Dipakai untuk halaman yang
	 * belum memiliki berkas panduan khusus.
	 */
	private static String bangunOtomatis(Component self) {
		StringBuilder sb = new StringBuilder();
		sb.append("<h2 style='margin:0 0 8px;color:#1d4ed8;'>Panduan Penggunaan Halaman</h2>");
		sb.append("<p style='color:#475569;margin:0 0 12px;'>Ringkasan ini disusun otomatis dari isi halaman untuk "
				+ "membantu Anda mengenali bagian, kolom, dan tombol yang tersedia. Panduan rinci per-field akan "
				+ "ditambahkan secara bertahap.</p>");
		try {
			Component root = self;
			if (root != null) {
				while (root.getParent() != null) {
					root = root.getParent();
				}
			}
			java.util.List<String> tabs = new java.util.ArrayList<String>();
			java.util.List<String> kolom = new java.util.ArrayList<String>();
			java.util.List<String> tombol = new java.util.ArrayList<String>();
			kumpulkan(root, tabs, kolom, tombol, 0);

			if (!tabs.isEmpty()) {
				sb.append("<h3 style='color:#0f172a;border-bottom:2px solid #e2e8f0;padding-bottom:4px;'>"
						+ "Bagian / Menu Halaman</h3><p>Halaman ini terbagi atas bagian berikut; klik judulnya untuk "
						+ "berpindah:</p><ul>");
				for (String t : tabs) {
					sb.append("<li><b>").append(esc(t)).append("</b></li>");
				}
				sb.append("</ul>");
			}
			if (!kolom.isEmpty()) {
				sb.append("<h3 style='color:#0f172a;border-bottom:2px solid #e2e8f0;padding-bottom:4px;'>"
						+ "Kolom Data</h3><p>Tabel data menampilkan kolom-kolom berikut. Setiap kolom memuat informasi "
						+ "sesuai namanya; kolom <i>Aksi</i> berisi tombol Ubah/Copy/Hapus untuk baris tersebut:</p><ul>");
				for (String k : kolom) {
					sb.append("<li><b>").append(esc(k)).append("</b></li>");
				}
				sb.append("</ul>");
			}
			if (!tombol.isEmpty()) {
				sb.append("<h3 style='color:#0f172a;border-bottom:2px solid #e2e8f0;padding-bottom:4px;'>"
						+ "Tombol / Aksi</h3><ul>");
				for (String b : tombol) {
					sb.append("<li><b>").append(esc(b)).append("</b></li>");
				}
				sb.append("</ul>");
			}
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:738");
			// abaikan → tetap tampilkan panduan umum di bawah
		}
		sb.append(panduanUmum());
		return sb.toString();
	}

	private static void kumpulkan(Component c, java.util.List<String> tabs, java.util.List<String> kolom,
			java.util.List<String> tombol, int depth) {
		if (c == null || depth > 60) {
			return;
		}
		try {
			if (c instanceof org.zkoss.zul.Tab) {
				tambahUnik(tabs, ((org.zkoss.zul.Tab) c).getLabel());
			} else if (c instanceof org.zkoss.zul.Column) {
				tambahUnik(kolom, ((org.zkoss.zul.Column) c).getLabel());
			} else if (c instanceof org.zkoss.zul.Toolbarbutton) {
				tambahUnik(tombol, ((org.zkoss.zul.Toolbarbutton) c).getLabel());
			} else if (c instanceof org.zkoss.zul.Button) {
				tambahUnik(tombol, ((org.zkoss.zul.Button) c).getLabel());
			}
			for (Object anak : c.getChildren()) {
				if (anak instanceof Component) {
					kumpulkan((Component) anak, tabs, kolom, tombol, depth + 1);
				}
			}
		} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:765");
		}
	}

	private static void tambahUnik(java.util.List<String> daftar, String nilai) {
		if (nilai == null) {
			return;
		}
		String v = nilai.trim();
		if (v.isEmpty() || "Bantuan".equalsIgnoreCase(v) || daftar.contains(v) || daftar.size() >= 60) {
			return;
		}
		daftar.add(v);
	}

	private static String esc(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	/**
	 * Panduan umum bawaan (±1.000 kata) untuk halaman yang belum memiliki berkas panduan khusus.
	 * Disusun dengan bahasa sederhana agar mudah dipahami pengguna yang sama sekali tidak memahami
	 * dunia teknologi informasi. Digabung dengan ringkasan struktur halaman (tab/kolom/tombol) pada
	 * {@link #bangunOtomatis(Component)} sehingga setiap panduan otomatis tetap lengkap dan panjang.
	 */
	private static String panduanUmum() {
		StringBuilder s = new StringBuilder(6000);
		String h3 = "<h3 style='color:#0f172a;border-bottom:2px solid #e2e8f0;padding-bottom:4px;margin:18px 0 8px;'>";
		String tip = "<div style='background:#eff6ff;border-left:4px solid #3b82f6;border-radius:6px;"
				+ "padding:8px 12px;margin:10px 0;color:#1e3a8a;'>";

		s.append("<h2 style='margin:0 0 8px;color:#1d4ed8;'>Panduan Penggunaan Halaman</h2>");
		s.append("<p>Selamat datang. Panduan ini menjelaskan dengan bahasa yang sesederhana mungkin cara memakai "
				+ "halaman ini, arti setiap bagian yang Anda lihat di layar, serta apa fungsi tombol dan kolom yang "
				+ "tersedia. Anda tidak perlu memahami istilah teknis apa pun untuk mengikutinya. Bacalah sekali dari "
				+ "atas ke bawah, lalu coba langsung sambil melihat layar Anda.</p>");

		s.append(h3).append("Gambaran Umum</h3>");
		s.append("<p>Sebagian besar halaman pada aplikasi ini bekerja dengan pola yang sama. Di bagian atas terdapat "
				+ "kotak <b>Pencarian &amp; Filter</b> untuk mencari data. Di bawahnya terdapat <b>tabel/daftar data</b> "
				+ "yang menampilkan informasi yang Anda cari. Setiap baris pada tabel mewakili satu data (misalnya satu "
				+ "orang, satu mata kuliah, atau satu transaksi). Pada ujung kanan setiap baris biasanya ada beberapa "
				+ "tombol kecil untuk mengelola data baris tersebut. Karena polanya seragam, begitu Anda terbiasa dengan "
				+ "satu halaman, halaman lain akan terasa mudah.</p>");

		s.append(h3).append("Cara Mencari Data</h3>");
		s.append("<p>Bila data sangat banyak, Anda tidak perlu menggulir satu per satu. Gunakan kotak <b>Pencarian "
				+ "&amp; Filter</b> di atas. Caranya: ketik kata kunci pada kolom yang sesuai (misalnya nama atau kode), "
				+ "atau pilih pilihan pada kotak pilihan (combobox), lalu tekan tombol <b>Cari</b>. Tabel akan langsung "
				+ "menampilkan hanya data yang cocok. Anda boleh mengisi satu kolom saja, atau beberapa kolom sekaligus "
				+ "untuk pencarian yang lebih sempit. Untuk mengosongkan pencarian dan menampilkan semua data kembali, "
				+ "kosongkan kolom filter lalu tekan <b>Cari</b> sekali lagi.</p>");
		s.append("<p>Jika halaman memiliki tombol <b>Pencarian Lanjut</b>, tombol itu menampilkan pilihan saringan "
				+ "tambahan untuk pencarian yang lebih rinci. Pada banyak halaman, semua saringan kini sudah tampil "
				+ "langsung sehingga Anda cukup mengisinya tanpa membuka jendela tambahan.</p>");
		s.append(tip).append("<b>Tips:</b> Pencarian biasanya tidak harus kata yang persis sama — mengetik sebagian "
				+ "nama saja sudah cukup untuk menemukan data yang Anda maksud.</div>");

		s.append(h3).append("Memahami Kolom pada Tabel</h3>");
		s.append("<p>Setiap kolom pada tabel memuat satu jenis informasi sesuai dengan judul kolomnya. Misalnya kolom "
				+ "<b>Kode</b> berisi kode, kolom <b>Nama</b> berisi nama, dan seterusnya. Beberapa kolom yang sering "
				+ "muncul dan perlu Anda kenali:</p>");
		s.append("<ul>");
		s.append("<li><b>Aktif</b> — penanda apakah data masih digunakan. Bila dicentang berarti data aktif; bila "
				+ "tidak, data dianggap nonaktif/arsip dan biasanya tidak ikut dipakai pada proses lain.</li>");
		s.append("<li><b>Keterangan</b> — catatan tambahan atau penjelasan singkat mengenai data pada baris itu.</li>");
		s.append("<li><b>Aksi</b> (kolom paling kanan) — berisi tombol-tombol untuk mengelola baris tersebut, "
				+ "yaitu Ubah, Copy, dan Hapus. Penjelasannya ada di bagian berikut.</li>");
		s.append("</ul>");
		s.append("<p>Pada beberapa judul kolom terdapat tanda panah kecil; mengklik judul tersebut akan mengurutkan "
				+ "data berdasarkan kolom itu (menaik atau menurun), sehingga memudahkan Anda menemukan data.</p>");

		s.append(h3).append("Menambah Data Baru</h3>");
		s.append("<p>Untuk memasukkan data baru, tekan tombol <b>Tambah</b> (biasanya bertanda plus). Sebuah formulir "
				+ "akan terbuka. Isilah kolom-kolom yang diminta. Kolom yang diberi tanda bintang (<b>*</b>) wajib diisi "
				+ "dan tidak boleh dikosongkan. Setelah lengkap, tekan tombol <b>Simpan</b>. Bila ada isian yang keliru "
				+ "atau belum lengkap, aplikasi akan memberi tahu Anda agar dapat diperbaiki sebelum tersimpan.</p>");

		s.append(h3).append("Mengubah, Menggandakan, dan Menghapus</h3>");
		s.append("<p>Pada kolom <b>Aksi</b> di setiap baris terdapat tiga tombol utama:</p>");
		s.append("<ul>");
		s.append("<li><b>Ubah</b> (ikon pensil) — membuka kembali data baris tersebut untuk Anda perbaiki, lalu "
				+ "simpan perubahannya.</li>");
		s.append("<li><b>Copy</b> (ikon dua lembar) — menggandakan data baris menjadi dasar data baru. Berguna bila "
				+ "Anda ingin membuat data yang mirip; Anda tinggal mengubah bagian yang berbeda lalu menyimpannya "
				+ "sebagai data baru tanpa mengetik ulang semuanya.</li>");
		s.append("<li><b>Hapus</b> (ikon tempat sampah) — menghapus data baris tersebut. Aplikasi akan meminta "
				+ "konfirmasi terlebih dahulu.</li>");
		s.append("</ul>");
		s.append("<div style='background:#fef2f2;border-left:4px solid #ef4444;border-radius:6px;padding:8px 12px;"
				+ "margin:10px 0;color:#991b1b;'><b>Perhatian:</b> Penghapusan bersifat permanen dan sulit dikembalikan. "
				+ "Pastikan Anda benar-benar memilih baris yang tepat sebelum menekan Hapus.</div>");

		s.append(h3).append("Berpindah Halaman (Navigasi)</h3>");
		s.append("<p>Bila data lebih dari satu halaman, di bagian bawah daftar tersedia nomor-nomor halaman beserta "
				+ "tombol berpindah (sebelumnya/berikutnya). Klik nomor halaman untuk melompat, atau gunakan tombol "
				+ "berikutnya untuk maju selangkah. Mengubah pencarian akan mengembalikan tampilan ke halaman pertama.</p>");

		s.append(h3).append("Tampilan di Ponsel dan Komputer</h3>");
		s.append("<p>Halaman dirancang menyesuaikan ukuran layar. Di komputer, tabel tampil penuh. Di ponsel, tabel "
				+ "dapat digeser ke samping untuk melihat kolom yang tidak muat, dan tombol dibuat cukup besar agar nyaman "
				+ "disentuh. Anda tidak perlu pengaturan khusus — cukup buka halaman seperti biasa.</p>");

		s.append(h3).append("Bila Terjadi Masalah</h3>");
		s.append("<p>Jika data yang Anda cari tidak muncul, periksa kembali kata kunci pencarian — mungkin terlalu "
				+ "sempit; coba kosongkan sebagian filter lalu tekan <b>Cari</b> lagi. Jika tampilan terasa belum "
				+ "diperbarui, muat ulang halaman. Bila Anda menemui pesan kesalahan atau ragu terhadap suatu data, "
				+ "sebaiknya hentikan dan tanyakan kepada administrator daripada menebak, terutama sebelum menghapus "
				+ "atau menyimpan data penting.</p>");

		s.append("<p style='margin-top:14px;color:#475569;'><i>Panduan rinci khusus untuk halaman ini dapat ditambahkan "
				+ "kemudian oleh administrator. Ringkasan bagian, kolom, dan tombol di atas (bila ada) diambil langsung "
				+ "dari isi halaman yang sedang Anda buka.</i></p>");
		return s.toString();
	}

	// =====================================================================
	// KATALOG BANTUAN
	//
	// Menampilkan daftar (katalog) seluruh panduan halaman yang telah disiapkan
	// di folder WEB-INF/bantuan, DISARING menurut hak akses pengguna: hanya
	// panduan untuk halaman yang menu-nya dimiliki oleh peran pengguna
	// (tbmuser.hakAkses().getMenus()) yang ditampilkan. Tersedia kotak pencarian
	// yang menyaring berdasarkan judul maupun ISI panduan.
	// =====================================================================

	/** Wadah ringkas satu entri katalog: kunci berkas, label menu, judul, teks
	 *  (untuk pencarian), dan cuplikan (untuk tampilan). */
	private static class Entri {
		String key;
		String label;
		String judul;
		String teksLower;
		String excerpt;
		String kategori;
		String tanggal;
		int buka;
	}

	/**
	 * Tampilkan jendela katalog bantuan yang berisi daftar panduan sesuai hak akses
	 * pengguna, lengkap dengan kotak pencarian judul/isi. Setiap entri dapat diklik
	 * untuk membuka panduan halaman terkait.
	 *
	 * @param self komponen pemicu (boleh null) untuk konteks halaman
	 */
	public static void tampilkanKatalog(final Component self) {
		try {
			final List<Entri> semua = kumpulkanEntriBantuan();

			final MyWindow window = new MyWindow("Katalog Bantuan", "normal", true);
			window.setWidth("82%");
			window.setHeight("88%");
			window.setMaximizable(true);
			window.setStyle("overflow:hidden;");
			window.setContentStyle("overflow:hidden;padding:0;background:#f6f8fb;");
			window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

			Div box = new Div();
			box.setWidth("100%");
			box.setHeight("100%");
			box.setStyle("display:flex;flex-direction:column;height:100%;box-sizing:border-box;");
			box.setParent(window);

			// Gaya kartu (termasuk efek hover yang tidak bisa dilakukan lewat style inline).
			Html gaya = new Html("<style>"
					+ ".kb-card{background:#fff;border:1px solid #e2e8f0;border-radius:10px;padding:12px 14px;"
					+ "margin:0 0 10px;cursor:pointer;transition:box-shadow .15s,border-color .15s;}"
					+ ".kb-card:hover{border-color:#93c5fd;box-shadow:0 4px 14px rgba(37,99,235,.12);}"
					+ ".kb-title{color:#1d4ed8;font-weight:600;font-size:14px;margin:0 0 3px;}"
					+ ".kb-meta{color:#64748b;font-size:11px;margin:0 0 6px;}"
					+ ".kb-ex{color:#475569;font-size:12px;line-height:1.55;}"
					+ ".kb-cetak:hover{background:#eff6ff !important;border-color:#93c5fd !important;}"
					+ ".kb-grup{margin:18px 0 8px;font-size:12px;font-weight:700;color:#0f172a;"
					+ "letter-spacing:.03em;text-transform:uppercase;border-left:3px solid #1d4ed8;padding-left:8px;}"
					+ ".kb-grup:first-child{margin-top:2px;}"
					+ ".kb-grup-n{color:#94a3b8;font-weight:600;}"
					+ "</style>");
			gaya.setParent(box);

			// Kepala: judul + keterangan + kotak pencarian.
			Div header = new Div();
			header.setWidth("100%");
			header.setStyle("flex:0 0 auto;background:#eef2f7;border-bottom:1px solid #d7dee8;padding:12px 16px;"
					+ "box-sizing:border-box;");
			header.setParent(box);

			// Baris atas: judul di kiri, tombol Cetak (global) di kanan.
			Div headerTop = new Div();
			headerTop.setWidth("100%");
			headerTop.setStyle("display:flex;justify-content:space-between;align-items:flex-start;gap:12px;"
					+ "box-sizing:border-box;");
			headerTop.setParent(header);

			// Pembungkus judul: tumbuh mengisi ruang (min-width:0 agar teks membungkus normal,
			// bukan mengecil ke lebar satu kata).
			Div titleWrap = new Div();
			titleWrap.setStyle("flex:1 1 auto;min-width:0;");
			titleWrap.setParent(headerTop);

			Html judulHtml = new Html("<div style='color:#0f172a;font-size:16px;font-weight:700;margin:0 0 2px;'>"
					+ "Katalog Bantuan</div>"
					+ "<div style='color:#64748b;font-size:12px;margin:0 0 10px;'>"
					+ "Daftar panduan penggunaan halaman yang sesuai dengan hak akses Anda. "
					+ "Ketik kata kunci untuk mencari berdasarkan judul maupun isi panduan.</div>");
			judulHtml.setParent(titleWrap);

			// Tombol Cetak diletakkan LANGSUNG (tanpa pembungkus Toolbar) agar tidak terkena
			// gaya tema ".z-toolbar" (gradient/border/box-shadow/min-height 46px) yang memakai
			// !important sehingga tak bisa ditimpa inline. Dibuat sebagai Div bergaya tombol
			// yang sepenuhnya kita kendalikan.
			final Div cetakSemua = new Div();
			cetakSemua.setSclass("kb-cetak");
			cetakSemua.setStyle("flex:0 0 auto;white-space:nowrap;cursor:pointer;display:inline-flex;"
					+ "align-items:center;gap:6px;padding:7px 13px;border:1px solid #cbd5e1;border-radius:8px;"
					+ "background:#ffffff;color:#1d4ed8;font-size:13px;font-weight:600;line-height:1;");
			cetakSemua.setTooltiptext("Cetak daftar panduan yang sedang ditampilkan");
			new Html("<span style='font-size:14px;'>&#128424;</span><span>Cetak</span>").setParent(cetakSemua);
			cetakSemua.setParent(headerTop);

			final Textbox cari = new Textbox();
			cari.setWidth("100%");
			cari.setStyle("padding:9px 12px;border:1px solid #cbd5e1;border-radius:8px;font-size:13px;box-sizing:border-box;");
			cari.setParent(header);

			final Label info = new Label();
			info.setStyle("display:block;color:#64748b;font-size:11px;margin:8px 0 0;");
			info.setParent(header);

			// Area gulir daftar panduan.
			Div scroll = new Div();
			scroll.setWidth("100%");
			scroll.setStyle("flex:1 1 auto;min-height:0;overflow-y:auto;overflow-x:hidden;background:#f6f8fb;"
					+ "max-height:calc(88vh - 150px);-webkit-overflow-scrolling:touch;");
			scroll.setParent(box);

			final Div daftar = new Div();
			daftar.setStyle("max-width:900px;margin:0 auto;padding:16px;");
			daftar.setParent(scroll);

			renderDaftar(self, daftar, semua, "", info);

			cari.addEventListener("onChanging", new EventListener() {
				@Override
				public void onEvent(Event ev) throws Exception {
					String q = ev instanceof InputEvent ? ((InputEvent) ev).getValue() : cari.getValue();
					renderDaftar(self, daftar, semua, q, info);
				}
			});
			cari.addEventListener("onOK", new EventListener() {
				@Override
				public void onEvent(Event ev) throws Exception {
					renderDaftar(self, daftar, semua, cari.getValue(), info);
				}
			});

			cetakSemua.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event ev) throws Exception {
					cetakKatalog(semua, cari.getValue());
				}
			});

			window.setVisible(true);
			window.onModal();
		} catch (Throwable t) {
			try {
				Common.tampilErrorJikaAdmin(t instanceof Exception ? (Exception) t : new Exception(t));
				PesanFormalHelper.tampilkanGagalException("penampilan katalog menu Bantuan",
						t,
						new String[] {
								"Muat ulang (refresh) halaman ini, lalu buka kembali menu Bantuan.",
								"Coba ulangi beberapa saat lagi.",
								"Bila kegagalan berulang, laporkan ke Administrator/pengembang disertai tangkapan layar (screenshot) pesan ini."
						});
			} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:1036");
			}
		}
	}

	/** Urutan tampil kelompok/modul dalam katalog. */
	private static final String[] KATEGORI_URUT = {
			"Kemahasiswaan", "Perkuliahan", "Kurikulum & OBE", "Ujian",
			"Keuangan & Pembayaran", "Kepegawaian", "Absensi", "Surat & SOP",
			"Aset & Pengadaan", "Kantin, Koperasi & Persediaan", "Sekolah", "Alumni & Tracer",
			"Biodata", "Laporan", "Transaksi Akademik", "Konfigurasi", "Master & Lainnya"
	};

	private static int kategoriRank(String k) {
		for (int i = 0; i < KATEGORI_URUT.length; i++) {
			if (KATEGORI_URUT[i].equals(k)) {
				return i;
			}
		}
		return KATEGORI_URUT.length;
	}

	/**
	 * Gambar ulang daftar panduan: disaring kata kunci (judul/isi), <b>dikelompokkan
	 * per modul</b> dengan tajuk kelompok, di dalam kelompok diurutkan berdasarkan
	 * jumlah buka (terpopuler dulu) lalu judul. Tiap kartu menampilkan tanggal
	 * pembaruan dan berapa kali panduan itu dibuka.
	 */
	private static void renderDaftar(final Component self, Div daftar, List<Entri> semua, String q, Label info) {
		try {
			daftar.getChildren().clear();
		} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:1067");
		}
		String qq = q == null ? "" : q.trim().toLowerCase();

		List<Entri> tampil = new ArrayList<Entri>();
		for (Entri e : semua) {
			if (e == null) {
				continue;
			}
			if (qq.length() > 0 && !cocokPencarian(e, qq)) {
				continue;
			}
			tampil.add(e);
		}

		java.util.TreeMap<String, List<Entri>> grup = new java.util.TreeMap<String, List<Entri>>(
				new Comparator<String>() {
					@Override
					public int compare(String a, String b) {
						int ra = kategoriRank(a), rb = kategoriRank(b);
						return ra != rb ? ra - rb : a.compareToIgnoreCase(b);
					}
				});
		for (Entri e : tampil) {
			String kat = e.kategori == null || e.kategori.isEmpty() ? "Master & Lainnya" : e.kategori;
			List<Entri> l = grup.get(kat);
			if (l == null) {
				l = new ArrayList<Entri>();
				grup.put(kat, l);
			}
			l.add(e);
		}

		int count = tampil.size();
		for (java.util.Map.Entry<String, List<Entri>> g : grup.entrySet()) {
			List<Entri> l = g.getValue();
			Collections.sort(l, new Comparator<Entri>() {
				@Override
				public int compare(Entri a, Entri b) {
					if (a.buka != b.buka) {
						return b.buka - a.buka;
					}
					String x = a.judul == null ? "" : a.judul;
					String y = b.judul == null ? "" : b.judul;
					return x.compareToIgnoreCase(y);
				}
			});

			Html hdr = new Html("<div class='kb-grup'>" + esc(g.getKey())
					+ " <span class='kb-grup-n'>(" + l.size() + ")</span></div>");
			hdr.setParent(daftar);

			for (final Entri e : l) {
				final Div card = new Div();
				card.setSclass("kb-card");
				card.setParent(daftar);

				StringBuilder c = new StringBuilder();
				c.append("<div class='kb-title'>").append(esc(e.judul)).append("</div>");
				c.append("<div class='kb-meta'>").append(esc(e.key));
				if (e.label != null && !e.label.isEmpty() && !e.label.equalsIgnoreCase(e.judul)) {
					c.append(" &middot; ").append(esc(e.label));
				}
				if (e.tanggal != null && !e.tanggal.isEmpty()) {
					c.append(" &middot; &#128197; ").append(esc(e.tanggal));
				}
				if (e.buka > 0) {
					c.append(" &middot; &#128065; ").append(e.buka).append("&times;");
				}
				c.append("</div>");
				if (e.excerpt != null && !e.excerpt.isEmpty()) {
					c.append("<div class='kb-ex'>").append(esc(e.excerpt)).append("</div>");
				}
				Html h = new Html(c.toString());
				h.setParent(card);

				card.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event ev) throws Exception {
						tampilkanDariResource(self, e.key,
								"Bantuan — " + (e.judul == null || e.judul.isEmpty() ? e.key : e.judul));
					}
				});
			}
		}

		if (count == 0) {
			Html kosong = new Html("<div style='color:#64748b;padding:28px 12px;text-align:center;'>"
					+ "Tidak ada panduan yang cocok dengan pencarian Anda. Coba kata kunci lain, "
					+ "atau kosongkan kotak pencarian untuk menampilkan semua panduan.</div>");
			kosong.setParent(daftar);
		}
		if (info != null) {
			try {
				info.setValue(count + " panduan ditampilkan"
						+ (qq.length() > 0 ? "" : " dalam " + grup.size() + " kelompok"));
			} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:1163");
			}
		}
	}

	// =====================================================================
	// INDEKS KATALOG
	//
	// MASALAH YANG DIPERBAIKI. Katalog dahulu memanggil muatKontenFile() untuk
	// SETIAP berkas panduan, dan tiap panggilan itu menembakkan satu query
	// Hibernate ke tabel Bantuan (openSession + criteria + close) hanya untuk
	// memeriksa apakah panduan tersebut pernah dimodifikasi. Dengan 1.337
	// panduan yang masuk katalog, sekali buka katalog berarti 1.337 query
	// berurutan — jauh lebih mahal daripada membaca berkasnya sendiri, dan
	// menggantung lama bila basis data sedang lambat.
	//
	// CARA BARU. Satu query proyeksi mengambil daftar kunci yang benar-benar
	// pernah dimodifikasi (tabel Bantuan hanya berisi panduan yang disunting,
	// biasanya sedikit). Hanya kunci itulah yang isinya diambil dari basis
	// data; sisanya dibaca langsung dari berkas.
	//
	// Hasil olahan disimpan dalam SoftReference yang dipakai bersama antar
	// sesi, sehingga membuka katalog berulang kali tidak mengolah ulang belasan
	// MB teks. SoftReference dipilih agar JVM boleh membuang cache ini ketika
	// memori menipis. Cache DIPISAH PER BAHASA karena isi panduan diterjemahkan
	// mengikuti bahasa pengguna.
	// =====================================================================

	/**
	 * Bagian mahal dari satu entri katalog yang TIDAK bergantung pada pengguna:
	 * judul, cuplikan, teks untuk pencarian, dan tanggal berkas. Objek ini dipakai
	 * bersama oleh semua sesi berbahasa sama.
	 */
	private static class IsiTerindeks {
		String judul;
		String excerpt;
		String teksLower;
		String tanggal;
	}

	private static final Object INDEKS_LOCK = new Object();

	/** bahasa → indeks panduan. Nilainya lunak agar boleh dibuang JVM saat memori menipis. */
	private static final Map<String, SoftReference<Map<String, IsiTerindeks>>> INDEKS_CACHE =
			new HashMap<String, SoftReference<Map<String, IsiTerindeks>>>();

	/** bahasa → sidik jari direktori saat indeks dibangun. */
	private static final Map<String, String> INDEKS_SIDIK = new HashMap<String, String>();

	/**
	 * Buang indeks katalog agar dibangun ulang. Dipanggil setelah panduan disunting
	 * atau dihapus lewat aplikasi, karena perubahan itu tidak mengubah berkas di
	 * disk sehingga tidak terdeteksi oleh sidik jari direktori.
	 */
	public static void invalidasiIndeksKatalog() {
		synchronized (INDEKS_LOCK) {
			INDEKS_CACHE.clear();
			INDEKS_SIDIK.clear();
		}
	}

	/**
	 * Sidik jari murah untuk mendeteksi perubahan berkas panduan: jumlah berkas,
	 * waktu ubah terbaru, total ukuran, dan jumlah hash NAMA berkas. Hanya
	 * melakukan stat, tidak membaca isi.
	 *
	 * <p>Hash nama wajib ikut: mengganti nama berkas tidak mengubah jumlah,
	 * ukuran, maupun waktu ubah, sehingga tanpa hash nama katalog akan tetap
	 * menampilkan kunci lama sampai aplikasi dinyalakan ulang. Penjumlahan
	 * dipakai (bukan perkalian berantai) agar hasilnya tidak bergantung pada
	 * urutan yang dikembalikan sistem berkas.</p>
	 */
	private static String sidikDirektoriBantuan(File[] berkas) {
		int n = 0;
		long terbaru = 0L;
		long total = 0L;
		long hashNama = 0L;
		for (int i = 0; i < berkas.length; i++) {
			File f = berkas[i];
			if (f == null || !f.isFile()) {
				continue;
			}
			String nama = f.getName();
			if (nama == null || !nama.toLowerCase().endsWith(".html")) {
				continue;
			}
			n++;
			long lm = f.lastModified();
			if (lm > terbaru) {
				terbaru = lm;
			}
			total += f.length();
			hashNama += nama.toLowerCase().hashCode();
		}
		return n + ":" + terbaru + ":" + total + ":" + hashNama;
	}

	/**
	 * Daftar kunci panduan yang pernah dimodifikasi lewat aplikasi, diambil dengan
	 * SATU query proyeksi (hanya kolom kunci, bukan isinya). Kembalikan himpunan
	 * kosong bila basis data tidak tersedia — katalog lalu memakai berkas bawaan,
	 * persis seperti perilaku lama saat query gagal.
	 */
	private static Set<String> kunciTermodifikasiDiTabel() {
		Set<String> hasil = new HashSet<String>();
		Session s = null;
		try {
			s = HibernateUtil.openSession();
			List<?> l = s.createCriteria(Bantuan.class)
					.setProjection(org.hibernate.criterion.Projections.property("kunci"))
					.list();
			if (l != null) {
				for (int i = 0; i < l.size(); i++) {
					Object o = l.get(i);
					if (o != null) {
						hasil.add(String.valueOf(o).trim().toLowerCase());
					}
				}
			}
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:kunciTermodifikasiDiTabel");
			// abaikan → anggap belum ada yang dimodifikasi (jatuh ke berkas HTML)
		} finally {
			HibernateUtil.closeSessionQuietly(s);
		}
		return hasil;
	}

	/** Baca satu berkas panduan apa adanya (tanpa menyentuh basis data). */
	private static String bacaBerkasBantuan(File f) {
		FileInputStream in = null;
		try {
			in = new FileInputStream(f);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte[] buf = new byte[8192];
			int n;
			while ((n = in.read(buf)) != -1) {
				bos.write(buf, 0, n);
			}
			return new String(bos.toByteArray(), "UTF-8");
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:bacaBerkasBantuan");
			return null;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:bacaBerkasBantuan-close");
				}
			}
		}
	}

	/** Bahasa aktif sebagai kunci cache; aman dipanggil kapan pun. */
	private static String bahasaSekarang() {
		try {
			String lang = ais.common.Common.currentLang();
			return lang == null ? "" : lang.trim().toLowerCase();
		} catch (Throwable t) {
			return "";
		}
	}

	/**
	 * Indeks seluruh panduan yang boleh muncul di katalog (berkas berawalan "_"
	 * dan berakhiran "_qa" tidak pernah ditampilkan sehingga tidak diindeks).
	 * Dibangun sekali lalu dipakai bersama sampai berkas berubah.
	 */
	private static Map<String, IsiTerindeks> indeksKatalog() {
		File dir = null;
		try {
			String path = Sessions.getCurrent().getWebApp().getRealPath("/WEB-INF/bantuan/");
			if (path != null) {
				dir = new File(path);
			}
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:indeksKatalog-dir");
		}
		if (dir == null || !dir.isDirectory()) {
			return Collections.emptyMap();
		}
		File[] berkas = dir.listFiles();
		if (berkas == null) {
			return Collections.emptyMap();
		}

		String bahasa = bahasaSekarang();
		String sidik = sidikDirektoriBantuan(berkas);
		synchronized (INDEKS_LOCK) {
			SoftReference<Map<String, IsiTerindeks>> ref = INDEKS_CACHE.get(bahasa);
			Map<String, IsiTerindeks> ada = ref == null ? null : ref.get();
			if (ada != null && sidik.equals(INDEKS_SIDIK.get(bahasa))) {
				return ada;
			}
		}

		// Satu query untuk seluruh katalog, menggantikan satu query per panduan.
		Set<String> termodifikasi = kunciTermodifikasiDiTabel();

		java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("dd-MM-yyyy");
		Map<String, IsiTerindeks> baru = new LinkedHashMap<String, IsiTerindeks>();
		for (int i = 0; i < berkas.length; i++) {
			File f = berkas[i];
			if (f == null || !f.isFile()) {
				continue;
			}
			String nama = f.getName();
			if (nama == null || !nama.toLowerCase().endsWith(".html")) {
				continue;
			}
			String kunci = nama.substring(0, nama.length() - 5).toLowerCase();
			if (kunci.startsWith("_") || kunci.endsWith("_qa")) {
				continue;
			}
			String html;
			if (termodifikasi.contains(kunci)) {
				// Hanya panduan yang memang pernah disunting yang menyentuh basis data.
				html = muatKontenFile(kunci);
			} else {
				html = bacaBerkasBantuan(f);
				html = terjemahKontenBantuanBilaPerlu(html);
			}
			if (html == null || html.trim().isEmpty()) {
				continue;
			}
			IsiTerindeks it = new IsiTerindeks();
			String judul = ekstrakJudul(html);
			it.judul = judul == null ? "" : judul;
			String teks = stripTags(html);
			it.excerpt = ringkas(teks, 220);
			it.teksLower = teks.toLowerCase();
			it.tanggal = fmt.format(new java.util.Date(f.lastModified()));
			baru.put(kunci, it);
		}

		synchronized (INDEKS_LOCK) {
			INDEKS_CACHE.put(bahasa, new SoftReference<Map<String, IsiTerindeks>>(baru));
			INDEKS_SIDIK.put(bahasa, sidik);
		}
		return baru;
	}

	/** Susun satu entri katalog dari indeks bersama. Teks pencarian dipakai ulang
	 *  lewat rujukan, tidak disalin, agar membuka katalog tidak menggandakan memori. */
	private static Entri entriDariIndeks(IsiTerindeks it, String key, String label) {
		Entri e = new Entri();
		e.key = key;
		e.label = label == null ? "" : label.trim();
		String judul = it.judul;
		if (judul == null || judul.isEmpty()) {
			judul = e.label.isEmpty() ? key : e.label;
		}
		e.judul = judul;
		e.teksLower = it.teksLower;
		e.excerpt = it.excerpt;
		e.kategori = kategoriDariKey(key);
		e.tanggal = it.tanggal;
		return e;
	}

	/**
	 * Apakah entri cocok dengan kata kunci pencarian. Judul dan label diuji
	 * terpisah dari teks isi supaya teks isi yang besar dapat dipakai bersama
	 * antar sesi tanpa disalin ulang per pengguna.
	 */
	private static boolean cocokPencarian(Entri e, String q) {
		if (e == null || q == null || q.length() == 0) {
			return true;
		}
		if (e.teksLower != null && e.teksLower.indexOf(q) >= 0) {
			return true;
		}
		if (e.judul != null && e.judul.toLowerCase().indexOf(q) >= 0) {
			return true;
		}
		if (e.label != null && e.label.toLowerCase().indexOf(q) >= 0) {
			return true;
		}
		return false;
	}

	/**
	 * Kumpulkan entri katalog. Bila daftar menu pengguna tersedia, hanya panduan
	 * untuk halaman yang menu-nya dimiliki pengguna yang disertakan. Bila tidak
	 * tersedia (mis. peran tanpa menu), seluruh berkas panduan ditampilkan sebagai
	 * cadangan agar katalog tidak kosong.
	 */
	private static List<Entri> kumpulkanEntriBantuan() {
		Map<String, IsiTerindeks> indeks = indeksKatalog();
		LinkedHashMap<String, Entri> map = new LinkedHashMap<String, Entri>();

		List<Menu> menus = daftarMenuPengguna();
		if (menus != null && !menus.isEmpty()) {
			for (Menu m : menus) {
				if (m == null) {
					continue;
				}
				if (m.getAktif() != null && !m.getAktif().booleanValue()) {
					continue;
				}
				String key = keyDariUrl(m.getUrl());
				if (key == null || map.containsKey(key)) {
					continue;
				}
				IsiTerindeks it = indeks.get(key);
				if (it == null) {
					continue;
				}
				map.put(key, entriDariIndeks(it, key, m.getLabel()));
			}
		}

		// Cadangan: bila belum ada satu pun (mis. menu belum termuat) seluruh panduan.
		if (map.isEmpty()) {
			for (Map.Entry<String, IsiTerindeks> en : indeks.entrySet()) {
				map.put(en.getKey(), entriDariIndeks(en.getValue(), en.getKey(), null));
			}
		}

		// Panduan menurut peran (berkas "panduan*.html") tidak terikat pada menu mana pun,
		// sehingga tidak akan pernah terjaring oleh penyaringan hak akses di atas. Panduan
		// ini bersifat umum dan aman ditampilkan kepada semua pengguna, jadi selalu
		// disertakan ke dalam katalog.
		for (Map.Entry<String, IsiTerindeks> en : indeks.entrySet()) {
			String kunci = en.getKey();
			if (!kunci.startsWith("panduan") || map.containsKey(kunci)) {
				continue;
			}
			map.put(kunci, entriDariIndeks(en.getValue(), kunci, "Panduan Pengguna"));
		}


		// Isi jumlah buka untuk analitik ringan.
		try {
			java.util.Properties stats = bacaStats();
			for (Entri e : map.values()) {
				try {
					e.buka = Integer.parseInt(stats.getProperty(e.key, "0"));
				} catch (Throwable ignore) {
					e.buka = 0;
				}
			}
		} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:1240");
		}

		List<Entri> hasil = new ArrayList<Entri>(map.values());
		Collections.sort(hasil, new Comparator<Entri>() {
			@Override
			public int compare(Entri a, Entri b) {
				String x = a == null || a.judul == null ? "" : a.judul;
				String y = b == null || b.judul == null ? "" : b.judul;
				return x.compareToIgnoreCase(y);
			}
		});
		return hasil;
	}

	/**
	 * Ambil daftar menu milik pengguna saat ini TANPA memicu akses basis data yang
	 * berisiko. Sumber utama: atribut sesi {@code current_menus} yang telah disiapkan
	 * saat membangun navigasi. Cadangan: koleksi menu peran bila sudah ter-inisialisasi
	 * (aman, tidak memicu lazy-load). Kembalikan null bila tidak tersedia.
	 */
	@SuppressWarnings("unchecked")
	private static List<Menu> daftarMenuPengguna() {
		try {
			Object o = Sessions.getCurrent().getAttribute("current_menus");
			if (o instanceof List) {
				List<?> l = (List<?>) o;
				if (!l.isEmpty()) {
					List<Menu> menus = new ArrayList<Menu>();
					for (Object x : l) {
						if (x instanceof Menu) {
							menus.add((Menu) x);
						}
					}
					if (!menus.isEmpty()) {
						return menus;
					}
				}
			}
		} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:1279");
		}
		try {
			Tbmuser u = Common.getCurrentUser();
			if (u != null && u.hakAkses() != null) {
				if (org.hibernate.Hibernate.isInitialized(u.hakAkses().getMenus())) {
					java.util.Set<Menu> set = u.hakAkses().getMenus();
					if (set != null && !set.isEmpty()) {
						return new ArrayList<Menu>(set);
					}
				}
			}
		} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:1291");
		}
		return null;
	}

	/** Turunkan kunci berkas panduan dari URL menu (nama berkas zul tanpa path/ekstensi). */
	private static String keyDariUrl(String url) {
		if (url == null) {
			return null;
		}
		String u = url.trim();
		if (u.isEmpty()) {
			return null;
		}
		int tanya = u.indexOf('?');
		if (tanya >= 0) {
			u = u.substring(0, tanya);
		}
		int pagar = u.indexOf('#');
		if (pagar >= 0) {
			u = u.substring(0, pagar);
		}
		int slash = u.lastIndexOf('/');
		if (slash >= 0) {
			u = u.substring(slash + 1);
		}
		int dot = u.lastIndexOf('.');
		if (dot >= 0) {
			u = u.substring(0, dot);
		}
		u = u.trim().toLowerCase();
		return u.isEmpty() ? null : u;
	}

	/** Bangun satu entri katalog dari kunci, label menu, dan isi HTML panduan. */
	private static Entri buatEntri(String key, String label, String html) {
		Entri e = new Entri();
		e.key = key;
		e.label = label == null ? "" : label.trim();
		String judul = ekstrakJudul(html);
		if (judul == null || judul.isEmpty()) {
			judul = e.label.isEmpty() ? key : e.label;
		}
		e.judul = judul;
		String teks = stripTags(html);
		e.teksLower = (e.judul + " " + e.label + " " + teks).toLowerCase();
		e.excerpt = ringkas(teks, 220);
		e.kategori = kategoriDariKey(key);
		e.tanggal = tanggalBerkas(key);
		return e;
	}

	/** Turunkan nama modul/kelompok dari awalan key (untuk pengelompokan katalog). */
	private static String kategoriDariKey(String key) {
		String k = key == null ? "" : key.toLowerCase();
		if (k.startsWith("konfigurasi")) return "Konfigurasi";
		if (k.startsWith("laporan") || k.contains("_laporan")) return "Laporan";
		if (k.startsWith("transaksi")) return "Transaksi Akademik";
		if (k.startsWith("biodata") || k.startsWith("biografi")) return "Biodata";
		if (k.startsWith("konfig")) return "Konfigurasi";
		if (k.startsWith("absen") || k.contains("absensi")) return "Absensi";
		if (k.startsWith("ujian") || k.contains("cbt")) return "Ujian";
		if (k.startsWith("pembayaran") || k.startsWith("tagihan") || k.startsWith("daftarulang")
				|| k.startsWith("biaya") || k.contains("keuangan") || k.startsWith("jurnal")
				|| k.startsWith("kas") || k.startsWith("pos_") || k.startsWith("bayar")) return "Keuangan & Pembayaran";
		if (k.startsWith("kantin") || k.startsWith("koperasi") || k.startsWith("outlet")
				|| k.startsWith("gudang") || k.startsWith("stok") || k.startsWith("item")) return "Kantin, Koperasi & Persediaan";
		if (k.startsWith("pegawai") || k.startsWith("gaji") || k.startsWith("penggajian")
				|| k.startsWith("cuti") || k.startsWith("karir") || k.contains("kepegawaian")) return "Kepegawaian";
		if (k.startsWith("rps") || k.startsWith("obe") || k.startsWith("cpmk") || k.startsWith("kurikulum")
				|| k.startsWith("matakuliah") || k.startsWith("mata_kuliah")) return "Kurikulum & OBE";
		if (k.startsWith("perkuliahan") || k.startsWith("pertemuan") || k.startsWith("aktifitas")
				|| k.startsWith("aktivitas") || k.startsWith("nilai") || k.startsWith("krs")
				|| k.startsWith("jadwal") || k.startsWith("jadual")) return "Perkuliahan";
		if (k.startsWith("surat") || k.startsWith("disposisi") || k.startsWith("sop")
				|| k.startsWith("alur")) return "Surat & SOP";
		if (k.startsWith("sekolah") || k.startsWith("siswa") || k.startsWith("pelajaran")) return "Sekolah";
		if (k.startsWith("alumni") || k.startsWith("tracer")) return "Alumni & Tracer";
		if (k.startsWith("aset") || k.startsWith("asset") || k.startsWith("pengadaan")
				|| k.startsWith("vendor") || k.startsWith("po_") || k.startsWith("rab")) return "Aset & Pengadaan";
		if (k.startsWith("mahasiswa") || k.startsWith("dosen") || k.startsWith("wisuda")
				|| k.startsWith("skripsi") || k.startsWith("bimbingan") || k.startsWith("pendaftaran")
				|| k.startsWith("spmb") || k.startsWith("ujian_masuk")) return "Kemahasiswaan";
		return "Master & Lainnya";
	}

	/** Tanggal modifikasi berkas panduan (dd-MM-yyyy) atau string kosong bila gagal. */
	private static String tanggalBerkas(String key) {
		try {
			if (key == null || key.trim().isEmpty()) return "";
			String path = Sessions.getCurrent().getWebApp()
					.getRealPath("/WEB-INF/bantuan/" + key.trim() + ".html");
			if (path == null) return "";
			File f = new File(path);
			if (!f.isFile()) return "";
			return new java.text.SimpleDateFormat("dd-MM-yyyy").format(new java.util.Date(f.lastModified()));
		} catch (Throwable t) {
			return "";
		}
	}

	// ---- Analitik ringan: hitung berapa kali tiap panduan dibuka ----
	private static final Object STATS_LOCK = new Object();

	private static String statsPath() {
		try {
			return Sessions.getCurrent().getWebApp().getRealPath("/WEB-INF/bantuan/_stats.properties");
		} catch (Throwable t) {
			return null;
		}
	}

	private static java.util.Properties bacaStats() {
		java.util.Properties p = new java.util.Properties();
		java.io.FileInputStream in = null;
		try {
			String path = statsPath();
			if (path != null) {
				File f = new File(path);
				if (f.isFile()) {
					in = new java.io.FileInputStream(f);
					p.load(in);
				}
			}
		} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:1415");
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:1420");
				}
			}
		}
		return p;
	}

	/** Catat satu kali buka panduan (best-effort; kegagalan diabaikan). */
	private static void catatBuka(String key) {
		if (key == null || key.trim().isEmpty()) {
			return;
		}
		final String safe = key.trim().toLowerCase();
		if (!safe.matches("[a-z0-9_\\-]+")) {
			return;
		}
		synchronized (STATS_LOCK) {
			java.io.FileOutputStream out = null;
			try {
				java.util.Properties p = bacaStats();
				int n = 0;
				try {
					n = Integer.parseInt(p.getProperty(safe, "0"));
				} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:1443");
				}
				p.setProperty(safe, String.valueOf(n + 1));
				String path = statsPath();
				if (path != null) {
					out = new java.io.FileOutputStream(path);
					p.store(out, "Statistik buka bantuan (key=jumlah)");
				}
			} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:1451");
			} finally {
				if (out != null) {
					try {
						out.close();
					} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:1456");
					}
				}
			}
		}
	}

	/** Ambil teks judul dari elemen {@code <h2>} pertama; null bila tidak ada. */
	private static String ekstrakJudul(String html) {
		if (html == null) {
			return null;
		}
		try {
			java.util.regex.Matcher m = java.util.regex.Pattern
					.compile("<h2[^>]*>(.*?)</h2>", java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL)
					.matcher(html);
			if (m.find()) {
				String isi = stripTags(m.group(1));
				return isi == null ? null : isi.trim();
			}
		} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:1476");
		}
		return null;
	}

	/** Buang seluruh tag HTML dan rapikan spasi/entitas dasar. */
	private static String stripTags(String html) {
		if (html == null) {
			return "";
		}
		String s = html.replaceAll("(?is)<style[^>]*>.*?</style>", " ");
		s = s.replaceAll("(?is)<script[^>]*>.*?</script>", " ");
		s = s.replaceAll("<[^>]+>", " ");
		s = s.replace("&nbsp;", " ").replace("&middot;", " ").replace("&amp;", "&")
				.replace("&lt;", "<").replace("&gt;", ">");
		s = s.replaceAll("\\s+", " ").trim();
		return s;
	}

	/**
	 * Cetak satu panduan: buka jendela baru berisi HTML panduan lalu picu dialog cetak
	 * peramban. Konten dikirim sebagai literal string JavaScript yang di-escape aman.
	 */
	private static void cetakHtml(String judul, String isiHtml) {
		try {
			String doc = "<!DOCTYPE html><html lang='id'><head><meta charset='utf-8'><title>" + esc(judul)
					+ "</title><style>"
					+ "body{font-family:'Segoe UI',Arial,sans-serif;color:#0f172a;max-width:900px;margin:0 auto;"
					+ "padding:24px;line-height:1.7;font-size:13px;}"
					+ "h2{color:#1d4ed8;} h3{color:#0f172a;border-bottom:2px solid #e2e8f0;padding-bottom:4px;}"
					+ "@media print{@page{margin:16mm;}}"
					+ "</style></head><body>" + (isiHtml == null ? "" : isiHtml) + "</body></html>";
			bukaDanCetak(doc);
		} catch (Throwable t) {
			try {
				Common.tampilErrorJikaAdmin(t instanceof Exception ? (Exception) t : new Exception(t));
				PesanFormalHelper.tampilkanGagalException("pencetakan konten Bantuan",
						t,
						new String[] {
								"Pastikan browser mengizinkan jendela cetak (popup) untuk dibuka.",
								"Coba ulangi proses pencetakan beberapa saat lagi.",
								"Bila kegagalan berulang, laporkan ke Administrator/pengembang disertai tangkapan layar (screenshot) pesan ini."
						});
			} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:1512");
			}
		}
	}

	/**
	 * Cetak katalog: susun daftar panduan yang sedang tampil (sesuai kata kunci) menjadi
	 * satu dokumen ringkas bernomor, lalu picu dialog cetak peramban.
	 */
	private static void cetakKatalog(List<Entri> semua, String q) {
		try {
			String qq = q == null ? "" : q.trim();
			StringBuilder b = new StringBuilder(8192);
			b.append("<h2>Katalog Bantuan</h2>");
			b.append("<p style='color:#475569;'>Daftar panduan penggunaan halaman yang sesuai dengan hak akses Anda");
			if (!qq.isEmpty()) {
				b.append(" untuk kata kunci “").append(esc(qq)).append("”");
			}
			b.append(".</p>");
			String qlow = qq.toLowerCase();
			int no = 0;
			b.append("<ol style='padding-left:20px;'>");
			for (Entri e : semua) {
				if (e == null) {
					continue;
				}
				if (qlow.length() > 0 && !cocokPencarian(e, qlow)) {
					continue;
				}
				no++;
				b.append("<li style='margin:0 0 10px;'>");
				b.append("<span style='color:#1d4ed8;font-weight:600;'>").append(esc(e.judul)).append("</span>");
				b.append("<span style='color:#64748b;font-size:0.9em;'> — ").append(esc(e.key));
				if (e.label != null && !e.label.isEmpty() && !e.label.equalsIgnoreCase(e.judul)) {
					b.append(" (").append(esc(e.label)).append(")");
				}
				b.append("</span>");
				if (e.excerpt != null && !e.excerpt.isEmpty()) {
					b.append("<br><span style='color:#475569;'>").append(esc(e.excerpt)).append("</span>");
				}
				b.append("</li>");
			}
			b.append("</ol>");
			b.append("<p style='color:#64748b;font-size:0.9em;'>Total: ").append(no).append(" panduan.</p>");

			String doc = "<!DOCTYPE html><html lang='id'><head><meta charset='utf-8'><title>Katalog Bantuan</title>"
					+ "<style>body{font-family:'Segoe UI',Arial,sans-serif;color:#0f172a;max-width:900px;margin:0 auto;"
					+ "padding:24px;line-height:1.6;font-size:13px;} h2{color:#1d4ed8;} @media print{@page{margin:16mm;}}"
					+ "</style></head><body>" + b.toString() + "</body></html>";
			bukaDanCetak(doc);
		} catch (Throwable t) {
			try {
				Common.tampilErrorJikaAdmin(t instanceof Exception ? (Exception) t : new Exception(t));
				PesanFormalHelper.tampilkanGagalException("pencetakan katalog Bantuan",
						t,
						new String[] {
								"Pastikan browser mengizinkan jendela cetak (popup) untuk dibuka.",
								"Coba ulangi proses pencetakan beberapa saat lagi.",
								"Bila kegagalan berulang, laporkan ke Administrator/pengembang disertai tangkapan layar (screenshot) pesan ini."
						});
			} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanHelper.java:1565");
			}
		}
	}

	/** Buka dokumen HTML di jendela baru peramban lalu picu dialog cetak. */
	private static void bukaDanCetak(String doc) {
		String js = "(function(){var w=window.open('','_blank');if(!w){alert('Mohon izinkan pop-up pada peramban "
				+ "untuk dapat mencetak.');return;}w.document.open();w.document.write(" + toJsString(doc)
				+ ");w.document.close();w.focus();setTimeout(function(){try{w.print();}catch(e){}},400);})();";
		Clients.evalJavaScript(js);
	}

	/** Ubah string menjadi literal string JavaScript yang aman (menghindari penutupan tag/skrip). */
	private static String toJsString(String s) {
		if (s == null) {
			return "\"\"";
		}
		StringBuilder b = new StringBuilder(s.length() + 64);
		b.append('"');
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
			case '\\':
				b.append("\\\\");
				break;
			case '"':
				b.append("\\\"");
				break;
			case '\n':
				b.append("\\n");
				break;
			case '\r':
				b.append("\\r");
				break;
			case '\t':
				b.append("\\t");
				break;
			case '<':
				b.append("\\u003c");
				break;
			case '>':
				b.append("\\u003e");
				break;
			default:
				b.append(c);
			}
		}
		b.append('"');
		return b.toString();
	}

	/** Potong teks menjadi cuplikan singkat pada batas kata, ditambah elipsis. */
	private static String ringkas(String teks, int maks) {
		if (teks == null) {
			return "";
		}
		String t = teks.trim();
		if (t.length() <= maks) {
			return t;
		}
		String potong = t.substring(0, maks);
		int spasi = potong.lastIndexOf(' ');
		if (spasi > maks - 40) {
			potong = potong.substring(0, spasi);
		}
		return potong.trim() + "…";
	}
}
