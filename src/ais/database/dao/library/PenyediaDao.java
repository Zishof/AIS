package ais.database.dao.library;

import ais.database.dao.GenericDao;
import ais.database.model.library.Penyedia;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.library.Penyedia} pada modul perpustakaan —
 * penyedia/pemasok pengadaan item pustaka. Interface ini sengaja kosong: seluruh operasi CRUD
 * generik sudah disediakan oleh {@link ais.database.dao.GenericDao}, lihat javadoc di sana untuk
 * detail perilakunya.
 *
 * @see ais.database.dao.GenericDao
 */
public interface PenyediaDao extends GenericDao<Penyedia, Long>{

}
