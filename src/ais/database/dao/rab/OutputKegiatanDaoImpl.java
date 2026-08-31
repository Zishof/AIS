package ais.database.dao.rab;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.rab.OutputKegiatan;

/**
 * Implementasi Hibernate untuk {@link OutputKegiatanDao}, mengelola entitas
 * {@link ais.database.model.rab.OutputKegiatan}. Kosong sesuai desain — seluruh logika CRUD
 * generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class OutputKegiatanDaoImpl extends
		GenericHibernateDao<OutputKegiatan, Long, OutputKegiatanDao> implements
		OutputKegiatanDao {

}
