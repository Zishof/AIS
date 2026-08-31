package ais.database.dao.employ;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.employ.JenisJabatan;

/**
 * Implementasi Hibernate untuk {@link JenisJabatanDao}, mengelola entitas
 * {@link ais.database.model.employ.JenisJabatan}. Kosong sesuai desain — seluruh logika CRUD
 * generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class JenisJabatanDaoImpl extends
		GenericHibernateDao<JenisJabatan, Long, JenisJabatanDao> implements
		JenisJabatanDao {

}
