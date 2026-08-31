package ais.action.master.bkd;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Tabpanel;

import ais.action.report.bkd.LaporanPerinkatBkdWindow;
import ais.action.report.bkd.LaporanPerinkatSemuaBkdWindow;
import ais.action.report.bkd.LaporanSummaryBkdWindow;
import ais.common.Common;
import ais.common.CommonPrivilages;

/**
 * Controller/action ZK untuk laporan bkd. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal: {@code ringkasan}, {@code peringkat}, {@code
 * peringkatSemua}; operasi lokal: {@code onPeringkat()}, {@code onPeringkatSemua()}, {@code tampilRingkasan()},
 * {@code doAfterCompose}(). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class LaporanBKDAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private Tabpanel ringkasan;
	private Tabpanel peringkat;
	private Tabpanel peringkatSemua;

	public void onPeringkat(Event event) throws Exception {
		if (peringkat.getChildren().isEmpty()) {
			LaporanPerinkatBkdWindow bkdWindow = new LaporanPerinkatBkdWindow();
			bkdWindow.setHeight("100%");
			bkdWindow.setWidth("100%");
			peringkat.appendChild(bkdWindow);
		}
	}

	public void onPeringkatSemua(Event event) throws Exception {
		if (peringkatSemua.getChildren().isEmpty()) {
			LaporanPerinkatSemuaBkdWindow bkdWindow = new LaporanPerinkatSemuaBkdWindow();
			bkdWindow.setHeight("100%");
			bkdWindow.setWidth("100%");
			peringkatSemua.appendChild(bkdWindow);
		}
	}

	public void tampilRingkasan() throws Exception {
		if (ringkasan.getChildren().isEmpty()) {
			LaporanSummaryBkdWindow bkdWindow = new LaporanSummaryBkdWindow();
			bkdWindow.setHeight("100%");
			bkdWindow.setWidth("100%");
			ringkasan.appendChild(bkdWindow);
		}
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		tampilRingkasan();

		// eventListener.onEvent(null);

	}

}
