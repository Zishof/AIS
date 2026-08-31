package ais.database.dao;

import ais.database.model.PaketJurusanPmb;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.PaketJurusanPmb} (data paket
 * jurusan PMB/penerimaan mahasiswa baru). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class PaketJurusanPmbDaoImpl extends
		GenericHibernateDao<PaketJurusanPmb, Long, PaketJurusanPmbDao>
		implements PaketJurusanPmbDao {

}
