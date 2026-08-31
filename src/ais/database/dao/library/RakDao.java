package ais.database.dao.library;

import ais.database.dao.GenericDao;
import ais.database.model.library.Rak;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.library.Rak} pada modul perpustakaan — rak
 * penyimpanan koleksi pustaka. Interface ini sengaja kosong: seluruh operasi CRUD generik sudah
 * disediakan oleh {@link ais.database.dao.GenericDao}, lihat javadoc di sana untuk detail
 * perilakunya.
 *
 * @see ais.database.dao.GenericDao
 */
public interface RakDao extends GenericDao<Rak, Long>{

}
