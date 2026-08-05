<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="ais.database.model.sekolah.CalonSiswa"%>
<%@page import="ais.database.model.sekolah.Sekolah"%>
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
<%@page import="ais.common.ConstantValues"%>
<%@page import="java.util.Map"%>

<%
String fak = request.getParameter("sekolah") == null ? "-1" : request.getParameter("sekolah");

Sekolah sekolahData = fak.equals("-1") ? null
		: (Sekolah) ConstantValues.ambil(Sekolah.class.getName(), Long.parseLong(fak));

Integer angkatansd = Integer.parseInt(request.getParameter("angkatansd"));
Integer angkatan = Integer.parseInt(request.getParameter("angkatan"));

Session mySession = HibernateUtil.currentNativeSession();

List<Sekolah> sekolahs = ConstantValues
		.simpleList(
		mySession.createCriteria(Sekolah.class)
				.add(sekolahData == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("id", sekolahData.getId()))
				.addOrder(Order.asc("nama"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
		Sekolah.class);
TreeSet<String> categories = new TreeSet<String>();
TreeMap<String, List<Integer>> seriesData = new TreeMap<String, List<Integer>>();

String tr = "<thead class=\"bg-dark text-white\"><tr>";
tr += "<th>Sekolah</th>";
for (int tahun = angkatan; tahun <= angkatansd; tahun++) {
	tr += "<th>" + tahun + "</th>";
}
tr += "<th>TOTAL</th>";
tr += "</tr></thead><tbody>";

for (Sekolah sekolah : sekolahs) {
	tr += "<tr>" + "<td>" + (sekolah == null ? "" : sekolah.getNama()) + "</td>";
	int total = 0;
	for (int tahun = angkatan; tahun <= angkatansd; tahun++) {
		Integer count = ((Number) mySession.createCriteria(Siswa.class)
		.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
		.setProjection(Projections.rowCount()).add(Restrictions.eq("sekolah", sekolah))
		.add(Restrictions.eq("tahunMasuk", tahun)).uniqueResult()).intValue();

		tr += "<td>" + count + "</td>";

		total += count;

		List<Integer> datas = seriesData.get(sekolah.getNama());
		if (datas == null) {
	datas = new ArrayList<Integer>();
		}
		datas.add(count);
		seriesData.put(sekolah.getNama(), datas);

		categories.add(tahun + "");
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