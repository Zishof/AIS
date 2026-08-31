package ais.database.dao.kedokteran;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.kedokteran.PertemuanKedokteran;

/**
 * Implementasi Hibernate {@link PertemuanKedokteranDao} untuk entitas
 * {@link ais.database.model.kedokteran.PertemuanKedokteran}. Kelas ini sengaja kosong: seluruh
 * logika CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc
 * di sana untuk detail perilaku method.
 */
public class PertemuanKedokteranDaoImpl extends
		GenericHibernateDao<PertemuanKedokteran, Long, PertemuanKedokteranDao> implements
		PertemuanKedokteranDao {

}
