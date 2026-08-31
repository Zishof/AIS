package ais.database.dao.library;

import ais.database.dao.GenericDao;
import ais.database.model.library.Anggota;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.library.Anggota} pada modul perpustakaan —
 * anggota perpustakaan (peminjam terdaftar). Interface ini sengaja kosong: seluruh operasi CRUD
 * generik (simpan, hapus, cari-by-id, cari-semua, cari-berhalaman, pencarian ilike) sudah
 * disediakan oleh {@link ais.database.dao.GenericDao}, lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericDao
 */
public interface AnggotaDao extends GenericDao<Anggota, Long>{

}
