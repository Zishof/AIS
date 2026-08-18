package ais.action.master.helper;

import ais.common.Common;
import ais.database.model.Mahasiswa;

/**
 * Penyusun surat elektronik "Broadcast Email ke Atasan Alumni" (Penilaian Pengguna Lulusan): subjek + isi
 * surat FORMAL (default >1000 kata) dipersonalisasi dengan data alumni & atasan, menyisipkan tautan
 * kuesioner pengguna-lulusan. Isi default dapat dioverride via konfigurasi {@code broadcast_email_atasan_body}
 * (placeholder {{namaAtasan}}, {{peranAtasan}}, {{namaPerguruan}}, {{namaAlumni}}, {{namaProdi}},
 * {{namaFakultas}}, {{tahunLulus}}, {{linkKuesioner}}).
 */
public class BroadcastAtasanEmailHelper {

    private BroadcastAtasanEmailHelper() {
    }

    /** Isi surat bawaan (>1000 kata). Newline dikonversi menjadi <br> saat dikirim sebagai email HTML. */
    private static final String DEFAULT_BODY = "Yth. Bapak/Ibu {{namaAtasan}}\n{{peranAtasan}}\ndi Tempat\n\nAssalamu'alaikum warahmatullahi wabarakatuh. Salam sejahtera dan salam hormat kami sampaikan, semoga Bapak/Ibu senantiasa berada dalam keadaan sehat wal'afiat serta sukses dalam menjalankan segala aktivitas dan amanah yang diemban.\n\nPerkenankan kami, segenap pimpinan dan pengelola {{namaPerguruan}}, menyampaikan surat elektronik ini kepada Bapak/Ibu selaku atasan, pimpinan, atau pihak yang membina dan menaungi Saudara/i {{namaAlumni}}, alumnus/alumna Program Studi {{namaProdi}}, {{namaFakultas}}, yang telah menyelesaikan studi dan dinyatakan lulus pada tahun {{tahunLulus}}. Melalui kesempatan yang berbahagia ini, izinkanlah kami menyampaikan maksud dan tujuan kami, sekaligus memohon kesediaan serta partisipasi Bapak/Ibu dalam sebuah kegiatan penting yang sedang kami laksanakan, yaitu Studi Pelacakan Lulusan (Tracer Study) khususnya pada dimensi Penilaian Pengguna Lulusan (User Survey).\n\nSebagaimana Bapak/Ibu maklumi, sebuah perguruan tinggi memikul tanggung jawab moral dan akademik untuk senantiasa memastikan bahwa lulusan yang dihasilkannya benar-benar memiliki kompetensi, karakter, dan kesiapan yang relevan dengan kebutuhan dunia kerja serta perkembangan masyarakat. Untuk dapat mengukur sejauh mana keberhasilan tersebut, kami sangat membutuhkan gambaran yang jujur, objektif, dan menyeluruh mengenai kinerja nyata para lulusan kami di lapangan. Dan tidak ada pihak yang lebih memahami kondisi tersebut selain Bapak/Ibu sendiri, sebagai atasan langsung maupun pihak yang setiap hari menyaksikan, membimbing, serta menilai kiprah Saudara/i {{namaAlumni}} di tempat kerja. Oleh karena itulah, penilaian dan masukan dari Bapak/Ibu memiliki nilai yang teramat berharga, bahkan tidak tergantikan, bagi kemajuan dan perbaikan mutu pendidikan di lingkungan kami.\n\nPerlu kami sampaikan bahwa masukan dari Bapak/Ibu tidak akan berhenti sebagai sekadar data administratif belaka. Setiap tanggapan, penilaian, kritik, maupun saran yang Bapak/Ibu berikan akan kami olah, kaji, dan jadikan bahan pertimbangan utama dalam proses evaluasi kurikulum, peninjauan capaian pembelajaran, penguatan kompetensi lulusan, serta penyempurnaan berbagai program akademik dan non-akademik di institusi kami. Dengan kata lain, partisipasi Bapak/Ibu hari ini adalah investasi nyata bagi lahirnya lulusan-lulusan berikutnya yang lebih unggul, lebih siap kerja, lebih profesional, dan lebih bermanfaat bagi organisasi tempat mereka mengabdi kelak. Boleh dikatakan, melalui secarik penilaian yang Bapak/Ibu sampaikan, Bapak/Ibu turut serta membangun masa depan dunia pendidikan sekaligus memperbaiki kualitas sumber daya manusia yang akan Bapak/Ibu terima di masa mendatang.\n\nKami menyadari sepenuhnya bahwa Bapak/Ibu adalah pribadi yang sibuk, dengan tanggung jawab dan agenda yang padat setiap harinya. Justru karena kesadaran itulah, kami telah berupaya sebaik mungkin merancang instrumen kuesioner ini agar ringkas, praktis, dan mudah diisi, tanpa mengurangi kualitas informasi yang kami butuhkan. Pengisian kuesioner ini diperkirakan hanya memerlukan waktu sekitar 5 (lima) hingga 10 (sepuluh) menit saja, dan dapat Bapak/Ibu lakukan kapan pun serta di mana pun melalui perangkat telepon genggam, komputer, maupun tablet, selama terhubung dengan jaringan internet. Kami sungguh berharap, di tengah kesibukan Bapak/Ibu yang padat, sudilah kiranya Bapak/Ibu meluangkan waktu barang sejenak untuk berkenan mengisi angket penilaian pengguna lulusan ini.\n\nAdapun materi pertanyaan yang kami sajikan dalam kuesioner ini telah disusun secara cermat oleh tim pengelola dan admin kami, dengan mempertimbangkan aspek-aspek yang paling esensial dalam menilai kinerja seorang lulusan di dunia kerja. Di antara hal-hal yang akan kami tanyakan meliputi: integritas dan etika kerja yang bersangkutan, keahlian dan penguasaan bidang ilmu (profesionalisme), kemampuan berbahasa asing, kemampuan pemanfaatan teknologi informasi, kemampuan berkomunikasi, kemampuan bekerja sama dalam tim, kemampuan mengembangkan diri, kedisiplinan, tanggung jawab, kepemimpinan, serta kesesuaian antara bidang studi yang ditempuh dengan bidang pekerjaan yang dijalani. Melalui butir-butir pertanyaan tersebut, kami berharap dapat memperoleh potret yang utuh dan seimbang mengenai kualitas lulusan kami sebagaimana yang Bapak/Ibu rasakan dan saksikan secara langsung.\n\nSehubungan dengan hal tersebut, dengan segala kerendahan hati kami memohon kesediaan Bapak/Ibu untuk berkenan mengisi kuesioner Penilaian Pengguna Lulusan melalui tautan (link) resmi yang telah kami sediakan secara khusus di bawah ini:\n\n{{linkKuesioner}}\n\nTautan tersebut bersifat khusus dan personal, telah kami tautkan langsung dengan data Saudara/i {{namaAlumni}}, sehingga Bapak/Ibu tidak perlu lagi mengisi data identitas lulusan secara manual. Bapak/Ibu cukup mengklik tautan tersebut, lalu menjawab setiap pertanyaan yang muncul sesuai dengan pengamatan, pengalaman, dan penilaian Bapak/Ibu yang sebenar-benarnya terhadap yang bersangkutan selama bekerja di bawah bimbingan dan naungan Bapak/Ibu.\n\nPerlu kami tegaskan pula, seluruh data dan informasi yang Bapak/Ibu sampaikan melalui kuesioner ini akan kami jaga kerahasiaannya dengan sebaik-baiknya, serta semata-mata akan kami pergunakan untuk kepentingan akademik, evaluasi mutu, dan pengembangan institusi. Kami menjunjung tinggi prinsip kerahasiaan dan etika penelitian, sehingga Bapak/Ibu tidak perlu merasa ragu, khawatir, ataupun sungkan dalam memberikan penilaian yang objektif dan apa adanya. Justru kejujuran dan objektivitas Bapak/Ibu-lah yang paling kami harapkan, sebab dari sanalah kami dapat mengenali kekurangan untuk kami perbaiki, sekaligus mengenali kelebihan untuk kami pertahankan dan tingkatkan.\n\nKami juga ingin menyampaikan bahwa keberadaan Saudara/i {{namaAlumni}} di tengah-tengah organisasi Bapak/Ibu adalah sebuah kehormatan sekaligus kebanggaan tersendiri bagi kami. Kami sungguh berharap yang bersangkutan dapat memberikan kontribusi terbaiknya, menjadi pribadi yang amanah, cakap, dan bermanfaat, serta senantiasa menjaga nama baik almamater di mana pun ia berada. Apabila selama ini terdapat kekurangan, kelemahan, ataupun hal-hal yang masih perlu dibenahi dari yang bersangkutan, kami memohon maaf yang sebesar-besarnya, dan dengan penuh kelapangan hati kami mengharapkan koreksi serta bimbingan dari Bapak/Ibu. Sebaliknya, apabila yang bersangkutan telah menunjukkan kinerja yang baik, kami turut bersyukur dan berterima kasih atas segala bimbingan, arahan, serta kesempatan yang telah Bapak/Ibu berikan kepadanya.\n\nBesar harapan kami, jalinan silaturahmi dan kerja sama yang baik antara institusi kami dengan lembaga/organisasi yang Bapak/Ibu pimpin dapat terus terjalin dan terpelihara di masa-masa mendatang. Kami sangat terbuka terhadap segala bentuk kemitraan, baik dalam hal penyerapan lulusan, kegiatan magang, praktik kerja lapangan, penelitian bersama, pengabdian kepada masyarakat, maupun berbagai bentuk kolaborasi lainnya yang dapat memberikan manfaat timbal balik bagi kedua belah pihak. Sekali lagi, partisipasi Bapak/Ibu dalam pengisian kuesioner ini merupakan langkah awal yang sangat berarti bagi terwujudnya kemitraan yang lebih erat dan berkelanjutan tersebut.\n\nDemikianlah surat permohonan ini kami sampaikan dengan segala kerendahan hati dan penuh rasa hormat. Atas segala perhatian, kesediaan, keluangan waktu, serta partisipasi Bapak/Ibu dalam mengisi kuesioner Penilaian Pengguna Lulusan ini, kami menyampaikan penghargaan yang setinggi-tingginya dan ucapan terima kasih yang sebesar-besarnya. Semoga segala kebaikan dan bantuan yang Bapak/Ibu berikan dicatat sebagai amal saleh dan dibalas dengan kebaikan yang berlipat ganda oleh Tuhan Yang Maha Esa. Apabila terdapat kata-kata maupun tutur bahasa dalam surat ini yang kurang berkenan di hati Bapak/Ibu, kami memohon maaf yang sebesar-besarnya.\n\nWassalamu'alaikum warahmatullahi wabarakatuh.\n\nHormat kami,\nTim Pengelola Tracer Study\n{{namaPerguruan}}\n\n---\nCatatan: Surat ini dikirim secara otomatis oleh sistem. Mohon tidak membalas surat elektronik ini secara langsung. Apabila Bapak/Ibu mengalami kendala dalam mengakses tautan kuesioner, silakan menghubungi bagian administrasi/pengelola tracer study pada institusi kami.\n";

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    /** Subjek email untuk atasan seorang alumni. */
    public static String buildSubject(Mahasiswa mhs) {
        String namaAlumni = mhs == null ? "" : nz(mhs.getNama());
        String nim = mhs == null ? "" : nz(mhs.getNim());
        return "Permohonan Pengisian Kuesioner Penilaian Pengguna Lulusan a.n. " + namaAlumni + " (" + nim + ")";
    }

