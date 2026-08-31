package ais.database.dao.rab;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.rab.SumberDana;

/**
 * Implementasi Hibernate untuk {@link SumberDanaDao}, mengelola entitas
 * {@link ais.database.model.rab.SumberDana}. Kosong sesuai desain — seluruh logika CRUD generik
 * diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class SumberDanaDaoImpl extends
		GenericHibernateDao<SumberDana, Long, SumberDanaDao> implements
		SumberDanaDao {

}
