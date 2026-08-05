package ais.ui.util;

import org.zkoss.zul.Window;

import ais.common.Common;

/**
 * <h2>Window yang MENERJEMAHKAN judul (title) tanpa efek samping visual.</h2>
 *
 * <p>Berbeda dari {@link MyWindow} (yang juga memasang background image kop + sclass standar dan
 * lebar dialog default), kelas ini HANYA menerjemahkan atribut {@code title} lewat
 * {@link Common#getBahasaConfig(String)}. Cocok untuk window popup (mis. {@code addWindow}
 * "Tambah/Ubah ...") yang butuh judul multi-bahasa TANPA mengubah tampilan/perilaku lain.</p>
 *
 * <p>Hanya teks STATIS yang boleh diterjemahkan; untuk judul berisi DATA DINAMIS gunakan
 * {@link #setTitleData(String)} agar tidak diterjemah.</p>
 */
public class MyWindowJudul extends Window {

	private static final long serialVersionUID = -8165594983232482912L;

	public MyWindowJudul() {
		super();
	}

	public MyWindowJudul(String title, String border, boolean closable) {
		super(Common.getBahasaConfig(title), border, closable);
	}

	@Override
	public void setTitle(String title) {
		super.setTitle(Common.getBahasaConfig(title));
	}

	/**
	 * Setel judul TANPA menerjemahkan — untuk DATA DINAMIS. Lihat {@link MyLabelConfig#setValueData(String)}.
	 */
	public MyWindowJudul setTitleData(String title) {
		super.setTitle(title);
		return this;
	}
}
