package ais.database.dao.library;

import ais.database.dao.GenericDao;
import ais.database.model.library.JenisItem;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.library.JenisItem} pada modul perpustakaan
 * — jenis item pustaka (mis. buku, majalah, CD). Interface ini sengaja kosong: seluruh operasi
 * CRUD generik sudah disediakan oleh {@link ais.database.dao.GenericDao}, lihat javadoc di sana
 * untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericDao
 */
public interface JenisItemDao extends GenericDao<JenisItem, Long>{

}
