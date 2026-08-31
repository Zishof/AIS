package ais.database.dao.employ;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.employ.Diklat;

/**
 * Implementasi Hibernate untuk {@link DiklatDao}, mengelola entitas {@link ais.database.model.employ.Diklat}.
 * Kosong sesuai desain — seluruh logika CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class DiklatDaoImpl extends
		GenericHibernateDao<Diklat, Long, DiklatDao> implements
		DiklatDao {

}
