<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.KrsMahasiswa"%>
<%@page import="ais.database.model.StatusAwalMahasiswa"%>
<%@page import="org.json.JSONArray"%>
<%@page import="java.util.TreeSet"%>
<%@page import="java.util.TreeMap"%>
<%@page import="java.util.ArrayList"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="ais.database.model.Kurikulum"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="java.util.List"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="java.util.Map"%>

<%
String fak = request.getParameter("fakultas") == null ? "-1" : request.getParameter("fakultas");
String jur = request.getParameter("prodi") == null ? "-1" : request.getParameter("prodi");

Fakultas fakultasData = fak.equals("-1") ? null
		: (Fakultas) ConstantValues.ambil(Fakultas.class.getName(), Long.parseLong(fak));
Jurusan jurusanData = jur.equals("-1") ? null
		: (Jurusan) ConstantValues.ambil(Jurusan.class.getName(), Long.parseLong(jur));

String ta = Common.getCurrentTahunAkademik();
boolean ganjil = Common.isNowSemensterGanjil();
String idsmt = request.getParameter("idsmt");
Integer angkatansd = Integer.parseInt(request.getParameter("angkatansd"));
Integer angkatan = Integer.parseInt(request.getParameter("angkatan"));

try {
	if (idsmt == null) {
		idsmt = ta.substring(0, 4) + (ganjil ? "1" : "2");
	}
} catch (Exception e) {
	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/o/ux/dashboard/service/max_ipk_mahasiswa.jsp:41");
}

if (idsmt != null) {
	ta = (Integer.parseInt(idsmt.substring(0, 4))) + "/" + (Integer.parseInt(idsmt.substring(0, 4)) + 1);
	ganjil = Integer.parseInt(idsmt.substring(4, 5)) == 1;
}

Session mySession = HibernateUtil.currentNativeSession();

List<Jurusan> jurusans = ConstantValues.simpleList(mySession.createCriteria(Jurusan.class)
		.add(jurusanData == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("id", jurusanData.getId()))
		.add(fakultasData == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("fakultas", fakultasData))
		.addOrder(Order.asc("fakultas"))
		.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))), Jurusan.class);
TreeSet<String> categories = new TreeSet<String>();
TreeMap<String, List<Double>> seriesData = new TreeMap<String, List<Double>>();

String tr = "<thead class=\"bg-dark text-white\"><tr>";
tr += "<th>Prodi</th>";
for (int tahun = angkatan; tahun <= angkatansd; tahun++) {
	tr += "<th>" + tahun + "</th>";
}
tr += "<th>TOTAL</th>";
tr += "</tr></thead><tbody>";

for (Jurusan jurusan : jurusans) {
	tr += "<tr>" + "<td>" + (jurusan == null ? "" : jurusan.getNama()) + "</td>";
	int total = 0;
	for (int tahun = angkatan; tahun <= angkatansd; tahun++) {
		try {
	Number dataIpk = 0.0;

	dataIpk = (Number) mySession.createCriteria(KrsMahasiswa.class).createAlias("mahasiswa", "mahasiswa")
			.setProjection(Projections.max("ipk")).add(Restrictions.eq("tahunAkademik", ta))
			.add(Restrictions.gt("ipk", 0.1))
			.add(Restrictions.eq("mahasiswa.tahunangkatan", tahun))
			.add(Restrictions.sqlRestriction("semester%2=" + (ganjil ? 1 : 0) + ""))
			.add(Restrictions.isNull("semesterPendek"))

			.add(Restrictions.or(Restrictions.isNull("mahasiswa.aktif"),
					Restrictions.eq("mahasiswa.aktif", true)))

			.add(Restrictions.eq("mahasiswa.jurusan", jurusan)).uniqueResult();

	if (dataIpk == null) {
		dataIpk = 0.0;
	}

	tr += "<td>" + Common.numberFormat.get().format(dataIpk.doubleValue()) + "</td>";

	total += dataIpk.doubleValue();

	List<Double> datas = seriesData.get(jurusan.getNama());
	if (datas == null) {
		datas = new ArrayList<Double>();
	}
	datas.add(Common.numberFormat.get().parse(Common.numberFormat.get().format(dataIpk.doubleValue())).doubleValue());
	seriesData.put(jurusan.getNama(), datas);

	categories.add(tahun + "");

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/o/ux/dashboard/service/max_ipk_mahasiswa.jsp:103");

		}
	}

	tr += "<td>" + total + "</td>";

	tr += "</tr>";
}

tr += "</tbody>";

// mySession.disconnect();
if (mySession.isOpen()) {mySession.disconnect();mySession.close();}
HibernateUtil.closeSession();

JSONObject jsonObject = new JSONObject();

JSONArray series = new JSONArray();

for (String key : seriesData.keySet()) {
	JSONObject a = new JSONObject();
	a.put("name", key);
	a.put("data", seriesData.get(key));
	series.put(a);
}
jsonObject.put("series", series);
jsonObject.put("categories", categories);
jsonObject.put("tr", tr);

out.println(jsonObject.toString());
%>