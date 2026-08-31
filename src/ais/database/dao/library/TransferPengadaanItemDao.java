package ais.database.dao.library;

import ais.database.dao.GenericDao;
import ais.database.model.library.TransferPengadaanItem;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.library.TransferPengadaanItem} pada modul
 * perpustakaan — transaksi (header) transfer item pustaka antar perpustakaan/cabang. Interface
 * ini sengaja kosong: seluruh operasi CRUD generik sudah disediakan oleh
 * {@link ais.database.dao.GenericDao}, lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericDao
 */
public interface TransferPengadaanItemDao extends GenericDao<TransferPengadaanItem, Long>{

}
