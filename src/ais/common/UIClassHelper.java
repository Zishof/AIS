package ais.common;

import java.net.URLEncoder;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Button;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Rows;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.GeneralValueObject;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.PengumumanAkademis;
import ais.database.model.PengumumanPerkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyFormRow;
import ais.ui.util.MyRowStyled;
import ais.ui.util.MyToolbarbuttonConfig;
// Pastikan import class domain Anda ada (Tbmuser, Mahasiswa, dll)

/**
 * Helper terfokus untuk ui class. Tipe ini membungkus satu variasi kecil dari alur yang lebih umum
 * agar pemanggil memakai nama domain yang jelas dan tidak menggandakan implementasi.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code String EXT_UPDATE}; pembacaan/pencarian
 * ({@code infoDiuploadOleh()}, {@code tampilkanUserDiUI()}, {@code tampilanScroll()}, {@code
 * tampilanScrollTabbox()}, {@code tampilanScroll1()}, {@code tampilanScroll2()}); operasi domain lain ({@code
 * generateOlehId()}, {@code createBaseGrid()}, {@code createBaseRow()}, {@code jadikanCenterScrollable()},
 * {@code createVideoConrefrence()}, {@code applyReadMore()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> sesuai operasi yang dipanggil, utilitas dapat mengubah komponen UI, membaca/menulis
 * persistence atau berkas, dan memanggil layanan lain. Gunakan method kanonik di kelas ini melalui konteks
 * request/transaksi yang tepat, bukan menyalin implementasinya.</p>
 */
public class UIClassHelper {

	private static final String EXT_UPDATE = "external_update;";

	/**
	 * Menghasilkan ID unik gabungan (ID + NamaClass) berdasarkan peran user.
	 * Menggunakan if-else menggantikan nested ternary operator agar mudah dibaca.
	 */
	public static String generateOlehId(Tbmuser tbmuser) {
		if (tbmuser == null || tbmuser.getUserId() == null) {
			return EXT_UPDATE;
		}

		try {
			// Cek prioritas dari yang paling spesifik ke umum
			// 1. Biodata Calon Mahasiswa
			BiodataCalonMahasiswa bcm = tbmuser.getBiodataCalonMahasiswa();
			if (bcm != null) {
				return bcm.getNoRegistrasi() + ";" + BiodataCalonMahasiswa.class.getName();
			}

			// 2. Calon Siswa
			CalonSiswa calonSiswa = tbmuser.getCalonSiswa();
			if (calonSiswa != null) {
				return calonSiswa.getNoRegistrasi() + ";" + CalonSiswa.class.getName();
			}

			// 3. Guru
			Guru guru = tbmuser.ambilGuru();
			if (guru != null) {
				return tbmuser.getUserId() + ";" + Guru.class.getName();
			}

			// 4. Siswa
			Siswa siswa = tbmuser.getSiswa();
			if (siswa != null) {
				return siswa.getNomorIndukNasional() + ";" + Siswa.class.getName();
			}

			// 5. Mahasiswa
			Mahasiswa mhs = tbmuser.getMahasiswa();
			if (mhs != null) {
				return mhs.getNim() + ";" + Mahasiswa.class.getName();
			}

			// 6. Dosen
			Dosen dosen = tbmuser.ambilDosen();
			if (dosen != null) {
				return tbmuser.getUserId() + ";" + Dosen.class.getName();
			}

			// 7. Pegawai
			Pegawai pegawai = tbmuser.ambilPegawai();
			if (pegawai != null) {
				return tbmuser.getUserId() + ";" + Pegawai.class.getName();
			}

			// Default: Tbmuser biasa
			return tbmuser.getUserId() + ";" + Tbmuser.class.getName();

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/UIClassHelper.java:105");
			return "";
		}
	}

