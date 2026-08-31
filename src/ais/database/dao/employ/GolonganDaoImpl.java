package ais.database.dao.employ;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.employ.Golongan;

/**
 * Implementasi Hibernate untuk {@link GolonganDao}, mengelola entitas
 * {@link ais.database.model.employ.Golongan}. Kosong sesuai desain — seluruh logika CRUD generik
 * diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class GolonganDaoImpl extends GenericHibernateDao<Golongan, Long, GolonganDao>
		implements GolonganDao {

}
