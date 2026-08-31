package ais.database.dao.employ;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.employ.KenaikanGajiBerkala;

/**
 * Implementasi Hibernate untuk {@link KenaikanGajiBerkalaDao}, mengelola entitas
 * {@link ais.database.model.employ.KenaikanGajiBerkala}. Kosong sesuai desain — seluruh logika
 * CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class KenaikanGajiBerkalaDaoImpl extends
		GenericHibernateDao<KenaikanGajiBerkala, Long, KenaikanGajiBerkalaDao> implements
		KenaikanGajiBerkalaDao {

}
