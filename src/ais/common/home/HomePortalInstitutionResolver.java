package ais.common.home;

import javax.servlet.http.HttpServletRequest;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.master.sirs.util.RumahSakitUtil;
import ais.common.Common;
import ais.database.model.PerguruanTinggi;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.database.model.sirs.RumahSakit;

/** Resolves website identity separately from the authenticated application shell. */
public class HomePortalInstitutionResolver {
    public HomePortalViewModel.Institution resolve(HttpServletRequest request) {
        HomePortalViewModel.Institution institution = new HomePortalViewModel.Institution();
        boolean ya = schoolOrFoundationMode();
        RumahSakit hospital = getHospital(request);
        Sekolah school = getSchool(request);
        Yayasan foundation = getFoundation(request);
        PerguruanTinggi college = getCollege(request);

        // Health domain is independent; education priority remains School -> Foundation -> College.
        if (hospital != null && hospital.getId() != null) {
            applyHealthcare(institution, request, hospital);
        } else if (school != null && school.getId() != null) {
            applySchool(institution, request, school);
        } else if (ya && foundation != null && foundation.getId() != null) {
            applyFoundation(institution, request, foundation);
        } else if (college != null && college.getId() != null) {
            applyCollege(institution, request, college);
        } else {
            applyFallback(institution);
        }

        finish(institution);
        return institution;
    }

    private void applyHealthcare(HomePortalViewModel.Institution i, HttpServletRequest request, RumahSakit hospital) {
        i.type = "healthcare";
        i.healthcare = true;
        i.college = false;
        i.hospitalId = hospital.getId();
        i.category = hospital.getLabelJenisFasilitas();
        i.name = text(hospital.getNama(), i.category);
        i.motto = text(hospital.getMotto(), hospital.getDeskripsi());
        i.address = text(hospital.getAlamat(), "");
        i.phone = text(hospital.getTelepon(), hospital.getWhatsapp());
        i.email = text(hospital.getEmail(), "");
        i.themeCss = theme(hospital.getCss());
        i.themePrimary = color(hospital.getWarna());
        i.themePrimaryDark = darken(i.themePrimary);
        i.logoUrl = RumahSakitUtil.getRumahSakitMedia(request, "logo_rumah_sakit_", hospital);
        i.heroUrl = RumahSakitUtil.getRumahSakitMedia(request, "background_rumah_sakit_", hospital);
    }

    private void applySchool(HomePortalViewModel.Institution i, HttpServletRequest request, Sekolah school) {
        i.type = "school";
        i.healthcare = false;
        i.college = false;
        i.schoolId = school.getId();
        i.name = text(school.getNama(), "Sekolah");
        i.motto = text(school.getMotto(), "");
        i.address = text(school.getAlamat(), "");
        i.phone = text(school.getTelp(), "");
        i.email = text(school.getEmail(), "");
        i.themeCss = theme(school.getCss());
        i.logoUrl = SekolahUtil.getSekolahMedia(request, "logo_sekolah_", school);
        i.heroUrl = SekolahUtil.getSekolahMedia(request, "background_sekolah_", school);
    }

    private void applyFoundation(HomePortalViewModel.Institution i, HttpServletRequest request, Yayasan foundation) {
        i.type = "foundation";
        i.healthcare = false;
        i.college = false;
        i.foundationId = foundation.getId();
        i.name = text(foundation.getNama(), "Yayasan Pendidikan");
        i.motto = text(foundation.getMotto(), "");
        i.address = text(foundation.getAlamat(), "");
        i.phone = text(foundation.getTelp(), "");
        i.email = text(foundation.getEmail(), "");
        i.themeCss = "";
        i.themePrimary = color(foundation.getWarna());
        i.themePrimaryDark = darken(i.themePrimary);
        i.logoUrl = SekolahUtil.getYayasanMedia(request, "logo_yayasan_", foundation);
        i.heroUrl = SekolahUtil.getYayasanMedia(request, "background_yayasan_", foundation);
    }

