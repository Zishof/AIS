package ais.action.master.helper.profile;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;
import org.zkoss.zul.LayoutRegion;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.ui.util.MyRowStyled;

/**
 * <h3>ProfileUiHelper — Pustaka Pembangun Antarmuka Panel Profil</h3>
 *
 * <p><b>Untuk apa:</b> Menyediakan seluruh blok pembangun HTML/CSS/ZK yang dipakai bersama
 * oleh semua kelas profil ({@link ProfileGuru}, {@link ProfileSiswa}, {@link ProfileMahasiswa},
 * {@link ProfileDosen}, {@link ProfileAdminSekolah}, {@link ProfileAdminPerguruanTinggi},
 * {@link ProfileGabunganPengguna}). Helper ini memastikan konsistensi visual dan tidak
 * ada duplikasi kode HTML template di setiap profil.</p>
 *
 * <p><b>Kelompok fungsionalitas:</b>
 * <ul>
 *   <li><b>CSS:</b> Konstanta {@code CSS} berisi seluruh gaya {@code .ais-profile-*}
 *       dan animasi (aisProfileIn, aisStatPop, aisShine, aisSpin). Disuntikkan sekali
 *       per halaman via {@link #prepareContentParent(Component)} atau {@link #appendCss}.</li>
 *   <li><b>Kartu statistik:</b> {@link #stat}, {@link #statPolos}, {@link #statClickable},
 *       {@link #donut}, {@link #donutClickable}, {@link #statsWrap} — membangun kartu
 *       angka dalam bentuk HTML string.</li>
 *   <li><b>Popup modal:</b> {@link #modal}, {@link #barisRincian}, {@link #tabelRincian},
 *       {@link #modalAngka} — membangun overlay rincian yang muncul saat kartu diklik.</li>
 *   <li><b>Layout:</b> {@link #panel}, {@link #infoBanner}, {@link #cols},
 *       {@link #infoTable}, {@link #mulaiKartuIdentitas}, {@link #appendPanelInfoRow}.</li>
 *   <li><b>Grafik:</b> {@link #buildTrendBars} (batang per semester), {@link #buildRadar}
 *       (radar 4-sumbu), {@link #radarN} (radar N-sumbu), {@link #barList} (bar horizontal).</li>
 *   <li><b>Query Hibernate:</b> {@link #hitung} (COUNT via openSession),
 *       {@link #kelompok} (GROUP BY via openSession), {@link #closeOpenSession}.</li>
 *   <li><b>Format utilitas:</b> {@link #fmt}, {@link #asDouble}, {@link #text},
 *       {@link #esc}, {@link #js}, {@link #notBlank}, {@link #umur}, {@link #tanggal},
 *       {@link #ttl}, {@link #nama}.</li>
 *   <li><b>Preset:</b> {@link #appendPrestasiRingkas} — kartu prestasi kontekstual
 *       (sekolah/PT/pegawai) dengan flag {@code sembunyikanPegawai} untuk anti-duplikat.</li>
 * </ul>
 * </p>
 *
 * <p><b>ThreadLocal sembunyikanPegawai:</b> Dipakai oleh {@link ProfileGabunganPengguna}
 * untuk mencegah kartu "Prestasi Pegawai" muncul dua kali ketika profil sekolah dan PT
 * keduanya dipanggil dalam satu render. Flag bersifat per-thread (ZK event thread) dan
 * selalu direset di blok {@code finally}.</p>
 *
 * <p><b>Keamanan HTML:</b> Semua nilai yang berasal dari data pengguna di-escape via
 * {@link #esc(Object)} sebelum disematkan ke HTML. JavaScript di modal dibatasi hanya
 * pada manipulasi {@code display} element by ID, tanpa eval input pengguna.</p>
 *
 * <p><b>Threading:</b> Method query Hibernate ({@link #hitung}, {@link #kelompok})
 * membuka sesi sendiri (openSession) dan menutupnya di blok {@code finally}, sehingga
 * aman dipanggil dari thread ZK. Method lainnya tidak menyimpan state dan thread-safe.</p>
 *
 * <p><b>Pemeliharaan:</b> Kelas ini {@code final} dan tidak boleh di-extend. Penambahan
 * gaya baru cukup tambahkan ke konstanta {@code CSS}. Penambahan metode pembangun HTML
 * harus memastikan semua input di-escape via {@link #esc}. Kompatibel Java 1.7 dan
 * ZKoss 5.5 — tidak menggunakan lambda, stream, atau API Java 8+.</p>
 */
public final class ProfileUiHelper {

	/**
	 * Konstruktor privat untuk mencegah instansiasi class utility statik.
	 *
	 * <p>Semua method di class ini bersifat statik. Konstruktor privat memaksa
	 * penggunaan via method statik dan mencegah pembuatan instance.</p>
	 */
	private ProfileUiHelper() {
	}

	/**
	 * Penanda per-thread untuk menyembunyikan kartu Prestasi Pegawai dari
	 * {@link #appendPrestasiRingkas(Rows, boolean, boolean, boolean)}.
	 *
	 * <p>Dipakai oleh profil gabungan (PT + Sekolah): kedua metode {@code lanjut}
	 * memanggil {@code appendPrestasiRingkas} dengan {@code pegawai=true}. Tanpa
	 * flag ini, kartu pegawai akan tampil dua kali. Profil gabungan menyalakan
	 * flag sebelum memanggil kedua {@code lanjut}, lalu menambahkan kartu pegawai
	 * sekali secara eksplisit setelah flag dimatikan di blok {@code finally}.</p>
	 */
	private static final ThreadLocal<Boolean> sembunyikanPegawai = new ThreadLocal<Boolean>();

	/**
	 * Menyalakan atau mematikan flag {@code sembunyikanPegawai} untuk thread saat ini.
	 *
	 * <p><b>Tujuan:</b> Mengontrol apakah blok {@code if (pegawai)} di
	 * {@link #appendPrestasiRingkas(Rows, boolean, boolean, boolean)} akan merender
	 * kartu "Prestasi Pegawai". Saat {@code true}, kartu pegawai dilewati meskipun
	 * argumen {@code pegawai} bernilai {@code true}.</p>
	 *
	 * <p><b>Penggunaan pola:</b>
	 * <pre>
	 *   ProfileUiHelper.setSembunyikanPegawai(true);
	 *   try {
	 *       ProfileAdminSekolah.lanjut(rows, tbmuser, chart);
	 *       ProfileAdminPerguruanTinggi.lanjut(rows, tbmuser, chart);
	 *   } finally {
	 *       ProfileUiHelper.setSembunyikanPegawai(false);
	 *   }
	 *   ProfileUiHelper.appendPrestasiRingkas(rows, false, false, true); // sekali saja
	 * </pre>
	 * </p>
	 *
	 * @param nilai {@code true} untuk menyembunyikan kartu pegawai dari {@code appendPrestasiRingkas};
	 *              {@code false} untuk menampilkan seperti biasa
	 */
	public static void setSembunyikanPegawai(boolean nilai) {
		sembunyikanPegawai.set(Boolean.valueOf(nilai));
	}

	/**
	 * Mengembalikan teks sapaan berdasarkan jam saat ini (Pagi/Siang/Sore/Malam).
	 *
	 * <p><b>Tujuan:</b> Menyediakan sapaan yang personal dan kontekstual untuk
	 * halaman profil, mis. "Hai, Selamat Pagi" atau "Hai, Selamat Malam".</p>
	 *
	 * <p><b>Cara kerja:</b> Mengambil jam via {@code Calendar.getInstance().HOUR_OF_DAY}
	 * dan menentukan kategori:
	 * <ul>
	 *   <li>0–9: Pagi</li>
	 *   <li>10–14: Siang</li>
	 *   <li>15–17: Sore</li>
	 *   <li>18–23 dan 0 (midnight): Malam</li>
	 * </ul>
	 * </p>
	 *
	 * @return string sapaan ({@code "Pagi"}, {@code "Siang"}, {@code "Sore"}, atau {@code "Malam"})
	 */
	public static String waktuSapaan() {
		int jam = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
		if (jam >= 10 && jam < 15) {
			return "Siang";
		}
		if (jam >= 15 && jam < 18) {
			return "Sore";
		}
		if (jam >= 18 || jam == 0) {
			return "Malam";
		}
		return "Pagi";
	}

	/**
	 * Mengambil nilai integer dari item Combobox ZK yang terpilih, dengan fallback.
	 *
	 * <p><b>Tujuan:</b> Membantu membaca nilai pilihan combobox (mis. semester Ganjil/Genap
	 * yang disimpan sebagai Integer 1/2) tanpa khawatir NullPointerException atau tipe data
	 * yang tidak konsisten ({@code Integer} vs {@code Long} vs {@code String}).</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Jika {@code combobox} null, langsung kembalikan {@code defaultValue}.</li>
	 *   <li>Mengambil {@code Comboitem} terpilih; jika null → kembalikan default.</li>
	 *   <li>Jika nilai item bertipe {@code Integer}, kembalikan langsung.</li>
	 *   <li>Jika {@code Number}, konversi via {@code intValue()}.</li>
	 *   <li>Fallback: parse {@code String.valueOf(value)} sebagai Integer; jika gagal →
	 *       kembalikan default.</li>
	 * </ol>
	 * </p>
	 *
	 * @param combobox     Combobox ZK yang akan dibaca; boleh {@code null}
	 * @param defaultValue nilai default jika combobox null atau item tidak terpilih
	 * @return nilai integer dari item terpilih, atau {@code defaultValue}
	 */
	public static Integer selectedInteger(Combobox combobox, int defaultValue) {
		if (combobox == null) {
			return Integer.valueOf(defaultValue);
		}
		Comboitem selected = combobox.getSelectedItem();
		Object value = selected == null ? null : selected.getValue();
		if (value instanceof Integer) {
			return (Integer) value;
		}
		if (value instanceof Number) {
			return Integer.valueOf(((Number) value).intValue());
		}
		try {
			return value == null ? Integer.valueOf(defaultValue) : Integer.valueOf(String.valueOf(value));
		} catch (Exception e) {
			return Integer.valueOf(defaultValue);
		}
	}

