<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.ConstantValues"%>
<%@ page import="ais.common.Common" %>
<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
    return;
}
String linkGanti = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=common&s=_do_ganti_password";
String username = null;

if (ConstantValues.aktifkanRememeberMe) {
    Cookie[] cookies = request.getCookies();

    if (cookies != null) {
    	for (Cookie aCookie : cookies) {
		    if (aCookie.getName().equals("userinfo")) {
		        // Decode URL Encoded string dari JS kembali menjadi karakter asli
		        username = java.net.URLDecoder.decode(aCookie.getValue(), "UTF-8");
		    }
		}

    }
}

boolean loginSedangDiIngatBrowser = false;
// Validasi NPE yang aman
if (username != null && !username.trim().isEmpty()) {
	loginSedangDiIngatBrowser = true;
}
%>
<script type="text/javascript">
    function tampilModalGantiPassword(hiddenOld) {
        // Hapus modal jika sudah ada sebelumnya agar tidak duplikat di DOM
        var existingModal = document.getElementById('modalGantiPassword');
        if (existingModal) {
            existingModal.remove();
        }

        // Tentukan apakah input kata sandi lama disembunyikan
        var oldPasswordStyle = hiddenOld ? 'display: none;' : '';

        // Template HTML untuk Modal Bootstrap
        var modalHtml = `
            <div class="modal fade" id="modalGantiPassword" tabindex="-1" role="dialog" aria-labelledby="modalGantiPasswordLabel" aria-hidden="true">
                <div class="modal-dialog modal-dialog-centered" role="document">
                    <div class="modal-content shadow-lg border-0">
                        <div class="modal-header bg-primary text-white">
                            <h5 class="modal-title" id="modalGantiPasswordLabel">
                                <i class="fas fa-key me-2"></i> <%= Common.getBahasaConfig("Ganti Kata Sandi") %>
                            </h5>
                            <button type="button" class="close text-white" data-dismiss="modal" aria-label="Close" onclick="$('#modalGantiPassword').modal('hide');">
                                <span aria-hidden="true">&times;</span>
                            </button>
                        </div>
                        <div class="modal-body px-4 py-4">
                            <form id="formGantiPassword">
                                <div class="form-group mb-3" style="` + oldPasswordStyle + `">
                                    <label class="font-weight-bold"><%= Common.getBahasaConfig("Kata Sandi Lama") %> <span class="text-danger">*</span></label>
                                    <div class="input-group">
                                        <div class="input-group-prepend">
                                            <span class="input-group-text"><i class="fas fa-unlock"></i></span>
                                        </div>
                                        <input type="password" class="form-control" id="passwordLama" name="passwordLama" placeholder="<%= Common.getBahasaConfig("Masukkan kata sandi lama Anda") %>">
                                    </div>
                                </div>
                                
                                <div class="form-group mb-3">
                                    <label class="font-weight-bold"><%= Common.getBahasaConfig("Kata Sandi Baru") %> <span class="text-danger">*</span></label>
                                    <div class="input-group">
                                        <div class="input-group-prepend">
                                            <span class="input-group-text"><i class="fas fa-lock"></i></span>
                                        </div>
                                        <input type="password" class="form-control" id="passwordBaru" name="passwordBaru" placeholder="<%= Common.getBahasaConfig("Masukkan kata sandi baru") %>">
                                    </div>
                                </div>

                                <div class="form-group mb-3">
                                    <label class="font-weight-bold"><%= Common.getBahasaConfig("Ulangi Kata Sandi Baru") %> <span class="text-danger">*</span></label>
                                    <div class="input-group">
                                        <div class="input-group-prepend">
                                            <span class="input-group-text"><i class="fas fa-lock"></i></span>
                                        </div>
                                        <input type="password" class="form-control" id="passwordBaruRepeat" name="passwordBaruRepeat" placeholder="<%= Common.getBahasaConfig("Ulangi kata sandi baru") %>">
                                    </div>
                                </div>
                            </form>
                            
                            <div class="alert alert-info mt-4 mb-0 text-sm">
                                <i class="fas fa-info-circle"></i> 
                                <%= Common.getBahasaConfig("Syarat kata sandi baru: minimal 8 karakter, mengandung huruf dan angka, serta setidaknya satu karakter spesial (contoh: !@#$%^&*()_+).") %>
                            </div>
                            
                            <div id="gantiPasswordAlert" class="alert mt-3" style="display: none;"></div>
                        </div>
                        <div class="modal-footer bg-light">
                            <button type="button" class="btn btn-secondary" onclick="$('#modalGantiPassword').modal('hide');">
                                <i class="fas fa-times"></i> <%= Common.getBahasaConfig("Batal") %>
                            </button>
                            <button type="button" class="btn btn-primary" onclick="prosesGantiPassword();">
                                <i class="fas fa-save"></i> <%= Common.getBahasaConfig("Simpan Perubahan") %>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;

        // Masukkan HTML modal ke dalam body
        document.body.insertAdjacentHTML('beforeend', modalHtml);

        // Tampilkan modal (menggunakan jQuery karena Bootstrap standar membutuhkan ini)
        $('#modalGantiPassword').modal('show');
    }

    function prosesGantiPassword() {
        var pwdLama = $('#passwordLama').val();
        var pwdBaru = $('#passwordBaru').val();
        var pwdBaruRepeat = $('#passwordBaruRepeat').val();
        var alertBox = $('#gantiPasswordAlert');

        // Validasi Frontend Sederhana
        if (!pwdBaru || !pwdBaruRepeat) {
            alertBox.removeClass('alert-success').addClass('alert-danger').html('<i class="fas fa-exclamation-triangle"></i> <%= Common.getBahasaConfig("Kata sandi baru tidak boleh kosong.") %>').show();
            return;
        }

        if (pwdBaru !== pwdBaruRepeat) {
            alertBox.removeClass('alert-success').addClass('alert-danger').html('<i class="fas fa-exclamation-triangle"></i> <%= Common.getBahasaConfig("Kata sandi baru dan pengulangannya tidak cocok.") %>').show();
            return;
        }

        // Disable tombol saat proses
        var btnSave = $('#modalGantiPassword .btn-primary');
        var originalText = btnSave.html();
        btnSave.html('<i class="fas fa-spinner fa-spin"></i> <%= Common.getBahasaConfig("Memproses...") %>').prop('disabled', true);

        // AJAX request ke file JSP service
        $.ajax({
            url: '<%=linkGanti%>',
            type: 'POST',
            dataType: 'json',
            data: {
                passwordLama: pwdLama,
                passwordBaru: pwdBaru
            },
            success: function(response) {
                btnSave.html(originalText).prop('disabled', false);
                if (response.status === 'success') {
                    alertBox.removeClass('alert-danger').addClass('alert-success').html('<i class="fas fa-check-circle"></i> ' + response.message).show();
                    setTimeout(function() {
                        $('#modalGantiPassword').modal('hide');
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

    
function tampilkanPopupKeluar() {
    // Inject variabel boolean dari server (JSP)
    var isLoginDiingat = <%= loginSedangDiIngatBrowser %>;
    
    // Siapkan Endpoint URL (pastikan Common.ROOT terdefinisi dengan baik)
    var urlLogoff = '<%= Common.ROOT %>/logoff';
    var urlClearCookie = '<%= Common.ROOT %>/clear-cookie';

    // Struktur Modal HTML 
    var modalHtml = '<div class="modal fade" id="modalKeluarSistem" tabindex="-1" aria-labelledby="modalKeluarSistemLabel" aria-hidden="true">' +
        '<div class="modal-dialog modal-dialog-centered" style="max-width: 450px;">' +
            '<div class="modal-content border-0 shadow">' +
                '<div class="modal-header bg-light">' +
                    '<h5 class="modal-title fs-0 fw-bold" id="modalKeluarSistemLabel">Pertanyaan</h5>' +
                    '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>' +
                '</div>' +
                '<div class="modal-body text-center py-4">' +
                    '<p class="mb-3 fs-7 text-800"><%=Common.getBahasaConfig("Apakah ingin keluar aplikasi?")%></p>';

    // Sisipkan Checkbox HANYA jika login sedang diingat di browser
    if (isLoginDiingat) {
        modalHtml += '<div class="form-check d-flex justify-content-center align-items-center mb-0 mt-3">' +
            '<input class="form-check-input me-2 shadow-none" type="checkbox" id="chkLupakanAkun" style="cursor: pointer; transform: scale(1.2);">' +
            '<label class="form-check-label text-700 fw-medium" for="chkLupakanAkun" style="cursor: pointer; user-select: none;">' +
                '<%=Common.getBahasaConfigJS("Hapus ingatan otomatis login di bowser ini.")%>' +
            '</label>' +
        '</div>';
    }

    modalHtml += '</div>' +
                '<div class="modal-footer justify-content-center bg-light border-0 py-3">' +
                    '<button type="button" class="btn btn-secondary px-4 me-2" data-bs-dismiss="modal">' +
                        '<i class="fas fa-times me-2"></i><%=Common.getBahasaConfig("Batal")%>' +
                    '</button>' +
                    '<button type="button" class="btn btn-primary px-4" onclick="prosesKeluarSistem(\'' + urlLogoff + '\', \'' + urlClearCookie + '\')">' +
                        '<i class="fas fa-sign-out-alt me-2"></i><%=Common.getBahasaConfig("Keluar")%>' +
                    '</button>' +
                '</div>' +
            '</div>' +
        '</div>' +
    '</div>';

    // Bersihkan Modal lama jika ada (untuk mencegah penumpukan elemen di DOM)
    var existingModal = document.getElementById('modalKeluarSistem');
    if (existingModal) {
        existingModal.parentNode.removeChild(existingModal);
    }

    // Suntikkan modal ke body HTML
    document.body.insertAdjacentHTML('beforeend', modalHtml);

    // Tampilkan menggunakan instance Bootstrap
    var modalElement = document.getElementById('modalKeluarSistem');
    var modalInstance = new bootstrap.Modal(modalElement, {
        keyboard: true,
        backdrop: 'static'
    });
    modalInstance.show();
}

function prosesKeluarSistem(urlLogoff, urlClearCookie) {
    var chkLupakanAkun = document.getElementById('chkLupakanAkun');
    
    // Periksa apakah elemen checkbox ada DAN kondisinya dicentang
    if (chkLupakanAkun && chkLupakanAkun.checked) {
        window.location.href = urlClearCookie;
    } else {
        window.location.href = urlLogoff;
    }
}
</script>