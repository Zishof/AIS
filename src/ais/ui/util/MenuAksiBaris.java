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
 * <p><b>Tombol aslinya TIDAK dibuang.</b> Ia dipindahkan langsung ke konten popup dalam pohon
 * komponen. Dengan begitu seluruh {@code EventListener} yang sudah terpasang tetap hidup —
 * termasuk yang menutup jendela, menyegarkan grid, atau membuka konfirmasi.</p>
 *
 * <p><b>Visibilitas dan status nonaktif diwarisi apa adanya.</b> Pada ZK, {@code setVisible}
 * di renderer hampir selalu berarti HAK AKSES ({@code button.setVisible(edit)}), bukan sekadar
 * "sedang tidak berlaku". Karena itu butir yang tombolnya tersembunyi ikut tersembunyi, bukan
 * diredupkan — meredupkannya akan memberi tahu pengguna tentang kewenangan yang memang bukan
 * miliknya. Ini berbeda dari versi POS/JSP, dan perbedaannya disengaja.</p>
 *
 * <p><b>Satu aksi tetap menjadi satu menu.</b> Kolom aksi harus mempunyai tepat satu pemicu
 * yang konsisten; karena itu satu tombol pun tetap dibungkus ke menu kebab.</p>
 */
public final class MenuAksiBaris {

	private MenuAksiBaris() {
	}

	/** Lihat {@link #pasang(org.zkoss.zul.Hbox, String)}; judul tombolnya "Aksi lain". */
	public static void pasang(org.zkoss.zul.Hbox toolbar) {
		pasangInternal(toolbar, "Aksi lain", true);
	}

	/**
	 * Ubah isi {@code toolbar} menjadi satu tombol "…" beserta menunya.
	 *
	 * @param toolbar wadah yang sudah berisi tombol aksi; boleh {@code null} (tidak melakukan apa pun)
	 * @param judul   tooltip tombol "…"
	 */
	public static void pasang(org.zkoss.zul.Hbox toolbar, String judul) {
		pasangInternal(toolbar, judul, true);
	}

	/**
	 * Selalu ringkas aksi menjadi tombol kebab, termasuk ketika hanya ada satu aksi.
	 * Cocok untuk kolom aksi sempit yang harus konsisten pada setiap baris.
	 */
	public static void pasangSelalu(org.zkoss.zul.Hbox toolbar) {
		pasangInternal(toolbar, "Aksi lain", true);
	}

	/** Lihat {@link #pasangSelalu(org.zkoss.zul.Hbox)} dengan tooltip khusus. */
	public static void pasangSelalu(org.zkoss.zul.Hbox toolbar, String judul) {
		pasangInternal(toolbar, judul, true);
	}

	private static void pasangInternal(org.zkoss.zul.Hbox toolbar, String judul, boolean selaluKebab) {
		if (toolbar == null) {
			return;
		}
		java.util.List<org.zkoss.zul.Toolbarbutton> tombol = new java.util.ArrayList<org.zkoss.zul.Toolbarbutton>();
		for (Object anak : new java.util.ArrayList<Object>(toolbar.getChildren())) {
			if (anak instanceof org.zkoss.zul.Toolbarbutton) {
				tombol.add((org.zkoss.zul.Toolbarbutton) anak);
			}
		}
		if (tombol.isEmpty() || (!selaluKebab && tombol.size() < 2)) {
			return;
		}

		/* Gunakan struktur popup yang sama dengan normalizer global UIHelper. Dengan satu format,
		 * tombol yang ditambahkan setelah pemanggilan ini dapat langsung diserap ke popup yang
		 * sudah ada dan tidak membentuk ikon/menu kedua pada kolom Aksi. */
		if (toolbar.getAttribute("ais_row_actions_popup") == null) {
			UIHelper.wrapKebab(toolbar);
		}
	}
}
