/* Shell TBU: menyuntik sidebar + topbar konsisten ke setiap halaman mockup.
   Setiap item nav: [id, label, ikon]. HREF otomatis = <id>.html (menyambung ke mockup).
   Halaman set window.TBU_PAGE = '<id>' sebelum memuat skrip ini agar item aktif ter-highlight. */
(function(){
  var NAV = [
    {items:[['dashboard','Beranda / Dasbor Pimpinan','fa-gauge-high']]},
    {group:'Akademik & Pembelajaran', items:[
      ['akademik','Akademik & KRS','fa-graduation-cap'],
      ['obe','Kurikulum OBE','fa-diagram-project'],
      ['lms','e-Learning / LMS','fa-laptop-code'],
      ['jadwal','Penjadwalan','fa-calendar-days'],
      ['kehadiran','Kehadiran','fa-fingerprint'],
      ['bimbingan','Bimbingan','fa-user-graduate'],
      ['sidang','Pengujian & Sidang','fa-gavel'],
      ['penilaian','Penilaian Terintegrasi','fa-square-poll-vertical'],
      ['pkl','PKL, KKN & Magang MBKM','fa-briefcase']
    ]},
    {group:'Penerimaan & Data', items:[
      ['pmb','PMB / RPL','fa-user-plus'],
      ['masterdata','Pendataan (Master Data)','fa-database'],
      ['setup','Setup & Utility','fa-sliders']
    ]},
    {group:'Keuangan & Usaha', items:[
      ['keuangan','Pembayaran & Tagihan','fa-credit-card'],
      ['ewallet','e-Wallet, POS & Koperasi','fa-wallet'],
      ['pos','POS & Perangkat Kasir','fa-cash-register'],
      ['beasiswa','Beasiswa','fa-hand-holding-heart'],
      ['akuntansi','Akuntansi Yayasan','fa-calculator'],
      ['anggaran','Anggaran & Realisasi','fa-money-bill-trend-up'],
      ['pengadaan','Pengadaan & Aset','fa-boxes-stacked'],
      ['dpc','Pembayaran Vendor (DPC)','fa-file-invoice-dollar']
    ]},
    {group:'Sumber Daya', items:[
      ['sdm','SDM & Kepegawaian','fa-users-gear'],
      ['payroll','Payroll','fa-money-check-dollar'],
      ['kinerja','Kinerja (SKP & KPI)','fa-chart-line'],
      ['edom','Angket & EDOM','fa-square-poll-horizontal'],
      ['sertifikat','Sertifikat & Pelatihan','fa-certificate'],
      ['kursus','Kursus Internal','fa-chalkboard-user'],
      ['prestasi','Prestasi','fa-trophy'],
      ['kedisiplinan','Kedisiplinan','fa-scale-balanced']
    ]},
    {group:'Penelitian & Mutu', items:[
      ['ppm','Penelitian & PPM','fa-flask'],
      ['repository','Repository','fa-server'],
      ['spmi','SPMI & AMI','fa-clipboard-check'],
      ['akreditasi','Akreditasi (LAM)','fa-award'],
      ['spi','Pengawasan Internal (SPI)','fa-user-shield'],
      ['dms','Manajemen Dokumen','fa-folder-tree']
    ]},
    {group:'Layanan & Fasilitas', items:[
      ['persuratan','Persuratan & Pengajuan','fa-envelope-open-text'],
      ['notifikasi','Notifikasi WhatsApp','fa-comment-dots'],
      ['wisuda','Kelulusan & Wisuda','fa-user-graduate'],
      ['alumni','Alumni & Tracer','fa-people-roof'],
      ['perpustakaan','Perpustakaan','fa-book-open'],
      ['pengaduan','Pengaduan Terpadu','fa-headset'],
      ['anjungan','Anjungan & Layanan','fa-desktop'],
      ['klinik','Klinik / RS','fa-hospital'],
      ['sirs','eMedic & SIRS (BPJS)','fa-notes-medical']
    ]},
    {group:'Kecerdasan Buatan & Otomasi', items:[
      ['ai','Asisten AI Terintegrasi','fa-wand-magic-sparkles']
    ]},
    {group:'Integrasi & Sistem', items:[
      ['neofeeder','Neo Feeder','fa-cloud-arrow-up'],
      ['sister','SISTER','fa-cloud-arrow-down'],
      ['portal','Multi-Portal & Kurikulum','fa-users-viewfinder']
    ]}
  ];

  function sidebar(active){
    var h = '<div class="brand"><img class="brand-full" src="assets/logo-tbu.png" alt="Taruna Bakti University"></div><nav class="nav">';
    NAV.forEach(function(sec){
      if(sec.group) h += '<div class="nav-group">'+sec.group+'</div>';
      sec.items.forEach(function(it){
        var on = it[0]===active?' active':'';
        h += '<a class="nav-item'+on+'" href="'+it[0]+'.html"><span class="ic"><i class="fa-solid '+it[2]+'"></i></span>'+it[1]+'<span class="chev"><i class="fa-solid fa-chevron-right"></i></span></a>';
      });
    });
    h += '<div class="nav-group">Sistem</div>';
    h += '<a class="nav-item'+(active==='mobile'?' active':'')+'" href="mobile.html"><span class="ic"><i class="fa-solid fa-mobile-screen-button"></i></span>Tampilan Mobile</a>';
    h += '<a class="nav-item'+(active==='index'?' active':'')+'" href="index.html"><span class="ic"><i class="fa-solid fa-table-cells-large"></i></span>Semua Modul (46)</a>';
    h += '<a class="nav-item" href="#"><span class="ic"><i class="fa-solid fa-gear"></i></span>Pengaturan</a></nav>';
    return h;
  }
  function topbar(){
    return '<button class="hamb" onclick="TBU.toggle()"><i class="fa-solid fa-bars"></i></button>'+
      '<div class="search"><i class="fa-solid fa-magnifying-glass"></i><input placeholder="Cari menu, data, mahasiswa, dosen, dokumen..."><span class="kbd">Ctrl K</span></div>'+
      '<div class="spacer"></div>'+
      '<button class="btn-qa"><i class="fa-solid fa-bolt"></i> Quick Action</button>'+
      '<a class="ico-btn" href="mobile.html" title="Buka Tampilan Mobile"><i class="fa-solid fa-mobile-screen-button"></i></a>'+
      '<button class="ico-btn"><i class="fa-regular fa-bell"></i><span class="dot">8</span></button>'+
      '<button class="ico-btn"><i class="fa-regular fa-envelope"></i><span class="dot">3</span></button>'+
      '<div class="prof"><div class="avatar">RS</div><div><b>Prof. Dr. Rahmat S.</b><span>Rektor TBU</span></div></div>';
  }
  window.TBU = {
    toggle:function(){document.querySelector('.sidebar').classList.toggle('open');document.querySelector('.backdrop').classList.toggle('show');},
    init:function(){
      var active = window.TBU_PAGE||'';
      var sb=document.querySelector('.sidebar'); if(sb) sb.innerHTML=sidebar(active);
      var tb=document.querySelector('.topbar'); if(tb) tb.innerHTML=topbar();
      if(!document.querySelector('.backdrop')){var b=document.createElement('div');b.className='backdrop';b.onclick=TBU.toggle;document.body.appendChild(b);}
    }
  };
  if(document.readyState!=='loading') TBU.init(); else document.addEventListener('DOMContentLoaded',TBU.init);
})();
