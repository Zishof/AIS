<%@page import="ais.database.model.file.LampiranLain"%>
<%@page import="ais.database.model.VOPembelajaran"%>
<%@page import="java.util.List"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.HashMap"%>
<%@page import="ais.database.model.PengajuanIzinTidakMasukPerkuliahan"%>
<%@page import="java.util.HashSet"%>
<%@page import="java.util.Set"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.DynamicFormGenerator"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.Pertemuan"%>
<%!
	// Java 1.7-compatible join (no Stream API) — returns "-111" sentinel when empty
	private String joinIds(List<Long> ids) {
		if (ids == null || ids.isEmpty()) return "-111";
		StringBuilder sb = new StringBuilder();
		for (Long id : ids) {
			if (sb.length() > 0) sb.append(",");
			sb.append(id);
		}
		return sb.length() == 0 ? "-111" : sb.toString();
	}
%>
<%
try {
	Tbmuser tbmuser = Common.getCurrentUser(request);
	Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class,
	request.getParameter("pertemuan").trim(), true);
	boolean bolehUbah = pertemuan.bolehUbahAbsenSaja(tbmuser);
	PengajuanIzinTidakMasukPerkuliahan pengajuanIzinTidakMasukPerkuliahan = (PengajuanIzinTidakMasukPerkuliahan) (request
	.getParameter("id") == null ? null
			: GeneralValueObject.ambilData(PengajuanIzinTidakMasukPerkuliahan.class,
					request.getParameter("id").trim(), true));
	String contentModal = request.getParameter("contentModal").trim();
	if (pengajuanIzinTidakMasukPerkuliahan == null) {
		pengajuanIzinTidakMasukPerkuliahan = new PengajuanIzinTidakMasukPerkuliahan();
	}
	VOPembelajaran pembelajaran = pertemuan.ambilVOPembelajaran();
	List<Long> mhsIds = (pembelajaran != null) ? pembelajaran.ambilMahasiswaById() : new java.util.ArrayList<Long>();
	List<Long> swIds  = (pembelajaran != null) ? pembelajaran.ambilSiswaById()     : new java.util.ArrayList<Long>();
	boolean isSiswaKelas = (swIds != null && !swIds.isEmpty()) && (mhsIds == null || mhsIds.isEmpty());

	String personLabel = isSiswaKelas ? Common.getBahasaConfig("Siswa") : Common.getBahasaConfig("Mahasiswa");
	String personField = isSiswaKelas ? "siswa" : "mahasiswa";

	String[] labels = { personLabel, "Jenis Pengajuan", "Keterangan / Alasan" };
	String[] formCols = { personField, "statusabsensi", "keterangan" };

	if (bolehUbah) {

		String fileData = "file_lampiran_izin_sakit;{\"jenis\":\"" + LampiranLain.IZIN_TIDAK_MASUK + "\",\"clazz\":\""
		+ LampiranLain.class.getName() + "\", \"id\":\""
		+ (pengajuanIzinTidakMasukPerkuliahan == null || pengajuanIzinTidakMasukPerkuliahan.getId() == null ? ""
				: pengajuanIzinTidakMasukPerkuliahan.getId().toString())
		+ "\"}";

		String statusabsensi = "statusabsensi;{\"where\":\"id in (" + ConstantValues.IZIN.getId() + ","+ ConstantValues.SAKIT.getId() + ")\"}";

		String personDropdown;
		if (isSiswaKelas) {
			String whereSw = joinIds(swIds);
			personDropdown = "siswa;{\"where\":\"id in (" + whereSw + ")\"}";
		} else {
			String whereMhs = joinIds(mhsIds);
			personDropdown = "mahasiswa;{\"where\":\"id in (" + whereMhs + ")\"}";
		}

		labels = new String[] { personLabel, "Jenis Pengajuan", "Keterangan / Alasan", "Persetujuan","Upload File / Surat Pengajuan" };
		formCols = new String[] { personDropdown, statusabsensi, "keterangan;text", "diizinkan", fileData };
	}

	Set<String> paramRequired = new HashSet<String>();
	paramRequired.add(personField);
	paramRequired.add("statusabsensi");
	paramRequired.add("keterangan");

	Set<String> paramTidakBolehSama = new HashSet<String>();

	Map<String, String> hiddenValues = new HashMap<String, String>();
	hiddenValues.put("pertemuan", request.getParameter("pertemuan").trim());

	// Generate HTML
	String htmlOutput = DynamicFormGenerator.generateForm(labels, formCols, contentModal,
	"Pengajuan izin/sakit untuk tidak hadir", PengajuanIzinTidakMasukPerkuliahan.class.getName(), paramRequired,
	paramTidakBolehSama, pengajuanIzinTidakMasukPerkuliahan, hiddenValues);
	//System.out.println(htmlOutput);
	out.println(htmlOutput);
} catch (Exception e) {
	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/pengajuan_izin_atau_sakit.jsp:92");
}
%>