package ais.database.dao.rab;

import ais.database.dao.GenericDao;
import ais.database.model.rab.RenstraProgram;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.rab.RenstraProgram} (program dalam Rencana
 * Strategis/Renstra). Tidak menambah method di luar {@link ais.database.dao.GenericDao} — seluruh
 * operasi CRUD generik diwariskan langsung dari sana.
 */
public interface RenstraProgramDao extends GenericDao<RenstraProgram, Long>{

}
