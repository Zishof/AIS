package ais.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <h2>Kamus terjemahan INTERNAL (manual-assisted) — Indonesia → English / Arab</h2>
 *
 * <p><b>Tujuan.</b> Mesin terjemah bawaan (tanpa layanan eksternal) untuk tombol "Terjemahkan Otomatis"
 * pada layar kelola Label Bahasa. Bersifat <b>manual-assisted</b>: mengisi terjemahan untuk kata/frasa yang
 * DIKENAL kamus, dan MENYISAKAN kata yang belum dikenal apa adanya agar dilengkapi manual oleh admin.</p>
 *
 * <p><b>Cara kerja {@link #terjemah(String, String)}.</b> (1) coba cocokkan SELURUH frasa (dinormalisasi
 * huruf kecil) pada kamus frasa; bila ketemu, pakai itu. (2) Bila tidak, terjemahkan KATA-DEMI-KATA:
 * setiap kata dicari di kamus kata; yang tak dikenal dibiarkan; tanda baca &amp; placeholder {@code {V1}}
 * dipertahankan. Huruf kapital di awal kata Indonesia ditiru pada hasil (untuk English).</p>
 *
 * <p><b>Perluasan.</b> Kamus di bawah dapat terus ditambah; sumber terjemahan yang lebih kaya tetap berasal
 * dari tabel DB {@code LabelBahasa} + file {@code DEFAULT_*.conf} (admin dapat menyunting hasilnya).</p>
 */
public class KamusBahasaInternal {

	private static final Map<String, String> FRASA_EN = new HashMap<String, String>();
	private static final Map<String, String> FRASA_AR = new HashMap<String, String>();
	private static final Map<String, String> KATA_EN = new HashMap<String, String>();
	private static final Map<String, String> KATA_AR = new HashMap<String, String>();
	private static final Map<String, String> FRASA_ZH = new HashMap<String, String>();
	private static final Map<String, String> KATA_ZH = new HashMap<String, String>();

	private KamusBahasaInternal() {
	}

	private static void en(String id, String en) {
		KATA_EN.put(id.toLowerCase(), en);
	}

	private static void ar(String id, String ar) {
		KATA_AR.put(id.toLowerCase(), ar);
	}

	private static void frasa(String id, String en, String ar) {
		FRASA_EN.put(id.toLowerCase().trim(), en);
		FRASA_AR.put(id.toLowerCase().trim(), ar);
	}

	private static void zh(String id, String zh) {
		KATA_ZH.put(id.toLowerCase(), zh);
	}

	private static void frasaZh(String id, String zh) {
		FRASA_ZH.put(id.toLowerCase().trim(), zh);
	}

