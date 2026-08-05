package ais.common;

/**
 * Interface independen untuk menjalankan proses berat (seperti Query Database).
 * Berjalan murni di Latar Belakang (Background Thread) tanpa membuat UI Browser Hang/Freeze.
 */
public interface BackgroundTask {
    
    /**
     * @return Object hasil dari database (misal: List, Map, atau Entitas) yang akan dilempar ke UITask.
     */
    Object doInBackground() throws Exception;
    
}