package ais.database.dao;

import ais.database.model.BankSoal;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.BankSoal} (data bank soal ujian). Pasangan
 * Dao/DaoImpl ini murni memakai perilaku generik {@link ais.database.dao.GenericDao} tanpa
 * method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface BankSoalDao extends GenericDao<BankSoal, Long>{

}
