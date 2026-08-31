package ais.database.dao;

import ais.database.model.ChecklistPenilaianDosen;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.ChecklistPenilaianDosen}
 * (item checklist penilaian dosen), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak
 * ada method tambahan -- seluruh perilaku CRUD memakai implementasi generik di kelas induk
 * tersebut.
 */
public class ChecklistPenilaianDosenDaoImpl
		extends
		GenericHibernateDao<ChecklistPenilaianDosen, Long, ChecklistPenilaianDosenDao>
		implements ChecklistPenilaianDosenDao {

}
