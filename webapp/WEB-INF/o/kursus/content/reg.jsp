
<%@page import="ais.database.model.sekolah.Yayasan"%>
<%@page import="ais.action.master.sekolah.util.SekolahUtil"%>
<%@page import="ais.database.model.sekolah.Sekolah"%>
<%@page import="ais.database.model.kursus.PesertaKursus"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="java.util.Calendar"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%
PerguruanTinggi perguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request);
String pesan = "";
boolean error = false;

try {

	if (request.getParameter("simpan") != null && request.getParameter("simpan").equals("simpan")
	&& request.getParameter("email") != null && request.getParameter("telp") != null) {

		String em = request.getParameter("email").trim();
		String telp = request.getParameter("telp").trim();

		Tbmuser tbmuser = (Tbmuser) ConstantValues.ambil(Tbmuser.class.getName(), em);

		if (tbmuser == null || tbmuser.getUserId() == null) {

	for (Object o : ConstantValues.ambilBerdasarClass(Tbmuser.class).values()) {
		Tbmuser t = (Tbmuser) o;
		if (t != null && t.getEmail() != null && t.getEmail().toLowerCase().contains(em.toLowerCase())) {
			tbmuser = t;
			break;
		}
	}

		}

		if (tbmuser != null) {
	pesan = "Email \"" + em + "\" telah terdaftar";
	error = true;
		} else {

	if (!request.getParameter("password1").equals(request.getParameter("password2"))) {

		pesan = "Email \"" + em + "\" tidak sama \"" + request.getParameter("password2") + "\"";
		error = true;
	} else {

		Sekolah sekolah = SekolahUtil.getSekolah(request);
		Yayasan yayasan = SekolahUtil.getYayasan(request);

		Session mySession = HibernateUtil.currentNativeSession();

		tbmuser = new Tbmuser();
		tbmuser.setUserId(em);
		tbmuser.setEmail(em);
		tbmuser.setUserNama(request.getParameter("namaLengkap"));
		tbmuser.setIs_encripted(true);
		tbmuser.setUserPassword(Common.desEncrypter.get().encrypt(request.getParameter("password1")));
		tbmuser.setUserRole(ConstantValues.rolePesertaKursusPerpustakaan);
		tbmuser.setHp(telp);
		tbmuser.setSekolah(sekolah.getId()==null?null:sekolah);
		tbmuser.setYayasan(yayasan.getId()==null?null:yayasan);
		tbmuser.setAktif(true);

		mySession.getTransaction().begin();
		mySession.save(tbmuser);
		mySession.getTransaction().commit();

		PesertaKursus pesertaKursus = (PesertaKursus) mySession.createCriteria(PesertaKursus.class)
				.add(Restrictions.eq("tbmuser", tbmuser)).setMaxResults(1).uniqueResult();
		if (pesertaKursus == null) {
			pesertaKursus = new PesertaKursus();
			pesertaKursus.setTbmuser(tbmuser);
			pesertaKursus.setNama(request.getParameter("namaLengkap"));
			pesertaKursus.setEmail(em);
			pesertaKursus.setTelp(telp);

			mySession.getTransaction().begin();
			mySession.save(pesertaKursus);
			mySession.getTransaction().commit();
		}

		// mySession.disconnect();
		if (mySession.isOpen()) {mySession.disconnect();mySession.close();}

		HibernateUtil.closeSession();

		pesan = "Pengguna berhasil ditambahkan";
		error = false;

	}
		}
	}

} catch (Exception e) {
	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/o/kursus/content/reg.jsp:100");
}
%>

<nav id="menu" class="fake_menu"></nav>

<div id="preloader">
	<div data-loader="circle-side"></div>
</div>
<!-- End Preload -->

<div id="login">
	<aside>
		<figure>
			<a href="<%=request.getContextPath()%>/kursus"><img
				src="<%=request.getContextPath()%>/img/logo.png" width="auto"
				height="42" data-retina="true" alt=""></a>

			

		</figure>
		
		
		<%
			if (!pesan.isEmpty()) {
			%>
			<font color="red"><%=pesan%></font>
			<br>
			<strong><a
				href="<%=request.getContextPath()%>/kursus/login.jsp">Login</a></strong>
			<%
			}
			%>

		<%
		if (pesan.isEmpty() || error) {
		%>

		<form autocomplete="off" action="" method="post">
			<input type="hidden" value="simpan" name="simpan">
			<div class="form-group">

				<span class="input"> <input class="input_field" type="text"
					value="<%=(request.getParameter("namaLengkap") == null ? "" : request.getParameter("namaLengkap"))%>"
					name="namaLengkap"> <label class="input_label"> <span
						class="input__label-content">Nama Lengkap</span>
				</label>
				</span> <span class="input"> <input class="input_field" type="email"
					value="<%=(request.getParameter("email") == null ? "" : request.getParameter("email"))%>"
					name="email"> <label class="input_label"> <span
						class="input__label-content">Email</span>
				</label>
				</span> <span class="input"> <input class="input_field" type="text"
					value="<%=(request.getParameter("telp") == null ? "" : request.getParameter("telp"))%>"
					name="telp"> <label class="input_label"> <span
						class="input__label-content">HP / WA</span>
				</label>
				</span> <span class="input"> <input class="input_field"
					value="<%=(request.getParameter("password1") == null ? "" : request.getParameter("password1"))%>"
					type="password" id="password1" name="password1"> <label
					class="input_label"> <span class="input__label-content">Password</span>
				</label>
				</span> <span class="input"> <input class="input_field"
					value="<%=(request.getParameter("password2") == null ? "" : request.getParameter("password2"))%>"
					type="password" id="password2" name="password2"> <label
					class="input_label"> <span class="input__label-content">Ulangi
							password</span>
				</label>
				</span>

				<div id="pass-info" class="clearfix"></div>
			</div>
			<button type="submit" id="kt_sign_in_submit"
				class="btn_1 rounded full-width add_top_60">Daftar</button>
			<div class="text-center add_top_10">
				Anda telah terdaftar sebelumnya? <strong><a
					href="<%=request.getContextPath()%>/kursus/login.jsp">Login</a></strong>
			</div>
		</form>
		<%
		}
		%>
		<div class="copy">
			�
			<%=Calendar.getInstance().get(Calendar.YEAR)%>
			<%=perguruanTinggi.getNama()%></div>
	</aside>
</div>
<!-- /login -->