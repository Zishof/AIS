<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.BiodataCalonMahasiswa"%>
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

Integer angkatansd = Integer.parseInt(request.getParameter("angkatansd"));
Integer angkatan = Integer.parseInt(request.getParameter("angkatan"));

Session mySession = HibernateUtil.currentNativeSession();

List<Jurusan> jurusans = ConstantValues.simpleList(mySession.createCriteria(Jurusan.class)
		.add(jurusanData == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("id", jurusanData.getId()))
		.add(fakultasData == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("fakultas", fakultasData))
		.addOrder(Order.asc("fakultas"))
		.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))), Jurusan.class);
TreeSet<String> categories = new TreeSet<String>();
TreeMap<String, List<Integer>> seriesData = new TreeMap<String, List<Integer>>();

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
		Integer count = ((Number) mySession.createCriteria(Mahasiswa.class)
		.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
		.setProjection(Projections.rowCount()).add(Restrictions.eq("jurusan", jurusan))
		.add(Restrictions.eq("tahunangkatan", tahun)).uniqueResult()).intValue();

		tr += "<td>" + count + "</td>";

		total += count;

		List<Integer> datas = seriesData.get(jurusan.getNama());
		if (datas == null) {
	datas = new ArrayList<Integer>();
		}
		datas.add(count);
		seriesData.put(jurusan.getNama(), datas);

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