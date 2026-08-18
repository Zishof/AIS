package ais.ui.util;

import org.zkoss.zul.Column;

import ais.common.Common;

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
		terapkanUkuranAksi(label);
	}

	@Override
	public void setTooltiptext(String text) {
		super.setTooltiptext(Common.getBahasaConfig(text));
	}

	@Override
	public void setLabel(String text) {
		super.setLabel(Common.getBahasaConfig(text));
		terapkanUkuranAksi(text);
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
		return this;
	}

}
