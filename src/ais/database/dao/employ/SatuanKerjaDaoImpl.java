package ais.database.dao.employ;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.employ.SatuanKerjaEmploy;

/**
 * Implementasi Hibernate untuk {@link SatuanKerjaDao}, mengelola entitas
 * {@link ais.database.model.employ.SatuanKerjaEmploy}. Kosong sesuai desain — seluruh logika CRUD
 * generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class SatuanKerjaDaoImpl extends
		GenericHibernateDao<SatuanKerjaEmploy, Long, SatuanKerjaDao> implements
		SatuanKerjaDao {

}
