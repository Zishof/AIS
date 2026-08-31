package ais.database.dao.employ;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.employ.UnitKerja;

/**
 * Implementasi Hibernate untuk {@link UnitKerjaDao}, mengelola entitas
 * {@link ais.database.model.employ.UnitKerja}. Kosong sesuai desain — seluruh logika CRUD generik
 * diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class UnitKerjaDaoImpl extends GenericHibernateDao<UnitKerja, Long, UnitKerjaDao>
		implements UnitKerjaDao {

}
