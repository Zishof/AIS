<%@page import="ais.database.model.koperasi.AnggotaKoperasi"%>
<%@page import="ais.database.model.Pegawai"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="ais.database.model.sekolah.Guru"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
// 1. PEMERIKSAAN KEAMANAN & SESI
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
    return;
}
String linkGanti = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=common&s=_do_update_profile";
Mahasiswa mahasiswa = null;
Siswa siswa = null;
Guru guru = null;
Dosen dosen = null;
Pegawai pegawai = null;

// Inisialisasi variabel untuk ditampilkan di antarmuka
String namaLengkap = "";
String emailUser = "";
String noHpUser = "";
String peranUser = "Administrator";

// 2. IDENTIFIKASI PERAN PENGGUNA
if (tbmuser.getMahasiswa() != null) {
    mahasiswa = tbmuser.getMahasiswa();
    namaLengkap = mahasiswa.getNama() != null ? mahasiswa.getNama() : "";
    emailUser = mahasiswa.getEmail() != null ? mahasiswa.getEmail() : "";
    noHpUser = mahasiswa.getTelp() != null ? mahasiswa.getTelp() : ""; 
    peranUser = "Mahasiswa";
} else if (tbmuser.getSiswa() != null) {
    siswa = tbmuser.getSiswa();
    namaLengkap = siswa.getNamaSiswa() != null ? siswa.getNamaSiswa() : "";
    emailUser = siswa.getAlamatEmail() != null ? siswa.getAlamatEmail() : "";
    noHpUser = siswa.getTeleponSiswa() != null ? siswa.getTeleponSiswa() : "";
    peranUser = "Siswa";
} else if (tbmuser.ambilGuru() != null) {
    guru = tbmuser.ambilGuru();
    namaLengkap = guru.getNamaGuru() != null ? guru.getNamaGuru() : "";
    emailUser = guru.getAlamatEmail() != null ? guru.getAlamatEmail() : "";
    noHpUser = guru.getTeleponGuru() != null ? guru.getTeleponGuru() : "";
    peranUser = "Guru";
} else if (tbmuser.ambilDosen() != null) {
    dosen = tbmuser.ambilDosen();
    namaLengkap = dosen.getNama() != null ? dosen.getNama() : "";
    emailUser = dosen.getEmail() != null ? dosen.getEmail() : "";
    noHpUser = dosen.getHp() != null ? dosen.getHp() : "";
    peranUser = "Dosen";
} else if (tbmuser.ambilPegawai() != null) {
    pegawai = tbmuser.ambilPegawai();
    namaLengkap = pegawai.getNama() != null ? pegawai.getNama() : "";
    emailUser = pegawai.getEmail() != null ? pegawai.getEmail() : "";
    noHpUser = pegawai.getHp() != null ? pegawai.getHp() : "";
    peranUser = "Pegawai";
} else {
    // Jika profil hanya administrator / Tbmuser murni
    namaLengkap = tbmuser.getUserNama() != null ? tbmuser.getUserNama() : "";
    emailUser = tbmuser.getEmail() != null ? tbmuser.getEmail() : "";
    noHpUser = tbmuser.getHp() != null ? tbmuser.getHp() : "";
    
    AnggotaKoperasi anggotaKoperasi = tbmuser.getAnggotaKoperasi();
    if(anggotaKoperasi != null){
    	emailUser = anggotaKoperasi.getEmail();
    	noHpUser = anggotaKoperasi.getHp();
    }
}




