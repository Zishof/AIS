<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.koperasi.CaraPembayaranKoperasi"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="ais.database.model.koperasi.AnggotaKoperasi"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
// Pengecekan Sesi Pengguna
Tbmuser tbmuser = Common.getCurrentUser(request);
AnggotaKoperasi curentAnggota = tbmuser == null ? null : tbmuser.getAnggotaKoperasi();

if(curentAnggota == null) {
    out.print("<div class='text-center p-5'><i class='fas fa-exclamation-triangle fa-3x text-warning mb-3'></i><br>" + Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "</div>");
    return;
}

String caraPembayaranKoperasidata = request.getParameter("cara_pembayaran_id");
CaraPembayaranKoperasi caraPembayaranKoperasi = (CaraPembayaranKoperasi) (caraPembayaranKoperasidata == null ? null : GeneralValueObject.ambilData(CaraPembayaranKoperasi.class, caraPembayaranKoperasidata));

if(caraPembayaranKoperasi == null){
    out.print("<div class='text-center p-5'><i class='fas fa-times-circle fa-3x text-danger mb-3'></i><br>" + Common.getBahasaConfig("Cara Pembayaran tidak ditemukan atau tidak valid.") + "</div>");
    return;
}

String rnd = Common.getGeneratedBarCode(7);
String idMember = String.valueOf(curentAnggota.getId());

// Endpoint base
String urlTopupService = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=kantin%2Fmember&s=_topup_service";

// Mengambil Konfigurasi Saluran (Channel) Pembayaran
String cannel_va_e_smartlink = Common.getKonfigurasi("cannel_va_e_smartlink", "VA_BNI:2500:BNI;VA_BRI:2500:BRI;VA_BCA:3500:BCA;VA_BNC:3500:BNC(Bank Neo Commerce);VA_CIMB:2500:CIMB Niaga;VA_MANDIRI:3500:Bank Mandiri;VA_PERMATA:2500:Bank Permata;VA_BSI:3000:BSI;VA_DANAMON:3000:Danamon;OTC_ALFAMART:3000:Alfamart;OTC_INDOMARET:3000:Indomart").getNilai();
%>

<style>
    /* CSS Khusus untuk Efek Visual Radio Check (Karena Bootstrap 5 Radio Cards butuh custom styling minimal) */
    input[name="channelRadio<%=rnd%>"]:checked + label {
        background-color: #f8f9fa;
        border-color: #198754 !important;
        box-shadow: 0 4px 8px rgba(25, 135, 84, 0.2) !important;
    }
    input[name="channelRadio<%=rnd%>"]:checked + label .check-icon<%=rnd%> {
        display: inline-block !important;
    }
    .custom-radio-card<%=rnd%> {
        transition: all 0.2s ease-in-out;
    }
    .custom-radio-card<%=rnd%>:hover {
        transform: translateY(-2px);
    }
</style>

