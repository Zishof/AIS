package ais.action.master.jurnal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ais.database.model.repository.RepoItem;
import ais.database.model.repository.RepoItemMetadata;

/**
 * Native, dependency-free metadata serializers shared by OAI and export jobs.
 * Values are always XML escaped and only Repository allowlisted fields are read.
 */
public final class JurnalMetadataFormatService {
    /**
     * Tipe implementasi bersarang {@link Format} milik {@link JurnalMetadataFormatService}. Kelas ini memberi nama
     * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * JurnalMetadataFormatService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String prefix}, {@code String
     * schema}, {@code String namespace}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang
     * dipanggilnya.</p>
     *
     * @see JurnalMetadataFormatService
     */
    public static final class Format {
        public final String prefix;
        public final String schema;
        public final String namespace;
        private Format(String prefix,String schema,String namespace){this.prefix=prefix;this.schema=schema;this.namespace=namespace;}
    }

    private static final Map<String,Format> FORMATS;
    static {
        Map<String,Format> m=new LinkedHashMap<String,Format>();
        add(m,"oai_dc","http://www.openarchives.org/OAI/2.0/oai_dc.xsd","http://www.openarchives.org/OAI/2.0/oai_dc/");
        add(m,"marc","http://www.openarchives.org/OAI/1.1/oai_marc.xsd","http://www.openarchives.org/OAI/1.1/oai_marc");
        add(m,"marcxml","https://www.loc.gov/standards/marcxml/schema/MARC21.xsd","http://www.loc.gov/MARC21/slim");
        add(m,"oai_jats","https://jats.nlm.nih.gov/publishing/1.3/xsd/JATS-journalpublishing1-3.xsd","https://jats.nlm.nih.gov");
        add(m,"rfc1807","http://www.openarchives.org/OAI/1.1/rfc1807.xsd","http://info.internet.isi.edu/in-notes/rfc/files/rfc1807.txt");
        FORMATS=Collections.unmodifiableMap(m);
    }
    private static void add(Map<String,Format> m,String p,String s,String n){m.put(p,new Format(p,s,n));}
    public List<Format> formats(){return new ArrayList<Format>(FORMATS.values());}
    public boolean supports(String prefix){return prefix!=null&&FORMATS.containsKey(prefix.trim());}

    public String serialize(String prefix,RepoItem item,List<RepoItemMetadata> metadata){
        return serialize(prefix,item,metadata,"");
    }

    /** Serializes metadata and optionally adds the canonical public landing-page URL. */
    public String serialize(String prefix,RepoItem item,List<RepoItemMetadata> metadata,String publicIdentifier){
        if(!supports(prefix))throw new IllegalArgumentException("Metadata format tidak didukung: "+prefix);
        if(item==null)throw new IllegalArgumentException("Item wajib diisi.");
        Map<String,List<String>> dc=dc(item,metadata);
        if(has(publicIdentifier)&&!dc.get("identifier").contains(publicIdentifier.trim()))dc.get("identifier").add(publicIdentifier.trim());
        String p=prefix.trim();
        if("marc".equals(p))return marc(dc,false);
        if("marcxml".equals(p))return marc(dc,true);
        if("oai_jats".equals(p))return jats(dc,item);
        if("rfc1807".equals(p))return rfc1807(dc);
        return oaiDc(dc);
    }

