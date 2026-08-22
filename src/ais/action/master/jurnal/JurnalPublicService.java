package ais.action.master.jurnal;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.repository.RepoCollection;
import ais.database.model.repository.RepoItem;

/** Query publik jurnal; hanya DTO allowlist dan publikasi berstatus PUBLISHED. */
public final class JurnalPublicService {
    public static final class JournalCard {public Long id;public String name,description,slug;}
    public static final class ArticleCard {public Long id,collectionId;public String title,authors,abstractText,doi,language,slug;public Date publishedAt;}
    public static final class IssueCard {public Long id,collectionId;public String title,slug;public Date publishedAt;public final List<ArticleCard> articles=new ArrayList<ArticleCard>();}
    public static final class Home {public final List<JournalCard> journals=new ArrayList<JournalCard>();public final List<ArticleCard> latest=new ArrayList<ArticleCard>();}

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
    public ArticleCard article(Long id){if(id==null)return null;Session s=HibernateUtil.currentSession();Query q=s.createQuery("select i from RepoItem i,RepoCollection c where i.id=:id and i.collectionId=c.id and i.aktif=true and c.aktif=true and upper(c.tipe)='JOURNAL' and i.workflowStatus='PUBLISHED' and i.isWithdrawn=false");q.setLong("id",id.longValue());RepoItem item=(RepoItem)q.uniqueResult();return item==null?null:card(item);}
    public JournalCard journal(String slug){if(slug==null)return null;Session s=HibernateUtil.currentSession();Query q=s.createQuery("from RepoCollection where aktif=true and upper(tipe)='JOURNAL' and lower(kode)=:s");q.setString("s",slug.trim().toLowerCase());q.setMaxResults(1);RepoCollection r=(RepoCollection)q.uniqueResult();if(r==null)return null;JournalCard d=new JournalCard();d.id=r.getId();d.name=r.getNama();d.description=r.getDeskripsi();d.slug=r.getKode();return d;}
    @SuppressWarnings("unchecked") public List<IssueCard> issues(Long collectionId,int page,int size){Session s=HibernateUtil.currentSession();Query q=s.createQuery("from RepoItem where collectionId=:c and documentType='JOURNAL_ISSUE' and workflowStatus='PUBLISHED' and aktif=true and isWithdrawn=false order by publishedAt desc,id desc");q.setLong("c",collectionId);q.setFirstResult(Math.max(0,page)*Math.max(1,size));q.setMaxResults(Math.max(1,Math.min(100,size)));List<RepoItem> rows=q.list();List<IssueCard> out=new ArrayList<IssueCard>();for(RepoItem r:rows)out.add(issueCard(r,false));return out;}
    public IssueCard issue(Long id){if(id==null)return null;Session s=HibernateUtil.currentSession();Query q=s.createQuery("from RepoItem where id=:id and documentType='JOURNAL_ISSUE' and workflowStatus='PUBLISHED' and aktif=true and isWithdrawn=false");q.setLong("id",id);RepoItem r=(RepoItem)q.uniqueResult();return r==null?null:issueCard(r,true);}
    @SuppressWarnings("unchecked") public List<ArticleCard> search(String term,Long collectionId,int page,int size){String x=term==null?"":term.trim().toLowerCase();if(x.length()>200)throw new IllegalArgumentException("Kata pencarian terlalu panjang.");Session s=HibernateUtil.currentSession();String hql="from RepoItem where documentType='JOURNAL_SUBMISSION' and workflowStatus='PUBLISHED' and aktif=true and isWithdrawn=false "+(collectionId==null?"":"and collectionId=:c ")+"and (lower(title) like :q or lower(authors) like :q or lower(subjects) like :q) order by publishedAt desc,id desc";Query q=s.createQuery(hql);if(collectionId!=null)q.setLong("c",collectionId);q.setString("q","%"+x.replace("%","").replace("_","")+"%");q.setFirstResult(Math.max(0,page)*Math.max(1,size));q.setMaxResults(Math.max(1,Math.min(100,size)));List<RepoItem> rows=q.list();List<ArticleCard> out=new ArrayList<ArticleCard>();for(RepoItem r:rows)out.add(card(r));return out;}
    @SuppressWarnings("unchecked") private IssueCard issueCard(RepoItem r,boolean include){IssueCard d=new IssueCard();d.id=r.getId();d.collectionId=r.getCollectionId();d.title=r.getTitle();d.slug=r.getSlug();d.publishedAt=r.getPublishedAt();if(include){Session s=HibernateUtil.currentSession();List<RepoItem> articles=s.createQuery("select a from RepoItem a,RepoItemRelation x where x.itemId=:i and x.relatedItemId=a.id and x.relationType='ISSUE_CONTAINS' and x.aktif=true and a.aktif=true and a.workflowStatus='PUBLISHED' and a.isWithdrawn=false order by x.sortOrder,a.id").setLong("i",r.getId()).list();for(RepoItem a:articles)d.articles.add(card(a));}return d;}
    public String citation(Long id,String format){ArticleCard a=article(id);if(a==null)return null;String f=format==null?"bibtex":format.toLowerCase();if("ris".equals(f))return "TY  - JOUR\nTI  - "+line(a.title)+"\nAU  - "+line(a.authors)+"\nPY  - "+(a.publishedAt==null?"":String.format("%tY",a.publishedAt))+"\nDO  - "+line(a.doi)+"\nER  - \n";String key="ais"+a.id;return "@article{"+key+",\n  title={"+braces(a.title)+"},\n  author={"+braces(a.authors)+"},\n  year={"+(a.publishedAt==null?"":String.format("%tY",a.publishedAt))+"},\n  doi={"+braces(a.doi)+"}\n}\n";}
    private static String line(String v){return v==null?"":v.replace("\r"," ").replace("\n"," ");}private static String braces(String v){return line(v).replace("{","\\{").replace("}","\\}");}
    private ArticleCard card(RepoItem r){ArticleCard d=new ArticleCard();d.id=r.getId();d.collectionId=r.getCollectionId();d.title=r.getTitle();d.authors=r.getAuthors();d.abstractText=r.getAbstractText();d.doi=r.getDoi();d.language=r.getLanguage();d.slug=r.getSlug();d.publishedAt=r.getPublishedAt();return d;}
}
