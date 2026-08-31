package ais.database.dao.library;

import ais.database.dao.GenericDao;
import ais.database.model.library.RakDetail;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.library.RakDetail} pada modul perpustakaan
 * — baris detail rak (rincian penempatan item pada {@link ais.database.model.library.Rak}).
 * Interface ini sengaja kosong: seluruh operasi CRUD generik sudah disediakan oleh
 * {@link ais.database.dao.GenericDao}, lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericDao
 */
public interface RakDetailDao extends GenericDao<RakDetail, Long>{

}
