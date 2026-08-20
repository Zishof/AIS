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
					tambahSclass(SCLASS_FAB);
				}
			}
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/ui/util/MyToolbarbuttonConfig.java:67");
			/* jangan pernah mengganggu render halaman */
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
