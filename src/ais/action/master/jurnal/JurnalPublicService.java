package ais.action.master.jurnal;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Comparator;
import org.hibernate.Query;
import org.hibernate.Session;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.repository.RepoCollection;
import ais.database.model.repository.RepoItem;
import ais.database.model.repository.RepoBitstream;
import org.json.JSONObject;

/** Query publik jurnal; hanya DTO allowlist dan publikasi berstatus PUBLISHED. */
public final class JurnalPublicService {
    /**
     * Tipe implementasi bersarang {@link BlockCard} milik {@link JurnalPublicService}. Kelas ini memberi nama pada
     * state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link JurnalPublicService}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String key}, {@code String title},
     * {@code String bodyText}; operasi lokal: {@code getKey()}, {@code getTitle()}, {@code getBodyText}(). Aturan
     * bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
     * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
     * tambahkan perilaku lintas domain pada service bersama.</p>
     *
     * @see JurnalPublicService
     */
    public static final class BlockCard {public String key,title,bodyText;public String getKey(){return key;}public String getTitle(){return title;}public String getBodyText(){return bodyText;}}
    /**
     * DTO allowlist metadata jurnal yang dikembalikan oleh {@link JurnalPublicService}. Field mencakup identitas,
     * deskripsi, slug, informasi pembaca/penulis/pustakawan, pengaturan submission, analytics, dan blok konten
     * publik. Tipe ini tidak memuat entity Hibernate atau aturan publikasi.
     *
     * @see JurnalPublicService
     */
    public static final class JournalCard {public Long id;public String name,description,slug,readerInfo,authorInfo,librarianInfo,subscriptionInfo,analyticsProvider,analyticsMeasurementId;public boolean allowSubmissions;public final List<BlockCard> customBlocks=new ArrayList<BlockCard>();public Long getId(){return id;}public String getName(){return name;}public String getDescription(){return description;}public String getSlug(){return slug;}public String getReaderInfo(){return readerInfo;}public String getAuthorInfo(){return authorInfo;}public String getLibrarianInfo(){return librarianInfo;}public String getSubscriptionInfo(){return subscriptionInfo;}public boolean isAllowSubmissions(){return allowSubmissions;}public List<BlockCard> getCustomBlocks(){return customBlocks;}public String getAnalyticsProvider(){return analyticsProvider;}public String getAnalyticsMeasurementId(){return analyticsMeasurementId;}}
    /**
     * DTO allowlist satu galley/berkas artikel publik. Hanya identitas, nama, MIME type, dan mode viewer yang
     * diekspos; stream dan pemeriksaan akses tetap ditangani endpoint/service terkait.
     *
     * @see JurnalPublicService
     */
    public static final class GalleyCard {public Long id;public String name,mimeType,viewer;public Long getId(){return id;}public String getName(){return name;}public String getMimeType(){return mimeType;}public String getViewer(){return viewer;}}
    /**
     * DTO allowlist artikel berstatus publik beserta metadata bibliografi, nama penulis, dan daftar galley.
     * Pembentukan DTO dilakukan oleh {@link JurnalPublicService}; tipe ini hanya membawa state respons dan tidak
     * boleh mengambil alih query atau keputusan status publikasi.
     *
     * @see JurnalPublicService
     */
    public static final class ArticleCard {public Long id,collectionId;public String title,authors,abstractText,doi,language,slug,journalTitle,publisher,issn;public Date publishedAt;public final List<String> authorNames=new ArrayList<String>();public final List<GalleyCard> galleys=new ArrayList<GalleyCard>();public Long getId(){return id;}public Long getCollectionId(){return collectionId;}public String getTitle(){return title;}public String getAuthors(){return authors;}public List<String> getAuthorNames(){return authorNames;}public String getAbstractText(){return abstractText;}public String getDoi(){return doi;}public String getLanguage(){return language;}public String getSlug(){return slug;}public String getJournalTitle(){return journalTitle;}public String getPublisher(){return publisher;}public String getIssn(){return issn;}public Date getPublishedAt(){return publishedAt;}public List<GalleyCard> getGalleys(){return galleys;}}
    /**
     * Tipe implementasi bersarang {@link IssueCard} milik {@link JurnalPublicService}. Kelas ini memberi nama pada
     * state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link JurnalPublicService}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Long id}, {@code Long collectionId},
     * {@code String title}, {@code String slug}, {@code Date publishedAt}, {@code List articles}; operasi lokal:
     * {@code getId()}, {@code getCollectionId()}, {@code getTitle()}, {@code getSlug()}, {@code getPublishedAt()},
     * {@code getArticles}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang
     * dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
     * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
     * tambahkan perilaku lintas domain pada service bersama.</p>
     *
     * @see JurnalPublicService
     */
    public static final class IssueCard {public Long id,collectionId;public String title,slug;public Date publishedAt;public final List<ArticleCard> articles=new ArrayList<ArticleCard>();public Long getId(){return id;}public Long getCollectionId(){return collectionId;}public String getTitle(){return title;}public String getSlug(){return slug;}public Date getPublishedAt(){return publishedAt;}public List<ArticleCard> getArticles(){return articles;}}
    /**
     * Tipe implementasi bersarang {@link StaticPage} milik {@link JurnalPublicService}. Kelas ini memberi nama
     * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link JurnalPublicService}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Long collectionId}, {@code String
     * journalSlug}, {@code String slug}, {@code String title}, {@code String bodyText}; operasi lokal: {@code
     * getCollectionId()}, {@code getJournalSlug()}, {@code getSlug()}, {@code getTitle()}, {@code getBodyText}().
     * Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
     * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
     * tambahkan perilaku lintas domain pada service bersama.</p>
     *
     * @see JurnalPublicService
     */
    public static final class StaticPage {public Long collectionId;public String journalSlug,slug,title,bodyText;public Long getCollectionId(){return collectionId;}public String getJournalSlug(){return journalSlug;}public String getSlug(){return slug;}public String getTitle(){return title;}public String getBodyText(){return bodyText;}}
    /**
     * Tipe implementasi bersarang {@link SubjectCard} milik {@link JurnalPublicService}. Kelas ini memberi nama
     * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link JurnalPublicService}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String name}, {@code int count};
     * operasi lokal: {@code getName()}, {@code getCount}(). Aturan bisnis bersama tetap berada pada kelas induk
     * atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
     * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
     * tambahkan perilaku lintas domain pada service bersama.</p>
     *
     * @see JurnalPublicService
     */
    public static final class SubjectCard {public String name;public int count;public String getName(){return name;}public int getCount(){return count;}}
    /**
     * Tipe implementasi bersarang {@link Home} milik {@link JurnalPublicService}. Kelas ini memberi nama pada
     * state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link JurnalPublicService}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code List journals}, {@code List latest};
     * operasi lokal: {@code getJournals()}, {@code getLatest}(). Aturan bisnis bersama tetap berada pada kelas
     * induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
     * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
     * tambahkan perilaku lintas domain pada service bersama.</p>
     *
     * @see JurnalPublicService
     */
    public static final class Home {public final List<JournalCard> journals=new ArrayList<JournalCard>();public final List<ArticleCard> latest=new ArrayList<ArticleCard>();public List<JournalCard> getJournals(){return journals;}public List<ArticleCard> getLatest(){return latest;}}

