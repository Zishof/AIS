package ais.ui.util;

import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

/**
 * Tipe khusus untuk my doublebox min. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDoublebox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan
 * yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code init()}, {@code getValue}(). Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 *
 * @see MyDoublebox
 */
public class MyDoubleboxMin extends MyDoublebox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5925156244581127226L;

	public MyDoubleboxMin() {
		super();
		init();
	}

	public MyDoubleboxMin(Double value) throws WrongValueException {
		super(value);
		init();
	}

	private void init() {
		setStyle("text-align: right;");
		setFormat("#,##0.##");
		// setWidth("95%");

		addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				setValue(getValue());
			}
		});
	}

	public Double getValue() {
		return super.getValue() == null ? 0.0 : -Math.abs(super.getValue());
	}
}
