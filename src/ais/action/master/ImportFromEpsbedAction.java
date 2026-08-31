package ais.action.master;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Label;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import ais.ui.util.MyToolbarbuttonConfig;

import ais.action.master.helper.impor.ImportFromEpsbedHelper;
import ais.common.Common;

/**
 * Controller/action ZK untuk import from epsbed. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal: {@code directory}, {@code progressmeter}, {@code
 * progressmeterChild}, {@code labelProses}, {@code button}, {@code timer}; operasi lokal: {@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code onImport}(). Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class ImportFromEpsbedAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4908026432590924291L;

	private Textbox directory;
	private Progressmeter progressmeter;
	private Progressmeter progressmeterChild;
	private Label labelProses;
	private MyToolbarbuttonConfig button;

	private Timer timer;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(
			org.zkoss.zk.ui.Page page, org.zkoss.zk.ui.Component parent,
			org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

	}

	public void onImport(Event event) {

		final Progressmeter myProgressmeter = new Progressmeter();
		final Progressmeter myProgressmeterChild = new Progressmeter();
		final Label myLabelProses = new Label();
		final String dir = directory.getValue().trim();

		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				ImportFromEpsbedHelper.importData(dir, myProgressmeter,
						myProgressmeterChild, myLabelProses);
			}
		};

		new Thread(runnable).start();

		timer = new Timer(1000);
		page.getFirstRoot().appendChild(timer);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				button.setDisabled(true);
				if (myProgressmeter.getValue() == 100) {
					button.setDisabled(false);
					MyMessageboxConfig.show("Import data berhasil dilakukan",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);

					timer.detach();
				}

				// System.out
				// .println("======================================================");
				progressmeter.setValue(myProgressmeter.getValue());
				progressmeterChild.setValue(myProgressmeterChild.getValue());
				labelProses.setValue(myLabelProses.getValue());
			}
		});
		timer.setRepeats(true);
		timer.start();

	}

}
