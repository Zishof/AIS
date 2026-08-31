package ais.database.dao.surat;

import ais.database.dao.GenericDao;
import ais.database.model.surat.KlasifikasiSuratKeluar;

/**
 * DAO untuk entitas {@link ais.database.model.surat.KlasifikasiSuratKeluar} — klasifikasi/kategori
 * surat keluar (modul tata naskah dinas/persuratan). Interface ini sengaja kosong: seluruh kontrak
 * CRUD generik diwariskan dari {@link ais.database.dao.GenericDao}, lihat Javadoc di sana untuk
 * detail perilaku method.
 */
public interface KlasifikasiSuratKeluarDao extends GenericDao<KlasifikasiSuratKeluar, Long>{

}
