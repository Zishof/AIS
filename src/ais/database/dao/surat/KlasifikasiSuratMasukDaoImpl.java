package ais.database.dao.surat;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.surat.KlasifikasiSuratMasuk;

/**
 * Implementasi Hibernate {@link KlasifikasiSuratMasukDao} untuk entitas
 * {@link ais.database.model.surat.KlasifikasiSuratMasuk}. Kelas ini sengaja kosong: seluruh logika
 * CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc di sana
 * untuk detail perilaku method.
 */
public class KlasifikasiSuratMasukDaoImpl
		extends
		GenericHibernateDao<KlasifikasiSuratMasuk, Long, KlasifikasiSuratMasukDao>
		implements KlasifikasiSuratMasukDao {

}
