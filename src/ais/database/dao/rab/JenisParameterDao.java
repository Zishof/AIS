package ais.database.dao.rab;

import ais.database.dao.GenericDao;
import ais.database.model.rab.JenisParameter;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.rab.JenisParameter} (jenis parameter
 * perencanaan dalam modul RAB). Tidak menambah method di luar {@link ais.database.dao.GenericDao}
 * — seluruh operasi CRUD generik diwariskan langsung dari sana.
 */
public interface JenisParameterDao extends GenericDao<JenisParameter, Long>{

}
