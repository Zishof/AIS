package ais.database.dao;

import ais.database.model.NamaTugasKelompok;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.NamaTugasKelompok} (data nama tugas
 * kelompok). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface NamaTugasKelompokDao extends
		GenericDao<NamaTugasKelompok, Long> {

}
