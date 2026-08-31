package ais.database.dao.employ;

import ais.database.dao.GenericDao;
import ais.database.model.employ.GajiPokok;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.employ.GajiPokok} (tabel gaji pokok pegawai).
 * Tidak menambah method di luar {@link ais.database.dao.GenericDao} — seluruh operasi CRUD generik
 * diwariskan langsung dari sana.
 */
public interface GajiPokokDao extends GenericDao<GajiPokok, Long> {
	
}