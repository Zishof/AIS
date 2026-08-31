package ais.database.dao.rab;

import ais.database.dao.GenericDao;
import ais.database.model.rab.ChecklistLaporan;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.rab.ChecklistLaporan} (checklist laporan
 * dalam modul RAB). Tidak menambah method di luar {@link ais.database.dao.GenericDao} — seluruh
 * operasi CRUD generik diwariskan langsung dari sana.
 */
public interface ChecklistLaporanDao extends GenericDao<ChecklistLaporan, Long> {

}
