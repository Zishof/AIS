package ais.ui.util;

import org.zkoss.zk.ui.ext.AfterCompose;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.helper.BantuanGlobalHook;
import ais.common.Common;

/**
 * Toolbarbutton standar aplikasi.
 *
 * Penataan tampilan tombol aksi (Simpan, Batal, Keluar, dst) dilakukan lewat
 * sclass yang didefinisikan terpusat di /css/css_utama.css (blok "ais-btn").
 * Hindari inline style: pada ZK 5 inline style ikut diduplikasi ke elemen
 * .z-toolbarbutton-cnt sehingga hasil render menjadi tidak rapi.
 */
public class MyToolbarbuttonConfig extends Toolbarbutton implements AfterCompose {

	private static final long serialVersionUID = -8165594983232482912L;

	private static final String SCLASS_ICON_PUTIH = "ais-tbar-icon-white";
	private static final String SCLASS_FAB = "kb-fab-inline";
	private static final String SCLASS_TOMBOL = "ais-btn";
	private static final String SCLASS_TOMBOL_HIJAU = "ais-btn-hijau";
	private static final String SCLASS_TOMBOL_MERAH = "ais-btn-merah";
	private static final String SCLASS_TOMBOL_MERAH_TUA = "ais-btn-merah-tua";
	private static final String SCLASS_TOMBOL_BIRU = "ais-btn-biru";

	private String labelLokal = null;
	private boolean putihkanSvg = false;
	private boolean tombolBerwarna = false;

	public MyToolbarbuttonConfig() {
		super();
	}

	public MyToolbarbuttonConfig(String label, String image) {
		super(Common.getBahasaConfig(label), MyMenuitem.svgIcon(label, image));
		this.labelLokal = label;
		aturStyleBerdasarkanLabel(label);
		aturIconSvgPutihJikaPerlu();
	}

	public MyToolbarbuttonConfig(String label) {
		super(Common.getBahasaConfig(label));
		this.labelLokal = label;
		aturStyleBerdasarkanLabel(label);
		aturIconSvgPutihJikaPerlu();
	}

	public void setTooltiptext(String text) {
		super.setTooltiptext(Common.getBahasaConfig(text));
	}

	/**
	 * Dipanggil ZK setelah komponen selesai dikomposisi dari ZUL. Hanya tombol
	 * berlabel "Bantuan" yang terpengaruh; tombol lain dilewati apa adanya.
	 *
	 * <ul>
	 * <li>Sakelar induk {@code bantuan_tombol_tampil} tidak aktif → tombol
	 * disembunyikan ({@code setVisible(false)}), sejalan dengan tombol Bantuan
	 * mengambang di halaman ZK lain dan di JSP.</li>
	 * <li>Sakelar induk aktif dan {@code bantuan_tombol_global} aktif → tombol
	 * dipindahkan ke pojok kanan-bawah (floating) lewat sclass {@link #SCLASS_FAB}.</li>
	 * <li>Sakelar induk aktif dan {@code bantuan_tombol_global} tidak aktif → tombol
	 * tetap menyatu di toolbar seperti semula.</li>
	 * </ul>
	 *
	 * <p>Seluruh proses dibungkus try/catch agar tidak pernah mengganggu render halaman.</p>
	 */
	public void afterCompose() {
		try {
			if (isTombolBantuan()) {
				if (!BantuanGlobalHook.tombolBantuanAktif()) {
					setVisible(false);
				} else if (BantuanGlobalHook.gayaMengambangAktif()) {
					/* BARU 20-08-2026: dahulu tombol Bantuan inline hanya DIAPUNGKAN ke sudut lewat
					 * SCLASS_FAB, sehingga di sudut yang sama bisa ada DUA tombol -- tombol inline ini
					 * dan tombol "?" milik BantuanGlobalHook -- dan sekali klik langsung membuka
					 * panduan halaman tanpa jalan menuju Tanya Jawab atau Semua Panduan. Kini bila
					 * hook global sudah memasang tombol "?" (menunya kini selalu lengkap), tombol
					 * inline cukup disembunyikan. Bila hook TIDAK memasang apa pun -- misalnya berkas
					 * panduan halaman tidak tersedia -- gaya lama tetap dipakai agar bantuan tidak
					 * hilang sama sekali. */
					if (!pasangKebabBantuan()) {
						tambahSclass(SCLASS_FAB);
					}
				}
			}
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/ui/util/MyToolbarbuttonConfig.java:67");
			/* jangan pernah mengganggu render halaman */
		}
	}