<div class="container-fluid p-3 p-md-4 animate__animated animate__fadeIn" id="topupContent<%=rnd%>">
    
    <div id="stepSelection<%=rnd%>" class="d-block">
        <div class="text-center mb-4">
            <div class="d-inline-flex align-items-center justify-content-center bg-primary bg-opacity-10 text-primary rounded-circle mb-3" style="width: 60px; height: 60px;">
                <i class="fas fa-wallet fa-2x"></i>
            </div>
            <h5 class="fw-bold text-dark mb-1"><%=Common.getBahasaConfig("Isi Saldo Akun")%></h5>
            <p class="text-muted small"><%=Common.getBahasaConfig("Silakan tentukan jumlah saldo dan metode pembayaran yang Anda inginkan.")%></p>
        </div>

        <h6 class="fw-bold text-secondary mb-3 border-bottom pb-2"><i class="fas fa-coins me-2"></i>1. <%=Common.getBahasaConfig("Pilih Nominal")%></h6>
        <div class="row g-2 g-md-3 mb-4 justify-content-center">
            <% 
                long[] presets = {10000, 20000, 50000, 100000, 250000, 500000, 1000000};
                for(int i = 0; i < presets.length; i++) {
                    long p = presets[i];
            %>
                <div class="col-6 col-sm-4 col-md-3">
                    <input type="radio" class="btn-check" name="nominalRadio<%=rnd%>" id="preset<%=i%><%=rnd%>" value="<%=p%>" onchange="handlePresetChange<%=rnd%>()">
                    <label class="btn btn-outline-primary w-100 h-100 p-3 rounded-4 shadow-sm d-flex align-items-center justify-content-center custom-radio-card<%=rnd%>" for="preset<%=i%><%=rnd%>">
                        <span class="fw-bolder fs-6"><%=Common.numberFormat.get().format(p)%></span>
                    </label>
                </div>
            <% } %>
        </div>

        <div class="row justify-content-center mb-5">
            <div class="col-12 col-md-8 col-lg-6">
                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Atau Masukkan Nominal (Rp)")%></label>
                <div class="input-group input-group-lg shadow-sm">
                    <span class="input-group-text bg-light border-end-0 text-muted fw-bold">Rp</span>
                    <input type="number" class="form-control border-start-0 fw-bolder text-dark" id="inputManualNominal<%=rnd%>" placeholder="0" oninput="handleManualInputChange<%=rnd%>()" min="10000">
                </div>
                <small class="text-muted mt-2 d-block"><i class="fas fa-info-circle me-1 text-primary"></i><%=Common.getBahasaConfig("Minimal pengisian saldo adalah Rp 10.000")%></small>
            </div>
        </div>

        <h6 class="fw-bold text-secondary mb-3 border-bottom pb-2"><i class="fas fa-university me-2"></i>2. <%=Common.getBahasaConfig("Pilih Saluran Pembayaran")%></h6>
        <div class="row g-2 justify-content-center mb-4">
            <%
                int chIndex = 0;
                for (String s : cannel_va_e_smartlink.split(";")) {
                    if (s.trim().isEmpty()) continue;
                    String[] d = StringUtils.split(s.trim(), ":");
                    if (d.length < 3) continue;

                    String channelCode = d[0];
                    double adminFee = Double.parseDouble(d[1]);
                    String channelName = d[2];
                    String inputValue = channelCode + "|" + adminFee + "|" + channelName;
            %>
                <div class="col-12 col-sm-6 col-md-4">
                    <input type="radio" class="btn-check" name="channelRadio<%=rnd%>" id="channel<%=chIndex%><%=rnd%>" value="<%=inputValue%>">
                    <label class="btn btn-outline-secondary w-100 p-3 rounded-4 shadow-sm text-start custom-radio-card<%=rnd%>" for="channel<%=chIndex%><%=rnd%>">
                        <div class="fw-bold text-dark mb-1"><i class="fas fa-check-circle text-success me-2 d-none check-icon<%=rnd%>"></i><%=channelName%></div>
                        <div class="small text-muted"><%=Common.getBahasaConfig("Biaya Admin:")%> <span class="fw-semibold text-danger"><%=Common.numberFormat.get().format(adminFee)%></span></div>
                    </label>
                </div>
            <% 
                    chIndex++;
                } 
            %>
        </div>

        <div class="text-center mt-5">
            <button class="btn btn-primary btn-lg rounded-pill px-5 fw-bold shadow-sm w-100" style="max-width: 400px;" onclick="goToConfirmation<%=rnd%>()">
                <%=Common.getBahasaConfig("Lanjutkan Transaksi")%><i class="fas fa-chevron-right ms-2"></i>
            </button>
        </div>
    </div>

    <div id="stepConfirmation<%=rnd%>" class="d-none animate__animated animate__fadeInRight">
        <div class="row justify-content-center">
            <div class="col-md-8 col-lg-6">
                <div class="card border-0 shadow-sm rounded-4 mb-4 border-top border-warning border-4">
                    <div class="card-body p-4 p-md-5 text-center">
                        <i class="fas fa-clipboard-check fa-4x text-warning mb-3"></i>
                        <h4 class="fw-bold text-dark mb-2"><%=Common.getBahasaConfig("Rincian Transaksi")%></h4>
                        <p class="text-muted small"><%=Common.getBahasaConfig("Mohon periksa kembali rincian di bawah ini sebelum mengonfirmasi pembayaran.")%></p>
                        
                        <div class="bg-light p-4 rounded-4 my-4 text-start border shadow-sm">
                            <div class="d-flex justify-content-between mb-2 border-bottom pb-2">
                                <span class="text-muted"><%=Common.getBahasaConfig("Nominal Isi Saldo")%></span>
                                <span class="fw-bold text-dark" id="confirmNominal<%=rnd%>">Rp 0</span>
                            </div>
                            <div class="d-flex justify-content-between mb-2 border-bottom pb-2">
                                <span class="text-muted"><%=Common.getBahasaConfig("Saluran Pembayaran")%></span>
                                <span class="fw-bold text-dark text-end" id="confirmChannelName<%=rnd%>">-</span>
                            </div>
                            <div class="d-flex justify-content-between mb-2 border-bottom pb-2">
                                <span class="text-muted"><%=Common.getBahasaConfig("Biaya Admin")%></span>
                                <span class="fw-bold text-danger" id="confirmAdminFee<%=rnd%>">Rp 0</span>
                            </div>
                            <div class="d-flex justify-content-between mt-3 align-items-center">
                                <span class="fw-bold text-dark"><%=Common.getBahasaConfig("Total Pembayaran")%></span>
                                <h3 class="fw-bolder text-primary mb-0" id="displayNominalConfirm<%=rnd%>">Rp 0</h3>
                            </div>
                        </div>

                        <div class="d-grid gap-2">
                            <button class="btn btn-success btn-lg rounded-pill fw-bold shadow-sm" id="btnConfirmFinal<%=rnd%>" onclick="executeTopup<%=rnd%>()">
                                <i class="fas fa-check-circle me-2"></i><%=Common.getBahasaConfig("Buat Tagihan Pembayaran")%>
                            </button>
                            <button class="btn btn-light rounded-pill fw-bold text-muted border mt-2" onclick="backToSelection<%=rnd%>()">
                                <i class="fas fa-arrow-left me-2"></i><%=Common.getBahasaConfig("Kembali")%>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div id="stepPayment<%=rnd%>" class="d-none animate__animated animate__zoomIn text-center py-5">
        <div id="loadingPayment<%=rnd%>">
            <div class="spinner-border text-primary mb-3" role="status" style="width: 3rem; height: 3rem;"></div>
            <h5 class="fw-bold text-dark" id="paymentStatusTitle<%=rnd%>"><%=Common.getBahasaConfig("Menghubungi Saluran Pembayaran...")%></h5>
            <p class="text-muted" id="paymentStatusDesc<%=rnd%>"><%=Common.getBahasaConfig("Sistem sedang membuatkan nomor virtual account / kode bayar untuk Anda. Mohon tunggu sebentar.")%></p>
        </div>
        
        <div class="mt-4" id="paymentErrorActions<%=rnd%>" style="display: none;">
             <button class="btn btn-outline-secondary rounded-pill px-4 fw-bold shadow-sm" onclick="location.reload()">
                <i class="fas fa-undo me-2"></i><%=Common.getBahasaConfig("Kembali")%>
            </button>
        </div>
    </div>

