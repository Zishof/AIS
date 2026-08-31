package ais.database.dao.kedokteran;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.kedokteran.PertemuanHasDosen;

/**
 * Implementasi Hibernate {@link PertemuanHasDosenDao} untuk entitas
 * {@link ais.database.model.kedokteran.PertemuanHasDosen}. Kelas ini sengaja kosong: seluruh
 * logika CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc
 * di sana untuk detail perilaku method.
 */
public class PertemuanHasDosenDaoImpl extends
		GenericHibernateDao<PertemuanHasDosen, Long, PertemuanHasDosenDao>
		implements PertemuanHasDosenDao {

}
