package ais.database.dao.rab;

import ais.database.dao.GenericDao;
import ais.database.model.rab.ChecklistLaporanDetail;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.rab.ChecklistLaporanDetail} (item detail
 * checklist laporan dalam modul RAB). Tidak menambah method di luar
 * {@link ais.database.dao.GenericDao} — seluruh operasi CRUD generik diwariskan langsung dari sana.
 */
public interface ChecklistLaporanDetailDao extends GenericDao<ChecklistLaporanDetail, Long>{

}
