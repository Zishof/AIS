package ais.common.home;

import java.util.ArrayList;
import java.util.List;

/** Data contract for indexable public pages below /web. */
public class WebsitePageViewModel {
    public HomePortalViewModel portal;
    public String path;
    public String eyebrow;
    public String title;
    public String description;
    public String body;
    public String updatedLabel;
    public String jsonLd;
    public boolean searchPage;
    public boolean notFound;
    public String searchQuery;
    public List<Section> sections = new ArrayList<Section>();
    public List<Card> cards = new ArrayList<Card>();
    public List<Crumb> breadcrumbs = new ArrayList<Crumb>();

    public static class Section {
        public String title;
        public String body;
    }

    public static class Card {
        public String eyebrow;
        public String title;
        public String summary;
        public String meta;
        public String url;
    }

    public static class Crumb {
        public String label;
        public String url;
    }
}