	/**
	 * Menampilkan informasi uploader ke UI. Menggunakan switch-case string (Java
	 * 1.7 compatible) dan helper method.
	 */
	public static void infoDiuploadOleh(String olehId, String oleh, Component parent) {
		if (olehId == null || olehId.trim().isEmpty() || olehId.trim().equalsIgnoreCase(EXT_UPDATE)) {
			new ais.ui.util.MyHtml(oleh == null ? "" : oleh).setParent(parent);
			return;
		}

		Hbox vbox = new Hbox();
		vbox.setPack("center");
		vbox.setAlign("center");
		vbox.setParent(parent);

		try {
			String[] spl = olehId.split(";");
			// Validasi array length untuk mencegah error jika format salah
			if (spl.length < 2)
				return;

			String userid = spl[0];
			String clazzName = spl[1];

			GeneralValueObject userObject = null;

			// Logika pengambilan data
			if (clazzName.equals(Tbmuser.class.getName())) {
				userObject = ConstantValues.ambil(Tbmuser.class.getName(), userid);
			} else if (clazzName.equals(Dosen.class.getName())) {
				Tbmuser t = (Tbmuser) ConstantValues.ambil(Tbmuser.class.getName(), userid);
				userObject = (t != null) ? t.ambilDosen() : null;
			} else if (clazzName.equals(Pegawai.class.getName())) {
				Tbmuser t = (Tbmuser) ConstantValues.ambil(Tbmuser.class.getName(), userid);
				userObject = (t != null) ? t.ambilPegawai() : null;
			} else if (clazzName.equals(Guru.class.getName())) {
				Tbmuser t = (Tbmuser) ConstantValues.ambil(Tbmuser.class.getName(), userid);
				userObject = (t != null) ? t.ambilGuru() : null;
			} else if (clazzName.equals(Mahasiswa.class.getName())) {
				userObject = ConstantValues.simpleObject(HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("nim", userid)).setMaxResults(1), Mahasiswa.class);
			} else if (clazzName.equals(BiodataCalonMahasiswa.class.getName())) {
				userObject = ConstantValues
						.simpleObject(
								HibernateUtil.currentSession().createCriteria(BiodataCalonMahasiswa.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(Restrictions.eq("noRegistrasi", userid)).setMaxResults(1),
								BiodataCalonMahasiswa.class);
			} else if (clazzName.equals(Siswa.class.getName())) {
				userObject = ConstantValues.simpleObject(HibernateUtil.currentSession().createCriteria(Siswa.class)
						.add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa", "")) // Empty string
																										// check
						.add(Restrictions.isNotNull("sekolah")).add(Restrictions.eq("nomorIndukNasional", userid))
						.setMaxResults(1), Siswa.class);
			} else if (clazzName.equals(CalonSiswa.class.getName())) {
				userObject = ConstantValues.simpleObject(HibernateUtil.currentSession().createCriteria(CalonSiswa.class)
						.add(Restrictions.isNotNull("gelombangPendaftaranPsb"))
						.add(Restrictions.eq("noRegistrasi", userid)).setMaxResults(1), CalonSiswa.class);
			}

			// Tampilkan hasil menggunakan helper
			if (userObject != null) {
				tampilkanUserDiUI(userObject, oleh, vbox);
			}

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/UIClassHelper.java:177");
			// Log error sebaiknya jangan dimatikan total saat development
			// e.printStackTrace();
		}
	}

	/**
	 * Helper private untuk menampilkan gambar dan label (Mencegah duplikasi code).
	 * 
	 * @throws Exception
	 */
	private static void tampilkanUserDiUI(GeneralValueObject userObj, String labelText, Component parent)
			throws Exception {
		CommonMedia.tampilkanGambarKecil(userObj).setParent(parent);
		new Label(labelText).setParent(parent);
	}

	// ================== TAMPILAN SCROLL HELPERS ==================

	/**
	 * Helper utama untuk membuat Grid transparan dasar.
	 */
	private static Grid createBaseGrid(Component parent, String sclass) {
		Grid grid = new Grid();
		grid.setSclass(sclass);
		grid.setStyle("border:0px;background: transparent;");
		if (parent != null) {
			grid.setParent(parent);
		}
		return grid;
	}

