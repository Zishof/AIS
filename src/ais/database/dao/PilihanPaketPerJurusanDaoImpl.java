package ais.database.dao;

import ais.database.model.PilihanPaketPerJurusanMhsBaru;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.PilihanPaketPerJurusanMhsBaru}
 * (data pilihan paket per jurusan untuk mahasiswa baru). Kelas ini murni mewarisi perilaku
 * generik dari {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat
 * javadoc di sana untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class PilihanPaketPerJurusanDaoImpl
		extends
		GenericHibernateDao<PilihanPaketPerJurusanMhsBaru, Long, PilihanPaketPerJurusanDao>
		implements PilihanPaketPerJurusanDao {

}
