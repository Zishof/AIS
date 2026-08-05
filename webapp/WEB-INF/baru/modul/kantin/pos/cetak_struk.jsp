<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
// Pengecekan Keamanan Akses
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	out.print("<div style='text-align:center; padding: 50px; font-family: sans-serif;'>" + Common.getBahasaConfig("Akses Ditolak. Sesi Anda telah berakhir.") + "</div>");
	return;
}

// Mengambil Parameter ID Pembelian (PembelianAnggotaKoperasi ID)
String idPembelian = request.getParameter("id");
String rnd = Common.getGeneratedBarCode(7);
%>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%=Common.getBahasaConfig("Cetak Struk")%></title>
    <style>
        /* Desain Struk Khusus Thermal Printer (Lebar ~80mm / 58mm POS) */
        /* Font tebal (700) + -webkit-text-stroke + print-color-adjust: cetakan thermal SANGAT sering
           keluar tipis/buram kalau font default (normal-weight, anti-aliased) dipakai apa adanya --
           browser merender teks dgn abu-abu di tepi glyph, lalu printer thermal menerjemahkan abu-abu
           itu jadi titik jarang (BUKAN hitam pekat). Ini bukan kosmetik layar, tapi menambah cakupan
           tinta per karakter di kertas. Sama persis dgn perbaikan di struk.js (Kasir Desktop) supaya
           ketiganya (web/Desktop/nanti Android) konsisten. */
        body {
            font-family: "Courier New", Courier, monospace;
            width: 300px;
            font-size: 14px;
            font-weight: 700;
            -webkit-text-stroke: 0.4px #000;
            margin: 0 auto;
            padding: 10px;
            color: #000;
            background: #fff;
            -webkit-print-color-adjust: exact;
            print-color-adjust: exact;
        }
        .text-center { text-align: center; }
        .text-right { text-align: right; }
        .bold { font-weight: 900; -webkit-text-stroke: 0.6px #000; }
        .border-top { border-top: 2px dashed #000; margin-top: 5px; padding-top: 5px; }
        .border-bottom { border-bottom: 2px dashed #000; margin-bottom: 5px; padding-bottom: 5px; }
        table { width: 100%; border-collapse: collapse; }
        td { vertical-align: top; }

        /* Hilangkan pesan loading saat mesin cetak dijalankan */
        @media print {
            .no-print { display: none !important; }
            body { padding: 0; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
        }
    </style>
</head>
<body>

    <div id="strukContainer<%=rnd%>">
        <div class="text-center py-5 no-print" style="margin-top: 50px;">
            <i><%=Common.getBahasaConfig("Menyiapkan format struk, mohon tunggu...")%></i>
        </div>
    </div>

    <script>
        const idPembelian<%=rnd%> = '<%=idPembelian%>';
        
        const formatRp<%=rnd%> = (angka) => {
            return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(angka || 0);
        };

        const loadStrukData<%=rnd%> = async () => {
            const container = document.getElementById('strukContainer<%=rnd%>');
            
            if (!idPembelian<%=rnd%> || idPembelian<%=rnd%> === 'null') {
                container.innerHTML = '<div class="text-center"><%=Common.getBahasaConfig("ID Transaksi tidak valid.")%></div>';
                return;
            }

            // Kueri pengambilan data Header (Pembelian Anggota Koperasi) dan Rincian (Pembelian)
            const sqlQuery = "SELECT pak.kode as kode_transaksi, " +
                             "TO_CHAR(pak.tanggal_pembayaran, 'DD-MM-YYYY HH24:MI') as waktu_transaksi, " +
                             "COALESCE(pak.bayar_tunai, 0) as uang_tunai, " +
                             "COALESCE(pak.kembalian, 0) as uang_kembalian, " +
                             "COALESCE(pak.total_biaya, 0) as total_bayar, " +
                             "COALESCE(pak.total_diskon, 0) as total_diskon_struk, " +
                             "COALESCE(pak.totalcashback, 0) as total_cashback_struk, " +
                             "pr.nama as nama_item, " +
                             "p.qty as jumlah, " +
                             "COALESCE(p.hargasatuan, (p.total / NULLIF(p.qty, 0)), 0) as harga, " +
                             "COALESCE(p.diskon, 0) as diskon_item, " +
                             "COALESCE(p.cashback, 0) as cashback_item, " +
                             "COALESCE(t.nama, 'Koperasi Utama') as nama_toko " +
                             "FROM koperasi.pembelian p " +
                             "LEFT JOIN koperasi.pembelian_anggota_koperasi pak ON p.pembelian_anggota_koperasi = pak.id " +
                             "LEFT JOIN koperasi.toko t ON p.toko = t.id " +
                             "LEFT JOIN koperasi.produk pr ON p.produk = pr.id " +
                             "WHERE p.pembelian_anggota_koperasi = " + idPembelian<%=rnd%> + " " +
                             "ORDER BY p.id ASC;";

            try {
                const response = await fetch('<%=Common.ROOT%>/Data', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ action: "sql", sql: sqlQuery })
                });
                
                const res = await response.json();
                const dataItems = res.data;

                if (!dataItems || dataItems.length === 0) {
                    container.innerHTML = '<div class="text-center"><%=Common.getBahasaConfig("Data pesanan tidak ditemukan atau kosong.")%></div>';
                    return;
                }

                // Header data diambil dari record/baris pertama
                const header = dataItems[0];
                const namaToko = header.nama_toko;
                const kodeTrx = header.kode_transaksi || '-';
                const waktuTrx = header.waktu_transaksi || '-';
                
                // Ringkasan tagihan didapatkan dari header 
                const uangTunai = parseFloat(header.uang_tunai);
                const kembalian = parseFloat(header.uang_kembalian);
                const totalBayar = parseFloat(header.total_bayar);
                const totalDiskonStruk = parseFloat(header.total_diskon_struk);
                const totalCashbackStruk = parseFloat(header.total_cashback_struk);

                // Mulai Membangun HTML Struk
                let htmlStruk = '';
                
                htmlStruk += '<div class="text-center bold border-bottom">';
                htmlStruk += '<h3 style="margin:5px 0;">' + namaToko + '</h3>';
                htmlStruk += '<div style="margin-bottom:5px;"><%=Common.getBahasaConfig("Struk Pembayaran")%></div>';
                htmlStruk += '</div>';
                
                htmlStruk += '<div style="margin-bottom:5px;">';
                htmlStruk += 'Waktu : ' + waktuTrx + '<br>';
                htmlStruk += 'No    : ' + kodeTrx + '<br>';
                htmlStruk += '</div>';
                
                htmlStruk += '<div class="border-top border-bottom">';
                htmlStruk += '<table>';

                let subtotalAwal = 0;

                // Looping Rincian Item (Keranjang)
                dataItems.forEach(item => {
                    const harga = parseFloat(item.harga || 0);
                    const qty = parseFloat(item.jumlah || 1);
                    const itemDiskon = parseFloat(item.diskon_item || 0);
                    const itemCashback = parseFloat(item.cashback_item || 0);
                    
                    const sub = harga * qty;
                    subtotalAwal += sub;
                    const subAfterDisc = sub - itemDiskon;
                    
                    htmlStruk += '<tr><td colspan="2">' + (item.nama_item || 'Item') + '</td></tr>';
                    htmlStruk += '<tr><td style="padding-bottom:5px;">' + qty + ' x ' + formatRp<%=rnd%>(harga) + '</td><td class="text-right" style="padding-bottom:5px;">' + formatRp<%=rnd%>(subAfterDisc) + '</td></tr>';
                    
                    // Tampilkan Note Promo di Struk Jika Ada
                    if(itemDiskon > 0) {
                        htmlStruk += '<tr><td colspan="2" style="padding-bottom:5px;">&nbsp;&nbsp;<i>Diskon Promo</i></td><td class="text-right">-' + formatRp<%=rnd%>(itemDiskon) + '</td></tr>';
                    }
                    if(itemCashback > 0) {
                        htmlStruk += '<tr><td colspan="2" style="padding-bottom:5px;">&nbsp;&nbsp;<i>Cashback Didapat</i></td><td class="text-right">+' + formatRp<%=rnd%>(itemCashback) + '</td></tr>';
                    }
                });
                
                htmlStruk += '</table></div>';
                
                // Ringkasan Akhir Transaksi
                htmlStruk += '<table>';
                htmlStruk += '<tr><td>Subtotal</td><td class="text-right">' + formatRp<%=rnd%>(subtotalAwal) + '</td></tr>';
                
                if (totalDiskonStruk > 0) {
                    htmlStruk += '<tr><td>Total Diskon</td><td class="text-right">-' + formatRp<%=rnd%>(totalDiskonStruk) + '</td></tr>';
                }
                
                htmlStruk += '<tr><td class="bold">Total Bayar</td><td class="text-right bold">' + formatRp<%=rnd%>(totalBayar) + '</td></tr>';
                
                if (totalCashbackStruk > 0) {
                    htmlStruk += '<tr><td class="bold">Total Cashback</td><td class="text-right bold">+' + formatRp<%=rnd%>(totalCashbackStruk) + '</td></tr>';
                }
                
                if (uangTunai > 0) {
                    htmlStruk += '<tr><td>Uang Tunai</td><td class="text-right">' + formatRp<%=rnd%>(uangTunai) + '</td></tr>';
                    htmlStruk += '<tr><td>Kembalian</td><td class="text-right">' + formatRp<%=rnd%>(kembalian) + '</td></tr>';
                }
                
                htmlStruk += '</table>';
                htmlStruk += '<div class="text-center border-top" style="margin-top: 15px;">Terima Kasih<br>Selamat Belanja Kembali</div>';

                // Terapkan ke kontainer layar
                container.innerHTML = htmlStruk;

                // Delay print sesaat agar browser sempat merender DOM sebelum jeda "Print Dialog"
                setTimeout(() => {
                    window.print();
                }, 600);

            } catch (error) {
                console.error("Fetch API Error:", error);
                container.innerHTML = '<div class="text-center text-danger"><%=Common.getBahasaConfig("Gagal terhubung ke server saat memuat data struk.")%></div>';
            }
        };

        // Eksekusi fungsi secara otomatis ketika jendela pop-up berhasil dimuat
        window.onload = loadStrukData<%=rnd%>;
    </script>
</body>
</html>