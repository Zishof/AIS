package ais.database.dao.library;

import ais.database.dao.GenericDao;
import ais.database.model.library.PesananAnggota;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.library.PesananAnggota} pada modul
 * perpustakaan — pesanan/reservasi item pustaka oleh anggota. Interface ini sengaja kosong:
 * seluruh operasi CRUD generik sudah disediakan oleh {@link ais.database.dao.GenericDao}, lihat
 * javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericDao
 */
public interface PesananAnggotaDao extends GenericDao<PesananAnggota, Long>{

}
