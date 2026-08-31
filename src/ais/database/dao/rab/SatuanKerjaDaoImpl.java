package ais.database.dao.rab;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.rab.SatuanKerja;

/**
 * Implementasi Hibernate untuk {@link SatuanKerjaDao}, mengelola entitas
 * {@link ais.database.model.rab.SatuanKerja}. Kosong sesuai desain — seluruh logika CRUD generik
 * diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class SatuanKerjaDaoImpl extends
		GenericHibernateDao<SatuanKerja, Long, SatuanKerjaDao> implements
		SatuanKerjaDao {

}
