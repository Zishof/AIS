
package ais.common.newui.menu;

import java.util.HashSet;
import java.util.Set;

/** Self-test resolver native tanpa container Servlet. */
public final class NewUiNativeJspResolverSelfTest {
    private NewUiNativeJspResolverSelfTest() { }
    private static void check(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
    public static void main(String[] args) {
        Set<String> paths = new HashSet<String>();
        paths.add("/WEB-INF/new/root/uiux/mahasiswa.jsp");
        paths.add("/WEB-INF/new/sirs/uiux/pasien.jsp");
        paths.add("/WEB-INF/new/rab/uiux/kalender.jsp");
        paths.add("/WEB-INF/new/kalender/uiux/kalender.jsp");
        NewUiNativeJspResolver.Result mahasiswa = NewUiNativeJspResolver.resolveFromPaths("/pages/master/mahasiswa.zul", false, paths);
        check(mahasiswa != null && "root".equals(mahasiswa.getModule()) && "mahasiswa".equals(mahasiswa.getPage()), "route mahasiswa");
        NewUiNativeJspResolver.Result pasien = NewUiNativeJspResolver.resolveFromPaths("/pages/master/sirs/pasien.zul", true, paths);
        check(pasien != null && "/WEB-INF/new/sirs/services/pasien_service.jsp".equals(pasien.getTarget()), "service pasien");
        NewUiNativeJspResolver.Result kalender = NewUiNativeJspResolver.resolveFromPaths("/pages/master/rab/kalender.zul", false, paths);
        check(kalender != null && "rab".equals(kalender.getModule()), "disambiguasi modul kalender");
        check(NewUiNativeJspResolver.resolveFromPaths("/pages/master/tidak_ada.zul", false, paths) == null, "route tak dikenal harus fail-closed");

        // Scaffold bernama sama pada modul induk dan submodulnya: submodul
        // hanya boleh menang bila route lama benar-benar menyebut namanya.
        Set<String> kembar = new HashSet<String>();
        kembar.add("/WEB-INF/new/root/uiux/pertemuan.jsp");
        kembar.add("/WEB-INF/new/root/maintenance/uiux/pertemuan.jsp");
        kembar.add("/WEB-INF/new/root/uiux/paket.jsp");
        kembar.add("/WEB-INF/new/root/pmb/uiux/paket.jsp");
        NewUiNativeJspResolver.Result pertemuan = NewUiNativeJspResolver.resolveFromPaths("/pages/master/pertemuan.zul", false, kembar);
        check(pertemuan != null && "root".equals(pertemuan.getModule()), "pertemuan master harus menang atas submodul maintenance");
        NewUiNativeJspResolver.Result pemeliharaan = NewUiNativeJspResolver.resolveFromPaths("/pages/maintenance/pertemuan.zul", false, kembar);
        check(pemeliharaan != null && "root/maintenance".equals(pemeliharaan.getModule()), "route maintenance harus memilih submodulnya");
        NewUiNativeJspResolver.Result paket = NewUiNativeJspResolver.resolveFromPaths("/pages/master/paket.zul", true, kembar);
        check(paket != null && "/WEB-INF/new/root/services/paket_service.jsp".equals(paket.getTarget()), "service paket master");
        NewUiNativeJspResolver.Result paketPmb = NewUiNativeJspResolver.resolveFromPaths("/pages/pmb/paket.zul", true, kembar);
        check(paketPmb != null && "/WEB-INF/new/root/pmb/services/paket_service.jsp".equals(paketPmb.getTarget()),
                "service submodul tidak boleh melompat ke berkas modul induk");

        System.out.println("NewUiNativeJspResolverSelfTest OK");
    }
}
