package ais.action.maintenance;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.zkoss.web.Attributes;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyInclude;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.DaftarPenggunaOnline;
import ais.action.master.helper.UserOnlineCounter;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.InitMenuHelper;
import ais.delivery.email.sender.MailHelper;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk login. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyInclude iframe}, {@code MyWindow
 * addWindow}, {@code MyToolbarbuttonConfig usersOnline}; inisialisasi/lifecycle ({@code doAfterCompose()});
 * mutasi data ({@code onUpdateUserOnline()}); operasi domain lain ({@code onLihatOnline()}, {@code
 * onClickPMB()}, {@code onClickLoginPMB()}, {@code onClickHome()}, {@code onClickMahasiswa()}, {@code
 * onClickPMDK()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class LoginAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 187958355469911830L;
	private MyInclude iframe;
	private MyWindow addWindow;

	private MyToolbarbuttonConfig usersOnline;

	public void doAfterCompose(Component comp) throws Exception {

		if (ConstantValues.AKTIF == null) {
			ConstantValues.hasbeeninit = false;
			ConstantValues.init();
		}

		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.REAL_PATH = session.getWebApp().getRealPath("/");
		CommonMedia.getMediaDirectory();
		Common.REAL_PATH_REPORT_TEMP = session.getWebApp().getRealPath("/report");

		Sessions.getCurrent().setAttribute("langSession", "in");
		Common.setUserAccess((HttpServletRequest) execution.getNativeRequest());
		Common.initLaguage();
		if (session.getAttribute("usersTemp") != null) {
			execution.sendRedirect(execution.getContextPath() + "/main");
			return;
		}

		Common.ROOT = execution.getContextPath();

		try { // refleksi: org.zkoss.util.logging.Log hanya ada di ZK 5
			Class<?> log = Class.forName("org.zkoss.util.logging.Log");
			Object l = log.getMethod("lookup", new Class[] { String.class }).invoke(null, new Object[] { "org.zkoss.io.serializable" });
			log.getMethod("setLevel", new Class[] { String.class }).invoke(l, new Object[] { "ERROR" });
		} catch (Throwable e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/LoginAction.java:68");
		}
		if (session != null) { session.setAttribute(Attributes.PREFERRED_LOCALE, new Locale("in", "ID")); }
		session.removeAttribute("usersTemp");

		Common.encripSemua();
		Common.setting();

		// settingHeader();

		onUpdateUserOnline(null);

//		page.getFirstRoot().appendChild(new CheckForParentScript());
		InitMenuHelper.initMenu();

		System.out.println("java.io.tmpdir => " + System.getProperty("java.io.tmpdir"));
	}

	public void onLihatOnline(Event event) throws Exception {
		DaftarPenggunaOnline window = new DaftarPenggunaOnline();
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
		window.setVisible(true);
		window.onModal();
	}

	public void onClickPMB() throws Exception {
		// iframe.setSrc("pmb.zul");
		execution.sendRedirect("pmb.zul");
	}

	public void onClickLoginPMB() throws Exception {
		iframe.setSrc("/pages/master/login_calon_mahasiswa.zul");
	}

	public void onClickHome() throws Exception {
		iframe.setSrc("/pages/main/blank.zul");
	}

	public void onClickMahasiswa() throws Exception {

		final MyWindow window = new MyWindow("Catatan Pembayaran Mahasiswa", "none", true);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight("90%");
		window.setWidth("900px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		Center center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		final MyInclude include = new MyInclude();
		include.setParent(center);
		include.setWidth("100%");
		include.setHeight("100%");
		include.setSrc("/pages/master/kegiatan_per_mahasiswa.zul");

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		button.setTooltiptext("Tutup");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}

		});
		button.setParent(toolbar);
		toolbar.setParent(south);

		window.setVisible(true);
		window.onModal();

		// iframe.setSrc("/pages/master/kegiatan_per_mahasiswa.zul");
	}

	public void onClickPMDK() throws Exception {
		iframe.setSrc("/pages/master/pendaftaranPMDK.zul");
	}

	public void onUpdateUserOnline(Event e) {
		try {

			if (UserOnlineCounter.count == null || UserOnlineCounter.countOnline == null) {
				UserOnlineCounter.check();
			}

			String result = "Access: " + Common.numberFormat.get().format(UserOnlineCounter.count) + ", logged in: "
					+ Common.numberFormat.get().format(UserOnlineCounter.countOnline);
			usersOnline.setLabel(result);

		} catch (Exception e1) {
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/maintenance/LoginAction.java:166");
		}
	}

	public void onForgotPassword() {
		MailHelper mailHelper = new MailHelper();
		mailHelper.tampil(addWindow);

	}

}
