package ais.database.dao.employ;

import ais.database.dao.GenericDao;
import ais.database.model.employ.RiwayatPendidikanPegawai;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.employ.RiwayatPendidikanPegawai} (riwayat
 * pendidikan pegawai). Tidak menambah method di luar {@link ais.database.dao.GenericDao} —
 * seluruh operasi CRUD generik diwariskan langsung dari sana.
 */
public interface RiwayatPendidikanPegawaiDao extends GenericDao<RiwayatPendidikanPegawai, Long> {
	
}