    /**
     * Menyusun isi surat (HTML) untuk seorang atasan. {@code link} = tautan kuesioner pengguna-lulusan
     * (varian -PenggunaLulusan-) yang langsung membuka halaman kuesioner tanpa login.
     */
    public static String buildBody(Mahasiswa mhs, AtasanMahasiswaHelper.Atasan atasan, String link) {
        String tmpl;
        try {
            tmpl = Common.getKonfigurasi("broadcast_email_atasan_body", DEFAULT_BODY).getNilai();
        } catch (Exception e) {
            tmpl = DEFAULT_BODY;
        }
        if (tmpl == null || tmpl.trim().isEmpty()) {
            tmpl = DEFAULT_BODY;
        }

        String namaPerguruan = "Perguruan Tinggi";
        try {
            namaPerguruan = Common.getKonfigurasi("label_universitas", "Perguruan Tinggi").getNilai();
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastAtasanEmailHelper.java:50");
        }

        String prodi = "";
        String fakultas = "";
        String tahunLulus = "";
        try {
            if (mhs != null && mhs.getJurusan() != null) {
                prodi = nz(mhs.getJurusan().getNama());
                if (mhs.getJurusan().getFakultas() != null) {
                    fakultas = nz(mhs.getJurusan().getFakultas().getNama());
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastAtasanEmailHelper.java:63");
        }
        try {
            if (mhs != null && mhs.getTahunLulus() != null) {
                tahunLulus = String.valueOf(mhs.getTahunLulus());
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastAtasanEmailHelper.java:69");
        }

        String linkHtml = "<a target='_blank' href='" + nz(link) + "'>" + nz(link) + "</a>";

        String out = tmpl
                .replace("{{namaAtasan}}", atasan == null ? "" : nz(atasan.nama))
                .replace("{{peranAtasan}}", atasan == null ? "" : nz(atasan.peran))
                .replace("{{namaPerguruan}}", nz(namaPerguruan))
                .replace("{{namaAlumni}}", mhs == null ? "" : nz(mhs.getNama()))
                .replace("{{namaProdi}}", nz(prodi))
                .replace("{{namaFakultas}}", nz(fakultas))
                .replace("{{tahunLulus}}", nz(tahunLulus))
                .replace("{{linkKuesioner}}", linkHtml);
        return out.replace("\n", "<br>");
    }
}
