<%@page import="java.util.TreeSet"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="ais.action.master.helper.MainHelper"%>
<%@page import="ais.database.model.file.FileFotoLain"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="java.util.HashSet"%>
<%@page import="ais.database.model.Menu"%>
<%@page import="java.util.Set"%>
<%@page import="ais.database.model.Tbmrole"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.sendRedirect(request.getContextPath() + "/logoff");
	return;
}
Tbmrole tbmrole;

Set<Menu> menus = new TreeSet<Menu>();
/*
try {
	tbmrole = tbmuser.hakAkses();
	menus = tbmrole.getMenus();
} catch (Exception e) {
	Session session2 = HibernateUtil.currentNativeSession();
	session2.refresh(tbmuser);

	tbmrole = tbmuser.hakAkses();
	session2.refresh(tbmrole);
	menus = tbmrole.getMenus();
	session2.disconnect();
	session2.close();
	HibernateUtil.closeSession();
}


request.setAttribute("tbmrole", tbmrole);
*/

Menu menu = new Menu();
menu.setAktif(true);
menu.setLabel("Akademik");
menu.setRoot(0L);
menu.setChild(1L);
menus.add(menu);

if (tbmuser != null && tbmuser.getMahasiswa() != null) {

	menu = new Menu();
	menu.setAktif(true);
	menu.setLabel("Perkuliahan");
	menu.setRoot(1L);
	menu.setChild(100L);
	menus.add(menu);

	menu = new Menu();
	menu.setAktif(true);
	menu.setLabel("Bimbingan Skripsi");
	menu.setRoot(1L);
	menu.setChild(101L);
	menus.add(menu);

	menu = new Menu();
	menu.setAktif(true);
	menu.setLabel("Sidang Skripsi");
	menu.setRoot(1L);
	menu.setChild(102L);
	menus.add(menu);

	menu = new Menu();
	menu.setAktif(true);
	menu.setLabel("KKN");
	menu.setRoot(1L);
	menu.setChild(103L);
	menus.add(menu);

	menu = new Menu();
	menu.setAktif(true);
	menu.setLabel("PKL");
	menu.setRoot(1L);
	menu.setChild(104L);
	menus.add(menu);

	menu = new Menu();
	menu.setAktif(true);
	menu.setLabel("Pembimbing Akademik");
	menu.setRoot(1L);
	menu.setChild(105L);
	menus.add(menu);

	menu = new Menu();
	menu.setAktif(true);
	menu.setLabel("Kegiatan Lain");
	menu.setRoot(1L);
	menu.setChild(106L);
	menus.add(menu);

	menu = new Menu();
	menu.setAktif(true);
	menu.setLabel("Wisuda");
	menu.setRoot(1L);
	menu.setChild(107L);
	menus.add(menu);
	
	
	menu = new Menu();
	menu.setAktif(true);
	menu.setLabel("KRS Mahasiswa");
	menu.setUrl(request.getContextPath()+ "/WEB-INF/o/ux/krs.jsp");
	menu.setRoot(0L);
	menu.setChild(2L);
	menus.add(menu);
	
	menu = new Menu();
	menu.setAktif(true);
	menu.setLabel("Nilai Mahasiswa");
	menu.setUrl(request.getContextPath()+ "/WEB-INF/o/ux/nilai.jsp");
	menu.setRoot(0L);
	menu.setChild(3L);
	menus.add(menu);
	
	menu = new Menu();
	menu.setAktif(true);
	menu.setLabel("Kehadiran Mahasiswa");
	menu.setUrl(request.getContextPath()+ "/WEB-INF/o/ux/absen.jsp");
	menu.setRoot(0L);
	menu.setChild(3L);
	menus.add(menu);
	
	menu = new Menu();
	menu.setAktif(true);
	menu.setLabel("Keuangan");
	menu.setRoot(0L);
	menu.setChild(4L);
	menus.add(menu);
	
	menu = new Menu();
	menu.setAktif(true);
	menu.setLabel("Daftar Tagihan");
	menu.setUrl(request.getContextPath()+ "/WEB-INF/o/ux/keuangan/tagihan.jsp");
	menu.setRoot(4L);
	menu.setChild(400L);
	menus.add(menu);
	
	menu = new Menu();
	menu.setAktif(true);
	menu.setLabel("Riwayat Pembayaran");
	menu.setUrl(request.getContextPath()+ "/WEB-INF/o/ux/keuangan/riwayat_pembayaran.jsp");
	menu.setRoot(4L);
	menu.setChild(401L);
	menus.add(menu);
	
	menu = new Menu();
	menu.setAktif(true);
	menu.setLabel("Bukti Pembayaran");
	menu.setUrl(request.getContextPath()+ "/WEB-INF/o/ux/keuangan/bukti_pembayaran.jsp");
	menu.setRoot(4L);
	menu.setChild(402L);
	menus.add(menu);
}

request.setAttribute("menus", menus);
request.setAttribute("menu", null);
%>
<!--begin::sidebar menu-->
<div class="app-sidebar-menu overflow-hidden flex-column-fluid">
	<!--begin::Menu wrapper-->
	<div id="kt_app_sidebar_menu_wrapper"
		class="app-sidebar-wrapper hover-scroll-overlay-y my-5"
		data-kt-scroll="true" data-kt-scroll-activate="true"
		data-kt-scroll-height="auto"
		data-kt-scroll-dependencies="#kt_app_sidebar_logo, #kt_app_sidebar_footer"
		data-kt-scroll-wrappers="#kt_app_sidebar_menu"
		data-kt-scroll-offset="5px" data-kt-scroll-save-state="true">
		<!--------------------------------------------------------- Menu Mulai-->
		<!--begin::Menu-->
		<div class="menu menu-column menu-rounded menu-sub-indention px-3"
			id="#kt_app_sidebar_menu" data-kt-menu="true"
			data-kt-menu-expand="false">


			<jsp:include page="/WEB-INF/o/ux/content/common/menu.jsp">
				<jsp:param value="${menus}" name="menus" />
				<jsp:param value="${menu}" name="menu" />
			</jsp:include>


		</div>
		<!--end::Menu-->
		<!-------------------------------------------------- Menu Selesai -->
	</div>
	<!--end::Menu wrapper-->
</div>
<!--end::sidebar menu-->