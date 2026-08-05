package ais.ui.util;

import org.zkoss.zul.Caption;
import org.zkoss.zul.Html;

public class MyCaptionStyled extends Caption {

	private static final long serialVersionUID = 6482108245021185374L;

	public MyCaptionStyled() {
		super();
		super.setStyle(
				"font-size:12px;font-weight: bolder;text-decoration: none;color:black;border: 1px solid black;\r\n"
						+ "  padding: 5px;  border-radius: 5px 15px;");
	}

	public MyCaptionStyled(String val) {
		super();
		super.setLabel(val);
		super.setStyle(
				"font-size:12px;font-weight: bolder;text-decoration: none;color:black;border: 1px solid black;\r\n"
						+ "  padding: 5px;  border-radius: 5px 15px;");
	}

	/**
	 * Konstruktor khusus ZK 5.5 untuk mendukung teks label beserta ikon Font-Awesome.
	 * Menggunakan komponen org.zkoss.zul.Html agar tag <i> tidak ter-escape (menjadi plain text).
	 * * @param val Teks label caption
	 * @param fontAwesome Class Font-Awesome (contoh: "fas fa-chart-bar")
	 */
	public MyCaptionStyled(String val, String fontAwesome) {
		super();
		
		// 1. Kosongkan label bawaan agar tidak menimpa elemen HTML
		super.setLabel(""); 
		
		// 2. Buat elemen HTML murni berisi Ikon Font-Awesome dan Teks
		Html htmlElement = new Html("<i class='" + fontAwesome + "'></i> <span style='margin-left:5px;'>" + val + "</span>");
		
		// 3. Sisipkan sebagai "anak" (child) dari komponen Caption ini
		this.appendChild(htmlElement); 
		
		super.setStyle(
				"font-size:12px;font-weight: bolder;text-decoration: none;color:black;border: 1px solid black;\r\n"
						+ "  padding: 5px;  border-radius: 5px 15px;");
	}

}