	static {
		// ---- Frasa umum (dicocokkan penuh lebih dulu) ----
		frasa("data berhasil disimpan", "Data saved successfully", "تم حفظ البيانات بنجاح");
		frasa("data berhasil dihapus", "Data deleted successfully", "تم حذف البيانات بنجاح");
		frasa("data gagal disimpan", "Failed to save data", "فشل حفظ البيانات");
		frasa("harus diisi", "is required", "مطلوب");
		frasa("apakah anda yakin", "Are you sure", "هل أنت متأكد");
		frasa("tidak dapat dihapus", "cannot be deleted", "لا يمكن حذفه");
		frasa("silakan coba lagi", "please try again", "يرجى المحاولة مرة أخرى");

		// ---- Tombol / aksi ----
		en("simpan", "Save"); ar("simpan", "حفظ");
		en("batal", "Cancel"); ar("batal", "إلغاء");
		en("tambah", "Add"); ar("tambah", "إضافة");
		en("ubah", "Edit"); ar("ubah", "تعديل");
		en("hapus", "Delete"); ar("hapus", "حذف");
		en("cari", "Search"); ar("cari", "بحث");
		en("bersihkan", "Clear"); ar("bersihkan", "مسح");
		en("tutup", "Close"); ar("tutup", "إغلاق");
		en("cetak", "Print"); ar("cetak", "طباعة");
		en("download", "Download"); ar("download", "تنزيل");
		en("unggah", "Upload"); ar("unggah", "رفع");
		en("upload", "Upload"); ar("upload", "رفع");
		en("kembali", "Back"); ar("kembali", "رجوع");
		en("lanjut", "Next"); ar("lanjut", "التالي");
		en("pilih", "Select"); ar("pilih", "اختر");
		en("refresh", "Refresh"); ar("refresh", "تحديث");
		en("bantuan", "Help"); ar("bantuan", "مساعدة");
		en("ya", "Yes"); ar("ya", "نعم");
		en("tidak", "No"); ar("tidak", "لا");

		// ---- Kata umum ----
		en("data", "Data"); ar("data", "بيانات");
		en("nama", "Name"); ar("nama", "الاسم");
		en("kode", "Code"); ar("kode", "الرمز");
		en("alamat", "Address"); ar("alamat", "العنوان");
		en("tanggal", "Date"); ar("tanggal", "التاريخ");
		en("waktu", "Time"); ar("waktu", "الوقت");
		en("jumlah", "Total"); ar("jumlah", "المجموع");
		en("status", "Status"); ar("status", "الحالة");
		en("keterangan", "Description"); ar("keterangan", "الوصف");
		en("informasi", "Information"); ar("informasi", "معلومة");
		en("peringatan", "Warning"); ar("peringatan", "تحذير");
		en("kesalahan", "Error"); ar("kesalahan", "خطأ");
		en("pertanyaan", "Question"); ar("pertanyaan", "سؤال");
		en("konfirmasi", "Confirmation"); ar("konfirmasi", "تأكيد");
		en("berhasil", "successful"); ar("berhasil", "ناجح");
		en("gagal", "failed"); ar("gagal", "فشل");
		en("harus", "must"); ar("harus", "يجب");
		en("diisi", "be filled"); ar("diisi", "تعبئة");
		en("wajib", "required"); ar("wajib", "مطلوب");
		en("belum", "not yet"); ar("belum", "لم");
		en("sudah", "already"); ar("sudah", "قد");
		en("aktif", "active"); ar("aktif", "نشط");
		en("nonaktif", "inactive"); ar("nonaktif", "غير نشط");
		en("dan", "and"); ar("dan", "و");
		en("atau", "or"); ar("atau", "أو");
		en("mohon", "please"); ar("mohon", "يرجى");
		en("maaf", "sorry"); ar("maaf", "عذرا");

		// ---- Domain akademik ----
		en("mahasiswa", "Student"); ar("mahasiswa", "طالب");
		en("dosen", "Lecturer"); ar("dosen", "محاضر");
		en("siswa", "Student"); ar("siswa", "طالب");
		en("guru", "Teacher"); ar("guru", "معلم");
		en("pegawai", "Employee"); ar("pegawai", "موظف");
		en("nilai", "Grade"); ar("nilai", "الدرجة");
		en("perkuliahan", "Course"); ar("perkuliahan", "المقرر");
		en("matakuliah", "Subject"); ar("matakuliah", "المادة");
		en("kelas", "Class"); ar("kelas", "الفصل");
		en("jadwal", "Schedule"); ar("jadwal", "الجدول");
		en("semester", "Semester"); ar("semester", "الفصل الدراسي");
		en("ganjil", "Odd"); ar("ganjil", "فردي");
		en("genap", "Even"); ar("genap", "زوجي");
		en("tahun", "Year"); ar("tahun", "السنة");
		en("akademik", "Academic"); ar("akademik", "أكاديمي");
		en("fakultas", "Faculty"); ar("fakultas", "الكلية");
		en("jurusan", "Department"); ar("jurusan", "القسم");
		en("program", "Program"); ar("program", "البرنامج");
		en("kurikulum", "Curriculum"); ar("kurikulum", "المنهج");
		en("ujian", "Exam"); ar("ujian", "الامتحان");
		en("tugas", "Assignment"); ar("tugas", "الواجب");
		en("kehadiran", "Attendance"); ar("kehadiran", "الحضور");
		en("pembayaran", "Payment"); ar("pembayaran", "الدفع");
		en("tagihan", "Bill"); ar("tagihan", "الفاتورة");
		en("biaya", "Cost"); ar("biaya", "التكلفة");
		en("ruang", "Room"); ar("ruang", "الغرفة");
		en("hari", "Day"); ar("hari", "اليوم");

		// ---- Frasa tambahan ----
		frasa("wajib diisi", "is required", "مطلوب");
		frasa("tidak boleh kosong", "cannot be empty", "لا يمكن أن يكون فارغا");
		frasa("pilih salah satu", "please select one", "يرجى اختيار واحد");
		frasa("tambah data", "Add Data", "إضافة بيانات");
		frasa("ubah data", "Edit Data", "تعديل البيانات");
		frasa("hapus data", "Delete Data", "حذف البيانات");
		frasa("cari data", "Search Data", "بحث في البيانات");
		frasa("data tidak ditemukan", "Data not found", "لم يتم العثور على البيانات");
		frasa("belum ada data", "No data yet", "لا توجد بيانات بعد");
		frasa("terjadi kesalahan", "An error occurred", "حدث خطأ");
		frasa("tahun akademik", "Academic Year", "السنة الأكاديمية");
		frasa("tahun ajaran", "Academic Year", "السنة الدراسية");
		frasa("program studi", "Study Program", "البرنامج الدراسي");
		frasa("jenis kelamin", "Gender", "الجنس");
		frasa("tempat lahir", "Place of Birth", "مكان الميلاد");
		frasa("tanggal lahir", "Date of Birth", "تاريخ الميلاد");
		frasa("nama lengkap", "Full Name", "الاسم الكامل");
		frasa("no telepon", "Phone Number", "رقم الهاتف");
		frasa("nomor telepon", "Phone Number", "رقم الهاتف");

		// ---- Aksi tambahan ----
		en("tampilkan", "Show"); ar("tampilkan", "عرض");
		en("sembunyikan", "Hide"); ar("sembunyikan", "إخفاء");
		en("reset", "Reset"); ar("reset", "إعادة تعيين");
		en("ekspor", "Export"); ar("ekspor", "تصدير");
		en("impor", "Import"); ar("impor", "استيراد");
		en("setuju", "Approve"); ar("setuju", "موافقة");
		en("setujui", "Approve"); ar("setujui", "الموافقة");
		en("disetujui", "Approved"); ar("disetujui", "تمت الموافقة");
		en("tolak", "Reject"); ar("tolak", "رفض");
		en("ditolak", "Rejected"); ar("ditolak", "مرفوض");
		en("kirim", "Send"); ar("kirim", "إرسال");
		en("terima", "Receive"); ar("terima", "استلام");
		en("proses", "Process"); ar("proses", "معالجة");
		en("mulai", "Start"); ar("mulai", "بدء");
		en("selesai", "Finish"); ar("selesai", "إنهاء");
		en("berhenti", "Stop"); ar("berhenti", "إيقاف");
		en("hitung", "Calculate"); ar("hitung", "حساب");
		en("verifikasi", "Verify"); ar("verifikasi", "تحقق");
		en("validasi", "Validate"); ar("validasi", "تدقيق");
		en("konfirmasi", "Confirm"); ar("konfirmasi", "تأكيد");
		en("lanjutkan", "Continue"); ar("lanjutkan", "متابعة");
		en("daftar", "List"); ar("daftar", "قائمة");
		en("laporan", "Report"); ar("laporan", "تقرير");
		en("dashboard", "Dashboard"); ar("dashboard", "لوحة التحكم");
		en("dasbor", "Dashboard"); ar("dasbor", "لوحة التحكم");
		en("pengaturan", "Settings"); ar("pengaturan", "الإعدادات");
		en("konfigurasi", "Configuration"); ar("konfigurasi", "التكوين");

		// ---- Field tambahan ----
		en("nomor", "Number"); ar("nomor", "رقم");
		en("telepon", "Phone"); ar("telepon", "الهاتف");
		en("email", "Email"); ar("email", "البريد الإلكتروني");
		en("agama", "Religion"); ar("agama", "الدين");
		en("pekerjaan", "Occupation"); ar("pekerjaan", "المهنة");
		en("jabatan", "Position"); ar("jabatan", "المنصب");
		en("golongan", "Rank"); ar("golongan", "الرتبة");
		en("pangkat", "Grade"); ar("pangkat", "الدرجة");
		en("unit", "Unit"); ar("unit", "الوحدة");
		en("bagian", "Division"); ar("bagian", "القسم");
		en("tempat", "Place"); ar("tempat", "المكان");
		en("lahir", "Birth"); ar("lahir", "الميلاد");
		en("umur", "Age"); ar("umur", "العمر");
		en("usia", "Age"); ar("usia", "العمر");
		en("foto", "Photo"); ar("foto", "الصورة");
		en("berkas", "File"); ar("berkas", "الملف");
		en("dokumen", "Document"); ar("dokumen", "المستند");
		en("lampiran", "Attachment"); ar("lampiran", "المرفق");
		en("catatan", "Note"); ar("catatan", "ملاحظة");
		en("pesan", "Message"); ar("pesan", "رسالة");
		en("judul", "Title"); ar("judul", "العنوان");
		en("isi", "Content"); ar("isi", "المحتوى");
		en("deskripsi", "Description"); ar("deskripsi", "الوصف");
		en("gender", "Gender"); ar("gender", "الجنس");

		// ---- Domain akademik tambahan ----
		en("angkatan", "Batch"); ar("angkatan", "الدفعة");
		en("gelombang", "Wave"); ar("gelombang", "الموجة");
		en("pendaftaran", "Registration"); ar("pendaftaran", "التسجيل");
		en("pendaftar", "Applicant"); ar("pendaftar", "المتقدم");
		en("calon", "Candidate"); ar("calon", "المرشح");
		en("lulus", "Pass"); ar("lulus", "ناجح");
		en("lulusan", "Graduate"); ar("lulusan", "الخريج");
		en("alumni", "Alumni"); ar("alumni", "الخريجون");
		en("wisuda", "Graduation"); ar("wisuda", "التخرج");
		en("transkrip", "Transcript"); ar("transkrip", "كشف الدرجات");
		en("ijazah", "Diploma"); ar("ijazah", "الشهادة");
		en("sertifikat", "Certificate"); ar("sertifikat", "الشهادة");
		en("skripsi", "Thesis"); ar("skripsi", "الأطروحة");
		en("bimbingan", "Guidance"); ar("bimbingan", "الإرشاد");
		en("pembimbing", "Supervisor"); ar("pembimbing", "المشرف");
		en("penguji", "Examiner"); ar("penguji", "الممتحن");
		en("sidang", "Defense"); ar("sidang", "المناقشة");
		en("presensi", "Attendance"); ar("presensi", "الحضور");
		en("materi", "Material"); ar("materi", "المادة العلمية");
		en("diskusi", "Discussion"); ar("diskusi", "المناقشة");
		en("pengumuman", "Announcement"); ar("pengumuman", "الإعلان");
		en("pertemuan", "Meeting"); ar("pertemuan", "اللقاء");
		en("krs", "Study Plan"); ar("krs", "خطة الدراسة");
		en("prodi", "Program"); ar("prodi", "البرنامج");

		// ---- Keuangan tambahan ----
		en("bayar", "Pay"); ar("bayar", "دفع");
		en("potongan", "Deduction"); ar("potongan", "الخصم");
		en("diskon", "Discount"); ar("diskon", "الخصم");
		en("denda", "Penalty"); ar("denda", "الغرامة");
		en("saldo", "Balance"); ar("saldo", "الرصيد");
		en("rekening", "Account"); ar("rekening", "الحساب");
		en("bank", "Bank"); ar("bank", "البنك");
		en("transfer", "Transfer"); ar("transfer", "تحويل");
		en("tunai", "Cash"); ar("tunai", "نقدا");
		en("kredit", "Credit"); ar("kredit", "دائن");
		en("debit", "Debit"); ar("debit", "مدين");
		en("pajak", "Tax"); ar("pajak", "الضريبة");
		en("gaji", "Salary"); ar("gaji", "الراتب");
		en("tunjangan", "Allowance"); ar("tunjangan", "البدل");
		en("lembur", "Overtime"); ar("lembur", "العمل الإضافي");
		en("cuti", "Leave"); ar("cuti", "الإجازة");
		en("izin", "Permit"); ar("izin", "الإذن");

		// ---- Status ----
		en("baru", "New"); ar("baru", "جديد");
		en("lama", "Old"); ar("lama", "قديم");
		en("menunggu", "Pending"); ar("menunggu", "قيد الانتظار");
		en("diproses", "Processed"); ar("diproses", "قيد المعالجة");
		en("dibatalkan", "Cancelled"); ar("dibatalkan", "ملغى");
		en("valid", "Valid"); ar("valid", "صالح");
		en("total", "Total"); ar("total", "الإجمالي");
		en("subtotal", "Subtotal"); ar("subtotal", "المجموع الفرعي");

		// ---- Waktu tambahan ----
		en("bulan", "Month"); ar("bulan", "الشهر");
		en("minggu", "Week"); ar("minggu", "الأسبوع");
		en("jam", "Hour"); ar("jam", "الساعة");
		en("menit", "Minute"); ar("menit", "الدقيقة");
		en("periode", "Period"); ar("periode", "الفترة");
		en("sampai", "Until"); ar("sampai", "إلى");

		// ---- Kata umum tambahan ----
		en("semua", "All"); ar("semua", "الكل");
		en("lainnya", "Others"); ar("lainnya", "أخرى");
		en("dari", "From"); ar("dari", "من");
		en("ke", "To"); ar("ke", "إلى");
		en("untuk", "For"); ar("untuk", "لـ");
		en("dengan", "With"); ar("dengan", "مع");
		en("oleh", "By"); ar("oleh", "بواسطة");
		en("pada", "On"); ar("pada", "في");
		en("ini", "This"); ar("ini", "هذا");
		en("itu", "That"); ar("itu", "ذلك");

		// ================= Batch dari DB label_bahasa (frekuensi tertinggi) =================
		// ---- Frasa umum tambahan ----
		frasa("tampilkan semua", "Show All", "عرض الكل");
		frasa("data tidak ditemukan", "Data not found", "لم يتم العثور على البيانات");
		frasa("tidak dapat", "cannot", "لا يمكن");
		frasa("sudah ada", "already exists", "موجود بالفعل");
		frasa("apakah anda yakin", "Are you sure", "هل أنت متأكد");
		frasa("terlebih dahulu", "first", "أولا");
		frasa("tidak tersedia", "not available", "غير متاح");
		frasa("tata kelola", "Governance", "الحوكمة");
		frasa("perguruan tinggi", "University", "الجامعة");
		frasa("uang muka", "Down Payment", "دفعة مقدمة");
		frasa("tatap muka", "Face to Face", "وجها لوجه");
		frasa("pengabdian masyarakat", "Community Service", "خدمة المجتمع");

		// ---- Frasa/label penuh tersering dari DB (urutan & tata bahasa benar) ----
		// pola "X Siswa/Mahasiswa/Pegawai/Dosen/Guru" → "Student/Employee/Lecturer/Teacher X"
		frasa("nilai siswa", "Student Grades", "درجات الطلاب");
		frasa("penilaian mahasiswa", "Student Assessment", "تقييم الطلاب");
		frasa("catatan siswa", "Student Notes", "ملاحظات الطلاب");
		frasa("catatan mahasiswa", "Student Notes", "ملاحظات الطلاب");
		frasa("catatan pegawai", "Employee Notes", "ملاحظات الموظفين");
		frasa("catatan guru", "Teacher Notes", "ملاحظات المعلمين");
		frasa("kegiatan siswa", "Student Activities", "أنشطة الطلاب");
		frasa("kegiatan kesiswaan", "Student Affairs Activities", "أنشطة شؤون الطلاب");
		frasa("keuangan siswa", "Student Finance", "مالية الطلاب");
		frasa("organisasi siswa", "Student Organization", "منظمة الطلاب");
		frasa("prestasi siswa", "Student Achievements", "إنجازات الطلاب");
		frasa("apresiasi siswa", "Student Appreciation", "تقدير الطلاب");
		frasa("kedisiplinan siswa", "Student Discipline", "انضباط الطلاب");
		frasa("kedisiplinan mahasiswa", "Student Discipline", "انضباط الطلاب");
		frasa("kelas siswa", "Student Class", "فصل الطلاب");
		frasa("rapor siswa", "Student Report Card", "بطاقة تقرير الطالب");
		frasa("angket siswa", "Student Questionnaire", "استبيان الطلاب");
		frasa("calon siswa", "Prospective Student", "الطالب المرشح");
		frasa("siswa alumni", "Alumni Student", "الطالب الخريج");
		frasa("data alumni", "Alumni Data", "بيانات الخريجين");
		frasa("pembayaran siswa", "Student Payment", "دفع الطلاب");
		frasa("penjualan ke siswa", "Sales to Students", "المبيعات للطلاب");
		frasa("biodata mahasiswa", "Student Biodata", "البيانات الشخصية للطالب");
		frasa("beranda mahasiswa", "Student Home", "الصفحة الرئيسية للطالب");
		frasa("cari data mahasiswa", "Search Student Data", "بحث بيانات الطلاب");
		frasa("rencana studi mahasiswa", "Student Study Plan", "خطة دراسة الطالب");
		frasa("pendaftaran mahasiswa cuti", "Student Leave Registration", "تسجيل إجازة الطالب");
		frasa("daftar pegawai", "Employee List", "قائمة الموظفين");
		frasa("diklat pegawai", "Employee Training", "تدريب الموظفين");
		frasa("catatan pegawai", "Employee Notes", "ملاحظات الموظفين");
		frasa("keluarga pegawai", "Employee Family", "عائلة الموظف");
		frasa("pendataan pegawai", "Employee Data Collection", "جمع بيانات الموظفين");
		frasa("pegawai pensiun", "Retired Employee", "الموظف المتقاعد");
		frasa("slip gaji pegawai", "Employee Payslip", "قسيمة راتب الموظف");
		frasa("gaji pegawai", "Employee Salary", "راتب الموظف");
		frasa("transaksi pegawai", "Employee Transaction", "معاملة الموظف");
		frasa("kinerja pegawai", "Employee Performance", "أداء الموظف");
		frasa("calon pegawai", "Prospective Employee", "الموظف المرشح");
		frasa("tipe pegawai", "Employee Type", "نوع الموظف");
		frasa("penjadwalan dosen", "Lecturer Scheduling", "جدولة المحاضرين");
		frasa("dosen wali", "Academic Advisor", "المرشد الأكاديمي");
		frasa("dosen pembimbing", "Supervising Lecturer", "المحاضر المشرف");
		frasa("jenis guru", "Teacher Type", "نوع المعلم");
		frasa("absen piket guru", "Teacher Picket Attendance", "حضور مناوبة المعلم");
		// pola "Jenis/Tipe/Kategori/Status/Satuan X" → "X Type/Category/Status/Unit"
		frasa("jenis jabatan", "Position Type", "نوع المنصب");
		frasa("jenis pensiun", "Pension Type", "نوع التقاعد");
		frasa("jenis sekolah", "School Type", "نوع المدرسة");
		frasa("jenis pimpinan", "Leadership Type", "نوع القيادة");
		frasa("jenis informasi", "Information Type", "نوع المعلومات");
		frasa("jenis penilaian", "Assessment Type", "نوع التقييم");
		frasa("jenis produk", "Product Type", "نوع المنتج");
		frasa("jenis angket", "Questionnaire Type", "نوع الاستبيان");
		frasa("jenis diklat", "Training Type", "نوع التدريب");
		frasa("jenis pelatihan", "Training Type", "نوع التدريب");
		frasa("jenis transaksi", "Transaction Type", "نوع المعاملة");
		frasa("jenis anggota", "Member Type", "نوع العضو");
		frasa("jenis item", "Item Type", "نوع العنصر");
		frasa("jenis shift", "Shift Type", "نوع الوردية");
		frasa("jenis asset", "Asset Type", "نوع الأصل");
		frasa("jenis penghapusan aset", "Asset Disposal Type", "نوع إتلاف الأصل");
		frasa("tipe produk", "Product Type", "نوع المنتج");
		frasa("tipe anggota", "Member Type", "نوع العضو");
		frasa("tipe item", "Item Type", "نوع العنصر");
		frasa("tipe pegawai", "Employee Type", "نوع الموظف");
		frasa("tipe masa kerja", "Tenure Type", "نوع مدة الخدمة");
		frasa("kategori produk", "Product Category", "فئة المنتج");
		frasa("kategori item", "Item Category", "فئة العنصر");
		frasa("status item", "Item Status", "حالة العنصر");
		frasa("status aset", "Asset Status", "حالة الأصل");
		frasa("status asset", "Asset Status", "حالة الأصل");
		frasa("status awal siswa", "Student Initial Status", "الحالة الأولية للطالب");
		frasa("status ruangan", "Room Status", "حالة الغرفة");
		frasa("satuan aset", "Asset Unit", "وحدة الأصل");
		frasa("kategori produk koperasi", "Cooperative Product Category", "فئة منتج التعاونية");
		// pola "Laporan X" → "X Report", "Data X" → "X Data", "Daftar X" → "X List"
		frasa("laporan lembur", "Overtime Report", "تقرير العمل الإضافي");
		frasa("laporan absensi", "Attendance Report", "تقرير الحضور");
		frasa("laporan kehadiran", "Attendance Report", "تقرير الحضور");
		frasa("laporan presensi", "Attendance Report", "تقرير الحضور");
		frasa("laporan pemakaian", "Usage Report", "تقرير الاستخدام");
		frasa("laporan pengadaan", "Procurement Report", "تقرير المشتريات");
		frasa("laporan sirkulasi", "Circulation Report", "تقرير التداول");
		frasa("laporan inventaris", "Inventory Report", "تقرير الجرد");
		frasa("laporan peminjaman", "Loan Report", "تقرير الإعارة");
		frasa("laporan kunjungan", "Visit Report", "تقرير الزيارات");
		frasa("laporan kurikulum", "Curriculum Report", "تقرير المنهج");
		frasa("laporan denda", "Penalty Report", "تقرير الغرامات");
		frasa("laporan saldo", "Balance Report", "تقرير الرصيد");
		frasa("laporan akun", "Account Report", "تقرير الحساب");
		frasa("data aset", "Asset Data", "بيانات الأصول");
		frasa("data kpi", "KPI Data", "بيانات مؤشرات الأداء");
		frasa("data barang dan jasa", "Goods and Services Data", "بيانات السلع والخدمات");
		frasa("daftar riwayat hidup", "Curriculum Vitae", "السيرة الذاتية");
		frasa("daftar lembur pegawai", "Employee Overtime List", "قائمة العمل الإضافي للموظفين");
		frasa("daftar upah tenaga kerja", "Labor Wage List", "قائمة أجور العمالة");
		// SOP / surat / persuratan
		frasa("alur sop", "SOP Flow", "تدفق الإجراء");
		frasa("aktor sop", "SOP Actor", "فاعل الإجراء");
		frasa("jenis sop", "SOP Type", "نوع الإجراء");
		frasa("dokumen sop", "SOP Document", "وثيقة الإجراء");
		frasa("parameter alur sop", "SOP Flow Parameter", "معامل تدفق الإجراء");
		frasa("surat masuk", "Incoming Mail", "البريد الوارد");
		frasa("surat keluar", "Outgoing Mail", "البريد الصادر");
		frasa("nomor surat", "Letter Number", "رقم الخطاب");
		frasa("kop surat", "Letterhead", "ترويسة الخطاب");
		frasa("loker surat", "Mail Locker", "خزانة البريد");
		frasa("peminjam surat", "Mail Borrower", "مستعير البريد");
		frasa("pengembalian surat", "Mail Return", "إرجاع البريد");
		frasa("sirkulasi surat", "Mail Circulation", "تداول البريد");
		frasa("tata kelola surat", "Mail Governance", "حوكمة البريد");
		frasa("disposisi surat masuk", "Incoming Mail Disposition", "توجيه البريد الوارد");
		frasa("disposisi surat keluar", "Outgoing Mail Disposition", "توجيه البريد الصادر");
		frasa("alur disposisi surat masuk", "Incoming Mail Disposition Flow", "تدفق توجيه البريد الوارد");
		frasa("alur disposisi surat keluar", "Outgoing Mail Disposition Flow", "تدفق توجيه البريد الصادر");
		frasa("klasifikasi surat masuk", "Incoming Mail Classification", "تصنيف البريد الوارد");
		// keuangan / koperasi / aset / pengadaan
		frasa("anggota koperasi", "Cooperative Member", "عضو التعاونية");
		frasa("usaha koperasi", "Cooperative Business", "أعمال التعاونية");
		frasa("produk koperasi", "Cooperative Product", "منتج التعاونية");
		frasa("transaksi koperasi", "Cooperative Transaction", "معاملة التعاونية");
		frasa("sistem kantin", "Canteen System", "نظام المقصف");
		frasa("kios koperasi", "Cooperative Kiosk", "كشك التعاونية");
		frasa("toko dan pedagang", "Stores and Merchants", "المتاجر والتجار");
		frasa("gaji pokok", "Basic Salary", "الراتب الأساسي");
		frasa("item gaji", "Salary Item", "بند الراتب");
		frasa("format item gaji", "Salary Item Format", "تنسيق بند الراتب");
		frasa("slip gaji", "Payslip", "قسيمة الراتب");
		frasa("buku besar", "General Ledger", "دفتر الأستاذ");
		frasa("jurnal umum", "General Journal", "دفتر اليومية العام");
		frasa("jurnal harian", "Daily Journal", "دفتر اليومية اليومي");
		frasa("posting jurnal", "Journal Posting", "ترحيل القيود");
		frasa("item biaya", "Cost Item", "بند التكلفة");
		frasa("grup laporan", "Report Group", "مجموعة التقارير");
		frasa("grup akun", "Account Group", "مجموعة الحسابات");
		frasa("kode akun", "Account Code", "رمز الحساب");
		frasa("pengembalian barang", "Goods Return", "إرجاع البضائع");
		frasa("pemakaian barang", "Goods Usage", "استخدام البضائع");
		frasa("penghapusan barang", "Goods Disposal", "إتلاف البضائع");
		frasa("barang inventaris", "Inventory Goods", "بضائع الجرد");
		frasa("aset dan pengadaan", "Assets and Procurement", "الأصول والمشتريات");
		frasa("aset dan inventaris", "Assets and Inventory", "الأصول والجرد");
		frasa("kelompok asset", "Asset Group", "مجموعة الأصول");
		frasa("pemilik aset", "Asset Owner", "مالك الأصل");
		frasa("pemilik asset", "Asset Owner", "مالك الأصل");
		frasa("peminjaman aset", "Asset Loan", "إعارة الأصل");
		frasa("cara pengadaan", "Procurement Method", "طريقة الشراء");
		frasa("cara pembayaran", "Payment Method", "طريقة الدفع");
		frasa("proses pembayaran", "Payment Process", "عملية الدفع");
		frasa("proses penggajian", "Payroll Process", "عملية الرواتب");
		frasa("posting penggajian", "Payroll Posting", "ترحيل الرواتب");
		frasa("rencana penggajian", "Payroll Plan", "خطة الرواتب");
		frasa("tagihan pembayaran", "Payment Bill", "فاتورة الدفع");
		// akademik / jadwal / kurikulum
		frasa("tahun akademik", "Academic Year", "السنة الأكاديمية");
		frasa("tahun ajaran", "Academic Year", "السنة الدراسية");
		frasa("kalender akademik", "Academic Calendar", "التقويم الأكاديمي");
		frasa("sistem informasi akademik", "Academic Information System", "نظام المعلومات الأكاديمي");
		frasa("mata pelajaran", "Subject", "المادة الدراسية");
		frasa("jadwal pelajaran", "Lesson Schedule", "جدول الدروس");
		frasa("jam pelajaran", "Lesson Hour", "حصة الدرس");
		frasa("jadwal perkuliahan", "Lecture Schedule", "جدول المحاضرات");
		frasa("aktifitas perkuliahan", "Lecture Activity", "نشاط المحاضرة");
		frasa("penjadwalan kelas", "Class Scheduling", "جدولة الفصول");
		frasa("penjadwalan ruangan", "Room Scheduling", "جدولة الغرف");
		frasa("penjadwalan uts dan uas", "Midterm and Final Exam Scheduling", "جدولة الاختبارات النصفية والنهائية");
		frasa("bahan kajian", "Study Material", "مادة الدراسة");
		frasa("kurikulum obe", "OBE Curriculum", "منهج التعليم القائم على المخرجات");
		frasa("capaian lulusan", "Graduate Outcomes", "مخرجات الخريجين");
		frasa("profil lulusan", "Graduate Profile", "ملف الخريج");
		frasa("profesi lulusan", "Graduate Profession", "مهنة الخريج");
		frasa("pertemuan dan absensi", "Meetings and Attendance", "اللقاءات والحضور");
		frasa("pembimbing kkn", "Community Service Program Supervisor", "مشرف برنامج خدمة المجتمع");
		frasa("pembimbing pkl", "Internship Supervisor", "مشرف التدريب الميداني");
		// umum / sistem / pengguna
		frasa("keluar aplikasi", "Exit Application", "الخروج من التطبيق");
		frasa("ubah password", "Change Password", "تغيير كلمة المرور");
		frasa("reset password user", "Reset User Password", "إعادة تعيين كلمة مرور المستخدم");
		frasa("pengaturan pengguna", "User Settings", "إعدادات المستخدم");
		frasa("grup pengguna", "User Group", "مجموعة المستخدمين");
		frasa("hari libur rutin", "Regular Holiday", "العطلة الدورية");
		frasa("hari libur nasional", "National Holiday", "العطلة الوطنية");
		frasa("unit kerja", "Work Unit", "وحدة العمل");
		frasa("lowongan pekerjaan", "Job Vacancy", "الوظيفة الشاغرة");
		frasa("jabatan fungsional", "Functional Position", "المنصب الوظيفي");
		frasa("jabatan struktural", "Structural Position", "المنصب الهيكلي");
		frasa("gelombang pendaftaran", "Registration Wave", "موجة التسجيل");
		frasa("penghasilan orang tua", "Parent Income", "دخل الوالدين");
		frasa("rekap uang makan", "Meal Allowance Recap", "ملخص بدل الطعام");
		frasa("rekap per hari", "Daily Recap", "الملخص اليومي");

		// ---- Frasa penuh batch-2 (rank 300-600 DB) ----
		frasa("pembayaran mahasiswa", "Student Payment", "دفع الطلاب");
		frasa("keuangan mahasiswa", "Student Finance", "مالية الطلاب");
		frasa("tunggakan mahasiswa", "Student Arrears", "متأخرات الطلاب");
		frasa("tunggakan siswa", "Student Arrears", "متأخرات الطلاب");
		frasa("konsultasi mahasiswa", "Student Consultation", "استشارة الطلاب");
		frasa("aktifitas mahasiswa", "Student Activities", "أنشطة الطلاب");
		frasa("data calon mahasiswa", "Prospective Student Data", "بيانات الطلاب المرشحين");
		frasa("daftar calon mahasiswa", "Prospective Student List", "قائمة الطلاب المرشحين");
		frasa("pembayaran calon siswa", "Prospective Student Payment", "دفع الطلاب المرشحين");
		frasa("tagihan siswa", "Student Bill", "فاتورة الطالب");
		frasa("rekap tagihan siswa", "Student Bill Recap", "ملخص فواتير الطلاب");
		frasa("deposit siswa", "Student Deposit", "وديعة الطالب");
		frasa("laporan saldo siswa", "Student Balance Report", "تقرير رصيد الطالب");
		frasa("pelanggaran siswa", "Student Violation", "مخالفة الطالب");
		frasa("pengajuan siswa", "Student Submission", "تقديم الطالب");
		frasa("kelas les siswa", "Student Tutoring Class", "فصل الدروس الخصوصية للطالب");
		frasa("penilaian dosen", "Lecturer Assessment", "تقييم المحاضر");
		frasa("kegiatan dosen", "Lecturer Activities", "أنشطة المحاضر");
		frasa("kewajiban dosen", "Lecturer Obligations", "التزامات المحاضر");
		frasa("status dosen", "Lecturer Status", "حالة المحاضر");
		frasa("beban kerja dosen", "Lecturer Workload", "عبء عمل المحاضر");
		frasa("dosen pembimbing akademik", "Academic Supervising Lecturer", "المحاضر المشرف الأكاديمي");
		frasa("pembimbing akademik", "Academic Advisor", "المرشد الأكاديمي");
		frasa("guru bk", "Counseling Teacher", "معلم الإرشاد");
		frasa("guru wali", "Homeroom Teacher", "معلم الفصل");
		frasa("pelanggaran pegawai", "Employee Violation", "مخالفة الموظف");
		frasa("pengajuan pegawai", "Employee Submission", "تقديم الموظف");
		frasa("pengajuan cuti pegawai", "Employee Leave Request", "طلب إجازة الموظف");
		frasa("kedisiplinan pegawai", "Employee Discipline", "انضباط الموظف");
		frasa("catatan harian pegawai", "Employee Daily Log", "السجل اليومي للموظف");
		frasa("pekerjaan orang tua", "Parent Occupation", "مهنة الوالدين");
		frasa("pendapatan orang tua", "Parent Income", "دخل الوالدين");
		frasa("pendidikan orang tua", "Parent Education", "تعليم الوالدين");
		// akademik
		frasa("penerimaan siswa baru", "New Student Admission", "قبول الطلاب الجدد");
		frasa("penerimaan peserta didik baru", "New Student Admission", "قبول الطلاب الجدد");
		frasa("pengumuman akademik", "Academic Announcement", "إعلان أكاديمي");
		frasa("pengumuman akademis", "Academic Announcement", "إعلان أكاديمي");
		frasa("kalender perkuliahan", "Lecture Calendar", "تقويم المحاضرات");
		frasa("kurikulum perkuliahan", "Lecture Curriculum", "منهج المحاضرات");
		frasa("paket perkuliahan", "Lecture Package", "حزمة المحاضرات");
		frasa("jam perkuliahan", "Lecture Hour", "ساعة المحاضرة");
		frasa("kelompok mata pelajaran", "Subject Group", "مجموعة المواد الدراسية");
		frasa("jurusan sekolah", "School Major", "تخصص المدرسة");
		frasa("semester pendek", "Short Semester", "الفصل الدراسي القصير");
		frasa("bank soal", "Question Bank", "بنك الأسئلة");
		frasa("nilai huruf", "Letter Grade", "الدرجة الحرفية");
		frasa("nilai transfer", "Transfer Grade", "درجة التحويل");
		frasa("format nilai skripsi", "Thesis Grade Format", "تنسيق درجة الأطروحة");
		frasa("pengajuan tugas akhir", "Final Project Submission", "تقديم المشروع النهائي");
		frasa("pembimbing tugas akhir", "Final Project Supervisor", "مشرف المشروع النهائي");
		frasa("laporan transkrip akademik", "Academic Transcript Report", "تقرير كشف الدرجات الأكاديمي");
		frasa("laporan kartu hasil studi", "Study Result Card Report", "تقرير بطاقة نتائج الدراسة");
		frasa("cetak undangan wisuda", "Print Graduation Invitation", "طباعة دعوة التخرج");
		frasa("daftar wisuda", "Graduation List", "قائمة التخرج");
		frasa("daftar lulus", "Pass List", "قائمة الناجحين");
		frasa("krs mahasiswa", "Student Study Plan", "خطة دراسة الطالب");
		// perpustakaan / karya
		frasa("informasi perpustakaan", "Library Information", "معلومات المكتبة");
		frasa("kartu anggota perpustakaan", "Library Membership Card", "بطاقة عضوية المكتبة");
		frasa("manajemen karya ilmiah", "Scientific Work Management", "إدارة الأعمال العلمية");
		frasa("publikasi karya ilmiah", "Scientific Publication", "النشر العلمي");
		frasa("koleksi repository", "Repository Collection", "مجموعة المستودع");
		// keuangan / akuntansi
		frasa("laporan keuangan", "Financial Report", "التقرير المالي");
		frasa("laporan arus kas", "Cash Flow Report", "تقرير التدفق النقدي");
		frasa("akun kas besar", "Main Cash Account", "حساب الصندوق الرئيسي");
		frasa("pengeluaran kas kecil", "Petty Cash Expense", "مصروف النثرية");
		frasa("penggantian kas kecil", "Petty Cash Reimbursement", "تعويض النثرية");
		frasa("saldo awal kas kecil", "Petty Cash Opening Balance", "الرصيد الافتتاحي للنثرية");
		frasa("sumber dana", "Fund Source", "مصدر التمويل");
		frasa("sumber data", "Data Source", "مصدر البيانات");
		frasa("penggunaan dana", "Fund Usage", "استخدام الأموال");
		frasa("penerimaan dana", "Fund Receipt", "استلام الأموال");
		frasa("rencana anggaran", "Budget Plan", "خطة الميزانية");
		frasa("jadwal anggaran", "Budget Schedule", "جدول الميزانية");
		frasa("cara pembayaran gaji", "Salary Payment Method", "طريقة دفع الراتب");
		frasa("pembayaran tagihan", "Bill Payment", "دفع الفاتورة");
		frasa("penerimaan tagihan", "Bill Receipt", "استلام الفاتورة");
		frasa("posting pembayaran", "Payment Posting", "ترحيل الدفع");
		frasa("akun pembayaran", "Payment Account", "حساب الدفع");
		frasa("setting biaya", "Fee Setting", "إعداد الرسوم");
		frasa("pengaturan biaya", "Fee Settings", "إعدادات الرسوم");
		frasa("pencairan diskon", "Discount Disbursement", "صرف الخصم");
		frasa("aturan diskon", "Discount Rule", "قاعدة الخصم");
		frasa("konfigurasi diskon", "Discount Configuration", "تكوين الخصم");
		frasa("pembayaran dp", "Down Payment", "الدفعة المقدمة");
		frasa("detail biaya", "Cost Detail", "تفاصيل التكلفة");
		frasa("draft jurnal", "Journal Draft", "مسودة القيد");
		frasa("transaksi vendor", "Vendor Transaction", "معاملة المورد");
		frasa("pengajuan beasiswa", "Scholarship Application", "طلب المنحة الدراسية");
		// aset / stok / pengadaan
		frasa("penerimaan aset", "Asset Receipt", "استلام الأصل");
		frasa("permintaan aset", "Asset Request", "طلب الأصل");
		frasa("pemesanan aset", "Asset Ordering", "طلب الأصل");
		frasa("stok opname", "Stock Taking", "جرد المخزون");
		// kinerja / kpi / sdm
		frasa("laporan kinerja", "Performance Report", "تقرير الأداء");
		frasa("penilaian kpi", "KPI Assessment", "تقييم مؤشرات الأداء");
		frasa("asesor kinerja", "Performance Assessor", "مقيّم الأداء");
		frasa("asesmen kinerja", "Performance Assessment", "تقييم الأداء");
		frasa("realisasi kerja pegawai", "Employee Work Realization", "تحقيق عمل الموظف");
		frasa("target kerja pegawai", "Employee Work Target", "هدف عمل الموظف");
		frasa("kenaikan pangkat dan jabatan", "Promotion of Rank and Position", "الترقية في الرتبة والمنصب");
		// umum / sistem
		frasa("surat menyurat", "Correspondence", "المراسلات");
		frasa("template surat", "Letter Template", "قالب الخطاب");
		frasa("template query", "Query Template", "قالب الاستعلام");
		frasa("pengajuan anda", "Your Submission", "طلبك");
		frasa("manajemen tugas", "Task Management", "إدارة المهام");
		frasa("manajemen proyek", "Project Management", "إدارة المشاريع");
		frasa("status kehadiran", "Attendance Status", "حالة الحضور");
		frasa("absensi pelajaran", "Lesson Attendance", "حضور الدرس");
		frasa("angket dan survey", "Questionnaire and Survey", "الاستبيان والمسح");
		frasa("angket penilaian", "Assessment Questionnaire", "استبيان التقييم");
		frasa("dokumen pendukung", "Supporting Document", "الوثيقة الداعمة");
		frasa("pengaturan bahasa", "Language Settings", "إعدادات اللغة");
		frasa("pengaturan berkas", "File Settings", "إعدادات الملفات");
		frasa("pengaturan konfigurasi", "Configuration Settings", "إعدادات التكوين");
		frasa("catatan administrasi", "Administrative Notes", "ملاحظات إدارية");
		frasa("customer service", "Customer Service", "خدمة العملاء");
		frasa("antar jemput", "Pick-up and Drop-off", "التوصيل والاستقبال");
		frasa("cek kesehatan", "Health Check", "الفحص الصحي");
		frasa("blokir anggota", "Block Member", "حظر العضو");
		frasa("kunjungan anggota", "Member Visit", "زيارة العضو");
		frasa("program donasi", "Donation Program", "برنامج التبرع");
		frasa("penyaluran donasi", "Donation Distribution", "توزيع التبرعات");
		frasa("persetujuan pajak", "Tax Approval", "الموافقة الضريبية");
		frasa("tingkat publikasi", "Publication Level", "مستوى النشر");
		frasa("kategori program", "Program Category", "فئة البرنامج");
		frasa("masa pendaftaran", "Registration Period", "فترة التسجيل");
		frasa("kelompok pendaftaran", "Registration Group", "مجموعة التسجيل");
		frasa("jenis seleksi pmb", "Admission Selection Type", "نوع اختيار القبول");
		frasa("ingat akun saya", "Remember my account", "تذكر حسابي");

		// ---- Frasa penuh batch-3 (rank 560-800 DB; hanya yang bermakna) ----
		frasa("sistem informasi terpadu", "Integrated Information System", "نظام المعلومات المتكامل");
		frasa("kerjasama antar instansi", "Inter-Agency Cooperation", "التعاون بين المؤسسات");
		frasa("status kerjasama mahasiswa", "Student Cooperation Status", "حالة تعاون الطالب");
		frasa("penelitian dan pengabdian", "Research and Community Service", "البحث وخدمة المجتمع");
		frasa("pengajuan penelitian atau pengabdian", "Research or Community Service Submission", "تقديم البحث أو خدمة المجتمع");
		frasa("rekap penelitian atau pengabdian", "Research or Community Service Recap", "ملخص البحث أو خدمة المجتمع");
		frasa("publikasi jurnal penelitian", "Research Journal Publication", "نشر مجلة البحث");
		frasa("data pendukung akreditasi", "Accreditation Supporting Data", "بيانات دعم الاعتماد");
		frasa("akreditasi perguruan tinggi", "University Accreditation", "اعتماد الجامعة");
		frasa("prasarana perguruan tinggi", "University Infrastructure", "البنية التحتية للجامعة");
		frasa("prasarana tambahan", "Additional Infrastructure", "بنية تحتية إضافية");
		// keuangan / anggaran
		frasa("jadwal realisasi anggaran", "Budget Realization Schedule", "جدول تحقيق الميزانية");
		frasa("rencana dan realisasi anggaran", "Budget Plan and Realization", "خطة وتحقيق الميزانية");
		frasa("realisasi anggaran per tanggal", "Budget Realization by Date", "تحقيق الميزانية حسب التاريخ");
		frasa("rencana anggaran tri wulan", "Quarterly Budget Plan", "خطة الميزانية الفصلية");
		frasa("realisasi anggaran tri wulan", "Quarterly Budget Realization", "تحقيق الميزانية الفصلي");
		frasa("rencana anggaran tiap bulan", "Monthly Budget Plan", "خطة الميزانية الشهرية");
		frasa("realisasi anggaran tiap bulan", "Monthly Budget Realization", "تحقيق الميزانية الشهري");
		frasa("anggaran belanja dan realisasi", "Expenditure Budget and Realization", "ميزانية الإنفاق والتحقيق");
		frasa("pertanggungjawaban kas besar", "Main Cash Accountability", "المساءلة عن الصندوق الرئيسي");
		frasa("pertanggungjawaban uang muka", "Down Payment Accountability", "المساءلة عن الدفعة المقدمة");
		frasa("persetujuan pengeluaran kas kecil", "Petty Cash Expense Approval", "الموافقة على مصروف النثرية");
		frasa("posting transaksi pengadaan", "Procurement Transaction Posting", "ترحيل معاملة الشراء");
		frasa("posting transaksi penyusutan", "Depreciation Transaction Posting", "ترحيل معاملة الإهلاك");
		frasa("laporan komparasi bulanan", "Monthly Comparison Report", "تقرير المقارنة الشهري");
		frasa("laporan komparasi tahunan", "Annual Comparison Report", "تقرير المقارنة السنوي");
		frasa("laporan realisasi kinerja", "Performance Realization Report", "تقرير تحقيق الأداء");
		frasa("tagihan vendor", "Vendor Bill", "فاتورة المورد");
		// pembayaran mahasiswa
		frasa("pembayaran calon mahasiswa", "Prospective Student Payment", "دفع الطلاب المرشحين");
		frasa("catatan pembayaran mahasiswa", "Student Payment Notes", "ملاحظات دفع الطلاب");
		frasa("monitor pembayaran mahasiswa", "Student Payment Monitor", "مراقبة دفع الطلاب");
		frasa("pembayaran mahasiswa lainnya", "Other Student Payments", "مدفوعات الطلاب الأخرى");
		frasa("rekapitulasi pembayaran mahasiswa", "Student Payment Recapitulation", "تلخيص دفع الطلاب");
		frasa("pembayaran daftar ulang mahasiswa", "Student Re-registration Payment", "دفع إعادة تسجيل الطالب");
		frasa("daftar ulang mahasiswa baru", "New Student Re-registration", "إعادة تسجيل الطلاب الجدد");
		frasa("daftar ulang mahasiswa", "Student Re-registration", "إعادة تسجيل الطالب");
		frasa("bypass pembayaran mahasiswa", "Bypass Student Payment", "تجاوز دفع الطالب");
		frasa("pengaturan jadwal pembayaran", "Payment Schedule Settings", "إعدادات جدول الدفع");
		frasa("pengaturan billing pembayaran", "Payment Billing Settings", "إعدادات فوترة الدفع");
		frasa("pengaturan denda pembayaran", "Payment Penalty Settings", "إعدادات غرامة الدفع");
		frasa("pengaturan nominal denda pembayaran", "Payment Penalty Amount Settings", "إعدادات مبلغ غرامة الدفع");
		frasa("pengaturan tagihan bulanan", "Monthly Billing Settings", "إعدادات الفواتير الشهرية");
		frasa("pengaturan matakuliah berbayar", "Paid Course Settings", "إعدادات المقررات المدفوعة");
		frasa("laporan rekap mahasiswa belum bayar", "Report of Unpaid Students", "تقرير الطلاب غير المسددين");
		frasa("laporan rekap mahasiswa sudah bayar", "Report of Paid Students", "تقرير الطلاب المسددين");
		frasa("laporan rekap pembayaran per prodi", "Payment Recap Report by Program", "تقرير ملخص الدفع حسب البرنامج");
		frasa("monitor belum bayar tunggakan", "Unpaid Arrears Monitor", "مراقبة المتأخرات غير المسددة");
		frasa("monitor sudah bayar tunggakan", "Paid Arrears Monitor", "مراقبة المتأخرات المسددة");
		frasa("metode pembayaran barang", "Goods Payment Method", "طريقة دفع البضائع");
		// akademik / skripsi
		frasa("revisi skripsi", "Thesis Revision", "مراجعة الأطروحة");
		frasa("pembimbing skripsi", "Thesis Supervisor", "مشرف الأطروحة");
		frasa("pengajuan sidang skripsi", "Thesis Defense Submission", "تقديم مناقشة الأطروحة");
		frasa("pembatasan ipk pengambilan krs", "GPA Limit for Study Plan", "حد المعدل التراكمي لخطة الدراسة");
		frasa("template jadwal perkuliahan", "Lecture Schedule Template", "قالب جدول المحاضرات");
		frasa("penjadwalan kalender acara", "Event Calendar Scheduling", "جدولة تقويم الفعاليات");
		frasa("verifikasi berkas dan kelulusan", "File and Graduation Verification", "التحقق من الملفات والتخرج");
		frasa("verifikasi gerbang qr", "QR Gate Verification", "التحقق من بوابة رمز الاستجابة");
		frasa("pengaturan ruang dan ujian", "Room and Exam Settings", "إعدادات الغرف والاختبارات");
		frasa("paket dan form pendaftaran", "Registration Package and Form", "حزمة واستمارة التسجيل");
		// kinerja / pegawai
		frasa("absensi kehadiran pegawai harian", "Employee Daily Attendance", "الحضور اليومي للموظفين");
		frasa("absensi kehadiran dosen harian", "Lecturer Daily Attendance", "الحضور اليومي للمحاضرين");
		frasa("penilaian capaian kerja pegawai", "Employee Work Achievement Assessment", "تقييم إنجاز عمل الموظف");
		frasa("rincian capaian kerja pegawai", "Employee Work Achievement Detail", "تفاصيل إنجاز عمل الموظف");
		frasa("realisasi kerja pegawai bulanan", "Employee Monthly Work Realization", "تحقيق العمل الشهري للموظف");
		frasa("realisasi kerja pegawai tahunan", "Employee Annual Work Realization", "تحقيق العمل السنوي للموظف");
		frasa("target kerja pegawai bulanan", "Employee Monthly Work Target", "هدف العمل الشهري للموظف");
		frasa("target kerja pegawai tahunan", "Employee Annual Work Target", "هدف العمل السنوي للموظف");
		frasa("evaluasi penetapan kinerja", "Performance Determination Evaluation", "تقييم تحديد الأداء");
		frasa("pengajuan transaksi pegawai", "Employee Transaction Submission", "تقديم معاملة الموظف");
		frasa("persetujuan pengajuan transaksi pegawai", "Employee Transaction Submission Approval", "الموافقة على تقديم معاملة الموظف");
		frasa("laporan rekapitulasi konsumsi", "Consumption Recapitulation Report", "تقرير تلخيص الاستهلاك");
		frasa("pendataan formulir kegiatan", "Activity Form Data Collection", "جمع بيانات استمارة النشاط");

		// ---- Frasa penuh batch-4 (kandidat bersih tambahan) ----
		// persuratan
		frasa("opsi surat masuk", "Incoming Mail Option", "خيار البريد الوارد");
		frasa("opsi surat keluar", "Outgoing Mail Option", "خيار البريد الصادر");
		frasa("surat masuk per jenis", "Incoming Mail by Type", "البريد الوارد حسب النوع");
		frasa("surat keluar per jenis", "Outgoing Mail by Type", "البريد الصادر حسب النوع");
		frasa("surat masuk per tanggal", "Incoming Mail by Date", "البريد الوارد حسب التاريخ");
		frasa("surat keluar per tanggal", "Outgoing Mail by Date", "البريد الصادر حسب التاريخ");
		frasa("surat masuk per loker", "Incoming Mail by Locker", "البريد الوارد حسب الخزانة");
		frasa("klasifikasi surat keluar", "Outgoing Mail Classification", "تصنيف البريد الصادر");
		frasa("persetujuan surat keluar", "Outgoing Mail Approval", "الموافقة على البريد الصادر");
		frasa("folder penyimpanan", "Storage Folder", "مجلد التخزين");
		// koperasi
		frasa("setup usaha koperasi", "Cooperative Business Setup", "إعداد أعمال التعاونية");
		frasa("tipe produk koperasi", "Cooperative Product Type", "نوع منتج التعاونية");
		frasa("tipe anggota koperasi", "Cooperative Member Type", "نوع عضو التعاونية");
		frasa("jenis anggota koperasi", "Cooperative Member Type", "نوع عضو التعاونية");
		frasa("jenis identitas koperasi", "Cooperative Identity Type", "نوع هوية التعاونية");
		frasa("jenis transaksi koperasi", "Cooperative Transaction Type", "نوع معاملة التعاونية");
		frasa("cara pembayaran koperasi", "Cooperative Payment Method", "طريقة دفع التعاونية");
		frasa("syarat produk koperasi", "Cooperative Product Requirements", "متطلبات منتج التعاونية");
		frasa("sistem informasi koperasi", "Cooperative Information System", "نظام معلومات التعاونية");
		frasa("tagihan angsuran koperasi", "Cooperative Installment Bill", "فاتورة أقساط التعاونية");
		frasa("pembayaran angsuran koperasi", "Cooperative Installment Payment", "دفع أقساط التعاونية");
		frasa("persetujuan transaksi koperasi", "Cooperative Transaction Approval", "الموافقة على معاملة التعاونية");
		frasa("biaya pendaftaran anggota", "Member Registration Fee", "رسوم تسجيل العضو");
		frasa("jenis identitas anggota", "Member Identity Type", "نوع هوية العضو");
		frasa("pesanan anggota", "Member Order", "طلب العضو");
		frasa("anggota peminjam", "Borrowing Member", "العضو المستعير");
		frasa("peminjaman per anggota", "Loan per Member", "الإعارة لكل عضو");
		frasa("pengembalian per anggota", "Return per Member", "الإرجاع لكل عضو");
		// pengadaan / stok / aset
		frasa("perencanaan pengadaan", "Procurement Planning", "تخطيط المشتريات");
		frasa("pemesanan pengadaan", "Procurement Ordering", "طلب المشتريات");
		frasa("penerimaan pengadaan", "Procurement Receipt", "استلام المشتريات");
		frasa("retur pengadaan", "Procurement Return", "إرجاع المشتريات");
		frasa("pengadaan dan pemakaian", "Procurement and Usage", "المشتريات والاستخدام");
		frasa("pengadaan dan perencanaan", "Procurement and Planning", "المشتريات والتخطيط");
		frasa("transfer item", "Item Transfer", "نقل العنصر");
		frasa("terima transfer item", "Receive Item Transfer", "استلام نقل العنصر");
		frasa("pendataan item", "Item Data Collection", "جمع بيانات العناصر");
		frasa("monitor stok item", "Item Stock Monitor", "مراقبة مخزون العنصر");
		frasa("tracking stok item", "Item Stock Tracking", "تتبع مخزون العنصر");
		frasa("cetak barcode item", "Print Item Barcode", "طباعة باركود العنصر");
		frasa("saldo buku awal", "Book Opening Balance", "الرصيد الافتتاحي للدفتر");
		frasa("saldo item awal", "Item Opening Balance", "الرصيد الافتتاحي للعنصر");
		frasa("ubah ke inventaris", "Convert to Inventory", "تحويل إلى الجرد");
		frasa("batas waktu pengembalian", "Return Deadline", "الموعد النهائي للإرجاع");
		frasa("jumlah eksemplar", "Number of Copies", "عدد النسخ");
		frasa("sistem inventory", "Inventory System", "نظام الجرد");
		// pegawai / kepegawaian
		frasa("cuti dan izin", "Leave and Permit", "الإجازة والإذن");
		frasa("usulan pensiun", "Pension Proposal", "اقتراح التقاعد");
		frasa("pengajuan pensiun", "Pension Submission", "تقديم التقاعد");
		frasa("kenaikan gaji berkala", "Periodic Salary Increase", "الزيادة الدورية للراتب");
		frasa("variable penggajian", "Payroll Variable", "متغير الرواتب");
		frasa("perkiraan slip gaji pegawai", "Employee Payslip Estimate", "تقدير قسيمة راتب الموظف");
		frasa("upload slip gaji manual", "Manual Payslip Upload", "رفع قسيمة الراتب يدويا");
		frasa("kehadiran pegawai harian", "Employee Daily Attendance", "الحضور اليومي للموظف");
		frasa("riwayat bekerja pegawai", "Employee Work History", "تاريخ عمل الموظف");
		frasa("riwayat pelatihan pegawai", "Employee Training History", "تاريخ تدريب الموظف");
		frasa("riwayat pendidikan pegawai", "Employee Education History", "تاريخ تعليم الموظف");
		frasa("riwayat status kepegawaian", "Employment Status History", "تاريخ حالة التوظيف");
		frasa("jenis kegiatan kepegawaian", "Personnel Activity Type", "نوع نشاط شؤون الموظفين");
		frasa("penghitungan masa kerja", "Tenure Calculation", "حساب مدة الخدمة");
		frasa("tarif asuransi pegawai", "Employee Insurance Rate", "معدل تأمين الموظف");
		frasa("rincian absensi karyawan", "Employee Attendance Detail", "تفاصيل حضور الموظف");
		frasa("rincian presensi karyawan", "Employee Attendance Detail", "تفاصيل حضور الموظف");
		frasa("pendaftaran tenaga kerja", "Labor Registration", "تسجيل العمالة");
		frasa("list lowongan pekerjaan", "Job Vacancy List", "قائمة الوظائف الشاغرة");
		// akademik / sekolah
		frasa("sistem sekolah", "School System", "نظام المدرسة");
		frasa("konfigurasi sekolah", "School Configuration", "تكوين المدرسة");
		frasa("penjurusan sekolah", "School Majoring", "تخصص المدرسة");
		frasa("rencana tahun ajaran", "Academic Year Plan", "خطة السنة الدراسية");
		frasa("jenis jam pelajaran", "Lesson Hour Type", "نوع حصة الدرس");
		frasa("aktivitas pelajaran", "Lesson Activity", "نشاط الدرس");
		frasa("aktifitas pelajaran", "Lesson Activity", "نشاط الدرس");
		frasa("catatan kelas", "Class Notes", "ملاحظات الفصل");
		frasa("tentang program studi", "About the Study Program", "حول البرنامج الدراسي");
		frasa("cari buku ajar", "Search Textbook", "بحث الكتاب الدراسي");
		frasa("melihat pendaftar wisuda", "View Graduation Registrants", "عرض المسجلين للتخرج");
		frasa("cek pendaftar wisuda", "Check Graduation Registrants", "فحص المسجلين للتخرج");
		frasa("alumni dan mahasiswa", "Alumni and Students", "الخريجون والطلاب");
		frasa("bahan ujian dan sertifikat", "Exam Materials and Certificate", "مواد الاختبار والشهادة");
		frasa("laporan belanja siswa", "Student Expenditure Report", "تقرير إنفاق الطلاب");
		// laporan / akuntansi umum
		frasa("laporan pengembalian", "Return Report", "تقرير الإرجاع");
		frasa("laporan cover absensi", "Attendance Cover Report", "تقرير غلاف الحضور");
		frasa("laporan arus harian", "Daily Flow Report", "تقرير التدفق اليومي");
		frasa("laporan daftar hadir dosen", "Lecturer Attendance List Report", "تقرير قائمة حضور المحاضرين");
		frasa("laporan daftar hadir ujian", "Exam Attendance List Report", "تقرير قائمة حضور الاختبار");
		frasa("laporan jadwal perkuliahan", "Lecture Schedule Report", "تقرير جدول المحاضرات");
		frasa("laporan rekapitulasi absen", "Attendance Recapitulation Report", "تقرير تلخيص الحضور");
		frasa("laporan rekapitulasi alumni", "Alumni Recapitulation Report", "تقرير تلخيص الخريجين");
		frasa("laporan berita acara skripsi", "Thesis Minutes Report", "تقرير محضر الأطروحة");
		frasa("laporan aktifitas pustakawan", "Librarian Activity Report", "تقرير نشاط أمين المكتبة");
		frasa("posting pembayaran mahasiswa", "Student Payment Posting", "ترحيل دفع الطلاب");
		frasa("akun transaksi", "Transaction Account", "حساب المعاملة");
		frasa("setup laporan", "Report Setup", "إعداد التقرير");
		frasa("setup grup akun", "Account Group Setup", "إعداد مجموعة الحسابات");
		frasa("setup kode akun", "Account Code Setup", "إعداد رمز الحساب");
		frasa("sejarah posting", "Posting History", "تاريخ الترحيل");
		frasa("pesan ruangan", "Room Booking", "حجز الغرفة");
		frasa("grup angket", "Questionnaire Group", "مجموعة الاستبيان");
		frasa("absen piket mahasiswa", "Student Picket Attendance", "حضور مناوبة الطالب");
		frasa("peminjaman dan pengembalian", "Loan and Return", "الإعارة والإرجاع");
		frasa("peminjaman belum dikembalikan", "Unreturned Loans", "الإعارات غير المعادة");
		frasa("data sister", "Sister Data", "بيانات سيستر");

		// ---- Frasa penuh batch-5 (band freq-2/3 bermakna) ----
		frasa("item aset", "Asset Item", "عنصر الأصل");
		frasa("item baru", "New Item", "عنصر جديد");
		frasa("jam kerja", "Working Hours", "ساعات العمل");
		frasa("jam masuk", "Check-in Time", "وقت الدخول");
		frasa("jenis smt", "Semester Type", "نوع الفصل الدراسي");
		frasa("kas besar", "Main Cash", "الصندوق الرئيسي");
		frasa("kas kecil", "Petty Cash", "النثرية");
		frasa("kas kasir", "Cashier Cash", "نقدية الصراف");
		frasa("buku ajar", "Textbook", "الكتاب الدراسي");
		frasa("buku bahan ajar", "Teaching Material Book", "كتاب المواد التعليمية");
		frasa("buku tamu", "Guest Book", "دفتر الزوار");
		frasa("guru tamu", "Guest Teacher", "المعلم الزائر");
		frasa("data diri", "Personal Data", "البيانات الشخصية");
		frasa("data ayah", "Father Data", "بيانات الأب");
		frasa("data guru", "Teacher Data", "بيانات المعلم");
		frasa("data lain", "Other Data", "بيانات أخرى");
		frasa("orang tua", "Parents", "الوالدان");
		frasa("form cuti", "Leave Form", "استمارة الإجازة");
		frasa("mhs baru", "New Student", "طالب جديد");
		frasa("calon mhs", "Prospective Student", "الطالب المرشح");
		frasa("menu role", "Role Menu", "قائمة الأدوار");
		frasa("grup guru", "Teacher Group", "مجموعة المعلمين");
		frasa("grup soal", "Question Group", "مجموعة الأسئلة");
		frasa("grup umum", "General Group", "المجموعة العامة");
		frasa("grup angket penilaian", "Assessment Questionnaire Group", "مجموعة استبيان التقييم");
		frasa("jenis angket penilaian", "Assessment Questionnaire Type", "نوع استبيان التقييم");
		frasa("jenis kartu identitas", "ID Card Type", "نوع بطاقة الهوية");
		frasa("jenis akun pembayaran", "Payment Account Type", "نوع حساب الدفع");
		frasa("jenis pembobotan nilai", "Grade Weighting Type", "نوع ترجيح الدرجات");
		frasa("jenis item perencanaan", "Planning Item Type", "نوع عنصر التخطيط");
		frasa("konfigurasi akun item", "Item Account Configuration", "تكوين حساب العنصر");
		frasa("konfigurasi invoice", "Invoice Configuration", "تكوين الفاتورة");
		frasa("konfigurasi bank host", "Host Bank Configuration", "تكوين البنك المضيف");
		frasa("laporan kinerja rinci", "Detailed Performance Report", "تقرير الأداء التفصيلي");
		frasa("laporan trial balance", "Trial Balance Report", "تقرير ميزان المراجعة");
		frasa("laporan rencana kinerja", "Performance Plan Report", "تقرير خطة الأداء");
		frasa("laporan rekapitulasi presensi", "Attendance Recapitulation Report", "تقرير تلخيص الحضور");
		frasa("laporan rekap jumlah mahasiswa", "Student Count Recap Report", "تقرير ملخص عدد الطلاب");
		frasa("laporan rekapitulasi data mahasiswa", "Student Data Recapitulation Report", "تقرير تلخيص بيانات الطلاب");
		frasa("pendataan beban kerja", "Workload Data Collection", "جمع بيانات عبء العمل");
		frasa("persetujuan kas besar", "Main Cash Approval", "الموافقة على الصندوق الرئيسي");
		frasa("persetujuan uang muka", "Down Payment Approval", "الموافقة على الدفعة المقدمة");
		frasa("pengajuan kas besar", "Main Cash Submission", "تقديم الصندوق الرئيسي");
		frasa("pengajuan uang muka", "Down Payment Submission", "تقديم الدفعة المقدمة");
		frasa("posting deposit siswa", "Student Deposit Posting", "ترحيل وديعة الطالب");
		frasa("kegiatan kemahasiswaan", "Student Affairs Activity", "نشاط شؤون الطلاب");
		frasa("lahan perguruan tinggi", "University Land", "أرض الجامعة");
		frasa("rencana tahun akademik", "Academic Year Plan", "خطة السنة الأكاديمية");
		frasa("riwayat transaksi saya", "My Transaction History", "سجل معاملاتي");
		frasa("satuan output kegiatan", "Activity Output Unit", "وحدة مخرجات النشاط");
		frasa("daftar pengajuan cheque", "Cheque Submission List", "قائمة تقديم الشيكات");
		frasa("semua jurnal terposting", "All Posted Journals", "جميع القيود المرحلة");
		frasa("setting akun pembayaran", "Payment Account Setting", "إعداد حساب الدفع");
		frasa("informasi kunjungan mahasiswa", "Student Visit Information", "معلومات زيارة الطلاب");
		frasa("alur persetujuan surat keluar", "Outgoing Mail Approval Flow", "تدفق الموافقة على البريد الصادر");
		frasa("catatan memo dan risalah", "Memo and Minutes Notes", "ملاحظات المذكرة والمحضر");
		frasa("pengaturan kegiatan", "Activity Settings", "إعدادات النشاط");
		frasa("anggaran per bulan", "Monthly Budget", "الميزانية الشهرية");
		frasa("realisasi per bulan", "Monthly Realization", "التحقيق الشهري");
		frasa("akreditasi sarjana", "Undergraduate Accreditation", "اعتماد البكالوريوس");
		frasa("jabatan penelitian", "Research Position", "منصب البحث");
		frasa("data jenis tugas", "Task Type Data", "بيانات نوع المهمة");
		frasa("ekspor ke feeder", "Export to Feeder", "تصدير إلى فيدر");
		frasa("impor dari feeder", "Import from Feeder", "استيراد من فيدر");
		frasa("grafik realisasi", "Realization Chart", "رسم بياني للتحقيق");
		frasa("grafik perencanaan", "Planning Chart", "رسم بياني للتخطيط");
		frasa("laporan belanja siswa", "Student Expenditure Report", "تقرير إنفاق الطلاب");
		frasa("pengajuan lain mahasiswa", "Other Student Submissions", "تقديمات الطلاب الأخرى");
		// aksi pendek
		frasa("isi data", "Fill Data", "تعبئة البيانات");
		frasa("isi surat", "Letter Content", "محتوى الخطاب");
		frasa("isi saldo", "Top Up Balance", "شحن الرصيد");
		frasa("cek bukti", "Check Proof", "فحص الإثبات");
		frasa("cek nilai", "Check Grade", "فحص الدرجة");
		frasa("cek ulang", "Recheck", "إعادة الفحص");
		frasa("coba lagi", "Try Again", "حاول مرة أخرى");
		frasa("cari soal", "Search Question", "بحث السؤال");
		frasa("cari menu", "Search Menu", "بحث القائمة");
		frasa("edit data", "Edit Data", "تعديل البيانات");
		frasa("copy data", "Copy Data", "نسخ البيانات");
		frasa("baca data", "Read Data", "قراءة البيانات");
		// label field umum (perbaikan urutan Nama/Kode X)
		frasa("nama bank", "Bank Name", "اسم البنك");
		frasa("nama guru", "Teacher Name", "اسم المعلم");
		frasa("nama item", "Item Name", "اسم العنصر");
		frasa("nama file", "File Name", "اسم الملف");
		frasa("nama grup", "Group Name", "اسم المجموعة");
		frasa("nama akun", "Account Name", "اسم الحساب");
		frasa("nama buku", "Book Name", "اسم الكتاب");
		frasa("nama aset", "Asset Name", "اسم الأصل");
		frasa("nama toko", "Store Name", "اسم المتجر");
		frasa("kode guru", "Teacher Code", "رمز المعلم");
		frasa("kode item", "Item Code", "رمز العنصر");
		frasa("kode kota", "City Code", "رمز المدينة");
		frasa("kode unit", "Unit Code", "رمز الوحدة");
		frasa("kode unik", "Unique Code", "الرمز الفريد");
		frasa("kode toko", "Store Code", "رمز المتجر");
		frasa("bulan ini", "This Month", "هذا الشهر");
		frasa("hari ini", "Today", "اليوم");
		frasa("saat ini", "Current", "الحالي");

		// ---- Kata umum & fungsional ----
		en("apakah", "whether"); ar("apakah", "هل");
		en("jika", "if"); ar("jika", "إذا");
		en("saat", "when"); ar("saat", "عند");
		en("ada", "exists"); ar("ada", "موجود");
		en("dalam", "in"); ar("dalam", "في");
		en("boleh", "may"); ar("boleh", "يجوز");
		en("satu", "one"); ar("satu", "واحد");
		en("bisa", "can"); ar("bisa", "يمكن");
		en("dapat", "can"); ar("dapat", "يمكن");
		en("hanya", "only"); ar("hanya", "فقط");
		en("lebih", "more"); ar("lebih", "أكثر");
		en("lain", "other"); ar("lain", "آخر");
		en("sebagai", "as"); ar("sebagai", "كـ");
		en("anda", "you"); ar("anda", "أنت");
		en("saya", "my"); ar("saya", "أنا");
		en("akan", "will"); ar("akan", "سوف");
		en("telah", "has"); ar("telah", "قد");
		en("tanpa", "without"); ar("tanpa", "بدون");
		en("sebelum", "before"); ar("sebelum", "قبل");
		en("sebelumnya", "previous"); ar("sebelumnya", "السابق");
		en("setelah", "after"); ar("setelah", "بعد");
		en("hingga", "until"); ar("hingga", "حتى");
		en("agar", "so that"); ar("agar", "حتى");
		en("juga", "also"); ar("juga", "أيضا");
		en("lagi", "again"); ar("lagi", "مجددا");
		en("sama", "same"); ar("sama", "نفس");
		en("sesuai", "according to"); ar("sesuai", "وفقا");
		en("berdasarkan", "based on"); ar("berdasarkan", "بناء على");
		en("melalui", "through"); ar("melalui", "عبر");
		en("menggunakan", "using"); ar("menggunakan", "باستخدام");
		en("ingin", "want"); ar("ingin", "يريد");
		en("yakin", "sure"); ar("yakin", "متأكد");
		en("silakan", "please"); ar("silakan", "من فضلك");
		en("harap", "please"); ar("harap", "يرجى");
		en("sekarang", "now"); ar("sekarang", "الآن");
		en("terakhir", "last"); ar("terakhir", "الأخير");
		en("bukan", "not"); ar("bukan", "ليس");
		en("memiliki", "have"); ar("memiliki", "يمتلك");
		en("menjadi", "become"); ar("menjadi", "يصبح");
		en("dianggap", "considered"); ar("dianggap", "يعتبر");
		en("terdapat", "there is"); ar("terdapat", "يوجد");
		en("cepat", "fast"); ar("cepat", "سريع");
		en("langsung", "direct"); ar("langsung", "مباشر");
		en("otomatis", "automatic"); ar("otomatis", "تلقائي");
		en("tetap", "fixed"); ar("tetap", "ثابت");
		en("khusus", "special"); ar("khusus", "خاص");
		en("mandiri", "independent"); ar("mandiri", "مستقل");
		en("besar", "large"); ar("besar", "كبير");
		en("kecil", "small"); ar("kecil", "صغير");
		en("tinggi", "high"); ar("tinggi", "عالي");
		en("tua", "old"); ar("tua", "قديم");
		en("depan", "front"); ar("depan", "أمام");
		en("bawah", "below"); ar("bawah", "أسفل");
		en("atas", "above"); ar("atas", "أعلى");
		en("awal", "initial"); ar("awal", "أولي");
		en("akhir", "final"); ar("akhir", "نهائي");
		en("umum", "general"); ar("umum", "عام");
		en("dasar", "basic"); ar("dasar", "أساسي");
		en("utama", "main"); ar("utama", "رئيسي");
		en("lengkap", "complete"); ar("lengkap", "كامل");
		en("seluruh", "entire"); ar("seluruh", "كامل");
		en("tersedia", "available"); ar("tersedia", "متاح");
		en("terhubung", "connected"); ar("terhubung", "متصل");
		en("terintegrasi", "integrated"); ar("terintegrasi", "متكامل");
		en("terpadu", "integrated"); ar("terpadu", "متكامل");
		en("nasional", "national"); ar("nasional", "وطني");
		en("harian", "daily"); ar("harian", "يومي");
		en("bulanan", "monthly"); ar("bulanan", "شهري");
		en("dipakai", "used"); ar("dipakai", "مستخدم");
		en("digunakan", "used"); ar("digunakan", "مستخدم");
		en("dipilih", "selected"); ar("dipilih", "مختار");
		en("ditemukan", "found"); ar("ditemukan", "تم العثور عليه");
		en("diterima", "accepted"); ar("diterima", "مقبول");
		en("dibayar", "paid"); ar("dibayar", "مدفوع");
		en("disimpan", "saved"); ar("disimpan", "محفوظ");
		en("tersimpan", "saved"); ar("tersimpan", "محفوظ");
		en("dihapus", "deleted"); ar("dihapus", "محذوف");
		en("terjadi", "occurred"); ar("terjadi", "حدث");
		en("berlaku", "applies"); ar("berlaku", "ساري");
		en("hadir", "present"); ar("hadir", "حاضر");

		// ---- Aksi ----
		en("tampil", "displayed"); ar("tampil", "يظهر");
		en("tampilkan", "Show"); ar("tampilkan", "عرض");
		en("lihat", "View"); ar("lihat", "عرض");
		en("melihat", "View"); ar("melihat", "عرض");
		en("masuk", "Enter"); ar("masuk", "دخول");
		en("keluar", "Exit"); ar("keluar", "خروج");
		en("buka", "Open"); ar("buka", "فتح");
		en("klik", "Click"); ar("klik", "انقر");
		en("ambil", "Take"); ar("ambil", "أخذ");
		en("mengambil", "Take"); ar("mengambil", "أخذ");
		en("buat", "Create"); ar("buat", "إنشاء");
		en("kelola", "Manage"); ar("kelola", "إدارة");
		en("sunting", "Edit"); ar("sunting", "تحرير");
		en("ganti", "Change"); ar("ganti", "تغيير");
		en("coba", "Try"); ar("coba", "حاول");
		en("periksa", "Check"); ar("periksa", "فحص");
		en("gunakan", "Use"); ar("gunakan", "استخدم");
		en("masukkan", "Enter"); ar("masukkan", "أدخل");
		en("aktifkan", "Activate"); ar("aktifkan", "تفعيل");
		en("batalkan", "Cancel"); ar("batalkan", "إلغاء");
		en("kosongkan", "Clear"); ar("kosongkan", "إفراغ");
		en("menyimpan", "Save"); ar("menyimpan", "حفظ");
		en("menghapus", "Delete"); ar("menghapus", "حذف");
		en("melakukan", "Perform"); ar("melakukan", "تنفيذ");
		en("memproses", "Process"); ar("memproses", "معالجة");
		en("muat", "Load"); ar("muat", "تحميل");
		en("memuat", "Load"); ar("memuat", "تحميل");
		en("unduh", "Download"); ar("unduh", "تنزيل");
		en("ulang", "Repeat"); ar("ulang", "إعادة");
		en("mengikuti", "Follow"); ar("mengikuti", "اتباع");
		en("ikut", "Join"); ar("ikut", "الانضمام");
		en("mengajar", "Teach"); ar("mengajar", "تدريس");
		en("sinkronkan", "Synchronize"); ar("sinkronkan", "مزامنة");

		// ---- Domain akademik & institusi ----
		en("sekolah", "School"); ar("sekolah", "المدرسة");
		en("kampus", "Campus"); ar("kampus", "الحرم الجامعي");
		en("sistem", "System"); ar("sistem", "النظام");
		en("institusi", "Institution"); ar("institusi", "مؤسسة");
		en("yayasan", "Foundation"); ar("yayasan", "مؤسسة");
		en("organisasi", "Organization"); ar("organisasi", "منظمة");
		en("biodata", "Biodata"); ar("biodata", "البيانات الشخصية");
		en("identitas", "Identity"); ar("identitas", "الهوية");
		en("pendidikan", "Education"); ar("pendidikan", "التعليم");
		en("pembelajaran", "Learning"); ar("pembelajaran", "التعلم");
		en("pelajaran", "Lesson"); ar("pelajaran", "درس");
		en("kuliah", "Lecture"); ar("kuliah", "محاضرة");
		en("studi", "Study"); ar("studi", "دراسة");
		en("bahan", "Material"); ar("bahan", "مادة");
		en("kajian", "Study"); ar("kajian", "دراسة");
		en("penelitian", "Research"); ar("penelitian", "البحث");
		en("ilmiah", "Scientific"); ar("ilmiah", "علمي");
		en("karya", "Work"); ar("karya", "عمل");
		en("prestasi", "Achievement"); ar("prestasi", "إنجاز");
		en("kegiatan", "Activity"); ar("kegiatan", "نشاط");
		en("aktivitas", "Activity"); ar("aktivitas", "نشاط");
		en("kelulusan", "Graduation"); ar("kelulusan", "التخرج");
		en("registrasi", "Registration"); ar("registrasi", "تسجيل");
		en("seleksi", "Selection"); ar("seleksi", "اختيار");
		en("akreditasi", "Accreditation"); ar("akreditasi", "الاعتماد");
		en("evaluasi", "Evaluation"); ar("evaluasi", "تقييم");
		en("penilaian", "Assessment"); ar("penilaian", "التقييم");
		en("angket", "Questionnaire"); ar("angket", "استبيان");
		en("soal", "Question"); ar("soal", "سؤال");
		en("jawaban", "Answer"); ar("jawaban", "إجابة");
		en("peserta", "Participant"); ar("peserta", "مشارك");
		en("wali", "Guardian"); ar("wali", "ولي");
		en("ayah", "Father"); ar("ayah", "الأب");
		en("ibu", "Mother"); ar("ibu", "الأم");
		en("orang", "Person"); ar("orang", "شخص");
		en("pembina", "Supervisor"); ar("pembina", "مشرف");
		en("pimpinan", "Leader"); ar("pimpinan", "قيادة");
		en("profesi", "Profession"); ar("profesi", "مهنة");
		en("karir", "Career"); ar("karir", "مهنة");
		en("kepegawaian", "Personnel"); ar("kepegawaian", "شؤون الموظفين");
		en("absensi", "Attendance"); ar("absensi", "الحضور");
		en("absen", "Attendance"); ar("absen", "حضور");
		en("penjadwalan", "Scheduling"); ar("penjadwalan", "الجدولة");
		en("capaian", "Outcome"); ar("capaian", "مخرج");

		// ---- Keuangan / koperasi / aset ----
		en("transaksi", "Transaction"); ar("transaksi", "معاملة");
		en("koperasi", "Cooperative"); ar("koperasi", "تعاونية");
		en("kantin", "Canteen"); ar("kantin", "مقصف");
		en("kas", "Cash"); ar("kas", "النقدية");
		en("uang", "Money"); ar("uang", "المال");
		en("dana", "Fund"); ar("dana", "صندوق");
		en("anggaran", "Budget"); ar("anggaran", "الميزانية");
		en("realisasi", "Realization"); ar("realisasi", "تحقيق");
		en("harga", "Price"); ar("harga", "السعر");
		en("nominal", "Nominal"); ar("nominal", "قيمة");
		en("angsuran", "Installment"); ar("angsuran", "قسط");
		en("piutang", "Receivable"); ar("piutang", "ذمم مدينة");
		en("sisa", "Remaining"); ar("sisa", "متبقي");
		en("belanja", "Expenditure"); ar("belanja", "إنفاق");
		en("jurnal", "Journal"); ar("jurnal", "دفتر اليومية");
		en("posting", "Posting"); ar("posting", "ترحيل");
		en("penggajian", "Payroll"); ar("penggajian", "الرواتب");
		en("aset", "Asset"); ar("aset", "أصل");
		en("barang", "Goods"); ar("barang", "بضاعة");
		en("stok", "Stock"); ar("stok", "مخزون");
		en("produk", "Product"); ar("produk", "منتج");
		en("jasa", "Service"); ar("jasa", "خدمة");
		en("layanan", "Service"); ar("layanan", "خدمة");
		en("toko", "Store"); ar("toko", "متجر");
		en("pelanggan", "Customer"); ar("pelanggan", "عميل");
		en("penyedia", "Provider"); ar("penyedia", "مزود");
		en("vendor", "Vendor"); ar("vendor", "مورد");
		en("pengadaan", "Procurement"); ar("pengadaan", "المشتريات");
		en("pemesanan", "Ordering"); ar("pemesanan", "طلب");
		en("pesanan", "Order"); ar("pesanan", "طلب");
		en("permintaan", "Request"); ar("permintaan", "طلب");
		en("penerimaan", "Receipt"); ar("penerimaan", "استلام");
		en("pengembalian", "Return"); ar("pengembalian", "إرجاع");
		en("peminjaman", "Loan"); ar("peminjaman", "إعارة");
		en("perpustakaan", "Library"); ar("perpustakaan", "المكتبة");
		en("pustaka", "Library"); ar("pustaka", "مكتبة");
		en("buku", "Book"); ar("buku", "كتاب");
		en("koleksi", "Collection"); ar("koleksi", "مجموعة");

		// ---- Surat / SOP / administrasi ----
		en("surat", "Letter"); ar("surat", "خطاب");
		en("disposisi", "Disposition"); ar("disposisi", "توجيه");
		en("persetujuan", "Approval"); ar("persetujuan", "الموافقة");
		en("pengajuan", "Submission"); ar("pengajuan", "تقديم");
		en("administrasi", "Administration"); ar("administrasi", "الإدارة");
		en("manajemen", "Management"); ar("manajemen", "الإدارة");
		en("operasional", "Operational"); ar("operasional", "تشغيلي");
		en("alur", "Flow"); ar("alur", "تدفق");
		en("revisi", "Revision"); ar("revisi", "مراجعة");
		en("rekap", "Recap"); ar("rekap", "ملخص");
		en("rekapitulasi", "Recapitulation"); ar("rekapitulasi", "تلخيص");
		en("ringkasan", "Summary"); ar("ringkasan", "ملخص");
		en("rincian", "Detail"); ar("rincian", "تفاصيل");
		en("riwayat", "History"); ar("riwayat", "السجل");
		en("agenda", "Agenda"); ar("agenda", "جدول أعمال");
		en("panduan", "Guide"); ar("panduan", "دليل");
		en("kunjungan", "Visit"); ar("kunjungan", "زيارة");
		en("notifikasi", "Notification"); ar("notifikasi", "إشعار");
		en("pendataan", "Data Collection"); ar("pendataan", "جمع البيانات");
		en("statistik", "Statistics"); ar("statistik", "إحصائيات");
		en("analisis", "Analysis"); ar("analisis", "تحليل");
		en("klasifikasi", "Classification"); ar("klasifikasi", "تصنيف");
		en("kriteria", "Criteria"); ar("kriteria", "معايير");
		en("kategori", "Category"); ar("kategori", "فئة");
		en("klasifikasi", "Classification"); ar("klasifikasi", "تصنيف");

		// ---- UI / sistem / umum ----
		en("pengguna", "User"); ar("pengguna", "المستخدم");
		en("akun", "Account"); ar("akun", "حساب");
		en("akses", "Access"); ar("akses", "الوصول");
		en("hak", "Right"); ar("hak", "حق");
		en("halaman", "Page"); ar("halaman", "صفحة");
		en("tombol", "Button"); ar("tombol", "زر");
		en("tabel", "Table"); ar("tabel", "جدول");
		en("kolom", "Column"); ar("kolom", "عمود");
		en("teks", "Text"); ar("teks", "نص");
		en("gambar", "Image"); ar("gambar", "صورة");
		en("grafik", "Chart"); ar("grafik", "رسم بياني");
		en("file", "File"); ar("file", "ملف");
		en("koneksi", "Connection"); ar("koneksi", "اتصال");
		en("jaringan", "Network"); ar("jaringan", "شبكة");
		en("peladen", "Server"); ar("peladen", "خادم");
		en("aplikasi", "Application"); ar("aplikasi", "تطبيق");
		en("fitur", "Feature"); ar("fitur", "ميزة");
		en("menu", "Menu"); ar("menu", "قائمة");
		en("pilihan", "Choice"); ar("pilihan", "خيار");
		en("opsi", "Option"); ar("opsi", "خيار");
		en("pencarian", "Search"); ar("pencarian", "بحث");
		en("hasil", "Result"); ar("hasil", "نتيجة");
		en("bukti", "Proof"); ar("bukti", "إثبات");
		en("kartu", "Card"); ar("kartu", "بطاقة");
		en("kunci", "Key"); ar("kunci", "مفتاح");
		en("tanda", "Sign"); ar("tanda", "علامة");
		en("contoh", "Example"); ar("contoh", "مثال");
		en("referensi", "Reference"); ar("referensi", "مرجع");
		en("parameter", "Parameter"); ar("parameter", "معامل");
		en("tampilan", "Display"); ar("tampilan", "عرض");
		en("laporan", "Report"); ar("laporan", "تقرير");
		en("formulir", "Form"); ar("formulir", "استمارة");
		en("sebaran", "Distribution"); ar("sebaran", "توزيع");

		// ---- Lain-lain domain ----
		en("jenis", "Type"); ar("jenis", "نوع");
		en("tipe", "Type"); ar("tipe", "نوع");
		en("item", "Item"); ar("item", "عنصر");
		en("kelompok", "Group"); ar("kelompok", "مجموعة");
		en("grup", "Group"); ar("grup", "مجموعة");
		en("anggota", "Member"); ar("anggota", "عضو");
		en("paket", "Package"); ar("paket", "حزمة");
		en("satuan", "Unit"); ar("satuan", "وحدة");
		en("lokasi", "Location"); ar("lokasi", "الموقع");
		en("kota", "City"); ar("kota", "مدينة");
		en("pusat", "Center"); ar("pusat", "مركز");
		en("induk", "Parent"); ar("induk", "رئيسي");
		en("tingkat", "Level"); ar("tingkat", "مستوى");
		en("tahap", "Stage"); ar("tahap", "مرحلة");
		en("batas", "Limit"); ar("batas", "حد");
		en("asal", "Origin"); ar("asal", "أصل");
		en("profil", "Profile"); ar("profil", "الملف الشخصي");
		en("kinerja", "Performance"); ar("kinerja", "الأداء");
		en("beban", "Load"); ar("beban", "عبء");
		en("bahasa", "Language"); ar("bahasa", "اللغة");
		en("masa", "Period"); ar("masa", "فترة");
		en("metode", "Method"); ar("metode", "طريقة");
		en("cara", "Method"); ar("cara", "طريقة");
		en("rencana", "Plan"); ar("rencana", "خطة");
		en("target", "Target"); ar("target", "هدف");
		en("syarat", "Requirement"); ar("syarat", "شرط");
		en("persyaratan", "Requirements"); ar("persyaratan", "متطلبات");
		en("format", "Format"); ar("format", "تنسيق");
		en("model", "Model"); ar("model", "نموذج");
		en("komponen", "Component"); ar("komponen", "مكون");
		en("percent", "Percent"); ar("percent", "بالمئة");
		en("persen", "Percent"); ar("persen", "بالمئة");
		en("koma", "comma"); ar("koma", "فاصلة");
		en("maksimal", "Maximum"); ar("maksimal", "الحد الأقصى");
		en("minimal", "Minimum"); ar("minimal", "الحد الأدنى");
		en("info", "Info"); ar("info", "معلومات");
		en("daring", "Online"); ar("daring", "عبر الإنترنت");
		en("sesi", "Session"); ar("sesi", "جلسة");
		en("dll", "etc"); ar("dll", "إلخ");
	}

