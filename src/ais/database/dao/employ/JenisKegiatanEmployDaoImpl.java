package ais.database.dao.employ;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.employ.JenisKegiatanEmploy;

/**
 * Implementasi Hibernate untuk {@link JenisKegiatanEmployDao}, mengelola entitas
 * {@link ais.database.model.employ.JenisKegiatanEmploy}. Kosong sesuai desain — seluruh logika
 * CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class JenisKegiatanEmployDaoImpl extends GenericHibernateDao<JenisKegiatanEmploy, Long, JenisKegiatanEmployDao>
		implements JenisKegiatanEmployDao {

}