</div>

<script>
    // ==========================================
    // LOGIKA JAVASCRIPT TOPUP
    // ==========================================
    let selectedValue<%=rnd%> = 0;
    let selectedChannelCode<%=rnd%> = "";
    let selectedChannelFee<%=rnd%> = 0;
    let selectedChannelName<%=rnd%> = "";
    const idMemberAktif<%=rnd%> = '<%=idMember%>';
    const serviceUrlBase<%=rnd%> = '<%=urlTopupService%>';
    const idCaraBayarKoperasi<%=rnd%> = '<%=caraPembayaranKoperasi.getId()%>';

    // Handler klik nominal preset
    const handlePresetChange<%=rnd%> = () => {
        const radios = document.getElementsByName('nominalRadio<%=rnd%>');
        for (let i = 0; i < radios.length; i++) {
            if (radios[i].checked) {
                selectedValue<%=rnd%> = parseInt(radios[i].value);
                break;
            }
        }
        document.getElementById('inputManualNominal<%=rnd%>').value = '';
    };

    // Handler saat input manual diketik
    const handleManualInputChange<%=rnd%> = () => {
        const radios = document.getElementsByName('nominalRadio<%=rnd%>');
        radios.forEach(radio => radio.checked = false);
        selectedValue<%=rnd%> = 0;
    };

    // Navigasi Tampilan (D-NONE / D-BLOCK)
    const switchStep<%=rnd%> = (oldStepId, newStepId) => {
        document.getElementById(oldStepId).classList.replace('d-block', 'd-none');
        document.getElementById(newStepId).classList.replace('d-none', 'd-block');
    };

    // Helper Format Rupiah (Menggunakan fungsi Intl bawaan Browser)
    const formatRpStr<%=rnd%> = (angka) => {
        return "Rp " + new Intl.NumberFormat('id-ID').format(angka);
    };

    // Menampilkan Notifikasi UI
    const showMessageUI<%=rnd%> = (msg) => {
        if (typeof tampilkanToast === 'function') {
            tampilkanToast(msg, 'bg-warning text-dark');
        } else {
            alert(msg); // Fallback jika library toast belum dimuat
        }
    };

    // Navigasi ke Halaman Konfirmasi
    const goToConfirmation<%=rnd%> = () => {
        // 1. Validasi Nominal
        const manualInput = document.getElementById('inputManualNominal<%=rnd%>').value;
        const manualVal = manualInput ? parseFloat(manualInput) : 0;
        const finalAmount = manualVal > 0 ? manualVal : selectedValue<%=rnd%>;

        if (finalAmount < 10000) {
            showMessageUI<%=rnd%>('<%=Common.getBahasaConfigJS("Mohon maaf, minimal nominal pengisian saldo adalah Rp 10.000.")%>');
            document.getElementById('inputManualNominal<%=rnd%>').focus();
            return;
        }

        // 2. Validasi Pemilihan Channel (Cara Pembayaran)
        const channelRadios = document.getElementsByName('channelRadio<%=rnd%>');
        let isChannelSelected = false;
        for (let i = 0; i < channelRadios.length; i++) {
            if (channelRadios[i].checked) {
                const valParts = channelRadios[i].value.split("|");
                selectedChannelCode<%=rnd%> = valParts[0];
                selectedChannelFee<%=rnd%> = parseFloat(valParts[1]);
                selectedChannelName<%=rnd%> = valParts[2];
                isChannelSelected = true;
                break;
            }
        }

        if (!isChannelSelected) {
            showMessageUI<%=rnd%>('<%=Common.getBahasaConfigJS("Silakan pilih Saluran Pembayaran (contoh: VA BCA, Alfamart) terlebih dahulu.")%>');
            return;
        }

        // 3. Set Tampilan Konfirmasi
        const totalBayar = finalAmount + selectedChannelFee<%=rnd%>;
        
        document.getElementById('confirmNominal<%=rnd%>').innerText = formatRpStr<%=rnd%>(finalAmount);
        document.getElementById('confirmChannelName<%=rnd%>').innerText = selectedChannelName<%=rnd%>;
        document.getElementById('confirmAdminFee<%=rnd%>').innerText = formatRpStr<%=rnd%>(selectedChannelFee<%=rnd%>);
        document.getElementById('displayNominalConfirm<%=rnd%>').innerText = formatRpStr<%=rnd%>(totalBayar);
        
        // Simpan nilai untuk dikirim ke API
        document.getElementById('btnConfirmFinal<%=rnd%>').setAttribute('data-nominal', finalAmount);

        switchStep<%=rnd%>('stepSelection<%=rnd%>', 'stepConfirmation<%=rnd%>');
    };

    const backToSelection<%=rnd%> = () => {
        switchStep<%=rnd%>('stepConfirmation<%=rnd%>', 'stepSelection<%=rnd%>');
    };

    // Eksekusi Panggilan API Top-Up
    const executeTopup<%=rnd%> = async () => {
        const nominal = document.getElementById('btnConfirmFinal<%=rnd%>').getAttribute('data-nominal');
        const btnConfirm = document.getElementById('btnConfirmFinal<%=rnd%>');
        
        btnConfirm.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Memproses...")%>';
        btnConfirm.disabled = true;

        // Pindah ke tampilan Loading
        switchStep<%=rnd%>('stepConfirmation<%=rnd%>', 'stepPayment<%=rnd%>');

        const payload = {
            action: "topup",
            id_member: idMemberAktif<%=rnd%>,
            nominal: nominal,
            selectedChannelCode: selectedChannelCode<%=rnd%>,
            cara_pembayaran_id: idCaraBayarKoperasi<%=rnd%>,
            admin: selectedChannelFee<%=rnd%>
        };

        // Format query parameter untuk memastikan data juga diterima jika menggunakan request.getParameter
        const finalUrl = serviceUrlBase<%=rnd%> + "&selectedChannelCode=" + encodeURIComponent(selectedChannelCode<%=rnd%>) + "&cara_pembayaran_id=" + idCaraBayarKoperasi<%=rnd%>;

        try {
            const response = await fetch(finalUrl, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            
            const result = await response.json();
            
            if (result.status === '00' || result.status === 'success') {
                // Menarik URL dari struktur JSON yang diminta
                let paymentUrl = null;
                if (result.data && Array.isArray(result.data) && result.data.length > 0) {
                    paymentUrl = result.data[0].link;
                } else if (result.url) {
                    paymentUrl = result.url;
                }
                
                if (paymentUrl) {
                    document.getElementById('paymentStatusTitle<%=rnd%>').innerText = '<%=Common.getBahasaConfigJS("Mengalihkan ke Pembayaran...")%>';
                    document.getElementById('paymentStatusDesc<%=rnd%>').innerText = '<%=Common.getBahasaConfigJS("Silakan selesaikan instruksi pembayaran di halaman selanjutnya.")%>';
                    
                    // Eksekusi Redirect Langsung
                    window.location.href = paymentUrl;
                } else {
                    throw new Error("<%=Common.getBahasaConfig("Tautan pembayaran tidak dikembalikan oleh sistem.")%>");
                }
            } else {
                showMessageUI<%=rnd%>(result.message || result.description || "<%=Common.getBahasaConfig("Terjadi kesalahan saat memproses permintaan pembayaran.")%>");
                setTimeout(() => { location.reload(); }, 3000);
            }
        } catch (e) {
            console.error("API Error:", e);
            
            // Tampilkan error UI jika Fetch/Redirect Gagal
            switchStep<%=rnd%>('stepConfirmation<%=rnd%>', 'stepPayment<%=rnd%>');
            document.getElementById('paymentStatusTitle<%=rnd%>').innerText = '<%=Common.getBahasaConfigJS("Gagal Terhubung ke Layanan Pembayaran")%>';
            document.getElementById('paymentStatusTitle<%=rnd%>').classList.replace('text-dark', 'text-danger');
            document.getElementById('paymentStatusDesc<%=rnd%>').innerText = '<%=Common.getBahasaConfigJS("Sistem tidak dapat menyelesaikan permintaan. Silakan periksa koneksi internet Anda atau coba lagi nanti.")%>';
            document.querySelector('#loadingPayment<%=rnd%> .spinner-border').style.display = 'none';
            document.getElementById('paymentErrorActions<%=rnd%>').style.display = 'block';
        }
    };
</script>