package ais.database.dao.akunting;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.akunting.GrupTransaksi;

/**
 * Implementasi Hibernate {@link GrupTransaksiDao} untuk entitas
 * {@link ais.database.model.akunting.GrupTransaksi}. Kelas ini sengaja kosong: seluruh logika CRUD
 * generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc di sana
 * untuk detail perilaku method.
 */
public class GrupTransaksiDaoImpl extends
		GenericHibernateDao<GrupTransaksi, Long, GrupTransaksiDao> implements
		GrupTransaksiDao {

}
