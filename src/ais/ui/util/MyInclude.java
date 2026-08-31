package ais.ui.util;

import org.zkoss.zul.Include;

/**
 * Tipe khusus untuk my include. Kelas ini memberi nama dan batas tanggung jawab yang eksplisit
 * pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Include}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code init()}, {@code setSrc}(). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 *
 * @see Include
 */
public class MyInclude extends Include {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8735575580753564474L;

	public MyInclude() {
		super();
		init();
	}

	public MyInclude(String src) {
		super(src != null && src.contains("WEB-INF") ? src : "/WEB-INF/z/x/y/" + src);
		init();
	}

	private void init() {
		setHeight("100%");
		setWidth("100%");
		/*
		 * Pada ZK lama Include dapat dirender sebagai elemen inline. Dalam kondisi
		 * itu width="100%" diabaikan browser dan isi hanya memakai lebar intrinsik,
		 * sehingga tab/popup menyisakan ruang kosong. Jadikan perilaku penuh sebagai
		 * default seluruh MyInclude; pemanggil tetap dapat menimpa style/ukuran bila
		 * memang membutuhkan tampilan khusus.
		 */
		setStyle("display:block;max-width:100%;box-sizing:border-box;");
	}

	@Override
	public void setSrc(String src) {
		// TODO Auto-generated method stub
		super.setSrc(src != null && src.contains("WEB-INF") ? src : "/WEB-INF/z/x/y/" + src);
	}

}
