package ais.database.dao.surat;

import ais.database.dao.GenericDao;
import ais.database.model.surat.SifatSurat;

/**
 * DAO untuk entitas {@link ais.database.model.surat.SifatSurat} — sifat surat (mis. biasa,
 * penting, rahasia) sebagai atribut klasifikasi surat masuk/keluar (modul tata naskah dinas/
 * persuratan). Interface ini sengaja kosong: seluruh kontrak CRUD generik diwariskan dari
 * {@link ais.database.dao.GenericDao}, lihat Javadoc di sana untuk detail perilaku method.
 */
public interface SifatSuratDao extends GenericDao<SifatSurat, Long> {

}
