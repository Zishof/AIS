<%@page import="ais.common.MenuHelper"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.inventory.Pedagang"%>
<%@page import="ais.common.CommonMenu"%>
<%@page import="java.util.Arrays"%>
<%@page import="java.util.List"%>
<%@page import="ais.database.model.Tbmrole"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%!
    private boolean bolehAksesMenuKantinPedagang(Tbmuser user, Tbmrole role, String menu) {
        if (role == null || menu == null) return true;
        // Kompatibilitas role dasar Kantin lama: admin kantin (tanpa relasi Pedagang/Toko)
        // selalu perlu menu ini untuk menambahkan akun pedagang. Role khusus dan pedagang
        // biasa tetap tunduk pada menu.pedagang di hak akses grup.
        if ("pedagang".equals(menu)
                && user != null && user.getPedagang() == null
                && role.getRoleId() != null && role.getRoleId().equalsIgnoreCase(Tbmrole.KANTIN)) {
            return true;
        }
        org.json.JSONObject ebisnisMenuRole = ais.common.EbisnisMenuKatalog.urai(role.getEbisnisMenu());
        if ("beranda".equals(menu)) return ebisnisMenuRole.optBoolean("berandaKantin", false);
        org.json.JSONObject menuTersimpan = ebisnisMenuRole.optJSONObject("menu");
        if ("ringkasan".equals(menu)) return menuTersimpan.optBoolean("ringkasan", true);
        if ("pos".equals(menu)) return menuTersimpan.optBoolean("kasir", true);
        if ("pesanan".equals(menu)) return menuTersimpan.optBoolean("pesanan", true);
        if ("pengaturan".equals(menu)) return menuTersimpan.optBoolean("konfigurasi", true);
        if ("laporan_laporan".equals(menu)) return menuTersimpan.optBoolean("laporan", true);
        // Menu Akuntansi/Laporan Keuangan fail-closed: bawaannya hanya terbuka utk peran
        // keu (Keuangan) dan am (Admin); setelan eksplisit pada peran tetap menang. Aturan
        // yang SAMA dipakai POS lewat PosApi -> EbisnisMenuKatalog.aksesAkuntansi.
        if ("laporan_keuangan".equals(menu)) {
            return ais.common.EbisnisMenuKatalog.aksesAkuntansi(role.getEbisnisMenu(),
                    role.getRoleId(), "laporankeuangan");
        }
        if ("anggota".equals(menu)) return menuTersimpan.optBoolean("anggota", true);
        if ("barang".equals(menu)) return menuTersimpan.optBoolean("produk", true);
        if ("kulakan".equals(menu)) return menuTersimpan.optBoolean("kulakan", true);
        if ("retur_penjualan".equals(menu)) return menuTersimpan.optBoolean("returpenjualan", true);
        if ("diskon".equals(menu)) return menuTersimpan.optBoolean("diskon", true);
        if ("pembayaran".equals(menu)) return menuTersimpan.optBoolean("pembayaran", true);
        if ("pedagang".equals(menu)) return menuTersimpan.optBoolean("pedagang", true);
        if ("stok".equals(menu)) return menuTersimpan.optBoolean("stokopname", true);
        if ("meja".equals(menu)) return menuTersimpan.optBoolean("meja", true);
        if ("penyedia".equals(menu)) return menuTersimpan.optBoolean("penyedia", true);
        if ("kas".equals(menu)) return menuTersimpan.optBoolean("kaskasir", true);
        if ("tenant".equals(menu)) return menuTersimpan.optBoolean("setorantenant", true);
        if ("opname".equals(menu)) return menuTersimpan.optBoolean("jadwalopname", true);
        if ("stok_expired".equals(menu)) return menuTersimpan.optBoolean("stokexpired", true);
        if ("limit_kredit".equals(menu)) return menuTersimpan.optBoolean("limitkredit", true);
        if ("mutasi_rekening".equals(menu)) return menuTersimpan.optBoolean("mutasirekening", true);
        if ("produksi".equals(menu)) return menuTersimpan.optBoolean("produksi", true);
        if ("pengaturan_laporan".equals(menu)) return menuTersimpan.optBoolean("pengaturanlaporan", true);
        // Modul Pengadaan POS -- default TAMPIL atas permintaan pemilik produk (2026-08-20);
        // hak per-aksi tetap diatur admin lewat grid CRUD TbmroleAction.
        if ("pengadaan_pr".equals(menu)) return menuTersimpan.optBoolean("pengadaan_pr", true);
        if ("pengadaan_po".equals(menu)) return menuTersimpan.optBoolean("pengadaan_po", true);
        if ("pengadaan_bast".equals(menu)) return menuTersimpan.optBoolean("pengadaan_bast", true);
        if ("pengadaan_tagihan".equals(menu)) return menuTersimpan.optBoolean("pengadaan_tagihan", true);
        if ("pengadaan_dpc".equals(menu)) return menuTersimpan.optBoolean("pengadaan_dpc", true);
        if ("pengadaan_bdp".equals(menu)) return menuTersimpan.optBoolean("pengadaan_bdp", true);
        // Bulk entry memakai hak menu tahap manapun; cukup salah satu aktif.
        if ("pengadaan_bulk".equals(menu)) return menuTersimpan.optBoolean("pengadaan_pr", true)
                || menuTersimpan.optBoolean("pengadaan_po", true)
                || menuTersimpan.optBoolean("pengadaan_bast", true);
        return true;
    }
