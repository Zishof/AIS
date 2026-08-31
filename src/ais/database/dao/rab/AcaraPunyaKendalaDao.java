package ais.database.dao.rab;

import ais.database.dao.GenericDao;
import ais.database.model.rab.AcaraPunyaKendala;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.rab.AcaraPunyaKendala} (relasi acara dengan
 * kendala/hambatan yang dihadapinya dalam modul RAB). Tidak menambah method di luar
 * {@link ais.database.dao.GenericDao} — seluruh operasi CRUD generik diwariskan langsung dari sana.
 */
public interface AcaraPunyaKendalaDao extends GenericDao<AcaraPunyaKendala, Long>{

}
