package ais.database.dao.rab;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.rab.JenisInformasiRab;

/**
 * Implementasi Hibernate untuk {@link JenisInformasiRabDao}, mengelola entitas
 * {@link ais.database.model.rab.JenisInformasiRab}. Kosong sesuai desain — seluruh logika CRUD
 * generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class JenisInformasiRabDaoImpl extends
		GenericHibernateDao<JenisInformasiRab, Long, JenisInformasiRabDao>
		implements JenisInformasiRabDao {

}
