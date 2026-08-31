package ais.database.dao.surat;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.surat.SuratMasuk;

/**
 * Implementasi Hibernate {@link SuratMasukDao} untuk entitas {@link ais.database.model.surat.SuratMasuk}.
 * Kelas ini sengaja kosong: seluruh logika CRUD generik diwariskan dari
 * {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc di sana untuk detail perilaku method.
 */
public class SuratMasukDaoImpl extends
		GenericHibernateDao<SuratMasuk, Long, SuratMasukDao> implements
		SuratMasukDao {

}
