package ais.database.dao;

import ais.database.model.NamaTugasKelompokPunyaMahasiswa;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.NamaTugasKelompokPunyaMahasiswa}
 * (data relasi nama tugas kelompok dengan anggota mahasiswanya). Kelas ini murni mewarisi
 * perilaku generik dari {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan --
 * lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class NamaTugasKelompokPunyaMahasiswaDaoImpl
		extends
		GenericHibernateDao<NamaTugasKelompokPunyaMahasiswa, Long, NamaTugasKelompokPunyaMahasiswaDao>
		implements NamaTugasKelompokPunyaMahasiswaDao {

}
