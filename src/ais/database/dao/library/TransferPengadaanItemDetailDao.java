package ais.database.dao.library;

import ais.database.dao.GenericDao;
import ais.database.model.library.TransferPengadaanItemDetail;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.library.TransferPengadaanItemDetail} pada
 * modul perpustakaan — baris detail transfer item pustaka (rincian per item dari
 * {@link ais.database.model.library.TransferPengadaanItem}). Interface ini sengaja kosong:
 * seluruh operasi CRUD generik sudah disediakan oleh {@link ais.database.dao.GenericDao}, lihat
 * javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericDao
 */
public interface TransferPengadaanItemDetailDao extends
		GenericDao<TransferPengadaanItemDetail, Long> {

}
