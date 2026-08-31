package ais.database.dao.library;

import ais.database.dao.GenericDao;
import ais.database.model.library.PemesananPengadaanItemDetail;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.library.PemesananPengadaanItemDetail} pada
 * modul perpustakaan — baris detail pemesanan pengadaan item pustaka (rincian per item dari
 * {@link ais.database.model.library.PemesananPengadaanItem}). Interface ini sengaja kosong:
 * seluruh operasi CRUD generik sudah disediakan oleh {@link ais.database.dao.GenericDao}, lihat
 * javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericDao
 */
public interface PemesananPengadaanItemDetailDao extends
		GenericDao<PemesananPengadaanItemDetail, Long> {

}
