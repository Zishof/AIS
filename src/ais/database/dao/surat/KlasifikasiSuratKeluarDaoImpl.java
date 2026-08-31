package ais.database.dao.surat;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.surat.KlasifikasiSuratKeluar;

/**
 * Implementasi Hibernate {@link KlasifikasiSuratKeluarDao} untuk entitas
 * {@link ais.database.model.surat.KlasifikasiSuratKeluar}. Kelas ini sengaja kosong: seluruh
 * logika CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc
 * di sana untuk detail perilaku method.
 */
public class KlasifikasiSuratKeluarDaoImpl
		extends
		GenericHibernateDao<KlasifikasiSuratKeluar, Long, KlasifikasiSuratKeluarDao>
		implements KlasifikasiSuratKeluarDao {

}
