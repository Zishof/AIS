package ais.action.master.jurnal;
import java.util.*;
/** Canonical 45 bundled OJS plugin dispositions; implementation evidence is tracked per key. */
public final class JurnalPluginCatalog{
 /**
  * Tipe implementasi bersarang {@link Entry} milik {@link JurnalPluginCatalog}. Kelas ini memberi nama pada
  * state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
  *
  * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link JurnalPluginCatalog}.
  * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
  * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code int id}, {@code String key}, {@code
  * String disposition}, {@code String target}. Aturan bisnis bersama tetap berada pada kelas induk atau service
  * yang dipanggilnya.</p>
  *
  * @see JurnalPluginCatalog
  */
 public static final class Entry{public final int id;public final String key,disposition,target;Entry(int i,String k,String d,String t){id=i;key=k;disposition=d;target=t;}}
 public static final List<Entry> ALL;private static final Map<String,Entry> BY_KEY;
 static{String raw=
 "blocks/browse|IMPLEMENTED_NATIVE|browse;blocks/developedBy|NOT_APPLICABLE_WITH_PROOF|footer;blocks/information|IMPLEMENTED_NATIVE|information;blocks/languageToggle|MAPPED_TO_EXISTING|i18n;blocks/makeSubmission|IMPLEMENTED_NATIVE|submission CTA;blocks/subscription|IMPLEMENTED_NATIVE|subscription info;generic/announcementFeed|IMPLEMENTED_NATIVE|announcement feed;generic/citationStyleLanguage|IMPLEMENTED_NATIVE|citation;generic/credit|IMPLEMENTED_NATIVE|CRediT;generic/crossref|IMPLEMENTED_NATIVE|Crossref;generic/customBlockManager|IMPLEMENTED_NATIVE|blocks;generic/datacite|IMPLEMENTED_NATIVE|DataCite;generic/driver|IMPLEMENTED_NATIVE|DRIVER;generic/dublinCoreMeta|IMPLEMENTED_NATIVE|DC;generic/googleAnalytics|MAPPED_TO_EXISTING|analytics;generic/googleScholar|IMPLEMENTED_NATIVE|Scholar;generic/htmlArticleGalley|IMPLEMENTED_NATIVE|HTML galley;generic/jatsTemplate|IMPLEMENTED_NATIVE|JATS;generic/lensGalley|IMPLEMENTED_NATIVE|XML reader;generic/pdfJsViewer|IMPLEMENTED_NATIVE|PDF viewer;generic/pflPlugin|IMPLEMENTED_NATIVE|integrity facts;generic/recommendByAuthor|IMPLEMENTED_NATIVE|recommend author;generic/recommendBySimilarity|IMPLEMENTED_NATIVE|recommend similarity;generic/staticPages|IMPLEMENTED_NATIVE|static pages;generic/tinymce|MAPPED_TO_EXISTING|rich text;generic/usageEvent|IMPLEMENTED_NATIVE|usage;generic/webFeed|IMPLEMENTED_NATIVE|feed;importexport/doaj|IMPLEMENTED_NATIVE|DOAJ;importexport/native|IMPLEMENTED_NATIVE|native XML;importexport/pubmed|IMPLEMENTED_NATIVE|PubMed;importexport/users|IMPLEMENTED_NATIVE|users;metadata/dc11|IMPLEMENTED_NATIVE|DC11;oaiMetadataFormats/dc|IMPLEMENTED_NATIVE|OAI DC;oaiMetadataFormats/marc|IMPLEMENTED_NATIVE|OAI MARC;oaiMetadataFormats/marcxml|IMPLEMENTED_NATIVE|OAI MARCXML;oaiMetadataFormats/oaiJats|IMPLEMENTED_NATIVE|OAI JATS;oaiMetadataFormats/rfc1807|IMPLEMENTED_NATIVE|OAI RFC1807;paymethod/manual|MAPPED_TO_EXISTING|manual payment;paymethod/paypal|MAPPED_TO_EXISTING|payment abstraction;pubIds/urn|IMPLEMENTED_NATIVE|URN;reports/articles|IMPLEMENTED_NATIVE|article report;reports/counter|IMPLEMENTED_NATIVE|COUNTER;reports/reviewReport|IMPLEMENTED_NATIVE|review report;reports/subscriptions|IMPLEMENTED_NATIVE|subscription report;themes/default|MAPPED_TO_EXISTING|AIS theme";
 List<Entry>a=new ArrayList<Entry>();Map<String,Entry>m=new LinkedHashMap<String,Entry>();String[]rows=raw.split(";");for(int i=0;i<rows.length;i++){String[]p=rows[i].split("\\|",3);Entry e=new Entry(i+1,p[0],p[1],p[2]);a.add(e);m.put(e.key,e);}if(a.size()!=45||m.size()!=45)throw new ExceptionInInitializerError("Plugin catalog must contain 45 unique entries");ALL=Collections.unmodifiableList(a);BY_KEY=Collections.unmodifiableMap(m);}
 private JurnalPluginCatalog(){}public static Entry get(String key){return BY_KEY.get(key);}
}
