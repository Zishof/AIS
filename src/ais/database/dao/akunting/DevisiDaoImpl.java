package ais.database.dao.akunting;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.akunting.Devisi;

/**
 * Implementasi Hibernate {@link DevisiDao} untuk entitas {@link ais.database.model.akunting.Devisi}.
 * Kelas ini sengaja kosong: seluruh logika CRUD generik diwariskan dari
 * {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc di sana untuk detail perilaku method.
 */
public class DevisiDaoImpl extends GenericHibernateDao<Devisi, Long, DevisiDao> implements DevisiDao{

}
