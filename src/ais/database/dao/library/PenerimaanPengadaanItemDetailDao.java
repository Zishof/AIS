package ais.database.dao.library;

import ais.database.dao.GenericDao;
import ais.database.model.library.PenerimaanPengadaanItemDetail;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.library.PenerimaanPengadaanItemDetail} pada
 * modul perpustakaan — baris detail penerimaan pengadaan item pustaka (rincian per item dari
 * {@link ais.database.model.library.PenerimaanPengadaanItem}). Interface ini sengaja kosong:
 * seluruh operasi CRUD generik sudah disediakan oleh {@link ais.database.dao.GenericDao}, lihat
 * javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericDao
 */
public interface PenerimaanPengadaanItemDetailDao extends
		GenericDao<PenerimaanPengadaanItemDetail, Long> {

}
