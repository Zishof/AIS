package ais.action.master.library.modern;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilderFactory;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import ais.common.Common;
import ais.common.newui.NewUiCsrfUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.library.Item;
import ais.database.model.library.Penerbit;

/** Typed MARCXML preview/import/export endpoint for catalogers. */
public final class LibraryMarcApi {
    private static final int MAX_RECORDS = 200;
    private static final int MAX_XML = 2 * 1024 * 1024;
    private LibraryMarcApi() { }

    public static JSONObject handle(HttpServletRequest request) throws Exception {
        Tbmuser user = Common.getCurrentUser(request);
        if (user == null || !Common.getApakahAdmin()) return error("Hak kataloger/administrator diperlukan.");
        String action = text(request.getParameter("action"), 40);
        if ("export".equals(action)) return exportRecord(positiveLong(request.getParameter("itemId")));
        if (!"POST".equalsIgnoreCase(request.getMethod())) return error("Preview dan import hanya melalui POST.");
        if (!NewUiCsrfUtil.isValid(request)) return error("Token keamanan tidak valid.");
        String xml = request.getParameter("xml");
        if (xml == null || xml.trim().length() == 0 || xml.length() > MAX_XML) return error("MARCXML wajib diisi dan maksimal 2 MB.");
        List<MarcRecord> records = parse(xml);
        if ("preview".equals(action)) return preview(records, request);
        if ("import".equals(action)) return importRecords(records, user, "true".equalsIgnoreCase(request.getParameter("allowDuplicates")), request);
        return error("Operasi MARC tidak dikenal.");
    }

    private static JSONObject preview(List<MarcRecord> records, HttpServletRequest request) throws Exception {
        Session session=null; try{session=HibernateUtil.openSession();JSONArray data=new JSONArray();int duplicates=0;
            for(MarcRecord record:records){long count=duplicateCount(session,record);if(count>0)duplicates++;data.put(record.json().put("duplicateCount",count));}
            return ok(request).put("data",data).put("total",records.size()).put("duplicates",duplicates);
        }finally{HibernateUtil.closeSessionQuietly(session);}
    }

    private static JSONObject importRecords(List<MarcRecord> records,Tbmuser user,boolean allowDuplicates,HttpServletRequest request)throws Exception{
        Session session=null;Transaction tx=null;int imported=0,skipped=0;JSONArray result=new JSONArray();
        try{session=HibernateUtil.openSession();tx=session.beginTransaction();
            for(MarcRecord record:records){long duplicates=duplicateCount(session,record);if(duplicates>0&&!allowDuplicates){skipped++;result.put(record.json().put("status","SKIPPED_DUPLICATE"));continue;}
                Item item=new Item();item.setKode("MARC-"+System.currentTimeMillis()+"-"+(imported+1));item.setNama(record.title);item.setIsbn(record.isbn);item.setIssn(record.issn);item.setPengarangs(record.author);item.setEdisi(record.edition);item.setTahun(record.year);item.setBahasa(record.language);item.setAbstrak(record.description);item.setKategories(record.subject);item.setDeweyDecimalClass(record.classification);item.setCatatan("Impor MARCXML; 001="+safe(record.controlNumber));item.setTanggal(new Date());item.setAktif(true);item.setDibuatOleh(user);
                if(record.publisher!=null)item.setPenerbit(publisher(session,record.publisher));session.save(item);imported++;result.put(record.json().put("status","IMPORTED").put("itemId",item.getId()));
            }tx.commit();return ok(request).put("data",result).put("imported",imported).put("skipped",skipped);
        }catch(Exception e){rollback(tx);throw e;}finally{HibernateUtil.closeSessionQuietly(session);}
    }

