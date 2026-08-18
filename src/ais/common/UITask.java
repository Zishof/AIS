package ais.common;

/**
 * Interface independen untuk merender dan memanipulasi komponen antarmuka ZKoss.
 * Dipastikan berjalan secara aman dengan proteksi Executions.activate(desktop).
 */
public interface UITask {
    
    /**
     * @param backgroundResult Data kembalian yang dilempar dari BackgroundTask.doInBackground()
     */
    void updateUI(Object backgroundResult) throws Exception;
    
}