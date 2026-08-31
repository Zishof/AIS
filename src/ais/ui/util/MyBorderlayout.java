package ais.ui.util;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.HeadlessActionContext;
import ais.database.model.PerguruanTinggi;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

/**
 * Komponen/konfigurasi ZK khusus AIS untuk my borderlayout. Tipe ini membakukan default dan
 * perilaku tampilan di atas komponen induk supaya layar tidak mengulang konfigurasi widget yang
 * sama.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Borderlayout}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan
 * yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code boolean initModel};
 * inisialisasi/lifecycle ({@code initBg()}); konfigurasi constructor: {@code initModel}. Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> setter dan helper mengubah state komponen ZK yang sedang terpasang pada desktop.
 * Gunakan pada event thread UI dan jangan membagikan instance antar session; aturan bisnis dan transaksi
 * persistence tetap harus didelegasikan ke action atau service pemanggil.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see Borderlayout
 */
public class MyBorderlayout extends Borderlayout {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9039320589896582492L;

	private boolean initModel = false;

	public MyBorderlayout() {
		super();
		initBg();
		if (HeadlessActionContext.isActive()) return;
		if (Common.isMobile() && !initModel) {
			initModel = true;
			setVisible(false);
			Common.createDefaultTimerNoBusy(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					try {
						UIUtil.checkBorderMobile(MyBorderlayout.this);
					} catch (Exception e) {
						ais.common.ErrorAuditUtil.record(e, "auto-audit(checkBorderMobile-gagal) src/ais/ui/util/MyBorderlayout.java");
					} finally {
						setVisible(true);
					}
				}
			});
		}
	}
	
	public MyBorderlayout(boolean initModel) {
		super();
		this.initModel = initModel;
		initBg();
		if (HeadlessActionContext.isActive()) return;
		if (Common.isMobile() && !initModel) {
			initModel = true;
			setVisible(false);
			Common.createDefaultTimerNoBusy(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					try {
						UIUtil.checkBorderMobile(MyBorderlayout.this);
					} catch (Exception e) {
						ais.common.ErrorAuditUtil.record(e, "auto-audit(checkBorderMobile-gagal) src/ais/ui/util/MyBorderlayout.java");
					} finally {
						setVisible(true);
					}
				}
			});
		}
	}

	public void initBg() {
		if (HeadlessActionContext.isActive()) return;

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

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/MyBorderlayout.java:92");

		}

	}
}
