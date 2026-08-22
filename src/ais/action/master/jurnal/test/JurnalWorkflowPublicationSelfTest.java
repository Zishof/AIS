package ais.action.master.jurnal.test;

import java.util.Date;
import java.util.HashSet;
import java.io.StringWriter;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONObject;
import ais.action.master.jurnal.JurnalAdministrationService;
import ais.action.master.jurnal.JurnalContributorService;
import ais.action.master.jurnal.JurnalIdentifierService;
import ais.action.master.jurnal.JurnalPublicationService;
import ais.action.master.jurnal.JurnalPublicService;
import ais.action.master.jurnal.JurnalReportService;
import ais.action.master.jurnal.JurnalPluginParityService;
import ais.action.master.jurnal.JurnalNativeImportService;
import ais.action.master.jurnal.JurnalAnnouncementService;
import ais.action.master.jurnal.JurnalWorkflowService;
import ais.common.JurnalAksesKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Menu;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.jurnal.PenugasanReviewerJurnal;
import ais.database.model.penelitiandanpengabdian.JurnalPenelitian;
import ais.database.model.repository.RepoItem;
import ais.database.model.repository.RepoCollection;

/** Rollback-only submit-review-production-publish compatibility projection journey. */
public final class JurnalWorkflowPublicationSelfTest {
    private JurnalWorkflowPublicationSelfTest() {}

