package ais.database.dao.library;

import ais.database.dao.GenericDao;
import ais.database.model.library.ReturPengadaanItemDetail;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.library.ReturPengadaanItemDetail} pada
 * modul perpustakaan — baris detail retur pengadaan item pustaka (rincian per item dari
 * {@link ais.database.model.library.ReturPengadaanItem}). Interface ini sengaja kosong: seluruh
 * operasi CRUD generik sudah disediakan oleh {@link ais.database.dao.GenericDao}, lihat javadoc
 * di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericDao
 */
public interface ReturPengadaanItemDetailDao extends
		GenericDao<ReturPengadaanItemDetail, Long> {

}
