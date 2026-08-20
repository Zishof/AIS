<%@ page isELIgnored="true" %>
<%@page import="ais.common.Common"%>
<%
// Bantuan kontekstual modul Pengadaan untuk versi JSP.
//
// Satu berkas melayani ketujuh tahap; halaman pemanggil cukup menyertakan
// jsp:include dengan param "tahap" (pr, po, bast, tagihan, dpc, bdp, pajak).
//
// Isinya sepadan dengan spesifikasiBantuanMenu pada Desktop/Android, sehingga
// operator yang berpindah kanal membaca penjelasan dan istilah yang sama.
// Tombolnya mengambang agar tata letak halaman pemanggil tidak perlu diubah.
String tahapBantuan = request.getParameter("tahap");
if (tahapBantuan == null || tahapBantuan.trim().isEmpty()) {
	tahapBantuan = "pr";
}
%>
<button type="button" class="btn btn-primary rounded-circle shadow"
        style="position:fixed; right:22px; bottom:22px; width:52px; height:52px; z-index:1040;"
        title="<%=Common.getBahasaConfig("Bantuan")%>"
        onclick="bukaBantuanPengadaan('<%=tahapBantuan%>')">
  <i class="fas fa-question"></i>
</button>

<div class="modal fade" id="bantuanPengadaanModal" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-dialog-scrollable">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="bantuanPengadaanJudul"><%=Common.getBahasaConfig("Bantuan")%></h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body" id="bantuanPengadaanIsi"></div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Tutup")%></button>
      </div>
    </div>
  </div>
</div>

