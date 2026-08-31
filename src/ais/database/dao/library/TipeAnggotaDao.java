package ais.database.dao.library;

import ais.database.dao.GenericDao;
import ais.database.model.library.TipeAnggota;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.library.TipeAnggota} pada modul
 * perpustakaan — tipe/kelas anggota perpustakaan. Interface ini sengaja kosong: seluruh operasi
 * CRUD generik sudah disediakan oleh {@link ais.database.dao.GenericDao}, lihat javadoc di sana
 * untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericDao
 */
public interface TipeAnggotaDao extends GenericDao<TipeAnggota, Long>{

}