	// ================= Kamus MANDARIN (中文, Simplified) — Indonesia → 中文 =================
	static {
		// ---- Frasa ----
		frasaZh("data berhasil disimpan", "数据保存成功");
		frasaZh("data berhasil dihapus", "数据删除成功");
		frasaZh("data gagal disimpan", "数据保存失败");
		frasaZh("apakah anda yakin", "您确定吗");
		frasaZh("silakan coba lagi", "请重试");
		frasaZh("wajib diisi", "必填");
		frasaZh("tidak dapat dihapus", "无法删除");
		frasaZh("nama lengkap", "全名");
		frasaZh("tahun akademik", "学年");
		frasaZh("tahun ajaran", "学年");
		frasaZh("program studi", "专业");
		frasaZh("jenis kelamin", "性别");
		frasaZh("tempat lahir", "出生地");
		frasaZh("tanggal lahir", "出生日期");
		frasaZh("nilai siswa", "学生成绩");
		frasaZh("surat masuk", "收文");
		frasaZh("surat keluar", "发文");
		frasaZh("kas kecil", "备用金");
		frasaZh("kas besar", "主现金");
		frasaZh("gaji pokok", "基本工资");
		frasaZh("pilih bahasa", "选择语言");
		frasaZh("selamat datang", "欢迎");
		frasaZh("hari ini", "今天");
		frasaZh("belum ada data", "暂无数据");
		frasaZh("data tidak ditemukan", "未找到数据");
		frasaZh("terjadi kesalahan", "发生错误");
		// ---- Aksi / tombol ----
		zh("simpan", "保存"); zh("batal", "取消"); zh("tambah", "添加"); zh("ubah", "编辑");
		zh("hapus", "删除"); zh("cari", "搜索"); zh("tutup", "关闭"); zh("cetak", "打印");
		zh("download", "下载"); zh("unduh", "下载"); zh("unggah", "上传"); zh("upload", "上传");
		zh("kembali", "返回"); zh("lanjut", "下一步"); zh("pilih", "选择"); zh("refresh", "刷新");
		zh("bantuan", "帮助"); zh("ya", "是"); zh("tidak", "否"); zh("tampilkan", "显示");
		zh("masuk", "登录"); zh("keluar", "退出"); zh("kirim", "发送"); zh("terima", "接收");
		zh("proses", "处理"); zh("mulai", "开始"); zh("selesai", "完成"); zh("buka", "打开");
		zh("klik", "点击"); zh("edit", "编辑"); zh("setuju", "批准"); zh("tolak", "拒绝");
		// ---- Kata umum ----
		zh("data", "数据"); zh("nama", "名称"); zh("kode", "代码"); zh("alamat", "地址");
		zh("tanggal", "日期"); zh("waktu", "时间"); zh("jumlah", "数量"); zh("status", "状态");
		zh("keterangan", "说明"); zh("informasi", "信息"); zh("peringatan", "警告");
		zh("kesalahan", "错误"); zh("konfirmasi", "确认"); zh("berhasil", "成功"); zh("gagal", "失败");
		zh("wajib", "必填"); zh("aktif", "启用"); zh("nonaktif", "禁用"); zh("dan", "和"); zh("atau", "或");
		zh("semua", "全部"); zh("baru", "新"); zh("lama", "旧"); zh("total", "合计");
		// ---- Domain akademik ----
		zh("mahasiswa", "学生"); zh("dosen", "讲师"); zh("siswa", "学生"); zh("guru", "教师");
		zh("pegawai", "员工"); zh("nilai", "成绩"); zh("perkuliahan", "课程"); zh("matakuliah", "科目");
		zh("pelajaran", "课程"); zh("kelas", "班级"); zh("jadwal", "日程"); zh("semester", "学期");
		zh("ganjil", "单"); zh("genap", "双"); zh("tahun", "年"); zh("akademik", "学术");
		zh("fakultas", "学院"); zh("jurusan", "系"); zh("program", "项目"); zh("kurikulum", "课程大纲");
		zh("ujian", "考试"); zh("tugas", "作业"); zh("kehadiran", "出勤"); zh("presensi", "考勤");
		zh("absensi", "考勤"); zh("materi", "教材"); zh("pertemuan", "会议"); zh("diskusi", "讨论");
		zh("pengumuman", "公告"); zh("kegiatan", "活动"); zh("prestasi", "成就"); zh("alumni", "校友");
		zh("wisuda", "毕业典礼"); zh("lulus", "通过"); zh("pendaftaran", "注册"); zh("registrasi", "注册");
		// ---- Keuangan ----
		zh("pembayaran", "付款"); zh("tagihan", "账单"); zh("biaya", "费用"); zh("bayar", "支付");
		zh("keuangan", "财务"); zh("gaji", "工资"); zh("kas", "现金"); zh("bank", "银行");
		zh("rekening", "账户"); zh("transfer", "转账"); zh("tunai", "现金"); zh("pajak", "税");
		zh("diskon", "折扣"); zh("denda", "罚款"); zh("saldo", "余额"); zh("anggaran", "预算");
		// ---- UI / sistem ----
		zh("laporan", "报告"); zh("pengaturan", "设置"); zh("konfigurasi", "配置"); zh("dasbor", "仪表板");
		zh("dashboard", "仪表板"); zh("pengguna", "用户"); zh("akun", "账户"); zh("akses", "访问");
		zh("halaman", "页面"); zh("tombol", "按钮"); zh("tabel", "表格"); zh("kolom", "列");
		zh("gambar", "图片"); zh("file", "文件"); zh("dokumen", "文档"); zh("menu", "菜单");
		zh("sistem", "系统"); zh("aplikasi", "应用"); zh("jenis", "类型"); zh("tipe", "类型");
		zh("kategori", "类别"); zh("item", "项目"); zh("kelompok", "组"); zh("grup", "组");
		zh("anggota", "成员"); zh("lokasi", "位置"); zh("ruang", "房间"); zh("hari", "天");
		// ---- Institusi ----
		zh("sekolah", "学校"); zh("kampus", "校园"); zh("koperasi", "合作社"); zh("kantin", "食堂");
		zh("perpustakaan", "图书馆"); zh("pustaka", "图书馆"); zh("buku", "书"); zh("surat", "信函");
		zh("yayasan", "基金会"); zh("institusi", "机构"); zh("organisasi", "组织"); zh("kepegawaian", "人事");
		// ---- Waktu ----
		zh("bulan", "月"); zh("minggu", "周"); zh("jam", "小时"); zh("menit", "分钟");
		zh("hari", "天"); zh("sekarang", "现在"); zh("periode", "期间"); zh("harian", "每日");
		zh("bulanan", "每月"); zh("tahunan", "每年");

		// ---- kata fungsional agar glosa lebih baik ----
		zh("dari", "从"); zh("untuk", "为"); zh("dengan", "与"); zh("pada", "在");
		zh("ini", "这"); zh("itu", "那"); zh("mohon", "请"); zh("silakan", "请");
	}

