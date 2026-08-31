package ais.database.dao.library;

import ais.database.dao.GenericDao;
import ais.database.model.library.VersiDdcItem;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.library.VersiDdcItem} pada modul
 * perpustakaan — versi/edisi skema klasifikasi Dewey Decimal Classification (DDC) yang dipakai.
 * Interface ini sengaja kosong: seluruh operasi CRUD generik sudah disediakan oleh
 * {@link ais.database.dao.GenericDao}, lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericDao
 */
public interface VersiDdcItemDao extends GenericDao<VersiDdcItem, Long>{

}