	/**
	 * Helper untuk membuat Row transparan dasar.
	 */
	private static MyFormRow createBaseRow(Grid grid) {
		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		return row;
	}

	public static MyFormRow tampilanScroll(Component component) {
		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(component);
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");

		Center center = new Center();
		center.setBorder("none");
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		// Reuse logic
		Grid grid = createBaseGrid(center, "dgrid");
		return createBaseRow(grid);
	}

	/**
	 * Varian tampilanScroll khusus komponen PENGISI TINGGI (Tabbox, Borderlayout,
	 * Iframe, dsb. yang di-set height 100%).
	 *
	 * tampilanScroll biasa menaruh konten di dalam Row sebuah Grid; tinggi Row
	 * mengikuti isinya (auto), sehingga child ber-height 100% kehilangan acuan dan
	 * kolaps menjadi sangat pendek (konten tab tampak kosong). Di sini konten
	 * langsung menjadi anak Center ber-flex yang tingginya pasti mengikuti parent,
	 * dengan autoscroll sebagai pengaman bila isi melebihi tinggi.
	 *
	 * Bonus UX: header tab tetap terlihat saat isi panel discroll, karena yang
	 * discroll adalah isi panel — bukan seluruh tabbox.
	 */
	public static Center tampilanScrollTabbox(Component component) {
		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(component);
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");

		Center center = new Center();
		center.setBorder("none");
		center.setAutoscroll(true);
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		/*
		 * Marker untuk CSS (css_utama.css blok "SCROLL PANEL TABBOX"): panel tab
		 * ZK 5 yang tingginya terkukur default-nya overflow:hidden sehingga
		 * konten panjang TERPOTONG tanpa scrollbar (contoh nyata: popup
		 * Informasi Pembayaran Mahasiswa). Class ini mengaktifkan overflow:auto
		 * pada isi panel — konten panjang bisa discroll, sedangkan komponen
		 * ber-height 100% (iframe/laporan) tetap mengisi penuh seperti biasa.
		 */
		center.setSclass("ais-scroll-tabbox-host");
		return center;
	}

	/**
	 * Konfigurasi Center (Borderlayout) yang SUDAH ADA agar bisa discroll — dipakai saat
	 * caller SUDAH punya Borderlayout+Center sendiri (mis. layar dengan toolbar North,
	 * grid isi di Center, total di South) dan tinggal butuh Center-nya scroll-ready.
	 *
	 * <p><b>WAJIB:</b> taruh Grid/komponen isi LANGSUNG sebagai anak Center ini
	 * ({@code grid.setParent(center)}) — JANGAN dibungkus Div tambahan ("scrollWrapper").
	 * Div pembungkus ber-overflow:auto+height:100% SERING TIDAK memunculkan scrollbar sama
	 * sekali pada versi ZK yang dipakai (height:100% di dalam &lt;td&gt; tidak ter-recompute
	 * otomatis setelah render ulang) — pola inilah yang dulu dipakai dan gagal di
	 * TransaksiJurnalUmumHelper (grid "Akun Transaksi Jurnal Umum"), diganti ke pola ini.</p>
	 *
	 * <p>Untuk Tab panel (butuh Borderlayout+Center BARU, bukan Center yang sudah ada),
	 * pakai {@link #tampilanScrollTabbox(Component)} — perilakunya sama, hanya beda siapa
	 * yang membuat Borderlayout+Center-nya.</p>
	 *
	 * @param center Center yang sudah di-parent-kan ke Borderlayout-nya sendiri.
	 */
	public static void jadikanCenterScrollable(Center center) {
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setAutoscroll(true);
		center.setStyle("overflow:auto;");
	}