<script>
(function(){
  if (window.bukaBantuanPengadaan) { return; }

  var BANTUAN = {
    pr: {
      judul: "Permintaan Pembelian (PR)",
      tujuan: "Mengajukan kebutuhan barang toko sebelum dipesan ke penyedia.",
      langkah: ["Buat PR", "Pilih barang", "Isi jumlah dan harga", "Ajukan", "Disetujui atau ditolak"],
      istilah: [
        "DRAFT = masih dapat diubah dan dihapus",
        "DISETUJUI = terkunci, menjadi dasar pembuatan PO",
        "TUTUP = permintaan tidak dilanjutkan lagi"
      ],
      penting: [
        "Nilai PR dihitung ulang server dari barisnya, jadi total dokumen selalu sama dengan rinciannya.",
        "PR yang sudah disetujui tidak dapat diubah; batalkan keputusannya dulu bila perlu dikoreksi.",
        "Menolak wajib menyertakan alasan minimal 5 karakter supaya pembuat PR tahu apa yang harus diperbaiki."
      ],
      tanya: [
        ["Kenapa nomor PR tidak bisa diketik sendiri?",
         "Nomor dibuat otomatis dengan pola PR/toko/periode/urut agar tidak kembar antar toko dan antar bulan."],
        ["Barang yang saya cari tidak muncul.",
         "Pemilih barang mengambil dari Produk POS milik toko Anda. Bila belum ada, daftarkan dulu di menu Produk."]
      ]
    },
    po: {
      judul: "Pemesanan Pembelian (PO)",
      tujuan: "Memesan barang ke penyedia, sekali bayar maupun bertahap dengan termin.",
      langkah: ["Buat PO atau ambil dari PR", "Pilih penyedia", "Isi baris barang", "Atur termin bila bertahap", "Ajukan dan setujui"],
      istilah: [
        "Termin = pembayaran bertahap sesuai jadwal",
        "DPP = nilai dasar pengenaan pajak, yaitu nilai penagihan termin",
        "DP = uang muka, hanya berlaku pada PO tanpa termin",
        "LUNAS = seluruh nilai PO sudah dibayar dan disetujui"
      ],
      penting: [
        "Jumlah seluruh termin wajib sama dengan nilai PO; selisih lebih dari Rp 1 ditolak saat menyimpan.",
        "DP dan termin saling meniadakan. Bila memakai termin, tuliskan uang mukanya sebagai termin pertama.",
        "PO yang sudah disetujui atau sudah menerima pembayaran tidak dapat diubah.",
        "PPh dan PPN diisi di sini per termin, dan itulah yang nanti muncul di menu Bayar Pajak."
      ],
      tanya: [
        ["Apa beda Dari PR dengan Buat PO?",
         "Dari PR mengambil sisa yang belum dipesan dari permintaan yang sudah disetujui, sehingga satu PR bisa dipecah menjadi beberapa PO tanpa kelebihan pesan. Buat PO untuk pesanan langsung tanpa permintaan."],
        ["Tombol Bagi Rata itu untuk apa?",
         "Membagi nilai PO rata ke seluruh termin. Pembulatannya dibebankan ke termin terakhir supaya jumlahnya tepat sama dengan nilai PO."]
      ]
    },
    bast: {
      judul: "Penerimaan Barang (BAST)",
      tujuan: "Mencatat barang yang datang dari penyedia dan memasukkannya ke stok toko.",
      langkah: ["Terima dari PO atau terima langsung", "Periksa jumlah yang datang", "Isi harga, potongan, dan PPN", "Setujui", "Sinkronkan ke stok Kulakan"],
      istilah: [
        "BAST = Berita Acara Serah Terima, bukti barang sudah diterima",
        "Sisa boleh diterima = jumlah dipesan dikurangi yang sudah diterima dokumen lain",
        "Tanpa PO = penerimaan langsung untuk pembelian toko tanpa pesanan"
      ],
      penting: [
        "Jumlah diterima tidak boleh melebihi sisa yang dipesan; angka sisanya tertera pada tiap baris.",
        "Satu PO boleh diterima bertahap bila barang datang beberapa kali.",
        "Stok baru bertambah setelah BAST disetujui DAN disinkronkan ke Kulakan.",
        "Sinkronisasi hanya dapat dilakukan sekali; pengulangan ditolak agar stok tidak tergandakan."
      ],
      tanya: [
        ["Kenapa tidak ada tombol Tolak seperti di PR dan PO?",
         "Penerimaan barang tidak mengenal penolakan. Bila keliru, perbaiki dokumennya atau hapus selama masih berstatus DRAFT."],
        ["Sinkronisasi gagal karena barang belum berpadanan produk toko.",
         "Barisnya menunjuk barang inventaris yang belum punya padanan Produk POS. Buat dokumennya dari daftar Produk POS, atau petakan barang tersebut lebih dulu."]
      ]
    },
    tagihan: {
      judul: "Terima Tagihan Vendor",
      tujuan: "Mencatat nomor dan tanggal faktur vendor atas barang yang sudah diterima.",
      langkah: ["Pilih penerimaan yang sudah disetujui", "Isi nomor faktur", "Isi tanggal faktur", "Simpan tagihan"],
      istilah: [
        "BELUM DITAGIH = barang sudah diterima tetapi fakturnya belum masuk",
        "SUDAH DITAGIH = nomor dan tanggal faktur sudah tercatat"
      ],
      penting: [
        "Hanya penerimaan yang sudah DISETUJUI yang muncul di sini; barang yang belum diakui diterima tidak boleh menimbulkan kewajiban bayar.",
        "Nomor dan tanggal faktur keduanya wajib karena menjadi rujukan pembayaran.",
        "Nomor faktur yang sama pada penyedia yang sama akan ditolak untuk mencegah tagihan berganda.",
        "Tagihan tidak dapat dibatalkan bila pesanannya sudah menerima pembayaran."
      ],
      tanya: [
        ["Penerimaan saya tidak muncul di daftar.",
         "Pastikan BAST-nya sudah disetujui. Selama masih DRAFT, dokumen itu belum boleh ditagihkan."]
      ]
    },
    dpc: {
      judul: "Pembayaran Vendor",
      tujuan: "Membayar tagihan penyedia atas pesanan yang sudah disetujui.",
      langkah: ["Pilih vendor", "Centang tagihan yang dibayar", "Isi nilai bayar", "Simpan", "Setujui dan pilih apakah diajukan transfer"],
      istilah: [
        "Tagihan terbuka = termin yang masih menyisakan kewajiban bayar",
        "Sisa = nilai tagih dikurangi yang sudah dibayar dokumen lain",
        "Pengajuan transfer = permintaan pencairan yang masuk antrean keuangan"
      ],
      penting: [
        "Dokumen DRAFT belum diakui sebagai pembayaran; status pesanan baru berubah setelah DISETUJUI.",
        "Nilai bayar tidak boleh melebihi sisa tagihan terminnya.",
        "Pembayaran yang sudah disetujui tidak dapat diubah maupun dihapus; batalkan persetujuannya dulu.",
        "Ajukan transfer bank hanya bila dibayar lewat transfer. Pembayaran tunai tidak perlu masuk antrean pencairan."
      ],
      tanya: [
        ["Saya sudah menyimpan pembayaran, kenapa PO belum berkurang?",
         "Pembayaran baru diakui setelah disetujui. Selama masih draf, kolom dibayar pada PO memang belum berubah."],
        ["Bisakah membayar sebagian dari satu termin?",
         "Bisa. Kurangi nilai bayarnya, dan sisanya tetap muncul sebagai tagihan terbuka pada pembayaran berikutnya."]
      ]
    },
    bdp: {
      judul: "Barang Dalam Proses",
      tujuan: "Memantau barang yang sudah dipesan tetapi belum diterima.",
      langkah: ["Buka daftar", "Saring yang terlambat", "Periksa umur pesanan", "Tindak lanjuti ke penyedia"],
      istilah: [
        "Belum datang = jumlah dipesan dikurangi yang sudah diterima",
        "Umur = berapa hari sejak pesanan dibuat",
        "Terlambat = sudah melewati batas kirim yang disepakati"
      ],
      penting: [
        "Halaman ini tidak dapat diubah; isinya dihitung dari selisih pesanan dan penerimaan.",
        "Angkanya memakai definisi yang sama dengan pagar penerimaan, jadi tidak akan berbeda dengan sisa yang boleh diterima di layar BAST."
      ],
      tanya: []
    },
    pajak: {
      judul: "Bayar Pajak",
      tujuan: "Menyetor PPh yang dipotong dan mencatat PPN dari pembayaran vendor.",
      langkah: ["Buka tab Terutang", "Centang baris yang disetor", "Isi NTPN dan tanggal setor", "Setor", "Periksa di tab Riwayat"],
      istilah: [
        "DPP = dasar pengenaan pajak, yaitu nilai penagihan termin",
        "PPh = pajak yang DIPOTONG dari pembayaran dan disetor ke negara",
        "PPN = pajak masukan yang DIBAYARKAN kepada vendor bersama tagihan",
        "NTPN = Nomor Transaksi Penerimaan Negara, bukti setoran diterima kas negara",
        "Terutang = pajak yang sudah timbul tetapi belum disetor"
      ],
      penting: [
        "PPN menambah tagihan ke vendor, sedangkan PPh dipotong dari kas yang keluar. Keduanya mudah tertukar, jadi ditampilkan terpisah.",
        "Pajak baru menjadi terutang setelah pembayaran vendor disetujui.",
        "Nilainya dihitung sebanding dengan porsi yang benar-benar dibayar, sehingga pembayaran sebagian tidak menyetorkan pajak atas nilai yang belum dibayar.",
        "NTPN dan tanggal setor wajib diisi karena keduanya bukti bahwa uangnya masuk kas negara.",
        "Baris yang sudah disetor tidak dapat disetor ulang."
      ],
      tanya: [
        ["Saya salah mengisi NTPN, bagaimana memperbaikinya?",
         "Batalkan setorannya di tab Riwayat. Rekamannya dinonaktifkan tanpa dihapus, dan pajaknya kembali menjadi terutang sehingga dapat disetor ulang dengan bukti yang benar."],
        ["Kenapa jenis pajaknya kosong?",
         "Jenis PPh dan PPN ditetapkan per termin saat membuat PO. Bila kosong, lengkapi dulu pada pesanan yang bersangkutan."]
      ]
    }
  };

  var RANTAI = ["Permintaan Pembelian", "Pemesanan Pembelian", "Penerimaan Barang",
                "Terima Tagihan", "Bayar Tagihan", "Bayar Pajak"];
  var URUT = { pr:0, po:1, bast:2, tagihan:3, dpc:4, pajak:5 };

  function esc(s){ return (s==null?"":String(s)).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;"); }

  function daftar(judul, isi){
    if (!isi || !isi.length) return "";
    var h = '<h6 class="fw-bold mt-3 mb-2">' + esc(judul) + '</h6><ul class="mb-0">';
    for (var i=0;i<isi.length;i++){ h += '<li class="mb-1">' + esc(isi[i]) + '</li>'; }
    return h + '</ul>';
  }

  window.bukaBantuanPengadaan = function(tahap){
    var b = BANTUAN[tahap] || BANTUAN.pr;
    document.getElementById("bantuanPengadaanJudul").textContent = "Bantuan - " + b.judul;

    var h = '<p class="mb-3">' + esc(b.tujuan) + '</p>';

    // Pita rantai: menunjukkan posisi tahap ini di antara keenam tahap lain,
    // supaya operator tahu apa yang mendahului dan apa yang menyusul.
    var posisi = URUT[tahap];
    h += '<div class="d-flex flex-wrap gap-1 mb-3">';
    for (var r=0;r<RANTAI.length;r++){
      var aktif = (posisi === r);
      h += '<span class="badge ' + (aktif ? "bg-primary" : "bg-light text-dark border")
         + '">' + (r+1) + '. ' + esc(RANTAI[r]) + '</span>';
    }
    h += '</div>';
    if (posisi === undefined){
      h += '<div class="alert alert-light small">Halaman ini berjalan di samping rantai utama, bukan salah satu tahapnya.</div>';
    }

    h += daftar("Langkah", b.langkah);
    h += daftar("Kamus istilah", b.istilah);
    h += daftar("Perlu diperhatikan", b.penting);

    if (b.tanya && b.tanya.length){
      h += '<h6 class="fw-bold mt-3 mb-2">Tanya jawab</h6>';
      for (var t=0;t<b.tanya.length;t++){
        h += '<div class="mb-2"><div class="fw-bold small">' + esc(b.tanya[t][0]) + '</div>'
           + '<div class="small text-muted">' + esc(b.tanya[t][1]) + '</div></div>';
      }
    }

    document.getElementById("bantuanPengadaanIsi").innerHTML = h;
    new bootstrap.Modal(document.getElementById("bantuanPengadaanModal")).show();
  };
})();
</script>