    public Home home(){Home out=new Home();out.journals.addAll(journals());out.latest.addAll(latest(12));return out;}
    @SuppressWarnings("unchecked") public List<JournalCard> journals(){
        Session s=HibernateUtil.currentSession();
        List<RepoCollection> rows=s.createQuery("from RepoCollection where aktif=true and upper(tipe)='JOURNAL' order by sortOrder,nama").setMaxResults(200).list();
        List<JournalCard> out=new ArrayList<JournalCard>();for(RepoCollection r:rows){JournalCard d=new JournalCard();d.id=r.getId();d.name=r.getNama();d.description=r.getDeskripsi();d.slug=r.getKode();out.add(d);}return out;
    }
    @SuppressWarnings("unchecked") public List<ArticleCard> latest(int limit){
        Session s=HibernateUtil.currentSession();
        Query q=s.createQuery("select i from RepoItem i,RepoCollection c where i.collectionId=c.id and i.aktif=true and c.aktif=true and upper(c.tipe)='JOURNAL' and i.documentType='JOURNAL_SUBMISSION' and i.workflowStatus='PUBLISHED' and i.isWithdrawn=false order by i.publishedAt desc,i.id desc");
        q.setMaxResults(Math.max(1,Math.min(100,limit)));List<RepoItem> rows=q.list();List<ArticleCard> out=new ArrayList<ArticleCard>();for(RepoItem r:rows)out.add(card(r));return out;
    }
    @SuppressWarnings("unchecked") public List<ArticleCard> announcements(Long collectionId,int limit){Session s=HibernateUtil.currentSession();String hql="select i from RepoItem i,RepoCollection c where i.collectionId=c.id and i.aktif=true and c.aktif=true and upper(c.tipe)='JOURNAL' and i.documentType='JOURNAL_ANNOUNCEMENT' and i.workflowStatus='PUBLISHED' and i.isWithdrawn=false "+(collectionId==null?"":"and i.collectionId=:c ")+"order by i.publishedAt desc,i.id desc";Query q=s.createQuery(hql);if(collectionId!=null)q.setLong("c",collectionId);q.setMaxResults(Math.max(1,Math.min(100,limit)));List<RepoItem> rows=q.list();List<ArticleCard> out=new ArrayList<ArticleCard>();for(RepoItem r:rows)out.add(card(r));return out;}
    public ArticleCard article(Long id){if(id==null)return null;Session s=HibernateUtil.currentSession();Query q=s.createQuery("select i from RepoItem i,RepoCollection c where i.id=:id and i.collectionId=c.id and i.aktif=true and c.aktif=true and upper(c.tipe)='JOURNAL' and i.workflowStatus='PUBLISHED' and i.isWithdrawn=false");q.setLong("id",id.longValue());RepoItem item=(RepoItem)q.uniqueResult();if(item==null)return null;ArticleCard out=card(item);addGalleys(out);return out;}
    public ArticleCard announcement(Long id){if(id==null)return null;Session s=HibernateUtil.currentSession();Query q=s.createQuery("select i from RepoItem i,RepoCollection c where i.id=:id and i.collectionId=c.id and i.aktif=true and c.aktif=true and upper(c.tipe)='JOURNAL' and i.documentType='JOURNAL_ANNOUNCEMENT' and i.workflowStatus='PUBLISHED' and i.isWithdrawn=false");q.setLong("id",id.longValue());q.setMaxResults(1);RepoItem item=(RepoItem)q.uniqueResult();return item==null?null:card(item);}
    public JournalCard journal(String slug){if(slug==null)return null;Session s=HibernateUtil.currentSession();Query q=s.createQuery("from RepoCollection where aktif=true and upper(tipe)='JOURNAL' and lower(kode)=:s");q.setString("s",slug.trim().toLowerCase());q.setMaxResults(1);RepoCollection r=(RepoCollection)q.uniqueResult();if(r==null)return null;JournalCard d=new JournalCard();d.id=r.getId();d.name=r.getNama();d.description=r.getDeskripsi();d.slug=r.getKode();applyPublicUi(d,r.getMetadataProfileJson());return d;}
    public StaticPage staticPage(String journalSlug,String pageSlug){if(!slug(journalSlug)||!slug(pageSlug))return null;Session s=HibernateUtil.currentSession();Query q=s.createQuery("from RepoCollection where aktif=true and upper(tipe)='JOURNAL' and lower(kode)=:s");q.setString("s",journalSlug.toLowerCase());q.setMaxResults(1);RepoCollection c=(RepoCollection)q.uniqueResult();if(c==null)return null;try{org.json.JSONArray pages=new JSONObject(c.getMetadataProfileJson()).optJSONArray("publicPages");if(pages==null)return null;for(int i=0;i<Math.min(100,pages.length());i++){JSONObject p=pages.getJSONObject(i);if(pageSlug.equalsIgnoreCase(p.optString("slug"))&&p.optBoolean("active",true)){String title=p.optString("title").trim(),body=p.optString("bodyText").trim();if(title.length()==0||title.length()>500||body.length()>262144)throw new IllegalStateException("Kontrak halaman jurnal rusak.");StaticPage out=new StaticPage();out.collectionId=c.getId();out.journalSlug=c.getKode();out.slug=pageSlug;out.title=title;out.bodyText=body;return out;}}return null;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException("Kontrak halaman jurnal rusak.",e);}}
    @SuppressWarnings("unchecked") public List<IssueCard> issues(Long collectionId,int page,int size){Session s=HibernateUtil.currentSession();Query q=s.createQuery("from RepoItem where collectionId=:c and documentType='JOURNAL_ISSUE' and workflowStatus='PUBLISHED' and aktif=true and isWithdrawn=false order by publishedAt desc,id desc");q.setLong("c",collectionId);q.setFirstResult(Math.max(0,page)*Math.max(1,size));q.setMaxResults(Math.max(1,Math.min(100,size)));List<RepoItem> rows=q.list();List<IssueCard> out=new ArrayList<IssueCard>();for(RepoItem r:rows)out.add(issueCard(r,false));return out;}
    public IssueCard issue(Long id){if(id==null)return null;Session s=HibernateUtil.currentSession();Query q=s.createQuery("from RepoItem where id=:id and documentType='JOURNAL_ISSUE' and workflowStatus='PUBLISHED' and aktif=true and isWithdrawn=false");q.setLong("id",id);RepoItem r=(RepoItem)q.uniqueResult();return r==null?null:issueCard(r,true);}
    @SuppressWarnings("unchecked") public List<ArticleCard> search(String term,Long collectionId,int page,int size){String x=term==null?"":term.trim().toLowerCase();if(x.length()>200)throw new IllegalArgumentException("Kata pencarian terlalu panjang.");Session s=HibernateUtil.currentSession();String hql="from RepoItem where documentType='JOURNAL_SUBMISSION' and workflowStatus='PUBLISHED' and aktif=true and isWithdrawn=false "+(collectionId==null?"":"and collectionId=:c ")+"and (lower(title) like :q or lower(authors) like :q or lower(subjects) like :q) order by publishedAt desc,id desc";Query q=s.createQuery(hql);if(collectionId!=null)q.setLong("c",collectionId);q.setString("q","%"+x.replace("%","").replace("_","")+"%");q.setFirstResult(Math.max(0,page)*Math.max(1,size));q.setMaxResults(Math.max(1,Math.min(100,size)));List<RepoItem> rows=q.list();List<ArticleCard> out=new ArrayList<ArticleCard>();for(RepoItem r:rows)out.add(card(r));return out;}
    @SuppressWarnings("unchecked") public List<SubjectCard> subjects(Long collectionId,int limit){Session s=HibernateUtil.currentSession();String hql="select subjects from RepoItem where documentType='JOURNAL_SUBMISSION' and workflowStatus='PUBLISHED' and aktif=true and isWithdrawn=false and subjects is not null "+(collectionId==null?"":"and collectionId=:c ")+"order by id desc";Query q=s.createQuery(hql);if(collectionId!=null)q.setLong("c",collectionId);q.setMaxResults(5000);Map<String,SubjectCard> found=new LinkedHashMap<String,SubjectCard>();for(String raw:(List<String>)q.list())if(raw!=null)for(String part:raw.split("[,;|]")){String name=part.trim().replaceAll("\\s+"," ");if(name.length()==0||name.length()>120)continue;String key=name.toLowerCase();SubjectCard row=found.get(key);if(row==null){row=new SubjectCard();row.name=name;found.put(key,row);}row.count++;}List<SubjectCard> out=new ArrayList<SubjectCard>(found.values());Collections.sort(out,new Comparator<SubjectCard>(){public int compare(SubjectCard a,SubjectCard b){int c=b.count-a.count;return c!=0?c:a.name.compareToIgnoreCase(b.name);}});return out.size()>Math.max(1,Math.min(200,limit))?new ArrayList<SubjectCard>(out.subList(0,Math.max(1,Math.min(200,limit)))):out;}
    @SuppressWarnings("unchecked") public List<ArticleCard> browseSubject(String subject,Long collectionId,int page,int size){String x=subject==null?"":subject.trim().toLowerCase();if(x.length()<1||x.length()>120)throw new IllegalArgumentException("Subjek tidak valid.");Session s=HibernateUtil.currentSession();String hql="from RepoItem where documentType='JOURNAL_SUBMISSION' and workflowStatus='PUBLISHED' and aktif=true and isWithdrawn=false "+(collectionId==null?"":"and collectionId=:c ")+"and lower(subjects) like :q order by publishedAt desc,id desc";Query q=s.createQuery(hql);if(collectionId!=null)q.setLong("c",collectionId);q.setString("q","%"+x.replace("%","").replace("_","")+"%");q.setFirstResult(Math.max(0,page)*Math.max(1,size));q.setMaxResults(Math.max(1,Math.min(100,size)));List<ArticleCard>out=new ArrayList<ArticleCard>();for(RepoItem r:(List<RepoItem>)q.list())out.add(card(r));return out;}
    @SuppressWarnings("unchecked") private IssueCard issueCard(RepoItem r,boolean include){IssueCard d=new IssueCard();d.id=r.getId();d.collectionId=r.getCollectionId();d.title=r.getTitle();d.slug=r.getSlug();d.publishedAt=r.getPublishedAt();if(include){Session s=HibernateUtil.currentSession();List<RepoItem> articles=s.createQuery("select a from RepoItem a,RepoItemRelation x where x.itemId=:i and x.relatedItemId=a.id and x.relationType='ISSUE_CONTAINS' and x.aktif=true and a.aktif=true and a.workflowStatus='PUBLISHED' and a.isWithdrawn=false order by x.sortOrder,a.id").setLong("i",r.getId()).list();for(RepoItem a:articles)d.articles.add(card(a));}return d;}
    public String citation(Long id,String format){ArticleCard a=article(id);if(a==null)return null;String f=format==null?"bibtex":format.toLowerCase();String year=a.publishedAt==null?"n.d.":String.format("%tY",a.publishedAt),authors=line(a.authors),title=line(a.title),doi=line(a.doi);if("ris".equals(f))return "TY  - JOUR\nTI  - "+title+"\nAU  - "+authors+"\nPY  - "+year+"\nDO  - "+doi+"\nER  - \n";if("apa".equals(f))return authors+" ("+year+"). "+title+"."+(doi.length()==0?"":" https://doi.org/"+doi)+"\n";if("vancouver".equals(f))return authors+". "+title+". "+year+"."+(doi.length()==0?"":" doi:"+doi)+"\n";if("ieee".equals(f))return authors+", \""+title+",\" "+year+(doi.length()==0?".":", doi: "+doi+".")+"\n";if("csljson".equals(f)||"json".equals(f))try{return new JSONObject().put("id","ais"+a.id).put("type","article-journal").put("title",title).put("authorLiteral",authors).put("issued",year).put("DOI",doi).toString(2)+"\n";}catch(Exception e){throw new IllegalStateException(e);}String key="ais"+a.id;return "@article{"+key+",\n  title={"+braces(a.title)+"},\n  author={"+braces(a.authors)+"},\n  year={"+year+"},\n  doi={"+braces(a.doi)+"}\n}\n";}
    private static String line(String v){return v==null?"":v.replace("\r"," ").replace("\n"," ");}private static String braces(String v){return line(v).replace("{","\\{").replace("}","\\}");}private static boolean slug(String v){return v!=null&&v.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,79}");}
    @SuppressWarnings("unchecked") private void addGalleys(ArticleCard article){List<RepoBitstream> rows=HibernateUtil.currentSession().createQuery("from RepoBitstream where itemId=:i and aktif=true and storageState='LINKED' and journalStage in ('PRODUCTION','PUBLICATION') order by primaryFile desc,id").setLong("i",article.id).setMaxResults(50).list();for(RepoBitstream r:rows){GalleyCard g=new GalleyCard();g.id=r.getId();g.name=r.getNamaFile();g.mimeType=r.getMimeType();String mime=g.mimeType==null?"":g.mimeType.toLowerCase();g.viewer="application/pdf".equals(mime)?"pdf":(("text/html".equals(mime)||"application/xhtml+xml".equals(mime))?"html":((mime.indexOf("xml")>=0)?"jats":"file"));article.galleys.add(g);}}
    private static void applyPublicUi(JournalCard d,String raw){try{JSONObject root=new JSONObject(JurnalProfileValidator.metadata(raw));JSONObject ui=root.optJSONObject("publicUi");if(ui==null)return;d.allowSubmissions=ui.optBoolean("allowSubmissions",false);JSONObject info=ui.optJSONObject("information");if(info!=null){d.readerInfo=info.optString("reader","");d.authorInfo=info.optString("author","");d.librarianInfo=info.optString("librarian","");}d.subscriptionInfo=ui.optString("subscriptionInfo","");JSONObject analytics=ui.optJSONObject("analytics");if(analytics!=null){d.analyticsProvider=analytics.optString("provider","DISABLED");d.analyticsMeasurementId=analytics.optString("measurementId","");}org.json.JSONArray blocks=ui.optJSONArray("customBlocks");if(blocks!=null)for(int i=0;i<blocks.length();i++){JSONObject b=blocks.getJSONObject(i);if(!b.optBoolean("active",true))continue;BlockCard x=new BlockCard();x.key=b.getString("key");x.title=b.getString("title");x.bodyText=b.getString("bodyText");d.customBlocks.add(x);}}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException("Kontrak public UI jurnal rusak.",e);}}
    @SuppressWarnings("unchecked") private ArticleCard card(RepoItem r){ArticleCard d=new ArticleCard();d.id=r.getId();d.collectionId=r.getCollectionId();d.title=r.getTitle();d.authors=r.getAuthors();d.abstractText=r.getAbstractText();d.doi=r.getDoi();d.language=r.getLanguage();d.slug=r.getSlug();d.publishedAt=r.getPublishedAt();Session s=HibernateUtil.currentSession();List<String>names=s.createQuery("select displayName from RepoItemContributor where itemId=:i and contributorRole='AUTHOR' and aktif=true order by sequenceNumber,id").setLong("i",r.getId()).setMaxResults(200).list();for(String name:names)if(name!=null&&name.trim().length()>0)d.authorNames.add(name.trim());if((d.authors==null||d.authors.trim().length()==0)&&!d.authorNames.isEmpty()){StringBuilder joined=new StringBuilder();for(String name:d.authorNames){if(joined.length()>0)joined.append("; ");joined.append(name);}d.authors=joined.toString();}RepoCollection c=(RepoCollection)s.get(RepoCollection.class,r.getCollectionId());if(c!=null){d.journalTitle=c.getNama();d.publisher="eCampus AIS";try{JSONObject publication=new JSONObject(JurnalProfileValidator.metadata(c.getMetadataProfileJson())).optJSONObject("publication");if(publication!=null){String journalTitle=publication.optString("journalTitle","").trim(),publisher=publication.optString("publisher","").trim();if(journalTitle.length()>0)d.journalTitle=journalTitle;if(publisher.length()>0)d.publisher=publisher;d.issn=publication.optString("issn","").trim().toUpperCase();}}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException("Kontrak publikasi jurnal rusak.",e);}}return d;}
}
