$(document).ready(function() {
    // Dummy Data
	/*
    const pengumumanData = [
        { id: 1, title: "Pengumuman Penerimaan Mahasiswa Baru Tahun Ajaran 2025/2026", cover: "https://via.placeholder.com/100/87CEEB/FFFFFF?Text=Pengumuman+1", content: "Dibuka pendaftaran mahasiswa baru untuk tahun ajaran 2025/2026. Informasi lebih lanjut dapat dilihat pada tautan berikut...", link: "#", lampiran: "dokumen1.pdf" },
        { id: 2, title: "Perubahan Jadwal Seleksi Penerimaan", cover: "https://via.placeholder.com/100/FFA07A/FFFFFF?Text=Pengumuman+2", content: "Terdapat perubahan jadwal seleksi penerimaan mahasiswa baru. Mohon perhatikan jadwal terbaru yang telah diumumkan.", link: "#", lampiran: null },
        { id: 3, title: "Informasi Webinar Mengenai Program Studi", cover: "https://via.placeholder.com/100/98FB98/FFFFFF?Text=Pengumuman+3", content: "Kami mengundang calon mahasiswa untuk mengikuti webinar informatif mengenai program studi yang tersedia di Universitas Demo.", link: "#", lampiran: null },
        { id: 4, title: "Pengumuman Hasil Seleksi Tahap 1", cover: "https://via.placeholder.com/100/ADD8E6/FFFFFF?Text=Pengumuman+4", content: "Pengumuman hasil seleksi tahap 1 penerimaan mahasiswa baru telah dapat dilihat di website resmi.", link: "#", lampiran: "hasil_seleksi_tahap1.pdf" },
        { id: 5, title: "Batas Akhir Pendaftaran Gelombang 1", cover: "https://via.placeholder.com/100/F08080/FFFFFF?Text=Pengumuman+5", content: "Diberitahukan bahwa batas akhir pendaftaran gelombang 1 adalah tanggal 30 April 2025.", link: "#", lampiran: null },
        { id: 6, title: "Sosialisasi Program Studi Teknik Informatika", cover: "https://via.placeholder.com/100/E0FFFF/FFFFFF?Text=Pengumuman+6", content: "Jangan lewatkan sosialisasi menarik mengenai Program Studi Teknik Informatika.", link: "#", lampiran: null },
        { id: 7, title: "Beasiswa untuk Calon Mahasiswa Berprestasi", cover: "https://via.placeholder.com/100/DDA0DD/FFFFFF?Text=Pengumuman+7", content: "Universitas Demo menyediakan beasiswa bagi calon mahasiswa berprestasi. Segera daftarkan diri Anda!", link: "#", lampiran: "info_beasiswa.pdf" },
        { id: 8, title: "Pembukaan Pendaftaran Gelombang 2", cover: "https://via.placeholder.com/100/AFEEEE/FFFFFF?Text=Pengumuman+8", content: "Pendaftaran mahasiswa baru gelombang 2 telah dibuka mulai tanggal 1 Mei 2025.", link: "#", lampiran: null }
    ];

    const pendaftaranData = [
        { id: 1, nama: "Jalur Reguler", cover: "https://via.placeholder.com/100/FFD700/FFFFFF?Text=Reguler", info: "Jalur pendaftaran reguler untuk semua program studi.", brosur: "brosur_reguler.pdf", biaya: "Rp 300.000" },
        { id: 2, nama: "Jalur Prestasi Akademik", cover: "https://via.placeholder.com/100/ADFF2F/FFFFFF?Text=Prestasi", info: "Jalur pendaftaran khusus bagi calon mahasiswa dengan prestasi akademik yang baik.", brosur: "brosur_prestasi.pdf", biaya: "Rp 200.000" },
        { id: 3, nama: "Jalur Prestasi Non-Akademik", cover: "https://via.placeholder.com/100/8FBC8F/FFFFFF?Text=Non-Akademik", info: "Jalur pendaftaran bagi calon mahasiswa yang memiliki prestasi di bidang non-akademik (olahraga, seni, dll.).", brosur: "brosur_non_akademik.pdf", biaya: "Rp 200.000" },
        { id: 4, nama: "Jalur KIP Kuliah", cover: "https://via.placeholder.com/100/4682B4/FFFFFF?Text=KIP", info: "Jalur pendaftaran melalui program Kartu Indonesia Pintar Kuliah (KIP Kuliah).", brosur: "brosur_kip_kuliah.pdf", biaya: "Gratis" },
        { id: 5, nama: "Jalur Transfer", cover: "https://via.placeholder.com/100/DAA520/FFFFFF?Text=Transfer", info: "Jalur pendaftaran bagi mahasiswa pindahan dari perguruan tinggi lain.", brosur: "brosur_transfer.pdf", biaya: "Rp 400.000" },
        { id: 6, nama: "Jalur Mandiri", cover: "https://via.placeholder.com/100/B0E0E6/FFFFFF?Text=Mandiri", info: "Jalur pendaftaran mandiri dengan persyaratan tertentu.", brosur: "brosur_mandiri.pdf", biaya: "Rp 500.000" },
        { id: 7, nama: "Jalur Internasional", cover: "https://via.placeholder.com/100/6495ED/FFFFFF?Text=Internasional", info: "Jalur pendaftaran khusus untuk calon mahasiswa internasional.", brosur: "brosur_internasional.pdf", biaya: "USD 50" },
        { id: 8, nama: "Jalur Afirmasi", cover: "https://via.placeholder.com/100/BC8F8F/FFFFFF?Text=Afirmasi", info: "Jalur pendaftaran untuk kelompok masyarakat tertentu.", brosur: "brosur_afirmasi.pdf", biaya: "Rp 150.000" }
    ];

    const prodiData = [
        { id: 1, nama: "Teknik Informatika", cover: "https://via.placeholder.com/100/008080/FFFFFF?Text=TI", deskripsi: "Mempelajari tentang pengembangan perangkat lunak, jaringan komputer, dan sistem informasi.", link: "#", lampiran: "kurikulum_ti.pdf", infoRinci: "Program studi Teknik Informatika memiliki fokus pada pengembangan aplikasi web, mobile, dan desktop. Lulusan memiliki peluang karir sebagai programmer, web developer, system analyst, dan lainnya." },
        { id: 2, nama: "Sistem Informasi", cover: "https://via.placeholder.com/100/2F4F4F/FFFFFF?Text=SI", deskripsi: "Mempelajari tentang perancangan, implementasi, dan pengelolaan sistem informasi dalam organisasi.", link: "#", lampiran: "kurikulum_si.pdf", infoRinci: "Program studi Sistem Informasi membekali mahasiswa dengan pengetahuan tentang basis data, analisis bisnis, dan manajemen proyek IT. Peluang karir meliputi business analyst, IT consultant, database administrator, dan sebagainya." },
        { id: 3, nama: "Manajemen", cover: "https://via.placeholder.com/100/556B2F/FFFFFF?Text=Manajemen", deskripsi: "Mempelajari tentang pengelolaan bisnis, pemasaran, keuangan, dan sumber daya manusia.", link: "#", lampiran: "kurikulum_manajemen.pdf", infoRinci: "Program studi Manajemen mempersiapkan mahasiswa untuk menjadi pemimpin dan manajer yang handal di berbagai jenis organisasi. Lulusan dapat berkarir di bidang marketing, finance, HR, dan operasional." },
        { id: 4, nama: "Akuntansi", cover: "https://via.placeholder.com/100/8B4513/FFFFFF?Text=Akuntansi", deskripsi: "Mempelajari tentang prinsip-prinsip akuntansi, pelaporan keuangan, dan audit.", link: "#", lampiran: "kurikulum_akuntansi.pdf", infoRinci: "Program studi Akuntansi menghasilkan lulusan yang kompeten dalam menyusun laporan keuangan, melakukan analisis keuangan, dan mengaudit perusahaan. Peluang karir meliputi akuntan publik, akuntan perusahaan, dan auditor." },
        { id: 5, nama: "Ilmu Komunikasi", cover: "https://via.placeholder.com/100/A0522D/FFFFFF?Text=IK", deskripsi: "Mempelajari tentang teori dan praktik komunikasi di berbagai konteks.", link: "#", lampiran: "kurikulum_ikom.pdf", infoRinci: "Program studi Ilmu Komunikasi membekali mahasiswa dengan keterampilan komunikasi interpersonal, komunikasi massa, dan public relations. Lulusan dapat berkarir sebagai jurnalis, PR specialist, content creator, dan lainnya." },
        { id: 6, nama: "Psikologi", cover: "https://via.placeholder.com/100/D2691E/FFFFFF?Text=Psikologi", deskripsi: "Mempelajari tentang perilaku dan proses mental manusia.", link: "#", lampiran: "kurikulum_psikologi.pdf", infoRinci: "Program studi Psikologi memberikan pemahaman mendalam tentang aspek-aspek psikologis manusia. Lulusan dapat berkarir sebagai psikolog, konselor, HR specialist, dan sebagainya." },
        { id: 7, nama: "Hukum", cover: "https://via.placeholder.com/100/B8860B/FFFFFF?Text=Hukum", deskripsi: "Mempelajari tentang sistem hukum dan peraturan perundang-undangan.", link: "#", lampiran: "kurikulum_hukum.pdf", infoRinci: "Program studi Hukum menghasilkan lulusan yang memiliki pemahaman tentang hukum dan mampu menerapkannya dalam berbagai bidang. Peluang karir meliputi pengacara, notaris, hakim, dan legal counsel." },
        { id: 8, nama: "Ekonomi Pembangunan", cover: "https://via.placeholder.com/100/CD853F/FFFFFF?Text=EP", deskripsi: "Mempelajari tentang teori dan kebijakan pembangunan ekonomi.", link: "#", lampiran: "kurikulum_ep.pdf", infoRinci: "Program studi Ekonomi Pembangunan mempersiapkan mahasiswa untuk menganalisis masalah-masalah ekonomi dan merumuskan kebijakan pembangunan. Lulusan dapat berkarir di instansi pemerintah, lembaga keuangan, dan organisasi non-profit." }
    ];

    const forumData = [
        { pengumumanId: 1, user: "Pengguna 1", date: "2 hari lalu", comment: "Saya ingin bertanya tentang persyaratan pendaftaran." },
        { pengumumanId: 1, user: "Admin", date: "1 hari lalu", comment: "Persyaratan pendaftaran dapat dilihat pada bagian jalur pendaftaran." },
        { pengumumanId: 1, user: "Pengguna 2", date: "Baru saja", comment: "Terima kasih atas informasinya!" },
        { pengumumanId: 3, user: "Calon Maba", date: "1 minggu lalu", comment: "Apakah webinar ini akan direkam dan bisa ditonton ulang?" },
        { pengumumanId: 3, user: "Panitia", date: "6 hari lalu", comment: "Ya, webinar akan direkam dan link rekamannya akan dibagikan setelah acara selesai." },
        { pengumumanId: 5, user: "Pendaftar", date: "3 hari lalu", comment: "Apakah ada perpanjangan batas akhir pendaftaran gelombang 1?" }
    ];
	*/
    const itemsPerPage = 5;
    let currentPagePengumuman = 1;
    let currentPagePendaftaran = 1;
    let currentPageProdi = 1;
    let currentPengumumanId = null;

    // Function to display pengumuman
    function displayPengumuman(page, data) {
        const startIndex = (page - 1) * itemsPerPage;
        const endIndex = startIndex + itemsPerPage;
        const currentData = data.slice(startIndex, endIndex);
        let html = '';
        if (currentData.length > 0) {
            currentData.forEach(item => {
                html += `
                    <div class="list-item">
                        <img src="${item.cover}" alt="Cover Pengumuman" class="item-cover">
                        <div class="item-details">
                            <h5>${item.title}</h5>
                            <p>${item.content.substring(0, 100)}...</p>
                            <button class="btn btn-info btn-sm btn-detail-pengumuman" data-id="${item.id}">Lihat Detail</button>
                        </div>
                    </div>
                `;
            });
        } else {
            html = '<p>Tidak ada pengumuman ditemukan.</p>';
        }
        $('#listPengumuman').html(html);
    }

    // Function to display pendaftaran
    function displayPendaftaran(page, data) {
        const startIndex = (page - 1) * itemsPerPage;
        const endIndex = startIndex + itemsPerPage;
        const currentData = data.slice(startIndex, endIndex);
        let html = '';
        if (currentData.length > 0) {
            currentData.forEach(item => {
                html += `
                    <div class="list-item">
                        <img src="${item.cover}" alt="Cover Jalur Pendaftaran" class="item-cover">
                        <div class="item-details">
                            <h5>${item.nama}</h5>
                            <p>${item.info.substring(0, 100)}...</p>
                            <button class="btn btn-info btn-sm btn-detail-pendaftaran" data-id="${item.id}">Lihat Detail</button>
                        </div>
                    </div>
                `;
            });
        } else {
            html = '<p>Tidak ada jalur pendaftaran ditemukan.</p>';
        }
        $('#listPendaftaran').html(html);
    }

    // Function to display prodi
    function displayProdi(page, data) {
        const startIndex = (page - 1) * itemsPerPage;
        const endIndex = startIndex + itemsPerPage;
        const currentData = data.slice(startIndex, endIndex);
        let html = '';
        if (currentData.length > 0) {
            currentData.forEach(item => {
                html += `
                    <div class="list-item">
                        <img src="${item.cover}" alt="Cover Informasi Prodi" class="item-cover">
                        <div class="item-details">
                            <h5>${item.nama}</h5>
                            <p>${item.deskripsi.substring(0, 100)}...</p>
                            <button class="btn btn-info btn-sm btn-detail-prodi" data-id="${item.id}">Lihat Detail</button>
                        </div>
                    </div>
                `;
            });
        } else {
            html = '<p>Tidak ada informasi prodi ditemukan.</p>';
        }
        $('#listProdi').html(html);
    }

    // Function to generate paging
    function generatePaging(totalPages, currentPage, targetId, displayFunction, data) {
        let paginationHtml = '';
        if (totalPages > 1) {
            paginationHtml += `<li class="page-item ${currentPage === 1 ? 'disabled' : ''}">
                                    <button class="page-link" data-page="${currentPage - 1}">Sebelumnya</button>
                                </li>`;
            for (let i = 1; i <= totalPages; i++) {
                paginationHtml += `<li class="page-item ${i === currentPage ? 'active' : ''}">
                                        <button class="page-link" data-page="${i}">${i}</button>
                                    </li>`;
            }
            paginationHtml += `<li class="page-item ${currentPage === totalPages ? 'disabled' : ''}">
                                    <button class="page-link" data-page="${currentPage + 1}">Selanjutnya</button>
                                </li>`;
        }
        $(`#${targetId}`).html(paginationHtml);
        $(`#${targetId} .page-link`).on('click', function() {
            const page = parseInt($(this).data('page'));
            if (targetId === 'pagingPengumuman') {
                currentPagePengumuman = page;
                displayPengumuman(currentPagePengumuman, filteredPengumumanData);
                generatePaging(Math.ceil(filteredPengumumanData.length / itemsPerPage), currentPagePengumuman, 'pagingPengumuman', displayPengumuman, filteredPengumumanData);
            } else if (targetId === 'pagingPendaftaran') {
                currentPagePendaftaran = page;
                displayPendaftaran(currentPagePendaftaran, filteredPendaftaranData);
                generatePaging(Math.ceil(filteredPendaftaranData.length / itemsPerPage), currentPagePendaftaran, 'pagingPendaftaran', displayPendaftaran, filteredPendaftaranData);
            } else if (targetId === 'pagingProdi') {
                currentPageProdi = page;
                displayProdi(currentPageProdi, filteredProdiData);
                generatePaging(Math.ceil(filteredProdiData.length / itemsPerPage), currentPageProdi, 'pagingProdi', displayProdi, filteredProdiData);
            }
        });
    }

    // Function to display forum comments
    function displayForum(pengumumanId) {
        const comments = forumData.filter(comment => comment.pengumumanId === pengumumanId);
        let html = '';
        comments.forEach(comment => {
            html += `
                <div class="media mb-3">
                    <img src="https://via.placeholder.com/50" class="mr-3 rounded-circle" alt="${comment.user}">
                    <div class="media-body">
                        <h6 class="mt-0">${comment.user} <small class="text-muted">${comment.date}</small></h6>
                        <p>${comment.comment}</p>
                    </div>
                </div>
            `;
        });
        $('#forumPengumuman').html(html);
    }

    // Initial display
    let filteredPengumumanData = [...pengumumanData];
    let filteredPendaftaranData = [...pendaftaranData];
    let filteredProdiData = [...prodiData];

    displayPengumuman(currentPagePengumuman, filteredPengumumanData);
    generatePaging(Math.ceil(filteredPengumumanData.length / itemsPerPage), currentPagePengumuman, 'pagingPengumuman', displayPengumuman, filteredPengumumanData);

    displayPendaftaran(currentPagePendaftaran, filteredPendaftaranData);
    generatePaging(Math.ceil(filteredPendaftaranData.length / itemsPerPage), currentPagePendaftaran, 'pagingPendaftaran', displayPendaftaran, filteredPendaftaranData);

    displayProdi(currentPageProdi, filteredProdiData);
    generatePaging(Math.ceil(filteredProdiData.length / itemsPerPage), currentPageProdi, 'pagingProdi', displayProdi, filteredProdiData);

    // Login Modal
    $('#loginBtn').click(function() {
        $('#loginModal').modal('show');
    });

    // Detail Pengumuman
    $('#listPengumuman').on('click', '.btn-detail-pengumuman', function() {
        const id = parseInt($(this).data('id'));
        currentPengumumanId = id;
        const pengumuman = pengumumanData.find(item => item.id === id);
        $('#pengumumanDetailCover').attr('src', pengumuman.cover);
        $('#pengumumanDetailTitle').text(pengumuman.title);
        $('#pengumumanDetailContent').html(pengumuman.content);
        if (pengumuman.lampiran) {
            $('#pengumumanDetailLink').html(`<a href="${pengumuman.lampiran}" target="_blank"><i class="fas fa-download"></i> Download Lampiran</a>`);
        } else {
            $('#pengumumanDetailLink').html('');
        }
        displayForum(id);
        $('#detailPengumumanModal').modal('show');
    });

    // Tambah Komentar Pengumuman
    $('#btnAddCommentPengumuman').click(function() {
        $('#addCommentPengumumanModal').modal('show');
    });

    $('#btnSaveCommentPengumuman').click(function() {
        const newCommentText = $('#newComment').val();
        if (newCommentText.trim() !== '') {
            const newComment = {
                pengumumanId: currentPengumumanId,
                user: "Pengguna Baru", // Anda bisa mengganti ini dengan informasi pengguna yang sebenarnya
                date: "Baru saja",
                comment: newCommentText
            };
            forumData.push(newComment);
            displayForum(currentPengumumanId);
            $('#newComment').val('');
            $('#addCommentPengumumanModal').modal('hide');
        } else {
            if (typeof tampilkanPesanGagalFormal === 'function') {
                tampilkanPesanGagalFormal(
                    "pengiriman komentar",
                    "Kolom komentar masih kosong, padahal wajib diisi sebelum dikirim.",
                    ["Isi kolom komentar terlebih dahulu.", "Setelah terisi, silakan klik tombol kirim/simpan kembali."]
                );
            } else {
                alert('Komentar tidak boleh kosong.');
            }
        }
    });

    // Detail Pendaftaran
    $('#listPendaftaran').on('click', '.btn-detail-pendaftaran', function() {
        const id = $(this).data('id');
        const pendaftaran = pendaftaranData.find(item => item.id === id);
        $('#pendaftaranDetailCover').attr('src', pendaftaran.cover);
        $('#pendaftaranDetailNama').text(pendaftaran.nama);
        $('#pendaftaranDetailInfo').text(pendaftaran.info);
        if (pendaftaran.brosur) {
            $('#pendaftaranDetailBrosur').html(`<a href="${pendaftaran.brosur}" target="_blank"><i class="fas fa-download"></i> Download Brosur</a>`);
        } else {
            $('#pendaftaranDetailBrosur').html('');
        }
        //$('#pendaftaranDetailBiaya').text(pendaftaran.biaya);
        $('#detailPendaftaranModal').modal('show');
    });

    // Detail Prodi
    $('#listProdi').on('click', '.btn-detail-prodi', function() {
        const id = $(this).data('id');
        const prodi = prodiData.find(item => item.id === id);
        $('#prodiDetailCover').attr('src', prodi.cover);
        $('#prodiDetailNama').text(prodi.nama);
        $('#prodiDetailDeskripsi').text(prodi.deskripsi);
        if (prodi.lampiran) {
            $('#prodiDetailLink').html(`<a href="${prodi.lampiran}" target="_blank"><i class="fas fa-download"></i> Download Lampiran</a>`);
        } else {
            $('#prodiDetailLink').html('');
        }
        $('#prodiDetailInfoRinci').html(prodi.infoRinci);
        $('#detailProdiModal').modal('show');
    });

    // Search Functionality
    $('#btnSearchPengumuman').click(function() {
        const searchTerm = $('#searchPengumuman').val().toLowerCase();
        filteredPengumumanData = pengumumanData.filter(item => item.title.toLowerCase().includes(searchTerm));
        currentPagePengumuman = 1;
        displayPengumuman(currentPagePengumuman, filteredPengumumanData);
        generatePaging(Math.ceil(filteredPengumumanData.length / itemsPerPage), currentPagePengumuman, 'pagingPengumuman', displayPengumuman, filteredPengumumanData);
    });

    $('#searchPengumuman').on('keypress', function(e) {
        if (e.which === 13) { // Enter key pressed
            $('#btnSearchPengumuman').click();
        }
    });

    $('#btnSearchPendaftaran').click(function() {
        const searchTerm = $('#searchPendaftaran').val().toLowerCase();
        filteredPendaftaranData = pendaftaranData.filter(item => item.nama.toLowerCase().includes(searchTerm));
        currentPagePendaftaran = 1;
        displayPendaftaran(currentPagePendaftaran, filteredPendaftaranData);
        generatePaging(Math.ceil(filteredPendaftaranData.length / itemsPerPage), currentPagePendaftaran, 'pagingPendaftaran', displayPendaftaran, filteredPendaftaranData);
    });

    $('#searchPendaftaran').on('keypress', function(e) {
        if (e.which === 13) { // Enter key pressed
            $('#btnSearchPendaftaran').click();
        }
    });

    $('#btnSearchProdi').click(function() {
        const searchTerm = $('#searchProdi').val().toLowerCase();
        filteredProdiData = prodiData.filter(item => item.nama.toLowerCase().includes(searchTerm) || item.deskripsi.toLowerCase().includes(searchTerm) || item.infoRinci.toLowerCase().includes(searchTerm));
        currentPageProdi = 1;
        displayProdi(currentPageProdi, filteredProdiData);
        generatePaging(Math.ceil(filteredProdiData.length / itemsPerPage), currentPageProdi, 'pagingProdi', displayProdi, filteredProdiData);
    });

    $('#searchProdi').on('keypress', function(e) {
        if (e.which === 13) { // Enter key pressed
            $('#btnSearchProdi').click();
        }
    });
});