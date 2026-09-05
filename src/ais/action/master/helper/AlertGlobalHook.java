package ais.action.master.helper;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.util.UiLifeCycle;
import org.zkoss.zul.Script;

/**
 * Memuat dialog alert global pada setiap desktop ZKoss.
 *
 * <p>Skrip bersifat idempotent. Selain mempercantik {@code Clients.alert}, skrip
 * memberi penanda {@code ais-alert-zk} pada window {@code Messagebox} yang dibuat
 * dinamis sehingga stylesheet global dapat mengubah alert tanpa memengaruhi
 * window/form ZK lainnya.</p>
 */
public class AlertGlobalHook implements UiLifeCycle {

	private static final String ATTR_SCRIPT_LOADED = AlertGlobalHook.class.getName() + ".loaded";
	private static final String SCRIPT_SRC = "~./js/pesan-formal.js?v=20260906";

	@Override
	public void afterPageAttached(Page page, Desktop desktop) {
		try {
			if (page == null || desktop == null || desktop.getAttribute(ATTR_SCRIPT_LOADED) != null) {
				return;
			}
			desktop.setAttribute(ATTR_SCRIPT_LOADED, Boolean.TRUE);
			Script script = new Script();
			script.setSrc(SCRIPT_SRC);
			script.setDefer(true);
			script.setPage(page);
		} catch (Throwable error) {
			// Tampilan alert tidak boleh menggagalkan proses render halaman.
			ais.common.ErrorAuditUtil.record(error,
					"auto-audit src/ais/action/master/helper/AlertGlobalHook.java:afterPageAttached");
		}
	}

	@Override
	public void afterComponentAttached(Component component, Page page) {
		// Tidak diperlukan: satu skrip per desktop sudah mencakup semua komponen.
	}

	@Override
	public void afterComponentDetached(Component component, Page page) {
		// Tidak ada resource server-side yang perlu dilepas.
	}

	@Override
	public void afterComponentMoved(Component parent, Component previousParent, Component component) {
		// Tidak ada penanganan khusus saat komponen dipindah.
	}

	@Override
	public void afterPageDetached(Page page, Desktop desktop) {
		// Penanda disimpan pada desktop dan akan hilang bersama desktop.
	}
}
