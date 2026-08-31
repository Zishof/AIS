package ais.database.dao.surat;

import ais.database.dao.GenericDao;
import ais.database.model.surat.SuratMasuk;

/**
 * DAO untuk entitas {@link ais.database.model.surat.SuratMasuk} — surat masuk, entitas inti modul
 * tata naskah dinas/persuratan untuk surat yang diterima dari pihak luar. Interface ini sengaja
 * kosong: seluruh kontrak CRUD generik diwariskan dari {@link ais.database.dao.GenericDao}, lihat
 * Javadoc di sana untuk detail perilaku method.
 */
public interface SuratMasukDao extends GenericDao<SuratMasuk, Long>{

}
