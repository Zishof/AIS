<%@page import="java.util.HashSet"%>
<%@page import="java.util.Set"%>
<%@page import="ais.common.DynamicTableGenerator"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
String[] cols   = { "matakuliah", "dosen1", "kelas", "semester", "tahunAjaran", "hari", "ruang", "waktuMulai", "waktuSelesai" };
String[] lbls   = { "Matakuliah", "Dosen", "Kelas", "Semester", "Tahun Akademik", "Hari", "Ruang", "Mulai", "Sampai" };
String[] search = { "matakuliah", "dosen1", "kelas", "semester", "tahunAjaran", "hari", "ruang", "jurusan" };
String[] forms  = {
    "matakuliah", "dosen1", "dosen2", "dosen3", "kelas", "ruang", "hari",
    "waktuMulai", "waktuSelesai", "semester", "ganjilGenap", "tahunAjaran",
    "masaPerkuliahan", "jurusan", "program", "kapasitasKelas", "kurikulum",
    "jenisEvaluasi", "jenis", "mode", "lingkup",
    "planning_jumlah_tatap_muka", "jumlahMaksimalPertemuan",
    "tanggalMulaiPerkuliahan", "perkuliahanDimulai", "perkuliahanSampai",
    "mahasiswaBolehAbsenMenggunakanFoto", "dosenBolehAbsenMenggunakanFoto",
    "bolehAbsenSebelumWaktuMulaiDalamMenit", "bolehAbsenSetelahWaktuMulaiDalamMenit",
    "persenKehadiranDinilai0", "batasWaktuBolehAbsenKehadiran",
    "feeder", "keterangan", "aktif"
};
Set required = new HashSet();
required.add("matakuliah");
required.add("semester");
required.add("kelas");
out.println(DynamicTableGenerator.generateTable(cols, lbls, search, forms,
    Common.getGeneratedBarCode(5), "Jadwal Perkuliahan (Simple)", Perkuliahan.class.getName(), required, new HashSet()));
%>