    private String oaiDc(Map<String,List<String>> dc){
        StringBuilder x=new StringBuilder("<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">");
        for(Map.Entry<String,List<String>> e:dc.entrySet())for(String v:e.getValue())if(has(v))x.append("<dc:").append(e.getKey()).append('>').append(xml(v)).append("</dc:").append(e.getKey()).append('>');
        return x.append("</oai_dc:dc>").toString();
    }

    private String marc(Map<String,List<String>> dc,boolean slim){
        String ns=slim?"http://www.loc.gov/MARC21/slim":"http://www.openarchives.org/OAI/1.1/oai_marc";
        StringBuilder x=new StringBuilder("<record xmlns=\"").append(ns).append("\"><leader>00000nam a2200000 i 4500</leader>");
        field(x,"245","a",first(dc,"title"));field(x,"100","a",first(dc,"creator"));field(x,"520","a",first(dc,"description"));
        for(String v:dc.get("subject"))field(x,"650","a",v);
        field(x,"260","b",first(dc,"publisher"));field(x,"260","c",first(dc,"date"));field(x,"041","a",first(dc,"language"));
        for(String v:dc.get("identifier"))field(x,"024","a",v);
        field(x,"540","a",first(dc,"rights"));return x.append("</record>").toString();
    }
    private void field(StringBuilder x,String tag,String code,String value){if(has(value))x.append("<datafield tag=\"").append(tag).append("\" ind1=\" \" ind2=\" \"><subfield code=\"").append(code).append("\">").append(xml(value)).append("</subfield></datafield>");}

    private String jats(Map<String,List<String>> dc,RepoItem item){
        StringBuilder x=new StringBuilder("<article xmlns:xlink=\"http://www.w3.org/1999/xlink\" article-type=\"research-article\"><front><article-meta><article-id pub-id-type=\"oai\">").append(xml(item.getOaiIdentifier())).append("</article-id><title-group><article-title>").append(xml(first(dc,"title"))).append("</article-title></title-group>");
        x.append("<contrib-group>");for(String a:dc.get("creator"))if(has(a))x.append("<contrib contrib-type=\"author\"><string-name>").append(xml(a)).append("</string-name></contrib>");x.append("</contrib-group>");
        if(has(first(dc,"description")))x.append("<abstract><p>").append(xml(first(dc,"description"))).append("</p></abstract>");
        return x.append("</article-meta></front></article>").toString();
    }

    private String rfc1807(Map<String,List<String>> dc){
        StringBuilder x=new StringBuilder("<rfc1807 xmlns=\"http://info.internet.isi.edu/in-notes/rfc/files/rfc1807.txt\">");
        tag(x,"title",first(dc,"title"));for(String v:dc.get("creator"))tag(x,"author",v);tag(x,"organization",first(dc,"publisher"));tag(x,"date",first(dc,"date"));tag(x,"abstract",first(dc,"description"));for(String v:dc.get("identifier"))tag(x,"id",v);return x.append("</rfc1807>").toString();
    }
    private void tag(StringBuilder x,String n,String v){if(has(v))x.append('<').append(n).append('>').append(xml(v)).append("</").append(n).append('>');}

    private Map<String,List<String>> dc(RepoItem i,List<RepoItemMetadata> metadata){
        Map<String,List<String>> d=new LinkedHashMap<String,List<String>>();
        put(d,"title",i.getTitle());putMany(d,"creator",i.getAuthors());putMany(d,"subject",i.getSubjects());put(d,"description",i.getAbstractText());put(d,"publisher",i.getPublisher());put(d,"date",iso(i.getIssuedAt()));put(d,"type",i.getDocumentType());put(d,"language",i.getLanguage());put(d,"rights",i.getAccessPolicy());put(d,"identifier",i.getOaiIdentifier());if(has(i.getDspaceHandle()))d.get("identifier").add("https://hdl.handle.net/"+i.getDspaceHandle());
        for(String k:Arrays.asList("contributor","format","source","relation","coverage"))d.put(k,new ArrayList<String>());
        if(metadata!=null)for(RepoItemMetadata m:metadata){String key=mapField(m.getMetadataField());if(key!=null&&has(m.getMetadataValue())){String value=m.getMetadataValue().trim();if(!d.get(key).contains(value))d.get(key).add(value);}}
        return d;
    }
    private String mapField(String f){if(f==null)return null;if(f.startsWith("dc.title"))return"title";if(f.startsWith("dc.contributor.author"))return"creator";if(f.startsWith("dc.contributor"))return"contributor";if(f.startsWith("dc.subject"))return"subject";if(f.startsWith("dc.description"))return"description";if(f.startsWith("dc.publisher"))return"publisher";if(f.startsWith("dc.date"))return"date";if(f.startsWith("dc.type"))return"type";if(f.startsWith("dc.format"))return"format";if(f.startsWith("dc.identifier"))return"identifier";if(f.startsWith("dc.source"))return"source";if(f.startsWith("dc.language"))return"language";if(f.startsWith("dc.relation"))return"relation";if(f.startsWith("dc.coverage"))return"coverage";if(f.startsWith("dc.rights"))return"rights";return null;}
    private void put(Map<String,List<String>> d,String k,String v){List<String>a=new ArrayList<String>();if(has(v))a.add(v);d.put(k,a);}private void putMany(Map<String,List<String>> d,String k,String v){List<String>a=new ArrayList<String>();if(has(v))for(String p:v.split("[\\r\\n;]+"))if(has(p))a.add(p.trim());d.put(k,a);}private String first(Map<String,List<String>>d,String k){List<String>x=d.get(k);return x==null||x.isEmpty()?"":x.get(0);}private boolean has(String v){return v!=null&&!v.trim().isEmpty();}
    private String iso(Date d){return d==null?"":String.format(java.util.Locale.ROOT,"%tFT%<tTZ",d);}public static String xml(String v){return v==null?"":v.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");}
}
