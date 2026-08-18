document.addEventListener('DOMContentLoaded', function () {
    // Data contoh
	/*
    const pengumumanData = [
        { id: 1, judul: 'Pengumuman 1', isi: 'Isi pengumuman 1...', gambar: 'https://via.placeholder.com/100' },
        { id: 2, judul: 'Pengumuman 2', isi: 'Isi pengumuman 2...', gambar: 'https://via.placeholder.com/100' },
        { id: 3, judul: 'Pengumuman 3', isi: 'Isi pengumuman 3...', gambar: 'https://via.placeholder.com/100' },
        { id: 4, judul: 'Pengumuman 4', isi: 'Isi pengumuman 4...', gambar: 'https://via.placeholder.com/100' },
        { id: 5, judul: 'Pengumuman 5', isi: 'Isi pengumuman 5...', gambar: 'https://via.placeholder.com/100' },
        { id: 6, judul: 'Pengumuman 6', isi: 'Isi pengumuman 6...', gambar: 'https://via.placeholder.com/100' },
        { id: 7, judul: 'Pengumuman 7', isi: 'Isi pengumuman 7...', gambar: 'https://via.placeholder.com/100' },
        { id: 8, judul: 'Pengumuman 8', isi: 'Isi pengumuman 8...', gambar: 'https://via.placeholder.com/100' }
    ];

    const jalurData = [
        { id: 1, nama: 'Jalur 1', deskripsi: 'Deskripsi jalur 1...', gambar: 'https://via.placeholder.com/100' },
        { id: 2, nama: 'Jalur 2', deskripsi: 'Deskripsi jalur 2...', gambar: 'https://via.placeholder.com/100' },
        { id: 3, nama: 'Jalur 3', deskripsi: 'Deskripsi jalur 3...', gambar: 'https://via.placeholder.com/100' },
        { id: 4, nama: 'Jalur 4', deskripsi: 'Deskripsi jalur 4...', gambar: 'https://via.placeholder.com/100' },
        { id: 5, nama: 'Jalur 5', deskripsi: 'Deskripsi jalur 5...', gambar: 'https://via.placeholder.com/100' },
        { id: 6, nama: 'Jalur 6', deskripsi: 'Deskripsi jalur 6...', gambar: 'https://via.placeholder.com/100' },
        { id: 7, nama: 'Jalur 7', deskripsi: 'Deskripsi jalur 7...', gambar: 'https://via.placeholder.com/100' },
        { id: 8, nama: 'Jalur 8', deskripsi: 'Deskripsi jalur 8...', gambar: 'https://via.placeholder.com/100' }
    ];

    const prodiData = [
        { id: 1, nama: 'Prodi 1', info: 'Info prodi 1...', gambar: 'https://via.placeholder.com/100' },
        { id: 2, nama: 'Prodi 2', info: 'Info prodi 2...', gambar: 'https://via.placeholder.com/100' },
        { id: 3, nama: 'Prodi 3', info: 'Info prodi 3...', gambar: 'https://via.placeholder.com/100' },
        { id: 4, nama: 'Prodi 4', info: 'Info prodi 4...', gambar: 'https://via.placeholder.com/100' },
        { id: 5, nama: 'Prodi 5', info: 'Info prodi 5...', gambar: 'https://via.placeholder.com/100' },
        { id: 6, nama: 'Prodi 6', info: 'Info prodi 6...', gambar: 'https://via.placeholder.com/100' },
        { id: 7, nama: 'Prodi 7', info: 'Info prodi 7...', gambar: 'https://via.placeholder.com/100' },
        { id: 8, nama: 'Prodi 8', info: 'Info prodi 8...', gambar: 'https://via.placeholder.com/100' }
    ];
*/
    let currentPagePengumuman = 1;
    let currentPageJalur = 1;
    let currentPageProdi = 1;
    const itemsPerPage = 5;

    const daftarPengumumanDiv = document.getElementById('daftarPengumuman');
    const daftarJalurDiv = document.getElementById('daftarJalur');
    const daftarProdiDiv = document.getElementById('daftarProdi');
    const pengumumanPaginationUl = document.getElementById('pengumumanPagination');
    const jalurPaginationUl = document.getElementById('jalurPagination');
    const prodiPaginationUl = document.getElementById('prodiPagination');

    const detailPengumumanModal = new bootstrap.Modal(document.getElementById('detailPengumumanModal'));
    const detailJalurModal = new bootstrap.Modal(document.getElementById('detailJalurModal'));
    const detailProdiModal = new bootstrap.Modal(document.getElementById('detailProdiModal'));
    const detailPengumumanContentDiv = document.getElementById('detailPengumumanContent');
    const detailJalurContentDiv = document.getElementById('detailJalurContent');
    const detailProdiContentDiv = document.getElementById('detailProdiContent');

    // Fungsi untuk menampilkan daftar pengumuman
    function tampilkanPengumuman(page, searchTerm = '') {
        daftarPengumumanDiv.innerHTML = '';
        const filteredData = pengumumanData.filter(item =>
            item.judul.toLowerCase().includes(searchTerm.toLowerCase())
        );
        const startIndex = (page - 1) * itemsPerPage;
        const endIndex = startIndex + itemsPerPage;
        const paginatedData = filteredData.slice(startIndex, endIndex);

        paginatedData.forEach(pengumuman => {
            const itemDiv = document.createElement('div');
            itemDiv.classList.add('pengumuman-item');
            itemDiv.innerHTML = `
                <img src="${pengumuman.gambar}" alt="Cover Pengumuman">
                <div>
                    <h5>${pengumuman.judul}</h5>
                    <button class="btn btn-primary btn-sm detail-btn" data-id="${pengumuman.id}">Lihat Detail</button>
                </div>
            `;
            daftarPengumumanDiv.appendChild(itemDiv);
        });

        tampilkanPagination(page, filteredData.length, 'pengumuman');
    }

    // Fungsi untuk menampilkan daftar jalur pendaftaran
    function tampilkanJalur(page, searchTerm = '') {
        daftarJalurDiv.innerHTML = '';
        const filteredData = jalurData.filter(item =>
            item.nama.toLowerCase().includes(searchTerm.toLowerCase())
        );
        const startIndex = (page - 1) * itemsPerPage;
        const endIndex = startIndex + itemsPerPage;
        const paginatedData = filteredData.slice(startIndex, endIndex);

        paginatedData.forEach(jalur => {
            const itemDiv = document.createElement('div');
            itemDiv.classList.add('jalur-item');
            itemDiv.innerHTML = `
                <img src="${jalur.gambar}" alt="Cover Jalur">
                <div>
                    <h5>${jalur.nama}</h5>
                    <button class="btn btn-primary btn-sm detail-btn" data-id="${jalur.id}">Lihat Detail</button>
                </div>
            `;
            daftarJalurDiv.appendChild(itemDiv);
        });

        tampilkanPagination(page, filteredData.length, 'jalur');
    }

    // Fungsi untuk menampilkan daftar informasi prodi
    function tampilkanProdi(page, searchTerm = '') {
        daftarProdiDiv.innerHTML = '';
        const filteredData = prodiData.filter(item =>
            item.nama.toLowerCase().includes(searchTerm.toLowerCase())
        );
        const startIndex = (page - 1) * itemsPerPage;
        const endIndex = startIndex + itemsPerPage;
        const paginatedData = filteredData.slice(startIndex, endIndex);

        paginatedData.forEach(prodi => {
            const itemDiv = document.createElement('div');
            itemDiv.classList.add('prodi-item');
            itemDiv.innerHTML = `
                <img src="${prodi.gambar}" alt="Cover Prodi">
                <div>
                    <h5>${prodi.nama}</h5>
                    <button class="btn btn-primary btn-sm detail-btn" data-id="${prodi.id}">Lihat Detail</button>
                </div>
            `;
            daftarProdiDiv.appendChild(itemDiv);
        });

        tampilkanPagination(page, filteredData.length, 'prodi');
    }

    // Fungsi untuk menampilkan pagination
    function tampilkanPagination(currentPage, totalItems, type) {
        const totalPages = Math.ceil(totalItems / itemsPerPage);
        let paginationUl;

        if (type === 'pengumuman') {
            paginationUl = pengumumanPaginationUl;
            paginationUl.innerHTML = '';
            currentPagePengumuman = currentPage;
        } else if (type === 'jalur') {
            paginationUl = jalurPaginationUl;
            paginationUl.innerHTML = '';
            currentPageJalur = currentPage;
        } else if (type === 'prodi') {
            paginationUl = prodiPaginationUl;
            paginationUl.innerHTML = '';
            currentPageProdi = currentPage;
        }

        for (let i = 1; i <= totalPages; i++) {
            const pageLi = document.createElement('li');
            pageLi.classList.add('page-item');
            if (i === currentPage) {
                pageLi.classList.add('active');
            }
            const pageLink = document.createElement('a');
            pageLink.classList.add('page-link');
            pageLink.href = '#';
            pageLink.textContent = i;
            pageLink.addEventListener('click', function (e) {
                e.preventDefault();
                if (type === 'pengumuman') {
                    tampilkanPengumuman(i, document.getElementById('cariPengumuman').value);
                } else if (type === 'jalur') {
                    tampilkanJalur(i, document.getElementById('cariJalur').value);
                } else if (type === 'prodi') {
                    tampilkanProdi(i, document.getElementById('cariProdi').value);
                }
            });
            pageLi.appendChild(pageLink);
            paginationUl.appendChild(pageLi);
        }
    }

    // Event listener untuk pencarian pengumuman
    document.getElementById('cariPengumuman').addEventListener('input', function () {
        tampilkanPengumuman(1, this.value);
    });

    // Event listener untuk pencarian jalur pendaftaran
    document.getElementById('cariJalur').addEventListener('input', function () {
        tampilkanJalur(1, this.value);
    });

    // Event listener untuk pencarian informasi prodi
    document.getElementById('cariProdi').addEventListener('input', function () {
        tampilkanProdi(1, this.value);
    });

    // Event listener untuk detail pengumuman
    daftarPengumumanDiv.addEventListener('click', function (e) {
        if (e.target.classList.contains('detail-btn')) {
            const id = e.target.dataset.id;
            const pengumuman = pengumumanData.find(item => item.id === parseInt(id));
            if (pengumuman) {
                detailPengumumanContentDiv.innerHTML = `
                    <div class="row">
                        <div class="col-md-4">
                            <img src="${pengumuman.gambar}" alt="Gambar Pengumuman" class="img-fluid">
                        </div>
                        <div class="col-md-8">
                            <h4>${pengumuman.judul}</h4>
                            <p>${pengumuman.isi}</p>
                            <a href="#" class="btn btn-success btn-sm"><i class="fas fa-download"></i> Download Lampiran</a>
                            <hr>
                            <h5>Forum Tanya Jawab</h5>
                            <div class="forum">
                                <div class="komentar">
                                    <strong>Pengguna 1:</strong> Komentar 1...
                                </div>
                                <div class="komentar">
                                    <strong>Pengguna 2:</strong> Komentar 2...
                                </div>
                                <div class="komentar">
                                    <strong>Pengguna 3:</strong> Komentar 3...
                                </div>
                                <button class="btn btn-info btn-sm mt-2" data-bs-toggle="modal" data-bs-target="#tambahKomentarModal"><i class="fas fa-plus"></i> Tambah Komentar</button>
                            </div>
                        </div>
                    </div>
                `;
                detailPengumumanModal.show();
            }
        }
    });

    // Event listener untuk detail jalur pendaftaran
    daftarJalurDiv.addEventListener('click', function (e) {
        if (e.target.classList.contains('detail-btn')) {
            const id = e.target.dataset.id;
            const jalur = jalurData.find(item => item.id === parseInt(id));
            if (jalur) {
                detailJalurContentDiv.innerHTML = `
                    <div class="row">
                        <div class="col-md-4">
                            <img src="${jalur.gambar}" alt="Gambar Jalur" class="img-fluid">
                        </div>
                        <div class="col-md-8">
                            <h4>${jalur.nama}</h4>
                            <p>${jalur.deskripsi}</p>
                            <a href="#" class="btn btn-success btn-sm"><i class="fas fa-download"></i> Download Brosur</a>
                            <p>Biaya Pendaftaran: Rp. 100.000</p>
                            <button class="btn btn-primary">Daftar Sekarang</button>
                        </div>
                    </div>
                `;
                detailJalurModal.show();
            }
        }
    });

    // Event listener untuk detail prodi
    daftarProdiDiv.addEventListener('click', function (e) {
        if (e.target.classList.contains('detail-btn')) {
            const id = e.target.dataset.id;
            const prodi = prodiData.find(item => item.id === parseInt(id));
            if (prodi) {
                detailProdiContentDiv.innerHTML = `
                    <div class="row">
                        <div class="col-md-4">
                            <img src="${prodi.gambar}" alt="Gambar Prodi" class="img-fluid">
                        </div>
                        <div class="col-md-8">
                            <h4>${prodi.nama}</h4>
                            <p>${prodi.info}</p>
                            <a href="#" class="btn btn-success btn-sm"><i class="fas fa-download"></i> Download Lampiran</a>
                            <p>Informasi Rinci Prodi...</p>
                        </div>
                    </div>
                `;
                detailProdiModal.show();
            }
        }
    });

    // Inisialisasi tampilan awal
    tampilkanPengumuman(currentPagePengumuman);
    tampilkanJalur(currentPageJalur);
    tampilkanProdi(currentPageProdi);

    // Tombol Login
    document.getElementById('loginBtn').addEventListener('click', function () {
        new bootstrap.Modal(document.getElementById('loginModal')).show();
    });

    // Tombol Daftar Sekarang
    const daftarSekarangBtn = document.getElementById('daftarSekarangBtn');
    const jalurTabBtn = document.getElementById('jalur-tab');

    daftarSekarangBtn.addEventListener('click', function () {
        jalurTabBtn.click(); // Memilih tab jalur pendaftaran
        sembunyikanTombolDaftar(); // Sembunyikan tombol setelah diklik
    });

    // Fungsi untuk menyembunyikan tombol "Daftar Sekarang"
    function sembunyikanTombolDaftar() {
        daftarSekarangBtn.style.display = 'none';
    }

    // Fungsi untuk menampilkan tombol "Daftar Sekarang"
    function tampilkanTombolDaftar() {
        daftarSekarangBtn.style.display = 'block';
    }

    // Cek apakah tab jalur pendaftaran aktif saat halaman dimuat
    if (jalurTabBtn.classList.contains('active')) {
        sembunyikanTombolDaftar();
    }

    // Event listener untuk tab change
    const myTabEl = document.getElementById('myTab');
    myTabEl.addEventListener('shown.bs.tab', function (event) {
        if (event.target.id === 'jalur-tab') {
            sembunyikanTombolDaftar();
        } else {
            tampilkanTombolDaftar();
        }
    });

    // Event listener untuk form tambah komentar
    document.getElementById('formTambahKomentar').addEventListener('submit', function (e) {
        e.preventDefault();
        const namaPengguna = document.getElementById('namaPengguna').value;
        const isiKomentar = document.getElementById('isiKomentar').value;

        // Tambahkan logika untuk menyimpan komentar (misalnya, kirim ke server)
        console.log('Nama:', namaPengguna);
        console.log('Komentar:', isiKomentar);

        // Tutup modal setelah menyimpan komentar
        bootstrap.Modal.getInstance(document.getElementById('tambahKomentarModal')).hide();

        // Reset form
        document.getElementById('formTambahKomentar').reset();
    });
});