package ais.database.dao.library;

import ais.database.dao.GenericDao;
import ais.database.model.library.DdcItem;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.library.DdcItem} pada modul perpustakaan —
 * master kode/kategori klasifikasi Dewey Decimal Classification (DDC). Interface ini sengaja
 * kosong: seluruh operasi CRUD generik sudah disediakan oleh {@link ais.database.dao.GenericDao},
 * lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericDao
 */
public interface DdcItemDao extends GenericDao<DdcItem, Long>{

}
