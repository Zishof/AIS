package ais.database.dao;

import ais.database.model.BukuBahanAjar;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.BukuBahanAjar} (data buku
 * bahan ajar), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan --
 * seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class BukuBahanAjarDaoImpl extends
		GenericHibernateDao<BukuBahanAjar, Long, BukuBahanAjarDao> implements
		BukuBahanAjarDao {

}
