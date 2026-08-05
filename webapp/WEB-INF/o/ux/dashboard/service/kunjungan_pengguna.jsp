<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="org.json.JSONObject"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="java.util.Date"%>
<%@page import="org.jfree.data.time.RegularTimePeriod"%>
<%@page import="org.jfree.data.time.TimePeriod"%>
<%@page import="org.jfree.data.time.Day"%>
<%@page import="org.jfree.data.time.TimeSeries"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page
	import="ais.action.master.dashboard.admin.DashboardStatistikKunjunganPengguna"%>
<%@page import="org.jfree.data.time.TimeSeriesCollection"%>
<%
String dateMulai = request.getParameter("tanggal_awal");
String dateSelesai = request.getParameter("tanggal_akhir");
boolean mhs = true;
boolean dsn = true;
boolean adm = true;

String fak = request.getParameter("fakultas") == null ? "-1" : request.getParameter("fakultas");
String jur = request.getParameter("prodi") == null ? "-1" : request.getParameter("prodi");
Fakultas fakultasData = fak.equals("-1") ? null
		: (Fakultas) ConstantValues.ambil(Fakultas.class.getName(), Long.parseLong(fak));
Jurusan jurusanData = jur.equals("-1") ? null
		: (Jurusan) ConstantValues.ambil(Jurusan.class.getName(), Long.parseLong(jur));
TimeSeriesCollection timeSeriesCollection = DashboardStatistikKunjunganPengguna.generateDataset(fakultasData,
		jurusanData, dateMulai, dateSelesai, mhs, dsn, adm);

TimeSeries mhsData = timeSeriesCollection.getSeries(0);
TimeSeries dsnData = timeSeriesCollection.getSeries(1);
TimeSeries admData = timeSeriesCollection.getSeries(2);

List<String> dtDate = new ArrayList<String>();
List<Number> dtAdm = new ArrayList<Number>();
for (int i = 0; i < admData.getItemCount(); i++) {
	RegularTimePeriod day = mhsData.getTimePeriod(i);
	Date d = day.getStart();
	Number nilai = admData.getValue(i);

	dtDate.add(Common.simpleDateFormat2.get().format(d));
	dtAdm.add(nilai);
}

List<Number> dtMhs = new ArrayList<Number>();
for (int i = 0; i < mhsData.getItemCount(); i++) {
	Number nilai = mhsData.getValue(i);
	dtMhs.add(nilai);
}

List<Number> dtDsn = new ArrayList<Number>();
for (int i = 0; i < dsnData.getItemCount(); i++) {
	Number nilai = dsnData.getValue(i);
	dtDsn.add(nilai);
}
JSONObject jsonObject = new JSONObject();
jsonObject.put("dtAdm", dtAdm);
jsonObject.put("dtMhs", dtMhs);
jsonObject.put("dtDsn", dtDsn);
jsonObject.put("dtDate", dtDate);
out.println(jsonObject.toString());
%>