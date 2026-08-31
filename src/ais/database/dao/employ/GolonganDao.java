package ais.database.dao.employ;

import ais.database.dao.GenericDao;
import ais.database.model.employ.Golongan;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.employ.Golongan} (golongan/pangkat
 * kepegawaian). Tidak menambah method di luar {@link ais.database.dao.GenericDao} — seluruh
 * operasi CRUD generik diwariskan langsung dari sana.
 */
public interface GolonganDao extends GenericDao<Golongan, Long> {
	
}