package ais.database.dao;

import ais.database.model.GrupChecklistPenilaianDosen;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.GrupChecklistPenilaianDosen}
 * (grup/pengelompokan item checklist penilaian dosen), lewat
 * {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan -- seluruh perilaku
 * CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class GrupChecklistPenilaianDosenDaoImpl
		extends
		GenericHibernateDao<GrupChecklistPenilaianDosen, Long, GrupChecklistPenilaianDosenDao>
		implements GrupChecklistPenilaianDosenDao {

}
