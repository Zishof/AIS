package ais.database.dao.library;

import ais.database.dao.GenericDao;
import ais.database.model.library.JenisIdentitasAnggota;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.library.JenisIdentitasAnggota} pada modul
 * perpustakaan — jenis dokumen identitas anggota (mis. KTP, SIM, Paspor). Interface ini sengaja
 * kosong: seluruh operasi CRUD generik sudah disediakan oleh {@link ais.database.dao.GenericDao},
 * lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericDao
 */
public interface JenisIdentitasAnggotaDao extends GenericDao<JenisIdentitasAnggota, Long>{

}