    public static void main(String[] args) throws Exception {
        String target = System.getenv("AIS_JURNAL_DB_NAME");
        if (target == null || target.trim().length() == 0 || "ais".equalsIgnoreCase(target.trim()))
            throw new IllegalStateException("Test wajib diarahkan ke clone main SIT/UAT.");
        System.setProperty("javax.persistence.validation.mode", "none");
        Tbmuser manager = user("JRN_WORKFLOW_MANAGER", true, true);
        Tbmuser reviewer = user("JRN_WORKFLOW_REVIEWER", false, false);
        Session session = HibernateUtil.currentSession();
        Transaction tx = session.beginTransaction();
        try {
            JurnalPenelitian journal = new JurnalAdministrationService().create("self-test",
                    "Journal Workflow Self Test", "journal-workflow-self-test", "id_ID", manager);
            RepoCollection journalCollection=(RepoCollection)session.get(RepoCollection.class,journal.getRepoCollectionId());journalCollection.setMetadataProfileJson("{\"schemaVersion\":1,\"publication\":{\"journalTitle\":\"Journal Workflow Self Test\",\"publisher\":\"eCampus AIS\",\"issn\":\"1234-5679\"},\"publicPages\":[{\"slug\":\"about\",\"title\":\"About Self Test\",\"bodyText\":\"Plain text <script>must be escaped by JSP</script>\",\"active\":true}]}");session.update(journalCollection);
            RepoItem article = new JurnalWorkflowService().createDraft(journal.getRepoCollectionId(),
                    "Integrated workflow article", "Rollback-only abstract", "id", manager, "workflow-self-test");
            article.setSubjects("Education; Open Science");
            new JurnalContributorService().addExternal(article.getId(), "External Author",
                    "external.author@example.invalid", "0000-0002-1825-0097", "AIS Test", "",
                    "AUTHOR", 0, true, manager);
            JurnalWorkflowService workflow = new JurnalWorkflowService();
            workflow.transition(article.getId(), null, "SUBMITTED", "SUBMIT", null, manager, "workflow-self-test");
            workflow.transition(article.getId(), null, "SCREENING", "START_SCREENING", null, manager, "workflow-self-test");
            PenugasanReviewerJurnal assignment = workflow.inviteReviewer(article.getId(), reviewer.getUserId(), 1,
                    "DOUBLE_ANONYMOUS", new Date(System.currentTimeMillis() + 86400000L),
                    new Date(System.currentTimeMillis() + 604800000L), "standard:1", manager, "workflow-self-test");
            workflow.respondInvitation(assignment.getId(), true, null, reviewer, "workflow-self-test");
            workflow.submitReview(assignment.getId(), "{\"quality\":5,\"comment\":\"accept\"}",
                    "ACCEPT", reviewer, "workflow-self-test");
            workflow.transition(article.getId(), null, "ACCEPTED", "FINAL_DECISION", null, manager, "workflow-self-test");
            workflow.transition(article.getId(), null, "COPYEDITING", "START_COPYEDIT", null, manager, "workflow-self-test");
            workflow.transition(article.getId(), null, "PRODUCTION", "START_PRODUCTION", null, manager, "workflow-self-test");
            workflow.transition(article.getId(), null, "PROOF", "START_PROOF", null, manager, "workflow-self-test");
            workflow.transition(article.getId(), null, "PUBLICATION_READY", "APPROVE_PROOF", null, manager, "workflow-self-test");
            new JurnalIdentifierService().assignDoi(article.getId(), "10.9999/ais.self-test", manager);
            new JurnalIdentifierService().assignUrn(article.getId(), "urn:ais:self-test", manager);
            JurnalPublicationService publication = new JurnalPublicationService();
            RepoItem issue = publication.createIssue(journal.getRepoCollectionId(), "Vol 1 No 1", 1, "1", 2026, null, manager);
            publication.placeArticle(issue.getId(), article.getId(), 0, manager);
            publication.publishIssue(issue.getId(), new Date(), manager);
            session.flush();
            Number projection = (Number) session.createQuery(
                    "select count(*) from Artikel where repoItemId=:i").setLong("i", article.getId()).uniqueResult();
            Number events = (Number) session.createQuery(
                    "select count(*) from RepoWorkflowEvent where itemId=:i").setLong("i", article.getId()).uniqueResult();
            if (projection.longValue() != 1L || events.longValue() < 9L
                    || !"PUBLISHED".equals(article.getWorkflowStatus()) || !"PUBLISHED".equals(issue.getWorkflowStatus()))
                throw new IllegalStateException("Journey publikasi atau projection tidak konsisten.");
            publication.projectPublishedArticle(article.getId(), manager);
            session.flush();
            Number projectionAgain = (Number) session.createQuery(
                    "select count(*) from Artikel where repoItemId=:i").setLong("i", article.getId()).uniqueResult();
            if (projectionAgain.longValue() != 1L)
                throw new IllegalStateException("Projection Artikel tidak idempoten.");
            JurnalPublicService publicService=new JurnalPublicService();String[] formats={"bibtex","ris","apa","vancouver","ieee","csljson"};for(String format:formats){String citation=publicService.citation(article.getId(),format);if(citation==null||citation.indexOf("Integrated workflow article")<0)throw new IllegalStateException("Format sitasi gagal: "+format);}if(publicService.subjects(journal.getRepoCollectionId(),20).size()!=2||publicService.browseSubject("Open Science",journal.getRepoCollectionId(),0,20).size()!=1)throw new IllegalStateException("Browse subject gagal.");JurnalPublicService.StaticPage page=publicService.staticPage("journal-workflow-self-test","about");if(page==null||!"About Self Test".equals(page.title)||publicService.staticPage("journal-workflow-self-test","../secret")!=null)throw new IllegalStateException("Static page contract tidak aman.");
            RepoItem announcement=new JurnalAnnouncementService().publish(journal.getRepoCollectionId(),"Announcement Self Test","Announcement body <safe>",manager);if(publicService.announcement(announcement.getId())==null||publicService.announcements(journal.getRepoCollectionId(),10).size()!=1)throw new IllegalStateException("Announcement feed source gagal.");
            JurnalReportService reports=new JurnalReportService();for(String type:new String[]{"ARTICLES","REVIEWS","SUBSCRIPTIONS"}){StringWriter csv=new StringWriter();reports.exportCsv(journal.getId(),type,null,null,csv,manager);if(csv.toString().indexOf('\n')<0||("ARTICLES".equals(type)&&csv.toString().indexOf("Integrated workflow article")<0))throw new IllegalStateException("Export laporan gagal: "+type);}
            JurnalPluginParityService plugins=new JurnalPluginParityService();for(String format:new String[]{"native","doaj","pubmed","jats","crossref","datacite"}){String xml=("crossref".equals(format)||"doaj".equals(format))?plugins.exportArticle(article.getId(),format,"https://journal.example.test/ais"):plugins.exportArticle(article.getId(),format);javax.xml.parsers.DocumentBuilderFactory factory=javax.xml.parsers.DocumentBuilderFactory.newInstance();factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",!"pubmed".equals(format));if("pubmed".equals(format)){factory.setFeature("http://xml.org/sax/features/external-general-entities",false);factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);}factory.newDocumentBuilder().parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));if(xml.indexOf("Integrated workflow article")<0)throw new IllegalStateException("Export plugin gagal: "+format);if("crossref".equals(format)){if(xml.indexOf("<resource>https://journal.example.test/ais/jurnal/article/")<0)throw new IllegalStateException("Resource Crossref tidak absolut.");validateOfficial(xml,System.getenv("AIS_JURNAL_CROSSREF_XSD"));}if("datacite".equals(format))validateOfficial(xml,System.getenv("AIS_JURNAL_DATACITE_XSD"));if("doaj".equals(format))validateOfficial(xml,System.getenv("AIS_JURNAL_DOAJ_XSD"));if("pubmed".equals(format))validateOfficialDtd(xml,System.getenv("AIS_JURNAL_PUBMED_DTD"));}boolean unsafeBaseDenied=false;try{plugins.exportArticle(article.getId(),"crossref","javascript://host/path");}catch(IllegalArgumentException expected){unsafeBaseDenied=true;}if(!unsafeBaseDenied)throw new IllegalStateException("Base URL Crossref tidak fail-closed.");if(plugins.integrityFacts(article.getId())==null||plugins.recommendations(article.getId(),"similarity",20)==null)throw new IllegalStateException("Facts/recommendation plugin gagal.");
            JurnalNativeImportService nativeImport=new JurnalNativeImportService();String nativeXml=plugins.exportArticle(article.getId(),"native");RepoItem imported=nativeImport.importDraft(journal.getRepoCollectionId(),nativeXml,"native-roundtrip-self-test",manager);RepoItem importedAgain=nativeImport.importDraft(journal.getRepoCollectionId(),nativeXml,"native-roundtrip-self-test",manager);if(!imported.getId().equals(importedAgain.getId())||!"DRAFT".equals(imported.getWorkflowStatus())||!"AIS_NATIVE_IMPORT".equals(imported.getSourceClass()))throw new IllegalStateException("Native import tidak aman/idempoten.");boolean xxeDenied=false;try{nativeImport.importDraft(journal.getRepoCollectionId(),"<!DOCTYPE x [<!ENTITY e SYSTEM 'file:///etc/passwd'>]><ais-journal-article schemaVersion=\"1\"><title>&e;</title></ais-journal-article>","native-xxe-self-test",manager);}catch(IllegalArgumentException expected){xxeDenied=true;}if(!xxeDenied)throw new IllegalStateException("Native import XXE seharusnya ditolak.");
            System.out.println("JurnalWorkflowPublicationSelfTest OK submit-review-production-publish projection-idempotent browse-subject citations=6 exports=6 configured-official-xsd native-import-idempotent-xxe-safe static-page announcement-feed facts recommendations reports=3 rollback");
        } finally {
            if (tx.isActive()) tx.rollback();
            HibernateUtil.closeSession();
            Tbmuser.getUserRoleYgDipakai.remove(manager.getUserId());
            Tbmuser.getUserRoleYgDipakai.remove(reviewer.getUserId());
        }
        System.exit(0);
    }

    private static void validateOfficial(String xml,String schemaUrl)throws Exception{if(schemaUrl==null||schemaUrl.trim().length()==0)return;if(!schemaUrl.matches("https://[^\\s]+"))throw new IllegalStateException("URL schema resmi HTTPS wajib diisi.");javax.xml.validation.SchemaFactory sf=javax.xml.validation.SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);sf.setProperty(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD,"");sf.setProperty(javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA,"https");sf.setResourceResolver(new org.w3c.dom.ls.LSResourceResolver(){public org.w3c.dom.ls.LSInput resolveResource(String type,String namespaceURI,String publicId,String systemId,String baseURI){if(systemId==null)return null;try{String secure=null;if(systemId.startsWith("http://www.w3.org/Math/XMLSchema/mathml3/"))secure="https://www.w3.org/Math/XMLSchema/mathml3/"+systemId.substring(systemId.lastIndexOf('/')+1);else if(systemId.startsWith("http://www.doaj.org/static/doaj/"))secure="https://doaj.org/static/doaj/"+systemId.substring(systemId.lastIndexOf('/')+1);if(secure==null)return null;java.net.URLConnection connection=new java.net.URL(secure).openConnection();connection.setConnectTimeout(30000);connection.setReadTimeout(30000);org.w3c.dom.bootstrap.DOMImplementationRegistry registry=org.w3c.dom.bootstrap.DOMImplementationRegistry.newInstance();org.w3c.dom.ls.DOMImplementationLS impl=(org.w3c.dom.ls.DOMImplementationLS)registry.getDOMImplementation("LS");org.w3c.dom.ls.LSInput input=impl.createLSInput();input.setSystemId(secure);input.setPublicId(publicId);input.setByteStream(connection.getInputStream());return input;}catch(Exception e){throw new IllegalStateException("Dependency schema HTTPS gagal: "+systemId,e);}}});javax.xml.validation.Validator validator=sf.newSchema(new java.net.URL(schemaUrl)).newValidator();validator.setProperty(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD,"");validator.setProperty(javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA,"https");validator.validate(new javax.xml.transform.stream.StreamSource(new java.io.StringReader(xml)));}