%>

<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    
    if (tbmuser != null && tbmuser.getUserId() != null && tbmuser.getAktif()) {
        Tbmrole tbmrole = tbmuser.hakAkses();
        
        String p = (request.getParameter("p") == null) ? "" : request.getParameter("p").trim();
        String s = (request.getParameter("s") == null) ? "" : request.getParameter("s").trim();
        if (p.length() == 0 && request.getAttribute("default_p") instanceof String) {
            p = ((String) request.getAttribute("default_p")).trim();
        }
        if (s.length() == 0 && request.getAttribute("default_s") instanceof String) {
            s = ((String) request.getAttribute("default_s")).trim();
        }
        String ctx = request.getContextPath();
        boolean kantinMemberLanding = tbmrole != null && ais.common.EbisnisMenuKatalog.urai(tbmrole.getEbisnisMenu()).optBoolean("landingKantin", false);

        List<String> validPages = Arrays.asList(new String[]{
            "laporan/akademik", "home", "elearning", "prestasi", "pustaka", 
            "pagesmastersoppengajuananda", "akademik", "suratmenyurat", "pengadaan", 
            "pembayaran", "keuangan", "akuntansi", "kepegawaian", 
            "kinerja", "presensi", "kalenderakademik", "infokegiatan", "kantin","repository"
        });
        boolean pilih = p.isEmpty() || validPages.contains(p);
        
        Pedagang pedagang = tbmuser.getPedagang();
        Toko toko = pedagang == null ? null : pedagang.getToko();
        boolean isAdminKantin = (toko == null);
        boolean pt = false;
        boolean ya = false;

        try {
            if (tbmuser != null) {
                boolean[] ptYa = Common.chekPtAtauSekolah(tbmuser);
                pt = ptYa[0];
                ya = ptYa[1];
            }
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/include/menu.jsp:46");
        } finally {
            try { HibernateUtil.closeSession(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/include/menu.jsp:48");}
        }
%>

<nav class="navbar navbar-expand-lg navbar-dark shadow-sm sticky-top navbar-mega-container">
    <div class="container-fluid">
        <a class="navbar-brand fw-bold text-white" href="<%=ctx%>/baru?p=home">
            <i class="fas fa-university me-2"></i><%=Common.getBahasaConfig("Home") %>
        </a>
        
        <button class="navbar-toggler border-0" type="button" data-bs-toggle="collapse" data-bs-target="#megaMenuContent">
            <span class="navbar-toggler-icon"></span>
        </button>

        <div class="collapse navbar-collapse" id="megaMenuContent">
            <ul class="navbar-nav me-auto mb-2 mb-lg-0">
                
                <%-- MEGA MENU 1: DASHBOARD UTAMA --%>
                <li class="nav-item dropdown mega-dropdown">
                    <a class="nav-link dropdown-toggle fw-bold text-white <%=pilih ? "active" : ""%>" href="#" data-bs-toggle="dropdown">
                        <i class="fas fa-th-large me-1"></i> <%=Common.getBahasaConfig("Dashboard Utama") %>
                    </a>
                    
                    <div class="dropdown-menu mega-menu shadow-lg border-0">
                        <div class="container-fluid">
                            <div class="row">
                                <div class="col-md-3 mb-3">
                                    <% if(tbmrole != null && tbmrole.getHalamanUtama() == null && !kantinMemberLanding) { %>
                                        <h6 class="mega-menu-header"><%=Common.getBahasaConfig("Umum") %></h6>
                                        <a class="mega-menu-item <%=(p.isEmpty() || p.equals("home")) ? "active" : "" %>" href="<%=ctx%>/baru?p=home">
                                            <i class="fas fa-home"></i> <span><%=Common.getBahasaConfig("Beranda") %></span>
                                        </a>
                                    <% } %>

                                    <% if(tbmrole != null && tbmrole.getElearning()) { %>
                                        <h6 class="mega-menu-header mt-3"><%=Common.getBahasaConfig("e-Learning") %></h6>
                                        <% 
                                           String[] subEL = pt ? new String[]{"linimasa", "ringkasan", "ujian", "tugas", "tugas_kelompok", "materi", "audio", "video", "kalender","kurikulum_obe","portofolio_mahasiswa"} : new String[]{"linimasa", "ringkasan", "ujian", "tugas", "tugas_kelompok", "materi", "audio", "video", "kalender"};
                                           for(String sub : subEL) {
                                               String label = sub.substring(0,1).toUpperCase() + sub.substring(1).replace("_", " ");
                                        %>
                                        <a class="mega-menu-item <%= (p.equals("elearning") && s.equals(sub)) ? "active" : "" %>" href="<%=ctx%>/baru?p=elearning&s=<%=sub%>">
                                           <i class="<%=MenuHelper.getIconForMenu(label)%>"></i> <span><%=Common.getBahasaConfig(label) %></span>
                                        </a>
                                        <% } %>
                                    <% } %>
                                </div>

                                <div class="col-md-3 mb-3">
                                    <% if(tbmrole != null && tbmrole.getKegiatanDanPrestasi()) { %>
                                        <h6 class="mega-menu-header"><%=Common.getBahasaConfig("Prestasi") %></h6>
                                        <% 
                                           String[] subPres = {"prestasi", "kegiatan", "karya", "organisasi", "catatan"};
                                           for(String sub : subPres) {
                                               String[] kataKata = sub.split("_");
                                               StringBuilder hasilAkhir = new StringBuilder();
                                               for (String kata : kataKata) {
                                                   if (kata.length() > 0) {
                                                       hasilAkhir.append(kata.substring(0, 1).toUpperCase()).append(kata.substring(1).toLowerCase()).append(" ");
                                                   }
                                               }
                                               String label = hasilAkhir.toString().trim();
                                        %>
                                        <a class="mega-menu-item <%= (p.equals("prestasi") && s.equals(sub)) ? "active" : "" %>" href="<%=ctx%>/baru?p=prestasi&s=<%=sub%>">
                                           <i class="<%=MenuHelper.getIconForMenu(label)%>"></i> <span><%=Common.getBahasaConfig(label) %></span>
                                        </a>
                                        <% } %>
                                    <% } %>

                                    <% if(tbmrole != null && tbmrole.getPustaka()) { %>
                                        <h6 class="mega-menu-header mt-3"><%=Common.getBahasaConfig("Pustaka") %></h6>
                                        <% 
                                           String[] subPus = {"katalog", "populer", "sirkulasi", "kunjungan", "dashboard", "dashboard", "beranda_anggota"};
                                           for(String sub : subPus) {
                                               String[] kataKata = sub.split("_");
                                               StringBuilder hasilAkhir = new StringBuilder();
                                               for (String kata : kataKata) {
                                                   if (kata.length() > 0) {
                                                       hasilAkhir.append(kata.substring(0, 1).toUpperCase()).append(kata.substring(1).toLowerCase()).append(" ");
                                                   }
                                               }
                                               String label = hasilAkhir.toString().trim();
                                        %>
                                        <a class="mega-menu-item <%= (p.equals("pustaka") && s.equals(sub)) ? "active" : "" %>" href="<%=ctx%>/baru?p=pustaka&s=<%=sub%>">
                                           <i class="<%=MenuHelper.getIconForMenu(label)%>"></i> <span><%=Common.getBahasaConfig(label) %></span>
                                        </a>
                                        <% } %>
                                    <% } %>
                                </div>

                                <div class="col-md-3 mb-3">
                                    <h6 class="mega-menu-header"><%=Common.getBahasaConfig("Administrasi & Layanan") %></h6>
                                    <% 
                                        if(tbmrole != null) {
                                            String[][] menus = {
                                                {"pagesmastersoppengajuananda", "getWorkflow", "Pengajuan Anda"},
                                                {"akademik", "getDashboard", "Akademik"},
                                                {"suratmenyurat", "getAdministrasi", "Surat Menyurat"},
                                                {"pengadaan", "getPengadaan", "Pengadaan"},
                                                {"pembayaran", "getPembayaran", "Pembayaran"},
                                                {"keuangan", "getKeuangan", "Keuangan"},
                                                {"akuntansi", "getAkunting", "Akuntansi"}
                                            };
                                            for(String[] menu : menus) {
                                                boolean hasAccess = false;
                                                if(menu[1].equals("getWorkflow")) hasAccess = tbmrole.getWorkflow(); // "Pengajuan Anda" (Dashboard Workflow/SOP) HANYA tampil bila flag "Tampilkan Dashboard Workflow / SOP" diaktifkan pada grup pengguna
                                                else if(menu[1].equals("getDashboard")) hasAccess = tbmrole.getDashboard();
                                                else if(menu[1].equals("getAdministrasi")) hasAccess = tbmrole.getAdministrasi();
                                                else if(menu[1].equals("getPengadaan")) hasAccess = tbmrole.getPengadaan();
                                                else if(menu[1].equals("getPembayaran")) hasAccess = tbmrole.getPembayaran();
                                                else if(menu[1].equals("getKeuangan")) hasAccess = tbmrole.getKeuangan();
                                                else if(menu[1].equals("getAkunting")) hasAccess = tbmrole.getAkunting();
                                                
                                                if(hasAccess) {
                                    %>
                                    <a class="mega-menu-item <%=p.equals(menu[0]) ? "active" : "" %>" href="<%=ctx%>/baru?p=<%=menu[0]%>">
                                        <i class="<%=MenuHelper.getIconForMenu(menu[2])%>"></i> <span><%=Common.getBahasaConfig(menu[2]) %></span>
                                    </a>
                                    <%          }
                                            }
                                        }
                                    %>
                                    
                                    <% if(tbmrole != null && tbmrole.getKegiatanDanPrestasi()) { %>
                                        <div class="mt-3">
                                            <% out.print(CommonMenu.loadTreeDynamicReport(tbmuser, request, "laporan")
                                                 .replace("nav-link", "mega-menu-item")
                                                 .replace("nav-item", "")
                                                 .replace("fas fa-chart-pie", "fas fa-chart-bar")); %> 
                                        </div>
                                    <% } %>
                                </div>

                                <div class="col-md-3 mb-3">
                                    <h6 class="mega-menu-header"><%=Common.getBahasaConfig("Lainnya") %></h6>
                                    <% 
                                        if(tbmrole != null) {
                                            String[][] menusLain = {
                                                {"kepegawaian", "getKepegawaian", "Kepegawaian"},
                                                {"kinerja", "getKinerja", "Kinerja"},
                                                {"presensi", "getPresensiKehadiran", "Presensi"},
                                                {"kalenderakademik", "getKalenderAkademik", "Kalender Akademik"},
                                                {"infokegiatan", "getInfoKegiatan", "Info Kegiatan"},
                                                {"repository", "getRepository", "Repository"}
                                            };
                                            for(String[] menu : menusLain) {
                                                boolean hasAccess = false;
                                                if(menu[1].equals("getKepegawaian")) hasAccess = tbmrole.getKepegawaian();
                                                else if(menu[1].equals("getKinerja")) hasAccess = tbmrole.getKinerja();
                                                else if(menu[1].equals("getPresensiKehadiran")) hasAccess = tbmrole.getPresensiKehadiran();
                                                else if(menu[1].equals("getKalenderAkademik")) hasAccess = tbmrole.getKalenderAkademik();
                                                else if(menu[1].equals("getInfoKegiatan")) hasAccess = tbmrole.getInfoKegiatan();
                                                else if(menu[1].equals("getRepository")) hasAccess = tbmrole.getDasborRepository();
                                                
                                                if(hasAccess) {
                                    %>
                                    <a class="mega-menu-item <%=p.equals(menu[0]) ? "active" : "" %>" href="<%=ctx%>/baru?p=<%=menu[0]%>">
                                        <i class="<%=MenuHelper.getIconForMenu(menu[2])%>"></i> <span><%=Common.getBahasaConfig(menu[2]) %></span>
                                    </a>
                                    <%          }
                                            }
                                        }
                                    %>

                                    <%-- Kantin Section --%>
                                    <% if(tbmrole != null && tbmrole.getKantin()) { %>
                                        <h6 class="mega-menu-header mt-3"><%=Common.getBahasaConfig("e-Kantin") %></h6>
                                        <% 
                                           String[] subEL = tbmuser.getPedagang() == null && tbmrole.getRoleId() != null && !tbmrole.getRoleId().equals(Tbmrole.KANTIN) ? new String[]{"beranda", "ringkasan", "pos", "pesanan", "pengaturan", "laporan_laporan", "laporan_keuangan"} : new String[]{"ringkasan", "pos", "pesanan", "pengaturan", "laporan_laporan", "laporan_keuangan"};
                                           String[] subSubEL = (isAdminKantin ? new String[]{"anggota", "barang", "kulakan","retur_penjualan","diskon","pembayaran","pedagang","stok","stok_expired","meja","penyedia","kas","tenant","opname","limit_kredit","mutasi_rekening","produksi","pengadaan_pr","pengadaan_po","pengadaan_bast","pengadaan_tagihan","pengadaan_dpc","pengadaan_bdp","pengadaan_bulk","pengaturan_laporan"} : new String[]{ "barang", "kulakan","retur_penjualan","diskon","pembayaran","pedagang","stok","stok_expired","meja","penyedia","kas","tenant","opname","limit_kredit","mutasi_rekening","produksi","pengadaan_pr","pengadaan_po","pengadaan_bast","pengadaan_tagihan","pengadaan_dpc","pengadaan_bdp","pengadaan_bulk"});
                                           for(String sub : subEL) {
                                               if(!Common.bolehKonfigurasi("kantin_menu_"+sub)){ continue; } // on/off menu via Konfigurasi (default ON)
                                               if(!bolehAksesMenuKantinPedagang(tbmuser, tbmrole, sub)){ continue; }
                                               String label = sub.substring(0,1).toUpperCase() + sub.substring(1).replace("_", " ");
                                               if(sub.equals("laporan_laporan")){ label = "Laporan-Laporan"; }
                                               if(sub.equals("laporan_keuangan")){ label = "Laporan Keuangan"; }
                                               if(label.equalsIgnoreCase("pengaturan")) {
                                        %>
                                                 <div class="fw-bold mt-2 mb-1 ms-2" style="font-size:12px; color:var(--theme-primary);"><i class="fas fa-cogs me-1"></i><%=Common.getBahasaConfig(label) %></div>
                                        <%
                                                   for(String subSub : subSubEL) {
                                                       if(!Common.bolehKonfigurasi("kantin_menu_"+subSub)){ continue; } // on/off submenu via Konfigurasi (default ON)
                                                       if(!bolehAksesMenuKantinPedagang(tbmuser, tbmrole, subSub)){ continue; }
                                                       label = subSub.substring(0,1).toUpperCase() + subSub.substring(1).replace("_", " ");
                                                       if(subSub.equals("retur_penjualan")){ label = "Retur Penjualan"; }
                                                       if(subSub.equals("kas")){ label = "Kas Kasir"; }
                                                       if(subSub.equals("tenant")){ label = "Setoran Tenant"; }
                                                       if(subSub.equals("opname")){ label = "Jadwal Opname"; }
                                                       if(subSub.equals("stok_expired")){ label = "Kedaluwarsa"; }
                                                       if(subSub.equals("limit_kredit")){ label = "Limit Kredit Anggota"; }
                                                       if(subSub.equals("mutasi_rekening")){ label = "Rekening Koran (Rekonsiliasi)"; }
                                                       if(subSub.equals("produksi")){ label = "Produksi Kantin"; }
                                                       if(subSub.equals("pengaturan_laporan")){ label = "Konfigurasi Laporan"; }
                                                       if(subSub.equals("pengadaan_pr")){ label = "Permintaan Pembelian (PR)"; }
                                                       if(subSub.equals("pengadaan_po")){ label = "Pemesanan Pembelian (PO)"; }
                                                       if(subSub.equals("pengadaan_bast")){ label = "Penerimaan Barang (BAST)"; }
                                                       if(subSub.equals("pengadaan_tagihan")){ label = "Terima Tagihan Vendor"; }
                                                       if(subSub.equals("pengadaan_dpc")){ label = "Pembayaran Vendor"; }
                                                       if(subSub.equals("pengadaan_bdp")){ label = "Barang Dalam Proses"; }
                                                       if(subSub.equals("pengadaan_bulk")){ label = "Bulk Entry Pengadaan"; }
                                        %>
                                                   <a class="mega-menu-item ms-3 <%= (p.equals("kantin") && s.equals(subSub)) ? "active" : "" %>" href="<%=ctx%>/baru?p=kantin&s=<%=subSub%>">
                                                       <i class="<%=MenuHelper.getIconForMenu(label)%> text-muted" style="font-size: 11px;"></i> <span><%=Common.getBahasaConfig(label) %></span>
                                                   </a>
                                        <%         }
                                               } else {
                                        %>
                                                 <a class="mega-menu-item <%= (p.equals("kantin") && s.equals(sub)) ? "active" : "" %>" href="<%=ctx%>/baru?p=kantin&s=<%=sub%>">
                                                    <i class="<%=MenuHelper.getIconForMenu(label)%>"></i> <span><%=Common.getBahasaConfig(label) %></span>
                                                 </a>
                                        <%     }
                                           } %>
                                    <% } else if(tbmuser.getPedagang() == null) { %>
                                        <h6 class="mega-menu-header mt-3"><%=Common.getBahasaConfig("Kantin") %></h6>
                                        <a class="mega-menu-item <%= (p.equals("kantin")) ? "active" : "" %>" href="<%=ctx%>/baru?p=kantin&s=beranda">
                                            <i class="fas fa-shopping-cart"></i> <span><%=Common.getBahasaConfig("Belanja Kantin") %></span>
                                        </a>
                                    <% } %>
                                </div>
                            </div>
                        </div>
                    </div>
                </li>

                <%-- MEGA MENU 2: MODUL APLIKASI (DARI DATABASE) --%>
                <% if(tbmrole != null && tbmrole.getRoleId() != null && !tbmrole.getRoleId().equalsIgnoreCase(Tbmrole.KANTIN) ) { %> 
                <li class="nav-item dropdown mega-dropdown">
                    <a class="nav-link dropdown-toggle fw-bold text-white" href="#" data-bs-toggle="dropdown">
                        <i class="fas fa-layer-group me-1"></i> <%=Common.getBahasaConfig("Modul Aplikasi") %>
                    </a>
                    
                    <div class="dropdown-menu mega-menu shadow-lg border-0">
                        <div class="container-fluid">
                            <div class="row">
                                <% out.print(MenuHelper.loadTree(tbmuser, request)); %>
                            </div>
                        </div>
                    </div>
                </li>
                <% } %>
                
            </ul>
        </div>
    </div>
</nav>
<% } %>
