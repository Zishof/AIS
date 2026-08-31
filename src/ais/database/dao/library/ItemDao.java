package ais.database.dao.library;

import ais.database.dao.GenericDao;
import ais.database.model.library.Item;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.library.Item} pada modul perpustakaan —
 * item koleksi pustaka (buku/bahan pustaka). Interface ini sengaja kosong: seluruh operasi CRUD
 * generik sudah disediakan oleh {@link ais.database.dao.GenericDao}, lihat javadoc di sana untuk
 * detail perilakunya.
 *
 * @see ais.database.dao.GenericDao
 */
public interface ItemDao extends GenericDao<Item, Long>{

}
