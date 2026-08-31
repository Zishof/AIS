package ais.database.dao;

import ais.database.model.Konversi;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.Konversi} (data konversi
 * nilai/mata kuliah), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method
 * tambahan -- seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class KonversiDaoImpl extends GenericHibernateDao<Konversi, Long, KonversiDao> implements KonversiDao{

}
