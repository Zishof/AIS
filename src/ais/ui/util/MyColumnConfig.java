package ais.ui.util;

import org.zkoss.zul.Column;

import ais.common.Common;

/**
 * Komponen/konfigurasi ZK khusus AIS untuk my column config. Tipe ini membakukan default dan
 * perilaku tampilan di atas komponen induk supaya layar tidak mengulang konfigurasi widget yang
 * sama.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Column}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah mutasi data ({@code setTooltiptext()}, {@code setLabel()}, {@code
 * setWidth()}, {@code setWidthData()}, {@code setLabelData()}); operasi domain lain ({@code
 * terapkanTooltipDefault()}, {@code terapkanUkuranAksi()}, {@code adalahLabelAksi()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> setter dan helper mengubah state komponen ZK yang sedang terpasang pada desktop.
 * Gunakan pada event thread UI dan jangan membagikan instance antar session; aturan bisnis dan transaksi
 * persistence tetap harus didelegasikan ke action atau service pemanggil.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see Column
 */
public class MyColumnConfig extends Column {

	private static final long serialVersionUID = -8165594983232482912L;

	public MyColumnConfig() {
		super();
	}

	/**
	 * Konstruktor cerdas untuk menentukan apakah parameter kedua berupa gambar atau ukuran lebar (width).
	 */
	public MyColumnConfig(String label, String imageOrWidth) {
		// Set label terlebih dahulu
		super(Common.getBahasaConfig(label));
		terapkanTooltipDefault(label);

		if (imageOrWidth != null && !imageOrWidth.trim().isEmpty()) {
			String val = imageOrWidth.trim().toLowerCase();

			// 1. Cek jika mengandung persentase (%) atau pixel (px), set sebagai Width
			if (val.contains("%") || val.contains("px")) {
				this.setWidth(imageOrWidth);
			} 
			// 2. Cek jika mengandung unsur gambar (ekstensi atau path folder img), set sebagai Image
		else if (val.contains(".png") || val.contains(".jpg") || val.contains(".jpeg") 
					|| val.contains(".gif") || val.contains(".svg") || val.contains("/img/")) {
				this.setImage(imageOrWidth);
			}
			// 3. Jika tidak keduanya, maka abaikan parameter tersebut
		}
		terapkanUkuranAksi(label);
	}

	public MyColumnConfig(String label) {
		super(Common.getBahasaConfig(label));
		terapkanTooltipDefault(label);
		terapkanUkuranAksi(label);
	}

	@Override
	public void setTooltiptext(String text) {
		super.setTooltiptext(Common.getBahasaConfig(text));
	}

	@Override
	public void setLabel(String text) {
		super.setLabel(Common.getBahasaConfig(text));
		terapkanTooltipDefault(text);
		terapkanUkuranAksi(text);
	}

	/**
	 * Berikan tooltip bawaan pada seluruh header tabel. Ini penting untuk kolom
	 * sempit/dinamis yang labelnya dipotong CSS (misalnya PL..., CPL..., dan kode
	 * Bahan Kajian). Pemanggil tetap dapat menggantinya dengan tooltip yang lebih
	 * lengkap melalui {@link #setTooltiptext(String)}.
	 */
	private void terapkanTooltipDefault(String label) {
		if ((getTooltiptext() == null || getTooltiptext().trim().length() == 0)
				&& label != null && label.trim().length() > 0) {
			super.setTooltiptext(Common.getBahasaConfig(label));
		}
	}

	@Override
	public void setWidth(String width) {
		// Definisi ZUL/Action lama banyak memakai 8%–20% atau 100–240px untuk
		// kolom Aksi. Setelah tombol dipindahkan ke menu kebab, lebar sebesar itu
		// hanya menghasilkan ruang kosong. Paksa satu ukuran baku untuk seluruh CRUD.
		if (adalahLabelAksi(getLabel())) {
			super.setWidth(GridKolomHelper.LEBAR_KOLOM_AKSI);
			super.setAlign("center");
			return;
		}
		super.setWidth(width);
	}

	private void terapkanUkuranAksi(String label) {
		if (adalahLabelAksi(label) || adalahLabelAksi(getLabel())) {
			super.setWidth(GridKolomHelper.LEBAR_KOLOM_AKSI);
			super.setAlign("center");
			String sc = getSclass() == null ? "" : getSclass();
			if (sc.indexOf("ais-action-column") < 0) {
				super.setSclass((sc + " ais-action-column").trim());
			}
		}
	}

	private static boolean adalahLabelAksi(String label) {
		if (label == null) return false;
		String value = label.trim().toLowerCase();
		return "aksi".equals(value) || "action".equals(value)
				|| value.startsWith("aksi ") || value.startsWith("action ");
	}

	public MyColumnConfig setWidthData(String val) {
		super.setWidth(val);
		return this;
	}

	/**
	 * Setel label TANPA menerjemahkan — untuk DATA DINAMIS. Lihat {@link MyLabelConfig#setValueData(String)}.
	 */
	public MyColumnConfig setLabelData(String text) {
		super.setLabel(text);
		terapkanTooltipDefault(text);
		return this;
	}

}
