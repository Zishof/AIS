package ais.action.master.jurnal.test;
import ais.action.master.jurnal.JurnalEmailTemplateCatalog;import ais.action.master.jurnal.JurnalPluginCatalog;import ais.action.master.jurnal.importer.OjsSourceCatalog;import ais.common.JurnalAksesKatalog;
/**
 * Harness uji manual (bukan JUnit, dijalankan lewat {@code main}) yang memverifikasi konsistensi
 * jumlah/isi beberapa katalog statis modul jurnal (integrasi OJS): {@link JurnalEmailTemplateCatalog}
 * harus berisi tepat 73 kunci template email dan memuat kunci pertama/terakhir tertentu
 * ({@code PASSWORD_RESET_CONFIRM}, {@code ORCID_REQUEST_UPDATE_SCOPE}); {@code JurnalAksesKatalog.DAFTAR}
 * harus berisi 28 menu akses; {@link OjsSourceCatalog#TABLES} harus berisi 134 tabel sumber OJS; dan
 * {@link JurnalPluginCatalog#ALL} harus berisi 45 plugin dengan entri {@code themes/default} yang
 * dapat diambil. Berfungsi sebagai pengaman regresi agar perubahan katalog (penambahan/penghapusan
 * entri) disadari lewat kegagalan {@link IllegalStateException} saat harness dijalankan.
 */
public final class JurnalCatalogSelfTest{private JurnalCatalogSelfTest(){}public static void main(String[]a){check(JurnalEmailTemplateCatalog.KEYS.size()==73,"73 email keys");check(JurnalAksesKatalog.DAFTAR.size()==28,"28 menus");check(OjsSourceCatalog.TABLES.size()==134,"134 OJS tables");check(JurnalPluginCatalog.ALL.size()==45,"45 plugins");check(JurnalEmailTemplateCatalog.contains("PASSWORD_RESET_CONFIRM"),"first key");check(JurnalEmailTemplateCatalog.contains("ORCID_REQUEST_UPDATE_SCOPE"),"last key");check(JurnalPluginCatalog.get("themes/default")!=null,"last plugin");System.out.println("JurnalCatalogSelfTest OK");}private static void check(boolean v,String m){if(!v)throw new IllegalStateException(m);}}
