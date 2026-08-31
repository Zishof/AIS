package ais.database.dao;

import ais.database.model.Kota;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.Kota} (data referensi kota),
 * lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan -- seluruh
 * perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class KotaDaoImpl extends GenericHibernateDao<Kota, Long, KotaDao> implements KotaDao{

}
