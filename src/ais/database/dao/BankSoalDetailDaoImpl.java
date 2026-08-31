package ais.database.dao;

import ais.database.model.BankSoalDetail;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.BankSoalDetail}
 * (detail/butir soal dalam suatu bank soal), lewat {@link ais.database.dao.GenericHibernateDao}.
 * Tidak ada method tambahan -- seluruh perilaku CRUD memakai implementasi generik di kelas induk
 * tersebut.
 */
public class BankSoalDetailDaoImpl extends
		GenericHibernateDao<BankSoalDetail, Long, BankSoalDetailDao> implements
		BankSoalDetailDao {

}
