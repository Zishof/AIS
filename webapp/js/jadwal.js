/*
const dailyScheduleData = [
    {
        foto: 'img/dosen1.jpg',
        nama: 'Prof. Dr. Anya Geraldine, M.Kom',
        nidn: '1234567890',
        matakuliah: 'Pemrograman Web Lanjut',
        hari: 'Senin',
        jam: '09:00 - 11:00',
        ruangan: 'LAB Komputer 1'
    },
    {
        foto: 'img/dosen2.jpg',
        nama: 'Dr. Budi Santoso, M.Si',
        nidn: '0987654321',
        matakuliah: 'Basis Data',
        hari: 'Senin',
        jam: '13:00 - 15:00',
        ruangan: 'Ruang Teori 2'
    },
    {
        foto: 'img/dosen3.jpg',
        nama: 'Siti Aminah, S.T., M.Eng',
        nidn: '1122334455',
        matakuliah: 'Analisis Algoritma',
        hari: 'Selasa',
        jam: '10:00 - 12:00',
        ruangan: 'LAB Komputer 2'
    },
    {
        foto: 'img/dosen4.jpg',
        nama: 'Rudi Kurniawan, M.Sc',
        nidn: '6677889900',
        matakuliah: 'Jaringan Komputer',
        hari: 'Selasa',
        jam: '14:00 - 16:00',
        ruangan: 'Ruang Teori 3'
    },
    {
        foto: 'img/dosen5.jpg',
        nama: 'Dewi Lestari, Ph.D',
        nidn: '5432109876',
        matakuliah: 'Sistem Operasi',
        hari: 'Rabu',
        jam: '08:00 - 10:00',
        ruangan: 'LAB Komputer 1'
    },
    {
        foto: 'img/dosen6.jpg',
        nama: 'Agus Setiawan, S.Kom',
        nidn: '0099887766',
        matakuliah: 'Pengembangan Aplikasi Mobile',
        hari: 'Rabu',
        jam: '11:00 - 13:00',
        ruangan: 'Ruang Teori 2'
    }
];

const weeklyScheduleData = [
    {
        foto: 'img/dosen1.jpg',
        nama: 'Prof. Dr. Anya Geraldine, M.Kom',
        nidn: '1234567890',
        matakuliah: 'Seminar Teknologi Informasi',
        hari: 'Kamis',
        jam: '10:00 - 12:00',
        ruangan: 'Auditorium'
    },
    {
        foto: 'img/dosen3.jpg',
        nama: 'Siti Aminah, S.T., M.Eng',
        nidn: '1122334455',
        matakuliah: 'Proyek 1',
        hari: 'Jumat',
        jam: '09:00 - 12:00',
        ruangan: 'LAB Proyek'
    },
    {
        foto: 'img/dosen5.jpg',
        nama: 'Dewi Lestari, Ph.D',
        nidn: '5432109876',
        matakuliah: 'Metodologi Penelitian',
        hari: 'Kamis',
        jam: '13:00 - 15:00',
        ruangan: 'Ruang Teori 1'
    },
    {
        foto: 'img/dosen2.jpg',
        nama: 'Dr. Budi Santoso, M.Si',
        nidn: '0987654321',
        matakuliah: 'Statistika dan Probabilitas',
        hari: 'Jumat',
        jam: '14:00 - 16:00',
        ruangan: 'Ruang Teori 4'
    },
    {
        foto: 'img/dosen4.jpg',
        nama: 'Rudi Kurniawan, M.Sc',
        nidn: '6677889900',
        matakuliah: 'Keamanan Jaringan',
        hari: 'Rabu',
        jam: '14:00 - 16:00',
        ruangan: 'LAB Keamanan'
    },
    {
        foto: 'img/dosen6.jpg',
        nama: 'Agus Setiawan, S.Kom',
        nidn: '0099887766',
        matakuliah: 'Pengujian Perangkat Lunak',
        hari: 'Selasa',
        jam: '16:00 - 18:00',
        ruangan: 'Ruang Teori 5'
    }
];
*/

const dailyScheduleList = document.querySelector('.schedule-daily .schedule-list');
const weeklyScheduleList = document.querySelector('.schedule-weekly .schedule-list');
const itemsToShow = 3;
const switchInterval = 5000; // 3 detik

function displaySchedule(scheduleData, scheduleList) {
    scheduleList.innerHTML = ''; // Bersihkan daftar sebelumnya
    for (let i = 0; i < itemsToShow && i < scheduleData.length; i++) {
        const item = scheduleData[i];
        const listItem = document.createElement('div');
        listItem.classList.add('schedule-item');
        listItem.innerHTML = `
            <img src="${item.foto}" alt="${item.nama}">
            <div>
                <span><strong>Nama:</strong> ${item.nama}</span><br>
                <span><strong>NIDN:</strong> ${item.nidn}</span><br>
                <span><strong>Mata Kuliah:</strong> ${item.matakuliah}</span><br>
                <span><strong>Hari/Jam:</strong> ${item.hari}, ${item.jam}</span><br>
                <span><strong>Ruangan:</strong> ${item.ruangan}</span>
            </div>
        `;
        scheduleList.appendChild(listItem);
        setTimeout(() => listItem.classList.add('show'), i * 200); // Efek stagger
    }
}

function rotateSchedule(scheduleData, scheduleList) {
    let startIndex = 0;
    setInterval(() => {
        const endIndex = Math.min(startIndex + itemsToShow, scheduleData.length);
        const currentItems = scheduleData.slice(startIndex, endIndex);
        scheduleList.innerHTML = ''; // Bersihkan daftar sebelum menampilkan yang baru
        currentItems.forEach((item, index) => {
            const listItem = document.createElement('div');
            listItem.classList.add('schedule-item');
            listItem.innerHTML = `
                <img src="${item.foto}" alt="${item.nama}">
                <div>
                    <span><strong>Nama:</strong> ${item.nama}</span><br>
                    <span><strong>NIDN:</strong> ${item.nidn}</span><br>
                    <span><strong>Mata Kuliah:</strong> ${item.matakuliah}</span><br>
                    <span><strong>Hari/Jam:</strong> ${item.hari}, ${item.jam}</span><br>
                    <span><strong>Ruangan:</strong> ${item.ruangan}</span>
                </div>
            `;
            scheduleList.appendChild(listItem);
            setTimeout(() => listItem.classList.add('show'), index * 200); // Efek stagger
        });

        startIndex += itemsToShow;
        if (startIndex >= scheduleData.length) {
            startIndex = 0;
        }
    }, switchInterval);
}

rotateSchedule(dailyScheduleData, dailyScheduleList);
rotateSchedule(weeklyScheduleData, weeklyScheduleList);