	public static Button createVideoConrefrence(final GeneralValueObject generalValueObject, Component hbox,
			boolean vertical, boolean button, final EventListener eventListener) throws Exception {

		Button toolbarbutton = button ? new MyButtonConfig("Online", "/img/svg/user-group.svg")
				: new MyToolbarbuttonConfig("Online", "/img/svg/user-group.svg");
		final String hangoutLink = generalValueObject.retreive("hangoutLink");
		if (hangoutLink != null && !hangoutLink.trim().isEmpty()) {
			toolbarbutton.setImage("/img/meet-google.png");
			toolbarbutton.setParent(hbox);
			if (vertical) {
				toolbarbutton.setOrient("vertical");
			}
			toolbarbutton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event a) throws Exception {

					generalValueObject.masukkanData("online");

					String server = hangoutLink + "?hs=122&ijlm=1588886137268";
					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(server, "_blank");
					} else {

						Clients.evalJavaScript(
								"popupCenter({url: '" + server + "', title: 'Video Conference', w: 1200, h: 600});");

					}

					if (eventListener != null) {
						eventListener.onEvent(null);
					}
				}
			});
		}

		else {

			TreeMap<String, String> d = generalValueObject.ambilData("online", null);

			int jumlah = d.size();
			if (vertical) {
				toolbarbutton.setOrient("vertical");
			}
			if (jumlah > 0) {
				toolbarbutton.setImage("/img/online-red-icon.png");
			}
			toolbarbutton.setParent(hbox);

			toolbarbutton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event a) throws Exception {
					Button toolbarbutton = (Button) a.getTarget();
					generalValueObject.masukkanData("online");

					String id = generalValueObject.getNama() + "_" + generalValueObject.getId();
					if (generalValueObject instanceof PengumumanAkademis) {
						id = ((PengumumanAkademis) generalValueObject).getJudul() + "_" + generalValueObject.getId();
					} else if (generalValueObject instanceof PengumumanPerkuliahan) {
						id = ((PengumumanPerkuliahan) generalValueObject).getJudul() + "_" + generalValueObject.getId();
					}

					toolbarbutton.setImage("/img/online-red-icon.png");
					HttpServletRequest request = null;
					if (ExecutionsCtrl.getCurrent() != null) {
						request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
					}

					if (request == null) {
						request = RequestContext.get();
					}
					String kodeStream = URLEncoder.encode(
							org.apache.commons.lang3.StringUtils.replace(request.getContextPath(), "/", ""), "UTF-8")
							+ "_" + id;
					try {
						String[] words = kodeStream.replaceAll("[^a-zA-Z0-9 ]", "_").toLowerCase().split("\\s+");
						kodeStream = "";
						for (String w : words) {
							kodeStream += kodeStream.isEmpty() ? w : "_" + w;
						}

						kodeStream = kodeStream.replaceAll("__", "_");
						kodeStream = kodeStream.replaceAll("__", "_");
						kodeStream = kodeStream.replaceAll("__", "_");

					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/UIClassHelper.java:362");
					}
					final String server = Common.getKonfigurasi("alamat_server_video_conference", "https://meet.jit.si")
							.getNilai() + "/" + kodeStream;

					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(server, "_blank");
					} else {

						Clients.evalJavaScript(
								"popupCenter({url: '" + server + "', title: 'Video Conference', w: 1200, h: 600});");

					}

					if (eventListener != null) {
						eventListener.onEvent(null);
					}
				}

			});
		}
		return toolbarbutton;

	}

	public static MyFormRow tampilanScroll1(Component component) {
		Grid grid = createBaseGrid(component, "dgrid");
		return createBaseRow(grid);
	}

	public static MyFormRow tampilanScroll2(Component component) {
		Grid grid = createBaseGrid(component, "dgrid");

		Rows rows = new Rows();
		rows.setParent(grid);

		// Khusus MyRowStyled
		MyRowStyled row = new MyRowStyled();
		row.setValign("top");
		row.setParent(rows); 
		return row;
	}

	public static MyFormRow tampilanScroll3(Component component) {
		// Bedanya hanya di sclass "fgrid"
		Grid grid = createBaseGrid(component, "fgrid");
		return createBaseRow(grid);
	}

	public static MyFormRow tampilanScroll4(Component component) {
		Grid grid = createBaseGrid(component, "dgrid");

		// Khusus Columns
		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig col1 = new MyColumnConfig();
		col1.setParent(columns);
		col1.setWidth("30%");

		MyColumnConfig col2 = new MyColumnConfig();
		col2.setParent(columns); // Kolom kedua tanpa width spesifik

		return createBaseRow(grid);
	}

	/**
	 * Method untuk memotong teks pada komponen org.zkoss.zul.Label
	 */
	public static void applyReadMore(final Label label, final String isiLabel, final int limit) {
		if (isiLabel == null || isiLabel.isEmpty()) {
			label.setValue("");
			return;
		}

		if (isiLabel.length() <= limit) {
			label.setValue(isiLabel);
			return;
		}

		final String truncatedText = isiLabel.substring(0, limit) + "... (Baca)";
		final String expandedText = isiLabel + " (Tutup)";

		label.setValue(truncatedText);
		label.setTooltiptext(isiLabel); // Menampilkan full teks saat di-hover
		label.setStyle("cursor: pointer; color: #0050b3; transition: 0.3s;"); // Ubah kursor jadi telunjuk agar UX jelas

		label.addEventListener(Events.ON_CLICK, new EventListener() {
			boolean isExpanded = false;

			@Override
			public void onEvent(Event event) throws Exception {
				isExpanded = !isExpanded;
				if (isExpanded) {
					label.setValue(expandedText);
				} else {
					label.setValue(truncatedText);
				}
			}
		});
	}

	public static void applyReadMore(A anchor, String isiLabel) {
		applyReadMore(anchor, isiLabel, 15);
	}

	/**
	 * Method untuk memotong teks pada komponen org.zkoss.zul.A (Anchor/Link).
	 * Mencegah bentrok event dengan memisahkan tombol (Baca)/(Tutup) ke Anchor baru.
	 */
	public static void applyReadMore(final A anchor, final String isiLabel, final int limit) {
		if (isiLabel == null || isiLabel.isEmpty()) {
			anchor.setLabel("");
			return;
		}

		if (isiLabel.length() <= limit) {
			anchor.setLabel(isiLabel);
			return;
		}

		final String truncatedText = isiLabel.substring(0, limit) + "...";
		final String fullText = isiLabel;

		anchor.setLabel(truncatedText);
		anchor.setTooltiptext(fullText);

		// Cek apakah anchor utama sudah ditempelkan ke komponen Parent
		if (anchor.getParent() != null) {
			// Buat anchor terpisah khusus untuk aksi Buka/Tutup
			final A actionAnchor = new A(" (Baca)");
			actionAnchor.setStyle("cursor: pointer; color: #0050b3; margin-left: 5px; font-size: 0.9em; font-style: italic;");
			
			// Sisipkan anchor aksi tepat di sebelah kanan anchor utama
			anchor.getParent().insertBefore(actionAnchor, anchor.getNextSibling());

			actionAnchor.addEventListener(Events.ON_CLICK, new EventListener() {
				boolean isExpanded = false;

				@Override
				public void onEvent(Event event) throws Exception {
					isExpanded = !isExpanded;
					if (isExpanded) {
						anchor.setLabel(fullText);
						actionAnchor.setLabel(" (Tutup)");
					} else {
						anchor.setLabel(truncatedText);
						actionAnchor.setLabel(" (Baca)");
					}
				}
			});
		} else {
			// Fallback (jika method ini dipanggil sebelum anchor di-append ke parent)
			// Logika lama dipertahankan sebagai cadangan
			final String truncFallback = truncatedText + " (Baca)";
			final String expFallback = fullText + " (Tutup)";
			
			anchor.setLabel(truncFallback);
			anchor.addEventListener(Events.ON_CLICK, new EventListener() {
				boolean isExpanded = false;

				@Override
				public void onEvent(Event event) throws Exception {
					isExpanded = !isExpanded;
					if (isExpanded) {
						anchor.setLabel(expFallback);
					} else {
						anchor.setLabel(truncFallback);
					}
				}
			});
		}
	}
}