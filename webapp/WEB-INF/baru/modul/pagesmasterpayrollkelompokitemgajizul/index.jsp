<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
/*
 * SENGAJA tidak lagi memanggil DynamicJspCrudGenerator.generate(...) untuk kelas ini.
 * KelompokItemGaji adalah katalog akun jurnal penggajian TANPA kolom tenant apa pun; form CRUD
 * otomatis merender setiap properti termasuk "aktif" (boolean) dan "akun"/"akunDebet" (JSON
 * pemetaan akun) tanpa satu pun pembatas satuan kerja/tenant. Menyalakan "aktif" lewat form ini
 * mempersenjatai pencocokan kode di ItemGaji.getKelompokItemGaji() -- kelompok baru yang kodenya
 * bertabrakan dengan kode komponen gaji yang sudah ada akan memindahkan akun jurnal komponen itu
 * lintas tenant. Layar ZK (KelompokItemGajiAction) tetap menjadi satu-satunya jalur ubah yang
 * disediakan, dengan penjaga tabrakan kode di checkKodeKelompokItemGaji(). Jangan aktifkan
 * kembali generator ini untuk kelas ini tanpa terlebih dahulu memberi entity ini sumbu tenant.
 */
out.println(
    "<div style='padding:2rem;text-align:center;color:var(--color-text-secondary)'>" +
    "<i class='fas fa-tools' style='font-size:2rem;margin-bottom:.75rem;display:block;opacity:.35' aria-hidden='true'></i>" +
    "<h5 style='margin:0 0 .4rem;font-weight:500;color:var(--color-text-primary)'>Kelompok Item Gaji</h5>" +
    "<p style='margin:0;font-size:.875rem'>Halaman ini belum tersedia di tampilan baru. Gunakan layar Kelompok Item Gaji pada tampilan lama.</p>" +
    "</div>"
);
%>