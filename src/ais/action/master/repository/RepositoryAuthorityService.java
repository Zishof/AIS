package ais.action.master.repository;

import java.text.Normalizer;
import java.util.Date;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import ais.database.model.repository.RepoAuthorAuthority;
import ais.database.model.repository.RepoItem;
import ais.database.model.repository.RepoItemContributor;
import ais.database.model.repository.RepoItemMetadata;

/** Membentuk authority penulis secara idempoten dari metadata item. */
public final class RepositoryAuthorityService {
    private RepositoryAuthorityService(){}

    @SuppressWarnings("unchecked")
    public static void synchronizeItem(Session session, RepoItem item) {
        if (session == null || item == null || item.getId() == null) return;
        List<RepoItemContributor> old = session.createCriteria(RepoItemContributor.class)
                .add(Restrictions.eq("itemId", item.getId())).list();
        for (RepoItemContributor row : old) { session.setReadOnly(row, false); row.setAktif(Boolean.FALSE); }
        String[] authors = split(item.getAuthors());
        List<RepoItemMetadata> metadata = session.createCriteria(RepoItemMetadata.class)
                .add(Restrictions.eq("itemId", item.getId())).add(Restrictions.or(Restrictions.isNull("aktif"),Restrictions.eq("aktif",Boolean.TRUE)))
                .addOrder(Order.asc("place")).addOrder(Order.asc("id")).list();
        for (int i=0;i<authors.length;i++) {
            String name=clean(authors[i]); if(name.length()==0)continue;
            String orcid=metadataAt(metadata,"repository.author.orcid",i);
            String affiliation=metadataAt(metadata,"repository.author.affiliation",i);
            String ror=metadataAt(metadata,"repository.author.ror",i);
            RepoAuthorAuthority authority=findOrCreate(session,name,orcid,affiliation,ror);
            RepoItemContributor link=findLink(old,authority.getId());
            if(link==null){link=new RepoItemContributor();link.setItemId(item.getId());link.setAuthorityId(authority.getId());link.setContributorRole("AUTHOR");link.setCreatedAt(new Date());}
            link.setDisplayName(name);link.setSequenceNumber(Integer.valueOf(i));link.setCorresponding(Boolean.valueOf(i==0));link.setAktif(Boolean.TRUE);session.saveOrUpdate(link);
        }
    }

    private static RepoAuthorAuthority findOrCreate(Session session,String name,String orcid,String affiliation,String ror){
        RepoAuthorAuthority row=null;
        if(clean(orcid).length()>0)row=(RepoAuthorAuthority)session.createCriteria(RepoAuthorAuthority.class)
                .add(Restrictions.eq("tenantKey",RepositoryTenantScope.currentKey())).add(Restrictions.eq("orcid",clean(orcid))).setMaxResults(1).uniqueResult();
        String normalized=normalizeName(name);
        if(row==null)row=(RepoAuthorAuthority)session.createCriteria(RepoAuthorAuthority.class)
                .add(Restrictions.eq("tenantKey",RepositoryTenantScope.currentKey())).add(Restrictions.eq("normalizedName",normalized)).setMaxResults(1).uniqueResult();
        Date now=new Date();
        if(row==null){row=new RepoAuthorAuthority();row.setTenantKey(RepositoryTenantScope.currentKey());row.setCanonicalName(canonicalName(name));row.setNormalizedName(normalized);row.setCreatedAt(now);row.setVerified(Boolean.FALSE);row.setAktif(Boolean.TRUE);}
        else session.setReadOnly(row,false);
        row.setNameVariants(appendVariant(row.getNameVariants(),name));
        if(clean(orcid).length()>0)row.setOrcid(clean(orcid)); if(clean(affiliation).length()>0)row.setAffiliation(clean(affiliation)); if(clean(ror).length()>0)row.setRorId(clean(ror));
        row.setUpdatedAt(now);session.saveOrUpdate(row);session.flush();return row;
    }

    private static RepoItemContributor findLink(List<RepoItemContributor> rows,Long authorityId){for(RepoItemContributor row:rows)if(authorityId.equals(row.getAuthorityId())&&"AUTHOR".equals(row.getContributorRole()))return row;return null;}
    private static String metadataAt(List<RepoItemMetadata> rows,String field,int place){for(RepoItemMetadata row:rows)if(field.equals(row.getMetadataField())&&row.getPlace()!=null&&row.getPlace().intValue()==place)return clean(row.getMetadataValue());return "";}
    private static String[] split(String value){return value==null?new String[0]:value.split("[;\\n\\r]+");}
    private static String canonicalName(String value){String name=clean(value);int comma=name.indexOf(',');return comma>0?clean(name.substring(comma+1))+" "+clean(name.substring(0,comma)):name;}
    public static String normalizeName(String value){String name=Normalizer.normalize(canonicalName(value).toLowerCase(),Normalizer.Form.NFD).replaceAll("\\p{M}+","").replaceAll("[^a-z0-9]+"," ").trim();return name.replaceAll("\\s+"," ");}
    private static String appendVariant(String existing,String value){String variant=clean(value),all=clean(existing);for(String old:all.split("\\n"))if(old.equalsIgnoreCase(variant))return all;return all.length()==0?variant:all+"\n"+variant;}
    private static String clean(String value){return value==null?"":value.trim().replaceAll("\\s+"," ");}
}
