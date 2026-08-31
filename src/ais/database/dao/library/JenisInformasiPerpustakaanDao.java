package ais.database.dao.library;

import ais.database.dao.GenericDao;
import ais.database.model.library.JenisInformasiPerpustakaan;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.library.JenisInformasiPerpustakaan} pada
 * modul perpustakaan — jenis/kategori informasi perpustakaan. Interface ini sengaja kosong:
 * seluruh operasi CRUD generik sudah disediakan oleh {@link ais.database.dao.GenericDao}, lihat
 * javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericDao
 */
public interface JenisInformasiPerpustakaanDao extends GenericDao<JenisInformasiPerpustakaan, Long>{

}
