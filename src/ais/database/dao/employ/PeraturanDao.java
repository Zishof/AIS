package ais.database.dao.employ;

import ais.database.dao.GenericDao;
import ais.database.model.employ.Peraturan;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.employ.Peraturan} (peraturan kepegawaian).
 * Tidak menambah method di luar {@link ais.database.dao.GenericDao} — seluruh operasi CRUD generik
 * diwariskan langsung dari sana.
 */
public interface PeraturanDao extends GenericDao<Peraturan, Long> {
	
}