package ais.action.master.library.modern;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/** Starts and stops the disabled-by-default saved-search dispatcher with the web application. */
public final class LibrarySavedSearchNotificationListener implements ServletContextListener {
    public void contextInitialized(ServletContextEvent event) { LibrarySavedSearchNotificationWorker.start(); }
    public void contextDestroyed(ServletContextEvent event) { LibrarySavedSearchNotificationWorker.stop(); }
}
