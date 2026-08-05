package ais.ui.util;

import org.zkoss.zk.au.AuRequest;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zul.Tab;

/**
 * Pengganti drop-in {@link org.zkoss.zul.Tab} yang MENELAN error tampilan
 * <b>"Exactly one selected tab is required: []"</b>.
 *
 * <p>Error tersebut dilempar {@link org.zkoss.zul.Tab#service} ketika KLIEN mengirim event onSelect
 * untuk sebuah tab yang—di sisi server—tabbox/tabs-nya sudah KOSONG atau tab-nya sudah terlepas
 * (mis. isi tabbox dibangun ulang / semua tab ditutup, sementara klik lama dari klien baru tiba).
 * Ini murni race TAMPILAN dan tidak merusak data. Kelas ini identik dengan {@code Tab} biasa
 * (tanpa terjemahan label / perubahan perilaku lain) — hanya menambah guard tsb — sehingga aman
 * dipakai menggantikan {@code new Tab(...)}.</p>
 *
 * <p>Java 1.6/1.7, ZK 5.5.</p>
 */
public class MyTab extends Tab {

	private static final long serialVersionUID = -4471183209871125001L;

	public MyTab() {
		super();
	}

	public MyTab(String label) {
		super(label);
	}

	public MyTab(String label, String image) {
		super(label, image);
	}

	@Override
	public void service(AuRequest request, boolean everError) {
		try {
			super.service(request, everError);
		} catch (UiException e) {
			String msg = e.getMessage() == null ? "" : e.getMessage();
			if (msg.indexOf("Exactly one selected tab is required") >= 0) {
				return;
			}
			throw e;
		}
	}
}
