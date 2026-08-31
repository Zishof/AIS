package ais.database.dao;

import ais.database.model.Bank;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.Bank} (data referensi bank). Pasangan
 * Dao/DaoImpl ini murni memakai perilaku generik {@link ais.database.dao.GenericDao} tanpa
 * method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface BankDao extends GenericDao<Bank, Long> {
    

}
