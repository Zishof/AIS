package ais.ui.util;

import org.zkoss.zul.Window;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.database.model.PerguruanTinggi;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

/**
 * Komponen/konfigurasi ZK khusus AIS untuk my window. Tipe ini membakukan default dan perilaku
 * tampilan di atas komponen induk supaya layar tidak mengulang konfigurasi widget yang sama.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Window}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code boolean pakaiClose}, {@code boolean
 * headless}; inisialisasi/lifecycle ({@code init()}, {@code initBg()}); mutasi data ({@code setVisible()},
 * {@code setTitle()}, {@code setTitleData()}, {@code setWidth()}); operasi domain lain ({@code
 * applyDefaultSclass()}, {@code applyDefaultDialogWidth()}, {@code onModal()}, {@code detach()}). Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> setter dan helper mengubah state komponen ZK yang sedang terpasang pada desktop.
 * Gunakan pada event thread UI dan jangan membagikan instance antar session; aturan bisnis dan transaksi
 * persistence tetap harus didelegasikan ke action atau service pemanggil.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see Window
 */
public class MyWindow extends Window {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3794161371195657634L;

	public static boolean pakaiClose = true;
	private boolean headless;

	public MyWindow() {
		super();
		init();
	}

	/**
	 * Window tidak ter-render untuk lifecycle Action yang dipanggil New UI.
	 * Melewati inisialisasi background/institusi yang hanya relevan untuk dialog ZK.
	 */
	public MyWindow(boolean headless) {
		super();
		this.headless = headless;
		if (!headless) init();
	}

	public MyWindow(String title, String border, boolean closable) {
		super(Common.getBahasaConfig(title), border, closable);
		init();
	}

	private void init() {
		initBg();
		applyDefaultSclass();
		addEventListener(Events.ON_CREATE, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				applyDefaultDialogWidth();
			}
		});
		if (pakaiClose) {
			setClosable(true);
//			addEventListener(Events.ON_CLOSE, new EventListener() {
//
//				@Override
//				public void onEvent(Event arg0) throws Exception {
//					MyWindow.this.setVisible(false);
//				}
//			});

		}

	}

	private void applyDefaultSclass() {
		try {
			String sclass = getSclass();
			if (sclass == null || sclass.trim().length() == 0) {
				setSclass("ais-standard-window ais-mywindow");
			} else if ((" " + sclass + " ").indexOf(" ais-mywindow ") < 0) {
				setSclass(sclass + " ais-standard-window ais-mywindow");
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/MyWindow.java:66");
		}
	}

	private void applyDefaultDialogWidth() {
		try {
			String width = getWidth();
			if (width != null && width.trim().length() > 0) {
				return;
			}
			String mode = getMode();
			if (mode == null || mode.trim().length() == 0 || "embedded".equalsIgnoreCase(mode.trim())) {
				return;
			}
			super.setWidth(Common.isMobile() ? "100%" : "75%");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/MyWindow.java:81");
		}
	}

	@Override
	public boolean setVisible(boolean visible) {
		if (headless) return false;
		if (visible) {
			applyDefaultDialogWidth();
		}
		return super.setVisible(visible);
	}

	@Override
	public void onModal() throws InterruptedException {
		if (headless) return;
		if (getPage() == null && org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrentCtrl() != null
				&& org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrentCtrl().getCurrentPage() != null) {
			setPage(org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrentCtrl().getCurrentPage());
		}
		if (getPage() == null && org.zkoss.zk.ui.Executions.getCurrent() != null
				&& org.zkoss.zk.ui.Executions.getCurrent().getDesktop() != null
				&& org.zkoss.zk.ui.Executions.getCurrent().getDesktop().getFirstPage() != null) {
			setPage(org.zkoss.zk.ui.Executions.getCurrent().getDesktop().getFirstPage());
		}
		if (getPage() == null) {
			throw new IllegalStateException("Dialog belum terhubung ke halaman aktif.");
		}
		super.onModal();
	}

	public void initBg() {

		setStyle("background:url('" + Common.ROOT
				+ "/img/bg_default.jpg') no-repeat center center fixed;-webkit-background-size: cover;-moz-background-size: cover;background-size: cover;-o-background-size: cover;");
		LampiranLain kop = null;
		try {
			Sekolah sekolah = SekolahUtil.getSekolah();

			if ((sekolah != null && sekolah.getId() != null)) {
				kop = LampiranLain.ambil(sekolah.getId(), LampiranLain.BG_SEKOLAH);
				if (kop == null || kop.getId() == null) {
					kop = LampiranLain.ambil(sekolah.getYayasan().getId(), LampiranLain.BG_YAYASAN);
				}
			}

			if (kop == null) {
				Yayasan yayasan = SekolahUtil.getYayasan();
				if ((yayasan != null && yayasan.getId() != null)) {
					kop = LampiranLain.ambil(yayasan.getId(), LampiranLain.BG_YAYASAN);
				}
			}

			if (kop == null) {
				PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
				if ((perguruanTinggi != null && perguruanTinggi.getId() != null)) {
					kop = LampiranLain.ambil(perguruanTinggi.getId(), LampiranLain.BG_PT);
				}
			}

			if (kop != null) {
				setStyle("background:url('" + kop.createLinkUri(true, true)
						+ "') no-repeat center center fixed;-webkit-background-size: cover;-moz-background-size: cover;background-size: cover;-o-background-size: cover;");
			}

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/MyWindow.java:127");

		}

	}

	@Override
	public void detach() {
		if (pakaiClose) {
			super.setVisible(false);

		} else {
			super.detach();
		}
	}

	public void setTitle(String title) {
		super.setTitle(Common.getBahasaConfig(title));
	}

	/**
	 * Setel judul TANPA menerjemahkan — untuk DATA DINAMIS. Lihat {@link MyLabelConfig#setValueData(String)}.
	 */
	public MyWindow setTitleData(String title) {
		super.setTitle(title);
		return this;
	}

	@Override
	public void setWidth(String width) {
		super.setWidth(width);
	}

}
