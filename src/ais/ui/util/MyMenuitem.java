package ais.ui.util;

import org.zkoss.zul.Menuitem;

import ais.common.Common;

/**
 * Komponen/konfigurasi ZK khusus AIS untuk my menuitem. Tipe ini membakukan default dan perilaku
 * tampilan di atas komponen induk supaya layar tidak mengulang konfigurasi widget yang sama.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Menuitem}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code String labelLokal}; mutasi data ({@code
 * setImage()}); operasi domain lain ({@code svgIcon()}, {@code svgIcon()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> setter dan helper mengubah state komponen ZK yang sedang terpasang pada desktop.
 * Gunakan pada event thread UI dan jangan membagikan instance antar session; aturan bisnis dan transaksi
 * persistence tetap harus didelegasikan ke action atau service pemanggil.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see Menuitem
 */
public class MyMenuitem extends Menuitem {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2772561761275196767L;
	private String labelLokal = null;

	public MyMenuitem() {
		super();
		setSclass("menu_item");
		// TODO Auto-generated constructor stub
	}

	public MyMenuitem(String prefix, String label, String src) {
		super(Common.getBahasaConfig(prefix, label), src);
		setSclass("menu_item");
		this.labelLokal = label;
	}

	public MyMenuitem(String label, String src) {
		super(Common.getBahasaConfig(label), src);
		setSclass("menu_item");
		this.labelLokal = label;
	}

	public MyMenuitem(String label) {
		super(Common.getBahasaConfig(label));
		setSclass("menu_item");
		this.labelLokal = label;
	}

	@Override
	public void setImage(String src) {
		String lbl = labelLokal == null || labelLokal.trim().isEmpty() ? getLabel() : labelLokal;
		String srcHower = MyMenuitem.svgIcon(lbl, src, false);
		super.setHoverImage(srcHower);
		src = MyMenuitem.svgIcon(lbl, src, true);
		super.setImage(src);
	}

	public static String svgIcon(String label, String image) {
		return svgIcon(label, image, true);
	}

