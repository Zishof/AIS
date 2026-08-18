package ais.action.master.helper;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Html;

/**
 * Helper UI: menyajikan aksi <b>"Proses Bayar"</b> sebagai <b>toggle ON/OFF
 * modern</b> (gaya seperti switch Bootstrap) — TANPA menulis ulang logika uang.
 *
 * <p>
 * Tombol "Proses Bayar" asli (dari {@link ais.ui.render.DetailPembayaranMahasiswaRenderer})
 * tetap dipakai &amp; disembunyikan; saat toggle <b>ON</b>, ia mem-{@code postEvent}
 * "onClick" ke tombol asli (memasukkan tagihan ke pembayaran). Saat toggle
 * <b>OFF</b>, pemanggil (subclass renderer) menjalankan aksi pembatalan
 * ({@link Aksi}) yang melepaskan tagihan dari pembayaran.
 *
 * <p>
 * Penanda jumlah pilihan disimpan di Desktop (counter) agar Wizard bisa
 * mencegah lanjut ke pembayaran bila belum ada yang dipilih.
 *
 * Kompatibel Java 1.7 dan ZK 5 CE.
 */
public class ProsesBayarCheckboxHelper {

	/** Counter di Desktop: berapa tagihan yang sedang dipilih utk dibayar. */
	public static final String ATTR_ADA_PILIHAN = "ecampus_wizard_jml_pilihan_bayar";
	/** Penanda CSS switch sudah di-inject (per Desktop). */
	private static final String ATTR_CSS = "ecampus_wizard_switch_css";

	/** Aksi yang dijalankan saat toggle dimatikan (mis. lepas tagihan). */
	public interface Aksi {
		void jalankan() throws Exception;
	}

	private ProsesBayarCheckboxHelper() {
	}

	/**
	 * Pasang toggle ON/OFF pada {@code cellParent} yang menggantikan tampilan
	 * tombol "Proses Bayar" asli.
	 *
	 * @param cellParent wadah (sel kolom) tempat toggle dipasang
	 * @param tombolAsli tombol "Proses Bayar" asli (disembunyikan, logikanya dipakai)
	 * @param onMati     aksi saat toggle dimatikan (boleh null)
	 * @return checkbox (toggle) yang dibuat
	 */
	public static Checkbox pasang(Component cellParent, final HtmlBasedComponent tombolAsli, final Aksi onMati) {
		// Sembunyikan tombol asli secara ANDAL (tetap di tree agar logikanya bisa dipicu).
		tombolAsli.setVisible(false);
		try {
			tombolAsli.setStyle("display:none !important;width:0;height:0;margin:0;padding:0;overflow:hidden;");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ProsesBayarCheckboxHelper.java:58");
		}

		injectCssSekali(tombolAsli);

		final Checkbox cb = new Checkbox();
		cb.setSclass("wz-switch");
		cb.setTooltiptext("Aktifkan untuk memasukkan tagihan ini ke pembayaran");
		// PENTING: engine memanggil Common.freeze(gridss,..) yang men-setDisabled
		// semua input di grid. Atribut "janganDisabled" membuat toggle DIKECUALIKAN
		// (lihat InitComboUtil.doFreeze) sehingga tetap bisa diklik.
		cb.setAttribute("janganDisabled", Boolean.TRUE);

		cb.addEventListener("onCheck", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				if (cb.isChecked()) {
					// ON: pakai logika lama (masukkan tagihan ke pembayaran).
					Events.postEvent("onClick", tombolAsli, null);
					ubahCounter(cb.getDesktop(), +1);
				} else {
					// OFF: lepas tagihan dari pembayaran (aksi disediakan pemanggil).
					if (onMati != null) {
						try {
							onMati.jalankan();
						} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/ProsesBayarCheckboxHelper.java:83");
							// jangan rusak UI bila pelepasan gagal
						}
					}
					ubahCounter(cb.getDesktop(), -1);
				}
			}
		});

		if (cellParent != null) {
			cb.setParent(cellParent);
		}
		return cb;
	}

	/** Naik/turunkan counter pilihan di Desktop (tidak < 0). */
	private static void ubahCounter(Desktop dt, int delta) {
		try {
			if (dt == null) {
				return;
			}
			Object v = dt.getAttribute(ATTR_ADA_PILIHAN);
			int c = (v instanceof Integer) ? (Integer) v : 0;
			c += delta;
			if (c < 0) {
				c = 0;
			}
			dt.setAttribute(ATTR_ADA_PILIHAN, Integer.valueOf(c));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ProsesBayarCheckboxHelper.java:111");
		}
	}

	/** Inject CSS switch sekali per Desktop. */
	private static void injectCssSekali(Component contoh) {
		try {
			Desktop dt = (contoh != null) ? contoh.getDesktop() : null;
			if (dt == null || dt.getAttribute(ATTR_CSS) != null) {
				return;
			}
			dt.setAttribute(ATTR_CSS, Boolean.TRUE);
			Html style = new Html("<style>"
					+ "@keyframes wzSwitchPulse{0%,100%{box-shadow:0 2px 8px rgba(245,158,11,.45);}"
					+ "50%{box-shadow:0 0 0 7px rgba(245,158,11,0),0 2px 10px rgba(245,158,11,.65);}}"
					+ ".wz-switch input[type='checkbox']{-webkit-appearance:none;appearance:none;outline:none;"
					+ "cursor:pointer;margin:0;vertical-align:middle;width:58px;height:30px;border-radius:30px;"
					+ "border:2px solid #ffffff;position:relative;transition:background .2s;"
					+ "background:#f59e0b;" // OFF = amber → menarik perhatian (ajakan klik)
					+ "background-image:radial-gradient(circle 11px at 16px 15px,#fff 99%,transparent 100%);"
					+ "animation:wzSwitchPulse 1.6s ease-in-out infinite;}"
					+ ".wz-switch input[type='checkbox']:checked{background:#16a34a;animation:none;" // ON = hijau
					+ "box-shadow:0 2px 10px rgba(22,163,74,.5);"
					+ "background-image:radial-gradient(circle 11px at 42px 15px,#fff 99%,transparent 100%);}"
					+ ".wz-switch input[type='checkbox']:hover{filter:brightness(1.03);}"
					+ "</style>");
			Component root = dt.getFirstPage() != null && dt.getFirstPage().getFirstRoot() != null
					? dt.getFirstPage().getFirstRoot()
					: contoh;
			if (root != null) {
				style.setParent(root);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ProsesBayarCheckboxHelper.java:143");
		}
	}
}
