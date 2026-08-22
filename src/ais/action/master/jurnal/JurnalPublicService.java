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
    public static final class Home {public final List<JournalCard> journals=new ArrayList<JournalCard>();public final List<ArticleCard> latest=new ArrayList<ArticleCard>();}

    public Home home(){Home out=new Home();out.journals.addAll(journals());out.latest.addAll(latest(12));return out;}
    @SuppressWarnings("unchecked") public List<JournalCard> journals(){
        Session s=HibernateUtil.currentSession();
        List<RepoCollection> rows=s.createQuery("from RepoCollection where aktif=true and upper(tipe)='JOURNAL' order by sortOrder,nama").setMaxResults(200).list();
        List<JournalCard> out=new ArrayList<JournalCard>();for(RepoCollection r:rows){JournalCard d=new JournalCard();d.id=r.getId();d.name=r.getNama();d.description=r.getDeskripsi();d.slug=r.getKode();out.add(d);}return out;
    }
    @SuppressWarnings("unchecked") public List<ArticleCard> latest(int limit){
        Session s=HibernateUtil.currentSession();
        Query q=s.createQuery("select i from RepoItem i,RepoCollection c where i.collectionId=c.id and i.aktif=true and c.aktif=true and upper(c.tipe)='JOURNAL' and i.workflowStatus='PUBLISHED' and i.isWithdrawn=false order by i.publishedAt desc,i.id desc");
        q.setMaxResults(Math.max(1,Math.min(100,limit)));List<RepoItem> rows=q.list();List<ArticleCard> out=new ArrayList<ArticleCard>();for(RepoItem r:rows)out.add(card(r));return out;
    }
    public ArticleCard article(Long id){if(id==null)return null;Session s=HibernateUtil.currentSession();Query q=s.createQuery("select i from RepoItem i,RepoCollection c where i.id=:id and i.collectionId=c.id and i.aktif=true and c.aktif=true and upper(c.tipe)='JOURNAL' and i.workflowStatus='PUBLISHED' and i.isWithdrawn=false");q.setLong("id",id.longValue());RepoItem item=(RepoItem)q.uniqueResult();return item==null?null:card(item);}
    private ArticleCard card(RepoItem r){ArticleCard d=new ArticleCard();d.id=r.getId();d.collectionId=r.getCollectionId();d.title=r.getTitle();d.authors=r.getAuthors();d.abstractText=r.getAbstractText();d.doi=r.getDoi();d.language=r.getLanguage();d.slug=r.getSlug();d.publishedAt=r.getPublishedAt();return d;}
}
