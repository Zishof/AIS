package ais.database.dao;

import ais.database.model.BankHost;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.BankHost} (data host/koneksi integrasi
 * layanan bank, mis. untuk pembayaran). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface BankHostDao extends GenericDao<BankHost, Long>{

}
