package ais.database.dao.rab;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.rab.RenstraProgram;

/**
 * Implementasi Hibernate untuk {@link RenstraProgramDao}, mengelola entitas
 * {@link ais.database.model.rab.RenstraProgram}. Kosong sesuai desain — seluruh logika CRUD
 * generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class RenstraProgramDaoImpl
		extends
		GenericHibernateDao<RenstraProgram, Long, RenstraProgramDao>
		implements RenstraProgramDao {

}