	/**
	 * Ganti tombol Bantuan inline dengan tombol bulat "?" bermenu tiga pilihan.
	 *
	 * <p>Dipakai bila hook global TIDAK memasang tombol untuk halaman ini -- misalnya karena
	 * key hasil turunan nama berkas ZUL tidak punya berkas panduan, padahal tombol inline-nya
	 * menunjuk key panduan yang lain. Tanpa jalur ini, halaman tersebut jatuh ke gaya pil
	 * berlabel "Bantuan" yang sekali klik langsung membuka panduan, tanpa jalan ke Tanya
	 * Jawab maupun Semua Panduan.</p>
	 *
	 * <p>Item "Bantuan Halaman Ini" sengaja memicu ulang event tombol ini sendiri, bukan
	 * menebak key: {@code onClick} milik ZUL sudah membawa key panduan yang benar.</p>
	 *
	 * @return {@code true} bila kebab terpasang; {@code false} bila pemanggil harus memakai
	 *         gaya cadangan.
	 */
	private boolean pasangKebabBantuan() {
		try {
			final org.zkoss.zk.ui.Component induk = getParent();
			if (induk == null) {
				return false;
			}
			/* FIX 21-08-2026 -- BANTUAN SALAH HALAMAN.
			 *
			 * Dahulu tombol "?" dipasang ke PAGE dan diklaim sekali per halaman. Karena banyak
			 * layar dimuat sebagai TAB ke dalam satu Page yang sama, tab yang dibuka pertama
			 * merebut slotnya dan key panduannya terkunci -- membuka tab lain tetap menampilkan
			 * panduan tab pertama. Kini tombol dipasang pada wadah tombol inline ini sendiri,
			 * sehingga otomatis menjadi MILIK TAB INI dan ikut tersembunyi bersama tabnya.
			 *
			 * Hook global memasang FAB pada wadah tab sebelum isi tab selesai dibangun, jadi ia
			 * belum tahu tab ini punya tombol Bantuan sendiri. FAB bawaan hook karena itu dilepas
			 * lebih dulu supaya tidak ada dua tombol pada satu tab. */
			org.zkoss.zk.ui.Component wadahTab = BantuanGlobalHook.wadahTab(this);
			if (wadahTab != null) {
				BantuanGlobalHook.lepasFabDari(wadahTab);
			} else {
				// Layar dibuka sebagai halaman penuh: FAB bawaan hook menempel pada Page.
				BantuanGlobalHook.lepasFabDariHalaman(getPage());
			}

			/* Key diambil dari include TAB terdekat, bukan dari request path halaman -- request
			 * path selalu menunjuk halaman induk sehingga selalu salah untuk layar bertab. */
			String key = BantuanGlobalHook.keyDariKomponen(this);
			if (key == null || key.trim().length() == 0) {
				key = "panduan";
			}
			org.zkoss.zul.Div fab = BantuanGlobalHook.buatFab(key, true,
					new org.zkoss.zk.ui.event.EventListener() {
						@Override
						public void onEvent(org.zkoss.zk.ui.event.Event event) throws Exception {
							/* Item "Bantuan Halaman Ini" memicu ulang event tombol ini sendiri,
							 * sebab onClick milik ZUL sudah membawa key panduan yang benar. */
							org.zkoss.zk.ui.event.Events.sendEvent(new org.zkoss.zk.ui.event.Event(
									org.zkoss.zk.ui.event.Events.ON_CLICK, MyToolbarbuttonConfig.this));
						}
					});
			/* FIX 21-08-2026 -- POSISI TOMBOL SALAH.
			 * Sebelumnya FAB dipasang ke induk tombol inline, yaitu toolbar DI DALAM panel filter.
			 * Panel itu memakai autoscroll (overflow), sehingga tombol yang position:fixed ikut
			 * terpotong wadahnya dan tampak menempel di pojok panel, bukan mengambang di sudut
			 * layar. Karena itu FAB dipasang setinggi mungkin di pohon komponen namun TETAP di
			 * dalam tab: wadah tab lebih dulu, lalu halaman, dan induk tombol hanya sebagai
			 * pilihan terakhir. Urutan ini juga menjaga sifat per-tab yang memperbaiki bug
			 * "bantuan tidak sesuai halaman". */
			boolean terpasang = false;
			if (wadahTab != null) {
				try {
					fab.setParent(wadahTab);
					terpasang = true;
				} catch (Throwable wadahMenolak) {
					terpasang = false;
				}
			}
			if (!terpasang && getPage() != null) {
				try {
					fab.setPage(getPage());
					terpasang = true;
				} catch (Throwable halamanMenolak) {
					terpasang = false;
				}
			}
			if (!terpasang) {
				fab.setParent(induk);
			}
			setVisible(false);
			return true;
		} catch (Throwable t) {
			ais.common.ErrorAuditUtil.record(t, "MyToolbarbuttonConfig.pasangKebabBantuan");
			return false;
		}
	}

	private boolean isTombolBantuan() {
		String lbl = labelLokal;
		if (isEmpty(lbl)) {
			lbl = getLabel();
		}
		return lbl != null && "bantuan".equals(lbl.trim().toLowerCase());
	}

	public void setLabel(String text) {
		this.labelLokal = text;
		super.setLabel(Common.getBahasaConfig(text));
		aturStyleBerdasarkanLabel(text);
		aturIconSvgPutihJikaPerlu();
	}

