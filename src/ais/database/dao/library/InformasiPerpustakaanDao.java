package ais.database.dao.library;

import ais.database.dao.GenericDao;
import ais.database.model.library.InformasiPerpustakaan;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.library.InformasiPerpustakaan} pada modul
 * perpustakaan — informasi/pengumuman yang diterbitkan perpustakaan. Interface ini sengaja kosong:
 * seluruh operasi CRUD generik sudah disediakan oleh {@link ais.database.dao.GenericDao}, lihat
 * javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericDao
 */
public interface InformasiPerpustakaanDao extends GenericDao<InformasiPerpustakaan, Long>{

}
