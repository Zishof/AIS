package ais.database.dao.library;

import ais.database.dao.GenericDao;
import ais.database.model.library.PenerimaanPengadaanItem;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.library.PenerimaanPengadaanItem} pada modul
 * perpustakaan — transaksi (header) penerimaan pengadaan item pustaka dari penyedia. Interface
 * ini sengaja kosong: seluruh operasi CRUD generik sudah disediakan oleh
 * {@link ais.database.dao.GenericDao}, lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericDao
 */
public interface PenerimaanPengadaanItemDao extends GenericDao<PenerimaanPengadaanItem, Long>{

}
