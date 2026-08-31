package ais.ui.util;

import org.zkoss.zul.Script;

/**
 * Tipe khusus untuk check for parent script. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Script}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Tipe ini sengaja tidak menambah state maupun operasi publik. Keberadaannya bukan duplikasi implementasi:
 * nama kelas dipakai sebagai penanda variasi untuk konfigurasi, binding ZK/SOAP, dependency lookup, atau
 * pemilihan perilaku polimorfik. Karena itu jangan menyalin method dari kelas induk ke sini kecuali kontraknya
 * memang berbeda.</p>
 *
 * @see Script
 */
public class CheckForParentScript extends Script {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5411499273013627526L;

	public CheckForParentScript() {
		super();
//		setContent("<SCRIPT language=\"JavaScript\">window.onbeforeunload = function() { return \"Your work will be lost.\"; };</SCRIPT>");
//		setContent("var isInIFrame = (window.location != window.parent.location && window.name != 'main') ? true : false;if(isInIFrame){parent.location.reload();}");
	}

}