	public static String svgIcon(String label, String image, boolean jikaSudahSvgKembali) {
//		if(true) {
//			return image;  
//		}

		if (jikaSudahSvgKembali) {
			if (image != null && image.toLowerCase().endsWith(".svg")) {
				return image;
			}
		}

		String icon = image;

		if (image != null && (label == null || label.isEmpty())) {
			if (image != null && image.endsWith("print.png") || image.endsWith("print.gif")) {
				icon = "/img/svg/printer.svg";
			} else if (image != null && image.endsWith("edit.png") || image.endsWith("edit.gif")) {
				icon = "/img/svg/edit-box-line.svg";
			} else if (image != null && image.endsWith("delete.png") || image.endsWith("delete.gif")) {
				icon = "/img/svg/trash.svg";
			} else if (image != null && image.endsWith("add.png") || image.endsWith("add.gif")) {
				icon = "/img/svg/addthis.svg";
			} else if (image != null && image.endsWith("copy.png") || image.endsWith("copy.gif")) {
				icon = "/img/svg/edit-copy.svg";
			} else if (image != null && image.endsWith("stock_data_edit_table.png")) {
				icon = "/img/svg/user-edit.svg";
			}

		} else if (label != null) {
			label = label.toLowerCase().trim();
			if (image == null) image = "";

			if (!label.isEmpty()) {

				if (label.endsWith(".jpeg") || label.endsWith(".jpg") || label.endsWith(".png")) {
					icon = "/img/svg/image.svg";
				}

				else if (label.endsWith(".doc") || label.endsWith(".docx")) {
					icon = "/img/svg/file-word.svg";
				}

				else if (label.endsWith(".xlsx") || label.endsWith(".xlsx")) {
					icon = "/img/svg/file-excel.svg";
				}

				else if (label.endsWith(".ppt") || label.endsWith(".pptx")) {
					icon = "/img/svg/file-ppt.svg";
				}

				else if (label.endsWith(".pdf")) {
					icon = "/img/svg/file-pdf.svg";
				}

				else if (label.contains("download")) {
					icon = "/img/svg/download.svg";
				} else if (label.contains("upload")) {
					icon = "/img/svg/upload.svg";
				}

				else if (label.contains("copy") || image.toLowerCase().contains("copy")) {
					icon = "/img/svg/edit-copy.svg";
				} else if (label.contains("hapus") || image.toLowerCase().contains("delete")) {
					icon = "/img/svg/trash.svg";
				}

				else if (label.contains("live") || image.toLowerCase().contains("online")) {
					icon = "/img/svg/user-group.svg";
				}

				else if (label.contains("ajukan")) {
					icon = "/img/svg/pencil-square.svg";
				}

				else if (label.contains("pengaturan pengguna") || label.contains("biodata")
						|| label.contains("profil")) {
					icon = "/img/svg/user.svg";
				} else if (label.contains("grup pengguna") || label.contains("peserta")) {
					icon = "/img/svg/users.svg";
				} else if (label.contains("hitung") || label.contains("proses") || label.contains("ekspor")) {
					icon = "/img/svg/process.svg";
				} else if (label.contains("sebelum") || label.contains("kembali")) {
					icon = "/img/svg/arrow-left-circle.svg";
				} else if (label.contains("lanjut") || label.contains("berikut")) {
					icon = "/img/svg/arrow-right-circle.svg";
				} else if (label.contains("kelompok") || label.contains("tgs.kel") || label.contains("absensi")
						|| label.contains("kehadiran")) {
					icon = "/img/svg/user-group.svg";
				} else if (label.contains("matakuliah") || label.contains("matapelajaran") || label.contains("cover")) {
					icon = "/img/svg/journal-bookmark-fill.svg";
				} else if (label.contains("dosen")) {
					icon = "/img/svg/user-business.svg";
				} else if (label.contains("guru")) {
					icon = "/img/svg/chalkboard-teacher-light.svg";
				} else if (label.contains("absen")) {
					icon = "/img/svg/fingerprint.svg";
				} else if (label.contains("android")) {
					icon = "/img/svg/coin.svg";
				} else if (label.contains("transaksi") || label.contains("tabung")) {
					icon = "/img/svg/transaction-order.svg";
				} else if (label.contains("qrcode")) {
					icon = "/img/svg/qrcode-scan.svg";
				} else if (label.contains("sekolah")) {
					icon = "/img/svg/student-thin.svg";
				} else if (label.contains("bayar") || label.contains("gaji") || label.contains("uang")) {
					icon = "/img/svg/cash.svg";
				} else if (label.contains("akun")) {
					icon = "/img/svg/account_tree.svg";
				} else if (label.contains("rekap")) {
					icon = "/img/svg/table.svg";
				} else if (label.contains("pengguna")) {
					icon = "/img/svg/user-circle.svg";
				} else if (label.contains("mobile")) {
					icon = "/img/svg/phone.svg";
				} else if (label.contains("android")) {
					icon = "/img/svg/android-logo-thin.svg";
				} else if (label.contains("apple")) {
					icon = "/img/svg/apple.svg";
				} else if (label.contains("reset password") || label.contains("buka kunci")) {
					icon = "/img/svg/unlock.svg";
				} else if (label.contains("info kegiatan") || label.contains("aktif") || label.contains("tugas")
						|| label.contains("jurnal")) {
					icon = "/img/svg/list-task.svg";
				} else if (label.contains("password") || label.contains("kunci")) {
					icon = "/img/svg/lock.svg";
				} else if (label.contains("home") || label.contains("beranda") || label.contains("bank")) {
					icon = "/img/svg/house.svg";
				} else if (label.contains("dashboard")) {
					icon = "/img/svg/dashboard-speed.svg";
				} else if (label.contains("mahasiswa") || label.contains("wisuda")) {
					icon = "/img/svg/user-graduate.svg";
				} else if (label.contains("pmb")) {
					icon = "/img/svg/student-duotone.svg";
				} else if (label.contains("blm vali") || label.contains("bentrok") || label.contains("alpa")) {
					icon = "/img/svg/warning-outline.svg";
				} else if (label.contains("setuju") || label.contains("dianggap") || label.contains("semua")
						|| label.contains("hadir yg")) {
					icon = "/img/svg/check2-all.svg";
				} else if (label.contains("siswa")) {
					icon = "/img/svg/address-book-thin.svg";
				} else if (label.contains("pengaturan konfigurasi") || label.contains("setup")
						|| label.contains("gen.")) {
					icon = "/img/svg/gear.svg";
				} else if (label.contains("konfigurasi")) {
					icon = "/img/svg/gear-six-duotone.svg";
				} else if (label.contains("jurusan") || label.contains("program studi") || label.contains("prodi")) {
					icon = "/img/svg/home_door.svg";
				} else if (label.contains("fakultas")) {
					icon = "/img/svg/home-alt.svg";
				} else if (label.contains("krs") || label.contains("perkuliahan") || label.contains("transkrip")
						|| label.contains("jenis") || label.contains("ipk") || label.contains("ips")
						|| label.contains("sks")) {
					icon = "/img/svg/card-checklist.svg";
				} else if (label.contains("ruang")) {
					icon = "/img/svg/house-door-fill.svg";
				} else if (label.contains("pengumuman") || label.contains("info")) {
					icon = "/img/svg/information-circle-outline.svg";
				} else if (label.contains("bahasa")) {
					icon = "/img/svg/flag.svg";
				} else if (label.contains("agenda")) {
					icon = "/img/svg/calendar-check.svg";
				} else if (label.contains("hari") || label.contains("jadwal")) {
					icon = "/img/svg/calendar2.svg";
				} else if (label.contains("bulan") || label.contains("bln") || label.contains("kalender")) {
					icon = "/img/svg/calendar3.svg";
				} else if (label.contains("minggu") || label.contains("mngg")) {
					icon = "/img/svg/calendar2-week.svg";
				} else if (label.contains("gambar")) {
					icon = "/img/svg/image.svg";
				} else if (label.contains("foto")) {
					icon = "/img/svg/user-box-line.svg";
				} else if (label.contains("audio") || label.contains("mp3")) {
					icon = "/img/svg/sound-on.svg";
				} else if (label.contains("video") || label.contains("mp4")) {
					icon = "/img/svg/camera-video.svg";
				} else if (label.contains("ambil") || label.contains("kegiatan")) {
					icon = "/img/svg/list-check.svg";
				} else if (label.contains("lihat")) {
					icon = "/img/svg/eye.svg";
				} else if (label.contains("hasil")) {
					icon = "/img/svg/task-line.svg";
				} else if (label.contains("format")) {
					icon = "/img/svg/list.svg";
				} else if (label.contains("simpan") || image.toLowerCase().contains("save")) {
					icon = "/img/svg/save-2-fill.svg";
				} else if (label.contains("batal")) {
					icon = "/img/svg/cancel_presentation.svg";
				} else if (label.contains("nilai") || label.contains("verif") || label.contains("ujian")
						|| label.contains("pilih")) {
					icon = "/img/svg/check-circled-outline.svg";
				} else if (label.contains("learning")) {
					icon = "/img/svg/book.svg";
				} else if (label.contains("prestasi")) {
					icon = "/img/svg/trophy.svg";
				} else if (label.contains("pustaka") || label.contains("koleksi") || label.contains("berita")) {
					icon = "/img/svg/books-thin.svg";
				} else if (label.contains("selesai") || label.contains("tutup")
						|| image.toLowerCase().contains("cancel")) {
					icon = "/img/svg/close-circle-line.svg";
				} else if (label.contains("refresh") || label.contains("sync") || label.contains("singkron")
						|| label.contains("proses") || label.contains("feeder")) {
					icon = "/img/svg/refresh-cw.svg";
				} else if (label.contains("search") || label.contains("cari")
						|| image.toLowerCase().contains("search")) {
					icon = "/img/svg/search.svg";
				} else if (label.contains("edit") || label.contains("ubah") || image.toLowerCase().contains("edit")) {
					icon = "/img/svg/edit-box-line.svg";
				} else if (label.contains("tambah") || image.toLowerCase().contains("add")
						|| label.toLowerCase().contains("baru") || label.toLowerCase().contains("buat")) {
					icon = "/img/svg/addthis.svg";
				} else if (label.contains("cetak") || image.toLowerCase().contains("print")) {
					icon = "/img/svg/printer.svg";
				} else if (label.contains("lampiran") || image.toLowerCase().contains("attachment")) {
					icon = "/img/svg/attachment-2.svg";
				} else if (label.contains("sejarah") || label.contains("history") || label.contains("linimasa")) {
					icon = "/img/svg/clock-history.svg";
				} else if (label.contains("keluar")) {
					icon = "/img/svg/power.svg";
				} else if (label.contains("diskusi") || label.contains("komen") || label.contains("pengumuman")
						|| label.contains("catatan")) {
					icon = "/img/svg/comment-2-text-line.svg";
				} else if (label.contains("aset")) {
					icon = "/img/svg/boxes.svg";
				} else if (label.contains("aset") || label.contains("satuan") || label.contains("kategori")) {
					icon = "/img/svg/boxes.svg";
				} else if (label.contains("akun")) {
					icon = "/img/svg/coin.svg";
				} else if (label.contains("data")) {
					icon = "/img/svg/data-all.svg";
				} else if (label.contains("bimbing")) {
					icon = "/img/svg/person-lines-fill.svg";
				} else if (label.contains("surat")) {
					icon = "/img/svg/file-earmark-text.svg";
				} else if (label.contains("dokumen")) {
					icon = "/img/svg/file-lines.svg";
				} else if (label.contains("posting") || label.contains("transfer")) {
					icon = "/img/svg/send.svg";
				} else if (label.contains("monitor") || label.contains("layar")) {
					icon = "/img/svg/desktop-light.svg";
				} else if (label.contains("manajemen")) {
					icon = "/img/svg/file-report.svg";
				} else if (label.contains("laporan") || label.contains("kurikulum") || label.contains("kbm")) {
					icon = "/img/svg/table-list.svg";
				} else if (label.contains("kinerja")) {
					icon = "/img/svg/medium_level.svg";
				} else if (label.contains("camera") || label.contains("kamera")) {
					icon = "/img/svg/camera.svg";
				} else if (label.contains("sidang")) {
					icon = "/img/svg/user-follow-line.svg";
				} else if (label.contains("kkn")) {
					icon = "/img/svg/user-group.svg";
				} else if (label.contains("pkl")) {
					icon = "/img/svg/chalkboard-user.svg";
				} else if (label.contains("pegawai")) {
					icon = "/img/svg/user-business.svg";
				} else if (label.contains("bantu")) {
					icon = "/img/svg/question-square.svg";
				} else if (label.contains("skripsi") || label.contains("tugas akhir") || label.contains("pelajaran")
						|| label.contains("thesis") || label.contains("ref.")) {
					icon = "/img/svg/journal-bookmark.svg";
				} else if (label.contains("grafik") || label.contains("dashboard") || label.contains("chart")) {
					icon = "/img/svg/chart-line-light.svg";
				} else if (label.equalsIgnoreCase("pa") || label.contains("pembimbing akademik")) {
					icon = "/img/svg/user-tie.svg";
				} else if (label.equalsIgnoreCase("form") || label.contains("format")) {
					icon = "/img/svg/form-one.svg";
				} else if (label.equalsIgnoreCase("paste") || label.contains("tempel")) {
					icon = "/img/svg/paste-clipboard.svg";
				} else if (label.equalsIgnoreCase("pilih") || label.equalsIgnoreCase("akses")) {
					icon = "/img/svg/journal-check.svg";
				} else if (label.equalsIgnoreCase("valid") || label.equalsIgnoreCase("diterima")) {
					icon = "/img/svg/check2.svg";
				} else if (label.contains("reset")) {
					icon = "/img/svg/reset_alt.svg";
				} else if (label.contains("pisah") || label.contains("split")) {
					icon = "/img/svg/view-split.svg";
				} else if (label.contains("bersihkan")) {
					icon = "/img/svg/eraser-light.svg";
				} else if (label.contains("keranjang") || label.contains("belanja") || label.contains("pesan")) {
					icon = "/img/svg/basket3.svg";
				} else if (label.contains("alur")) {
					icon = "/img/svg/arrow-return-right.svg";
				} else if (label.contains("sop")) {
					icon = "/img/svg/journal-arrow-up.svg";
				} else if (label.contains("item") || label.contains("golongan")) {
					icon = "/img/svg/boxes.svg";
				}
			}
		}

		return icon;
	}

}