	/**
	 * Terjemahkan {@code teksIndonesia} ke bahasa target ("english"/"arab"/"mandarin" atau kode ZK setara).
	 * Manual-assisted: kata tak dikenal dibiarkan; placeholder {@code {V1}} &amp; tanda baca dipertahankan.
	 */
	// ================================================================================================
	//  KORPUS TAMBAHAN — kata-fungsi/penghubung + akar frekuensi-tinggi + frasa halaman bantuan.
	//  Dengan stemmer + segmentasi longest-match, korpus AKAR ini otomatis menutup banyak bentuk berimbuhan
	//  (mis. "guna" menutup meng-guna-kan/di-guna-kan/peng-guna/peng-guna-an) → prosa bantuan lebih mengalir.
	// ================================================================================================
	static {
		// ---- Kata penghubung / fungsi (agar prosa tidak menyisakan kata Indonesia) ----
		en("dan", "and"); ar("dan", "و"); zh("dan", "和");
		en("atau", "or"); ar("atau", "أو"); zh("atau", "或");
		en("yang", "which"); ar("yang", "الذي"); zh("yang", "的");
		en("untuk", "for"); ar("untuk", "لـ"); zh("untuk", "用于");
		en("dengan", "with"); ar("dengan", "مع"); zh("dengan", "使用");
		en("pada", "at"); ar("pada", "في"); zh("pada", "在");
		en("dari", "from"); ar("dari", "من"); zh("dari", "从");
		en("dalam", "in"); ar("dalam", "في"); zh("dalam", "在...中");
		en("ini", "this"); ar("ini", "هذا"); zh("ini", "这个");
		en("itu", "that"); ar("itu", "ذلك"); zh("itu", "那个");
		en("tersebut", "the"); ar("tersebut", "المذكور"); zh("tersebut", "该");
		en("jika", "if"); ar("jika", "إذا"); zh("jika", "如果");
		en("maka", "then"); ar("maka", "إذن"); zh("maka", "那么");
		en("akan", "will"); ar("akan", "سوف"); zh("akan", "将");
		en("dapat", "can"); ar("dapat", "يمكن"); zh("dapat", "可以");
		en("bisa", "can"); ar("bisa", "يمكن"); zh("bisa", "能");
		en("harus", "must"); ar("harus", "يجب"); zh("harus", "必须");
		en("sebagai", "as"); ar("sebagai", "كـ"); zh("sebagai", "作为");
		en("agar", "so that"); ar("agar", "لكي"); zh("agar", "以便");
		en("supaya", "so that"); ar("supaya", "لكي"); zh("supaya", "以便");
		en("karena", "because"); ar("karena", "لأن"); zh("karena", "因为");
		en("sehingga", "so that"); ar("sehingga", "بحيث"); zh("sehingga", "从而");
		en("setelah", "after"); ar("setelah", "بعد"); zh("setelah", "之后");
		en("sebelum", "before"); ar("sebelum", "قبل"); zh("sebelum", "之前");
		en("saat", "when"); ar("saat", "عند"); zh("saat", "当");
		en("ketika", "when"); ar("ketika", "عندما"); zh("ketika", "当");
		en("setiap", "each"); ar("setiap", "كل"); zh("setiap", "每个");
		en("semua", "all"); ar("semua", "الكل"); zh("semua", "全部");
		en("hanya", "only"); ar("hanya", "فقط"); zh("hanya", "仅");
		en("juga", "also"); ar("juga", "أيضا"); zh("juga", "也");
		en("tanpa", "without"); ar("tanpa", "بدون"); zh("tanpa", "没有");
		en("sudah", "already"); ar("sudah", "بالفعل"); zh("sudah", "已经");
		en("belum", "not yet"); ar("belum", "لم يتم بعد"); zh("belum", "尚未");
		en("sedang", "currently"); ar("sedang", "حاليا"); zh("sedang", "正在");
		en("lagi", "again"); ar("lagi", "مرة أخرى"); zh("lagi", "再次");
		en("silakan", "please"); ar("silakan", "من فضلك"); zh("silakan", "请");
		en("mohon", "please"); ar("mohon", "يرجى"); zh("mohon", "请");
		en("harap", "please"); ar("harap", "يرجى"); zh("harap", "请");
		en("tunggu", "wait"); ar("tunggu", "انتظر"); zh("tunggu", "等待");
		en("mengenai", "regarding"); ar("mengenai", "بخصوص"); zh("mengenai", "关于");
		en("melalui", "through"); ar("melalui", "عبر"); zh("melalui", "通过");
		en("sesuai", "according to"); ar("sesuai", "وفقا"); zh("sesuai", "根据");

		// ---- Akar frekuensi-tinggi (menutup banyak bentuk berimbuhan lewat stemmer) ----
		en("guna", "use"); ar("guna", "استخدام"); zh("guna", "使用");
		en("atur", "set"); ar("atur", "ضبط"); zh("atur", "设置");
		en("kelola", "manage"); ar("kelola", "إدارة"); zh("kelola", "管理");
		en("muat", "load"); ar("muat", "تحميل"); zh("muat", "加载");
		en("tampil", "display"); ar("tampil", "عرض"); zh("tampil", "显示");
		en("lihat", "view"); ar("lihat", "عرض"); zh("lihat", "查看");
		en("isi", "fill"); ar("isi", "ملء"); zh("isi", "填写");
		en("pilih", "select"); ar("pilih", "اختيار"); zh("pilih", "选择");
		en("daftar", "list"); ar("daftar", "قائمة"); zh("daftar", "列表");
		en("catat", "record"); ar("catat", "تسجيل"); zh("catat", "记录");
		en("hitung", "count"); ar("hitung", "حساب"); zh("hitung", "计算");
		en("bayar", "pay"); ar("bayar", "دفع"); zh("bayar", "支付");
		en("kirim", "send"); ar("kirim", "إرسال"); zh("kirim", "发送");
		en("terima", "receive"); ar("terima", "استلام"); zh("terima", "接收");
		en("simpan", "save"); ar("simpan", "حفظ"); zh("simpan", "保存");
		en("hapus", "delete"); ar("hapus", "حذف"); zh("hapus", "删除");
		en("ubah", "change"); ar("ubah", "تغيير"); zh("ubah", "修改");
		en("tambah", "add"); ar("tambah", "إضافة"); zh("tambah", "添加");
		en("cari", "search"); ar("cari", "بحث"); zh("cari", "搜索");
		en("buka", "open"); ar("buka", "فتح"); zh("buka", "打开");
		en("tutup", "close"); ar("tutup", "إغلاق"); zh("tutup", "关闭");
		en("coba", "try"); ar("coba", "حاول"); zh("coba", "尝试");
		en("urut", "sort"); ar("urut", "ترتيب"); zh("urut", "排序");
		en("saring", "filter"); ar("saring", "تصفية"); zh("saring", "筛选");

		// ---- Kata benda / status umum ----
		en("halaman", "page"); ar("halaman", "صفحة"); zh("halaman", "页面");
		en("kolom", "column"); ar("kolom", "عمود"); zh("kolom", "列");
		en("baris", "row"); ar("baris", "صف"); zh("baris", "行");
		en("tombol", "button"); ar("tombol", "زر"); zh("tombol", "按钮");
		en("pesan", "message"); ar("pesan", "رسالة"); zh("pesan", "消息");
		en("informasi", "information"); ar("informasi", "معلومات"); zh("informasi", "信息");
		en("catatan", "note"); ar("catatan", "ملاحظة"); zh("catatan", "备注");
		en("keterangan", "description"); ar("keterangan", "وصف"); zh("keterangan", "说明");
		en("wajib", "required"); ar("wajib", "إلزامي"); zh("wajib", "必填");
		en("opsional", "optional"); ar("opsional", "اختياري"); zh("opsional", "可选");
		en("kosong", "empty"); ar("kosong", "فارغ"); zh("kosong", "空");
		en("benar", "correct"); ar("benar", "صحيح"); zh("benar", "正确");
		en("salah", "wrong"); ar("salah", "خطأ"); zh("salah", "错误");
		en("berhasil", "success"); ar("berhasil", "نجح"); zh("berhasil", "成功");
		en("gagal", "failed"); ar("gagal", "فشل"); zh("gagal", "失败");
		en("selesai", "finished"); ar("selesai", "انتهى"); zh("selesai", "完成");
		en("jumlah", "total"); ar("jumlah", "مجموع"); zh("jumlah", "数量");

		// ---- Frasa halaman bantuan (dicocokkan sebagai satu unit oleh longest-match) ----
		frasa("klik tombol", "click the button", "انقر على الزر");
		frasaZh("klik tombol", "点击按钮");
		frasa("silakan pilih", "please select", "يرجى الاختيار");
		frasaZh("silakan pilih", "请选择");
		frasa("silakan isi", "please fill in", "يرجى الملء");
		frasaZh("silakan isi", "请填写");
		frasa("harap tunggu", "please wait", "يرجى الانتظار");
		frasaZh("harap tunggu", "请稍候");
		frasa("mohon tunggu", "please wait", "يرجى الانتظار");
		frasaZh("mohon tunggu", "请稍候");
		frasa("tidak ada data", "no data", "لا توجد بيانات");
		frasaZh("tidak ada data", "没有数据");
		frasa("data tidak ditemukan", "data not found", "لم يتم العثور على البيانات");
		frasaZh("data tidak ditemukan", "未找到数据");
		frasa("wajib diisi", "must be filled in", "يجب ملؤه");
		frasaZh("wajib diisi", "必须填写");
		frasa("kata sandi", "password", "كلمة المرور");
		frasaZh("kata sandi", "密码");
		frasa("nama pengguna", "username", "اسم المستخدم");
		frasaZh("nama pengguna", "用户名");
		frasa("mata kuliah", "course", "مقرر دراسي");
		frasaZh("mata kuliah", "课程");
		frasa("tahun akademik", "academic year", "السنة الأكاديمية");
		frasaZh("tahun akademik", "学年");
		frasa("tahun ajaran", "school year", "العام الدراسي");
		frasaZh("tahun ajaran", "学年");
		frasa("data berhasil diperbarui", "Data updated successfully", "تم تحديث البيانات بنجاح");
		frasaZh("data berhasil diperbarui", "数据更新成功");
		frasa("apakah anda yakin ingin menghapus", "Are you sure you want to delete",
				"هل أنت متأكد أنك تريد الحذف");
		frasaZh("apakah anda yakin ingin menghapus", "您确定要删除吗");
		frasa("kembali ke halaman utama", "back to the main page", "العودة إلى الصفحة الرئيسية");
		frasaZh("kembali ke halaman utama", "返回主页");
		frasa("pengaturan berhasil disimpan", "Settings saved successfully", "تم حفظ الإعدادات بنجاح");
		frasaZh("pengaturan berhasil disimpan", "设置保存成功");
	}

	// ================================================================================================
	//  KORPUS BESAR dari korpus label NYATA (LabelBahasa 26.776 baris) — memperkecil hasil campur-bahasa.
	//  Kata Indonesia frekuensi-tinggi + loanword/teknis (EN=asli, AR/ZH baku) + akronim akademik + frasa.
	// ================================================================================================
	static {
		en("absensi", "Attendance"); ar("absensi", "الحضور"); zh("absensi", "考勤");
		en("kehadiran", "attendance"); ar("kehadiran", "حضور"); zh("kehadiran", "出勤");
		en("kedatangan", "arrival"); ar("kedatangan", "الوصول"); zh("kedatangan", "到达");
		en("kepulangan", "departure"); ar("kepulangan", "المغادرة"); zh("kepulangan", "离开");
		en("otomatis", "automatic"); ar("otomatis", "تلقائي"); zh("otomatis", "自动");
		en("terintegrasi", "integrated"); ar("terintegrasi", "متكامل"); zh("terintegrasi", "集成");
		en("integrasi", "integration"); ar("integrasi", "تكامل"); zh("integrasi", "集成");
		en("mapel", "subject"); ar("mapel", "مادة"); zh("mapel", "科目");
		en("limitasi", "limitation"); ar("limitasi", "تحديد"); zh("limitasi", "限制");
		en("radius", "radius"); ar("radius", "نطاق"); zh("radius", "半径");
		en("lokasi", "location"); ar("lokasi", "موقع"); zh("lokasi", "位置");
		en("kartu", "card"); ar("kartu", "بطاقة"); zh("kartu", "卡");
		en("kerja", "work"); ar("kerja", "عمل"); zh("kerja", "工作");
		en("bekerja", "work"); ar("bekerja", "يعمل"); zh("bekerja", "工作");
		en("serta", "and"); ar("serta", "و"); zh("serta", "以及");
		en("dahulu", "first"); ar("dahulu", "أولا"); zh("dahulu", "首先");
		en("dulu", "first"); ar("dulu", "أولا"); zh("dulu", "先");
		en("kata", "word"); ar("kata", "كلمة"); zh("kata", "词");
		en("pengabdian", "community service"); ar("pengabdian", "خدمة المجتمع"); zh("pengabdian", "社区服务");
		en("merupakan", "is"); ar("merupakan", "يعتبر"); zh("merupakan", "是");
		en("berupa", "in the form of"); ar("berupa", "على شكل"); zh("berupa", "以...形式");
		en("kebutuhan", "need"); ar("kebutuhan", "حاجة"); zh("kebutuhan", "需求");
		en("mendukung", "support"); ar("mendukung", "يدعم"); zh("mendukung", "支持");
		en("ditentukan", "determined"); ar("ditentukan", "محدد"); zh("ditentukan", "确定");
		en("pedagang", "merchant"); ar("pedagang", "تاجر"); zh("pedagang", "商人");
		en("ketercapaian", "achievement"); ar("ketercapaian", "تحقيق"); zh("ketercapaian", "达成");
		en("sandi", "code"); ar("sandi", "رمز"); zh("sandi", "密码");
		en("publikasi", "publication"); ar("publikasi", "نشر"); zh("publikasi", "出版");
		en("akuntansi", "accounting"); ar("akuntansi", "محاسبة"); zh("akuntansi", "会计");
		en("uji", "test"); ar("uji", "اختبار"); zh("uji", "测试");
		en("konsultasi", "consultation"); ar("konsultasi", "استشارة"); zh("konsultasi", "咨询");
		en("kerjasama", "cooperation"); ar("kerjasama", "تعاون"); zh("kerjasama", "合作");
		en("matriks", "matrix"); ar("matriks", "مصفوفة"); zh("matriks", "矩阵");
		en("sumber", "source"); ar("sumber", "مصدر"); zh("sumber", "来源");
		en("belajar", "study"); ar("belajar", "يدرس"); zh("belajar", "学习");
		en("instansi", "agency"); ar("instansi", "جهة"); zh("instansi", "机构");
		en("tenaga", "staff"); ar("tenaga", "كادر"); zh("tenaga", "人员");
		en("beasiswa", "scholarship"); ar("beasiswa", "منحة دراسية"); zh("beasiswa", "奖学金");
		en("kalender", "calendar"); ar("kalender", "تقويم"); zh("kalender", "日历");
		en("siap", "ready"); ar("siap", "جاهز"); zh("siap", "准备好");
		en("skor", "score"); ar("skor", "نتيجة"); zh("skor", "分数");
		en("inventaris", "inventory"); ar("inventaris", "جرد"); zh("inventaris", "库存");
		en("jenjang", "level"); ar("jenjang", "مستوى"); zh("jenjang", "层级");
		en("bidang", "field"); ar("bidang", "مجال"); zh("bidang", "领域");
		en("bobot", "weight"); ar("bobot", "وزن"); zh("bobot", "权重");
		en("tabungan", "savings"); ar("tabungan", "مدخرات"); zh("tabungan", "储蓄");
		en("pendek", "short"); ar("pendek", "قصير"); zh("pendek", "短");
		en("usaha", "business"); ar("usaha", "عمل"); zh("usaha", "业务");
		en("libur", "holiday"); ar("libur", "عطلة"); zh("libur", "假期");
		en("berjalan", "running"); ar("berjalan", "يعمل"); zh("berjalan", "运行");
		en("pendukung", "supporting"); ar("pendukung", "داعم"); zh("pendukung", "支持");
		en("lunas", "paid off"); ar("lunas", "مسدد"); zh("lunas", "已付清");
		en("distribusi", "distribution"); ar("distribusi", "توزيع"); zh("distribusi", "分配");
		en("berbasis", "based on"); ar("berbasis", "قائم على"); zh("berbasis", "基于");
		en("indikator", "indicator"); ar("indikator", "مؤشر"); zh("indikator", "指标");
		en("kepala", "head"); ar("kepala", "رئيس"); zh("kepala", "负责人");
		en("ekonomi", "economy"); ar("ekonomi", "اقتصاد"); zh("ekonomi", "经济");
		en("individu", "individual"); ar("individu", "فرد"); zh("individu", "个人");
		en("resmi", "official"); ar("resmi", "رسمي"); zh("resmi", "正式");
		en("pisahkan", "separate"); ar("pisahkan", "افصل"); zh("pisahkan", "分隔");
		en("paling", "most"); ar("paling", "الأكثر"); zh("paling", "最");
		en("saudara", "sibling"); ar("saudara", "أخ"); zh("saudara", "兄弟姐妹");
		en("komprehensif", "comprehensive"); ar("komprehensif", "شامل"); zh("komprehensif", "综合");
		en("cek", "check"); ar("cek", "تحقق"); zh("cek", "检查");
		en("meja", "desk"); ar("meja", "مكتب"); zh("meja", "桌");
		en("pelatihan", "training"); ar("pelatihan", "تدريب"); zh("pelatihan", "培训");
		en("tiap", "each"); ar("tiap", "كل"); zh("tiap", "每");
		en("terkait", "related"); ar("terkait", "مرتبط"); zh("terkait", "相关");
		en("perbandingan", "comparison"); ar("perbandingan", "مقارنة"); zh("perbandingan", "比较");
		en("media", "media"); ar("media", "وسائط"); zh("media", "媒体");
		en("perusahaan", "company"); ar("perusahaan", "شركة"); zh("perusahaan", "公司");
		en("ajukan", "submit"); ar("ajukan", "قدم"); zh("ajukan", "提交");
		en("diajukan", "submitted"); ar("diajukan", "مقدم"); zh("diajukan", "已提交");
		en("kecamatan", "district"); ar("kecamatan", "منطقة"); zh("kecamatan", "区");
		en("kami", "we"); ar("kami", "نحن"); zh("kami", "我们");
		en("belakang", "back"); ar("belakang", "خلف"); zh("belakang", "后");
		en("perlu", "need"); ar("perlu", "يحتاج"); zh("perlu", "需要");
		en("masyarakat", "community"); ar("masyarakat", "مجتمع"); zh("masyarakat", "社会");
		en("antarmuka", "interface"); ar("antarmuka", "واجهة"); zh("antarmuka", "界面");
		en("bagi", "for"); ar("bagi", "لـ"); zh("bagi", "为");
		en("komentar", "comment"); ar("komentar", "تعليق"); zh("komentar", "评论");
		en("huruf", "letter"); ar("huruf", "حرف"); zh("huruf", "字母");
		en("standar", "standard"); ar("standar", "معيار"); zh("standar", "标准");
		en("versi", "version"); ar("versi", "إصدار"); zh("versi", "版本");
		en("pembelian", "purchase"); ar("pembelian", "شراء"); zh("pembelian", "采购");
		en("sirkulasi", "circulation"); ar("sirkulasi", "تداول"); zh("sirkulasi", "流通");
		en("proposal", "proposal"); ar("proposal", "مقترح"); zh("proposal", "提案");
		en("katalog", "catalog"); ar("katalog", "فهرس"); zh("katalog", "目录");
		en("tertentu", "certain"); ar("tertentu", "معين"); zh("tertentu", "特定");
		en("sendiri", "own"); ar("sendiri", "خاص"); zh("sendiri", "自己");
		en("tautan", "link"); ar("tautan", "رابط"); zh("tautan", "链接");
		en("fasilitas", "facility"); ar("fasilitas", "مرفق"); zh("fasilitas", "设施");
		en("kasir", "cashier"); ar("kasir", "أمين الصندوق"); zh("kasir", "收银员");
		en("sejarah", "history"); ar("sejarah", "تاريخ"); zh("sejarah", "历史");
		en("pastikan", "make sure"); ar("pastikan", "تأكد"); zh("pastikan", "确保");
		en("rentang", "range"); ar("rentang", "نطاق"); zh("rentang", "范围");
		en("mudah", "easy"); ar("mudah", "سهل"); zh("mudah", "容易");
		en("prasyarat", "prerequisite"); ar("prasyarat", "متطلب مسبق"); zh("prasyarat", "先决条件");
		en("pengembangan", "development"); ar("pengembangan", "تطوير"); zh("pengembangan", "开发");
		en("diperbarui", "updated"); ar("diperbarui", "محدث"); zh("diperbarui", "已更新");
		en("propinsi", "province"); ar("propinsi", "محافظة"); zh("propinsi", "省");
		en("provinsi", "province"); ar("provinsi", "محافظة"); zh("provinsi", "省");
		en("aktifitas", "activity"); ar("aktifitas", "نشاط"); zh("aktifitas", "活动");
		en("aktivitas", "activity"); ar("aktivitas", "نشاط"); zh("aktivitas", "活动");
		en("aman", "safe"); ar("aman", "آمن"); zh("aman", "安全");
		en("kursus", "course"); ar("kursus", "دورة"); zh("kursus", "课程");
		en("muncul", "appear"); ar("muncul", "يظهر"); zh("muncul", "出现");
		en("ganda", "double"); ar("ganda", "مزدوج"); zh("ganda", "双");
		en("spesifik", "specific"); ar("spesifik", "محدد"); zh("spesifik", "具体");
		en("kuesioner", "questionnaire"); ar("kuesioner", "استبيان"); zh("kuesioner", "问卷");
		en("implementasi", "implementation"); ar("implementasi", "تنفيذ"); zh("implementasi", "实施");
		en("keluarga", "family"); ar("keluarga", "عائلة"); zh("keluarga", "家庭");
		en("tertib", "orderly"); ar("tertib", "منظم"); zh("tertib", "有序");
		en("saluran", "channel"); ar("saluran", "قناة"); zh("saluran", "渠道");
		en("penjelasan", "explanation"); ar("penjelasan", "شرح"); zh("penjelasan", "说明");
		en("staf", "staff"); ar("staf", "موظف"); zh("staf", "员工");
		en("akta", "certificate"); ar("akta", "شهادة"); zh("akta", "证书");
		en("penyusutan", "depreciation"); ar("penyusutan", "إهلاك"); zh("penyusutan", "折旧");
		en("kabupaten", "regency"); ar("kabupaten", "منطقة"); zh("kabupaten", "县");
		en("antar", "between"); ar("antar", "بين"); zh("antar", "之间");
		en("cabang", "branch"); ar("cabang", "فرع"); zh("cabang", "分支");
		en("pokok", "principal"); ar("pokok", "أساسي"); zh("pokok", "基本");
		en("pensiun", "pension"); ar("pensiun", "تقاعد"); zh("pensiun", "退休");
		en("pemakaian", "usage"); ar("pemakaian", "استخدام"); zh("pemakaian", "使用");
		en("kalkulasi", "calculation"); ar("kalkulasi", "حساب"); zh("kalkulasi", "计算");
		en("mutu", "quality"); ar("mutu", "جودة"); zh("mutu", "质量");
		en("banyak", "many"); ar("banyak", "كثير"); zh("banyak", "许多");
		en("toleransi", "tolerance"); ar("toleransi", "تسامح"); zh("toleransi", "容差");
		en("tindak", "action"); ar("tindak", "إجراء"); zh("tindak", "行动");
		en("tangan", "hand"); ar("tangan", "يد"); zh("tangan", "手");
		en("aspek", "aspect"); ar("aspek", "جانب"); zh("aspek", "方面");
		en("pantau", "monitor"); ar("pantau", "راقب"); zh("pantau", "监控");
		en("konversi", "conversion"); ar("konversi", "تحويل"); zh("konversi", "转换");
		en("deposit", "deposit"); ar("deposit", "وديعة"); zh("deposit", "存款");
		en("les", "tutoring"); ar("les", "دروس خصوصية"); zh("les", "辅导");
		en("slip", "slip"); ar("slip", "قسيمة"); zh("slip", "单据");
		en("sinkronisasi", "synchronization"); ar("sinkronisasi", "مزامنة"); zh("sinkronisasi", "同步");
		en("sinkronkan", "synchronize"); ar("sinkronkan", "زامن"); zh("sinkronkan", "同步");
		en("singkronkan", "synchronize"); ar("singkronkan", "زامن"); zh("singkronkan", "同步");
		en("pemetaan", "mapping"); ar("pemetaan", "تخطيط"); zh("pemetaan", "映射");
		en("angka", "number"); ar("angka", "رقم"); zh("angka", "数字");
		en("centang", "check"); ar("centang", "علامة"); zh("centang", "勾选");
		en("selamat", "welcome"); ar("selamat", "مرحبا"); zh("selamat", "欢迎");
		en("pelaksanaan", "implementation"); ar("pelaksanaan", "تنفيذ"); zh("pelaksanaan", "执行");
		en("perbaikan", "repair"); ar("perbaikan", "إصلاح"); zh("perbaikan", "修复");
		en("tujuan", "purpose"); ar("tujuan", "هدف"); zh("tujuan", "目的");
		en("alpa", "absent"); ar("alpa", "غائب"); zh("alpa", "缺席");
		en("penuh", "full"); ar("penuh", "ممتلئ"); zh("penuh", "满");
		en("mutasi", "transfer"); ar("mutasi", "نقل"); zh("mutasi", "调动");
		en("lowongan", "vacancy"); ar("lowongan", "وظيفة شاغرة"); zh("lowongan", "职位空缺");
		en("masih", "still"); ar("masih", "لا يزال"); zh("masih", "仍然");
		en("jalur", "path"); ar("jalur", "مسار"); zh("jalur", "路径");
		en("arsip", "archive"); ar("arsip", "أرشيف"); zh("arsip", "档案");
		en("inggris", "English"); ar("inggris", "الإنجليزية"); zh("inggris", "英语");
		en("berapa", "how many"); ar("berapa", "كم"); zh("berapa", "多少");
		en("kualitas", "quality"); ar("kualitas", "جودة"); zh("kualitas", "质量");
		en("tamu", "guest"); ar("tamu", "ضيف"); zh("tamu", "访客");
		en("datang", "arrive"); ar("datang", "يصل"); zh("datang", "到达");
		en("repositori", "repository"); ar("repositori", "مستودع"); zh("repositori", "仓库");
		en("acara", "event"); ar("acara", "حدث"); zh("acara", "活动");
		en("ajar", "teach"); ar("ajar", "يعلم"); zh("ajar", "教");
		en("topik", "topic"); ar("topik", "موضوع"); zh("topik", "主题");
		en("rinci", "detailed"); ar("rinci", "مفصل"); zh("rinci", "详细");
		en("lokal", "local"); ar("lokal", "محلي"); zh("lokal", "本地");
		en("pindah", "move"); ar("pindah", "انقل"); zh("pindah", "移动");
		en("badan", "body"); ar("badan", "هيئة"); zh("badan", "机构");
		en("penjualan", "sales"); ar("penjualan", "مبيعات"); zh("penjualan", "销售");
		en("keamanan", "security"); ar("keamanan", "أمن"); zh("keamanan", "安全");
		en("mana", "which"); ar("mana", "أي"); zh("mana", "哪个");
		en("anjungan", "kiosk"); ar("anjungan", "كشك"); zh("anjungan", "自助终端");
		en("kemampuan", "ability"); ar("kemampuan", "قدرة"); zh("kemampuan", "能力");
		en("diri", "self"); ar("diri", "ذات"); zh("diri", "自身");
		en("beli", "buy"); ar("beli", "اشتر"); zh("beli", "购买");
		en("fisik", "physical"); ar("fisik", "مادي"); zh("fisik", "物理");
		en("galeri", "gallery"); ar("galeri", "معرض"); zh("galeri", "图库");
		en("pengajar", "instructor"); ar("pengajar", "مدرس"); zh("pengajar", "讲师");
		en("teknis", "technical"); ar("teknis", "تقني"); zh("teknis", "技术");
		en("memberikan", "give"); ar("memberikan", "يعطي"); zh("memberikan", "给予");
		en("tentang", "about"); ar("tentang", "حول"); zh("tentang", "关于");
		en("remedial", "remedial"); ar("remedial", "علاجي"); zh("remedial", "补考");
		en("keranjang", "cart"); ar("keranjang", "سلة"); zh("keranjang", "购物车");
		en("konten", "content"); ar("konten", "محتوى"); zh("konten", "内容");
		en("tunggakan", "arrears"); ar("tunggakan", "متأخرات"); zh("tunggakan", "欠款");
		en("kop", "letterhead"); ar("kop", "ترويسة"); zh("kop", "信头");
		en("anak", "child"); ar("anak", "طفل"); zh("anak", "子");
		en("layar", "screen"); ar("layar", "شاشة"); zh("layar", "屏幕");
		en("massal", "mass"); ar("massal", "جماعي"); zh("massal", "批量");
		en("pulang", "go home"); ar("pulang", "العودة"); zh("pulang", "回家");
		en("tentukan", "determine"); ar("tentukan", "حدد"); zh("tentukan", "确定");
		en("sini", "here"); ar("sini", "هنا"); zh("sini", "这里");
		en("prioritas", "priority"); ar("prioritas", "أولوية"); zh("prioritas", "优先级");
		en("memfasilitasi", "facilitate"); ar("memfasilitasi", "تسهيل"); zh("memfasilitasi", "促进");
		en("kendala", "obstacle"); ar("kendala", "عائق"); zh("kendala", "障碍");
		en("seperti", "like"); ar("seperti", "مثل"); zh("seperti", "如");
		en("maupun", "as well as"); ar("maupun", "وكذلك"); zh("maupun", "以及");
		en("hubungi", "contact"); ar("hubungi", "اتصل"); zh("hubungi", "联系");
		en("fungsi", "function"); ar("fungsi", "وظيفة"); zh("fungsi", "功能");
		en("rekam", "record"); ar("rekam", "سجل"); zh("rekam", "记录");
		en("kelurahan", "urban village"); ar("kelurahan", "قرية حضرية"); zh("kelurahan", "社区");
		en("tinggal", "stay"); ar("tinggal", "يقيم"); zh("tinggal", "居住");
		en("alat", "tool"); ar("alat", "أداة"); zh("alat", "工具");
		en("pejabat", "official"); ar("pejabat", "مسؤول"); zh("pejabat", "官员");
		en("pelanggaran", "violation"); ar("pelanggaran", "مخالفة"); zh("pelanggaran", "违规");
		en("pencairan", "disbursement"); ar("pencairan", "صرف"); zh("pencairan", "拨付");
		en("mengenai", "regarding"); ar("mengenai", "بخصوص"); zh("mengenai", "关于");
		en("dilakukan", "done"); ar("dilakukan", "تم"); zh("dilakukan", "完成");
		en("tren", "trend"); ar("tren", "اتجاه"); zh("tren", "趋势");
		en("linimasa", "timeline"); ar("linimasa", "الجدول الزمني"); zh("linimasa", "时间线");
		en("nomor", "number"); ar("nomor", "رقم"); zh("nomor", "号码");
		en("tunai", "cash"); ar("tunai", "نقدا"); zh("tunai", "现金");
		en("piutang", "receivable"); ar("piutang", "ذمم مدينة"); zh("piutang", "应收款");
		en("hutang", "payable"); ar("hutang", "ذمم دائنة"); zh("hutang", "应付款");
		en("jurnal", "journal"); ar("jurnal", "دفتر يومية"); zh("jurnal", "日记账");
		en("neraca", "balance sheet"); ar("neraca", "الميزانية"); zh("neraca", "资产负债表");
		en("modal", "capital"); ar("modal", "رأس المال"); zh("modal", "资本");
		en("saldo", "balance"); ar("saldo", "رصيد"); zh("saldo", "余额");
		en("aset", "asset"); ar("aset", "أصل"); zh("aset", "资产");
		en("login", "Login"); ar("login", "تسجيل الدخول"); zh("login", "登录");
		en("logout", "Logout"); ar("logout", "تسجيل الخروج"); zh("logout", "登出");
		en("online", "Online"); ar("online", "متصل"); zh("online", "在线");
		en("offline", "Offline"); ar("offline", "غير متصل"); zh("offline", "离线");
		en("admin", "Admin"); ar("admin", "مسؤول"); zh("admin", "管理员");
		en("default", "Default"); ar("default", "افتراضي"); zh("default", "默认");
		en("link", "Link"); ar("link", "رابط"); zh("link", "链接");
		en("modul", "Module"); ar("modul", "وحدة"); zh("modul", "模块");
		en("cache", "Cache"); ar("cache", "ذاكرة مؤقتة"); zh("cache", "缓存");
		en("form", "Form"); ar("form", "نموذج"); zh("form", "表单");
		en("portal", "Portal"); ar("portal", "بوابة"); zh("portal", "门户");
		en("home", "Home"); ar("home", "الرئيسية"); zh("home", "主页");
		en("background", "Background"); ar("background", "خلفية"); zh("background", "背景");
		en("password", "Password"); ar("password", "كلمة المرور"); zh("password", "密码");
		en("username", "Username"); ar("username", "اسم المستخدم"); zh("username", "用户名");
		en("database", "Database"); ar("database", "قاعدة بيانات"); zh("database", "数据库");
		en("server", "Server"); ar("server", "خادم"); zh("server", "服务器");
		en("video", "Video"); ar("video", "فيديو"); zh("video", "视频");
		en("audio", "Audio"); ar("audio", "صوت"); zh("audio", "音频");
		en("mobile", "Mobile"); ar("mobile", "جوال"); zh("mobile", "移动端");
		en("preview", "Preview"); ar("preview", "معاينة"); zh("preview", "预览");
		en("error", "Error"); ar("error", "خطأ"); zh("error", "错误");
		en("gateway", "Gateway"); ar("gateway", "بوابة"); zh("gateway", "网关");
		en("generate", "Generate"); ar("generate", "إنشاء"); zh("generate", "生成");
		en("learning", "Learning"); ar("learning", "تعلم"); zh("learning", "学习");
		en("user", "User"); ar("user", "مستخدم"); zh("user", "用户");
		en("barcode", "Barcode"); ar("barcode", "باركود"); zh("barcode", "条形码");
		en("master", "Master"); ar("master", "رئيسي"); zh("master", "主");
		en("footer", "Footer"); ar("footer", "تذييل"); zh("footer", "页脚");
		en("header", "Header"); ar("header", "رأس الصفحة"); zh("header", "页眉");
		en("banner", "Banner"); ar("banner", "لافتة"); zh("banner", "横幅");
		en("template", "Template"); ar("template", "قالب"); zh("template", "模板");
		en("payment", "Payment"); ar("payment", "دفع"); zh("payment", "支付");
		en("report", "Report"); ar("report", "تقرير"); zh("report", "报告");
		en("folder", "Folder"); ar("folder", "مجلد"); zh("folder", "文件夹");
		en("index", "Index"); ar("index", "فهرس"); zh("index", "索引");
		en("drive", "Drive"); ar("drive", "محرك"); zh("drive", "云盘");
		en("setup", "Setup"); ar("setup", "إعداد"); zh("setup", "设置");
		en("input", "Input"); ar("input", "إدخال"); zh("input", "输入");
		en("panel", "Panel"); ar("panel", "لوحة"); zh("panel", "面板");
		en("role", "Role"); ar("role", "دور"); zh("role", "角色");
		en("account", "Account"); ar("account", "حساب"); zh("account", "账户");
		en("profile", "Profile"); ar("profile", "ملف شخصي"); zh("profile", "个人资料");
		en("virtual", "Virtual"); ar("virtual", "افتراضي"); zh("virtual", "虚拟");
		en("monitoring", "Monitoring"); ar("monitoring", "مراقبة"); zh("monitoring", "监控");
		en("monitor", "Monitor"); ar("monitor", "مراقبة"); zh("monitor", "监控");
		en("platform", "Platform"); ar("platform", "منصة"); zh("platform", "平台");
		en("scan", "Scan"); ar("scan", "مسح"); zh("scan", "扫描");
		en("copy", "Copy"); ar("copy", "نسخ"); zh("copy", "复制");
		en("session", "Session"); ar("session", "جلسة"); zh("session", "会话");
		en("log", "Log"); ar("log", "سجل"); zh("log", "日志");
		en("draft", "Draft"); ar("draft", "مسودة"); zh("draft", "草稿");
		en("repository", "Repository"); ar("repository", "مستودع"); zh("repository", "仓库");
		en("maks", "max"); ar("maks", "الحد الأقصى"); zh("maks", "最大");
		en("min", "min"); ar("min", "الحد الأدنى"); zh("min", "最小");
		en("global", "Global"); ar("global", "عام"); zh("global", "全局");
		en("shift", "Shift"); ar("shift", "وردية"); zh("shift", "班次");
		en("prefix", "Prefix"); ar("prefix", "بادئة"); zh("prefix", "前缀");
		en("enterprise", "Enterprise"); ar("enterprise", "مؤسسة"); zh("enterprise", "企业");
		en("auto", "Auto"); ar("auto", "تلقائي"); zh("auto", "自动");
		en("test", "Test"); ar("test", "اختبار"); zh("test", "测试");
		en("member", "Member"); ar("member", "عضو"); zh("member", "成员");
		en("closing", "Closing"); ar("closing", "إغلاق"); zh("closing", "结账");
		en("uts", "Midterm Exam"); ar("uts", "امتحان منتصف الفصل"); zh("uts", "期中考试");
		en("uas", "Final Exam"); ar("uas", "الامتحان النهائي"); zh("uas", "期末考试");
		en("ipk", "GPA"); ar("ipk", "المعدل التراكمي"); zh("ipk", "绩点");
		en("khs", "Grade Report"); ar("khs", "كشف الدرجات"); zh("khs", "成绩单");
		en("sks", "Credits"); ar("sks", "الساعات المعتمدة"); zh("sks", "学分");
		en("nim", "Student ID"); ar("nim", "رقم الطالب"); zh("nim", "学号");
		en("nip", "Employee ID"); ar("nip", "رقم الموظف"); zh("nip", "员工编号");
		en("nis", "Student Number"); ar("nis", "رقم الطالب"); zh("nis", "学籍号");
		en("ktp", "ID Card"); ar("ktp", "بطاقة الهوية"); zh("ktp", "身份证");
		en("npwp", "Tax ID"); ar("npwp", "الرقم الضريبي"); zh("npwp", "税号");
		en("sdm", "HR"); ar("sdm", "الموارد البشرية"); zh("sdm", "人力资源");

		frasa("tatap muka", "face to face", "حضوري"); frasaZh("tatap muka", "面对面");
		frasa("uang muka", "down payment", "دفعة مقدمة"); frasaZh("uang muka", "预付款");
		frasa("peserta didik", "student", "الطالب"); frasaZh("peserta didik", "学生");
		frasa("tata kelola", "governance", "الحوكمة"); frasaZh("tata kelola", "治理");
		frasa("rata rata", "average", "المتوسط"); frasaZh("rata rata", "平均");
		frasa("uji coba", "trial", "تجربة"); frasaZh("uji coba", "试用");
		frasa("semester pendek", "short semester", "الفصل القصير"); frasaZh("semester pendek", "短学期");
		frasa("satuan kerja", "work unit", "وحدة العمل"); frasaZh("satuan kerja", "工作单位");
		frasa("terlebih dahulu", "beforehand", "مسبقا"); frasaZh("terlebih dahulu", "事先");
		frasa("mata pelajaran", "subject", "مادة دراسية"); frasaZh("mata pelajaran", "科目");
		frasa("sumber belajar", "learning resource", "مصدر التعلم"); frasaZh("sumber belajar", "学习资源");
		frasa("tugas individu", "individual assignment", "مهمة فردية"); frasaZh("tugas individu", "个人作业");
		frasa("selamat datang", "welcome", "مرحبا بك"); frasaZh("selamat datang", "欢迎");
		frasa("berita acara", "official report", "محضر"); frasaZh("berita acara", "会议记录");
		frasa("finger print", "fingerprint", "بصمة الإصبع"); frasaZh("finger print", "指纹");
		frasa("secara otomatis", "automatically", "تلقائيا"); frasaZh("secara otomatis", "自动");
		frasa("tahun anggaran", "fiscal year", "السنة المالية"); frasaZh("tahun anggaran", "财政年度");
		frasa("kartu keluarga", "family card", "بطاقة العائلة"); frasaZh("kartu keluarga", "家庭卡");
		frasa("wali kelas", "homeroom teacher", "مربي الفصل"); frasaZh("wali kelas", "班主任");
		frasa("sumber daya manusia", "human resources", "الموارد البشرية"); frasaZh("sumber daya manusia", "人力资源");
		frasa("tenaga kependidikan", "education personnel", "الكادر التعليمي"); frasaZh("tenaga kependidikan", "教职工");
		frasa("nomor induk", "identification number", "الرقم التعريفي"); frasaZh("nomor induk", "学号");
	}

