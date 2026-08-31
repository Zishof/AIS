package ais.database.dao;

import ais.database.model.BankSoal;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.BankSoal} (data bank soal
 * ujian), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan --
 * seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class BankSoalDaoImpl extends
		GenericHibernateDao<BankSoal, Long, BankSoalDao> implements BankSoalDao {

}
