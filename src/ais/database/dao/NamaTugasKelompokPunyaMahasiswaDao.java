package ais.database.dao;

import ais.database.model.NamaTugasKelompokPunyaMahasiswa;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.NamaTugasKelompokPunyaMahasiswa} (data
 * relasi nama tugas kelompok dengan anggota mahasiswanya). Pasangan Dao/DaoImpl ini murni
 * memakai perilaku generik {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat
 * javadoc di sana untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface NamaTugasKelompokPunyaMahasiswaDao extends
		GenericDao<NamaTugasKelompokPunyaMahasiswa, Long> {

}
