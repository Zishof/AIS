package ais.database.dao;

import ais.database.model.BankSoalDetail;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.BankSoalDetail} (detail/butir soal dalam
 * suatu bank soal). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface BankSoalDetailDao extends GenericDao<BankSoalDetail, Long> {

}
