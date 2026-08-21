package ais.common.home;

import java.util.ArrayList;
import java.util.List;

/** Immutable-at-render-time data contract for the public home page. */
public class HomePortalViewModel {
    public Institution institution = new Institution();
    public Terminology terminology = new Terminology();
    public Seo seo = new Seo();
    public String assetVersion;
    public String contextPath;
    public String language;
    public String direction;
    public String eyebrow;
    public String headline;
    public String description;
    public String primaryLabel;
    public String primaryUrl;
    public String primaryTarget;
    public String primaryRel;
    public String secondaryLabel;
    public String secondaryUrl;
    public String secondaryTarget;
    public String secondaryRel;
    public String loginUrl;
    public String loginTarget;
    public String loginRel;
    public String announcementText;
    public String announcementUrl;
    public String announcementTarget;
    public String announcementRel;
    public String androidUrl;
    public String iosUrl;
    public boolean showAnnouncement;
    public boolean showPrograms;
    public boolean showAdmission;
    public boolean showNews;
    public boolean showAgenda;
    public boolean showImpact;
    public boolean showPartners;
    public boolean showSearch;
    public boolean showLanguage;
    public List<ServiceItem> services = new ArrayList<ServiceItem>();
    public List<ProgramItem> programs = new ArrayList<ProgramItem>();
    public List<StatItem> statistics = new ArrayList<StatItem>();
    public List<NewsItem> news = new ArrayList<NewsItem>();
    public List<AgendaItem> agenda = new ArrayList<AgendaItem>();
    public List<ImpactItem> impacts = new ArrayList<ImpactItem>();
    public Admission admission;

    public static class Institution {
        public Long id;
        public Long schoolId;
        public Long foundationId;
        public String type;
        public String name;
        public String shortName;
        public String motto;
        public String address;
        public String phone;
        public String email;
        public String logoUrl;
        public String heroUrl;
        public String themeCss;
        public boolean college;
    }

    public static class Terminology {
        public String learnerPlural;
        public String teacherPlural;
        public String admissionLabel;
        public String programLabel;
        public String institutionLabel;
    }

    public static class Seo {
        public String title;
        public String description;
        public String canonical;
        public String image;
        public String jsonLd;
    }

    public static class LinkItem {
        public String label;
        public String url;
        public String target;
        public String rel;
    }

    public static class ServiceItem extends LinkItem {
        public String description;
        public String icon;
        public String group;
    }

    public static class ProgramItem extends LinkItem {
        public String level;
        public String accreditation;
        public String unit;
        public String description;
    }

    public static class StatItem {
        public String value;
        public String label;
    }

    public static class NewsItem extends LinkItem {
        public String summary;
        public String date;
        public String category;
        public String imageUrl;
    }

    public static class AgendaItem extends LinkItem {
        public String day;
        public String month;
        public String date;
        public String description;
    }

    public static class ImpactItem extends LinkItem {
        public String description;
        public String icon;
    }

    public static class Admission extends LinkItem {
        public String period;
        public String startDate;
        public String endDate;
        public String description;
        public boolean open;
    }
}
