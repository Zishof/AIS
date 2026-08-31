package ais.database.dao.rab;

import ais.database.dao.GenericDao;
import ais.database.model.rab.ChecklistLaporanDetailDefault;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.rab.ChecklistLaporanDetailDefault}
 * (template/default item detail checklist laporan dalam modul RAB). Tidak menambah method di luar
 * {@link ais.database.dao.GenericDao} — seluruh operasi CRUD generik diwariskan langsung dari sana.
 */
public interface ChecklistLaporanDetailDefaultDao extends
		GenericDao<ChecklistLaporanDetailDefault, Long> {

}
