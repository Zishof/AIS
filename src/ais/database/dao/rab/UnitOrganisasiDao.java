package ais.database.dao.rab;

import ais.database.dao.GenericDao;
import ais.database.model.rab.UnitOrganisasi;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.rab.UnitOrganisasi} (unit organisasi dalam
 * modul RAB). Tidak menambah method di luar {@link ais.database.dao.GenericDao} — seluruh operasi
 * CRUD generik diwariskan langsung dari sana.
 */
public interface UnitOrganisasiDao extends GenericDao<UnitOrganisasi, Long>{

}
