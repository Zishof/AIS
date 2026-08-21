package ais.common.home;

import javax.servlet.http.HttpServletRequest;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.database.model.PerguruanTinggi;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

public class HomePortalInstitutionResolver {
    public HomePortalViewModel.Institution resolve(HttpServletRequest request) {
        HomePortalViewModel.Institution i = new HomePortalViewModel.Institution();
        i.type = "college";
        i.college = true;
        try {
            boolean[] detected = Common.chekPtAtauSekolah(null);
            i.college = detected != null && detected.length > 0 && detected[0];
            if (!i.college && (detected == null || detected.length < 2 || !detected[1])) i.college = true;
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "HomePortalInstitutionResolver.detect");
        }
        PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);
        if (i.college && pt != null) {
            i.id = pt.getId();
            i.name = text(pt.getNama(), "Institusi Pendidikan");
            i.motto = text(pt.getMotto(), "");
            i.address = text(pt.getAlamat1(), "");
            i.phone = text(pt.getTelepon(), "");
            i.email = text(pt.getEmail(), "");
            i.themeCss = theme(pt.getCss());
        } else {
            i.college = false;
            i.type = "school";
            Sekolah school = SekolahUtil.getSekolah(request);
            Yayasan foundation = SekolahUtil.getYayasan(request);
            if (school != null) {
                i.schoolId = school.getId();
                i.name = text(school.getNama(), "Institusi Pendidikan");
                i.motto = text(school.getMotto(), "");
                i.address = text(school.getAlamat(), "");
                i.email = text(school.getEmail(), "");
                i.themeCss = theme(school.getCss());
            } else if (foundation != null) {
                i.foundationId = foundation.getId();
                i.name = text(foundation.getNama(), "Yayasan Pendidikan");
                i.address = text(foundation.getAlamat(), "");
                i.email = text(foundation.getEmail(), "");
            }
        }
        if (i.name == null) i.name = "Institusi Pendidikan";
        i.shortName = i.name.length() > 44 ? i.name.substring(0, 41) + "..." : i.name;
        i.logoUrl = PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
        i.heroUrl = PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "background_perguruanTinggi_");
        String root = Common.ROOT == null ? "" : Common.ROOT;
        if (i.logoUrl == null || i.logoUrl.trim().length() == 0) i.logoUrl = root + "/img/logo.png";
        if (i.heroUrl == null || i.heroUrl.trim().length() == 0) i.heroUrl = root + "/img/pmb_bg.webp";
        return i;
    }

    private String theme(String raw) {
        if (raw == null || raw.trim().length() == 0) return "";
        String file = raw.replace('\\', '/');
        file = file.substring(file.lastIndexOf('/') + 1);
        return file.matches("[A-Za-z0-9._-]+\\.css") ? "/css/baru/" + file : "";
    }

    private String text(String value, String fallback) {
        return value == null || value.trim().length() == 0 ? fallback : value.trim();
    }
}