	// ---- KORPUS TAMBAHAN BATCH-2 (kata Indonesia nyata sisa + istilah keuangan/lokasi + frasa biodata) ----
	static {
		// domain absensi/kehadiran
		en("absen", "absent"); ar("absen", "غائب"); zh("absen", "缺勤");
		en("hadir", "present"); ar("hadir", "حاضر"); zh("hadir", "出席");
		en("izin", "permit"); ar("izin", "إذن"); zh("izin", "请假");
		en("sakit", "sick"); ar("sakit", "مريض"); zh("sakit", "病假");
		en("terlambat", "late"); ar("terlambat", "متأخر"); zh("terlambat", "迟到");
		en("lembur", "overtime"); ar("lembur", "عمل إضافي"); zh("lembur", "加班");
		en("dinas", "official duty"); ar("dinas", "مهمة رسمية"); zh("dinas", "公务");
		en("cuti", "leave"); ar("cuti", "إجازة"); zh("cuti", "休假");
		en("pulang", "leave"); ar("pulang", "الانصراف"); zh("pulang", "下班");
		en("masuk", "in"); ar("masuk", "دخول"); zh("masuk", "签到");
		en("keluar", "out"); ar("keluar", "خروج"); zh("keluar", "签退");
		en("aplikasi", "application"); ar("aplikasi", "تطبيق"); zh("aplikasi", "应用");
		// istilah pengumuman / dasbor beranda
		en("utama", "main"); ar("utama", "رئيسي"); zh("utama", "主要");
		en("umum", "public"); ar("umum", "عام"); zh("umum", "公开");
		en("akademik", "academic"); ar("akademik", "أكاديمي"); zh("akademik", "学术");
		en("calon", "prospective"); ar("calon", "مرشح"); zh("calon", "候选");
		en("pengumuman", "announcement"); ar("pengumuman", "إعلان"); zh("pengumuman", "公告");
		en("informasi", "information"); ar("informasi", "معلومات"); zh("informasi", "信息");
		en("panduan", "guide"); ar("panduan", "دليل"); zh("panduan", "指南");
		en("alur", "flow"); ar("alur", "تدفق"); zh("alur", "流程");
		en("contoh", "example"); ar("contoh", "مثال"); zh("contoh", "示例");
		en("tampilan", "view"); ar("tampilan", "عرض"); zh("tampilan", "界面");
		frasa("prioritas utama", "top priority", "أولوية قصوى"); frasaZh("prioritas utama", "首要");
		frasa("untuk umum", "for public", "للعامة"); frasaZh("untuk umum", "面向公众");
		frasa("untuk calon mahasiswa", "for prospective students", "للطلاب المرشحين");
		frasaZh("untuk calon mahasiswa", "面向准大学生");
		frasa("untuk calon siswa", "for prospective students", "للطلاب المرشحين");
		frasaZh("untuk calon siswa", "面向准学生");
		frasa("pengumuman akademik", "academic announcement", "إعلان أكاديمي");
		frasaZh("pengumuman akademik", "学术公告");
		frasa("pengumuman dan informasi", "announcements and information", "الإعلانات والمعلومات");
		frasaZh("pengumuman dan informasi", "公告与信息");
		en("kondisi", "condition"); ar("kondisi", "حالة"); zh("kondisi", "条件");
		en("muka", "front"); ar("muka", "أمام"); zh("muka", "前");
		en("rata", "flat"); ar("rata", "مسطح"); zh("rata", "平");
		en("tata", "arrangement"); ar("tata", "ترتيب"); zh("tata", "布局");
		en("mata", "eye"); ar("mata", "عين"); zh("mata", "眼");
		en("didik", "educate"); ar("didik", "يعلم"); zh("didik", "教育");
		en("masing", "each"); ar("masing", "كل"); zh("masing", "各");
		en("dikali", "times"); ar("dikali", "مضروب في"); zh("dikali", "乘以");
		en("saja", "only"); ar("saja", "فقط"); zh("saja", "仅");
		en("lalu", "then"); ar("lalu", "ثم"); zh("lalu", "然后");
		en("kali", "times"); ar("kali", "مرة"); zh("kali", "次");
		en("habis", "finished"); ar("habis", "انتهى"); zh("habis", "用完");
		en("telpon", "phone"); ar("telpon", "هاتف"); zh("telpon", "电话");
		en("telepon", "phone"); ar("telepon", "هاتف"); zh("telepon", "电话");
		en("telp", "phone"); ar("telp", "هاتف"); zh("telp", "电话");
		en("sertifikasi", "certification"); ar("sertifikasi", "شهادة"); zh("sertifikasi", "认证");
		en("kuota", "quota"); ar("kuota", "حصة"); zh("kuota", "配额");
		en("ekosistem", "ecosystem"); ar("ekosistem", "نظام بيئي"); zh("ekosistem", "生态系统");
		en("korelasi", "correlation"); ar("korelasi", "ارتباط"); zh("korelasi", "相关性");
		en("dirancang", "designed"); ar("dirancang", "مصمم"); zh("dirancang", "设计");
		en("indeks", "index"); ar("indeks", "مؤشر"); zh("indeks", "索引");
		en("diklat", "training"); ar("diklat", "تدريب"); zh("diklat", "培训");
		en("transparan", "transparent"); ar("transparan", "شفاف"); zh("transparan", "透明");
		en("aktivasi", "activation"); ar("aktivasi", "تفعيل"); zh("aktivasi", "激活");
		en("terhadap", "toward"); ar("terhadap", "تجاه"); zh("terhadap", "对于");
		en("arus", "flow"); ar("arus", "تدفق"); zh("arus", "流");
		en("sasaran", "target"); ar("sasaran", "هدف"); zh("sasaran", "目标");
		en("lembar", "sheet"); ar("lembar", "ورقة"); zh("lembar", "页");
		en("beberapa", "several"); ar("beberapa", "عدة"); zh("beberapa", "几个");
		en("bentuk", "form"); ar("bentuk", "شكل"); zh("bentuk", "形式");
		en("jadikan", "make"); ar("jadikan", "اجعل"); zh("jadikan", "使");
		en("fisika", "physics"); ar("fisika", "فيزياء"); zh("fisika", "物理");
		en("pengaduan", "complaint"); ar("pengaduan", "شكوى"); zh("pengaduan", "投诉");
		en("penerbit", "publisher"); ar("penerbit", "ناشر"); zh("penerbit", "出版商");
		en("pengarang", "author"); ar("pengarang", "مؤلف"); zh("pengarang", "作者");
		en("panitia", "committee"); ar("panitia", "لجنة"); zh("panitia", "委员会");
		en("sangat", "very"); ar("sangat", "جدا"); zh("sangat", "非常");
		en("publik", "public"); ar("publik", "عام"); zh("publik", "公众");
		en("aktor", "actor"); ar("aktor", "فاعل"); zh("aktor", "角色");
		en("luar", "outside"); ar("luar", "خارج"); zh("luar", "外部");
		en("perjanjian", "agreement"); ar("perjanjian", "اتفاقية"); zh("perjanjian", "协议");
		en("pemilik", "owner"); ar("pemilik", "مالك"); zh("pemilik", "所有者");
		en("instruksi", "instruction"); ar("instruksi", "تعليمات"); zh("instruksi", "指令");
		en("apabila", "if"); ar("apabila", "إذا"); zh("apabila", "如果");
		en("permanen", "permanent"); ar("permanen", "دائم"); zh("permanen", "永久");
		en("solusi", "solution"); ar("solusi", "حل"); zh("solusi", "解决方案");
		en("praktik", "practice"); ar("praktik", "ممارسة"); zh("praktik", "实践");
		en("praktek", "practice"); ar("praktek", "ممارسة"); zh("praktek", "实践");
		en("ketik", "type"); ar("ketik", "اكتب"); zh("ketik", "输入");
		en("luas", "wide"); ar("luas", "واسع"); zh("luas", "宽");
		en("terputus", "disconnected"); ar("terputus", "منقطع"); zh("terputus", "断开");
		en("antrian", "queue"); ar("antrian", "طابور"); zh("antrian", "队列");
		en("baca", "read"); ar("baca", "اقرأ"); zh("baca", "阅读");
		en("komparasi", "comparison"); ar("komparasi", "مقارنة"); zh("komparasi", "比较");
		en("artikel", "article"); ar("artikel", "مقال"); zh("artikel", "文章");
		en("operator", "operator"); ar("operator", "مشغل"); zh("operator", "操作员");
		en("islam", "Islam"); ar("islam", "الإسلام"); zh("islam", "伊斯兰教");
		en("asuransi", "insurance"); ar("asuransi", "تأمين"); zh("asuransi", "保险");
		en("analitik", "analytics"); ar("analitik", "تحليلات"); zh("analitik", "分析");
		en("internet", "Internet"); ar("internet", "إنترنت"); zh("internet", "互联网");
		en("modern", "modern"); ar("modern", "حديث"); zh("modern", "现代");
		en("promo", "promo"); ar("promo", "عرض"); zh("promo", "促销");
		en("administrator", "administrator"); ar("administrator", "مدير"); zh("administrator", "管理员");
		en("pesantren", "Islamic boarding school"); ar("pesantren", "مدرسة داخلية إسلامية"); zh("pesantren", "伊斯兰寄宿学校");
		en("tracer", "tracer"); ar("tracer", "متتبع"); zh("tracer", "追踪");
		en("education", "Education"); ar("education", "تعليم"); zh("education", "教育");
		en("lembaga", "institution"); ar("lembaga", "مؤسسة"); zh("lembaga", "机构");
		en("kompetensi", "competence"); ar("kompetensi", "كفاءة"); zh("kompetensi", "能力");
		en("cashback", "cashback"); ar("cashback", "استرداد نقدي"); zh("cashback", "返现");
		en("kompetisi", "competition"); ar("kompetisi", "مسابقة"); zh("kompetisi", "竞赛");
		en("juri", "judge"); ar("juri", "حكم"); zh("juri", "评委");
		en("penilai", "assessor"); ar("penilai", "مقيم"); zh("penilai", "评估者");
		en("dinilai", "assessed"); ar("dinilai", "مقيم"); zh("dinilai", "被评估");
		en("terhitung", "counted"); ar("terhitung", "محسوب"); zh("terhitung", "计入");
		en("berlaku", "valid"); ar("berlaku", "ساري"); zh("berlaku", "有效");
		en("kedaluwarsa", "expired"); ar("kedaluwarsa", "منتهي الصلاحية"); zh("kedaluwarsa", "过期");
		en("wilayah", "region"); ar("wilayah", "منطقة"); zh("wilayah", "地区");
		en("negara", "country"); ar("negara", "دولة"); zh("negara", "国家");
		en("kota", "city"); ar("kota", "مدينة"); zh("kota", "城市");
		en("desa", "village"); ar("desa", "قرية"); zh("desa", "村庄");
		en("dusun", "hamlet"); ar("dusun", "قرية صغيرة"); zh("dusun", "村落");
		en("gedung", "building"); ar("gedung", "مبنى"); zh("gedung", "建筑");
		en("lantai", "floor"); ar("lantai", "طابق"); zh("lantai", "楼层");
		en("kamar", "room"); ar("kamar", "غرفة"); zh("kamar", "房间");
		en("gudang", "warehouse"); ar("gudang", "مستودع"); zh("gudang", "仓库");
		en("toko", "store"); ar("toko", "متجر"); zh("toko", "商店");
		en("barang", "goods"); ar("barang", "بضاعة"); zh("barang", "货物");
		en("satuan", "unit"); ar("satuan", "وحدة"); zh("satuan", "单位");
		en("merk", "brand"); ar("merk", "علامة تجارية"); zh("merk", "品牌");
		en("merek", "brand"); ar("merek", "علامة تجارية"); zh("merek", "品牌");
		en("stok", "stock"); ar("stok", "مخزون"); zh("stok", "库存");
		en("harga", "price"); ar("harga", "سعر"); zh("harga", "价格");
		en("diskon", "discount"); ar("diskon", "خصم"); zh("diskon", "折扣");
		en("pajak", "tax"); ar("pajak", "ضريبة"); zh("pajak", "税");
		en("kwitansi", "receipt"); ar("kwitansi", "إيصال"); zh("kwitansi", "收据");
		en("faktur", "invoice"); ar("faktur", "فاتورة"); zh("faktur", "发票");
		en("anggaran", "budget"); ar("anggaran", "ميزانية"); zh("anggaran", "预算");
		en("realisasi", "realization"); ar("realisasi", "تحقيق"); zh("realisasi", "实现");
		en("pemasukan", "income"); ar("pemasukan", "دخل"); zh("pemasukan", "收入");
		en("pengeluaran", "expenditure"); ar("pengeluaran", "نفقة"); zh("pengeluaran", "支出");
		en("laba", "profit"); ar("laba", "ربح"); zh("laba", "利润");
		en("rugi", "loss"); ar("rugi", "خسارة"); zh("rugi", "亏损");
		en("transaksi", "transaction"); ar("transaksi", "معاملة"); zh("transaksi", "交易");
		en("mutasi", "transfer"); ar("mutasi", "حركة"); zh("mutasi", "变动");
		en("rekening", "account"); ar("rekening", "حساب بنكي"); zh("rekening", "银行账户");
		en("cabang", "branch"); ar("cabang", "فرع"); zh("cabang", "分行");

		frasa("masing masing", "each", "كل واحد"); frasaZh("masing masing", "各自");
		frasa("tracer study", "tracer study", "دراسة التتبع"); frasaZh("tracer study", "毕业生追踪");
		frasa("uang saku", "allowance", "مصروف"); frasaZh("uang saku", "津贴");
		frasa("jam kerja", "working hours", "ساعات العمل"); frasaZh("jam kerja", "工作时间");
		frasa("hari kerja", "working day", "يوم عمل"); frasaZh("hari kerja", "工作日");
		frasa("kartu tanda penduduk", "ID card", "بطاقة الهوية"); frasaZh("kartu tanda penduduk", "身份证");
		frasa("tanda tangan", "signature", "توقيع"); frasaZh("tanda tangan", "签名");
		frasa("nomor telepon", "phone number", "رقم الهاتف"); frasaZh("nomor telepon", "电话号码");
		frasa("jenis kelamin", "gender", "الجنس"); frasaZh("jenis kelamin", "性别");
		frasa("tempat lahir", "place of birth", "مكان الميلاد"); frasaZh("tempat lahir", "出生地");
		frasa("tanggal lahir", "date of birth", "تاريخ الميلاد"); frasaZh("tanggal lahir", "出生日期");
	}

	// ---- BACKFILL MANDARIN untuk kata en/ar yg belum punya zh + kata baru + frasa (kurangi campur ZH) ----
	static {
		zh("dipakai", "使用");
		zh("apakah", "是否");
		zh("ke", "至");
		zh("anda", "您");
		zh("biodata", "个人资料");
		zh("ada", "存在");
		zh("pendataan", "数据采集");
		zh("pengajuan", "提交");
		zh("pendidikan", "教育");
		zh("revisi", "修订");
		zh("rekap", "汇总");
		zh("lampiran", "附件");
		zh("riwayat", "历史");
		zh("penilaian", "评估");
		zh("aktifkan", "启用");
		zh("ulang", "重复");
		zh("pengadaan", "采购");
		zh("rincian", "明细");
		zh("boleh", "可以");
		zh("lulusan", "毕业生");
		zh("satu", "一");
		zh("posting", "过账");
		zh("oleh", "由");
		zh("peserta", "参与者");
		zh("wali", "监护人");
		zh("manajemen", "管理");
		zh("memuat", "加载");
		zh("persetujuan", "批准");
		zh("krs", "学习计划");
		zh("lebih", "更多");
		zh("lain", "其他");
		zh("layanan", "服务");
		zh("produk", "产品");
		zh("format", "格式");
		zh("pilihan", "选择");
		zh("jasa", "服务");
		zh("prodi", "专业");
		zh("soal", "题目");
		zh("pencarian", "搜索");
		zh("teks", "文本");
		zh("ditemukan", "找到");
		zh("terjadi", "发生");
		zh("penerimaan", "收据");
		zh("telah", "已");
		zh("studi", "学习");
		zh("profil", "资料");
		zh("judul", "标题");
		zh("ambil", "获取");
		zh("orang", "人");
		zh("pembelajaran", "学习");
		zh("pekerjaan", "职业");
		zh("vendor", "供应商");
		zh("tinggi", "高");
		zh("tua", "旧");
		zh("depan", "前");
		zh("disposisi", "处置");
		zh("opsi", "选项");
		zh("akhir", "最终");
		zh("masukkan", "输入");
		zh("awal", "初始");
		zh("hasil", "结果");
		zh("tersimpan", "已保存");
		zh("bukti", "凭证");
		zh("asal", "来源");
		zh("batas", "限制");
		zh("capaian", "成果");
		zh("email", "电子邮件");
		zh("berdasarkan", "根据");
		zh("menggunakan", "使用");
		zh("jabatan", "职位");
		zh("ingin", "想要");
		zh("koneksi", "连接");
		zh("khusus", "特殊");
		zh("rekapitulasi", "汇总");
		zh("angket", "问卷");
		zh("ayah", "父亲");
		zh("administrasi", "行政");
		zh("parameter", "参数");
		zh("berkas", "文件");
		zh("sunting", "编辑");
		zh("akreditasi", "认证");
		zh("formulir", "表单");
		zh("sebelumnya", "之前");
		zh("evaluasi", "评价");
		zh("ibu", "母亲");
		zh("statistik", "统计");
		zh("ganti", "更换");
		zh("kuliah", "课程");
		zh("langsung", "直接");
		zh("bahan", "材料");
		zh("tersedia", "可用");
		zh("seluruh", "全部");
		zh("gunakan", "使用");
		zh("batalkan", "取消");
		zh("unit", "单位");
		zh("rencana", "计划");
		zh("deskripsi", "描述");
		zh("pembimbing", "导师");
		zh("identitas", "身份");
		zh("jawaban", "答案");
		zh("referensi", "参考");
		zh("agenda", "议程");
		zh("masa", "期间");
		zh("lahir", "出生");
		zh("sebaran", "分布");
		zh("disimpan", "已保存");
		zh("tanda", "标记");
		zh("kosongkan", "清空");
		zh("yakin", "确定");
		zh("lengkap", "完整");
		zh("melakukan", "执行");
		zh("uang", "资金");
		zh("penelitian", "研究");
		zh("ringkasan", "摘要");
		zh("besar", "大");
		zh("mengambil", "获取");
		zh("kinerja", "绩效");
		zh("skripsi", "论文");
		zh("terdapat", "存在");
		zh("dipilih", "已选");
		zh("pusat", "中心");
		zh("seleksi", "选拔");
		zh("angkatan", "届");
		zh("peminjaman", "借阅");
		zh("hak", "权限");
		zh("foto", "照片");
		zh("menghapus", "删除");
		zh("pesanan", "订单");
		zh("kunci", "键");
		zh("maksimal", "最大");
		zh("kecil", "小");
		zh("dibayar", "已付");
		zh("karya", "作品");
		zh("verifikasi", "验证");
		zh("sampai", "至");
		zh("dasar", "基础");
		zh("melihat", "查看");
		zh("karir", "职业");
		zh("pemesanan", "订购");
		zh("syarat", "条件");
		zh("minimal", "最小");
		zh("buat", "创建");
		zh("gelombang", "批次");
		zh("info", "信息");
		zh("target", "目标");
		zh("metode", "方法");
		zh("jaringan", "网络");
		zh("cara", "方法");
		zh("diterima", "已接收");
		zh("bahasa", "语言");
		zh("sama", "相同");
		zh("paket", "套餐");
		zh("sidang", "答辩");
		zh("tetap", "固定");
		zh("terhubung", "已连接");
		zh("ikut", "参加");
		zh("mengajar", "授课");
		zh("model", "模型");
		zh("ijazah", "文凭");
		zh("terpadu", "集成");
		zh("menyimpan", "保存");
		zh("kajian", "研究");
		zh("kelulusan", "毕业");
		zh("tempat", "地点");
		zh("bawah", "下");
		zh("profesi", "职业");
		zh("nominal", "金额");
		zh("pengembalian", "归还");
		zh("operasional", "运营");
		zh("klasifikasi", "分类");
		zh("peladen", "服务器");
		zh("diisi", "填写");
		zh("transkrip", "成绩单");
		zh("periksa", "检查");
		zh("kriteria", "标准");
		zh("dll", "等");
		zh("mandiri", "独立");
		zh("permintaan", "请求");
		zh("induk", "主");
		zh("memproses", "处理");
		zh("pimpinan", "领导");
		zh("analisis", "分析");
		zh("penyedia", "供应商");
		zh("kunjungan", "访问");
		zh("notifikasi", "通知");
		zh("tahap", "阶段");
		zh("menjadi", "成为");
		zh("mengikuti", "遵循");
		zh("ilmiah", "学术");
		zh("angsuran", "分期");
		zh("sisa", "剩余");
		zh("belanja", "支出");
		zh("atas", "上");
		zh("nasional", "国家");
		zh("pembina", "指导");
		zh("fitur", "功能");
		zh("beban", "负载");
		zh("persyaratan", "要求");
		zh("daring", "在线");
		zh("pendaftar", "申请人");
		zh("memiliki", "拥有");
		zh("cepat", "快");
		zh("dana", "资金");
		zh("grafik", "图表");
		zh("tingkat", "级别");
		zh("koma", "逗号");
		zh("bimbingan", "指导");
		zh("hingga", "直到");
		zh("dianggap", "视为");
		zh("penjadwalan", "排程");
		zh("penggajian", "薪资");
		zh("lainnya", "其他");
		zh("saya", "我的");
		zh("sesi", "会话");
		zh("terakhir", "最后");
		zh("digunakan", "使用");
		zh("pelanggan", "客户");
		zh("bukan", "非");
		zh("dihapus", "已删除");
		zh("koleksi", "馆藏");
		zh("komponen", "组件");
		zh("persen", "百分比");
		zh("dibatalkan", "已取消");
		zh("validasi", "验证");
		zh("penguji", "考官");
		zh("bagian", "部门");
		zh("sertifikat", "证书");
		zh("valid", "有效");
		zh("disetujui", "已批准");
		zh("reset", "重置");
		zh("setujui", "批准");
		zh("golongan", "级别");
		zh("lanjutkan", "继续");
		zh("ekspor", "导出");
		zh("umur", "年龄");
		zh("pertanyaan", "问题");
		zh("agama", "宗教");
		zh("maaf", "抱歉");
		zh("bersihkan", "清除");
		zh("diproses", "已处理");
		zh("ditolak", "已拒绝");
		zh("menunggu", "等待");
		zh("kredit", "贷方");
		zh("potongan", "扣除");
		zh("tunjangan", "津贴");
		zh("pangkat", "军衔");
		zh("sembunyikan", "隐藏");
		zh("usia", "年龄");
		zh("berhenti", "停止");
		zh("debit", "借方");
		zh("impor", "导入");
		zh("subtotal", "小计");
		zh("percent", "百分比");
		zh("gender", "性别");

		en("minta", "request"); ar("minta", "طلب"); zh("minta", "请求");
		en("multi", "multi"); ar("multi", "متعدد"); zh("multi", "多");
		en("fungsional", "functional"); ar("fungsional", "وظيفي"); zh("fungsional", "职能");
		en("relasi", "relation"); ar("relasi", "علاقة"); zh("relasi", "关系");
		en("pakai", "use"); ar("pakai", "استخدم"); zh("pakai", "使用");
		en("struk", "receipt"); ar("struk", "إيصال"); zh("struk", "小票");
		en("pemberitahuan", "notification"); ar("pemberitahuan", "إشعار"); zh("pemberitahuan", "通知");
		en("strategis", "strategic"); ar("strategis", "استراتيجي"); zh("strategis", "战略");
		en("kuantitas", "quantity"); ar("kuantitas", "كمية"); zh("kuantitas", "数量");
		en("pratinjau", "preview"); ar("pratinjau", "معاينة"); zh("pratinjau", "预览");
		en("submenu", "submenu"); ar("submenu", "قائمة فرعية"); zh("submenu", "子菜单");
		en("kedisiplinan", "discipline"); ar("kedisiplinan", "انضباط"); zh("kedisiplinan", "纪律");
		en("perangkat", "device"); ar("perangkat", "جهاز"); zh("perangkat", "设备");
		en("singkat", "brief"); ar("singkat", "موجز"); zh("singkat", "简短");
		en("kesehatan", "health"); ar("kesehatan", "صحة"); zh("kesehatan", "健康");
		en("sosial", "social"); ar("sosial", "اجتماعي"); zh("sosial", "社会");
		en("pertanggungjawaban", "accountability"); ar("pertanggungjawaban", "مساءلة"); zh("pertanggungjawaban", "问责");
		en("opname", "stocktaking"); ar("opname", "جرد"); zh("opname", "盘点");
		en("risiko", "risk"); ar("risiko", "مخاطرة"); zh("risiko", "风险");
		en("ilmu", "science"); ar("ilmu", "علم"); zh("ilmu", "科学");
		en("kelamin", "gender"); ar("kelamin", "جنس"); zh("kelamin", "性别");
		en("eksternal", "external"); ar("eksternal", "خارجي"); zh("eksternal", "外部");
		en("membantu", "help"); ar("membantu", "يساعد"); zh("membantu", "帮助");
		en("pertama", "first"); ar("pertama", "أول"); zh("pertama", "第一");
		en("presentasi", "presentation"); ar("presentasi", "عرض تقديمي"); zh("presentasi", "演示");
		en("tes", "test"); ar("tes", "اختبار"); zh("tes", "测试");
		en("komputer", "computer"); ar("komputer", "حاسوب"); zh("komputer", "计算机");
		en("kebijakan", "policy"); ar("kebijakan", "سياسة"); zh("kebijakan", "政策");
		en("terapkan", "apply"); ar("terapkan", "طبق"); zh("terapkan", "应用");
		en("gerbang", "gate"); ar("gerbang", "بوابة"); zh("gerbang", "网关");
		en("koreksi", "correction"); ar("koreksi", "تصحيح"); zh("koreksi", "更正");
		en("komunikasi", "communication"); ar("komunikasi", "اتصال"); zh("komunikasi", "沟通");
		en("pihak", "party"); ar("pihak", "طرف"); zh("pihak", "方");
		en("rumah", "house"); ar("rumah", "منزل"); zh("rumah", "房屋");
		en("dinyatakan", "declared"); ar("dinyatakan", "معلن"); zh("dinyatakan", "被宣布");
		en("murid", "pupil"); ar("murid", "تلميذ"); zh("murid", "学生");
		en("beserta", "along with"); ar("beserta", "مع"); zh("beserta", "连同");
		en("menyediakan", "provide"); ar("menyediakan", "يوفر"); zh("menyediakan", "提供");
		en("baku", "standard"); ar("baku", "معياري"); zh("baku", "标准");
		en("bebas", "free"); ar("bebas", "حر"); zh("bebas", "自由");
		en("penawaran", "offer"); ar("penawaran", "عرض"); zh("penawaran", "报价");
		en("level", "level"); ar("level", "مستوى"); zh("level", "级别");
		en("salin", "copy"); ar("salin", "انسخ"); zh("salin", "复制");
		en("terbaik", "best"); ar("terbaik", "الأفضل"); zh("terbaik", "最佳");
		en("langkah", "step"); ar("langkah", "خطوة"); zh("langkah", "步骤");
		en("kios", "kiosk"); ar("kios", "كشك"); zh("kios", "自助机");
		en("asrama", "dormitory"); ar("asrama", "سكن"); zh("asrama", "宿舍");
		en("rekomendasi", "recommendation"); ar("rekomendasi", "توصية"); zh("rekomendasi", "推荐");
		en("jemput", "pickup"); ar("jemput", "استلام"); zh("jemput", "接送");
		en("struktural", "structural"); ar("struktural", "هيكلي"); zh("struktural", "结构");
		en("tim", "team"); ar("tim", "فريق"); zh("tim", "团队");
		en("rekanan", "partner"); ar("rekanan", "شريك"); zh("rekanan", "合作伙伴");
		en("kontrol", "control"); ar("kontrol", "تحكم"); zh("kontrol", "控制");
		en("keputusan", "decision"); ar("keputusan", "قرار"); zh("keputusan", "决定");
		en("kontak", "contact"); ar("kontak", "اتصال"); zh("kontak", "联系");
		en("penyusunan", "preparation"); ar("penyusunan", "إعداد"); zh("penyusunan", "编制");
		en("ayat", "clause"); ar("ayat", "بند"); zh("ayat", "条款");
		en("pengecualian", "exception"); ar("pengecualian", "استثناء"); zh("pengecualian", "例外");
		en("akumulasi", "accumulation"); ar("akumulasi", "تراكم"); zh("akumulasi", "累计");
		en("transportasi", "transportation"); ar("transportasi", "نقل"); zh("transportasi", "运输");
		en("peminjam", "borrower"); ar("peminjam", "مقترض"); zh("peminjam", "借款人");
		en("asesor", "assessor"); ar("asesor", "مقيّم"); zh("asesor", "评估员");
		en("kandung", "biological"); ar("kandung", "بيولوجي"); zh("kandung", "亲生");
		en("loker", "locker"); ar("loker", "خزانة"); zh("loker", "储物柜");
		en("afiliasi", "affiliation"); ar("afiliasi", "انتماء"); zh("afiliasi", "隶属");
		en("murni", "pure"); ar("murni", "صافي"); zh("murni", "纯");
		en("berkala", "periodic"); ar("berkala", "دوري"); zh("berkala", "定期");
		en("harian", "daily"); ar("harian", "يومي"); zh("harian", "每日");
		en("bulanan", "monthly"); ar("bulanan", "شهري"); zh("bulanan", "每月");
		en("tahunan", "annual"); ar("tahunan", "سنوي"); zh("tahunan", "年度");
		en("mingguan", "weekly"); ar("mingguan", "أسبوعي"); zh("mingguan", "每周");
		en("denda", "penalty"); ar("denda", "غرامة"); zh("denda", "罚款");
		en("presensi", "attendance"); ar("presensi", "حضور"); zh("presensi", "考勤");
		en("konsumsi", "consumption"); ar("konsumsi", "استهلاك"); zh("konsumsi", "消耗");
		en("pustakawan", "librarian"); ar("pustakawan", "أمين مكتبة"); zh("pustakawan", "图书管理员");

		frasaZh("dosen wali", "学术导师");
		frasaZh("dosen pembimbing", "指导教师");
		frasaZh("dosen pembimbing akademik", "学术指导教师");
		frasaZh("nilai huruf", "字母成绩");
		frasaZh("nilai transfer", "转换成绩");
		frasaZh("calon siswa", "准学生");
		frasaZh("calon pegawai", "准员工");
		frasaZh("calon mhs", "准大学生");
		frasaZh("surat menyurat", "信函往来");
		frasaZh("tampilkan semua", "显示全部");
		frasaZh("tambah data", "添加数据");
		frasaZh("pegawai pensiun", "退休员工");
		frasaZh("tidak boleh kosong", "不能为空");
		frasaZh("tidak tersedia", "不可用");
		frasaZh("tidak dapat", "无法");
		frasaZh("kelas siswa", "学生班级");
		frasaZh("data diri", "个人资料");
		frasaZh("data alumni", "校友数据");
		frasaZh("data guru", "教师数据");
		frasaZh("data diri siswa", "学生个人资料");
		frasaZh("laporan keuangan", "财务报告");
		frasaZh("laporan absensi", "考勤报告");
		frasaZh("laporan kehadiran", "出勤报告");
		frasaZh("laporan kinerja", "绩效报告");
		frasaZh("nama bank", "银行名称");
		frasaZh("nama file", "文件名");
		frasaZh("kartu hasil studi", "学习成绩单");
		frasaZh("transkrip akademik", "学术成绩单");
		frasaZh("arus kas", "现金流");
	}

