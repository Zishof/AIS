package ais.common;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Listener untuk mendaftarkan mapping extension ZUL sedini mungkin saat
 * aplikasi web mulai. Tetap kompatibel Java 1.6/1.7.
 */
public class ZkLanguageBootstrapListener implements ServletContextListener {

    public void contextInitialized(ServletContextEvent event) {
        ZkLanguageBootstrap.ensureZulLanguageMapping();
    }

    public void contextDestroyed(ServletContextEvent event) {
        // Tidak ada resource yang perlu ditutup.
    }
}