    private void applyCollege(HomePortalViewModel.Institution i, HttpServletRequest request, PerguruanTinggi college) {
        i.type = "college";
        i.healthcare = false;
        i.college = true;
        i.id = college.getId();
        i.name = text(college.getNama(), "Perguruan Tinggi");
        i.motto = text(college.getMotto(), "");
        i.address = text(college.getAlamat1(), "");
        i.phone = text(college.getTelepon(), "");
        i.email = text(college.getEmail(), "");
        i.themeCss = theme(college.getCss());
        i.logoUrl = PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_", college);
        i.heroUrl = PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "background_perguruanTinggi_", college);
    }

    private void applyFallback(HomePortalViewModel.Institution i) {
        i.type = "college";
        i.healthcare = false;
        i.college = true;
        i.name = "Institusi Pendidikan";
        i.motto = "";
        i.address = "";
        i.phone = "";
        i.email = "";
        i.themeCss = "";
    }

    private void finish(HomePortalViewModel.Institution i) {
        if (i.name == null || i.name.trim().length() == 0) i.name = "Institusi Pendidikan";
        i.shortName = i.name.length() > 44 ? i.name.substring(0, 41) + "..." : i.name;
        String root = Common.ROOT == null ? "" : Common.ROOT;
        if (i.logoUrl == null || i.logoUrl.trim().length() == 0) i.logoUrl = root + "/img/logo.png";
        if (i.heroUrl == null || i.heroUrl.trim().length() == 0) i.heroUrl = root + "/img/pmb_bg.webp";
        if (i.themeCss == null) i.themeCss = "";
        if (i.themePrimary == null) i.themePrimary = "";
        if (i.themePrimaryDark == null) i.themePrimaryDark = "";
        if (i.category == null) i.category = "";
    }

    private boolean schoolOrFoundationMode() {
        try {
            boolean[] detected = Common.chekPtAtauSekolah(null);
            return detected != null && detected.length > 1 && detected[1];
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "HomePortalInstitutionResolver.mode");
            return false;
        }
    }

    private Sekolah getSchool(HttpServletRequest request) {
        try { return SekolahUtil.getSekolah(request); }
        catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "HomePortalInstitutionResolver.school"); return null; }
    }

    private RumahSakit getHospital(HttpServletRequest request) {
        try { return RumahSakitUtil.getRumahSakit(request); }
        catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "HomePortalInstitutionResolver.healthcare"); return null; }
    }

    private Yayasan getFoundation(HttpServletRequest request) {
        try { return SekolahUtil.getYayasan(request); }
        catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "HomePortalInstitutionResolver.foundation"); return null; }
    }

    private PerguruanTinggi getCollege(HttpServletRequest request) {
        try { return PerguruanTinggiUtil.getPerguruanTinggi(request); }
        catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "HomePortalInstitutionResolver.college"); return null; }
    }

    private String theme(String raw) {
        if (raw == null || raw.trim().length() == 0) return "";
        String file = raw.replace('\\', '/');
        file = file.substring(file.lastIndexOf('/') + 1);
        return file.matches("[A-Za-z0-9._-]+\\.css") ? "/css/baru/" + file : "";
    }

    private String color(String raw) {
        if (raw == null) return "";
        String value = raw.trim();
        if (value.matches("#[0-9A-Fa-f]{3}")) {
            value = "#" + value.substring(1, 2) + value.substring(1, 2)
                    + value.substring(2, 3) + value.substring(2, 3)
                    + value.substring(3, 4) + value.substring(3, 4);
        }
        return value.matches("#[0-9A-Fa-f]{6}") ? value.toLowerCase() : "";
    }

    private String darken(String value) {
        if (value == null || !value.matches("#[0-9A-Fa-f]{6}")) return "";
        int red = Integer.parseInt(value.substring(1, 3), 16);
        int green = Integer.parseInt(value.substring(3, 5), 16);
        int blue = Integer.parseInt(value.substring(5, 7), 16);
        return String.format("#%02x%02x%02x", Integer.valueOf(red * 45 / 100),
                Integer.valueOf(green * 45 / 100), Integer.valueOf(blue * 45 / 100));
    }

    private String text(String value, String fallback) {
        return value == null || value.trim().length() == 0 ? fallback : value.trim();
    }
}
