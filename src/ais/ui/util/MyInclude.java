package ais.ui.util;

import org.zkoss.zul.Include;

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
