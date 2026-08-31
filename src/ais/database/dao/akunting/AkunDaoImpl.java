package ais.database.dao.akunting;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.akunting.Akun;

/**
 * Implementasi Hibernate {@link AkunDao} untuk entitas {@link ais.database.model.akunting.Akun}.
 * Kelas ini sengaja kosong: seluruh logika CRUD generik diwariskan dari
 * {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc di sana untuk detail perilaku method.
 */
public class AkunDaoImpl extends GenericHibernateDao<Akun, Long, AkunDao> implements AkunDao{

}
