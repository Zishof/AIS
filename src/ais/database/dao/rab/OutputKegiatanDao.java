package ais.database.dao.rab;

import ais.database.dao.GenericDao;
import ais.database.model.rab.OutputKegiatan;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.rab.OutputKegiatan} (output/keluaran
 * kegiatan dalam modul RAB). Tidak menambah method di luar {@link ais.database.dao.GenericDao} —
 * seluruh operasi CRUD generik diwariskan langsung dari sana.
 */
public interface OutputKegiatanDao extends GenericDao<OutputKegiatan, Long> {

}
