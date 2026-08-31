package ais.database.dao.employ;

import ais.database.dao.GenericDao;
import ais.database.model.employ.DaftarNilaiPelaksanaanPekerjaan;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.employ.DaftarNilaiPelaksanaanPekerjaan}
 * (daftar nilai pelaksanaan pekerjaan/DP3 pegawai) dalam modul kepegawaian. Tidak menambah method
 * di luar {@link ais.database.dao.GenericDao} — seluruh operasi CRUD generik diwariskan langsung
 * dari sana.
 */
public interface DaftarNilaiPelaksanaanPekerjaanDao extends GenericDao<DaftarNilaiPelaksanaanPekerjaan, Long> {
	
}