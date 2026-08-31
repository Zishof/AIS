package ais.database.dao.rab;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.rab.InformasiRab;

/**
 * Implementasi Hibernate untuk {@link InformasiRabDao}, mengelola entitas
 * {@link ais.database.model.rab.InformasiRab}. Kosong sesuai desain — seluruh logika CRUD generik
 * diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class InformasiRabDaoImpl
		extends
		GenericHibernateDao<InformasiRab, Long, InformasiRabDao>
		implements InformasiRabDao {

}
