package ais.database.dao;

import ais.database.model.KelompokMatakuliah;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.KelompokMatakuliah}
 * (kelompok mata kuliah), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method
 * tambahan -- seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class KelompokMatakuliahDaoImpl extends GenericHibernateDao<KelompokMatakuliah, Long, KelompokMatakuliahDao> implements KelompokMatakuliahDao{

}
