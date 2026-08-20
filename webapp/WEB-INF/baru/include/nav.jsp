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
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%!
    private boolean bolehAksesMenuKantinPedagang(Tbmuser user, Tbmrole role, String menu) {
        if (role == null || menu == null) return true;
        // Role dasar Kantin lama menyimpan menu.pedagang=false. Admin kantin tidak terikat
        // ke satu Pedagang/Toko, sehingga harus tetap dapat membuat dan mengelola akun pedagang.
        // Role khusus serta akun pedagang biasa tetap mengikuti hak akses granular yang tersimpan.
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
        return true;
    }
%>

<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    
    if (tbmuser != null && tbmuser.getUserId() != null && tbmuser.getAktif()) {
        Tbmrole tbmrole = tbmuser.hakAkses();
        
        // Ambil Parameter dengan default value
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

        // Cek apakah menu dashboard harus terbuka (pilih)
        List<String> validPages = Arrays.asList(new String[]{
            "laporan/akademik", "home", "elearning", "prestasi", "pustaka", 
            "pagesmastersoppengajuananda", "akademik", "suratmenyurat", "pengadaan", 
            "pembayaran", "keuangan", "akuntansi", "kepegawaian", 
            "kinerja", "presensi", "kalenderakademik", "infokegiatan", "kantin","repository",
            "spmi", "koperasi", "gaji", "antarjemput", "feeder"
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
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/include/nav.jsp:47");
        } finally {
            // Blok finally untuk memastikan penutupan session atau pelepasan resource (jika ada pemanggilan koneksi database secara langsung).
        }
%>

<nav class="navbar navbar-light navbar-vertical navbar-expand-xl navbar-vibrant">
    <div class="d-flex align-items-center mb-3 mt-2">
        <div class="toggle-icon-wrapper ps-2">
            <button class="btn navbar-toggler-humburger-icon navbar-vertical-toggle" 
                    data-bs-toggle="tooltip" title="<%=Common.getBahasaConfig("Toggle Navigasi") %>">
                <span class="navbar-toggle-icon"><span class="toggle-line"></span></span>
            </button>
        </div>
    </div>

    <div class="collapse navbar-collapse" id="navbarVerticalCollapse">
        <div class="navbar-vertical-content scrollbar">
            <ul class="navbar-nav flex-column mb-3" id="navbarVerticalNav">
                
                <li class="nav-item">
                    <a class="nav-link dropdown-indicator <%=pilih ? "" : "collapsed" %>" 
                       href="#dashboard" role="button" data-bs-toggle="collapse" 
                       aria-expanded="<%=pilih%>">
                        <div class="d-flex align-items-center">
                            <span class="nav-link-icon"><span class="fas fa-th-large"></span></span>
                            <span class="nav-link-text ps-2 fw-medium"><%=Common.getBahasaConfig("Dashboard Utama") %></span>
                        </div>
                    </a>
                    
                    <ul class="nav collapse <%=pilih ? "show" : "" %>" id="dashboard">
                    
                    <% if(tbmrole != null && tbmrole.getHalamanUtama() == null && !kantinMemberLanding) { %>
                        <li class="nav-item">
                            <a class="nav-link <%=(p.isEmpty() || p.equals("home")) ? "active" : "" %>" href="<%=ctx%>/baru?p=home">
                                <span class="nav-link-text"><%=Common.getBahasaConfig("Beranda") %></span>
                            </a>
                        </li>
					<% } %>

                        <% if(tbmrole != null) { %>
                            
                            <% if(tbmrole.getElearning()) { %>
                            <li class="nav-item">
                                <a class="nav-link dropdown-indicator <%=p.equals("elearning") ? "" : "collapsed" %>" 
                                   href="#elearning_sub" role="button" data-bs-toggle="collapse">
                                    <span class="nav-link-text"><%=Common.getBahasaConfig("e-Learning") %></span>
                                </a>
                                <ul class="nav collapse <%=p.equals("elearning") ? "show" : "" %>" id="elearning_sub">
                                    <% 
                                       String[] subEL = pt ? new String[]{"dasbor","linimasa", "ringkasan", "ujian", "tugas", "tugas_kelompok", "materi", "audio", "video", "kalender","kurikulum_obe","portofolio_mahasiswa","pikobe"} : new String[]{"linimasa", "ringkasan", "ujian", "tugas", "tugas_kelompok", "materi", "audio", "video", "kalender"};
                                       for(String sub : subEL) {
                                           String label = sub.substring(0,1).toUpperCase() + sub.substring(1).replace("_", " ");
                                    %>
                                    <li class="nav-item">
                                        <a class="nav-link <%= (p.equals("elearning") && s.equals(sub)) ? "active" : "" %>" 
                                           href="<%=ctx%>/baru?p=elearning&s=<%=sub%>">
                                           <%=Common.getBahasaConfig(label) %>
                                        </a>
                                    </li>
                                    <% } %>
                                </ul>
                            </li>
                            <% } %>
                            
                            
                            <% if(tbmrole.getKegiatanDanPrestasi()) { %>
                            <li class="nav-item">
                                <a class="nav-link dropdown-indicator <%=p.equals("prestasi") ? "" : "collapsed" %>" 
                                   href="#prestasi_sub" role="button" data-bs-toggle="collapse">
                                    <span class="nav-link-text"><%=Common.getBahasaConfig("Prestasi") %></span>
                                </a>
                                <ul class="nav collapse <%=p.equals("prestasi") ? "show" : "" %>" id="prestasi_sub">
                                    <% 
                                       String[] subEL = {"prestasi", "kegiatan", "karya", "organisasi", "catatan"};
                                       for(String sub : subEL) {
                                    	// Pisahkan kata berdasarkan underscore (_)
                                           String[] kataKata = sub.split("_");
                                           StringBuilder hasilAkhir = new StringBuilder();

                                           for (String kata : kataKata) {
                                               if (kata.length() > 0) {
                                                   // Ubah huruf pertama jadi kapital, sisanya huruf kecil
                                                   String hurufPertama = kata.substring(0, 1).toUpperCase();
                                                   String sisaHuruf = kata.substring(1).toLowerCase();
                                                   
                                                   // Gabungkan dan tambahkan spasi
                                                   hasilAkhir.append(hurufPertama).append(sisaHuruf).append(" ");
                                               }
                                           }

                                           // Hapus spasi berlebih di akhir string dengan trim()
                                           String label = hasilAkhir.toString().trim();
                                    %>
                                    <li class="nav-item">
                                        <a class="nav-link <%= (p.equals("prestasi") && s.equals(sub)) ? "active" : "" %>" 
                                           href="<%=ctx%>/baru?p=prestasi&s=<%=sub%>">
                                           <%=Common.getBahasaConfig(label) %>
                                        </a>
                                    </li>
                                    <% } %>
                                </ul>
                            </li>
                            <% } %>
                            
                            
                             <% if(tbmrole.getPustaka()) { %>
                            <li class="nav-item">
                                <a class="nav-link dropdown-indicator <%=p.equals("pustaka") ? "" : "collapsed" %>" 
                                   href="#pustaka_sub" role="button" data-bs-toggle="collapse">
                                    <span class="nav-link-text"><%=Common.getBahasaConfig("Pustaka") %></span>
                                </a>
                                <ul class="nav collapse <%=p.equals("pustaka") ? "show" : "" %>" id="pustaka_sub">
                                    <% 
                                       String[] subEL = {"katalog", "populer", "sirkulasi", "kunjungan", "dashboard", "dashboard", "beranda_anggota"};
                                       for(String sub : subEL) {
                                    	// Pisahkan kata berdasarkan underscore (_)
                                           String[] kataKata = sub.split("_");
                                           StringBuilder hasilAkhir = new StringBuilder();

                                           for (String kata : kataKata) {
                                               if (kata.length() > 0) {
                                                   // Ubah huruf pertama jadi kapital, sisanya huruf kecil
                                                   String hurufPertama = kata.substring(0, 1).toUpperCase();
                                                   String sisaHuruf = kata.substring(1).toLowerCase();
                                                   
                                                   // Gabungkan dan tambahkan spasi
                                                   hasilAkhir.append(hurufPertama).append(sisaHuruf).append(" ");
                                               }
                                           }

                                           // Hapus spasi berlebih di akhir string dengan trim()
                                           String label = hasilAkhir.toString().trim();
                                    %>
                                    <li class="nav-item">
                                        <a class="nav-link <%= (p.equals("pustaka") && s.equals(sub)) ? "active" : "" %>" 
                                           href="<%=ctx%>/baru?p=pustaka&s=<%=sub%>">
                                           <%=Common.getBahasaConfig(label) %>
                                        </a>
                                    </li>
                                    <% } %>
                                </ul>
                            </li>
                            <% } %>

                            <%-- Mapping Menu Otomatis dengan pemeriksaan Role --%>
                            <% 
                                String[][] menus = {
                                    {"pagesmastersoppengajuananda", "getWorkflow", "Pengajuan Anda"},
                                    {"akademik", "getDashboard", "Akademik"},
                                    {"suratmenyurat", "getAdministrasi", "Surat Menyurat"},
                                    {"pengadaan", "getPengadaan", "Pengadaan"},
                                    {"pembayaran", "getPembayaran", "Pembayaran"},
                                    {"keuangan", "getKeuangan", "Keuangan"},
                                    {"akuntansi", "getAkunting", "Akuntansi"},
                                    {"kepegawaian", "getKepegawaian", "Kepegawaian"},
                                    {"kinerja", "getKinerja", "Kinerja"},
                                    {"presensi", "getPresensiKehadiran", "Presensi"},
                                    {"kalenderakademik", "getKalenderAkademik", "Kalender Akademik"},
                                    {"infokegiatan", "getInfoKegiatan", "Info Kegiatan"},
                                    {"repository", "getRepository", "Repository"},
                                    {"spmi", "getTampilkanSpmi", "SPMI"},
                                    {"koperasi", "getDashboardKoperasi", "Koperasi"},
                                    {"gaji", "getTampilkanGaji", "Gaji"},
                                    {"antarjemput", "getDasboardAntarJemput", "Antar Jemput"}
                                };

                                for(String[] menu : menus) {
                                    // Menggunakan reflection sederhana atau manual check untuk Java 1.7
                                    boolean hasAccess = false;
                                    if(menu[1].equals("getKegiatanDanPrestasi")) hasAccess = tbmrole.getKegiatanDanPrestasi();
                                    else if(menu[1].equals("getPustaka")) hasAccess = tbmrole.getPustaka();
                                    else if(menu[1].equals("getWorkflow")) hasAccess = tbmrole.getWorkflow(); // "Pengajuan Anda" (Dashboard Workflow/SOP) HANYA tampil bila flag "Tampilkan Dashboard Workflow / SOP" diaktifkan pada grup pengguna
                                    else if(menu[1].equals("getDashboard")) hasAccess = tbmrole.getDashboard();
                                    else if(menu[1].equals("getAdministrasi")) hasAccess = tbmrole.getAdministrasi();
                                    else if(menu[1].equals("getPengadaan")) hasAccess = tbmrole.getPengadaan();
                                    else if(menu[1].equals("getPembayaran")) hasAccess = tbmrole.getPembayaran();
                                    else if(menu[1].equals("getKeuangan")) hasAccess = tbmrole.getKeuangan();
                                    else if(menu[1].equals("getAkunting")) hasAccess = tbmrole.getAkunting();
                                    else if(menu[1].equals("getKepegawaian")) hasAccess = tbmrole.getKepegawaian();
                                    else if(menu[1].equals("getKinerja")) hasAccess = tbmrole.getKinerja();
                                    else if(menu[1].equals("getPresensiKehadiran")) hasAccess = tbmrole.getPresensiKehadiran();
                                    else if(menu[1].equals("getKalenderAkademik")) hasAccess = tbmrole.getKalenderAkademik();
                                    else if(menu[1].equals("getInfoKegiatan")) hasAccess = tbmrole.getInfoKegiatan();
                                    else if(menu[1].equals("getRepository")) hasAccess = tbmrole.getDasborRepository();
                                    else if(menu[1].equals("getTampilkanSpmi")) hasAccess = tbmrole.getTampilkanSpmi();
                                    else if(menu[1].equals("getDashboardKoperasi")) hasAccess = tbmrole.getDashboardKoperasi();
                                    else if(menu[1].equals("getTampilkanGaji")) hasAccess = tbmrole.getTampilkanGaji();
                                    else if(menu[1].equals("getDasboardAntarJemput")) hasAccess = tbmrole.getDasboardAntarJemput();

                                    if(hasAccess) {
                            %>
                                    <li class="nav-item">
                                        <a class="nav-link <%=p.equals(menu[0]) ? "active" : "" %>" href="<%=ctx%>/baru?p=<%=menu[0]%>">
                                            <span class="nav-link-text"><%=Common.getBahasaConfig(menu[2]) %></span>
                                        </a>
                                    </li>
                            <% 
                                    }
                                } 
                            %>
                            
                            <% if(ais.common.Common.getApakahAdminBolehAksesFeeder()) { %>
                            <li class="nav-item">
                                <a class="nav-link <%=p.equals("feeder") ? "active" : "" %>" href="<%=ctx%>/baru?p=feeder">
                                    <span class="nav-link-text"><%=Common.getBahasaConfig("Neo Feeder") %></span>
                                </a>
                            </li>
                            <% } %>

                            <% if(tbmrole.getKantin()) { %>
                            <li class="nav-item">
                                <a class="nav-link dropdown-indicator <%=p.equals("kantin") || tbmrole.getRoleId().equalsIgnoreCase(Tbmrole.KANTIN) ? "" : "collapsed" %>" 
                                   href="#kantin_sub" role="button" data-bs-toggle="collapse">
                                    <span class="nav-link-text"><%=Common.getBahasaConfig("e-Kantin") %></span>
                                </a>
                                <ul class="nav collapse <%=p.equals("kantin") || tbmrole.getRoleId().equalsIgnoreCase(Tbmrole.KANTIN)  ? "show" : "" %>" id="kantin_sub">
                                    <% 
                                       String[] subEL = tbmuser.getPedagang() == null && tbmrole.getRoleId() != null && !tbmrole.getRoleId().equals(Tbmrole.KANTIN) ? new String[]{"beranda", "ringkasan", "pos", "pesanan", "pengaturan", "laporan_laporan", "laporan_keuangan"} : new String[]{"ringkasan", "pos", "pesanan", "pengaturan", "laporan_laporan", "laporan_keuangan"};
                                       String[] subSubEL = (isAdminKantin ? new String[]{"anggota", "barang", "kulakan","retur_penjualan","diskon","pembayaran","pedagang","stok","stok_expired","meja","penyedia","kas","tenant","opname","limit_kredit","mutasi_rekening","produksi","pengaturan_laporan"} : new String[]{ "barang", "kulakan","retur_penjualan","diskon","pembayaran","pedagang","stok","stok_expired","meja","penyedia","kas","tenant","opname","limit_kredit","mutasi_rekening","produksi"});
                                       for(String sub : subEL) {
                                           if(!Common.bolehKonfigurasi("kantin_menu_"+sub)){ continue; } // on/off menu via Konfigurasi (default ON)
                                           if(!bolehAksesMenuKantinPedagang(tbmuser, tbmrole, sub)){ continue; }
                                           String label = sub.substring(0,1).toUpperCase() + sub.substring(1).replace("_", " ");
                                           if(sub.equals("laporan_laporan")){ label = "Laporan-Laporan"; }
                                           if(sub.equals("laporan_keuangan")){ label = "Laporan Keuangan"; }
                                           if(label.equalsIgnoreCase("pengaturan")){
                                        	   Boolean subCollapse = false;
                                        	   for(String subSub : subSubEL) {
                                               if(subSub.equalsIgnoreCase(s) && bolehAksesMenuKantinPedagang(tbmuser, tbmrole, subSub)){
                                        			   subCollapse = true;
                                        			   break;
                                        		   } 
                                        	   }
                                        	   %>
                                               <li class="nav-item">
                                                   <a class="nav-link dropdown-indicator <%= (p.equals("kantin") && subCollapse) ? "" : "collapsed" %>" 
                                                      href="#kantin_sub_sub" role="button" data-bs-toggle="collapse">
                                                      <span class="nav-link-text"><%=Common.getBahasaConfig(label) %></span>
                                                   </a>
                                                   
                                                   <ul class="nav collapse <%=p.equals("kantin") && subCollapse  ? "show" : "" %>" id="kantin_sub_sub">
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
                                                	   %>
                                                       <li class="nav-item">
                                                           <a class="nav-link <%= (p.equals("kantin") && s.equals(subSub)) ? "active" : "" %>" 
                                                              href="<%=ctx%>/baru?p=kantin&s=<%=subSub%>">
                                                              <%=Common.getBahasaConfig(label) %>
                                                           </a>
                                                       </li>
                                                       <% 	
                                                   }
                                                   
                                                   %>
                                                   </ul>
                                               </li>
                                               <% 		
                                        	   
                                           } else {
                                    %>
                                    <li class="nav-item">
                                        <a class="nav-link <%= (p.equals("kantin") && s.equals(sub)) ? "active" : "" %>" 
                                           href="<%=ctx%>/baru?p=kantin&s=<%=sub%>">
                                           <%=Common.getBahasaConfig(label) %>
                                        </a>
                                    </li>
                                    <% 			}
                                           }%>
                                           
                                           
                                    
                                </ul>
                            </li>
                            
                             <% out.print(CommonMenu.loadTreeDynamicReport(tbmuser, request, "laporan")); %> 
                            
                            <% } else if(tbmuser.getPedagang() == null) { %>
                            <li class="nav-item">
                                  <a class="nav-link <%= (p.equals("kantin")) ? "active" : "" %>" 
                                                              href="<%=ctx%>/baru?p=kantin&s=beranda">
                                                              <%=Common.getBahasaConfig("Belanja Kantin") %>
                                  </a>
                            </li>
                            
                        <% }
                        } // end if tbmrole != null %>
                    </ul>
                </li>

				<% if(tbmrole != null && tbmrole.getRoleId() != null && !tbmrole.getRoleId().equalsIgnoreCase(Tbmrole.KANTIN) ) { %> 
                <li class="nav-item">
                    <div class="row navbar-vertical-label-wrapper mt-4 mb-2">
                        <div class="col-auto text-white navbar-vertical-label text-uppercase fw-bold small">
                            <%=Common.getBahasaConfig("Modul Aplikasi") %>
                        </div>
                        <div class="col ps-0"><hr class="mb-0 navbar-vertical-divider" /></div>
                    </div>
                    
                    <div class="custom-tree-container">
                        <% out.print(CommonMenu.loadTree(tbmuser, request)); %>
                    </div>
                </li>
  				<% } %>
            </ul>
        </div>
    </div>
</nav>
<%
    }
%>
