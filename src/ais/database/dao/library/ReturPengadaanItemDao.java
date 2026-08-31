package ais.database.dao.library;

import ais.database.dao.GenericDao;
import ais.database.model.library.ReturPengadaanItem;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.library.ReturPengadaanItem} pada modul
 * perpustakaan — transaksi (header) retur pengadaan item pustaka ke penyedia. Interface ini
 * sengaja kosong: seluruh operasi CRUD generik sudah disediakan oleh
 * {@link ais.database.dao.GenericDao}, lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericDao
 */
public interface ReturPengadaanItemDao extends GenericDao<ReturPengadaanItem, Long>{

}
