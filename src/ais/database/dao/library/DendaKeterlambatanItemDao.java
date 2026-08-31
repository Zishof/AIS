package ais.database.dao.library;

import ais.database.dao.GenericDao;
import ais.database.model.library.DendaKeterlambatanItem;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.library.DendaKeterlambatanItem} pada modul
 * perpustakaan — denda keterlambatan pengembalian item pustaka. Interface ini sengaja kosong:
 * seluruh operasi CRUD generik sudah disediakan oleh {@link ais.database.dao.GenericDao}, lihat
 * javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericDao
 */
public interface DendaKeterlambatanItemDao extends
		GenericDao<DendaKeterlambatanItem, Long> {

}