    private static void validateOfficialDtd(String xml,String dtd)throws Exception{if(dtd==null||dtd.trim().length()==0)return;if(!"https://dtd.nlm.nih.gov/ncbi/pubmed/in/PubMed.dtd".equals(dtd.trim()))throw new IllegalArgumentException("DTD PubMed resmi tidak dikenal.");javax.xml.parsers.DocumentBuilderFactory f=javax.xml.parsers.DocumentBuilderFactory.newInstance();f.setValidating(true);f.setNamespaceAware(true);f.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD,"https");javax.xml.parsers.DocumentBuilder b=f.newDocumentBuilder();b.setErrorHandler(new org.xml.sax.ErrorHandler(){public void warning(org.xml.sax.SAXParseException e)throws org.xml.sax.SAXException{throw e;}public void error(org.xml.sax.SAXParseException e)throws org.xml.sax.SAXException{throw e;}public void fatalError(org.xml.sax.SAXParseException e)throws org.xml.sax.SAXException{throw e;}});b.parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));}

    private static Tbmuser user(String id, boolean administrator, boolean all) throws Exception {
        Tbmrole role = new Tbmrole(); role.setRoleId(administrator ? Tbmrole.ADMINISTRATOR : id + "_ROLE");
        JSONObject json = JurnalAksesKatalog.modelUntukEditor(null);
        HashSet<Menu> menus = new HashSet<Menu>();
        for (JurnalAksesKatalog.Entri entry : JurnalAksesKatalog.DAFTAR) {
            boolean grant = all || "review-assignments".equals(entry.kunci);
            json.getJSONObject("menu").put(entry.kunci, grant);
            for (String action : JurnalAksesKatalog.AKSI_CRUD)
                json.getJSONObject("crud").getJSONObject(entry.kunci).put(action, grant);
            Menu menu = new Menu(); menu.setId(Long.valueOf(2000000000L + entry.child)); menus.add(menu);
        }
        for (String action : JurnalAksesKatalog.AKSI_WORKFLOW)
            json.getJSONObject("workflow").put(action, all);
        role.setJurnalAksesJson(json.toString()); role.setMenus(menus);
        Tbmuser user = new Tbmuser(); user.setUserId(id); user.setUserRole(role);
        Tbmuser.getUserRoleYgDipakai.put(id, role); return user;
    }
}
