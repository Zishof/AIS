package ais.action.master.jurnal;

import java.io.StringReader;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.repository.RepoCollection;
import ais.database.model.repository.RepoItem;

/** Strict AIS-native XML import. It creates drafts only and never imports executable configuration. */
public final class JurnalNativeImportService {
    private final JurnalAuthorizationService auth=new JurnalAuthorizationService();
    public RepoItem importDraft(Long collectionId,String xml,String idempotencyKey,Tbmuser actor){
        auth.requireCrud(actor,"pluginIntegrasi","create");if(xml==null||xml.length()==0||xml.length()>1048576)throw new IllegalArgumentException("Ukuran XML native tidak valid.");String key=token(idempotencyKey);
            Session s=HibernateUtil.currentSession();Transaction tx=s.getTransaction();boolean own=!tx.isActive();try{if(own)tx.begin();RepoCollection c=(RepoCollection)s.get(RepoCollection.class,collectionId);if(c==null||!Boolean.TRUE.equals(c.getAktif())||!"JOURNAL".equalsIgnoreCase(c.getTipe()))throw new IllegalArgumentException("Koleksi jurnal tidak ditemukan.");auth.requireCollectionScope(s,actor,collectionId,"JOURNAL");
            Query q=s.createQuery("from RepoItem where collectionId=:c and sourceClass='AIS_NATIVE_IMPORT' and sourceLabel=:k and aktif=true order by id");q.setLong("c",collectionId);q.setString("k",key);q.setMaxResults(1);RepoItem old=(RepoItem)q.uniqueResult();if(old!=null){if(own)tx.commit();return old;}
            Document d=parse(xml);Element root=d.getDocumentElement();if(!"ais-journal-article".equals(root.getTagName())||!"1".equals(root.getAttribute("schemaVersion")))throw new IllegalArgumentException("Kontrak XML native tidak didukung.");String title=text(root,"title",10000,true),abstractText=text(root,"abstract",262144,false),language=text(root,"language",30,false),authors=text(root,"authors",262144,false);
            RepoItem item=new JurnalWorkflowService().createDraft(collectionId,title,abstractText,language,actor,"native-import:"+key);item.setAuthors(authors);item.setSourceClass("AIS_NATIVE_IMPORT");item.setSourceLabel(key);item.setSyncStatus("IMPORTED_DRAFT");item.setSyncMessage("Identifier dan status publikasi source wajib divalidasi melalui workflow AIS.");s.update(item);if(own)tx.commit();return item;
        }catch(RuntimeException e){if(own&&tx.isActive())tx.rollback();throw e;}catch(Exception e){if(own&&tx.isActive())tx.rollback();throw new IllegalArgumentException("XML native tidak valid.",e);}
    }
    private static Document parse(String xml)throws Exception{DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);f.setFeature("http://xml.org/sax/features/external-general-entities",false);f.setFeature("http://xml.org/sax/features/external-parameter-entities",false);f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);f.setXIncludeAware(false);f.setExpandEntityReferences(false);try{f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");}catch(IllegalArgumentException ignored){}return f.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));}
    private static String text(Element root,String name,int max,boolean required){org.w3c.dom.NodeList n=root.getElementsByTagName(name);String v=n.getLength()==0?"":n.item(0).getTextContent().trim();if((required&&v.length()==0)||v.length()>max)throw new IllegalArgumentException("Field XML native tidak valid: "+name);return v;}
    private static String token(String v){String x=v==null?"":v.trim();if(x.length()<2||x.length()>255||!x.matches("[A-Za-z0-9._:/-]+"))throw new IllegalArgumentException("Idempotency key import tidak valid.");return x;}
}
