package ais.database.dao.rab;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.rab.Pejabat;

/**
 * Implementasi Hibernate untuk {@link PejabatDao}, mengelola entitas {@link ais.database.model.rab.Pejabat}.
 * Kosong sesuai desain — seluruh logika CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class PejabatDaoImpl extends
		GenericHibernateDao<Pejabat, Long, PejabatDao> implements
		PejabatDao {

}
