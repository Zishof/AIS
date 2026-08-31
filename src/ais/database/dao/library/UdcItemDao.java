package ais.database.dao.library;

import ais.database.dao.GenericDao;
import ais.database.model.library.UdcItem;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.library.UdcItem} pada modul perpustakaan —
 * master kode/kategori klasifikasi Universal Decimal Classification (UDC). Interface ini sengaja
 * kosong: seluruh operasi CRUD generik sudah disediakan oleh {@link ais.database.dao.GenericDao},
 * lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericDao
 */
public interface UdcItemDao extends GenericDao<UdcItem, Long>{

}
