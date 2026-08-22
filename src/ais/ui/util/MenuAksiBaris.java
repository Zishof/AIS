package ais.ui.util;

/**
 * <h3>MenuAksiBaris — meringkas deretan tombol aksi baris menjadi satu tombol "…"</h3>
 *
 * <p><b>Mengapa ada.</b> Baris grid CRUD di sini lazim memuat tiga sampai tujuh
 * {@link MyToolbarbuttonConfig} berjajar di dalam satu {@code Hbox}. Tiga masalahnya nyata:
 * kolom aksi memakan lebar yang seharusnya milik data, ikon tanpa label hanya dapat ditebak
 * artinya karena {@code tooltiptext} tidak muncul di layar sentuh, dan target kliknya terlalu
 * rapat sehingga salah tekan mudah terjadi — berbahaya ketika salah satunya Hapus.</p>
 *
 * <p><b>Cara pakai.</b> Satu baris tambahan pada renderer yang sudah ada, dipanggil SESUDAH
 * seluruh tombol dipasang ke {@code Hbox}-nya:</p>
 *
 * <pre>
 * Hbox toolbar = new Hbox();
 * ... buat dan pasang tombol seperti biasa ...
 * MenuAksiBaris.pasang(toolbar);
 * toolbar.setParent(row);
 * </pre>
 *
 * <p><b>Tombol aslinya TIDAK dibuang.</b> Ia dipindahkan ke wadah tersembunyi yang tetap berada
 * di pohon komponen, lalu diklik ulang lewat {@code Events.postEvent} ketika butir menunya
 * dipilih. Dengan begitu seluruh {@code EventListener} yang sudah terpasang padanya tetap
 * hidup — termasuk yang menutup jendela, menyegarkan grid, atau membuka konfirmasi. Menyalin
 * listener-nya ke butir menu akan melahirkan salinan kedua yang lambat laun berbeda dari
 * aslinya tanpa ada yang menyadarinya.</p>
 *
 * <p><b>Visibilitas dan status nonaktif diwarisi apa adanya.</b> Pada ZK, {@code setVisible}
 * di renderer hampir selalu berarti HAK AKSES ({@code button.setVisible(edit)}), bukan sekadar
 * "sedang tidak berlaku". Karena itu butir yang tombolnya tersembunyi ikut tersembunyi, bukan
 * diredupkan — meredupkannya akan memberi tahu pengguna tentang kewenangan yang memang bukan
 * miliknya. Ini berbeda dari versi POS/JSP, dan perbedaannya disengaja.</p>
 *
 * <p><b>Kurang dari dua tombol dibiarkan apa adanya.</b> Menu yang isinya satu butir hanya
 * menyembunyikan satu-satunya aksi di balik klik tambahan.</p>
 */
public final class MenuAksiBaris {

	/** Karakter elipsis horizontal (U+22EF), sama dengan yang dipakai versi POS dan JSP. */
	private static final String LABEL_TOMBOL = "⋯";

	private MenuAksiBaris() {
	}

	/** Lihat {@link #pasang(org.zkoss.zul.Hbox, String)}; judul tombolnya "Aksi lain". */
	public static void pasang(org.zkoss.zul.Hbox toolbar) {
		pasang(toolbar, "Aksi lain");
	}

