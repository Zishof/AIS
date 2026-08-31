package ais.database.dao.employ;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.employ.GajiPokok;

/**
 * Implementasi Hibernate untuk {@link GajiPokokDao}, mengelola entitas
 * {@link ais.database.model.employ.GajiPokok}. Kosong sesuai desain — seluruh logika CRUD generik
 * diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class GajiPokokDaoImpl extends
		GenericHibernateDao<GajiPokok, Long, GajiPokokDao> implements
		GajiPokokDao {

}
