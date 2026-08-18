<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    String rnd = Common.getGeneratedBarCode(7);
    String judulNamaPerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getNama();
%>

<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%=Common.getBahasaConfig("Masuk Sistem Perpustakaan")%> - <%=judulNamaPerguruanTinggi%></title>
    
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/v4-shims.min.css">
    
    <style>
        body {
            margin: 0;
            padding: 0;
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            background: linear-gradient(rgba(13, 110, 253, 0.8), rgba(10, 88, 202, 0.9)), url('<%=Common.ROOT%>/img/buku.jpeg') center/cover no-repeat;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }

        .login-card-<%=rnd%> {
            background: rgba(255, 255, 255, 0.98);
            border-radius: 1.5rem;
            box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
            overflow: hidden;
            width: 100%;
            max-width: 450px;
            padding: 3rem 2.5rem;
            position: relative;
            backdrop-filter: blur(10px);
        }

        .login-header-<%=rnd%> { text-align: center; margin-bottom: 2rem; }
        .login-header-<%=rnd%> i { font-size: 3.5rem; color: #0d6efd; margin-bottom: 1rem; }
        .login-header-<%=rnd%> h4 { font-weight: 700; color: #212529; margin-bottom: 0.5rem; }
        .login-header-<%=rnd%> p { color: #6c757d; font-size: 0.9rem; }

        .input-group-custom-<%=rnd%> {
            background: #f8f9fa;
            border: 1px solid #ced4da;
            border-radius: 0.75rem;
            display: flex;
            align-items: center;
            padding: 0.5rem 1rem;
            margin-bottom: 1.25rem;
            transition: all 0.3s ease;
        }
        .input-group-custom-<%=rnd%>:focus-within { border-color: #0d6efd; box-shadow: 0 0 0 0.25rem rgba(13, 110, 253, 0.15); background: #fff; }
        .input-group-custom-<%=rnd%> i { color: #6c757d; margin-right: 10px; width: 20px; text-align: center; }
        .input-group-custom-<%=rnd%> input {
            border: none; background: transparent; width: 100%; outline: none; color: #212529; font-weight: 500;
        }

        .btn-login-<%=rnd%> {
            background: #0d6efd; color: white; border: none; border-radius: 0.75rem; width: 100%;
            padding: 0.8rem; font-weight: 700; font-size: 1rem; transition: all 0.3s ease; cursor: pointer;
        }
        .btn-login-<%=rnd%>:hover { background: #0b5ed7; transform: translateY(-2px); box-shadow: 0 5px 15px rgba(13, 110, 253, 0.3); }
        .btn-login-<%=rnd%>:disabled { background: #6c757d; cursor: not-allowed; transform: none; box-shadow: none; }

        .back-link-<%=rnd%> {
            display: block; text-align: center; margin-top: 1.5rem; color: #6c757d; text-decoration: none; font-size: 0.9rem; transition: color 0.2s;
        }
        .back-link-<%=rnd%>:hover { color: #0d6efd; }

        .alert-<%=rnd%> {
            display: none; padding: 0.75rem 1rem; border-radius: 0.5rem; margin-bottom: 1.5rem; font-size: 0.85rem; font-weight: 600; text-align: center;
        }
        .alert-danger-<%=rnd%> { background: #f8d7da; color: #842029; border: 1px solid #f5c2c7; }
        .alert-success-<%=rnd%> { background: #d1e7dd; color: #0f5132; border: 1px solid #badbcc; }
    </style>
</head>
<body>

    <div class="login-card-<%=rnd%>">
        <div class="login-header-<%=rnd%>">
            <i class="fas fa-book-reader"></i>
            <h4><%=Common.getBahasaConfig("Otentikasi Anggota")%></h4>
            <p><%=Common.getBahasaConfig("Masukkan identitas Anda untuk mengakses layanan perpustakaan.")%></p>
        </div>

        <div id="alertBox<%=rnd%>" class="alert-<%=rnd%>"></div>

        <form id="formLogin<%=rnd%>" onsubmit="prosesLogin<%=rnd%>(event)">
            <div class="input-group-custom-<%=rnd%>">
                <i class="fas fa-user"></i>
                <input type="text" id="username<%=rnd%>" name="username" placeholder="<%=Common.getBahasaConfig("Nomor Induk / Nama Pengguna")%>" required autocomplete="off">
            </div>

            <div class="input-group-custom-<%=rnd%>">
                <i class="fas fa-lock"></i>
                <input type="password" id="password<%=rnd%>" name="password" placeholder="<%=Common.getBahasaConfig("Kata Sandi")%>" required>
            </div>

            <button type="submit" id="btnSubmit<%=rnd%>" class="btn-login-<%=rnd%>">
                <i class="fas fa-sign-in-alt me-2"></i> <%=Common.getBahasaConfig("Masuk ke Sistem")%>
            </button>
        </form>

        <a href="<%=Common.ROOT%>/pustaka" class="back-link-<%=rnd%>">
            <i class="fas fa-arrow-left me-1"></i> <%=Common.getBahasaConfig("Kembali ke Halaman Utama")%>
        </a>
    </div>

    <script>
        async function prosesLogin<%=rnd%>(event) {
            event.preventDefault();
            
            const btnSubmit = document.getElementById('btnSubmit<%=rnd%>');
            const alertBox = document.getElementById('alertBox<%=rnd%>');
            const usernameVal = document.getElementById('username<%=rnd%>').value;
            const passwordVal = document.getElementById('password<%=rnd%>').value;

            // Atur status proses
            btnSubmit.disabled = true;
            btnSubmit.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i> <%=Common.getBahasaConfig("Memverifikasi...")%>';
            alertBox.style.display = 'none';
            alertBox.className = 'alert-<%=rnd%>';

            try {
                // Menyiapkan payload HTTP POST standar form-urlencoded
                const payload = new URLSearchParams();
                payload.append('username', usernameVal);
                payload.append('password', passwordVal);

                const response = await fetch('<%=Common.ROOT%>/pustaka?hanya_tampil_jsp=true&p=pustaka&s=_login_pustaka_service', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: payload.toString()
                });

                const result = await response.json();

                if (result.status === 'success') {
                    alertBox.classList.add('alert-success-<%=rnd%>');
                    alertBox.innerHTML = '<i class="fas fa-check-circle me-2"></i> ' + result.message;
                    alertBox.style.display = 'block';
                    
                    // Mengalihkan langsung ke halaman Beranda Anggota
                    setTimeout(() => {
                        window.location.href = '<%=Common.ROOT%>/pustaka?p=pustaka&s=beranda_anggota';
                    }, 1000);
                } else {
                    alertBox.classList.add('alert-danger-<%=rnd%>');
                    alertBox.innerHTML = '<i class="fas fa-exclamation-circle me-2"></i> ' + result.message;
                    alertBox.style.display = 'block';
                    
                    // Mengembalikan tombol jika gagal
                    btnSubmit.disabled = false;
                    btnSubmit.innerHTML = '<i class="fas fa-sign-in-alt me-2"></i> <%=Common.getBahasaConfig("Masuk ke Sistem")%>';
                }
            } catch (error) {
                alertBox.classList.add('alert-danger-<%=rnd%>');
                alertBox.innerHTML = '<i class="fas fa-wifi me-2"></i> <%=Common.getBahasaConfig("Gagal terhubung ke peladen. Periksa koneksi Anda.")%>';
                alertBox.style.display = 'block';
                
                btnSubmit.disabled = false;
                btnSubmit.innerHTML = '<i class="fas fa-sign-in-alt me-2"></i> <%=Common.getBahasaConfig("Masuk ke Sistem")%>';
            }
        }
    </script>
</body>
</html>