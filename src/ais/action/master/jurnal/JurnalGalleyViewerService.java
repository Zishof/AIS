package ais.action.master.jurnal;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Safelist;
import ais.database.model.Tbmuser;
import ais.database.model.repository.RepoBitstream;

/** Safe, bounded renderers for public HTML and JATS journal galleys. */
public final class JurnalGalleyViewerService {
    public static final long MAX_RENDER_BYTES=2L*1024L*1024L;
    private static final Charset UTF8=Charset.forName("UTF-8");
    private final JurnalFileService files=new JurnalFileService();
    public static final class Rendered {public final Long bitstreamId;public final String title,bodyHtml;Rendered(Long id,String title,String body){bitstreamId=id;this.title=title;bodyHtml=body;}}
    public Rendered renderHtml(Long id,Tbmuser actor,String remote)throws Exception{RepoBitstream meta=metadata(id,actor,remote);String mime=lower(meta.getMimeType());if(!("text/html".equals(mime)||"application/xhtml+xml".equals(mime)))throw new IllegalArgumentException("File bukan galley HTML.");return new Rendered(meta.getId(),safeTitle(meta.getNamaFile()),sanitizeHtml(read(meta,actor,remote)));}
    public Rendered renderJats(Long id,Tbmuser actor,String remote)throws Exception{RepoBitstream meta=metadata(id,actor,remote);String mime=lower(meta.getMimeType());if(!("application/xml".equals(mime)||"text/xml".equals(mime)||"application/jats+xml".equals(mime)||"application/xhtml+xml".equals(mime)))throw new IllegalArgumentException("File bukan galley JATS/XML.");return new Rendered(meta.getId(),safeTitle(meta.getNamaFile()),jatsToAccessibleHtml(read(meta,actor,remote)));}
    public RepoBitstream requirePdf(Long id,Tbmuser actor,String remote)throws java.io.FileNotFoundException{RepoBitstream meta=metadata(id,actor,remote);if(!"application/pdf".equals(lower(meta.getMimeType())))throw new IllegalArgumentException("File bukan galley PDF.");return meta;}
    private RepoBitstream metadata(Long id,Tbmuser actor,String remote)throws java.io.FileNotFoundException{RepoBitstream meta=files.metadataForDownload(id,actor,remote);Long size=meta.getUkuranByte();if(size==null||size.longValue()<1||size.longValue()>MAX_RENDER_BYTES)throw new IllegalArgumentException("Galley terlalu besar untuk viewer aman.");return meta;}
    private String read(RepoBitstream meta,Tbmuser actor,String remote)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream((int)meta.getUkuranByte().longValue());files.stream(meta.getId(),actor,remote,out);if(out.size()!=meta.getUkuranByte().longValue())throw new IllegalStateException("Ukuran galley tidak konsisten.");return new String(out.toByteArray(),UTF8);}
    public static String sanitizeHtml(String raw){if(raw==null)return"";Safelist allow=new Safelist().addTags("article","section","header","footer","main","nav","aside","h1","h2","h3","h4","h5","h6","p","br","hr","blockquote","pre","code","strong","em","b","i","u","s","sub","sup","ul","ol","li","dl","dt","dd","table","caption","thead","tbody","tfoot","tr","th","td","a","span").addAttributes("a","href","title").addAttributes("th","scope","colspan","rowspan").addAttributes("td","colspan","rowspan").addProtocols("a","href","http","https","mailto").addEnforcedAttribute("a","rel","noopener noreferrer");return Jsoup.clean(raw,"",allow,new Document.OutputSettings().prettyPrint(false));}
    public static String jatsToAccessibleHtml(String xml){if(xml==null)return"";String upper=xml.toUpperCase(java.util.Locale.ENGLISH);if(upper.indexOf("<!DOCTYPE")>=0||upper.indexOf("<!ENTITY")>=0)throw new IllegalArgumentException("DTD/entity pada JATS tidak diizinkan.");Document doc=Jsoup.parse(xml,"",Parser.xmlParser());doc.select("script,style,iframe,object,embed").remove();StringBuilder out=new StringBuilder();Element title=doc.select("article-title").first();if(title!=null&&title.text().trim().length()>0)out.append("<h1>").append(html(title.text())).append("</h1>");Element abs=doc.select("abstract").first();if(abs!=null&&abs.text().trim().length()>0)out.append("<section><h2>Abstrak</h2><p>").append(html(abs.text())).append("</p></section>");for(Element sec:doc.select("body > sec")){Element heading=sec.select("title").first();out.append("<section>");if(heading!=null)out.append("<h2>").append(html(heading.text())).append("</h2>");for(Element p:sec.select("p"))out.append("<p>").append(html(p.text())).append("</p>");out.append("</section>");}if(out.length()==0)out.append("<pre>").append(html(doc.text())).append("</pre>");return out.toString();}
    private static String lower(String value){return value==null?"":value.trim().toLowerCase(java.util.Locale.ENGLISH);}
    private static String safeTitle(String value){String x=value==null?"Galley":value.replaceAll("[\\r\\n<>]"," ").trim();return x.length()==0?"Galley":x;}
    public static String html(String value){if(value==null)return"";return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
}