	// ---- BATCH-4: akar kata nyata sisa dari corpus (buang akronim/nama-kelas/teknis) ----
	static {
		en("seminar", "seminar"); ar("seminar", "ندوة"); zh("seminar", "研讨会");
		en("ekivalen", "equivalent"); ar("ekivalen", "مكافئ"); zh("ekivalen", "等效");
		en("jalan", "road"); ar("jalan", "طريق"); zh("jalan", "道路");
		en("universitas", "university"); ar("universitas", "جامعة"); zh("universitas", "大学");
		en("alasan", "reason"); ar("alasan", "سبب"); zh("alasan", "原因");
		en("antara", "between"); ar("antara", "بين"); zh("antara", "之间");
		en("mitra", "partner"); ar("mitra", "شريك"); zh("mitra", "合作伙伴");
		en("kendaraan", "vehicle"); ar("kendaraan", "مركبة"); zh("kendaraan", "车辆");
		en("rekonsiliasi", "reconciliation"); ar("rekonsiliasi", "تسوية"); zh("rekonsiliasi", "对账");
		en("kanal", "channel"); ar("kanal", "قناة"); zh("kanal", "渠道");
		en("milik", "belongs to"); ar("milik", "ملك"); zh("milik", "属于");
		en("autentikasi", "authentication"); ar("autentikasi", "مصادقة"); zh("autentikasi", "认证");
		en("suami", "husband"); ar("suami", "زوج"); zh("suami", "丈夫");
		en("istri", "wife"); ar("istri", "زوجة"); zh("istri", "妻子");
		en("persentase", "percentage"); ar("persentase", "نسبة مئوية"); zh("persentase", "百分比");
		en("formula", "formula"); ar("formula", "صيغة"); zh("formula", "公式");
		en("teori", "theory"); ar("teori", "نظرية"); zh("teori", "理论");
		en("bila", "if"); ar("bila", "إذا"); zh("bila", "如果");
		en("posisi", "position"); ar("posisi", "موضع"); zh("posisi", "位置");
		en("rujukan", "reference"); ar("rujukan", "مرجع"); zh("rujukan", "参考");
		en("pembahasan", "discussion"); ar("pembahasan", "مناقشة"); zh("pembahasan", "讨论");
		en("formal", "formal"); ar("formal", "رسمي"); zh("formal", "正式");
		en("magang", "internship"); ar("magang", "تدريب"); zh("magang", "实习");
		en("skema", "scheme"); ar("skema", "مخطط"); zh("skema", "方案");
		en("ketentuan", "provision"); ar("ketentuan", "حكم"); zh("ketentuan", "规定");
		en("instrumen", "instrument"); ar("instrumen", "أداة"); zh("instrumen", "工具");
		en("mikro", "micro"); ar("mikro", "صغير"); zh("mikro", "微");
		en("makro", "macro"); ar("makro", "كبير"); zh("makro", "宏");
		en("sinkron", "synchronous"); ar("sinkron", "متزامن"); zh("sinkron", "同步");
		en("berkomitmen", "committed"); ar("berkomitmen", "ملتزم"); zh("berkomitmen", "承诺");
		en("bentrok", "conflict"); ar("bentrok", "تعارض"); zh("bentrok", "冲突");
		en("retur", "return"); ar("retur", "إرجاع"); zh("retur", "退货");
		en("ringkas", "concise"); ar("ringkas", "موجز"); zh("ringkas", "简洁");
		en("misal", "example"); ar("misal", "مثال"); zh("misal", "例如");
		en("struktur", "structure"); ar("struktur", "هيكل"); zh("struktur", "结构");
		en("departemen", "department"); ar("departemen", "قسم"); zh("departemen", "部门");
		en("lahan", "land"); ar("lahan", "أرض"); zh("lahan", "土地");
		en("butir", "point"); ar("butir", "بند"); zh("butir", "项");
		en("mempunyai", "have"); ar("mempunyai", "يملك"); zh("mempunyai", "拥有");
		en("kewarganegaraan", "citizenship"); ar("kewarganegaraan", "جنسية"); zh("kewarganegaraan", "国籍");
		en("pernah", "ever"); ar("pernah", "سبق"); zh("pernah", "曾经");
		en("spesialisasi", "specialization"); ar("spesialisasi", "تخصص"); zh("spesialisasi", "专业");
		en("ukuran", "size"); ar("ukuran", "حجم"); zh("ukuran", "尺寸");
		en("efisien", "efficient"); ar("efisien", "فعال"); zh("efisien", "高效");
		en("lambat", "slow"); ar("lambat", "بطيء"); zh("lambat", "慢");
		en("dukungan", "support"); ar("dukungan", "دعم"); zh("dukungan", "支持");
		en("gabungan", "combined"); ar("gabungan", "مدمج"); zh("gabungan", "合并");
		en("insentif", "incentive"); ar("insentif", "حافز"); zh("insentif", "奖励");
		en("interaktif", "interactive"); ar("interaktif", "تفاعلي"); zh("interaktif", "互动");
		en("simulasi", "simulation"); ar("simulasi", "محاكاة"); zh("simulasi", "模拟");
		en("progres", "progress"); ar("progres", "تقدم"); zh("progres", "进度");
		en("keunggulan", "excellence"); ar("keunggulan", "تميز"); zh("keunggulan", "优势");
		en("rapi", "neat"); ar("rapi", "مرتب"); zh("rapi", "整齐");
		en("lingkungan", "environment"); ar("lingkungan", "بيئة"); zh("lingkungan", "环境");
		en("privasi", "privacy"); ar("privasi", "خصوصية"); zh("privasi", "隐私");
		en("cicilan", "installment"); ar("cicilan", "قسط"); zh("cicilan", "分期");
		en("paralel", "parallel"); ar("paralel", "متوازي"); zh("paralel", "并行");
		en("sivitas", "academic community"); ar("sivitas", "المجتمع الأكاديمي"); zh("sivitas", "学术界");
		en("kiri", "left"); ar("kiri", "يسار"); zh("kiri", "左");
		en("kanan", "right"); ar("kanan", "يمين"); zh("kanan", "右");
		en("pangkal", "base"); ar("pangkal", "قاعدة"); zh("pangkal", "基部");
		en("mencakup", "covers"); ar("mencakup", "يشمل"); zh("mencakup", "涵盖");
		en("thesis", "thesis"); ar("thesis", "أطروحة"); zh("thesis", "论文");
		en("aksi", "action"); ar("aksi", "إجراء"); zh("aksi", "操作");
		en("akte", "certificate"); ar("akte", "شهادة"); zh("akte", "证书");
		en("sederhana", "simple"); ar("sederhana", "بسيط"); zh("sederhana", "简单");
		en("ekstrakurikuler", "extracurricular"); ar("ekstrakurikuler", "لا منهجي"); zh("ekstrakurikuler", "课外");
		en("rute", "route"); ar("rute", "مسار"); zh("rute", "路线");
		en("perkiraan", "estimate"); ar("perkiraan", "تقدير"); zh("perkiraan", "估算");
		en("negeri", "state"); ar("negeri", "حكومي"); zh("negeri", "国立");
		en("berbagai", "various"); ar("berbagai", "متنوع"); zh("berbagai", "各种");
		en("pangkalan", "base"); ar("pangkalan", "قاعدة"); zh("pangkalan", "基地");
		en("nyata", "real"); ar("nyata", "حقيقي"); zh("nyata", "真实");
		en("pengawas", "supervisor"); ar("pengawas", "مشرف"); zh("pengawas", "监督员");
		en("ikatan", "bond"); ar("ikatan", "رابطة"); zh("ikatan", "契约");
		en("pengalaman", "experience"); ar("pengalaman", "خبرة"); zh("pengalaman", "经验");
		en("bisnis", "business"); ar("bisnis", "أعمال"); zh("bisnis", "商业");
		en("konsultan", "consultant"); ar("konsultan", "مستشار"); zh("konsultan", "顾问");
		en("mewujudkan", "realize"); ar("mewujudkan", "يحقق"); zh("mewujudkan", "实现");
		en("menengah", "intermediate"); ar("menengah", "متوسط"); zh("menengah", "中等");
		en("kuitansi", "receipt"); ar("kuitansi", "إيصال"); zh("kuitansi", "收据");
		en("portofolio", "portfolio"); ar("portofolio", "حافظة"); zh("portofolio", "作品集");
		en("profesional", "professional"); ar("profesional", "محترف"); zh("profesional", "专业");
		en("tepat", "exact"); ar("tepat", "دقيق"); zh("tepat", "准确");
		en("pendampingan", "mentoring"); ar("pendampingan", "مرافقة"); zh("pendampingan", "陪同辅导");
		en("elektronik", "electronic"); ar("elektronik", "إلكتروني"); zh("elektronik", "电子");
		en("infrastruktur", "infrastructure"); ar("infrastruktur", "بنية تحتية"); zh("infrastruktur", "基础设施");
		en("segarkan", "refresh"); ar("segarkan", "حدث"); zh("segarkan", "刷新");
		en("merender", "render"); ar("merender", "يعرض"); zh("merender", "渲染");
		en("galat", "error"); ar("galat", "خطأ"); zh("galat", "错误");
		en("pembicara", "speaker"); ar("pembicara", "متحدث"); zh("pembicara", "演讲者");
		en("karakter", "character"); ar("karakter", "حرف"); zh("karakter", "字符");
		en("mencukupi", "sufficient"); ar("mencukupi", "كافٍ"); zh("mencukupi", "足够");
		en("skala", "scale"); ar("skala", "مقياس"); zh("skala", "比例");
		en("temukan", "find"); ar("temukan", "ابحث"); zh("temukan", "查找");
		en("abaikan", "ignore"); ar("abaikan", "تجاهل"); zh("abaikan", "忽略");
		en("logistik", "logistics"); ar("logistik", "لوجستيات"); zh("logistik", "物流");
		en("lingkup", "scope"); ar("lingkup", "نطاق"); zh("lingkup", "范围");
		en("area", "area"); ar("area", "منطقة"); zh("area", "区域");
		en("kamera", "camera"); ar("kamera", "كاميرا"); zh("kamera", "摄像头");
		en("gangguan", "disruption"); ar("gangguan", "اضطراب"); zh("gangguan", "故障");
		en("pengawasan", "supervision"); ar("pengawasan", "إشراف"); zh("pengawasan", "监督");
		en("apresiasi", "appreciation"); ar("apresiasi", "تقدير"); zh("apresiasi", "赞赏");
		en("unggul", "superior"); ar("unggul", "متفوق"); zh("unggul", "卓越");
		en("karyawan", "employee"); ar("karyawan", "موظف"); zh("karyawan", "员工");
		en("ringan", "light"); ar("ringan", "خفيف"); zh("ringan", "轻");
		en("presisi", "precision"); ar("presisi", "دقة"); zh("presisi", "精度");
		en("kampung", "village"); ar("kampung", "قرية"); zh("kampung", "村庄");
		en("nol", "zero"); ar("nol", "صفر"); zh("nol", "零");
		en("estimasi", "estimate"); ar("estimasi", "تقدير"); zh("estimasi", "估算");
		en("manfaat", "benefit"); ar("manfaat", "فائدة"); zh("manfaat", "益处");
		en("menjamin", "guarantee"); ar("menjamin", "يضمن"); zh("menjamin", "保证");
		en("merubah", "change"); ar("merubah", "يغير"); zh("merubah", "更改");
		en("mengubah", "change"); ar("mengubah", "يغير"); zh("mengubah", "修改");
		en("dekan", "dean"); ar("dekan", "عميد"); zh("dekan", "院长");
		en("pengaju", "applicant"); ar("pengaju", "مقدم"); zh("pengaju", "申请人");
		en("reguler", "regular"); ar("reguler", "عادي"); zh("reguler", "常规");
		en("penunjang", "supporting"); ar("penunjang", "داعم"); zh("penunjang", "支撑");
		en("pengembang", "developer"); ar("pengembang", "مطور"); zh("pengembang", "开发者");
		en("kapasitas", "capacity"); ar("kapasitas", "سعة"); zh("kapasitas", "容量");
		en("jejak", "trace"); ar("jejak", "أثر"); zh("jejak", "轨迹");
		en("direktori", "directory"); ar("direktori", "دليل"); zh("direktori", "目录");
		en("entitas", "entity"); ar("entitas", "كيان"); zh("entitas", "实体");
		en("entri", "entry"); ar("entri", "إدخال"); zh("entri", "条目");
		en("stempel", "stamp"); ar("stempel", "ختم"); zh("stempel", "印章");
		en("eksekusi", "execution"); ar("eksekusi", "تنفيذ"); zh("eksekusi", "执行");
		en("terenkripsi", "encrypted"); ar("terenkripsi", "مشفر"); zh("terenkripsi", "加密");
		en("perihal", "regarding"); ar("perihal", "بشأن"); zh("perihal", "关于");
		en("petunjuk", "instruction"); ar("petunjuk", "تعليمات"); zh("petunjuk", "指示");
		en("generasi", "generation"); ar("generasi", "جيل"); zh("generasi", "代");
		en("berita", "news"); ar("berita", "خبر"); zh("berita", "新闻");
		en("peran", "role"); ar("peran", "دور"); zh("peran", "角色");
		en("finansial", "financial"); ar("finansial", "مالي"); zh("finansial", "财务");
		en("domisili", "domicile"); ar("domisili", "محل الإقامة"); zh("domisili", "住所");
		en("penyelenggaraan", "implementation"); ar("penyelenggaraan", "تنظيم"); zh("penyelenggaraan", "举办");
		en("alokasi", "allocation"); ar("alokasi", "تخصيص"); zh("alokasi", "分配");
		en("automasi", "automation"); ar("automasi", "أتمتة"); zh("automasi", "自动化");
		en("otomasi", "automation"); ar("otomasi", "أتمتة"); zh("otomasi", "自动化");
		en("eksemplar", "copy"); ar("eksemplar", "نسخة"); zh("eksemplar", "本");
		en("kenaikan", "increase"); ar("kenaikan", "زيادة"); zh("kenaikan", "增长");
		en("makan", "meal"); ar("makan", "وجبة"); zh("makan", "餐");
		en("membutuhkan", "need"); ar("membutuhkan", "يحتاج"); zh("membutuhkan", "需要");
		en("lakukan", "do"); ar("lakukan", "افعل"); zh("lakukan", "执行");
		en("layak", "eligible"); ar("layak", "مؤهل"); zh("layak", "合格");
		en("darurat", "emergency"); ar("darurat", "طارئ"); zh("darurat", "紧急");
		en("seri", "series"); ar("seri", "سلسلة"); zh("seri", "系列");
		en("pintar", "smart"); ar("pintar", "ذكي"); zh("pintar", "智能");
		en("arahkan", "direct"); ar("arahkan", "وجه"); zh("arahkan", "定向");
		en("kotak", "box"); ar("kotak", "صندوق"); zh("kotak", "框");
		en("teknologi", "technology"); ar("teknologi", "تقنية"); zh("teknologi", "技术");
		en("asisten", "assistant"); ar("asisten", "مساعد"); zh("asisten", "助理");
		en("tema", "theme"); ar("tema", "سمة"); zh("tema", "主题");
		en("pinjaman", "loan"); ar("pinjaman", "قرض"); zh("pinjaman", "贷款");
		en("blok", "block"); ar("blok", "كتلة"); zh("blok", "块");
		en("kustom", "custom"); ar("kustom", "مخصص"); zh("kustom", "自定义");
		en("punya", "have"); ar("punya", "يملك"); zh("punya", "拥有");
		en("populer", "popular"); ar("populer", "شائع"); zh("populer", "热门");
		en("pelajar", "student"); ar("pelajar", "تلميذ"); zh("pelajar", "学生");
		en("frekuensi", "frequency"); ar("frekuensi", "تردد"); zh("frekuensi", "频率");
		en("jatah", "quota"); ar("jatah", "حصة"); zh("jatah", "配额");
		en("pengesahan", "ratification"); ar("pengesahan", "تصديق"); zh("pengesahan", "批准");
		en("integritas", "integrity"); ar("integritas", "نزاهة"); zh("integritas", "诚信");
		en("pernyataan", "statement"); ar("pernyataan", "بيان"); zh("pernyataan", "声明");
		en("mengurangi", "reduce"); ar("mengurangi", "يقلل"); zh("mengurangi", "减少");
		en("pelamar", "applicant"); ar("pelamar", "متقدم"); zh("pelamar", "申请人");
		en("cipta", "create"); ar("cipta", "إنشاء"); zh("cipta", "创作");
		en("titik", "point"); ar("titik", "نقطة"); zh("titik", "点");
		en("klausul", "clause"); ar("klausul", "بند"); zh("klausul", "条款");
		en("wakil", "deputy"); ar("wakil", "نائب"); zh("wakil", "副职");
		en("matkul", "course"); ar("matkul", "مقرر"); zh("matkul", "课程");
		en("sukses", "success"); ar("sukses", "نجاح"); zh("sukses", "成功");
		en("pemeliharaan", "maintenance"); ar("pemeliharaan", "صيانة"); zh("pemeliharaan", "维护");
		en("lisensi", "license"); ar("lisensi", "ترخيص"); zh("lisensi", "许可");
		en("sponsor", "sponsor"); ar("sponsor", "راعٍ"); zh("sponsor", "赞助商");
		en("perawatan", "maintenance"); ar("perawatan", "صيانة"); zh("perawatan", "保养");
		en("sertifikasi", "certification"); ar("sertifikasi", "شهادة"); zh("sertifikasi", "认证");
		en("wilayah", "region"); ar("wilayah", "إقليم"); zh("wilayah", "地区");
		en("kabag", "division head"); ar("kabag", "رئيس القسم"); zh("kabag", "科长");
		en("kasubag", "sub-division head"); ar("kasubag", "رئيس القسم الفرعي"); zh("kasubag", "副科长");
		en("koordinator", "coordinator"); ar("koordinator", "منسق"); zh("koordinator", "协调员");
		en("penanggung", "responsible"); ar("penanggung", "مسؤول"); zh("penanggung", "负责");
		en("jawab", "answer"); ar("jawab", "إجابة"); zh("jawab", "回答");
		en("berjenjang", "tiered"); ar("berjenjang", "متدرج"); zh("berjenjang", "分级");
		en("terpusat", "centralized"); ar("terpusat", "مركزي"); zh("terpusat", "集中");
		en("tersebar", "distributed"); ar("tersebar", "موزع"); zh("tersebar", "分散");
		en("kelola", "manage"); ar("kelola", "إدارة"); zh("kelola", "管理");
		en("pengelola", "manager"); ar("pengelola", "مدير"); zh("pengelola", "管理者");
		en("pemantauan", "monitoring"); ar("pemantauan", "مراقبة"); zh("pemantauan", "监测");
		en("penelusuran", "tracing"); ar("penelusuran", "تتبع"); zh("penelusuran", "追溯");
		en("penetapan", "determination"); ar("penetapan", "تحديد"); zh("penetapan", "确定");
		en("penyaluran", "distribution"); ar("penyaluran", "توزيع"); zh("penyaluran", "发放");
		en("pencatatan", "recording"); ar("pencatatan", "تسجيل"); zh("pencatatan", "记录");
		en("pengukuran", "measurement"); ar("pengukuran", "قياس"); zh("pengukuran", "测量");
		en("penyimpanan", "storage"); ar("penyimpanan", "تخزين"); zh("penyimpanan", "存储");
		en("pengiriman", "delivery"); ar("pengiriman", "إرسال"); zh("pengiriman", "发送");
		en("penerima", "recipient"); ar("penerima", "مستلم"); zh("penerima", "接收人");
		en("pengirim", "sender"); ar("pengirim", "مرسل"); zh("pengirim", "发件人");
		en("penerbitan", "publishing"); ar("penerbitan", "إصدار"); zh("penerbitan", "发行");
	}

