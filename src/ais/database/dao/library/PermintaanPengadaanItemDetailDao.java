package ais.database.dao.library;

import ais.database.dao.GenericDao;
import ais.database.model.library.PermintaanPengadaanItemDetail;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.library.PermintaanPengadaanItemDetail} pada
 * modul perpustakaan — baris detail permintaan pengadaan item pustaka (rincian per item dari
 * {@link ais.database.model.library.PermintaanPengadaanItem}). Interface ini sengaja kosong:
 * seluruh operasi CRUD generik sudah disediakan oleh {@link ais.database.dao.GenericDao}, lihat
 * javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericDao
 */
public interface PermintaanPengadaanItemDetailDao extends
		GenericDao<PermintaanPengadaanItemDetail, Long> {

}
