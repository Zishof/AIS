package ais.database.dao.rab;

import ais.database.dao.GenericDao;
import ais.database.model.rab.SumberDana;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.rab.SumberDana} (sumber dana anggaran dalam
 * modul RAB). Tidak menambah method di luar {@link ais.database.dao.GenericDao} — seluruh operasi
 * CRUD generik diwariskan langsung dari sana.
 */
public interface SumberDanaDao extends GenericDao<SumberDana, Long>{

}