	static {

		// ================= Batch dari sweep webapp/WEB-INF/baru/*.jsp (top-level) =================
		// Kata tunggal (freq>=2 di 35 file JSP top-level, belum dikenal kamus sebelumnya).
		en("digital", "digital"); ar("digital", "رقمي");
		en("outlet", "outlet"); ar("outlet", "منفذ");
		en("medis", "medical"); ar("medis", "طبي");
		en("investor", "investor"); ar("investor", "مستثمر");
		en("mengelola", "manage"); ar("mengelola", "إدارة");
		en("obat", "medicine"); ar("obat", "دواء");
		en("pasien", "patient"); ar("pasien", "مريض");
		en("audit", "audit"); ar("audit", "تدقيق");
		en("bertahap", "gradual"); ar("bertahap", "تدريجي");
		en("farmasi", "pharmacy"); ar("farmasi", "صيدلية");
		en("klinik", "clinic"); ar("klinik", "عيادة");
		en("investasi", "investment"); ar("investasi", "استثمار");
		en("pelaporan", "reporting"); ar("pelaporan", "إعداد التقارير");
		en("ekspedisi", "shipping"); ar("ekspedisi", "شحن");
		en("internal", "internal"); ar("internal", "داخلي");
		en("resep", "prescription"); ar("resep", "وصفة طبية");
		en("transformasi", "transformation"); ar("transformasi", "تحول");
		en("real", "real"); ar("real", "حقيقي");
		en("time", "time"); ar("time", "وقت");
		en("memastikan", "ensure"); ar("memastikan", "التأكد");
		en("dokter", "doctor"); ar("dokter", "طبيب");
		en("terpisah", "separate"); ar("terpisah", "منفصل");
		en("ritel", "retail"); ar("ritel", "تجزئة");
		en("antrean", "queue"); ar("antrean", "طابور");
		en("demo", "demo"); ar("demo", "عرض تجريبي");
		en("pengelolaan", "management"); ar("pengelolaan", "إدارة");
		en("dihitung", "calculated"); ar("dihitung", "محسوب");
		en("meningkatkan", "improve"); ar("meningkatkan", "تحسين");
		en("kepada", "to"); ar("kepada", "إلى");
		en("puskesmas", "health center"); ar("puskesmas", "مركز صحي");
		en("fee", "fee"); ar("fee", "رسوم");
		en("petugas", "officer"); ar("petugas", "موظف");
		en("mempercepat", "accelerate"); ar("mempercepat", "تسريع");
		en("migrasi", "migration"); ar("migrasi", "ترحيل");
		en("mesin", "machine"); ar("mesin", "آلة");
		en("persuratan", "correspondence"); ar("persuratan", "المراسلات");
		en("pondok", "boarding house"); ar("pondok", "مسكن");
		en("disiapkan", "prepared"); ar("disiapkan", "معد");
		en("roadmap", "roadmap"); ar("roadmap", "خارطة طريق");
		en("hardware", "hardware"); ar("hardware", "أجهزة");
		en("fase", "phase"); ar("fase", "مرحلة");
		en("tindakan", "action"); ar("tindakan", "إجراء");
		en("memantau", "monitor"); ar("memantau", "مراقبة");
		en("perubahan", "change"); ar("perubahan", "تغيير");
		en("pendekatan", "approach"); ar("pendekatan", "نهج");
		en("adalah", "is"); ar("adalah", "هو");
		en("fondasi", "foundation"); ar("fondasi", "أساس");
		en("akurat", "accurate"); ar("akurat", "دقيق");
		en("dipantau", "monitored"); ar("dipantau", "تتم مراقبته");
		en("terstruktur", "structured"); ar("terstruktur", "منظم");
		en("apa", "what"); ar("apa", "ماذا");
		en("berdiri", "stand"); ar("berdiri", "يقف");
		en("putus", "dropout"); ar("putus", "انقطاع");
		en("tetapi", "but"); ar("tetapi", "لكن");
		en("terukur", "measurable"); ar("terukur", "قابل للقياس");
		en("mencatat", "record"); ar("mencatat", "تسجيل");
		en("disusun", "arranged"); ar("disusun", "مرتب");
		en("selalu", "always"); ar("selalu", "دائما");
		en("berkelanjutan", "sustainable"); ar("berkelanjutan", "مستدام");
		en("kesiapan", "readiness"); ar("kesiapan", "الجاهزية");
		en("tercatat", "recorded"); ar("tercatat", "مسجل");
		en("first", "first"); ar("first", "أولا");
		en("racikan", "compounding"); ar("racikan", "تركيب الدواء");
		en("riset", "research"); ar("riset", "بحث");
		en("responsif", "responsive"); ar("responsif", "سريع الاستجابة");
		en("serah", "handover"); ar("serah", "تسليم");
		en("termasuk", "including"); ar("termasuk", "بما في ذلك");
		en("sepenuhnya", "fully"); ar("sepenuhnya", "بالكامل");
		en("pemeriksaan", "examination"); ar("pemeriksaan", "فحص");
		en("birokrasi", "bureaucracy"); ar("birokrasi", "بيروقراطية");
		en("perhitungan", "calculation"); ar("perhitungan", "حساب");
		en("eksekutif", "executive"); ar("eksekutif", "تنفيذي");
		en("konsisten", "consistent"); ar("konsisten", "متسق");
		en("dilengkapi", "equipped"); ar("dilengkapi", "مجهز");
		en("transparansi", "transparency"); ar("transparansi", "الشفافية");
		en("penjemput", "picker"); ar("penjemput", "المستلم");
		en("gambaran", "overview"); ar("gambaran", "نظرة عامة");
		en("memberi", "give"); ar("memberi", "إعطاء");
		en("penagihan", "billing"); ar("penagihan", "الفوترة");
		en("merekam", "record"); ar("merekam", "تسجيل");
		en("procurement", "procurement"); ar("procurement", "المشتريات");
		en("sekali", "once"); ar("sekali", "مرة واحدة");
		en("tahapan", "stage"); ar("tahapan", "مرحلة");
		en("saling", "mutual"); ar("saling", "متبادل");
		en("akurasi", "accuracy"); ar("akurasi", "الدقة");
		en("terdokumentasi", "documented"); ar("terdokumentasi", "موثق");
		en("histori", "history"); ar("histori", "السجل");
		en("trail", "trail"); ar("trail", "أثر");
		en("draf", "draft"); ar("draf", "مسودة");
		en("memudahkan", "facilitate"); ar("memudahkan", "تسهيل");
		en("memahami", "understand"); ar("memahami", "فهم");
		en("bersama", "together"); ar("bersama", "معا");
		en("langganan", "subscription"); ar("langganan", "اشتراك");
		en("selisih", "difference"); ar("selisih", "فرق");
		en("non", "non"); ar("non", "غير");
		en("dikonfigurasi", "configured"); ar("dikonfigurasi", "تم تكوينه");
		en("jelas", "clear"); ar("jelas", "واضح");
		en("peningkatan", "improvement"); ar("peningkatan", "تحسين");
		en("beradaptasi", "adapt"); ar("beradaptasi", "التكيف");
		en("digitalisasi", "digitalization"); ar("digitalisasi", "الرقمنة");
		en("dua", "two"); ar("dua", "اثنان");
		en("fleksibilitas", "flexibility"); ar("fleksibilitas", "المرونة");
		en("dual", "dual"); ar("dual", "مزدوج");
		en("mengevaluasi", "evaluate"); ar("mengevaluasi", "تقييم");
		en("terbuka", "open"); ar("terbuka", "مفتوح");
		en("loop", "loop"); ar("loop", "حلقة");
		en("pembiayaan", "financing"); ar("pembiayaan", "التمويل");
		en("strong", "strong"); ar("strong", "قوي");
		en("kadaluarsa", "expired"); ar("kadaluarsa", "منتهي الصلاحية");
		en("melayani", "serve"); ar("melayani", "خدمة");
		en("memperoleh", "obtain"); ar("memperoleh", "الحصول على");
		en("memetakan", "map"); ar("memetakan", "رسم خريطة");
		en("inap", "stay"); ar("inap", "إقامة");
		en("rawat", "care"); ar("rawat", "رعاية");
		en("membayar", "pay"); ar("membayar", "الدفع");
		en("perkembangan", "development"); ar("perkembangan", "تطور");
		en("mencegah", "prevent"); ar("mencegah", "منع");
		en("mengawal", "oversee"); ar("mengawal", "الإشراف");
		en("self", "self"); ar("self", "ذاتي");
		en("pemindaian", "scanning"); ar("pemindaian", "المسح");
		en("dipasang", "installed"); ar("dipasang", "مثبت");
		en("diimplementasikan", "implemented"); ar("diimplementasikan", "مطبق");
		en("standalone", "standalone"); ar("standalone", "مستقل");
		en("menuju", "towards"); ar("menuju", "نحو");
		en("dicatat", "recorded"); ar("dicatat", "مسجل");
		en("sekadar", "merely"); ar("sekadar", "مجرد");
		en("vokasi", "vocational"); ar("vokasi", "مهني");
		en("kejuruan", "vocational"); ar("kejuruan", "مهني");
		en("tertata", "organized"); ar("tertata", "منظم");
		en("sering", "often"); ar("sering", "كثيرا");
		en("pembuatan", "creation"); ar("pembuatan", "إنشاء");
		en("keahlian", "expertise"); ar("keahlian", "الخبرة");
		en("mengintegrasikan", "integrate"); ar("mengintegrasikan", "دمج");
		en("kertas", "paper"); ar("kertas", "ورق");
		en("sulit", "difficult"); ar("sulit", "صعب");
		en("dibaca", "read"); ar("dibaca", "مقروء");
		en("menjelaskan", "explain"); ar("menjelaskan", "شرح");
		en("menjual", "sell"); ar("menjual", "بيع");
		en("efisiensi", "efficiency"); ar("efisiensi", "الكفاءة");
		en("menyatukan", "unite"); ar("menyatukan", "توحيد");
		en("panjang", "long"); ar("panjang", "طويل");
		en("kecepatan", "speed"); ar("kecepatan", "السرعة");
		en("siklus", "cycle"); ar("siklus", "دورة");
		en("loket", "counter"); ar("loket", "شباك");
		en("full", "full"); ar("full", "كامل");
		en("integrated", "integrated"); ar("integrated", "متكامل");
		en("suite", "suite"); ar("suite", "مجموعة");
		en("hierarki", "hierarchy"); ar("hierarki", "التسلسل الهرمي");
		en("panggilan", "call"); ar("panggilan", "مكالمة");
		en("import", "import"); ar("import", "استيراد");
		en("penyesuaian", "adjustment"); ar("penyesuaian", "تعديل");
		en("adaptif", "adaptive"); ar("adaptif", "تكيفي");
		en("industri", "industry"); ar("industri", "الصناعة");
		en("akademika", "academics"); ar("akademika", "الأكاديميون");
		en("menyiapkan", "prepare"); ar("menyiapkan", "تحضير");
		en("regulasi", "regulation"); ar("regulasi", "اللوائح");
		en("menciptakan", "create"); ar("menciptakan", "إنشاء");
		en("kemudahan", "convenience"); ar("kemudahan", "سهولة");
		en("administratif", "administrative"); ar("administratif", "إداري");
		en("para", "the"); ar("para", "الـ");
		en("mempublikasikan", "publish"); ar("mempublikasikan", "نشر");
		en("sistematis", "systematic"); ar("sistematis", "منهجي");
		en("minimum", "minimum"); ar("minimum", "الحد الأدنى");
		en("bersih", "clean"); ar("bersih", "نظيف");
		en("penjaminan", "assurance"); ar("penjaminan", "ضمان");
		en("satpam", "security guard"); ar("satpam", "حارس أمن");
		en("sopir", "driver"); ar("sopir", "سائق");
		en("kenek", "conductor"); ar("kenek", "مساعد السائق");
		en("santri", "student (Islamic boarding school)"); ar("santri", "طالب المعهد الديني");
		en("cerdas", "smart"); ar("cerdas", "ذكي");
		en("journal", "journal"); ar("journal", "دفتر اليومية");
		en("disesuaikan", "adjusted"); ar("disesuaikan", "معدل");
		en("based", "based"); ar("based", "قائم على");
		en("intuitif", "intuitive"); ar("intuitif", "بديهي");
		en("begitu", "so"); ar("begitu", "هكذا");
		en("klinis", "clinical"); ar("klinis", "سريري");
		en("sekitar", "around"); ar("sekitar", "حول");
		en("berbeda", "different"); ar("berbeda", "مختلف");
		en("menangani", "handle"); ar("menangani", "معالجة");
		en("bersamaan", "simultaneous"); ar("bersamaan", "متزامن");
		en("diakses", "accessed"); ar("diakses", "يتم الوصول إليه");
		en("bersifat", "having the nature of"); ar("bersifat", "ذو طابع");
		en("citra", "image"); ar("citra", "صورة");
		en("keras", "hard"); ar("keras", "صلب");
		en("diagnosa", "diagnosis"); ar("diagnosa", "تشخيص");
		en("diukur", "measured"); ar("diukur", "يقاس");
		en("mutakhir", "latest"); ar("mutakhir", "الأحدث");
		en("metodologi", "methodology"); ar("metodologi", "منهجية");
		en("peneliti", "researcher"); ar("peneliti", "باحث");
		en("kini", "now"); ar("kini", "الآن");
		en("jangka", "term"); ar("jangka", "مدى");
		en("didukung", "supported"); ar("didukung", "مدعوم");
		en("menyusun", "compile"); ar("menyusun", "تجميع");
		en("memungkinkan", "enable"); ar("memungkinkan", "تمكين");
		en("double", "double"); ar("double", "مزدوج");
		en("entry", "entry"); ar("entry", "إدخال");
		en("fleksibel", "flexible"); ar("fleksibel", "مرن");
		en("live", "live"); ar("live", "مباشر");
		en("via", "via"); ar("via", "عبر");
		en("ketat", "strict"); ar("ketat", "صارم");
		en("susun", "arrange"); ar("susun", "ترتيب");
		en("tertelusur", "traceable"); ar("tertelusur", "قابل للتتبع");
		en("dini", "early"); ar("dini", "مبكر");
		en("keandalan", "reliability"); ar("keandalan", "الموثوقية");
		en("lintas", "cross"); ar("lintas", "عبر");
		en("kemitraan", "partnership"); ar("kemitraan", "الشراكة");
		en("memvalidasi", "validate"); ar("memvalidasi", "التحقق من صحة");
		en("memperluas", "expand"); ar("memperluas", "توسيع");
		en("maksimum", "maximum"); ar("maksimum", "الحد الأقصى");
		en("ditelusuri", "traced"); ar("ditelusuri", "يتم تتبعه");
		en("tayang", "display"); ar("tayang", "عرض");
		en("slide", "slide"); ar("slide", "شريحة");
		en("intelektual", "intellectual"); ar("intelektual", "فكري");
		en("instan", "instant"); ar("instan", "فوري");
		en("pengecekan", "checking"); ar("pengecekan", "الفحص");
		en("mempermudah", "simplify"); ar("mempermudah", "تسهيل");
		en("pengunjung", "visitor"); ar("pengunjung", "زائر");
		en("mendistribusikan", "distribute"); ar("mendistribusikan", "توزيع");
		en("memverifikasi", "verify"); ar("memverifikasi", "التحقق");
		en("pula", "also"); ar("pula", "أيضا");
		en("hibah", "grant"); ar("hibah", "منحة");
		en("bertingkat", "tiered"); ar("bertingkat", "متدرج");
		en("manajerial", "managerial"); ar("manajerial", "إداري");
		en("berulang", "recurring"); ar("berulang", "متكرر");
		en("seketika", "instantly"); ar("seketika", "فورا");
		en("pelacakan", "tracking"); ar("pelacakan", "التتبع");
		en("software", "software"); ar("software", "برمجيات");
		en("menyelesaikan", "complete"); ar("menyelesaikan", "إكمال");
		en("kapan", "when"); ar("kapan", "متى");
		en("kanak", "kindergarten"); ar("kanak", "رياض الأطفال");
		en("rapat", "meeting"); ar("rapat", "اجتماع");
		en("menggerakkan", "drive"); ar("menggerakkan", "تحريك");
		en("tantangan", "challenge"); ar("tantangan", "التحدي");
		en("smart", "smart"); ar("smart", "ذكي");
		en("pemilihan", "selection"); ar("pemilihan", "اختيار");
		en("tidur", "bed"); ar("tidur", "سرير");
		en("keberhasilan", "success"); ar("keberhasilan", "النجاح");
		en("didesain", "designed"); ar("didesain", "مصمم");
		en("akuntabilitas", "accountability"); ar("akuntabilitas", "المساءلة");
		en("all", "all"); ar("all", "الكل");
		en("one", "one"); ar("one", "واحد");
		en("sentuh", "touch"); ar("sentuh", "لمس");
		en("tersinkronisasi", "synchronized"); ar("tersinkronisasi", "متزامن");
		en("pengisian", "filling"); ar("pengisian", "التعبئة");
		en("assessment", "assessment"); ar("assessment", "التقييم");
		en("bagaimana", "how"); ar("bagaimana", "كيف");
		en("performa", "performance"); ar("performa", "الأداء");
		en("cocok", "suitable"); ar("cocok", "مناسب");
		en("membaca", "read"); ar("membaca", "قراءة");
		en("objektif", "objective"); ar("objektif", "موضوعي");
		en("kendali", "control"); ar("kendali", "التحكم");
		en("kuat", "strong"); ar("kuat", "قوي");
		en("senantiasa", "always"); ar("senantiasa", "دائما");
		en("approval", "approval"); ar("approval", "الموافقة");
		en("enkripsi", "encryption"); ar("enkripsi", "التشفير");
		en("menyeluruh", "comprehensive"); ar("menyeluruh", "شامل");
		en("perawat", "nurse"); ar("perawat", "ممرض");
		en("farmasis", "pharmacist"); ar("farmasis", "صيدلي");
		en("berlangganan", "subscribe"); ar("berlangganan", "الاشتراك");
		en("tiga", "three"); ar("tiga", "ثلاثة");
		en("berisi", "containing"); ar("berisi", "يحتوي على");
		en("latar", "background"); ar("latar", "خلفية");
		en("kesepakatan", "agreement"); ar("kesepakatan", "اتفاق");
		en("cloud", "cloud"); ar("cloud", "سحابة");
		en("mengawasi", "supervise"); ar("mengawasi", "الإشراف");
		en("pemenuhan", "fulfillment"); ar("pemenuhan", "الوفاء");
		en("pendanaan", "funding"); ar("pendanaan", "التمويل");
		en("melacak", "track"); ar("melacak", "تتبع");
		en("dunia", "world"); ar("dunia", "العالم");
		en("logbook", "logbook"); ar("logbook", "سجل اليوميات");
		en("mencetak", "print"); ar("mencetak", "طباعة");
		en("tap", "tap"); ar("tap", "نقر");
		en("pengelompokan", "grouping"); ar("pengelompokan", "التجميع");
		en("pintu", "door"); ar("pintu", "باب");
		en("click", "click"); ar("click", "نقرة");
		en("sync", "sync"); ar("sync", "مزامنة");
		en("terotomatisasi", "automated"); ar("terotomatisasi", "مؤتمت");
		en("mengganggu", "disrupt"); ar("mengganggu", "تعطيل");
		en("memaksimalkan", "maximize"); ar("memaksimalkan", "تعظيم");
		en("rollup", "rollup"); ar("rollup", "تجميع");
		en("dimonitor", "monitored"); ar("dimonitor", "تتم مراقبته");
		en("keselamatan", "safety"); ar("keselamatan", "السلامة");
		en("dikelola", "managed"); ar("dikelola", "تتم إدارته");
		en("dimigrasikan", "migrated"); ar("dimigrasikan", "تم ترحيله");
		en("validitas", "validity"); ar("validitas", "الصلاحية");
		en("peminatan", "specialization"); ar("peminatan", "التخصص");
		en("pengawalan", "escort"); ar("pengawalan", "المرافقة");
		en("anti", "anti"); ar("anti", "مضاد");
		en("jadwalkan", "schedule"); ar("jadwalkan", "جدولة");
		en("jelajahi", "explore"); ar("jelajahi", "استكشاف");
		en("berikut", "following"); ar("berikut", "التالي");
		en("bahwa", "that"); ar("bahwa", "أن");
		en("point", "point"); ar("point", "نقطة");
		en("sale", "sale"); ar("sale", "بيع");
		en("zonasi", "zoning"); ar("zonasi", "التقسيم إلى مناطق");
		en("tervalidasi", "validated"); ar("tervalidasi", "تم التحقق منه");
		en("mendokumentasikan", "document"); ar("mendokumentasikan", "توثيق");
		en("kelengkapan", "completeness"); ar("kelengkapan", "الاكتمال");
		en("ketua", "chairman"); ar("ketua", "رئيس");
		en("mendapatkan", "obtain"); ar("mendapatkan", "الحصول على");
		en("terasa", "felt"); ar("terasa", "محسوس");
		en("bergaya", "stylish"); ar("bergaya", "أنيق");
		en("pencetakan", "printing"); ar("pencetakan", "الطباعة");
		en("umpan", "feed"); ar("umpan", "تغذية");
		en("balik", "back"); ar("balik", "رجوع");
		en("sekaligus", "at once"); ar("sekaligus", "في آن واحد");
		en("menampilkan", "display"); ar("menampilkan", "عرض");
		en("rekrutmen", "recruitment"); ar("rekrutmen", "التوظيف");
		en("mereka", "they"); ar("mereka", "هم");
		en("tutor", "tutor"); ar("tutor", "مدرس خصوصي");
		en("memonitor", "monitor"); ar("memonitor", "مراقبة");
		en("mengkalkulasi", "calculate"); ar("mengkalkulasi", "حساب");
		en("meminimalisasi", "minimize"); ar("meminimalisasi", "تقليل");
		en("menghasilkan", "produce"); ar("menghasilkan", "إنتاج");
		en("serapan", "absorption"); ar("serapan", "الاستيعاب");
		en("walau", "although"); ar("walau", "رغم أن");
		en("menerbitkan", "publish"); ar("menerbitkan", "نشر");
		en("higienis", "hygienic"); ar("higienis", "صحي");
		en("mendampingi", "accompany"); ar("mendampingi", "مرافقة");
		en("gratis", "free"); ar("gratis", "مجاني");
		en("menerima", "receive"); ar("menerima", "استلام");
		en("mengakomodasi", "accommodate"); ar("mengakomodasi", "استيعاب");
		en("improvement", "improvement"); ar("improvement", "التحسين");
		en("menyelenggarakan", "organize"); ar("menyelenggarakan", "تنظيم");
		en("kompleksitas", "complexity"); ar("kompleksitas", "التعقيد");
		en("lapangan", "field"); ar("lapangan", "ميدان");
		en("meliputi", "cover"); ar("meliputi", "يشمل");
		en("intensif", "intensive"); ar("intensif", "مكثف");
		en("cashless", "cashless"); ar("cashless", "بدون نقد");
		en("memperkuat", "strengthen"); ar("memperkuat", "تعزيز");
		en("lancar", "smooth"); ar("lancar", "سلس");
		en("mereduksi", "reduce"); ar("mereduksi", "تقليل");
		en("klerikal", "clerical"); ar("klerikal", "كتابي");
		en("modernisasi", "modernization"); ar("modernisasi", "التحديث");
		en("diterapkan", "applied"); ar("diterapkan", "مطبق");
		en("penyempurnaan", "refinement"); ar("penyempurnaan", "التحسين");
		en("kesan", "impression"); ar("kesan", "انطباع");
		en("literatur", "literature"); ar("literatur", "الأدبيات");
		en("web", "web"); ar("web", "ويب");
		en("membangun", "build"); ar("membangun", "بناء");
		en("menyelaraskan", "align"); ar("menyelaraskan", "مواءمة");
		en("sentralisasi", "centralization"); ar("sentralisasi", "المركزية");
		en("sesudah", "after"); ar("sesudah", "بعد");
		en("sumbernya", "its source"); ar("sumbernya", "مصدره");
		en("ekstra", "extra"); ar("ekstra", "إضافي");
		en("akhirnya", "finally"); ar("akhirnya", "أخيرا");
		en("pengenalan", "introduction/recognition"); ar("pengenalan", "تعريف/تعرف");
		en("dikurangi", "reduced"); ar("dikurangi", "مخفض");
		en("diperiksa", "checked"); ar("diperiksa", "تم فحصه");
		en("diberi", "given"); ar("diberi", "معطى");
		en("face", "face"); ar("face", "وجه");
		en("recognition", "recognition"); ar("recognition", "التعرف");
		en("geotagging", "geotagging"); ar("geotagging", "وسم جغرافي");
		en("admisi", "admission"); ar("admisi", "القبول");
		en("dipahami", "understood"); ar("dipahami", "مفهوم");
		en("aksesibilitas", "accessibility"); ar("aksesibilitas", "إمكانية الوصول");
		en("kiosk", "kiosk"); ar("kiosk", "كشك");
		en("touchscreen", "touchscreen"); ar("touchscreen", "شاشة لمس");
		en("batang", "bar"); ar("batang", "شريط");
		en("scanner", "scanner"); ar("scanner", "ماسح ضوئي");
		en("access", "access"); ar("access", "الوصول");
		en("tertunda", "delayed"); ar("tertunda", "مؤجل");
		en("menjaga", "maintain"); ar("menjaga", "الحفاظ على");
		en("honorarium", "honorarium"); ar("honorarium", "مكافأة");
		en("menghitung", "calculate"); ar("menghitung", "حساب");
		en("pelengkap", "complement"); ar("pelengkap", "مكمل");
		en("didanai", "funded"); ar("didanai", "ممول");
		en("kerangka", "framework"); ar("kerangka", "إطار");
		en("dibahas", "discussed"); ar("dibahas", "تمت مناقشته");
		en("yaitu", "namely"); ar("yaitu", "وهي");
		en("dikenakan", "imposed"); ar("dikenakan", "مفروض");
		en("besarnya", "its amount"); ar("besarnya", "مقداره");
		en("produksi", "production"); ar("produksi", "الإنتاج");
		en("pun", "also"); ar("pun", "أيضا");
		en("pungutan", "levy"); ar("pungutan", "رسم");
		en("cakupan", "coverage"); ar("cakupan", "التغطية");
		en("forum", "forum"); ar("forum", "منتدى");
		en("komersial", "commercial"); ar("komersial", "تجاري");
		en("central", "central"); ar("central", "مركزي");
		en("beroperasi", "operate"); ar("beroperasi", "تعمل");
		en("menentukan", "determine"); ar("menentukan", "تحديد");
		en("okupansi", "occupancy"); ar("okupansi", "الإشغال");
		en("duplikasi", "duplication"); ar("duplikasi", "التكرار");
		en("berkurang", "decreased"); ar("berkurang", "انخفض");
		en("seragam", "uniform"); ar("seragam", "موحد");
		en("divalidasi", "validated"); ar("divalidasi", "تم التحقق منه");
		en("pemasok", "supplier"); ar("pemasok", "المورد");
		en("diverifikasi", "verified"); ar("diverifikasi", "تم التحقق منه");
		en("era", "era"); ar("era", "حقبة");
		en("berkembang", "developing"); ar("berkembang", "متطور");
		en("bersaing", "compete"); ar("bersaing", "المنافسة");
		en("jaminan", "guarantee"); ar("jaminan", "ضمان");
		en("kompetitif", "competitive"); ar("kompetitif", "تنافسي");
		en("templat", "template"); ar("templat", "قالب");
		en("permohonan", "request"); ar("permohonan", "طلب");
		en("pengarsipan", "archiving"); ar("pengarsipan", "الأرشفة");
		en("pinjam", "borrow"); ar("pinjam", "استعارة");
		en("pembagian", "distribution"); ar("pembagian", "التوزيع");
		en("eksklusif", "exclusive"); ar("eksklusif", "حصري");
		en("tulisan", "writing"); ar("tulisan", "الكتابة");
		en("lanjutan", "advanced"); ar("lanjutan", "متقدم");
		en("library", "library"); ar("library", "مكتبة");
		en("dikembangkan", "developed"); ar("dikembangkan", "تم تطويره");
		en("menghadirkan", "present"); ar("menghadirkan", "تقديم");
		en("esensial", "essential"); ar("esensial", "أساسي");
		en("executive", "executive"); ar("executive", "تنفيذي");
		en("terminal", "terminal"); ar("terminal", "محطة");
		en("lobi", "lobby"); ar("lobi", "ردهة");
		en("mengakses", "access"); ar("mengakses", "الوصول إلى");
		en("mengantre", "queue"); ar("mengantre", "الاصطفاف");
		en("menata", "arrange"); ar("menata", "ترتيب");
		en("batch", "batch"); ar("batch", "دفعة");
		en("aditif", "additive"); ar("aditif", "إضافي");
		en("ketangguhan", "robustness"); ar("ketangguhan", "المتانة");
		en("dinegosiasikan", "negotiated"); ar("dinegosiasikan", "تم التفاوض عليه");
		en("baik", "good"); ar("baik", "جيد");
		en("ditaksir", "estimated"); ar("ditaksir", "مقدر");
		en("kasar", "rough"); ar("kasar", "تقريبي");
		en("segera", "immediately"); ar("segera", "فورا");
		en("komposisi", "composition"); ar("komposisi", "التركيب");
		en("ledger", "ledger"); ar("ledger", "دفتر الأستاذ");
		en("pergerakan", "movement"); ar("pergerakan", "الحركة");
		en("outcome", "outcome"); ar("outcome", "المخرج");
		en("sub", "sub"); ar("sub", "فرعي");
		en("sikap", "attitude"); ar("sikap", "الموقف");
		en("pengetahuan", "knowledge"); ar("pengetahuan", "المعرفة");
		en("keterampilan", "skill"); ar("keterampilan", "المهارة");
		en("bronze", "bronze"); ar("bronze", "برونزي");
		en("silver", "silver"); ar("silver", "فضي");
		en("gold", "gold"); ar("gold", "ذهبي");
		en("platinum", "platinum"); ar("platinum", "بلاتيني");
		en("kitab", "book"); ar("kitab", "كتاب");
		en("kuning", "yellow"); ar("kuning", "أصفر");
		en("instalasi", "installation"); ar("instalasi", "التركيب");
		en("diharapkan", "expected"); ar("diharapkan", "متوقع");
		en("mengakselerasi", "accelerate"); ar("mengakselerasi", "تسريع");
		en("dikustomisasi", "customized"); ar("dikustomisasi", "مخصص");
		en("visualisasi", "visualization"); ar("visualisasi", "التصور");
		en("borang", "form"); ar("borang", "نموذج");
		en("perumusan", "formulation"); ar("perumusan", "الصياغة");
		en("penugasan", "assignment"); ar("penugasan", "التكليف");
		en("curang", "cheating"); ar("curang", "الغش");
		en("depresiasi", "depreciation"); ar("depresiasi", "الإهلاك");
		en("jangan", "do not"); ar("jangan", "لا");
		en("memasuki", "enter"); ar("memasuki", "الدخول إلى");
		en("kredensial", "credential"); ar("kredensial", "بيانات الاعتماد");
		en("tak", "not"); ar("tak", "لا");
		en("konsistensi", "consistency"); ar("konsistensi", "الاتساق");
		en("pemrosesan", "processing"); ar("pemrosesan", "المعالجة");
		en("raihan", "achievement"); ar("raihan", "الإنجاز");
		en("pencapaian", "achievement"); ar("pencapaian", "الإنجاز");
		en("institusional", "institutional"); ar("institusional", "مؤسسي");
		en("katalogisasi", "cataloging"); ar("katalogisasi", "الفهرسة");
		en("koordinasi", "coordination"); ar("koordinasi", "التنسيق");
		en("pemanfaatan", "utilization"); ar("pemanfaatan", "الاستفادة");
		en("kesulitan", "difficulty"); ar("kesulitan", "الصعوبة");
		en("dewan", "board"); ar("dewan", "مجلس");
		en("klaim", "claim"); ar("klaim", "مطالبة");
		en("kompatibel", "compatible"); ar("kompatibel", "متوافق");
		en("protokol", "protocol"); ar("protokol", "بروتوكول");
		en("internasional", "international"); ar("internasional", "دولي");
		en("konsolidasi", "consolidation"); ar("konsolidasi", "التوحيد");
		en("disajikan", "presented"); ar("disajikan", "معروض");
		en("pelayanan", "service"); ar("pelayanan", "الخدمة");
		en("helpdesk", "helpdesk"); ar("helpdesk", "مكتب المساعدة");
		en("instruktur", "instructor"); ar("instruktur", "المدرب");
		en("logo", "logo"); ar("logo", "الشعار");
		en("makin", "increasingly"); ar("makin", "بشكل متزايد");
		en("hemat", "save"); ar("hemat", "توفير");
		en("pengambil", "taker"); ar("pengambil", "آخذ");
		en("melengkapi", "complete"); ar("melengkapi", "إكمال");
		en("derajat", "degree"); ar("derajat", "درجة");
		en("produktivitas", "productivity"); ar("produktivitas", "الإنتاجية");
		en("melestarikan", "preserve"); ar("melestarikan", "الحفاظ على");
		en("budaya", "culture"); ar("budaya", "الثقافة");
		en("kandidat", "candidate"); ar("kandidat", "مرشح");
		en("campur", "mix"); ar("campur", "خلط");
		en("berisiko", "risky"); ar("berisiko", "محفوف بالمخاطر");
		en("penjemputan", "pickup"); ar("penjemputan", "الاستلام");
		en("manifest", "manifest"); ar("manifest", "بيان الشحن");
		en("perbedaan", "difference"); ar("perbedaan", "الفرق");
		en("mendaftarkan", "register"); ar("mendaftarkan", "تسجيل");
		en("knowledge", "knowledge"); ar("knowledge", "المعرفة");
		en("demografi", "demographics"); ar("demografi", "الديموغرافيا");
		en("membuka", "open"); ar("membuka", "فتح");
		en("wawancara", "interview"); ar("wawancara", "مقابلة");
		en("mengamankan", "secure"); ar("mengamankan", "تأمين");
		en("krusial", "crucial"); ar("krusial", "حاسم");
		en("mempersiapkan", "prepare"); ar("mempersiapkan", "تحضير");
		en("zero", "zero"); ar("zero", "صفر");
		en("investment", "investment"); ar("investment", "الاستثمار");
		en("jangkauan", "reach"); ar("jangkauan", "المدى");
		en("disiplin", "discipline"); ar("disiplin", "الانضباط");
		en("kenyamanan", "comfort"); ar("kenyamanan", "الراحة");
		en("ketersediaan", "availability"); ar("ketersediaan", "التوفر");
		en("training", "training"); ar("training", "التدريب");
		en("akomodasi", "accommodation"); ar("akomodasi", "الإقامة");
		en("masif", "massive"); ar("masif", "هائل");
		en("keperluan", "necessity"); ar("keperluan", "الحاجة");
		en("mendorong", "encourage"); ar("mendorong", "تشجيع");
		en("literasi", "literacy"); ar("literasi", "محو الأمية");
		en("dinamika", "dynamics"); ar("dinamika", "الديناميكية");
		en("menumpuk", "pile up"); ar("menumpuk", "تراكم");
		en("kompleks", "complex"); ar("kompleks", "معقد");
		en("legal", "legal"); ar("legal", "قانوني");
		en("terkendali", "controlled"); ar("terkendali", "تحت السيطرة");
		en("penulis", "author"); ar("penulis", "الكاتب");
		en("terkecil", "smallest"); ar("terkecil", "الأصغر");
		en("kehilangan", "loss"); ar("kehilangan", "الفقدان");
		en("terjadinya", "occurrence"); ar("terjadinya", "حدوث");
		en("penumpukan", "accumulation"); ar("penumpukan", "التراكم");
		en("produktif", "productive"); ar("produktif", "منتج");
		en("pendidik", "educator"); ar("pendidik", "المربي");
		en("utuh", "whole"); ar("utuh", "كامل");
		en("solid", "solid"); ar("solid", "صلب");
		en("mengoptimalkan", "optimize"); ar("mengoptimalkan", "تحسين");
		en("privat", "private"); ar("privat", "خاص");
		en("mengalihkan", "divert"); ar("mengalihkan", "تحويل");
		en("antarbagian", "inter-division"); ar("antarbagian", "بين الأقسام");
		en("basis", "basis"); ar("basis", "أساس");
		en("sejak", "since"); ar("sejak", "منذ");
		en("meniadakan", "eliminate"); ar("meniadakan", "إلغاء");
		en("mendaftar", "register"); ar("mendaftar", "التسجيل");
		en("mengunggah", "upload"); ar("mengunggah", "رفع");
		en("yudisium", "graduation ceremony"); ar("yudisium", "حفل التخرج");
		en("mengotomatisasi", "automate"); ar("mengotomatisasi", "أتمتة");
		en("terbatas", "limited"); ar("terbatas", "محدود");
		en("keilmuan", "scientific"); ar("keilmuan", "علمي");
		en("ketiga", "third"); ar("ketiga", "ثالث");
		en("kepatuhan", "compliance"); ar("kepatuhan", "الامتثال");
		en("pemeringkatan", "ranking"); ar("pemeringkatan", "الترتيب");
		en("tampung", "capacity"); ar("tampung", "الاستيعاب");
		en("menjembatani", "bridge"); ar("menjembatani", "ربط");
		en("mensinergikan", "synergize"); ar("mensinergikan", "التآزر");
		en("menyederhanakan", "simplify"); ar("menyederhanakan", "تبسيط");
		en("kontrak", "contract"); ar("kontrak", "عقد");
		en("tesis", "thesis"); ar("tesis", "أطروحة");
		en("disertasi", "dissertation"); ar("disertasi", "رسالة دكتوراه");
		en("menghilangkan", "eliminate"); ar("menghilangkan", "إزالة");
		en("mematuhi", "comply"); ar("mematuhi", "الامتثال");
		en("blast", "blast"); ar("blast", "إرسال جماعي");
		en("mengirim", "send"); ar("mengirim", "إرسال");
		en("pribadi", "personal"); ar("pribadi", "شخصي");
		en("finance", "finance"); ar("finance", "المالية");
		en("mendesak", "urgent"); ar("mendesak", "عاجل");
		en("dibangun", "built"); ar("dibangun", "تم بناؤه");
		en("otentikasi", "authentication"); ar("otentikasi", "المصادقة");
		en("dipertanggungjawabkan", "accounted for"); ar("dipertanggungjawabkan", "تتم مساءلته");
		en("negosiasi", "negotiation"); ar("negosiasi", "التفاوض");
		en("volume", "volume"); ar("volume", "الحجم");
		en("mendadak", "sudden"); ar("mendadak", "مفاجئ");
		en("disadari", "realized"); ar("disadari", "تم إدراكه");
		en("bertumbuh", "grow"); ar("bertumbuh", "ينمو");
		en("tersinkron", "synced"); ar("tersinkron", "متزامن");
		en("mode", "mode"); ar("mode", "وضع");
		en("backup", "backup"); ar("backup", "نسخة احتياطية");
		en("memperbarui", "update"); ar("memperbarui", "تحديث");
		en("membuat", "make"); ar("membuat", "إنشاء");
		en("prinsip", "principle"); ar("prinsip", "مبدأ");
		en("poli", "clinic (poly-)"); ar("poli", "عيادة");
		en("penguncian", "locking"); ar("penguncian", "القفل");
		en("matang", "mature"); ar("matang", "ناضج");
		en("cukup", "enough"); ar("cukup", "كافي");
		en("penting", "important"); ar("penting", "مهم");
		en("purchase", "purchase"); ar("purchase", "شراء");
		en("jarak", "distance"); ar("jarak", "المسافة");
		en("jauh", "far"); ar("jauh", "بعيد");
		en("taman", "garden/park"); ar("taman", "حديقة");
		en("penanganan", "handling"); ar("penanganan", "المعالجة");
		en("dialihkan", "diverted"); ar("dialihkan", "تم تحويله");
		en("dispensing", "dispensing"); ar("dispensing", "صرف الدواء");
		en("terbaru", "latest"); ar("terbaru", "الأحدث");
		en("terpencil", "remote"); ar("terpencil", "نائي");
		en("berada", "located"); ar("berada", "يقع");
		en("rawan", "prone"); ar("rawan", "عرضة");
		en("pemberian", "giving"); ar("pemberian", "الإعطاء");
		en("dicari", "searched"); ar("dicari", "تم البحث عنه");
		en("mencari", "search"); ar("mencari", "البحث");
		en("kerahasiaan", "confidentiality"); ar("kerahasiaan", "السرية");
		en("pratama", "primary"); ar("pratama", "أولي");
		en("menjawab", "answer"); ar("menjawab", "الإجابة");
		en("sebuah", "a/an"); ar("sebuah", "واحد");
		en("semakin", "increasingly"); ar("semakin", "بشكل متزايد");
		en("sehari", "a day"); ar("sehari", "يوم واحد");
		en("juta", "million"); ar("juta", "مليون");
		en("selain", "besides"); ar("selain", "بالإضافة إلى");
		en("inti", "core"); ar("inti", "الأساسي");
		en("undang", "invite/law"); ar("undang", "دعوة/قانون");
		en("siapa", "who"); ar("siapa", "من");
		en("digitalkan", "digitized"); ar("digitalkan", "تمت رقمنته");
		en("private", "private"); ar("private", "خاص");
		en("transformatif", "transformative"); ar("transformatif", "تحويلي");
		en("seluler", "mobile/cellular"); ar("seluler", "محمول");
		en("konvensional", "conventional"); ar("konvensional", "تقليدي");
		en("dihasilkan", "produced"); ar("dihasilkan", "تم إنتاجه");
		en("onsite", "onsite"); ar("onsite", "في الموقع");
		en("menelusuri", "trace"); ar("menelusuri", "تتبع");
		en("diberikan", "given"); ar("diberikan", "تم إعطاؤه");
		en("dihubungi", "contacted"); ar("dihubungi", "تم الاتصال به");
		en("smp", "Junior High School"); ar("smp", "المدرسة المتوسطة");
		en("sma", "Senior High School"); ar("sma", "المدرسة الثانوية");
		en("smk", "Vocational High School"); ar("smk", "المدرسة الثانوية المهنية");
		en("tridharma", "Three Pillars of Higher Education"); ar("tridharma", "الركائز الثلاث للتعليم العالي");
		en("dievaluasi", "evaluated"); ar("dievaluasi", "تم تقييمه");

		// Frasa pendek tambahan dari sweep JSP di atas.
		frasa("point of sale", "Point of Sale", "نقطة البيع");
		frasa("real time", "real-time", "الوقت الفعلي");
		frasa("cara kerja", "how it works", "كيفية العمل");
		frasa("mari mulai", "let's start", "لنبدأ");
		frasa("hubungi kami", "contact us", "اتصل بنا");
		frasa("pelajari lebih lanjut", "learn more", "اعرف المزيد");
	}

