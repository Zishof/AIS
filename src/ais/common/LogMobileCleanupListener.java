package ais.common;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Listener kecil khusus (pola sama {@link ZkLanguageBootstrapListener}/{@link ErrorAuditContextListener})
 * untuk menyalakan/mematikan {@link ais.service.maintenance.LogMobileCleanupService} lewat siklus hidup
 * webapp. Dipisah dari {@code AppStartupListener} (file panas yang sedang dikerjakan sesi lain) supaya
 * tidak bertabrakan.
 */
public class LogMobileCleanupListener implements ServletContextListener {

	public void contextInitialized(ServletContextEvent event) {
		try {
			ais.service.maintenance.LogMobileCleanupService.mulai();
		} catch (Throwable throwable) {
			ErrorAuditUtil.record(throwable, "Gagal mengaktifkan LogMobileCleanupListener", null, false);
		}
	}

	public void contextDestroyed(ServletContextEvent event) {
		try {
			ais.service.maintenance.LogMobileCleanupService.hentikan();
		} catch (Throwable ignored) {
			ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/LogMobileCleanupListener.java:contextDestroyed");
		}
	}
}
