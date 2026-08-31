package ais.database.dao.library;

import ais.database.dao.GenericDao;
import ais.database.model.library.PeminjamanPengadaanItemDetail;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.library.PeminjamanPengadaanItemDetail} pada
 * modul perpustakaan — baris detail peminjaman item pustaka (rincian per item dari
 * {@link ais.database.model.library.PeminjamanPengadaanItem}). Interface ini sengaja kosong:
 * seluruh operasi CRUD generik sudah disediakan oleh {@link ais.database.dao.GenericDao}, lihat
 * javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericDao
 */
public interface PeminjamanPengadaanItemDetailDao extends
		GenericDao<PeminjamanPengadaanItemDetail, Long> {

}
