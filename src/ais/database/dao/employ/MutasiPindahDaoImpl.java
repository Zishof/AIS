package ais.database.dao.employ;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.employ.MutasiPindah;

/**
 * Implementasi Hibernate untuk {@link MutasiPindahDao}, mengelola entitas
 * {@link ais.database.model.employ.MutasiPindah}. Kosong sesuai desain — seluruh logika CRUD
 * generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class MutasiPindahDaoImpl extends
		GenericHibernateDao<MutasiPindah, Long, MutasiPindahDao> implements
		MutasiPindahDao {

}
