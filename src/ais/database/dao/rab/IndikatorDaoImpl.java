package ais.database.dao.rab;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.rab.Indikator;

/**
 * Implementasi Hibernate untuk {@link IndikatorDao}, mengelola entitas
 * {@link ais.database.model.rab.Indikator}. Kosong sesuai desain — seluruh logika CRUD generik
 * diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class IndikatorDaoImpl extends
		GenericHibernateDao<Indikator, Long, IndikatorDao> implements
		IndikatorDao {

}
