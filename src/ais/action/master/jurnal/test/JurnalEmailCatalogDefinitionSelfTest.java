package ais.action.master.jurnal.test;
import java.util.HashSet;import ais.action.master.jurnal.JurnalEmailTemplateCatalog;
/**
 * Harness uji manual yang memverifikasi kelengkapan dan konsistensi katalog template email jurnal
 * di {@link JurnalEmailTemplateCatalog}. Memeriksa: (1) jumlah kunci template tepat 73 dan jumlah
 * definisi (kunci x locale) tepat 146 — yaitu setiap kunci punya persis 2 definisi locale; (2)
 * tidak ada duplikat pasangan {@code key|locale}; (3) setiap definisi punya subjek dan isi yang
 * tidak kosong; (4) setiap variabel standar di {@code STANDARD_VARIABLES} benar-benar dipakai
 * (muncul sebagai placeholder {@code {{variabel}}}) di subjek atau isi tiap definisi. Melempar
 * {@link IllegalStateException} dengan pesan yang menyebutkan kunci/variabel bermasalah bila ada
 * pemeriksaan yang gagal — dimaksudkan sebagai pagar agar penambahan/pengubahan template email
 * jurnal tidak diam-diam menghilangkan locale atau variabel wajib.
 */
public final class JurnalEmailCatalogDefinitionSelfTest{private JurnalEmailCatalogDefinitionSelfTest(){}
	/** Menjalankan seluruh pemeriksaan cakupan dan konsistensi katalog template email jurnal; lihat javadoc kelas. */
	public static void main(String[]a){if(JurnalEmailTemplateCatalog.KEYS.size()!=73||JurnalEmailTemplateCatalog.definitions().size()!=146)throw new IllegalStateException("Coverage default email bukan 73x2.");HashSet<String>ids=new HashSet<String>();for(JurnalEmailTemplateCatalog.Definition d:JurnalEmailTemplateCatalog.definitions()){if(!ids.add(d.key+"|"+d.locale)||d.subject.length()==0||d.body.length()==0)throw new IllegalStateException("Definition email invalid: "+d.key);for(String v:JurnalEmailTemplateCatalog.STANDARD_VARIABLES)if(d.subject.indexOf("{{"+v+"}}")<0&&d.body.indexOf("{{"+v+"}}")<0)throw new IllegalStateException("Variable tidak digunakan: "+d.key+"/"+v);}System.out.println("JurnalEmailCatalogDefinitionSelfTest OK keys=73 locales=146 safe-default-definitions");}}
