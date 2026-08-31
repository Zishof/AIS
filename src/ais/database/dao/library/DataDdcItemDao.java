package ais.database.dao.library;

import ais.database.dao.GenericDao;
import ais.database.model.library.DataDdcItem;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.library.DataDdcItem} pada modul
 * perpustakaan — data klasifikasi Dewey Decimal Classification (DDC) yang melekat pada item
 * pustaka tertentu. Interface ini sengaja kosong: seluruh operasi CRUD generik sudah disediakan
 * oleh {@link ais.database.dao.GenericDao}, lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericDao
 */
public interface DataDdcItemDao extends GenericDao<DataDdcItem, Long>{

}
