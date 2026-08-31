package ais.database.dao.library;

import ais.database.dao.GenericDao;
import ais.database.model.library.KembaliPengadaanItemDetail;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.library.KembaliPengadaanItemDetail} pada
 * modul perpustakaan — baris detail transaksi pengembalian pengadaan item pustaka (rincian per
 * item dari {@link ais.database.model.library.KembaliPengadaanItem}). Interface ini sengaja
 * kosong: seluruh operasi CRUD generik sudah disediakan oleh {@link ais.database.dao.GenericDao},
 * lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericDao
 */
public interface KembaliPengadaanItemDetailDao extends
		GenericDao<KembaliPengadaanItemDetail, Long> {

}