	private static void muatKosakataTambahan() {
		// ==== Tambahan kosakata umum (auto, dari sweep FAQ/bantuan 2026-07-08) ====
		en("perhatikan", "note"); ar("perhatikan", "لاحظ"); zh("perhatikan", "注意");
		en("berkaitan", "related"); ar("berkaitan", "مرتبط"); zh("berkaitan", "相关");
		en("ragu", "unsure"); ar("ragu", "متردد"); zh("ragu", "犹豫");
		en("ikuti", "follow"); ar("ikuti", "اتبع"); zh("ikuti", "请遵循");
		en("hal", "matter"); ar("hal", "أمر"); zh("hal", "事项");
		en("ketahui", "know"); ar("ketahui", "اعرف"); zh("ketahui", "了解");
		en("tertera", "stated"); ar("tertera", "مذكور"); zh("tertera", "所列");
		en("isian", "field"); ar("isian", "حقل"); zh("isian", "填写项");
		en("penyaring", "filter"); ar("penyaring", "مرشح"); zh("penyaring", "筛选器");
		en("biasanya", "usually"); ar("biasanya", "عادة"); zh("biasanya", "通常");
		en("sebagian", "some"); ar("sebagian", "بعض"); zh("sebagian", "部分");
		en("tanya", "ask"); ar("tanya", "اسأل"); zh("tanya", "询问");
		en("umumnya", "generally"); ar("umumnya", "عموما"); zh("umumnya", "一般");
		en("diperlukan", "required"); ar("diperlukan", "مطلوب"); zh("diperlukan", "所需");
		en("penggunaan", "usage"); ar("penggunaan", "استخدام"); zh("penggunaan", "使用");
		en("menemukan", "find"); ar("menemukan", "إيجاد"); zh("menemukan", "查找");
		en("keliru", "incorrect"); ar("keliru", "خاطئ"); zh("keliru", "错误");
		en("peramban", "browser"); ar("peramban", "المتصفح"); zh("peramban", "浏览器");
		en("memeriksa", "check"); ar("memeriksa", "فحص"); zh("memeriksa", "检查");
		en("kekeliruan", "error"); ar("kekeliruan", "خطأ"); zh("kekeliruan", "差错");
		en("menambah", "add"); ar("menambah", "إضافة"); zh("menambah", "添加");
		en("kewenangan", "authority"); ar("kewenangan", "صلاحية"); zh("kewenangan", "权限");
		en("terutama", "especially"); ar("terutama", "خصوصا"); zh("terutama", "尤其");
		en("menutup", "close"); ar("menutup", "إغلاق"); zh("menutup", "关闭");
		en("menekan", "press"); ar("menekan", "اضغط"); zh("menekan", "按下");
		en("jendela", "window"); ar("jendela", "نافذة"); zh("jendela", "窗口");
		en("tips", "tips"); ar("tips", "نصائح"); zh("tips", "提示");
		en("perbaiki", "fix"); ar("perbaiki", "أصلح"); zh("perbaiki", "修正");
		en("mengapa", "why"); ar("mengapa", "لماذا"); zh("mengapa", "为什么");
		en("memerlukan", "require"); ar("memerlukan", "يتطلب"); zh("memerlukan", "需要");
		en("sebaiknya", "should"); ar("sebaiknya", "يفضل"); zh("sebaiknya", "最好");
		en("menyesuaikan", "adjust"); ar("menyesuaikan", "تعديل"); zh("menyesuaikan", "调整");
		en("daripada", "rather than"); ar("daripada", "بدلا من"); zh("daripada", "而不是");
		en("menghemat", "save"); ar("menghemat", "توفير"); zh("menghemat", "节省");
		en("tambahan", "additional"); ar("tambahan", "إضافي"); zh("tambahan", "附加");
		en("misalnya", "for example"); ar("misalnya", "مثلا"); zh("misalnya", "例如");
		en("siapkan", "prepare"); ar("siapkan", "جهز"); zh("siapkan", "准备");
		en("cermat", "careful"); ar("cermat", "بدقة"); zh("cermat", "仔细");
		en("diperbaiki", "repaired"); ar("diperbaiki", "تم إصلاحه"); zh("diperbaiki", "已修正");
		en("berubah", "change"); ar("berubah", "تغير"); zh("berubah", "更改");
		en("berwenang", "authorized"); ar("berwenang", "مخول"); zh("berwenang", "有权");
		en("kemudian", "then"); ar("kemudian", "ثم"); zh("kemudian", "然后");
		en("penghapusan", "deletion"); ar("penghapusan", "حذف"); zh("penghapusan", "删除");
		en("hasilnya", "the result"); ar("hasilnya", "النتيجة"); zh("hasilnya", "结果");
		en("dibuka", "opened"); ar("dibuka", "مفتوح"); zh("dibuka", "已打开");
		en("tampak", "appears"); ar("tampak", "يظهر"); zh("tampak", "显示");
		en("mungkin", "possibly"); ar("mungkin", "ربما"); zh("mungkin", "可能");
		en("sedangkan", "whereas"); ar("sedangkan", "بينما"); zh("sedangkan", "而");
		en("namun", "however"); ar("namun", "لكن"); zh("namun", "但是");
		en("terlihat", "visible"); ar("terlihat", "مرئي"); zh("terlihat", "可见");
		en("persiapan", "preparation"); ar("persiapan", "تحضير"); zh("persiapan", "准备");
		en("memang", "indeed"); ar("memang", "بالفعل"); zh("memang", "确实");
		en("arti", "meaning"); ar("arti", "معنى"); zh("arti", "含义");
		en("laporkan", "report"); ar("laporkan", "أبلغ"); zh("laporkan", "报告");
		en("mirip", "similar"); ar("mirip", "مشابه"); zh("mirip", "相似");
		en("mengurutkan", "sort"); ar("mengurutkan", "فرز"); zh("mengurutkan", "排序");
		en("kemungkinan", "possibility"); ar("kemungkinan", "احتمال"); zh("kemungkinan", "可能性");
		en("lengkapi", "complete"); ar("lengkapi", "أكمل"); zh("lengkapi", "补全");
		en("terkini", "latest"); ar("terkini", "الأحدث"); zh("terkini", "最新");
		en("bergantung", "depends"); ar("bergantung", "يعتمد"); zh("bergantung", "取决于");
		en("ditinjau", "reviewed"); ar("ditinjau", "تمت مراجعته"); zh("ditinjau", "已审阅");
		en("mengisi", "fill"); ar("mengisi", "ملء"); zh("mengisi", "填写");
		en("menghindari", "avoid"); ar("menghindari", "تجنب"); zh("menghindari", "避免");
		en("manfaatkan", "utilize"); ar("manfaatkan", "استفد"); zh("manfaatkan", "利用");
		en("perbarui", "update"); ar("perbarui", "حدث"); zh("perbarui", "更新");
		en("dipulihkan", "restored"); ar("dipulihkan", "تمت استعادته"); zh("dipulihkan", "已恢复");
		en("menambahkan", "add"); ar("menambahkan", "إضافة"); zh("menambahkan", "增加");
		en("urutan", "order"); ar("urutan", "ترتيب"); zh("urutan", "顺序");
		en("menindaklanjuti", "follow up"); ar("menindaklanjuti", "متابعة"); zh("menindaklanjuti", "跟进");
		en("ditampilkan", "displayed"); ar("ditampilkan", "معروض"); zh("ditampilkan", "已显示");
		en("sementara", "temporarily"); ar("sementara", "مؤقتا"); zh("sementara", "暂时");
		en("mengenali", "recognize"); ar("mengenali", "تعرف"); zh("mengenali", "识别");
		en("menyaring", "filter"); ar("menyaring", "تصفية"); zh("menyaring", "筛选");
		en("selama", "during"); ar("selama", "خلال"); zh("selama", "期间");
		en("sampaikan", "convey"); ar("sampaikan", "بلغ"); zh("sampaikan", "传达");
		en("perhatian", "attention"); ar("perhatian", "انتباه"); zh("perhatian", "关注");
		en("keadaan", "condition"); ar("keadaan", "حالة"); zh("keadaan", "状态");
		en("diselesaikan", "completed"); ar("diselesaikan", "تم إنجازه"); zh("diselesaikan", "已完成");
		en("isinya", "its content"); ar("isinya", "محتواه"); zh("isinya", "其内容");
		en("berpindah", "move"); ar("berpindah", "انتقل"); zh("berpindah", "切换");
		en("berurutan", "sequential"); ar("berurutan", "متسلسل"); zh("berurutan", "依次");
		en("meminta", "request"); ar("meminta", "طلب"); zh("meminta", "请求");
		en("navigasi", "navigation"); ar("navigasi", "التنقل"); zh("navigasi", "导航");
		en("keduanya", "both"); ar("keduanya", "كلاهما"); zh("keduanya", "两者");
		en("stabil", "stable"); ar("stabil", "مستقر"); zh("stabil", "稳定");
		en("termuat", "loaded"); ar("termuat", "محمل"); zh("termuat", "已加载");
		en("menyetujui", "approve"); ar("menyetujui", "الموافقة"); zh("menyetujui", "批准");
		en("merespons", "respond"); ar("merespons", "يستجيب"); zh("merespons", "响应");
		en("berasal", "originates"); ar("berasal", "ينشأ"); zh("berasal", "来自");
		en("berfungsi", "functions"); ar("berfungsi", "يعمل"); zh("berfungsi", "起作用");
		en("janggal", "odd"); ar("janggal", "غريب"); zh("janggal", "异常");
		en("membatalkan", "cancel"); ar("membatalkan", "إلغاء"); zh("membatalkan", "取消");
		en("mengetik", "type"); ar("mengetik", "اكتب"); zh("mengetik", "输入");
		en("menandakan", "indicates"); ar("menandakan", "يشير"); zh("menandakan", "表示");
		en("seputar", "about"); ar("seputar", "حول"); zh("seputar", "关于");
		en("konsultasikan", "consult"); ar("konsultasikan", "استشر"); zh("konsultasikan", "咨询");
		en("disegarkan", "refreshed"); ar("disegarkan", "تم التحديث"); zh("disegarkan", "已刷新");
		en("menerjemahkan", "translate"); ar("menerjemahkan", "ترجمة"); zh("menerjemahkan", "翻译");
		en("ponsel", "phone"); ar("ponsel", "الهاتف"); zh("ponsel", "手机");
		en("terjawab", "answered"); ar("terjawab", "تمت الإجابة"); zh("terjawab", "已解答");
		en("mendasar", "basic"); ar("mendasar", "أساسي"); zh("mendasar", "基本");
		en("duplikat", "duplicate"); ar("duplikat", "مكرر"); zh("duplikat", "重复");
		en("kerjakan", "do"); ar("kerjakan", "نفذ"); zh("kerjakan", "执行");
		en("tanyakan", "ask"); ar("tanyakan", "اسأل"); zh("tanyakan", "询问");
		en("menemui", "encounter"); ar("menemui", "تواجه"); zh("menemui", "遇到");
		en("sebenarnya", "actually"); ar("sebenarnya", "في الواقع"); zh("sebenarnya", "实际上");
		en("ketelitian", "accuracy"); ar("ketelitian", "الدقة"); zh("ketelitian", "准确");
		en("membagikan", "share"); ar("membagikan", "مشاركة"); zh("membagikan", "分享");
		en("teliti", "careful"); ar("teliti", "دقيق"); zh("teliti", "仔细");
		en("pembaruan", "update"); ar("pembaruan", "تحديث"); zh("pembaruan", "更新");
		en("prosedur", "procedure"); ar("prosedur", "إجراء"); zh("prosedur", "流程");
		en("mencerminkan", "reflects"); ar("mencerminkan", "يعكس"); zh("mencerminkan", "反映");
		en("memengaruhi", "affect"); ar("memengaruhi", "يؤثر"); zh("memengaruhi", "影响");
		en("berikutnya", "next"); ar("berikutnya", "التالي"); zh("berikutnya", "下一个");
		en("tetapkan", "set"); ar("tetapkan", "حدد"); zh("tetapkan", "设定");
		en("datanya", "its data"); ar("datanya", "بياناته"); zh("datanya", "其数据");
		en("jaga", "keep"); ar("jaga", "حافظ"); zh("jaga", "保持");
		en("ditetapkan", "set"); ar("ditetapkan", "محدد"); zh("ditetapkan", "已设定");
		en("terlebih", "especially"); ar("terlebih", "خاصة"); zh("terlebih", "尤其");
		en("meninjau", "review"); ar("meninjau", "مراجعة"); zh("meninjau", "审阅");
		en("tekan", "press"); ar("tekan", "اضغط"); zh("tekan", "按");
		en("menimbulkan", "cause"); ar("menimbulkan", "يسبب"); zh("menimbulkan", "导致");
		en("relevan", "relevant"); ar("relevan", "ذو صلة"); zh("relevan", "相关");
		en("memilih", "choose"); ar("memilih", "اختر"); zh("memilih", "选择");
		en("memakai", "use"); ar("memakai", "استخدم"); zh("memakai", "使用");
		en("label", "label"); ar("label", "تسمية"); zh("label", "标签");
		en("terlewat", "missed"); ar("terlewat", "فائت"); zh("terlewat", "遗漏");
		en("penomoran", "numbering"); ar("penomoran", "ترقيم"); zh("penomoran", "编号");
		en("telusuri", "trace"); ar("telusuri", "تتبع"); zh("telusuri", "追溯");
		en("diubah", "changed"); ar("diubah", "تم تغييره"); zh("diubah", "已更改");
		en("menandai", "mark"); ar("menandai", "وضع علامة"); zh("menandai", "标记");
		en("memperbaiki", "fix"); ar("memperbaiki", "إصلاح"); zh("memperbaiki", "修正");
		en("usang", "obsolete"); ar("usang", "قديم"); zh("usang", "过时");
		en("dibutuhkan", "needed"); ar("dibutuhkan", "مطلوب"); zh("dibutuhkan", "所需");
		en("sesuaikan", "adjust"); ar("sesuaikan", "عدل"); zh("sesuaikan", "调整");
		en("bermakna", "meaningful"); ar("bermakna", "ذو معنى"); zh("bermakna", "有意义");
		en("masalah", "problem"); ar("masalah", "مشكلة"); zh("masalah", "问题");
		en("beri", "give"); ar("beri", "أعط"); zh("beri", "给予");
		en("serupa", "similar"); ar("serupa", "مماثل"); zh("serupa", "类似");
		en("seharusnya", "should"); ar("seharusnya", "ينبغي"); zh("seharusnya", "应该");
		en("salinan", "copy"); ar("salinan", "نسخة"); zh("salinan", "副本");
		en("kebiasaan", "habit"); ar("kebiasaan", "عادة"); zh("kebiasaan", "习惯");
		en("dicetak", "printed"); ar("dicetak", "مطبوع"); zh("dicetak", "已打印");
		en("kenali", "recognize"); ar("kenali", "تعرف"); zh("kenali", "识别");
		en("ditandai", "marked"); ar("ditandai", "معلم"); zh("ditandai", "已标记");
		en("mengunduh", "download"); ar("mengunduh", "تنزيل"); zh("mengunduh", "下载");
		en("hilang", "lost"); ar("hilang", "مفقود"); zh("hilang", "丢失");
		en("diatur", "arranged"); ar("diatur", "منظم"); zh("diatur", "已设置");
		en("rusak", "broken"); ar("rusak", "تالف"); zh("rusak", "损坏");
		en("tuntas", "complete"); ar("tuntas", "مكتمل"); zh("tuntas", "完成");
		en("perilaku", "behavior"); ar("perilaku", "سلوك"); zh("perilaku", "行为");
		en("diminta", "requested"); ar("diminta", "مطلوب"); zh("diminta", "被请求");
		en("melibatkan", "involve"); ar("melibatkan", "يشمل"); zh("melibatkan", "涉及");
		en("terisi", "filled"); ar("terisi", "ممتلئ"); zh("terisi", "已填写");
		en("kejadian", "event"); ar("kejadian", "حدث"); zh("kejadian", "事件");
		en("istilah", "term"); ar("istilah", "مصطلح"); zh("istilah", "术语");
		en("ditujukan", "intended"); ar("ditujukan", "موجه"); zh("ditujukan", "面向");
		en("fokus", "focus"); ar("fokus", "تركيز"); zh("fokus", "聚焦");
		en("dibagikan", "shared"); ar("dibagikan", "تمت مشاركته"); zh("dibagikan", "已分享");
		en("nyaman", "comfortable"); ar("nyaman", "مريح"); zh("nyaman", "舒适");
		en("biasakan", "get used to"); ar("biasakan", "اعتد"); zh("biasakan", "养成习惯");
		en("berdampak", "impact"); ar("berdampak", "يؤثر"); zh("berdampak", "产生影响");
		en("melanjutkan", "continue"); ar("melanjutkan", "متابعة"); zh("melanjutkan", "继续");
		en("meninggalkan", "leave"); ar("meninggalkan", "مغادرة"); zh("meninggalkan", "离开");
		en("padahal", "whereas"); ar("padahal", "رغم أن"); zh("padahal", "尽管");
		en("menyimpulkan", "conclude"); ar("menyimpulkan", "استنتاج"); zh("menyimpulkan", "得出结论");
		en("ejaan", "spelling"); ar("ejaan", "الإملاء"); zh("ejaan", "拼写");
		en("letak", "location"); ar("letak", "موقع"); zh("letak", "位置");
		en("terdiri", "consists"); ar("terdiri", "يتكون"); zh("terdiri", "由...组成");
		en("kumpulan", "collection"); ar("kumpulan", "مجموعة"); zh("kumpulan", "集合");
		en("terpotong", "cut off"); ar("terpotong", "مقطوع"); zh("terpotong", "被截断");
		en("menyertakan", "include"); ar("menyertakan", "إرفاق"); zh("menyertakan", "附上");
		en("disarankan", "recommended"); ar("disarankan", "ينصح"); zh("disarankan", "建议");
		en("dibanding", "compared to"); ar("dibanding", "مقارنة بـ"); zh("dibanding", "相比");
		en("dihindari", "avoided"); ar("dihindari", "يتجنب"); zh("dihindari", "应避免");
		en("meningkat", "increase"); ar("meningkat", "يزداد"); zh("meningkat", "提升");
		en("dikenal", "known"); ar("dikenal", "معروف"); zh("dikenal", "已知");
		en("persempit", "narrow"); ar("persempit", "ضيق"); zh("persempit", "缩小");
		en("jeda", "pause"); ar("jeda", "توقف"); zh("jeda", "暂停");
		en("melapor", "report"); ar("melapor", "يبلغ"); zh("melapor", "汇报");
		en("mengandalkan", "rely on"); ar("mengandalkan", "الاعتماد على"); zh("mengandalkan", "依赖");
		en("elemen", "element"); ar("elemen", "عنصر"); zh("elemen", "元素");
		en("namanya", "its name"); ar("namanya", "اسمه"); zh("namanya", "其名称");
		en("ujung", "end"); ar("ujung", "نهاية"); zh("ujung", "末端");
		en("orientasi", "orientation"); ar("orientasi", "الاتجاه"); zh("orientasi", "方向");
		en("bingung", "confused"); ar("bingung", "محتار"); zh("bingung", "困惑");
		en("tafsir", "interpretation"); ar("tafsir", "تفسير"); zh("tafsir", "解读");
		en("kembalikan", "restore"); ar("kembalikan", "أعد"); zh("kembalikan", "恢复");
		en("berbagi", "share"); ar("berbagi", "مشاركة"); zh("berbagi", "共享");
		en("menolak", "reject"); ar("menolak", "رفض"); zh("menolak", "拒绝");
		en("unsur", "element"); ar("unsur", "عنصر"); zh("unsur", "要素");
		en("kerapian", "neatness"); ar("kerapian", "الترتيب"); zh("kerapian", "整洁");
		en("penamaan", "naming"); ar("penamaan", "التسمية"); zh("penamaan", "命名");
		en("pendukungnya", "its support"); ar("pendukungnya", "دعمه"); zh("pendukungnya", "其支持");
		en("keterangannya", "its description"); ar("keterangannya", "وصفه"); zh("keterangannya", "其说明");
		en("tugasnya", "its task"); ar("tugasnya", "مهمته"); zh("tugasnya", "其任务");
		en("pembukuan", "bookkeeping"); ar("pembukuan", "مسك الدفاتر"); zh("pembukuan", "记账");
		en("penggolongan", "classification"); ar("penggolongan", "التصنيف"); zh("penggolongan", "分类");
		en("penyaringan", "filtering"); ar("penyaringan", "التصفية"); zh("penyaringan", "筛选");
		en("penambahan", "addition"); ar("penambahan", "الإضافة"); zh("penambahan", "添加");
		en("keterkaitan", "linkage"); ar("keterkaitan", "الترابط"); zh("keterkaitan", "关联");
		en("pembatalan", "cancellation"); ar("pembatalan", "الإلغاء"); zh("pembatalan", "取消");
		en("pembatasan", "restriction"); ar("pembatasan", "التقييد"); zh("pembatasan", "限制");
		en("menyoroti", "highlight"); ar("menyoroti", "تسليط الضوء"); zh("menyoroti", "突出");
		en("mengulang", "repeat"); ar("mengulang", "كرر"); zh("mengulang", "重复");
		en("terjemahan", "translation"); ar("terjemahan", "الترجمة"); zh("terjemahan", "翻译");
		en("tertinggal", "left behind"); ar("tertinggal", "متأخر"); zh("tertinggal", "落后");
		en("dampaknya", "its impact"); ar("dampaknya", "تأثيره"); zh("dampaknya", "其影响");
		en("diperhatikan", "noticed"); ar("diperhatikan", "ملحوظ"); zh("diperhatikan", "被注意");
		en("tertukar", "swapped"); ar("tertukar", "متبادل"); zh("tertukar", "弄错");
		en("membiasakan", "accustom"); ar("membiasakan", "تعويد"); zh("membiasakan", "使习惯");
		en("bacalah", "read"); ar("bacalah", "اقرأ"); zh("bacalah", "请阅读");
		en("ditulis", "written"); ar("ditulis", "مكتوب"); zh("ditulis", "已写");
		en("harapkan", "expect"); ar("harapkan", "توقع"); zh("harapkan", "期望");
		en("melengkapinya", "complete it"); ar("melengkapinya", "إكماله"); zh("melengkapinya", "补全它");
	}

	static {
		muatKosakataTambahan();
	}

	/** Jumlah kata maksimum yang dicoba sebagai satu frasa saat segmentasi longest-match. */
	private static final int MAKS_KATA_FRASA = 7;

	public static String terjemah(String teksIndonesia, String targetLang) {
		if (teksIndonesia == null || teksIndonesia.trim().length() == 0) {
			return teksIndonesia == null ? "" : teksIndonesia;
		}
		String tl = targetLang == null ? "" : targetLang.toLowerCase();
		boolean arab = tl.startsWith("ar") || tl.contains("arab");
		boolean mandarin = tl.startsWith("zh") || tl.contains("mandarin") || tl.contains("china")
				|| tl.contains("chinese");
		Map<String, String> frasa = mandarin ? FRASA_ZH : (arab ? FRASA_AR : FRASA_EN);
		Map<String, String> kata = mandarin ? KATA_ZH : (arab ? KATA_AR : KATA_EN);
		boolean tanpaKapital = arab || mandarin;

		// (1) Cocokkan SELURUH string sebagai satu frasa (jalur cepat & paling akurat).
		String norm = teksIndonesia.trim().toLowerCase();
		String normBersih = norm;
		while (normBersih.length() > 0 && isTandaBaca(normBersih.charAt(normBersih.length() - 1))) {
			normBersih = normBersih.substring(0, normBersih.length() - 1);
		}
		if (frasa.containsKey(normBersih)) {
			return frasa.get(normBersih);
		}

		// (2) Pisahkan menjadi KATA + PEMISAH (spasi/tanda baca/kurung kurawal dipertahankan urut).
		List<String> words = new ArrayList<String>();
		List<String> seps = new ArrayList<String>(); // seps[k] = pemisah SEBELUM words[k]; elemen terakhir = pemisah ekor
		StringBuilder sep = new StringBuilder();
		StringBuilder w = new StringBuilder();
		for (int i = 0; i <= teksIndonesia.length(); i++) {
			char c = i < teksIndonesia.length() ? teksIndonesia.charAt(i) : ' ';
			boolean pemisah = i == teksIndonesia.length() || Character.isWhitespace(c) || isTandaBaca(c) || c == '{'
					|| c == '}';
			if (!pemisah) {
				if (w.length() == 0) {
					seps.add(sep.toString());
					sep.setLength(0);
				}
				w.append(c);
			} else {
				if (w.length() > 0) {
					words.add(w.toString());
					w.setLength(0);
				}
				if (i < teksIndonesia.length()) {
					sep.append(c);
				}
			}
		}
		seps.add(sep.toString()); // pemisah ekor (setelah kata terakhir)

		// (3) Susun ulang dengan SEGMENTASI LONGEST-MATCH: cocokkan jendela kata terpanjang yang ada di kamus
		// frasa; bila tak ada, jatuh ke kata tunggal (dengan pencarian akar/stemming) atau dibiarkan.
		int n = words.size();
		StringBuilder out = new StringBuilder(teksIndonesia.length() + 32);
		int idx = 0;
		while (idx < n) {
			out.append(seps.get(idx));
			String frasaVal = null;
			int cocok = 0;
			int maxW = Math.min(MAKS_KATA_FRASA, n - idx);
			for (int wlen = maxW; wlen >= 2; wlen--) {
				StringBuilder key = new StringBuilder();
				for (int j = 0; j < wlen; j++) {
					if (j > 0) {
						key.append(' ');
					}
					key.append(words.get(idx + j).toLowerCase());
				}
				String k = key.toString();
				if (frasa.containsKey(k)) {
					frasaVal = frasa.get(k);
					cocok = wlen;
					break;
				}
			}
			if (frasaVal != null) {
				out.append(tiruKapital(frasaVal, words.get(idx), tanpaKapital));
				idx += cocok;
			} else {
				out.append(terjemahKata(words.get(idx), kata, tanpaKapital));
				idx += 1;
			}
		}
		out.append(seps.get(n));

		String hasil = out.toString();
		return hasil.trim().length() == 0 ? teksIndonesia : hasil;
	}

	private static String tiruKapital(String nilai, String kataSumber, boolean tanpaKapital) {
		if (tanpaKapital || nilai == null || nilai.length() == 0 || kataSumber == null || kataSumber.length() == 0) {
			return nilai;
		}
		if (Character.isUpperCase(kataSumber.charAt(0))) {
			return Character.toUpperCase(nilai.charAt(0)) + nilai.substring(1);
		}
		return nilai;
	}

	private static String terjemahKata(String kataAsli, Map<String, String> kata, boolean tanpaKapital) {
		String lower = kataAsli.toLowerCase();
		String t = kata.get(lower);
		if (t == null) {
			// Coba temukan AKAR kata (stemming morfologis Indonesia) yang terdaftar di kamus.
			String akar = cariAkarBerkamus(lower, kata);
			if (akar != null) {
				t = kata.get(akar);
			}
		}
		if (t == null) {
			// tak dikenal → biarkan apa adanya (manual-assisted)
			return kataAsli;
		}
		return tiruKapital(t, kataAsli, tanpaKapital);
	}

	/**
	 * <b>Stemmer morfologis Indonesia ringan & konservatif.</b> Mencoba melepas imbuhan (awalan/akhiran)
	 * beserta perubahan morfofonemik umum (meny→s, meng→k, mem→p, men→t) dan HANYA menerima akar yang
	 * BENAR-BENAR ada di kamus — sehingga tidak memaksakan tebakan yang salah. Mengembalikan akar terdaftar
	 * atau {@code null}.
	 */
	private static String cariAkarBerkamus(String lower, Map<String, String> kata) {
		if (lower == null || lower.length() < 4) {
			return null;
		}
		String[] SUF = { "kannya", "annya", "inya", "nya", "kan", "lah", "kah", "pun", "an", "i" };
		String[] PRE = { "memper", "member", "menge", "menye", "meng", "meny", "mem", "men", "peng", "peny", "pem",
				"pen", "per", "ber", "bel", "ter", "di", "ke", "se", "pe", "me" };

		// (a) akhiran saja
		String rr = lepasAkhiran(lower, SUF, kata);
		if (rr != null) {
			return rr;
		}
		// (b) awalan saja (termasuk rekonstruksi morfofonemik)
		for (int p = 0; p < PRE.length; p++) {
			String pre = PRE[p];
			if (lower.startsWith(pre) && lower.length() - pre.length() >= 2) {
				String sisa = lower.substring(pre.length());
				String akar = cocokAkar(sisa, pre, kata);
				if (akar != null) {
					return akar;
				}
			}
		}
		// (c) awalan + akhiran
		for (int p = 0; p < PRE.length; p++) {
			String pre = PRE[p];
			if (lower.startsWith(pre) && lower.length() - pre.length() >= 3) {
				String tengah = lower.substring(pre.length());
				for (int s = 0; s < SUF.length; s++) {
					String suf = SUF[s];
					if (tengah.endsWith(suf) && tengah.length() - suf.length() >= 2) {
						String sisa = tengah.substring(0, tengah.length() - suf.length());
						String akar = cocokAkar(sisa, pre, kata);
						if (akar != null) {
							return akar;
						}
					}
				}
			}
		}
		return null;
	}

	private static String lepasAkhiran(String lower, String[] SUF, Map<String, String> kata) {
		for (int s = 0; s < SUF.length; s++) {
			String suf = SUF[s];
			if (lower.endsWith(suf) && lower.length() - suf.length() >= 3) {
				String r = lower.substring(0, lower.length() - suf.length());
				if (kata.containsKey(r)) {
					return r;
				}
			}
		}
		return null;
	}

	/**
	 * Cocokkan sisa kata setelah awalan dilepas dengan akar terdaftar, mempertimbangkan rekonstruksi
	 * morfofonemik peluluhan konsonan awal: meN-/peN- + "y..." ⇐ root "s..."; + vokal ⇐ root bisa "k..."/vokal;
	 * mem-/pem- ⇐ root "p..."/apa adanya; men-/pen- ⇐ root "t..."/apa adanya.
	 */
	private static String cocokAkar(String sisa, String pre, Map<String, String> kata) {
		if (sisa == null || sisa.length() < 2) {
			return null;
		}
		// apa adanya
		if (kata.containsKey(sisa)) {
			return sisa;
		}
		boolean ny = pre.equals("meny") || pre.equals("peny") || pre.equals("menye") || pre.equals("penye");
		boolean ng = pre.equals("meng") || pre.equals("peng") || pre.equals("menge") || pre.equals("penge");
		boolean m = pre.equals("mem") || pre.equals("pem") || pre.equals("member");
		boolean nn = pre.equals("men") || pre.equals("pen");
		if (ny) {
			String c = "s" + sisa; // menyimpan → s+impan = simpan
			if (kata.containsKey(c)) {
				return c;
			}
		}
		if (ng) {
			String c = "k" + sisa; // mengirim → k+irim = kirim
			if (kata.containsKey(c)) {
				return c;
			}
		}
		if (m) {
			String c = "p" + sisa; // memukul → p+ukul = pukul
			if (kata.containsKey(c)) {
				return c;
			}
		}
		if (nn) {
			String c = "t" + sisa; // menulis → t+ulis = tulis
			if (kata.containsKey(c)) {
				return c;
			}
		}
		return null;
	}

	private static boolean isTandaBaca(char c) {
		return c == '.' || c == ',' || c == ';' || c == ':' || c == '!' || c == '?' || c == '"' || c == '\''
				|| c == '(' || c == ')' || c == '-' || c == '/';
	}

	/**
	 * Terjemahkan konten HTML dengan MENJAGA tag &amp; atribut: hanya SEGMEN TEKS di antara tag yang
	 * diterjemahkan (frasa/kata via {@link #terjemah(String, String)}); isi {@code <script>} &amp;
	 * {@code <style>} dibiarkan utuh. Dipakai untuk auto-terjemah halaman bantuan bila bahasa aktif
	 * bukan Indonesia. Aman: bila argumen null/kosong dikembalikan apa adanya.
	 */
	public static String terjemahHtml(String html, String targetLang) {
		if (html == null || html.length() == 0) {
			return html;
		}
		StringBuilder out = new StringBuilder(html.length() + 64);
		String lowerAll = html.toLowerCase();
		int i = 0;
		int n = html.length();
		while (i < n) {
			char c = html.charAt(i);
			if (c == '<') {
				int end = html.indexOf('>', i);
				if (end < 0) {
					out.append(html.substring(i));
					break;
				}
				String tag = html.substring(i, end + 1);
				out.append(tag);
				String lowerTag = tag.toLowerCase();
				if (lowerTag.startsWith("<script") || lowerTag.startsWith("<style")) {
					String penutup = lowerTag.startsWith("<script") ? "</script" : "</style";
					int close = lowerAll.indexOf(penutup, end + 1);
					if (close < 0) {
						out.append(html.substring(end + 1));
						break;
					}
					out.append(html.substring(end + 1, close));
					i = close;
				} else {
					i = end + 1;
				}
			} else {
				int lt = html.indexOf('<', i);
				if (lt < 0) {
					lt = n;
				}
				out.append(terjemahSegmenTeks(html.substring(i, lt), targetLang));
				i = lt;
			}
		}
		return out.toString();
	}

	private static String terjemahSegmenTeks(String teks, String targetLang) {
		if (teks == null || teks.trim().length() == 0) {
			return teks;
		}
		int a = 0;
		while (a < teks.length() && Character.isWhitespace(teks.charAt(a))) {
			a++;
		}
		int b = teks.length();
		while (b > a && Character.isWhitespace(teks.charAt(b - 1))) {
			b--;
		}
		return teks.substring(0, a) + terjemah(teks.substring(a, b), targetLang) + teks.substring(b);
	}
}
