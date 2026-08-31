package ais.database.dao;

import ais.database.model.KurikulumPunyaMatakuliahDetail;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.KurikulumPunyaMatakuliahDetail}
 * (detail relasi kurikulum dengan mata kuliah anggotanya), lewat
 * {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan -- seluruh perilaku
 * CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class KurikulumPunyaMatakuliahDetailDaoImpl
		extends
		GenericHibernateDao<KurikulumPunyaMatakuliahDetail, Long, KurikulumPunyaMatakuliahDetailDao>
		implements KurikulumPunyaMatakuliahDetailDao {

}
