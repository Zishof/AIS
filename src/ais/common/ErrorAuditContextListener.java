package ais.common;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Listener global untuk menangkap uncaught exception dari background thread.
 * Ini membantu error proses async/scheduler yang tidak lewat request ZK/Servlet.
 */
public class ErrorAuditContextListener implements ServletContextListener {

    private static Thread.UncaughtExceptionHandler previousHandler;

    public void contextInitialized(ServletContextEvent event) {
        try {
            previousHandler = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                public void uncaughtException(Thread thread, Throwable throwable) {
                    if (ErrorAuditUtil.isClientAbortIgnored(throwable)) {
                        return;
                    }
                    // Saat webapp berhenti/di-reload, JANGAN coba simpan error ke DB: classloader
                    // & JDBC driver sudah/akan ditutup, sehingga record() hanya menghasilkan
                    // IllegalStateException ("web application instance has been stopped already")
                    // + log4j "No appenders". Cukup teruskan ke handler bawaan JVM.
                    boolean aplikasiBerhenti = false;
                    try {
                        aplikasiBerhenti = Common.aplikasiSedangBerhenti;
                    } catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/common/ErrorAuditContextListener.java:29");
                    }
                    if (!aplikasiBerhenti) {
                        ErrorAuditUtil.record(throwable,
                                "Uncaught exception pada thread: " + (thread == null ? "" : thread.getName()), null, false);
                    }
                    if (previousHandler != null) {
                        previousHandler.uncaughtException(thread, throwable);
                    }
                }
            });
        } catch (Throwable throwable) {
            if (!ErrorAuditUtil.isClientAbortIgnored(throwable)) {
                ErrorAuditUtil.record(throwable, "Gagal mengaktifkan ErrorAuditContextListener", null, false);
            }
        }
    }

    public void contextDestroyed(ServletContextEvent event) {
        try {
            if (previousHandler != null) {
                Thread.setDefaultUncaughtExceptionHandler(previousHandler);
            }
        } catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/ErrorAuditContextListener.java:52");
        }
    }
}