	/**
	 * Ubah isi {@code toolbar} menjadi satu tombol "…" beserta menunya.
	 *
	 * @param toolbar wadah yang sudah berisi tombol aksi; boleh {@code null} (tidak melakukan apa pun)
	 * @param judul   tooltip tombol "…"
	 */
	public static void pasang(org.zkoss.zul.Hbox toolbar, String judul) {
		if (toolbar == null) {
			return;
		}
		java.util.List<org.zkoss.zul.Toolbarbutton> tombol = new java.util.ArrayList<org.zkoss.zul.Toolbarbutton>();
		for (Object anak : new java.util.ArrayList<Object>(toolbar.getChildren())) {
			if (anak instanceof org.zkoss.zul.Toolbarbutton) {
				tombol.add((org.zkoss.zul.Toolbarbutton) anak);
			}
		}
		if (tombol.size() < 2) {
			return;
		}

		final org.zkoss.zul.Menupopup menu = new org.zkoss.zul.Menupopup();
		menu.setSclass("ais-menu-aksi-baris");

		// Wadah tersembunyi: tombol aslinya tetap di pohon komponen supaya listener-nya hidup.
		org.zkoss.zul.Hbox gudang = new org.zkoss.zul.Hbox();
		gudang.setVisible(false);

		int tampil = 0;
		for (int i = 0; i < tombol.size(); i++) {
			final org.zkoss.zul.Toolbarbutton asli = tombol.get(i);
			org.zkoss.zul.Menuitem butir = new org.zkoss.zul.Menuitem(labelDari(asli));
			String gambar = asli.getImage();
			if (gambar != null && gambar.length() > 0) {
				butir.setImage(gambar);
			}
			butir.setVisible(asli.isVisible());
			butir.setDisabled(asli.isDisabled());
			if (asli.isVisible()) {
				tampil++;
			}
			butir.addEventListener("onClick", new org.zkoss.zk.ui.event.EventListener() {
				@Override
				public void onEvent(org.zkoss.zk.ui.event.Event event) throws Exception {
					// Diteruskan ke tombol ASLINYA, bukan dikerjakan ulang di sini.
					org.zkoss.zk.ui.event.Events.postEvent(
							new org.zkoss.zk.ui.event.Event("onClick", asli));
				}
			});
			menu.appendChild(butir);
			asli.setParent(gudang);
		}

		/* Tidak ada satu pun aksi yang boleh dipakai pengguna ini — tombolnya tidak ditampilkan
		 * sama sekali daripada membuka menu yang seluruh isinya tersembunyi. Tombol aslinya tetap
		 * dipindah ke gudang supaya kolomnya benar-benar kosong, bukan menyisakan ikon berjajar. */
		if (tampil == 0) {
			gudang.setParent(toolbar);
			return;
		}

		final org.zkoss.zul.Toolbarbutton pemicu = new org.zkoss.zul.Toolbarbutton(LABEL_TOMBOL);
		pemicu.setTooltiptext(ais.common.Common.getBahasaConfig(judul));
		pemicu.setSclass("ais-btn-aksi-baris");
		pemicu.addEventListener("onClick", new org.zkoss.zk.ui.event.EventListener() {
			@Override
			public void onEvent(org.zkoss.zk.ui.event.Event event) throws Exception {
				menu.open(pemicu, "after_start");
			}
		});

		/* Menu di-parent ke AKAR HALAMAN, bukan ke toolbar. Baris grid kerap berada di dalam
		 * wadah bergulir; menu yang tinggal di dalamnya akan terpotong oleh overflow wadah itu.
		 * Idiom yang sama sudah dipakai dropdown profil di MainAction. Bila halamannya belum
		 * tersedia (komponen belum terpasang), menu menumpang pada toolbar — masih berfungsi,
		 * hanya berpotensi terpotong pada grid yang bergulir. */
		org.zkoss.zk.ui.Page halaman = toolbar.getPage();
		if (halaman != null && halaman.getFirstRoot() != null) {
			menu.setParent(halaman.getFirstRoot());
		} else {
			menu.setParent(toolbar);
		}

		pemicu.setParent(toolbar);
		gudang.setParent(toolbar);
	}

	/**
	 * Label butir menu. {@code tooltiptext} didahulukan karena di renderer sinilah arti tombol
	 * ditulis — tombolnya sendiri hampir selalu berlabel kosong dan hanya bergambar ikon.
	 */
	private static String labelDari(org.zkoss.zul.Toolbarbutton tombol) {
		String tip = tombol.getTooltiptext();
		if (tip != null && tip.trim().length() > 0) {
			return tip;
		}
		String label = tombol.getLabel();
		if (label != null && label.trim().length() > 0) {
			return label;
		}
		return ais.common.Common.getBahasaConfig("Aksi");
	}
}
