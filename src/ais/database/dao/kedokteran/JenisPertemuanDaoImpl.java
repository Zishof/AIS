package ais.database.dao.kedokteran;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.kedokteran.JenisPertemuan;

/**
 * Implementasi Hibernate {@link JenisPertemuanDao} untuk entitas
 * {@link ais.database.model.kedokteran.JenisPertemuan}. Kelas ini sengaja kosong: seluruh logika
 * CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc di sana
 * untuk detail perilaku method.
 */
public class JenisPertemuanDaoImpl extends
		GenericHibernateDao<JenisPertemuan, Long, JenisPertemuanDao> implements
		JenisPertemuanDao {

}
