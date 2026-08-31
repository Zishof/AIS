package ais.database.dao;

import ais.database.model.PesanRuangan;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.PesanRuangan} (data pemesanan
 * ruangan). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class PesanRuanganDaoImpl extends
		GenericHibernateDao<PesanRuangan, Long, PesanRuanganDao> implements
		PesanRuanganDao {

}
