package ais.action.master.repository;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.repository.RepoItem;
import ais.database.model.repository.RepoNotification;
import ais.database.model.repository.RepoUserPreference;

/** Memproses SEARCH_ALERT menjadi notifikasi in-app yang tenant-safe dan idempoten. */
public class RepositoryAlertService {
    private static final String[] PUBLIC_STATUSES={"SYNCED","PUBLISHED","APPROVED"};

    public static class Summary {
        public int scanned,matched,created,failed;
        public String toString(){return "dipindai="+scanned+", cocok="+matched+", notifikasi="+created+", gagal="+failed;}
    }

    @SuppressWarnings("unchecked")
    public Summary process(int maximumAlerts,int maximumItemsPerAlert){
        Summary summary=new Summary();Session listing=null;List<Long> ids=new ArrayList<Long>();
        try{
            listing=HibernateUtil.openSession();
            ids=listing.createCriteria(RepoUserPreference.class)
                    .add(Restrictions.eq("preferenceType","SEARCH_ALERT"))
                    .add(Restrictions.or(Restrictions.isNull("aktif"),Restrictions.eq("aktif",Boolean.TRUE)))
                    .addOrder(Order.asc("lastCheckedAt")).addOrder(Order.asc("id"))
                    .setProjection(Projections.property("id")).setMaxResults(clamp(maximumAlerts,1,500)).list();
        }finally{HibernateUtil.closeSessionQuietly(listing);}
        for(Long id:ids){summary.scanned++;processOne(id,clamp(maximumItemsPerAlert,1,50),summary);}
        return summary;
    }

    @SuppressWarnings("unchecked")
    private void processOne(Long preferenceId,int maximum,Summary summary){
        Session session=null;Transaction tx=null;
        try{
            session=HibernateUtil.openSession();tx=session.beginTransaction();
            RepoUserPreference preference=(RepoUserPreference)session.get(RepoUserPreference.class,preferenceId);
            if(preference==null){tx.commit();return;}
            session.lock(preference,LockMode.UPGRADE);
            if(Boolean.FALSE.equals(preference.getAktif())||!"SEARCH_ALERT".equals(preference.getPreferenceType())){tx.commit();return;}
            Date now=new Date();Date since=preference.getLastCheckedAt()!=null?new Date(Math.max(0L,preference.getLastCheckedAt().getTime()-60000L)):preference.getCreatedAt();
            if(since==null)since=now;
            AlertFilter filter=parse(preference.getQueryValue());
            Criteria criteria=criteria(session,preference.getTenantKey(),filter,since)
                    .addOrder(Order.desc("publishedAt")).addOrder(Order.desc("id")).setMaxResults(maximum);
            List<RepoItem> matches=criteria.list();summary.matched+=matches.size();Long newest=null;
            for(RepoItem item:matches){
                Number duplicate=(Number)session.createCriteria(RepoNotification.class)
                        .add(Restrictions.eq("preferenceId",preference.getId())).add(Restrictions.eq("itemId",item.getId()))
                        .setProjection(Projections.rowCount()).uniqueResult();
                if(duplicate!=null&&duplicate.longValue()>0L)continue;
                RepoNotification notification=new RepoNotification();notification.setItemId(item.getId());notification.setPreferenceId(preference.getId());
                notification.setRecipientId(preference.getUserId());notification.setType("SEARCH_ALERT");
                notification.setMessage(limit("Alert “"+safe(preference.getLabel())+"”: "+safe(item.getTitle()),1000));notification.setCreatedAt(now);session.save(notification);
                summary.created++;if(newest==null||item.getId().longValue()>newest.longValue())newest=item.getId();
            }
            preference.setLastCheckedAt(now);preference.setLastError("");preference.setFailureCount(Integer.valueOf(0));
            if(!matches.isEmpty()){preference.setLastMatchedAt(now);if(newest!=null)preference.setLastNotifiedItemId(newest);}
            session.update(preference);tx.commit();
        }catch(Exception error){
            rollback(tx);summary.failed++;
            HibernateUtil.closeSessionQuietly(session);session=null;recordFailure(preferenceId,error);
            ais.common.ErrorAuditUtil.record(error,"RepositoryAlertService.processOne "+preferenceId);
        }finally{HibernateUtil.closeSessionQuietly(session);}
    }

    private void recordFailure(Long preferenceId,Exception error){Session recoverySession=null;Transaction recovery=null;try{recoverySession=HibernateUtil.openSession();recovery=recoverySession.beginTransaction();RepoUserPreference preference=(RepoUserPreference)recoverySession.get(RepoUserPreference.class,preferenceId);if(preference!=null){int failures=preference.getFailureCount()==null?0:preference.getFailureCount().intValue();preference.setFailureCount(Integer.valueOf(failures+1));preference.setLastError(limit(error.getMessage(),1000));recoverySession.update(preference);}recovery.commit();}catch(Exception ignored){rollback(recovery);ais.common.ErrorAuditUtil.record(ignored,"RepositoryAlertService.failure-state");}finally{HibernateUtil.closeSessionQuietly(recoverySession);}}