    @SuppressWarnings("unchecked")
    private static JSONObject exportRecord(Long itemId)throws Exception{
        if(itemId==null)return error("Item tidak valid.");Session session=null;try{session=HibernateUtil.openSession();Item item=(Item)session.get(Item.class,itemId);if(item==null)return error("Item tidak ditemukan.");
            StringBuilder x=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><collection xmlns=\"http://www.loc.gov/MARC21/slim\"><record>");
            control(x,"001",safe(item.getKode()));data(x,"020","a",safe(item.getIsbn()));data(x,"022","a",safe(item.getIssn()));data(x,"100","a",safe(item.getPengarangs()));data(x,"245","a",safe(item.getNama()));data(x,"250","a",safe(item.getEdisi()));
            data(x,"264","b",item.getPenerbit()==null?"":safe(item.getPenerbit().getNama()));data(x,"264","c",item.getTahun()==null?"":String.valueOf(item.getTahun()));data(x,"041","a",safe(item.getBahasa()));data(x,"082","a",safe(item.getDeweyDecimalClass()));data(x,"520","a",safe(item.getAbstrak()));data(x,"650","a",safe(item.getKategories()));x.append("</record></collection>");
            return new JSONObject().put("ok",true).put("status","success").put("xml",x.toString()).put("filename","marc-item-"+itemId+".xml");
        }finally{HibernateUtil.closeSessionQuietly(session);}
    }

    private static List<MarcRecord> parse(String xml)throws Exception{
        DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();factory.setNamespaceAware(true);factory.setXIncludeAware(false);factory.setExpandEntityReferences(false);
        feature(factory,"http://apache.org/xml/features/disallow-doctype-decl",true);feature(factory,"http://xml.org/sax/features/external-general-entities",false);feature(factory,"http://xml.org/sax/features/external-parameter-entities",false);feature(factory,"http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
        Document document=factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));NodeList nodes=document.getElementsByTagNameNS("*","record");if(nodes.getLength()==0)throw new IllegalArgumentException("MARCXML tidak memiliki record.");if(nodes.getLength()>MAX_RECORDS)throw new IllegalArgumentException("Maksimal 200 record per import.");
        List<MarcRecord> records=new ArrayList<MarcRecord>();for(int i=0;i<nodes.getLength();i++){Element e=(Element)nodes.item(i);MarcRecord r=new MarcRecord();r.controlNumber=control(e,"001");r.isbn=cleanIdentifier(sub(e,"020","a"));r.issn=cleanIdentifier(sub(e,"022","a"));r.author=first(sub(e,"100","a"),sub(e,"110","a"),sub(e,"700","a"));r.title=join(sub(e,"245","a"),sub(e,"245","b"));r.edition=sub(e,"250","a");r.publisher=first(sub(e,"264","b"),sub(e,"260","b"));r.year=year(first(sub(e,"264","c"),sub(e,"260","c")));r.language=sub(e,"041","a");r.classification=sub(e,"082","a");r.description=sub(e,"520","a");r.subject=sub(e,"650","a");if(r.title==null)throw new IllegalArgumentException("Setiap record wajib memiliki judul MARC 245$a.");records.add(r);}return records;
    }

    private static String control(Element record,String tag){NodeList list=record.getElementsByTagNameNS("*","controlfield");for(int i=0;i<list.getLength();i++){Element e=(Element)list.item(i);if(tag.equals(e.getAttribute("tag")))return trim(e.getTextContent(),255);}return null;}
    private static String sub(Element record,String tag,String code){NodeList fields=record.getElementsByTagNameNS("*","datafield");for(int i=0;i<fields.getLength();i++){Element f=(Element)fields.item(i);if(!tag.equals(f.getAttribute("tag")))continue;NodeList children=f.getChildNodes();for(int j=0;j<children.getLength();j++){Node n=children.item(j);if(n instanceof Element&&"subfield".equals(n.getLocalName())&&code.equals(((Element)n).getAttribute("code")))return trim(n.getTextContent(),4000);}}return null;}
    private static long duplicateCount(Session s,MarcRecord r){String sql="select count(id) from library.item where coalesce(aktif,true)=true and (";if(r.isbn!=null)sql+="regexp_replace(coalesce(isbn,''),'[^0-9Xx]','','g')=:isbn or ";sql+="(lower(trim(coalesce(nama,'')))=:title and lower(trim(coalesce(pengarangs,'')))=:author))";Query q=s.createSQLQuery(sql);if(r.isbn!=null)q.setString("isbn",r.isbn);q.setString("title",r.title.toLowerCase().trim());q.setString("author",safe(r.author).toLowerCase().trim());return number(q.uniqueResult());}
    private static Penerbit publisher(Session s,String name){Penerbit p=(Penerbit)s.createCriteria(Penerbit.class).add(org.hibernate.criterion.Restrictions.ilike("nama",name,org.hibernate.criterion.MatchMode.EXACT)).setMaxResults(1).uniqueResult();if(p==null){p=new Penerbit();p.setNama(name);p.setKeterangan("Dibuat dari impor MARCXML");s.save(p);}return p;}
    private static void control(StringBuilder x,String tag,String value){if(value.length()>0)x.append("<controlfield tag=\"").append(tag).append("\">").append(esc(value)).append("</controlfield>");}
    private static void data(StringBuilder x,String tag,String code,String value){if(value.length()>0)x.append("<datafield tag=\"").append(tag).append("\" ind1=\" \" ind2=\" \"><subfield code=\"").append(code).append("\">").append(esc(value)).append("</subfield></datafield>");}
    private static void feature(DocumentBuilderFactory f,String name,boolean value)throws Exception{f.setFeature(name,value);}
    private static String cleanIdentifier(String v){if(v==null)return null;v=v.replaceAll("[^0-9Xx]","");return v.length()==0?null:v;}
    private static Integer year(String v){if(v==null)return null;java.util.regex.Matcher m=java.util.regex.Pattern.compile("(1[5-9][0-9]{2}|20[0-9]{2}|21[0-9]{2})").matcher(v);return m.find()?Integer.valueOf(m.group(1)):null;}
    private static String first(String a,String b,String c){return a!=null?a:(b!=null?b:c);}private static String first(String a,String b){return a!=null?a:b;}
    private static String join(String a,String b){String v=(safe(a)+" "+safe(b)).trim();return v.length()==0?null:v;}
    private static String trim(String v,int max){if(v==null)return null;v=v.trim().replaceAll("\\s+"," ");return v.length()==0?null:(v.length()>max?v.substring(0,max):v);}
    private static String text(String v,int max){return trim(v,max);}private static String safe(String v){return v==null?"":v;}
    private static long number(Object v){return v instanceof Number?((Number)v).longValue():0L;}private static Long positiveLong(String v){try{long n=Long.parseLong(v);return n>0?Long.valueOf(n):null;}catch(Exception e){return null;}}
    private static String esc(String v){return safe(v).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
    private static JSONObject ok(HttpServletRequest r)throws Exception{return new JSONObject().put("ok",true).put("status","success").put("csrf",NewUiCsrfUtil.getToken(r.getSession()));}private static JSONObject error(String m)throws Exception{return new JSONObject().put("ok",false).put("status","error").put("error",m).put("message",m);}
    private static void rollback(Transaction tx){try{if(tx!=null&&tx.isActive())tx.rollback();}catch(Exception ignored){}}

    private static final class MarcRecord{String controlNumber,isbn,issn,author,title,edition,publisher,language,classification,description,subject;Integer year;JSONObject json()throws Exception{return new JSONObject().put("controlNumber",safe(controlNumber)).put("isbn",safe(isbn)).put("issn",safe(issn)).put("author",safe(author)).put("title",safe(title)).put("edition",safe(edition)).put("publisher",safe(publisher)).put("year",year==null?JSONObject.NULL:year).put("language",safe(language)).put("classification",safe(classification)).put("description",safe(description)).put("subject",safe(subject));}}
}
