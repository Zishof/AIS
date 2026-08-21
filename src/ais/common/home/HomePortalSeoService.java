package ais.common.home;

public class HomePortalSeoService {
    public HomePortalViewModel.Seo build(HomePortalViewModel vm, HomePortalSectionResolver config, String requestUrl) {
        HomePortalViewModel.Seo seo = new HomePortalViewModel.Seo();
        seo.title = config.value("home_v3_meta_title", vm.institution.name + " - Website Resmi");
        seo.description = config.value("home_v3_meta_description", vm.description);
        seo.canonical = requestUrl == null ? "" : requestUrl;
        seo.image = config.value("home_v3_og_image", vm.institution.heroUrl);
        String type = vm.institution.healthcare ? "MedicalOrganization"
                : (vm.institution.college ? "CollegeOrUniversity" : "School");
        StringBuilder json = new StringBuilder();
        json.append("{\"@context\":\"https://schema.org\",\"@type\":\"").append(type).append("\"");
        add(json, "name", vm.institution.name);
        add(json, "url", seo.canonical);
        add(json, "logo", vm.institution.logoUrl);
        add(json, "description", seo.description);
        add(json, "telephone", vm.institution.phone);
        add(json, "email", vm.institution.email);
        if (vm.institution.address != null && vm.institution.address.length() > 0) {
            json.append(",\"address\":{\"@type\":\"PostalAddress\",\"streetAddress\":\"")
                .append(escape(vm.institution.address)).append("\"}");
        }
        json.append("}");
        seo.jsonLd = json.toString();
        return seo;
    }

    private void add(StringBuilder json, String key, String value) {
        if (value != null && value.trim().length() > 0) json.append(",\"").append(key).append("\":\"").append(escape(value)).append("\"");
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", " ").replace("\n", " ").replace("<", "\\u003c");
    }
}