	/**
	 * Setel label TANPA menerjemahkan — untuk DATA DINAMIS. Lihat {@link MyLabelConfig#setValueData(String)}.
	 */
	public MyToolbarbuttonConfig setLabelData(String text) {
		this.labelLokal = text;
		super.setLabel(text);
		aturIconSvgPutihJikaPerlu();
		return this;
	}

	@Override
	public void setImage(String src) {
		String lbl = labelLokal;

		if (isEmpty(lbl)) {
			lbl = getLabel();
		}

		if (isEmpty(lbl)) {
			lbl = getTooltiptext();
		}

		src = MyMenuitem.svgIcon(lbl, src, true);
		super.setImage(src);

		aturIconSvgPutihJikaPerlu();
	}

	private void aturStyleBerdasarkanLabel(String label) {
		String lower = toLower(label);

		if (isEmpty(lower)) {
			return;
		}

		if (equalsAny(lower, new String[] { "simpan", "save" })) {
			setTombolBerwarna(SCLASS_TOMBOL_HIJAU);

		} else if (equalsAny(lower, new String[] { "keluar", "logout", "log out" })) {
			setTombolBerwarna(SCLASS_TOMBOL_MERAH_TUA);

		} else if (equalsAny(lower, new String[] { "batal", "tutup", "selesai", "close", "cancel" })) {
			setTombolBerwarna(SCLASS_TOMBOL_MERAH);

		}
		// Catatan: tombol "Hitung Ulang" TIDAK lagi diberi warna biru khusus (SCLASS_TOMBOL_BIRU) —
		// permintaan user agar tampil SAMA seperti tombol toolbar lain (polos, ikon+teks), tanpa beda.
	}

	private void setTombolBerwarna(String sclassWarna) {
		this.putihkanSvg = true;
		this.tombolBerwarna = true;

		tambahSclass(SCLASS_TOMBOL);
		tambahSclass(sclassWarna);

		/* Ikon harus sejajar di kiri teks (bukan di atas teks). */
		super.setOrient("horizontal");

		aturIconSvgPutihJikaPerlu();
	}

	/**
	 * Banyak pemanggil menyetel orient "vertical" secara massal. Untuk tombol
	 * aksi berwarna (Simpan/Batal/Keluar) orient dipaksa tetap horizontal agar
	 * ikon dan label sejajar dan tombol tampil rapi.
	 */
	public void setOrient(String orient) {
		if (tombolBerwarna) {
			super.setOrient("horizontal");
		} else {
			super.setOrient(orient);
		}
	}

	private void aturIconSvgPutihJikaPerlu() {
		String image = null;

		try {
			image = getImage();
		} catch (Exception e) {
			image = null;
		}

		if (putihkanSvg && isSvg(image)) {
			tambahSclass(SCLASS_ICON_PUTIH);
		} else {
			hapusSclass(SCLASS_ICON_PUTIH);
		}
	}

	private boolean isSvg(String src) {
		if (isEmpty(src)) {
			return false;
		}

		String lower = src.toLowerCase();

		return lower.endsWith(".svg")
				|| lower.indexOf(".svg?") >= 0
				|| lower.indexOf(".svg#") >= 0
				|| lower.indexOf("image/svg+xml") >= 0;
	}

	private void tambahSclass(String namaClass) {
		if (isEmpty(namaClass)) {
			return;
		}

		String sclass = getSclass();

		if (isEmpty(sclass)) {
			setSclass(namaClass);
			return;
		}

		if (!punyaSclass(sclass, namaClass)) {
			setSclass(sclass + " " + namaClass);
		}
	}

	private void hapusSclass(String namaClass) {
		String sclass = getSclass();

		if (isEmpty(sclass) || isEmpty(namaClass)) {
			return;
		}

		String[] arr = sclass.split("\\s+");
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < arr.length; i++) {
			if (!namaClass.equals(arr[i])) {
				if (sb.length() > 0) {
					sb.append(" ");
				}
				sb.append(arr[i]);
			}
		}

		setSclass(sb.toString());
	}

	private boolean punyaSclass(String sclass, String namaClass) {
		if (isEmpty(sclass) || isEmpty(namaClass)) {
			return false;
		}

		String[] arr = sclass.split("\\s+");

		for (int i = 0; i < arr.length; i++) {
			if (namaClass.equals(arr[i])) {
				return true;
			}
		}

		return false;
	}

	private boolean equalsAny(String lower, String[] daftarKata) {
		if (isEmpty(lower) || daftarKata == null) {
			return false;
		}

		for (int i = 0; i < daftarKata.length; i++) {
			if (daftarKata[i] != null && lower.equalsIgnoreCase(daftarKata[i])) {
				return true;
			}
		}

		return false;
	}

	private String toLower(String text) {
		return text == null ? "" : text.trim().toLowerCase();
	}

	private boolean isEmpty(String text) {
		return text == null || text.trim().length() == 0;
	}
}
