package ais.database.dao.library;

import ais.database.dao.GenericDao;
import ais.database.model.library.StatusItem;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.library.StatusItem} pada modul perpustakaan
 * — status item pustaka (mis. tersedia, dipinjam, rusak, hilang). Interface ini sengaja kosong:
 * seluruh operasi CRUD generik sudah disediakan oleh {@link ais.database.dao.GenericDao}, lihat
 * javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericDao
 */
public interface StatusItemDao extends GenericDao<StatusItem, Long>{

}