%>
<script type="text/javascript">
    function tampilModalUbahProfil() {
        var existingModal = document.getElementById('modalUbahProfil');
        if (existingModal) {
            existingModal.remove();
        }

        var modalHtml = `
            <div class="modal fade" id="modalUbahProfil" tabindex="-1" role="dialog" aria-hidden="true">
                <div class="modal-dialog modal-dialog-centered" role="document">
                    <div class="modal-content shadow-lg border-0">
                        <div class="modal-header bg-info text-white">
                            <h5 class="modal-title">
                                <i class="fas fa-user-edit me-2"></i> <%= Common.getBahasaConfig("Ubah Profil Pengguna") %>
                            </h5>
                            <button type="button" class="close text-white" data-dismiss="modal" aria-label="Close" onclick="$('#modalUbahProfil').modal('hide');">
                                <span aria-hidden="true">&times;</span>
                            </button>
                        </div>
                        <div class="modal-body px-4 py-4">
                            <form id="formUbahProfil">
                                <div class="text-center mb-4">
                                    <span class="badge badge-primary px-3 py-2"><i class="fas fa-id-badge"></i> <%= peranUser %></span>
                                </div>
                                
                                <div class="form-group mb-3">
                                    <label class="font-weight-bold"><%= Common.getBahasaConfig("Nama Lengkap") %></label>
                                    <div class="input-group">
                                        <div class="input-group-prepend">
                                            <span class="input-group-text"><i class="fas fa-user"></i></span>
                                        </div>
                                        <input type="text" class="form-control bg-light" value="<%= namaLengkap %>" readonly disabled>
                                    </div>
                                    <small class="form-text text-muted"><%= Common.getBahasaConfig("Nama lengkap tidak dapat diubah secara mandiri.") %></small>
                                </div>
                                
                                <div class="form-group mb-3">
                                    <label class="font-weight-bold"><%= Common.getBahasaConfig("Alamat Email") %> <span class="text-danger">*</span></label>
                                    <div class="input-group">
                                        <div class="input-group-prepend">
                                            <span class="input-group-text"><i class="fas fa-envelope"></i></span>
                                        </div>
                                        <input type="email" class="form-control" id="profilEmail" name="profilEmail" value="<%= emailUser %>" placeholder="<%= Common.getBahasaConfig("Masukkan alamat email aktif") %>">
                                    </div>
                                </div>

                                <div class="form-group mb-3">
                                    <label class="font-weight-bold"><%= Common.getBahasaConfig("Nomor Telepon / HP") %> <span class="text-danger">*</span></label>
                                    <div class="input-group">
                                        <div class="input-group-prepend">
                                            <span class="input-group-text"><i class="fas fa-phone"></i></span>
                                        </div>
                                        <input type="text" class="form-control" id="profilNoHp" name="profilNoHp" value="<%= noHpUser %>" placeholder="<%= Common.getBahasaConfig("Masukkan nomor telepon") %>">
                                    </div>
                                </div>
                            </form>
                            
                            <div id="ubahProfilAlert" class="alert mt-3" style="display: none;"></div>
                        </div>
                        <div class="modal-footer bg-light">
                            <button type="button" class="btn btn-secondary" onclick="$('#modalUbahProfil').modal('hide');">
                                <i class="fas fa-times"></i> <%= Common.getBahasaConfig("Batal") %>
                            </button>
                            <button type="button" class="btn btn-info" onclick="prosesUbahProfil();">
                                <i class="fas fa-save"></i> <%= Common.getBahasaConfig("Simpan Perubahan") %>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;

        document.body.insertAdjacentHTML('beforeend', modalHtml);
        $('#modalUbahProfil').modal('show');
    }

    function prosesUbahProfil() {
        var emailInput = $('#profilEmail').val();
        var noHpInput = $('#profilNoHp').val();
        var alertBox = $('#ubahProfilAlert');

        if (!emailInput || !noHpInput) {
            alertBox.removeClass('alert-success').addClass('alert-danger').html('<i class="fas fa-exclamation-triangle"></i> <%= Common.getBahasaConfig("Email dan Nomor Telepon tidak boleh kosong.") %>').show();
            return;
        }

        var btnSave = $('#modalUbahProfil .btn-info');
        var originalText = btnSave.html();
        btnSave.html('<i class="fas fa-spinner fa-spin"></i> <%= Common.getBahasaConfig("Memproses...") %>').prop('disabled', true);

        $.ajax({
            url: '<%=linkGanti%>',
            type: 'POST',
            dataType: 'json',
            data: {
                email: emailInput,
                noHp: noHpInput
            },
            success: function(response) {
                btnSave.html(originalText).prop('disabled', false);
                if (response.status === 'success') {
                    alertBox.removeClass('alert-danger').addClass('alert-success').html('<i class="fas fa-check-circle"></i> ' + response.message).show();
                    setTimeout(function() {
                        $('#modalUbahProfil').modal('hide');
                        window.location.reload();
                    }, 500);
                } else {
                    alertBox.removeClass('alert-success').addClass('alert-danger').html('<i class="fas fa-exclamation-triangle"></i> ' + response.message).show();
                }
            },
            error: function() {
                btnSave.html(originalText).prop('disabled', false);
                alertBox.removeClass('alert-success').addClass('alert-danger').html('<i class="fas fa-exclamation-triangle"></i> <%= Common.getBahasaConfig("Terjadi kesalahan pada server. Silakan coba lagi.") %>').show();
            }
        });
    }
</script>