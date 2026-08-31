package ais.database.dao.library;

import ais.database.dao.GenericDao;
import ais.database.model.library.DataUdcItem;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.library.DataUdcItem} pada modul
 * perpustakaan — data klasifikasi Universal Decimal Classification (UDC) yang melekat pada item
 * pustaka tertentu. Interface ini sengaja kosong: seluruh operasi CRUD generik sudah disediakan
 * oleh {@link ais.database.dao.GenericDao}, lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericDao
 */
public interface DataUdcItemDao extends GenericDao<DataUdcItem, Long>{

}