	/**
	 * Menutup sesi Hibernate yang dibuka via {@code openSession()} dengan aman.
	 *
	 * <p><b>Tujuan:</b> Pembersih standar untuk sesi yang dibuka secara mandiri
	 * (bukan {@code currentSession()}). Dipanggil di blok {@code finally} pada
	 * {@link #hitung(Class, Criterion[])} dan {@link #kelompok(Class, String, Criterion[], int)}.</p>
	 *
	 * <p><b>Cara kerja:</b> Memanggil {@code session.clear()} terlebih dahulu untuk
	 * melepaskan seluruh entitas dari level-1 cache sebelum {@code session.close()}.
	 * Jika salah satu operasi gagal, error ditampilkan ke admin via
	 * {@code Common.tampilErrorJikaAdmin} dan eksekusi lanjut ke langkah berikutnya
	 * (tidak melempar ulang exception).</p>
	 *
	 * <p><b>Perhatian:</b> Jangan gunakan untuk menutup {@code currentSession()} —
	 * sesi yang dikelola oleh HibernateUtil (managed session) tidak boleh ditutup
	 * secara manual. Method ini khusus untuk {@code openSession()}.</p>
	 *
	 * @param session sesi Hibernate yang akan ditutup; jika {@code null} method langsung return
	 */
	public static void closeOpenSession(Session session) {
		if (session == null) {
			return;
		}
		try {
			session.clear();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		try {
			session.close();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Memeriksa apakah string tidak kosong setelah trim.
	 *
	 * <p>Mengembalikan {@code true} hanya jika {@code value} tidak {@code null}
	 * dan {@code value.trim()} tidak kosong. Dipakai sebagai penjaga sebelum
	 * menyematkan nilai ke HTML agar tidak ada tag kosong atau attribut blank.</p>
	 *
	 * @param value string yang diperiksa; boleh {@code null}
	 * @return {@code true} jika string berisi karakter non-whitespace
	 */
	public static boolean notBlank(String value) {
		return value != null && !value.trim().isEmpty();
	}

	/**
	 * Meng-escape string agar aman digunakan sebagai literal string JavaScript.
	 *
	 * <p><b>Tujuan:</b> Mencegah injeksi JavaScript saat nilai data disisipkan ke
	 * dalam atribut onclick atau argumen fungsi JS di template HTML profil.</p>
	 *
	 * <p><b>Cara kerja:</b> Mengubah: {@code \} → {@code \\}, {@code '} → {@code \'},
	 * {@code "} → {@code \"}, {@code \r} dan {@code \n} → spasi (mencegah pemecahan
	 * baris dalam string literal JS).</p>
	 *
	 * @param value nilai yang akan di-escape; {@code null} diperlakukan sebagai string kosong
	 * @return string yang aman untuk disisipkan dalam string literal JavaScript
	 */
	public static String js(Object value) {
		String s = value == null ? "" : String.valueOf(value);
		s = s.replace("\\", "\\\\");
		s = s.replace("'", "\\'");
		s = s.replace("\"", "\\\"");
		s = s.replace("\r", " ");
		s = s.replace("\n", " ");
		return s;
	}

	private static final String CSS = "<style type=\"text/css\">"
			+ ".ais-profile-modern{font-family:Arial,Helvetica,sans-serif;color:#0f172a;}"
			+ ".ais-profile-card{background:#fff;border:1px solid #dbeafe;border-radius:16px;box-shadow:0 12px 30px rgba(15,23,42,.08);margin:10px 0 14px 0;overflow:hidden;}"
			+ ".ais-profile-head{padding:14px 16px;background:linear-gradient(135deg,#e0f2fe 0%,#f8fafc 52%,#ecfeff 100%);border-bottom:1px solid #dbeafe;border-left:4px solid #2563eb;}"
			+ ".ais-profile-title{font-weight:900;font-size:15px;color:#0f172a;line-height:1.35;letter-spacing:-.01em;}"
			+ ".ais-profile-desc{font-size:11.5px;color:#475569;line-height:1.55;margin-top:4px;}"
			+ ".ais-profile-grid{display:flex;flex-wrap:wrap;gap:10px;padding:12px;}"
			+ ".ais-profile-stat{flex:1 1 115px;min-width:105px;background:linear-gradient(180deg,#ffffff 0%,#f8fafc 100%);border:1px solid #dbeafe;border-radius:14px;padding:11px;box-shadow:0 6px 16px rgba(15,23,42,.05);}"
			+ ".ais-profile-stat .label{font-size:10px;color:#64748b;text-transform:uppercase;letter-spacing:.04em;font-weight:700;}"
			+ ".ais-profile-stat .value{font-size:18px;color:#0f172a;font-weight:800;margin-top:4px;}"
			+ ".ais-profile-stat .hint{font-size:10.5px;color:#64748b;margin-top:4px;line-height:1.35;}"
			+ ".ais-profile-chart-wrap{padding:12px;}"
			+ ".ais-profile-trend{display:flex;align-items:flex-end;gap:8px;min-height:168px;padding:14px 8px 22px 8px;border:1px solid #e2e8f0;border-radius:12px;background:repeating-linear-gradient(to top,#fff 0,#fff 32px,#f1f5f9 33px);overflow-x:auto;}"
			+ ".ais-profile-bar-col{min-width:46px;text-align:center;font-size:10px;color:#64748b;}"
			+ ".ais-profile-bars{height:130px;display:flex;align-items:flex-end;justify-content:center;gap:3px;}"
			+ ".ais-profile-bar{width:9px;border-radius:8px 8px 2px 2px;display:inline-block;box-shadow:0 2px 6px rgba(15,23,42,.10);}"
			+ ".ais-profile-bar.sks{background:linear-gradient(180deg, var(--ais-theme-accent,#38bdf8), var(--ais-theme-primary,#2563eb));}"
			+ ".ais-profile-bar.kum{background:linear-gradient(180deg,#34d399,#059669);}"
			+ ".ais-profile-bar.ips{background:linear-gradient(180deg,#fbbf24,#f97316);}"
			+ ".ais-profile-bar.ipk{background:linear-gradient(180deg,#a78bfa,#7c3aed);}"
			+ ".ais-profile-legend{display:flex;flex-wrap:wrap;gap:8px;margin-top:10px;font-size:10.5px;color:#475569;}"
			+ ".ais-profile-dot{display:inline-block;width:9px;height:9px;border-radius:50%;margin-right:4px;vertical-align:middle;}"
			+ ".ais-profile-radar{padding:12px;text-align:center;}"
			+ ".ais-profile-table{width:100%;border-collapse:collapse;font-size:11px;}"
			+ ".ais-profile-table th{background:#eff6ff;color:#1e293b;text-align:left;padding:8px;border-bottom:1px solid #dbeafe;font-weight:800;}"
			+ ".ais-profile-table td{padding:8px;border-bottom:1px solid #f1f5f9;color:#334155;vertical-align:top;}"
			+ ".ais-profile-badge{display:inline-block;border-radius:999px;padding:2px 7px;font-size:10px;font-weight:700;background:#eef2ff;color:#3730a3;}"
			+ ".ais-profile-loading{padding:18px;text-align:center;color:#475569;background:#fff;border:1px solid #e2e8f0;border-radius:12px;margin:8px 0;}"
			+ ".ais-profile-spinner{display:inline-block;width:14px;height:14px;border:2px solid #cbd5e1;border-top-color:#2563eb;border-radius:50%;animation:aisSpin .8s linear infinite;margin-right:6px;vertical-align:-2px;}"
			+ ".ais-profile-card:hover{box-shadow:0 14px 34px rgba(15,23,42,.10);}"
			+ ".ais-profile-table tr:nth-child(even) td{background:#fbfdff;}"
			+ ".ais-profile-cols{display:flex;flex-wrap:wrap;gap:12px;align-items:flex-start;}"
			+ ".ais-profile-cols > div{flex:1 1 300px;min-width:260px;}"
			+ ".ais-profile-donut{width:86px;height:86px;border-radius:50%;margin:8px auto 4px;position:relative;background:#e2e8f0;}"
			+ ".ais-profile-donut i{position:absolute;left:13px;top:13px;width:60px;height:60px;border-radius:50%;background:#fff;font-style:normal;line-height:60px;text-align:center;font-weight:800;font-size:13px;color:#0f172a;}"
			+ ".ais-profile-hbar{display:flex;gap:8px;align-items:center;font-size:11px;color:#475569;margin:7px 0;}"
			+ ".ais-profile-hbar .nm{flex:0 0 110px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;}"
			+ ".ais-profile-hbar .bg{flex:1 1 auto;height:12px;border-radius:999px;background:#e2e8f0;overflow:hidden;}"
			+ ".ais-profile-hbar .bg b{display:block;height:12px;border-radius:999px;background:linear-gradient(90deg,#60a5fa,#2563eb);}"
			+ ".ais-profile-hbar .vl{flex:0 0 44px;text-align:right;font-weight:700;color:#334155;}"
			+ "@media screen and (max-width:360px){.ais-profile-grid{display:block;padding:10px}.ais-profile-stat{margin-bottom:8px;min-width:0}.ais-profile-head{padding:12px}.ais-profile-title{font-size:14px}.ais-profile-desc{font-size:11px}.ais-profile-hbar .nm{flex-basis:84px}}"
			// ── Animasi tampil pertama + polish (transisi halus saat dirender) ──
			+ ".ais-profile-card{animation:aisProfileIn .55s cubic-bezier(.21,.61,.36,1) both;transition:box-shadow .25s ease,transform .25s ease;}"
			+ ".ais-profile-card:hover{transform:translateY(-2px);}"
			+ ".ais-profile-head{position:relative;overflow:hidden;}"
			+ ".ais-profile-head:after{content:'';position:absolute;top:0;right:-30%;width:55%;height:100%;background:linear-gradient(120deg,rgba(255,255,255,0) 0%,rgba(255,255,255,.45) 50%,rgba(255,255,255,0) 100%);transform:skewX(-18deg);animation:aisShine 2.4s ease 1;}"
			+ ".ais-profile-stat{animation:aisStatPop .5s ease both;transition:transform .18s ease,box-shadow .18s ease;}"
			+ ".ais-profile-stat:hover{transform:translateY(-3px);box-shadow:0 12px 24px rgba(15,23,42,.12);}"
			+ ".ais-profile-stat .value{transition:color .2s ease;}"
			+ ".ais-profile-prestasi .ais-profile-stat .value{color:#7c3aed;}"
			+ ".ais-profile-prestasi .ais-profile-head{border-left-color:#7c3aed;background:linear-gradient(135deg,#f5f3ff 0%,#fafaff 55%,#eef2ff 100%);}"
			+ ".ais-profile-bar{transition:filter .2s ease;}"
			+ ".ais-profile-hbar .bg b{transition:width .6s cubic-bezier(.21,.61,.36,1);}"
			+ "@keyframes aisProfileIn{from{opacity:0;transform:translateY(14px) scale(.99)}to{opacity:1;transform:none}}"
			+ "@keyframes aisStatPop{0%{opacity:0;transform:translateY(8px)}100%{opacity:1;transform:none}}"
			+ "@keyframes aisShine{0%{left:-60%}100%{left:130%}}"
			+ "@keyframes aisSpin{to{transform:rotate(360deg)}}"
			// ── Kartu angka yang bisa diklik + popup rincian ──
			+ ".ais-profile-stat-click{cursor:pointer;}"
			+ ".ais-profile-stat-click:hover{border-color:#2563eb;box-shadow:0 12px 26px rgba(15,23,42,.16);}"
			+ ".ais-profile-click-hint{font-size:9px;color:#2563eb;font-weight:800;margin-top:5px;}"
			+ ".ais-profile-modal{display:none;position:fixed;left:0;top:0;right:0;bottom:0;background:rgba(15,23,42,.55);z-index:99999;align-items:center;justify-content:center;padding:16px;box-sizing:border-box;}"
			+ ".ais-profile-modal-box{background:#fff;border-radius:16px;max-width:540px;width:100%;max-height:82vh;overflow:auto;box-shadow:0 24px 64px rgba(0,0,0,.32);}"
			+ ".ais-profile-modal-head{padding:14px 16px;font-weight:900;font-size:15px;color:#0f172a;border-bottom:1px solid #dbeafe;display:flex;justify-content:space-between;align-items:center;gap:10px;background:linear-gradient(135deg,#e0f2fe,#f8fafc);}"
			+ ".ais-profile-modal-x{cursor:pointer;font-size:22px;line-height:1;color:#64748b;padding:0 4px;}"
			+ ".ais-profile-modal-x:hover{color:#0f172a;}"
			+ ".ais-profile-modal-body{padding:15px 16px;font-size:12px;color:#334155;line-height:1.6;}"
			+ ".ais-profile-modal-body p{margin:0 0 9px 0;}"
			+ ".ais-profile-mtbl{width:100%;border-collapse:collapse;font-size:12px;margin:6px 0;}"
			+ ".ais-profile-mtbl th,.ais-profile-mtbl td{border:1px solid #e2e8f0;padding:6px 9px;text-align:left;}"
			+ ".ais-profile-mtbl thead th{background:#eff6ff;color:#0f172a;font-weight:800;}"
			+ ".ais-profile-mtbl td:last-child,.ais-profile-mtbl th:last-child{text-align:right;width:96px;font-weight:700;white-space:nowrap;}"
			+ ".ais-profile-mtbl tfoot td{background:#f8fafc;font-weight:800;}"
			+ "</style>";

	/**
	 * Menyiapkan area isi yang aman untuk konten profil, menangani kasus ZK LayoutRegion.
	 *
	 * <p><b>Tujuan:</b> ZK {@code LayoutRegion} (East/North/Center/West) hanya boleh
	 * memiliki SATU child komponen. Method ini memastikan CSS dan konten profil selalu
	 * dimasukkan ke dalam satu {@code Div} pembungkus saat parent adalah {@code LayoutRegion},
	 * sedangkan untuk parent biasa (Div, Vbox, dll.) CSS langsung ditambahkan ke parent.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Membersihkan seluruh child komponen yang ada di {@code parent} via
	 *       {@code Common.clear(parent)}; fallback ke {@code parent.getChildren().clear()}.</li>
	 *   <li>Jika parent adalah {@code LayoutRegion}: membuang sisa child via detach
	 *       manual, membuat {@code Div} baru (full width/height, overflow auto, background
	 *       gradient), menambahkan CSS ke Div tersebut, dan mengembalikannya sebagai
	 *       wrapper aman.</li>
	 *   <li>Jika parent bukan {@code LayoutRegion}: menambahkan CSS langsung ke parent
	 *       dan mengembalikan parent itu sendiri.</li>
	 * </ol>
	 * </p>
	 *
	 * @param parent komponen ZK tujuan; jika {@code null} mengembalikan {@code null}
	 * @return komponen pembungkus yang siap menerima child konten profil;
	 *         {@code Div} baru jika parent adalah {@code LayoutRegion}, atau {@code parent} itu sendiri
	 */
	public static Component prepareContentParent(Component parent) {
		if (parent == null) {
			return null;
		}
		try {
			Common.clear(parent);
		} catch (Exception e) {
			try {
				parent.getChildren().clear();
			} catch (Exception ex) {
				Common.tampilErrorJikaAdmin(ex);
			}
		}

		if (parent instanceof LayoutRegion) {
			try {
				while (parent.getFirstChild() != null) {
					parent.getFirstChild().detach();
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
			Div content = new Div();
			content.setWidth("100%");
			content.setHeight("100%");
			content.setStyle("overflow:auto;box-sizing:border-box;padding:8px;background:linear-gradient(180deg,#f8fafc 0%,#eef6ff 100%);");
			content.setParent(parent);
			appendCss(content);
			return content;
		}

		appendCss(parent);
		return parent;
	}

	/**
	 * Menyuntikkan blok CSS profil ke dalam komponen ZK yang diberikan.
	 *
	 * <p>Membuat elemen {@code Html} berisi konstanta {@code CSS} dan menambahkannya
	 * sebagai child dari {@code parent}. Dipanggil oleh {@link #prepareContentParent}
	 * dan dapat juga dipanggil langsung jika CSS belum ada di parent.</p>
	 *
	 * <p>Tidak ada efek samping jika {@code parent} null.</p>
	 *
	 * @param parent komponen ZK tujuan penyuntikan CSS; boleh {@code null}
	 */
	public static void appendCss(Component parent) {
		if (parent != null && parent.getPage() != null) {
			parent.appendChild(new Html(CSS));
		}
	}

	/**
	 * Menambahkan baris judul panel info (HTML kartu header) ke dalam grid baris.
	 *
	 * <p><b>Tujuan:</b> Menyisipkan judul dan deskripsi panel sebelum baris data
	 * dalam grid profil. Kartu ini memiliki desain yang sama dengan kartu angka
	 * ({@link #panel}), tapi tanpa body — hanya header gradien biru.</p>
	 *
	 * <p><b>Cara kerja:</b> Membuat baris {@code MyRowStyled} baru dengan spans sesuai
	 * parameter, memasangnya ke {@code rows}, lalu menambahkan {@code Html} berisi
	 * {@code CSS + infoBanner(title, description)}. CSS disertakan di sini agar kartu
	 * tetap berfungsi bahkan jika di-render sebelum {@code prepareContentParent} dipanggil.</p>
	 *
	 * @param rows        baris ZK tujuan; jika {@code null} method langsung return
	 * @param spans       jumlah kolom yang digabung (mis. 2 untuk grid 2-kolom)
	 * @param title       judul panel; di-escape via {@link #esc}
	 * @param description deskripsi singkat panel; di-escape via {@link #esc}
	 */
	public static void appendPanelInfoRow(Rows rows, int spans, String title, String description) {
		if (rows == null) {
			return;
		}
		Row row = new MyRowStyled();
		row.setSpans(String.valueOf(spans));
		row.setParent(rows);
		// Judul & deskripsi banner = teks STATIS → terjemahkan mengikuti bahasa aktif.
		row.appendChild(new Html(CSS + infoBanner(ais.common.Common.getBahasaConfig(title),
				ais.common.Common.getBahasaConfig(description))));
	}

	/**
	 * Menambahkan kartu "Ringkasan Prestasi" ke dalam baris grid profil, sesuai cakupan admin.
	 *
	 * <p><b>Tujuan:</b> Menyajikan ringkasan kuantitatif prestasi sivitas akademika
	 * sesuai jenis admin yang sedang login. Admin sekolah mendapat kartu siswa + guru;
	 * admin PT mendapat kartu mahasiswa + dosen; semua mendapat kartu pegawai jika tidak
	 * disembunyikan oleh flag {@code sembunyikanPegawai}.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Jika {@code sekolah=true}: menghitung PrestasiSiswa dan PrestasiGuru via
	 *       {@link #hitung}, lalu memanggil {@link #appendKartuPrestasi} dengan aksen ungu.</li>
	 *   <li>Jika {@code pt=true}: menghitung
	 *       KegiatanKemahasiswaanPunyaMahasiswa, OrganisasiIntraKampusPunyaMahasiswa,
	 *       PrestasiMahasiswa, PenghargaanMahasiswa, CatatanMahasiswa untuk satu kartu mahasiswa;
	 *       dan KegiatanKedosenanPunyaDosen, OrganisasiDosenPunyaDosen, PrestasiDosen,
	 *       PenghargaanDosen untuk satu kartu dosen.</li>
	 *   <li>Jika {@code pegawai=true} dan flag {@code sembunyikanPegawai} tidak aktif:
	 *       menghitung PrestasiPegawai.</li>
	 * </ol>
	 * Setiap kartu angka diklik untuk membuka popup rincian via
	 * {@link #tambahKartuAngka(List, StringBuffer, String, long, String, String)}.
	 * Seluruh method dibungkus try-catch sehingga aman dipanggil dari konteks apapun.</p>
	 *
	 * <p><b>Anti-duplikat pegawai:</b> Jika {@code sembunyikanPegawai} ThreadLocal aktif
	 * (set via {@link #setSembunyikanPegawai(boolean)}), kartu pegawai dilewati meskipun
	 * {@code pegawai=true}. Ini mencegah duplikasi di profil gabungan PT+Sekolah.</p>
	 *
	 * @param rows    baris ZK tujuan; jika {@code null} method langsung return
	 * @param sekolah {@code true} untuk menambahkan kartu Prestasi Siswa dan Guru
	 * @param pt      {@code true} untuk menambahkan kartu Aktivitas Mahasiswa dan Dosen
	 * @param pegawai {@code true} untuk menambahkan kartu Prestasi Pegawai (kecuali flag aktif)
	 */
	public static void appendPrestasiRingkas(Rows rows, boolean sekolah, boolean pt, boolean pegawai) {
		if (rows == null) {
			return;
		}
		try {
			if (sekolah) {
				List<String> s = new ArrayList<String>();
				StringBuffer mod = new StringBuffer();
				tambahKartuAngka(s, mod, "Prestasi Siswa",
						hitung(ais.database.model.sekolah.PrestasiSiswa.class, null),
						"Total prestasi siswa tercatat", "Daftar rinci per individu ada di menu Prestasi (Siswa).");
				tambahKartuAngka(s, mod, "Prestasi Guru",
						hitung(ais.database.model.sekolah.PrestasiGuru.class, null),
						"Total prestasi guru tercatat", "Daftar rinci per individu ada di menu Prestasi (Guru).");
				appendKartuPrestasi(rows, "Ringkasan Prestasi Sekolah", "Rekap prestasi siswa & guru.", s,
						mod.toString());
			}
			if (pt) {
				// Mahasiswa: kegiatan, organisasi, prestasi, karya, catatan (sesuai tab modul Prestasi).
				List<String> m = new ArrayList<String>();
				StringBuffer modM = new StringBuffer();
				tambahKartuAngka(m, modM, "Kegiatan",
						hitung(ais.database.model.KegiatanKemahasiswaanPunyaMahasiswa.class, null),
						"Kegiatan kemahasiswaan", "Daftar rinci ada di menu Kemahasiswaan / Prestasi Mahasiswa.");
				tambahKartuAngka(m, modM, "Organisasi",
						hitung(ais.database.model.OrganisasiIntraKampusPunyaMahasiswa.class, null),
						"Keanggotaan organisasi", "Daftar rinci ada di menu Kemahasiswaan / Prestasi Mahasiswa.");
				tambahKartuAngka(m, modM, "Prestasi", hitung(ais.database.model.PrestasiMahasiswa.class, null),
						"Prestasi mahasiswa", "Daftar rinci ada di menu Prestasi Mahasiswa.");
				tambahKartuAngka(m, modM, "Karya", hitung(ais.database.model.PenghargaanMahasiswa.class, null),
						"Karya & penghargaan", "Daftar rinci ada di menu Prestasi Mahasiswa.");
				tambahKartuAngka(m, modM, "Catatan", hitung(ais.database.model.CatatanMahasiswa.class, null),
						"Catatan pembinaan", "Daftar rinci ada di menu Mahasiswa (Catatan).");
				appendKartuPrestasi(rows, "Aktivitas & Prestasi Mahasiswa",
						"Rekap kegiatan, organisasi, prestasi, karya, dan catatan mahasiswa.", m, modM.toString());

				// Dosen: kegiatan, organisasi, prestasi, karya.
				List<String> d = new ArrayList<String>();
				StringBuffer modD = new StringBuffer();
				tambahKartuAngka(d, modD, "Kegiatan", hitung(ais.database.model.KegiatanKedosenanPunyaDosen.class, null),
						"Kegiatan kedosenan", "Daftar rinci ada di menu Prestasi Dosen.");
				tambahKartuAngka(d, modD, "Organisasi", hitung(ais.database.model.OrganisasiDosenPunyaDosen.class, null),
						"Keanggotaan organisasi", "Daftar rinci ada di menu Prestasi Dosen.");
				tambahKartuAngka(d, modD, "Prestasi", hitung(ais.database.model.PrestasiDosen.class, null),
						"Prestasi dosen", "Daftar rinci ada di menu Prestasi Dosen.");
				tambahKartuAngka(d, modD, "Karya", hitung(ais.database.model.PenghargaanDosen.class, null),
						"Karya & penghargaan", "Daftar rinci ada di menu Prestasi Dosen.");
				appendKartuPrestasi(rows, "Aktivitas & Prestasi Dosen",
						"Rekap kegiatan, organisasi, prestasi, dan karya dosen.", d, modD.toString());
			}
			if (pegawai && !Boolean.TRUE.equals(sembunyikanPegawai.get())) {
				List<String> p = new ArrayList<String>();
				StringBuffer mod = new StringBuffer();
				tambahKartuAngka(p, mod, "Prestasi Pegawai", hitung(ais.database.model.PrestasiPegawai.class, null),
						"Total prestasi pegawai tercatat", "Daftar rinci ada di menu Prestasi Pegawai.");
				appendKartuPrestasi(rows, "Ringkasan Prestasi Pegawai", "Rekap prestasi pegawai.", p, mod.toString());
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Merender satu kartu prestasi bergaya ungu ke dalam baris grid.
	 *
	 * <p><b>Tujuan:</b> Pembangun kartu HTML yang dipakai oleh
	 * {@link #appendPrestasiRingkas(Rows, boolean, boolean, boolean)} untuk menghasilkan
	 * kartu prestasi dengan aksen warna ungu (berbeda dari kartu angka akademik yang biru),
	 * berisi deretan stat angka clickable dan popup rincian.</p>
	 *
	 * <p><b>Cara kerja:</b> Membungkus {@code stats} dalam {@link #statsWrap(String[])},
	 * menyusun HTML kartu dengan class {@code ais-profile-prestasi} (aksen ungu dari CSS),
	 * dan menambahkan {@code modalsHtml} (string HTML popup tersembunyi). Baris baru
	 * dibuat dengan kolspan 2 dan ditambahkan ke {@code rows}.</p>
	 *
	 * <p><b>Guard:</b> Jika {@code rows} null atau {@code stats} null/kosong, method
	 * langsung return tanpa membuat baris apapun (aman dipanggil dengan data kosong).</p>
	 *
	 * @param rows      baris ZK tujuan; tidak boleh {@code null}
	 * @param title     judul kartu prestasi; di-escape via {@link #esc}
	 * @param desc      deskripsi singkat; di-escape via {@link #esc}
	 * @param stats     daftar HTML string kartu angka (dari {@link #statClickable});
	 *                  jika kosong, method return tanpa render
	 * @param modalsHtml string HTML semua popup yang dipakai oleh kartu di {@code stats}; boleh null
	 */
	private static void appendKartuPrestasi(Rows rows, String title, String desc, List<String> stats,
			String modalsHtml) {
		if (rows == null || stats == null || stats.isEmpty()) {
			return;
		}
		String isi = statsWrap(stats.toArray(new String[stats.size()]));
		String html = "<div class=\"ais-profile-modern ais-profile-card ais-profile-prestasi\">"
				+ "<div class=\"ais-profile-head\"><div class=\"ais-profile-title\">" + esc(title) + "</div>"
				+ "<div class=\"ais-profile-desc\">" + esc(desc) + "</div></div>" + isi + "</div>"
				+ (modalsHtml == null ? "" : modalsHtml);
		Row row = new MyRowStyled();
		try {
			ais.ui.util.ZkCompat.setSpans(row, "2");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileUiHelper.java:586");
		}
		row.setParent(rows);
		row.appendChild(new Html(html));
	}

	/**
	 * Membangun HTML kartu header tanpa body (hanya judul dan deskripsi).
	 *
	 * <p>Menghasilkan elemen {@code div.ais-profile-card} dengan head yang berisi judul
	 * dan deskripsi, tanpa area body. Dipakai untuk memperkenalkan sebuah seksi atau
	 * panel sebelum komponen ZK lainnya.</p>
	 *
	 * @param title       judul yang ditampilkan di header; di-escape via {@link #esc}
	 * @param description teks pendek di bawah judul; di-escape via {@link #esc}
	 * @return string HTML kartu header
	 */
	public static String infoBanner(String title, String description) {
		return "<div class=\"ais-profile-modern ais-profile-card\"><div class=\"ais-profile-head\"><div class=\"ais-profile-title\">"
				+ esc(title) + "</div><div class=\"ais-profile-desc\">" + esc(description) + "</div></div></div>";
	}

	/**
	 * Membangun HTML kartu profil lengkap: header (judul + deskripsi) + body.
	 *
	 * <p>Menghasilkan {@code div.ais-profile-card} berisi header gradien biru dan
	 * body yang berisi HTML apapun yang diberikan via {@code body}. Dipakai untuk
	 * membungkus kartu statistik, infoTable, tren grafik, dan panel lainnya.</p>
	 *
	 * <p><b>Perhatian:</b> Parameter {@code body} tidak di-escape — harus sudah
	 * berupa HTML yang aman (pastikan konten pengguna di dalamnya sudah di-escape
	 * via {@link #esc} sebelum diteruskan).</p>
	 *
	 * @param title       judul kartu; di-escape via {@link #esc}
	 * @param description deskripsi pendek; di-escape via {@link #esc}
	 * @param body        isi HTML; boleh {@code null} (diabaikan)
	 * @return string HTML kartu penuh
	 */
	public static String panel(String title, String description, String body) {
		// Judul & deskripsi panel = teks STATIS → terjemahkan mengikuti bahasa aktif (getBahasaConfig).
		return "<div class=\"ais-profile-modern ais-profile-card\"><div class=\"ais-profile-head\"><div class=\"ais-profile-title\">"
				+ esc(ais.common.Common.getBahasaConfig(title)) + "</div><div class=\"ais-profile-desc\">"
				+ esc(ais.common.Common.getBahasaConfig(description))
				+ "</div></div>" + (body == null ? "" : body) + "</div>";
	}

	/**
	 * Membangun HTML kartu statistik angka yang otomatis bisa diklik untuk popup rincian.
	 *
	 * <p><b>Tujuan:</b> Pembangun kartu angka standar. Setiap kartu yang dihasilkan oleh
	 * method ini OTOMATIS bisa diklik dan membuka popup dengan nilai + keterangan, tanpa
	 * pemanggil perlu mengatur popup secara eksplisit. Berlaku untuk semua profil
	 * (Admin Sekolah, Guru, Siswa, Mahasiswa, Dosen) tanpa mengubah kode pemanggil.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menghasilkan ID modal unik via {@link #nextModalId(String)}.</li>
	 *   <li>Membangun HTML kartu dengan onclick yang memunculkan popup tersebut.</li>
	 *   <li>Membangun HTML popup via {@link #modal(String, String, String)}
	 *       berisi label, nilai, dan keterangan.</li>
	 *   <li>Menggabungkan kartu + popup dan mengembalikan satu string HTML.</li>
	 * </ol>
	 * Popup menggunakan {@code position:fixed} sehingga tidak mengganggu tata letak
	 * grid kartu.</p>
	 *
	 * <p>Jika pemanggil ingin popup dengan konten kustom (bukan auto-popup), gunakan
	 * {@link #statClickable(String, Object, String, String)} dengan ID modal yang sudah
	 * dibuat sebelumnya dan popup dibangun secara terpisah.</p>
	 *
	 * @param label nama metrik, mis. "Siswa" atau "Guru"; di-escape via {@link #esc}
	 * @param value nilai yang ditampilkan; di-escape via {@link #esc}
	 * @param hint  keterangan singkat di bawah nilai; di-escape via {@link #esc}
	 * @return string HTML kartu angka + popup tersembunyi (siap diembed ke {@code statsWrap})
	 */
	public static String stat(String label, Object value, String hint) {
		String mid = nextModalId("aisStat");
		// Label & hint kartu = teks STATIS → terjemahkan; nilai (value) TETAP (data dinamis).
		label = ais.common.Common.getBahasaConfig(label);
		hint = ais.common.Common.getBahasaConfig(hint);
		String card = "<div class=\"ais-profile-stat ais-profile-stat-click\" onclick=\"var m=document.getElementById('"
				+ mid + "');if(m){m.style.display='flex';}\"><div class=\"label\">" + esc(label)
				+ "</div><div class=\"value\">" + esc(value) + "</div><div class=\"hint\">" + esc(hint)
				+ "</div><div class=\"ais-profile-click-hint\">&#128712; " + ais.common.Common.getBahasaConfig("Klik untuk rincian") + "</div></div>";
		String body = "<p><b>" + esc(label) + "</b>" + (notBlank(hint) ? " &mdash; " + esc(hint) : "") + ".</p>"
				+ "<p style=\"font-size:15px;\">Nilai: <b style=\"color:#2563eb;\">" + esc(value) + "</b></p>";
		return card + modal(mid, ais.common.Common.getBahasaConfig("Rincian") + " " + label, body);
	}

	/**
	 * Membangun HTML kartu statistik angka versi polos (tidak bisa diklik, tanpa popup).
	 *
	 * <p><b>Tujuan:</b> Digunakan saat kartu angka tidak perlu interaktif — misalnya
	 * dalam konteks tampilan ringkas di layar kecil atau panel yang tidak memerlukan
	 * detail tambahan. Berbeda dengan {@link #stat(String, Object, String)} yang selalu
	 * menghasilkan kartu clickable + popup.</p>
	 *
	 * @param label nama metrik; di-escape via {@link #esc}
	 * @param value nilai yang ditampilkan; di-escape via {@link #esc}
	 * @param hint  keterangan singkat; di-escape via {@link #esc}
	 * @return string HTML kartu angka non-interaktif
	 */
	public static String statPolos(String label, Object value, String hint) {
		return "<div class=\"ais-profile-stat\"><div class=\"label\">" + esc(label) + "</div><div class=\"value\">"
				+ esc(value) + "</div><div class=\"hint\">" + esc(hint) + "</div></div>";
	}

	/**
	 * Menampilkan placeholder "sedang memuat" di dalam area konten profil.
	 *
	 * <p><b>Tujuan:</b> Digunakan saat konten panel profil belum selesai dimuat
	 * (mis. saat data diambil secara asinkron atau sesi belum diinisialisasi).
	 * Menampilkan kartu dengan spinner animasi, judul, dan deskripsi.</p>
	 *
	 * <p><b>Cara kerja:</b> Memanggil {@link #prepareContentParent(Component)} untuk
	 * mendapatkan wrapper aman, kemudian menambahkan {@code Html} berisi HTML loading
	 * (div {@code ais-profile-loading} + spinner {@code ais-profile-spinner}).</p>
	 *
	 * @param parent      komponen ZK tujuan; jika {@code null} method langsung return
	 * @param title       teks judul yang ditampilkan dalam placeholder loading (di-bold)
	 * @param description teks keterangan di bawah judul (font kecil)
	 */
	public static void showLoading(Component parent, String title, String description) {
		Component contentParent = prepareContentParent(parent);
		if (contentParent == null) {
			return;
		}
		contentParent.appendChild(new Html("<div class=\"ais-profile-modern ais-profile-loading\"><span class=\"ais-profile-spinner\"></span><b>"
				+ esc(title) + "</b><br/><span style=\"font-size:11px;\">" + esc(description) + "</span></div>"));
	}

	/**
	 * Membangun grafik batang tren akademik per semester dalam format HTML/CSS.
	 *
	 * <p><b>Tujuan:</b> Menyajikan visualisasi perkembangan studi mahasiswa per semester
	 * dalam bentuk batang warna-warni: SKS Semester (biru), SKS Kumulatif (hijau),
	 * IPS (oranye), dan IPK (ungu). Grafik ini dirender di panel profil mahasiswa
	 * ({@link ProfileMahasiswa#initDashboard}).</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Mencari nilai maksimum SKS dari semua semester untuk normalisasi tinggi batang.</li>
	 *   <li>Per semester: menghitung tinggi relatif 4 batang (SKS, SKS kum, IPS, IPK)
	 *       dengan minimum 4px agar batang tidak menghilang.</li>
	 *   <li>IPS/IPK dinormalisasi terhadap skala 4.0; SKS terhadap maxSks.</li>
	 *   <li>Menambahkan tooltip {@code title} HTML pada setiap batang untuk nilai eksak.</li>
	 *   <li>Menambahkan legenda warna di bawah grafik.</li>
	 * </ol>
	 * </p>
	 *
	 * <p>Jika {@code trendData} null atau kosong, mengembalikan div placeholder teks.</p>
	 *
	 * <p><b>Format map per elemen:</b> Kunci yang dibutuhkan: {@code "sks"} (SKS semester),
	 * {@code "sksk"} (SKS kumulatif), {@code "ips"}, {@code "ipk"}, {@code "smt"}
	 * (label semester, mis. "1/2023"). Nilai yang tidak terbaca sebagai angka dianggap 0.</p>
	 *
	 * @param trendData daftar data per semester (urutan dari semester pertama ke terakhir);
	 *                  boleh {@code null} atau kosong
	 * @return string HTML grafik batang; tidak pernah {@code null}
	 */
	public static String buildTrendBars(List<Map<String, Object>> trendData) {
		if (trendData == null || trendData.isEmpty()) {
			return "<div style=\"padding:14px;color:#64748b;font-size:11px;\">Belum ada data semester yang dapat divisualisasikan.</div>";
		}
		double maxSks = 1.0;
		int i;
		for (i = 0; i < trendData.size(); i++) {
			Map<String, Object> tr = trendData.get(i);
			maxSks = Math.max(maxSks, asDouble(tr.get("sks")));
			maxSks = Math.max(maxSks, asDouble(tr.get("sksk")));
		}

		StringBuffer sb = new StringBuffer();
		sb.append("<div class=\"ais-profile-chart-wrap\"><div class=\"ais-profile-trend\">");
		for (i = 0; i < trendData.size(); i++) {
			Map<String, Object> tr = trendData.get(i);
			double sks = asDouble(tr.get("sks"));
			double sksk = asDouble(tr.get("sksk"));
			double ips = asDouble(tr.get("ips"));
			double ipk = asDouble(tr.get("ipk"));
			int hSks = Math.max(4, (int) Math.round((sks / maxSks) * 120.0));
			int hKum = Math.max(4, (int) Math.round((sksk / maxSks) * 120.0));
			int hIps = Math.max(4, (int) Math.round((Math.min(4.0, ips) / 4.0) * 120.0));
			int hIpk = Math.max(4, (int) Math.round((Math.min(4.0, ipk) / 4.0) * 120.0));
			sb.append("<div class=\"ais-profile-bar-col\"><div class=\"ais-profile-bars\">");
			sb.append("<span title=\"SKS Semester: ").append(esc(fmt(sks))).append("\" class=\"ais-profile-bar sks\" style=\"height:").append(hSks).append("px\"></span>");
			sb.append("<span title=\"SKS Kumulatif: ").append(esc(fmt(sksk))).append("\" class=\"ais-profile-bar kum\" style=\"height:").append(hKum).append("px\"></span>");
			sb.append("<span title=\"IPS: ").append(esc(fmt(ips))).append("\" class=\"ais-profile-bar ips\" style=\"height:").append(hIps).append("px\"></span>");
			sb.append("<span title=\"IPK: ").append(esc(fmt(ipk))).append("\" class=\"ais-profile-bar ipk\" style=\"height:").append(hIpk).append("px\"></span>");
			sb.append("</div><div>Smt ").append(esc(tr.get("smt"))).append("</div></div>");
		}
		sb.append("</div><div class=\"ais-profile-legend\">");
		sb.append("<span><i class=\"ais-profile-dot\" style=\"background:#2563eb\"></i>SKS Semester</span>");
		sb.append("<span><i class=\"ais-profile-dot\" style=\"background:#059669\"></i>SKS Kumulatif</span>");
		sb.append("<span><i class=\"ais-profile-dot\" style=\"background:#f97316\"></i>IPS</span>");
		sb.append("<span><i class=\"ais-profile-dot\" style=\"background:#7c3aed\"></i>IPK</span>");
		sb.append("</div></div>");
		return sb.toString();
	}

	/**
	 * Membangun grafik radar (spider web) 4-sumbu akademik mahasiswa dalam format SVG.
	 *
	 * <p><b>Tujuan:</b> Menyajikan ringkasan visual kekuatan akademik mahasiswa dari
	 * empat dimensi: IPK, IPS, SKS Kumulatif, dan proporsi nilai valid. Grafik ini
	 * dirender di panel profil mahasiswa bersama tren bar.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menormalisasi setiap dimensi ke rentang 0..1 via {@link #clamp(double)}:
	 *       IPK → IPK/4.0; IPS → IPS/4.0; SKS → sksKum/144.0 (standar S1);
	 *       Valid → valid/total (proporsi matakuliah lulus dari yang diambil).</li>
	 *   <li>Menggunakan sudut -90° (atas=IPK), 0° (kanan=IPS), 90° (bawah=SKS),
	 *       180° (kiri=Valid) via {@link #point(int, int, double, int)}.</li>
	 *   <li>Menggambar poligon luar (batas), poligon tengah (50%), sumbu silang,
	 *       dan poligon isi (nilai aktual, semi-transparan biru).</li>
	 *   <li>Menambahkan label empat sumbu dan caption teks di bawah.</li>
	 * </ol>
	 * </p>
	 *
	 * <p>Untuk radar N-sumbu yang lebih generik, lihat {@link #radarN(String[], double[], String)}.</p>
	 *
	 * @param ipk     Indeks Prestasi Kumulatif (0..4)
	 * @param ips     Indeks Prestasi Semester terakhir (0..4)
	 * @param sksKum  total SKS kumulatif; dinormalisasi terhadap 144
	 * @param valid   jumlah matakuliah dengan nilai valid
	 * @param total   jumlah total matakuliah yang diambil; jika 0, dimensi Valid = 0
	 * @return string HTML berisi div wrapper + SVG radar chart
	 */
	public static String buildRadar(double ipk, double ips, double sksKum, double valid, double total) {
		double pIpk = clamp(ipk / 4.0);
		double pIps = clamp(ips / 4.0);
		double pSks = clamp(sksKum / 144.0);
		double pValid = total <= 0 ? 0.0 : clamp(valid / total);
		int cx = 100;
		int cy = 92;
		int r = 66;
		String points = point(cx, cy, r * pIpk, -90) + " " + point(cx, cy, r * pIps, 0) + " "
				+ point(cx, cy, r * pSks, 90) + " " + point(cx, cy, r * pValid, 180);
		StringBuffer sb = new StringBuffer();
		sb.append("<div class=\"ais-profile-radar\"><svg width=\"210\" height=\"190\" viewBox=\"0 0 210 190\" role=\"img\" aria-label=\"Radar akademik\">");
		sb.append("<polygon points=\"100,26 166,92 100,158 34,92\" fill=\"#f8fafc\" stroke=\"#cbd5e1\" stroke-width=\"1\"></polygon>");
		sb.append("<polygon points=\"100,48 144,92 100,136 56,92\" fill=\"none\" stroke=\"#e2e8f0\" stroke-width=\"1\"></polygon>");
		sb.append("<line x1=\"100\" y1=\"26\" x2=\"100\" y2=\"158\" stroke=\"#e2e8f0\"/><line x1=\"34\" y1=\"92\" x2=\"166\" y2=\"92\" stroke=\"#e2e8f0\"/>");
		sb.append("<polygon points=\"").append(points).append("\" fill=\"rgba(37,99,235,.22)\" stroke=\"#2563eb\" stroke-width=\"2\"></polygon>");
		sb.append("<text x=\"100\" y=\"16\" text-anchor=\"middle\" font-size=\"10\" fill=\"#334155\">IPK</text>");
		sb.append("<text x=\"176\" y=\"96\" font-size=\"10\" fill=\"#334155\">IPS</text>");
		sb.append("<text x=\"100\" y=\"176\" text-anchor=\"middle\" font-size=\"10\" fill=\"#334155\">SKS</text>");
		sb.append("<text x=\"4\" y=\"96\" font-size=\"10\" fill=\"#334155\">Valid</text>");
		sb.append("</svg><div style=\"font-size:10.5px;color:#64748b;line-height:1.45;\">Ringkasan visual ini merangkum kekuatan akademik mahasiswa dari sisi IPK, IPS, SKS, dan validitas nilai.</div></div>");
		return sb.toString();
	}

	/**
	 * Menghitung koordinat titik SVG di atas lingkaran pada sudut tertentu.
	 *
	 * <p>Mengkonversi koordinat polar (jari-jari {@code r}, sudut {@code degree} dalam
	 * derajat) ke koordinat Cartesian (x, y) relatif terhadap pusat (cx, cy).
	 * Hasilnya berupa string "x,y" yang langsung dapat digunakan dalam atribut
	 * {@code points} SVG.</p>
	 *
	 * @param cx     koordinat x pusat lingkaran dalam pixel SVG
	 * @param cy     koordinat y pusat lingkaran dalam pixel SVG
	 * @param r      jari-jari (sudah diskala ke nilai 0..max); bisa desimal
	 * @param degree sudut dalam derajat (0=kanan, -90=atas, 90=bawah, 180=kiri)
	 * @return string koordinat "x,y" untuk atribut SVG points
	 */
	private static String point(int cx, int cy, double r, int degree) {
		double rad = Math.toRadians(degree);
		int x = (int) Math.round(cx + Math.cos(rad) * r);
		int y = (int) Math.round(cy + Math.sin(rad) * r);
		return x + "," + y;
	}

	/**
	 * Membatasi nilai {@code double} ke rentang [0.0, 1.0].
	 *
	 * <p>Digunakan untuk memastikan proporsi (nilai/total) tidak keluar dari rentang
	 * valid sebelum dikonversi ke derajat atau persentase. Nilai negatif di-clamp ke 0;
	 * nilai di atas 1 di-clamp ke 1.</p>
	 *
	 * @param d nilai yang akan di-clamp
	 * @return nilai dalam rentang [0.0, 1.0]
	 */
	private static double clamp(double d) {
		if (d < 0.0) {
			return 0.0;
		}
		if (d > 1.0) {
			return 1.0;
		}
		return d;
	}

	/**
	 * Memformat nilai numerik sebagai string angka dengan pemisah ribuan (mis. "1.234").
	 *
	 * <p>Menggunakan {@code Common.numberFormat} (ThreadLocal {@code NumberFormat}) untuk
	 * format lokal Indonesia. Jika format gagal (mis. nilai null), mengembalikan "0" atau
	 * representasi string mentah dari nilai.</p>
	 *
	 * @param value nilai yang akan diformat; boleh {@code null}, {@code Integer}, {@code Long},
	 *              {@code Double}, atau objek lainnya yang didukung {@code NumberFormat}
	 * @return string terformat, mis. {@code "1.234"} untuk nilai 1234
	 */
	public static String fmt(Object value) {
		try {
			return Common.numberFormat.get().format(value);
		} catch (Exception e) {
			return value == null ? "0" : String.valueOf(value);
		}
	}

	/**
	 * Mengkonversi nilai objek apapun ke {@code double} dengan aman.
	 *
	 * <p>Urutan konversi: null → 0.0; Number → doubleValue(); lainnya →
	 * {@code Double.parseDouble(String.valueOf(value))}. Jika parse gagal
	 * (mis. nilai berupa teks non-numerik), mengembalikan 0.0 tanpa exception.</p>
	 *
	 * @param value nilai yang akan dikonversi; boleh {@code null}
	 * @return nilai {@code double}, atau {@code 0.0} jika konversi gagal
	 */
	public static double asDouble(Object value) {
		if (value == null) {
			return 0.0;
		}
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}
		try {
			return Double.parseDouble(String.valueOf(value));
		} catch (Exception e) {
			return 0.0;
		}
	}

	/**
	 * Mengkonversi nilai objek ke string, mengembalikan string kosong untuk null.
	 *
	 * <p>Padanan null-safe dari {@code String.valueOf(value)} yang tidak pernah
	 * mengembalikan {@code "null"}. Digunakan sebagai langkah pertama sebelum
	 * {@link #esc(Object)} jika nilai objek bukan tipe String.</p>
	 *
	 * @param value nilai yang akan dikonversi; boleh {@code null}
	 * @return string representasi nilai, atau {@code ""} jika null
	 */
	public static String text(Object value) {
		return value == null ? "" : String.valueOf(value);
	}

	/**
	 * Meng-escape karakter khusus HTML agar aman disisipkan ke dalam konten HTML.
	 *
	 * <p><b>Tujuan:</b> Mencegah XSS (Cross-Site Scripting) saat nilai data pengguna
	 * disisipkan ke dalam template HTML profil. Harus dipanggil untuk SETIAP nilai
	 * yang berasal dari database atau input pengguna sebelum dimasukkan ke dalam
	 * elemen HTML (label, judul, teks keterangan, nama, dll.).</p>
	 *
	 * <p><b>Karakter yang di-escape:</b>
	 * {@code &} → {@code &amp;}, {@code <} → {@code &lt;},
	 * {@code >} → {@code &gt;}, {@code "} → {@code &quot;}.</p>
	 *
	 * @param value nilai yang akan di-escape; {@code null} diperlakukan sebagai string kosong
	 * @return string yang aman untuk disisipkan ke dalam konten HTML
	 */
	public static String esc(Object value) {
		String s = value == null ? "" : String.valueOf(value);
		s = s.replace("&", "&amp;");
		s = s.replace("<", "&lt;");
		s = s.replace(">", "&gt;");
		s = s.replace("\"", "&quot;");
		return s;
	}

	/* ================= Builder tambahan yang bisa dipakai ulang ================= */

	/**
	 * Merender kartu identitas profil satu baris ke dalam baris grid.
	 *
	 * <p><b>Tujuan:</b> Membangun baris pertama halaman profil: foto profil di kiri,
	 * sapaan dan informasi identitas (nama, NIP/NIM, HP, email) di kanan. Layout
	 * menggunakan CSS kelas {@code ais-profil-kartu} dari {@code css_utama.css}.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Membuat baris {@code MyRowStyled} baru dengan spans sesuai parameter.</li>
	 *   <li>Membangun {@code Div.ais-profil-kartu} berisi dua div anak:
	 *       {@code ais-profil-kartu-foto} (untuk komponen foto) dan
	 *       {@code ais-profil-kartu-info} (untuk teks identitas).</li>
	 *   <li>Jika foto tidak null, memasangnya ke div foto.</li>
	 *   <li>Jika sapaan tidak kosong, menambahkan {@code Label.ais-profil-kartu-sapaan}.</li>
	 *   <li>Mengembalikan div info; pemanggil dapat langsung menambahkan child seperti
	 *       {@code MyLabelBoldAja(nama)}, {@code tbmuser.tampilkanHp(info)}, dll.</li>
	 * </ol>
	 * </p>
	 *
	 * @param rows   baris ZK tujuan; tidak boleh {@code null}
	 * @param spans  jumlah kolom yang digabung (mis. 2 untuk grid 2-kolom)
	 * @param foto   komponen ZK gambar profil; boleh {@code null} (kartu tetap dirender)
	 * @param sapaan teks sapaan kontekstual (mis. "Hai, Selamat Pagi"); boleh null
	 * @return div info yang siap menerima child label nama, HP, email, dll.
	 * Styling: css_utama.css blok "KARTU IDENTITAS PROFIL".
	 */
	public static Div mulaiKartuIdentitas(Rows rows, int spans, Component foto, String sapaan) {
		Row row = new MyRowStyled();
		try {
			ais.ui.util.ZkCompat.setSpans(row, String.valueOf(spans));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileUiHelper.java:993");
		}
		row.setParent(rows);

		Div kartu = new Div();
		kartu.setSclass("ais-profil-kartu");
		kartu.setParent(row);

		Div kotakFoto = new Div();
		kotakFoto.setSclass("ais-profil-kartu-foto");
		kotakFoto.setParent(kartu);
		if (foto != null) {
			foto.setParent(kotakFoto);
		}

		Div info = new Div();
		info.setSclass("ais-profil-kartu-info");
		info.setParent(kartu);

		if (notBlank(sapaan)) {
			// Sapaan ("Hai, Selamat Pagi/…") = teks STATIS → terjemahkan mengikuti bahasa aktif.
			org.zkoss.zul.Label labelSapaan = new org.zkoss.zul.Label(ais.common.Common.getBahasaConfig(sapaan));
			labelSapaan.setSclass("ais-profil-kartu-sapaan");
			labelSapaan.setParent(info);
		}
		return info;
	}

	/**
	 * Membungkus beberapa kartu angka dalam satu baris responsif {@code .ais-profile-grid}.
	 *
	 * <p>Menggabungkan array HTML string kartu (dari {@link #stat}, {@link #donut},
	 * {@link #statClickable}, {@link #donutClickable}) ke dalam div wrapper bergaya
	 * flexbox responsif. Elemen null dalam array diabaikan (aman untuk elemen opsional
	 * seperti donut yang hanya ditampilkan jika ada data).</p>
	 *
	 * @param statHtml array string HTML kartu; boleh {@code null} atau berisi null per elemen
	 * @return string HTML div wrapper berisi semua kartu; tidak pernah {@code null}
	 */
	public static String statsWrap(String[] statHtml) {
		StringBuffer sb = new StringBuffer("<div class=\"ais-profile-grid\">");
		if (statHtml != null) {
			for (int i = 0; i < statHtml.length; i++) {
				if (statHtml[i] != null) {
					sb.append(statHtml[i]);
				}
			}
		}
		sb.append("</div>");
		return sb.toString();
	}

	/**
	 * Membangun layout dua kolom responsif dalam HTML.
	 *
	 * <p>Menggunakan div {@code .ais-profile-cols} (flexbox) yang secara otomatis
	 * menumpuk ke bawah pada layar kecil ({@code flex-wrap:wrap}). Digunakan untuk
	 * menempatkan dua panel (mis. biodata + data orang tua) secara berdampingan
	 * di layar lebar dan vertikal di layar sempit.</p>
	 *
	 * @param kiri  HTML konten kolom kiri; boleh null (diabaikan)
	 * @param kanan HTML konten kolom kanan; boleh null (diabaikan)
	 * @return string HTML layout dua kolom
	 */
	public static String cols(String kiri, String kanan) {
		return "<div class=\"ais-profile-cols\"><div>" + (kiri == null ? "" : kiri) + "</div><div>"
				+ (kanan == null ? "" : kanan) + "</div></div>";
	}

	/**
	 * Membuat pasangan label-nilai sebagai array dua elemen untuk dipakai dalam {@link #infoTable}.
	 *
	 * <p>Shorthand untuk {@code new String[]{label, text(value)}} agar kode pemanggil
	 * lebih ringkas. Baris dengan nilai kosong akan otomatis diabaikan oleh
	 * {@link #infoTable(String, String, String[][])} sehingga tabel tidak menampilkan
	 * baris "Label: " yang kosong.</p>
	 *
	 * @param label nama properti yang ditampilkan di kolom kiri tabel
	 * @param value nilai properti; dikonversi ke string via {@link #text(Object)};
	 *              boleh null (menjadi string kosong dan dilewati oleh infoTable)
	 * @return array dua elemen {@code [label, valueAsString]}
	 */
	public static String[] pasangan(String label, Object value) {
		return new String[] { label, text(value) };
	}

	/**
	 * Membangun kartu tabel label-nilai untuk informasi biodata atau detail entitas.
	 *
	 * <p><b>Tujuan:</b> Menyajikan data biodata (nama, NIP, jabatan, dll.) dalam bentuk
	 * tabel dua kolom yang bersih. Baris dengan nilai kosong atau {@code "-"}
	 * secara otomatis dilewati sehingga tabel tidak menampilkan baris yang tidak informatif.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Iterasi array {@code data}; setiap elemen adalah array 2 string [label, nilai].</li>
	 *   <li>Baris dilewati jika: null, kurang dari 2 elemen, nilai kosong, atau nilai {@code "-"}.</li>
	 *   <li>Nilai di-escape via {@link #esc(Object)} sebelum disisipkan ke HTML.</li>
	 *   <li>Jika tidak ada satu pun baris yang ditampilkan, menambahkan teks
	 *       "Data belum diisi."</li>
	 *   <li>Membungkus tabel dalam {@link #panel(String, String, String)} sehingga
	 *       tabel memiliki header yang konsisten dengan kartu lainnya.</li>
	 * </ol>
	 * </p>
	 *
	 * @param title       judul panel yang membungkus tabel; di-escape
	 * @param description deskripsi singkat panel; di-escape
	 * @param data        array pasangan [label, nilai]; boleh {@code null} atau berisi null per baris;
	 *                    gunakan {@link #pasangan(String, Object)} untuk mengisi tiap elemen
	 * @return string HTML kartu tabel yang sudah dibungkus dalam {@link #panel}
	 */
	public static String infoTable(String title, String description, String[][] data) {
		StringBuffer sb = new StringBuffer("<div style=\"padding:12px;\"><table class=\"ais-profile-table\">");
		int tampil = 0;
		if (data != null) {
			for (int i = 0; i < data.length; i++) {
				if (data[i] == null || data[i].length < 2 || !notBlank(data[i][1]) || "-".equals(data[i][1].trim())) {
					continue;
				}
				sb.append("<tr><th style=\"width:38%;\">").append(esc(data[i][0])).append("</th><td>")
						.append(esc(data[i][1])).append("</td></tr>");
				tampil++;
			}
		}
		sb.append("</table>");
		if (tampil == 0) {
			sb.append("<div style=\"font-size:11px;color:#64748b;\">Data belum diisi.</div>");
		}
		sb.append("</div>");
		return panel(title, description, sb.toString());
	}

	/**
	 * Membangun HTML kartu donut (CSS conic-gradient) yang otomatis bisa diklik untuk popup rincian.
	 *
	 * <p><b>Tujuan:</b> Menyajikan proporsi nilai terhadap total dalam bentuk lingkaran
	 * donut warna, dilengkapi persentase di tengah. Kartu secara otomatis membuka popup
	 * rincian (nilai, total, porsi persen) saat diklik — tanpa pemanggil perlu mengatur
	 * popup secara eksplisit.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menghitung proporsi {@code p = nilai/total} dan clamp ke [0,1].</li>
	 *   <li>Mengkonversi ke derajat {@code derajat = round(p * 360)} dan persen.</li>
	 *   <li>Menggunakan CSS {@code conic-gradient(warna N deg, #e2e8f0 0)} untuk
	 *       lingkaran donut (tidak memerlukan SVG atau Chart.js).</li>
	 *   <li>Menghasilkan ID modal unik dan menambahkan popup rincian via {@link #modal}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p>Jika ingin popup kustom, gunakan {@link #donutClickable(String, double, double, String, String, String)}
	 * dengan ID modal yang ditetapkan sendiri.</p>
	 *
	 * @param label nama metrik, mis. "Laki-laki"; di-escape via {@link #esc}
	 * @param nilai nilai bagian (pembilang)
	 * @param total nilai total (penyebut); jika 0 → proporsi = 0
	 * @param warna warna CSS (mis. "#2563eb"); jika null/kosong → default biru
	 * @param hint  keterangan singkat di bawah angka; di-escape via {@link #esc}
	 * @return string HTML kartu donut + popup tersembunyi
	 */
	public static String donut(String label, double nilai, double total, String warna, String hint) {
		double p = total <= 0 ? 0.0 : clamp(nilai / total);
		int derajat = (int) Math.round(p * 360.0);
		int persen = (int) Math.round(p * 100.0);
		String warnaAman = notBlank(warna) ? warna : "#2563eb";
		String mid = nextModalId("aisDonut");
		String card = "<div class=\"ais-profile-stat ais-profile-stat-click\" style=\"text-align:center;\" onclick=\"var m=document.getElementById('"
				+ mid + "');if(m){m.style.display='flex';}\"><div class=\"label\">" + esc(label)
				+ "</div><div class=\"ais-profile-donut\" style=\"background:conic-gradient(" + warnaAman + " " + derajat
				+ "deg,#e2e8f0 0);\"><i>" + persen + "%</i></div><div class=\"value\" style=\"font-size:14px;\">"
				+ esc(fmt(Double.valueOf(nilai))) + "</div><div class=\"hint\">" + esc(hint)
				+ "</div><div class=\"ais-profile-click-hint\">&#128712; " + ais.common.Common.getBahasaConfig("Klik untuk rincian") + "</div></div>";
		String body = (notBlank(hint) ? "<p>" + esc(hint) + "</p>" : "") + tabelRincian(new String[] {
				barisRincian(label, fmt(Double.valueOf(nilai))), barisRincian("Total", fmt(Double.valueOf(total))),
				barisRincian("Porsi", persen + "%") });
		return card + modal(mid, ais.common.Common.getBahasaConfig("Rincian") + " " + label, body);
	}

	/**
	 * Membangun HTML kartu statistik yang saat diklik membuka popup dengan ID tertentu.
	 *
	 * <p>Versi {@link #stat(String, Object, String)} dengan ID modal eksternal: popup
	 * dibangun oleh pemanggil secara terpisah (via {@link #modal(String, String, String)})
	 * dan disematkan ke dalam konteks yang sama. Digunakan saat satu popup perlu dibagi
	 * oleh beberapa kartu, atau saat konten popup lebih kompleks dari otomatis.</p>
	 *
	 * @param label   nama metrik; di-escape via {@link #esc}
	 * @param value   nilai yang ditampilkan; di-escape via {@link #esc}
	 * @param hint    keterangan singkat; di-escape via {@link #esc}
	 * @param modalId ID elemen popup HTML yang akan dibuka saat kartu diklik
	 * @return string HTML kartu angka clickable (tanpa popup — popup disiapkan pemanggil)
	 */
	public static String statClickable(String label, Object value, String hint, String modalId) {
		return "<div class=\"ais-profile-stat ais-profile-stat-click\" onclick=\"var m=document.getElementById('"
				+ esc(modalId) + "');if(m){m.style.display='flex';}\"><div class=\"label\">" + esc(label)
				+ "</div><div class=\"value\">" + esc(value) + "</div><div class=\"hint\">" + esc(hint)
				+ "</div><div class=\"ais-profile-click-hint\">&#128712; " + ais.common.Common.getBahasaConfig("Klik untuk rincian") + "</div></div>";
	}

	/**
	 * Membangun HTML kartu donut yang saat diklik membuka popup dengan ID tertentu.
	 *
	 * <p>Versi {@link #donut(String, double, double, String, String)} dengan ID modal
	 * eksternal: popup dibangun oleh pemanggil secara terpisah. Digunakan saat konten
	 * popup lebih kompleks (mis. tabel rincian per prodi di kartu akademik PT)
	 * daripada popup auto yang dihasilkan oleh {@link #donut}.</p>
	 *
	 * @param label   nama metrik; di-escape via {@link #esc}
	 * @param nilai   nilai bagian (pembilang)
	 * @param total   nilai total (penyebut); jika 0 → proporsi = 0
	 * @param warna   warna CSS donut; null/kosong → default biru
	 * @param hint    keterangan singkat di bawah angka; di-escape via {@link #esc}
	 * @param modalId ID elemen popup HTML yang akan dibuka saat kartu diklik
	 * @return string HTML kartu donut clickable (tanpa popup — popup disiapkan pemanggil)
	 */
	public static String donutClickable(String label, double nilai, double total, String warna, String hint,
			String modalId) {
		double p = total <= 0 ? 0.0 : clamp(nilai / total);
		int derajat = (int) Math.round(p * 360.0);
		int persen = (int) Math.round(p * 100.0);
		String warnaAman = notBlank(warna) ? warna : "#2563eb";
		return "<div class=\"ais-profile-stat ais-profile-stat-click\" style=\"text-align:center;\" onclick=\"var m=document.getElementById('"
				+ esc(modalId) + "');if(m){m.style.display='flex';}\"><div class=\"label\">" + esc(label)
				+ "</div><div class=\"ais-profile-donut\" style=\"background:conic-gradient(" + warnaAman + " " + derajat
				+ "deg,#e2e8f0 0);\"><i>" + persen + "%</i></div><div class=\"value\" style=\"font-size:14px;\">"
				+ esc(fmt(Double.valueOf(nilai))) + "</div><div class=\"hint\">" + esc(hint)
				+ "</div><div class=\"ais-profile-click-hint\">&#128712; " + ais.common.Common.getBahasaConfig("Klik untuk rincian") + "</div></div>";
	}

	/**
	 * Membangun HTML overlay popup modal yang tersembunyi (display:none saat inisialisasi).
	 *
	 * <p><b>Tujuan:</b> Membuat popup rincian yang muncul saat kartu angka diklik.
	 * Popup menggunakan {@code position:fixed} dengan z-index tinggi sehingga selalu
	 * tampil di atas seluruh konten, dan menutup saat pengguna mengklik latar (overlay)
	 * atau tombol tanda silang (×).</p>
	 *
	 * <p><b>Cara kerja:</b> Membangun div dengan ID {@code modalId} dan class
	 * {@code ais-profile-modal} (display:none → flex saat diklik). Di dalamnya ada
	 * {@code ais-profile-modal-box} berisi header (judul + tombol ×) dan body
	 * ({@code bodyHtml}).</p>
	 *
	 * <p><b>Perhatian:</b> {@code bodyHtml} tidak di-escape — harus sudah berupa HTML
	 * yang aman. Judul di-escape via {@link #esc(Object)}. {@code modalId} juga
	 * di-escape untuk keamanan.</p>
	 *
	 * @param modalId  ID unik elemen popup (digunakan oleh onclick kartu untuk membuka popup)
	 * @param title    judul yang ditampilkan di header popup; di-escape via {@link #esc}
	 * @param bodyHtml konten HTML body popup; tidak di-escape (sudah harus aman)
	 * @return string HTML div popup tersembunyi; siap disisipkan ke halaman
	 */
	public static String modal(String modalId, String title, String bodyHtml) {
		return "<div id=\"" + esc(modalId)
				+ "\" class=\"ais-profile-modal\" onclick=\"if(event.target===this){this.style.display='none';}\">"
				+ "<div class=\"ais-profile-modal-box\"><div class=\"ais-profile-modal-head\"><span>" + esc(title)
				+ "</span><span class=\"ais-profile-modal-x\" onclick=\"var m=document.getElementById('" + esc(modalId)
				+ "');if(m){m.style.display='none';}\">&times;</span></div><div class=\"ais-profile-modal-body\">"
				+ (bodyHtml == null ? "" : bodyHtml) + "</div></div></div>";
	}

	/**
	 * Membangun satu baris HTML tabel dua kolom (label | nilai) untuk isi popup rincian.
	 *
	 * <p>Shorthand untuk {@code <tr><td>label</td><td>nilai</td></tr>} dengan escape.
	 * Dimaksudkan untuk dipakai bersama {@link #tabelRincian(String[])} yang membungkus
	 * baris-baris dalam elemen tabel.</p>
	 *
	 * @param label nama properti di kolom kiri; di-escape via {@link #esc}
	 * @param nilai nilai di kolom kanan; di-escape via {@link #esc}
	 * @return string HTML satu baris {@code <tr>...</tr>}
	 */
	public static String barisRincian(String label, Object nilai) {
		return "<tr><td>" + esc(label) + "</td><td>" + esc(nilai) + "</td></tr>";
	}

	/**
	 * Membungkus baris-baris HTML tabel (dari {@link #barisRincian}) menjadi tabel rincian popup.
	 *
	 * <p>Menghasilkan elemen {@code table.ais-profile-mtbl} berisi semua baris yang
	 * diberikan. Elemen null dalam array diabaikan. Digunakan untuk menyusun isi body
	 * popup rincian (mis. tabel porsi: label, nilai, total, persentase).</p>
	 *
	 * @param baris array string HTML baris tabel (dari {@link #barisRincian}); boleh null atau berisi null
	 * @return string HTML {@code <table>...</table>}; tidak pernah null
	 */
	public static String tabelRincian(String[] baris) {
		StringBuffer sb = new StringBuffer("<table class=\"ais-profile-mtbl\"><tbody>");
		if (baris != null) {
			for (int i = 0; i < baris.length; i++) {
				if (baris[i] != null) {
					sb.append(baris[i]);
				}
			}
		}
		sb.append("</tbody></table>");
		return sb.toString();
	}

	/**
	 * Counter global untuk pembuatan ID modal HTML yang unik di satu halaman.
	 *
	 * <p>Menggunakan {@code AtomicLong} agar aman di lingkungan multi-thread: setiap
	 * pemanggilan {@link #nextModalId(String)} menjamin nilai yang berbeda, sehingga
	 * popup tidak bertabrakan ID meskipun banyak kartu dirender dalam satu request.</p>
	 */
	private static final java.util.concurrent.atomic.AtomicLong MODAL_SEQ = new java.util.concurrent.atomic.AtomicLong();

	/**
	 * Menghasilkan ID HTML unik untuk elemen popup modal.
	 *
	 * <p><b>Tujuan:</b> Memastikan setiap popup di halaman profil memiliki ID HTML
	 * yang berbeda, sehingga onclick kartu dapat menemukan popup yang tepat via
	 * {@code document.getElementById(id)}.</p>
	 *
	 * <p><b>Cara kerja:</b> Mengambil dan menginkremen {@code MODAL_SEQ} secara atomik,
	 * lalu menggabungkan prefix dan angka. Format hasil: {@code "prefix123"} atau
	 * {@code "aism123"} jika prefix null/kosong.</p>
	 *
	 * <p><b>Thread-safety:</b> {@code AtomicLong.incrementAndGet()} bersifat thread-safe
	 * dan tidak memerlukan sinkronisasi eksternal.</p>
	 *
	 * @param prefix awalan ID, mis. {@code "aisStat"}, {@code "aisDonut"}, {@code "aisPrest"};
	 *               jika null atau kosong, menggunakan default {@code "aism"}
	 * @return string ID unik untuk digunakan sebagai nilai atribut {@code id} HTML
	 */
	public static String nextModalId(String prefix) {
		return (prefix == null || prefix.trim().length() == 0 ? "aism" : prefix) + MODAL_SEQ.incrementAndGet();
	}

	/**
	 * Membangun konten HTML popup rincian untuk sebuah angka agregat.
	 *
	 * <p><b>Tujuan:</b> Menyajikan popup yang ringan (tanpa query tambahan) untuk
	 * kartu angka yang tidak menampilkan daftar per-individu (misalnya angka total
	 * Prestasi Siswa, Guru, Pegawai). Popup hanya menampilkan makna angka, jumlah
	 * total, dan panduan menu untuk melihat daftar rinci.</p>
	 *
	 * <p><b>Alasan tidak ada query tambahan:</b> Angka yang sangat besar (mis. ribuan
	 * prestasi) tidak praktis ditampilkan dalam popup — pengguna diarahkan ke menu
	 * spesifik yang sudah mendukung pencarian dan paginasi.</p>
	 *
	 * @param label   nama metrik, mis. "Prestasi Siswa"; di-escape via {@link #esc}
	 * @param value   nilai total yang ditampilkan; di-escape via {@link #esc}
	 * @param hint    keterangan makna angka, mis. "Total prestasi siswa tercatat"; di-escape
	 * @param panduan teks panduan menu, mis. "Daftar rinci ada di menu Prestasi"; di-escape;
	 *                boleh null (tidak ditampilkan)
	 * @return string HTML isi popup (siap diteruskan ke {@link #modal(String, String, String)})
	 */
	public static String modalAngka(String label, Object value, String hint, String panduan) {
		StringBuffer sb = new StringBuffer();
		sb.append("<p><b>").append(esc(label)).append("</b> &mdash; ").append(esc(hint)).append(".</p>");
		sb.append("<p>Jumlah tercatat: <b style=\"font-size:16px;color:#2563eb;\">").append(esc(value))
				.append("</b></p>");
		if (notBlank(panduan)) {
			sb.append("<p style=\"color:#475569;\">").append(esc(panduan)).append("</p>");
		}
		return sb.toString();
	}

	/**
	 * Menambahkan satu kartu angka clickable beserta popup-nya ke daftar stats dan buffer modals.
	 *
	 * <p><b>Tujuan:</b> Pembangun berpasangan yang dipakai oleh
	 * {@link #appendPrestasiRingkas(Rows, boolean, boolean, boolean)}: setiap panggilan
	 * menambahkan satu kartu HTML ke list {@code stats} dan satu popup tersembunyi ke
	 * {@code modals}. ID modal dibuat unik via {@link #nextModalId(String)}.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menghasilkan ID unik via {@code nextModalId("aisPrest")}.</li>
	 *   <li>Menambahkan HTML kartu via {@link #statClickable(String, Object, String, String)}
	 *       ke list {@code stats}.</li>
	 *   <li>Menambahkan HTML popup via {@link #modal} + {@link #modalAngka} ke buffer
	 *       {@code modals}.</li>
	 * </ol>
	 * </p>
	 *
	 * @param stats   list mutable yang akan ditambahi string HTML kartu
	 * @param modals  StringBuffer yang akan ditambahi string HTML popup
	 * @param label   nama metrik, mis. "Prestasi Siswa"; di-escape via {@link #esc}
	 * @param jumlah  nilai agregat yang ditampilkan (diformat via {@link #fmt})
	 * @param hint    keterangan singkat di bawah angka kartu
	 * @param panduan panduan menu untuk daftar rinci per individu
	 */
	private static void tambahKartuAngka(List<String> stats, StringBuffer modals, String label, long jumlah,
			String hint, String panduan) {
		String mid = nextModalId("aisPrest");
		stats.add(statClickable(label, fmt(Long.valueOf(jumlah)), hint, mid));
		modals.append(modal(mid, ais.common.Common.getBahasaConfig("Rincian") + " " + label, modalAngka(label, fmt(Long.valueOf(jumlah)), hint, panduan)));
	}

	/**
	 * Membangun daftar bar horizontal CSS dari data {label, jumlah}.
	 *
	 * <p><b>Tujuan:</b> Menampilkan komposisi atau peringkat (mis. jumlah siswa per kelas,
	 * distribusi nilai) dalam bentuk bar horizontal proporsional. Cocok untuk data dengan
	 * sedikit kategori (&lt;20 item) yang perlu perbandingan visual.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Mencari nilai maksimum dari semua elemen untuk normalisasi lebar bar.</li>
	 *   <li>Per baris: menghitung lebar persentase ({@code nilai/max*100}), minimum 3%
	 *       agar bar selalu terlihat jika ada nilainya.</li>
	 *   <li>Label yang kosong ditampilkan sebagai "Tidak diisi".</li>
	 *   <li>Menggunakan CSS class {@code ais-profile-hbar} (flexbox: label | bar | nilai).</li>
	 * </ol>
	 * </p>
	 *
	 * <p>Jika {@code data} null atau kosong, mengembalikan div teks "Belum ada data."</p>
	 *
	 * @param data list array objek berformat {@code [label, jumlah]}; boleh null atau kosong;
	 *             setiap elemen diakses via indeks 0 (label) dan 1 (jumlah)
	 * @return string HTML daftar bar horizontal
	 */
	public static String barList(List<Object[]> data) {
		if (data == null || data.isEmpty()) {
			return "<div style=\"padding:12px;color:#64748b;font-size:11px;\">Belum ada data.</div>";
		}
		double max = 1.0;
		int i;
		for (i = 0; i < data.size(); i++) {
			max = Math.max(max, asDouble(data.get(i)[1]));
		}
		StringBuffer sb = new StringBuffer("<div style=\"padding:12px;\">");
		for (i = 0; i < data.size(); i++) {
			Object[] d = data.get(i);
			double nilai = asDouble(d[1]);
			int lebar = (int) Math.round((nilai / max) * 100.0);
			if (lebar < 3 && nilai > 0) {
				lebar = 3;
			}
			String label = notBlank(text(d[0])) ? text(d[0]) : "Tidak diisi";
			sb.append("<div class=\"ais-profile-hbar\"><span class=\"nm\" title=\"").append(esc(label)).append("\">")
					.append(esc(label)).append("</span><span class=\"bg\"><b style=\"width:").append(lebar)
					.append("%\"></b></span><span class=\"vl\">").append(esc(fmt(nilai))).append("</span></div>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	/**
	 * Membangun grafik radar (spider web) SVG untuk N sumbu (minimum 3, maksimum ~8).
	 *
	 * <p><b>Tujuan:</b> Versi generik dari {@link #buildRadar(double, double, double, double, double)}
	 * yang mendukung jumlah sumbu bebas. Digunakan untuk menyajikan profil multidimensi
	 * yang jumlah dimensinya tidak selalu 4 (mis. kompetensi dosen atau rekap aspek OBE).</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memvalidasi: labels dan nilai01 tidak null, panjangnya sama, dan minimal 3 sumbu.
	 *       Jika tidak valid, mengembalikan string kosong.</li>
	 *   <li>Membagi 360° secara merata per sumbu mulai dari -90° (atas).</li>
	 *   <li>Per sumbu: menghitung titik polygon luar (r=70), tengah (r=35), isi (r*nilai01[i]),
	 *       dan posisi label teks (r+16).</li>
	 *   <li>Menggunakan {@link #clamp(double)} untuk memastikan nilai01[i] dalam [0,1].</li>
	 *   <li>Menambahkan caption teks di bawah jika tidak kosong.</li>
	 * </ol>
	 * </p>
	 *
	 * @param labels  array label setiap sumbu; minimal 3 elemen, sama panjang dengan {@code nilai01}
	 * @param nilai01 array nilai dinormalisasi [0.0 .. 1.0] per sumbu (1.0 = skor penuh)
	 * @param caption teks penjelasan di bawah grafik; boleh null/kosong (tidak ditampilkan)
	 * @return string HTML div + SVG grafik radar, atau string kosong jika input tidak valid
	 */
	public static String radarN(String[] labels, double[] nilai01, String caption) {
		if (labels == null || nilai01 == null || labels.length < 3 || labels.length != nilai01.length) {
			return "";
		}
		int cx = 110, cy = 100, r = 70;
		StringBuffer luar = new StringBuffer();
		StringBuffer dalam = new StringBuffer();
		StringBuffer isi = new StringBuffer();
		StringBuffer teks = new StringBuffer();
		for (int i = 0; i < labels.length; i++) {
			double rad = Math.toRadians(-90.0 + (360.0 * i / labels.length));
			int x1 = (int) Math.round(cx + Math.cos(rad) * r);
			int y1 = (int) Math.round(cy + Math.sin(rad) * r);
			int x2 = (int) Math.round(cx + Math.cos(rad) * (r / 2.0));
			int y2 = (int) Math.round(cy + Math.sin(rad) * (r / 2.0));
			double rIsi = r * clamp(nilai01[i]);
			int x3 = (int) Math.round(cx + Math.cos(rad) * rIsi);
			int y3 = (int) Math.round(cy + Math.sin(rad) * rIsi);
			int lx = (int) Math.round(cx + Math.cos(rad) * (r + 16));
			int ly = (int) Math.round(cy + Math.sin(rad) * (r + 16));
			if (i > 0) {
				luar.append(' ');
				dalam.append(' ');
				isi.append(' ');
			}
			luar.append(x1).append(',').append(y1);
			dalam.append(x2).append(',').append(y2);
			isi.append(x3).append(',').append(y3);
			teks.append("<text x=\"").append(lx).append("\" y=\"").append(ly)
					.append("\" text-anchor=\"middle\" font-size=\"9\" fill=\"#334155\">").append(esc(labels[i]))
					.append("</text>");
		}
		StringBuffer sb = new StringBuffer("<div class=\"ais-profile-radar\"><svg width=\"220\" height=\"205\" viewBox=\"0 0 220 205\">");
		sb.append("<polygon points=\"").append(luar).append("\" fill=\"#f8fafc\" stroke=\"#cbd5e1\"></polygon>");
		sb.append("<polygon points=\"").append(dalam).append("\" fill=\"none\" stroke=\"#e2e8f0\"></polygon>");
		sb.append("<polygon points=\"").append(isi).append("\" fill=\"rgba(37,99,235,.22)\" stroke=\"#2563eb\" stroke-width=\"2\"></polygon>");
		sb.append(teks);
		sb.append("</svg>");
		if (notBlank(caption)) {
			sb.append("<div style=\"font-size:10.5px;color:#64748b;line-height:1.45;\">").append(esc(caption)).append("</div>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	/**
	 * Menghitung dan memformat umur dalam tahun dari tanggal lahir.
	 *
	 * <p>Menghitung selisih tahun antara tanggal lahir dan tanggal saat ini, dengan
	 * koreksi jika hari ulang tahun tahun ini belum lewat ({@code DAY_OF_YEAR}
	 * saat ini &lt; hari lahir → umur dikurangi 1). Mengembalikan format "N tahun"
	 * atau string kosong jika tanggal lahir null atau hasilnya negatif.</p>
	 *
	 * @param tanggalLahir tanggal lahir; boleh {@code null} (mengembalikan "")
	 * @return string umur, mis. {@code "17 tahun"}, atau {@code ""} jika tidak dapat dihitung
	 */
	public static String umur(Date tanggalLahir) {
		if (tanggalLahir == null) {
			return "";
		}
		try {
			Calendar lahir = Calendar.getInstance();
			lahir.setTime(tanggalLahir);
			Calendar kini = Calendar.getInstance();
			int tahun = kini.get(Calendar.YEAR) - lahir.get(Calendar.YEAR);
			if (kini.get(Calendar.DAY_OF_YEAR) < lahir.get(Calendar.DAY_OF_YEAR)) {
				tahun--;
			}
			return tahun < 0 ? "" : tahun + " tahun";
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * Memformat tanggal sebagai string dengan format {@code dd-MM-yyyy}.
	 *
	 * <p>Menggunakan {@code SimpleDateFormat} dengan pola {@code "dd-MM-yyyy"}.
	 * Mengembalikan string kosong jika tanggal null atau format gagal.</p>
	 *
	 * @param date tanggal yang akan diformat; boleh {@code null}
	 * @return string tanggal dalam format "dd-MM-yyyy", atau {@code ""} jika null/gagal
	 */
	public static String tanggal(Date date) {
		if (date == null) {
			return "";
		}
		try {
			return new SimpleDateFormat("dd-MM-yyyy").format(date);
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * Memformat tempat lahir, tanggal lahir, dan umur dalam satu kalimat singkat.
	 *
	 * <p>Menghasilkan format seperti "Bandung, 12-06-1995 (29 tahun)". Komponen yang
	 * kosong (tempat kosong, tanggal null, umur tidak bisa dihitung) dilewati secara
	 * otomatis. Jika semua kosong, mengembalikan string kosong.</p>
	 *
	 * @param tempat tempat lahir; boleh null/kosong (dilewati)
	 * @param lahir  tanggal lahir; boleh null (tanggal dan umur dilewati)
	 * @return string TTL terformat, mis. {@code "Bandung, 12-06-1995 (29 tahun)"},
	 *         atau {@code ""} jika semua komponen kosong
	 */
	public static String ttl(String tempat, Date lahir) {
		String t = notBlank(tempat) ? tempat.trim() : "";
		String tgl = tanggal(lahir);
		String u = umur(lahir);
		StringBuffer sb = new StringBuffer();
		if (t.length() > 0) {
			sb.append(t);
		}
		if (tgl.length() > 0) {
			sb.append(sb.length() > 0 ? ", " : "").append(tgl);
		}
		if (u.length() > 0) {
			sb.append(" (").append(u).append(")");
		}
		return sb.toString();
	}

	/**
	 * Mengambil nilai {@code getNama()} dari objek relasi Hibernate dengan aman via refleksi.
	 *
	 * <p><b>Tujuan:</b> Membantu mengambil nama dari objek relasi (mis. Agama, JenisGuru,
	 * StatusKepegawaian) tanpa perlu casting dan tanpa import kelas tersebut, sehingga kode
	 * generik di ProfileUiHelper tidak perlu bergantung pada entitas spesifik.</p>
	 *
	 * <p><b>Cara kerja:</b> Mengambil metode {@code getNama} via refleksi Java dan
	 * meng-invoke-nya. Exception (NoSuchMethodException jika entitas tidak punya getNama,
	 * atau InvocationTargetException) diswallow dan mengembalikan string kosong.</p>
	 *
	 * @param relasi objek entitas relasi yang memiliki method {@code getNama()}; boleh null
	 * @return nilai {@code getNama()}, atau {@code ""} jika null atau tidak ada method tersebut
	 */
	public static String nama(Object relasi) {
		if (relasi == null) {
			return "";
		}
		try {
			Object value = relasi.getClass().getMethod("getNama", new Class[0]).invoke(relasi, new Object[0]);
			return value == null ? "" : String.valueOf(value);
		} catch (Exception e) {
			return "";
		}
	}

	/* ================= Query ringkas untuk kartu angka admin ================= */

	/**
	 * Menghitung jumlah baris (COUNT) sebuah entitas Hibernate dengan filter opsional.
	 *
	 * <p><b>Tujuan:</b> Query COUNT yang aman untuk dipakai dari mana saja, membuka
	 * sesi sendiri via {@code openSession()}, sehingga tidak bergantung pada
	 * {@code currentSession()} yang mungkin sudah tidak aktif di konteks rendering profil.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Membuka sesi via {@code HibernateUtil.getSessionFactory().openSession()}.</li>
	 *   <li>Membuat Criteria untuk {@code clazz}.</li>
	 *   <li>Menambahkan filter yang tidak null dari array {@code filters}.</li>
	 *   <li>Menerapkan proyeksi {@code Projections.rowCount()}.</li>
	 *   <li>Mengembalikan hasil sebagai {@code long}; jika bukan Number → 0.</li>
	 * </ol>
	 * Sesi selalu ditutup di blok {@code finally} via {@link #closeOpenSession(Session)}.</p>
	 *
	 * <p><b>Penanganan error:</b> Exception ditampilkan ke admin dan metode mengembalikan 0L,
	 * bukan melempar ulang exception — profil tetap dirender meski satu query gagal.</p>
	 *
	 * @param clazz   kelas entitas Hibernate yang akan dihitung; tidak boleh {@code null}
	 * @param filters array Criterion Hibernate untuk filter; boleh null atau berisi null
	 *                per elemen (null diabaikan)
	 * @return jumlah baris yang memenuhi filter, atau 0 jika gagal
	 */
	public static long hitung(Class<?> clazz, Criterion[] filters) {
		Session session = null;
		try {
			session = ais.database.hibernate.HibernateUtil.getSessionFactory().openSession();
			Criteria criteria = session.createCriteria(clazz);
			if (filters != null) {
				for (int i = 0; i < filters.length; i++) {
					if (filters[i] != null) {
						criteria.add(filters[i]);
					}
				}
			}
			criteria.setProjection(Projections.rowCount());
			Object value = criteria.uniqueResult();
			return value instanceof Number ? ((Number) value).longValue() : 0L;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return 0L;
		} finally {
			closeOpenSession(session);
		}
	}

	/**
	 * Mengelompokkan (GROUP BY) jumlah baris berdasarkan satu properti entitas Hibernate.
	 *
	 * <p><b>Tujuan:</b> Menghasilkan distribusi nilai suatu properti — misalnya komposisi
	 * jenis kelamin siswa ({@code groupBy jenisKelamin}) atau distribusi status mahasiswa.
	 * Hasil diurutkan dari jumlah terbanyak dan dapat dibatasi jumlah barisnya.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Membuka sesi via {@code openSession()}.</li>
	 *   <li>Membuat Criteria untuk {@code clazz} + menambahkan filters tidak null.</li>
	 *   <li>Menerapkan proyeksi GROUP BY + COUNT:
	 *       {@code Projections.groupProperty(property)} + {@code Projections.rowCount()}.</li>
	 *   <li>Mengumpulkan hasil ke list {@code Object[]} (elemen 0 = label, elemen 1 = jumlah).</li>
	 *   <li>Mengurutkan hasil secara descending berdasarkan jumlah via {@code Comparator}.</li>
	 *   <li>Jika {@code maxBaris > 0}, memotong hasil ke N baris pertama.</li>
	 * </ol>
	 * Sesi selalu ditutup di blok {@code finally}.</p>
	 *
	 * <p><b>Penanganan error:</b> Exception ditampilkan ke admin; mengembalikan list kosong
	 * (bukan null) sehingga pemanggil tidak perlu null-check.</p>
	 *
	 * @param clazz    kelas entitas Hibernate; tidak boleh null
	 * @param property nama properti untuk GROUP BY (mis. "jenisKelamin"); tidak boleh null
	 * @param filters  filter tambahan; boleh null atau berisi null per elemen
	 * @param maxBaris batas jumlah baris hasil; 0 atau negatif berarti tidak ada batas
	 * @return list tidak pernah null; setiap elemen adalah {@code Object[]{label, jumlah}}
	 *         diurutkan dari jumlah terbesar
	 */
	@SuppressWarnings("unchecked")
	public static List<Object[]> kelompok(Class<?> clazz, String property, Criterion[] filters, int maxBaris) {
		List<Object[]> hasil = new ArrayList<Object[]>();
		Session session = null;
		try {
			session = ais.database.hibernate.HibernateUtil.getSessionFactory().openSession();
			Criteria criteria = session.createCriteria(clazz);
			if (filters != null) {
				for (int i = 0; i < filters.length; i++) {
					if (filters[i] != null) {
						criteria.add(filters[i]);
					}
				}
			}
			criteria.setProjection(Projections.projectionList().add(Projections.groupProperty(property))
					.add(Projections.rowCount()));
			List<Object[]> rows = criteria.list();
			if (rows != null) {
				for (int i = 0; i < rows.size(); i++) {
					hasil.add(rows.get(i));
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeOpenSession(session);
		}
		java.util.Collections.sort(hasil, new java.util.Comparator<Object[]>() {
			public int compare(Object[] a, Object[] b) {
				double da = asDouble(a[1]);
				double db = asDouble(b[1]);
				return da > db ? -1 : (da < db ? 1 : 0);
			}
		});
		if (maxBaris > 0 && hasil.size() > maxBaris) {
			return new ArrayList<Object[]>(hasil.subList(0, maxBaris));
		}
		return hasil;
	}
}
