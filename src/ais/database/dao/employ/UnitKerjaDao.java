package ais.database.dao.employ;

import ais.database.dao.GenericDao;
import ais.database.model.employ.UnitKerja;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.employ.UnitKerja} (unit kerja dalam modul
 * kepegawaian). Tidak menambah method di luar {@link ais.database.dao.GenericDao} — seluruh
 * operasi CRUD generik diwariskan langsung dari sana.
 */
public interface UnitKerjaDao extends GenericDao<UnitKerja, Long> {
	
}