    private Criteria criteria(Session session,String tenant,AlertFilter filter,Date since){
        Criteria criteria=session.createCriteria(RepoItem.class)
                .add(Restrictions.eq("tenantKey",safe(tenant)))
                .add(Restrictions.or(Restrictions.isNull("aktif"),Restrictions.eq("aktif",Boolean.TRUE)))
                .add(Restrictions.or(Restrictions.isNull("isWithdrawn"),Restrictions.eq("isWithdrawn",Boolean.FALSE)))
                .add(Restrictions.in("syncStatus",PUBLIC_STATUSES))
                .add(Restrictions.sqlRestriction("coalesce({alias}.published_at,{alias}.tanggal_dirubah,{alias}.issued_at) > ?",since,Hibernate.TIMESTAMP));
        if(filter.collectionId!=null)criteria.add(Restrictions.eq("collectionId",filter.collectionId));
        if(filter.type.length()>0)criteria.add(Restrictions.eq("documentType",filter.type));
        if(filter.access.length()>0)criteria.add(Restrictions.eq("accessPolicy",filter.access));
        if(filter.language.length()>0)criteria.add(Restrictions.eq("language",filter.language));
        if(filter.author.length()>0)criteria.add(Restrictions.ilike("authors",filter.author,MatchMode.ANYWHERE));
        if(filter.subject.length()>0)criteria.add(Restrictions.ilike("subjects",filter.subject,MatchMode.ANYWHERE));
        if(filter.identifier.length()>0)criteria.add(Restrictions.or(Restrictions.or(Restrictions.ilike("oaiIdentifier",filter.identifier,MatchMode.ANYWHERE),Restrictions.ilike("dspaceHandle",filter.identifier,MatchMode.ANYWHERE)),Restrictions.ilike("doi",filter.identifier,MatchMode.ANYWHERE)));
        if(filter.program.length()>0)criteria.add(Restrictions.sqlRestriction("exists (select 1 from repo_item_metadata rpm where rpm.item_id={alias}.id and rpm.metadata_field='repository.programStudy' and lower(rpm.metadata_value) like lower(?))","%"+filter.program+"%",Hibernate.STRING));
        if(filter.year!=null){criteria.add(Restrictions.sqlRestriction("extract(year from {alias}.issued_at)=?",filter.year,Hibernate.INTEGER));}
        if("WITH_FILE".equals(filter.fullText))criteria.add(Restrictions.sqlRestriction("exists (select 1 from repo_bitstream rb where rb.item_id={alias}.id and coalesce(rb.aktif,true)=true and rb.access_policy='OPEN_ACCESS' and coalesce(rb.virus_scan_status,'PENDING') not in ('ERROR','INFECTED'))"));
        if(filter.keyword.length()>0)addKeyword(criteria,filter.keyword,filter.field);
        return criteria;
    }

    private void addKeyword(Criteria criteria,String keyword,String field){
        for(String token:keyword.split("\\s+")){if(token.length()<2)continue;
            if("title".equals(field))criteria.add(Restrictions.ilike("title",token,MatchMode.ANYWHERE));
            else if("author".equals(field))criteria.add(Restrictions.ilike("authors",token,MatchMode.ANYWHERE));
            else if("abstract".equals(field)||"fulltext".equals(field))criteria.add(Restrictions.ilike("abstractText",token,MatchMode.ANYWHERE));
            else if("subject".equals(field))criteria.add(Restrictions.ilike("subjects",token,MatchMode.ANYWHERE));
            else criteria.add(Restrictions.disjunction().add(Restrictions.ilike("title",token,MatchMode.ANYWHERE)).add(Restrictions.ilike("authors",token,MatchMode.ANYWHERE)).add(Restrictions.ilike("abstractText",token,MatchMode.ANYWHERE)).add(Restrictions.ilike("subjects",token,MatchMode.ANYWHERE)));
        }
    }

    private AlertFilter parse(String queryValue){
        AlertFilter filter=new AlertFilter();String value=safe(queryValue).replace("&amp;","&");int question=value.indexOf('?');if(question>=0)value=value.substring(question+1);
        Map<String,String> parameters=new LinkedHashMap<String,String>();for(String pair:value.split("&")){int equals=pair.indexOf('=');String name=decode(equals<0?pair:pair.substring(0,equals));String content=decode(equals<0?"":pair.substring(equals+1));if(!parameters.containsKey(name))parameters.put(name,content);}
        filter.keyword=limit(parameters.get("q"),200);filter.field=choice(parameters.get("field"),new String[]{"all","title","author","abstract","subject","fulltext"},"all");
        filter.type=limit(parameters.get("type"),100);filter.access=limit(parameters.get("access"),40);filter.language=limit(parameters.get("language"),20);filter.author=limit(parameters.get("author"),200);filter.subject=limit(parameters.get("subject"),200);filter.identifier=limit(parameters.get("identifier"),255);filter.program=limit(parameters.get("program"),200);filter.fullText=choice(parameters.get("fullText"),new String[]{"WITH_FILE","METADATA_ONLY"},"");
        filter.collectionId=positiveLong(parameters.get("collection"));filter.year=integer(parameters.get("year"));return filter;
    }

    private static class AlertFilter {String keyword="",field="all",type="",access="",language="",author="",subject="",identifier="",program="",fullText="";Long collectionId;Integer year;}
    private static String decode(String value){try{return URLDecoder.decode(safe(value),"UTF-8");}catch(Exception e){return safe(value);}}
    private static String choice(String value,String[] allowed,String fallback){String clean=safe(value);for(String option:allowed)if(option.equalsIgnoreCase(clean))return option;return fallback;}
    private static Long positiveLong(String value){try{long parsed=Long.parseLong(safe(value));return parsed>0?Long.valueOf(parsed):null;}catch(Exception e){return null;}}
    private static Integer integer(String value){try{return Integer.valueOf(Integer.parseInt(safe(value)));}catch(Exception e){return null;}}
    private static int clamp(int value,int minimum,int maximum){return Math.max(minimum,Math.min(value,maximum));}
    private static String safe(String value){return value==null?"":value.trim();}
    private static String limit(String value,int maximum){String clean=safe(value);return clean.length()>maximum?clean.substring(0,maximum):clean;}
    private static void rollback(Transaction tx){try{if(tx!=null&&tx.isActive())tx.rollback();}catch(Exception ignored){}}
}
