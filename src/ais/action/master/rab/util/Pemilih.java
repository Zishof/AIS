package ais.action.master.rab.util;

import java.util.List;

import ais.database.model.rab.Workspace;

/**
 * Kontrak dialog pemilihan satu {@link Workspace} dari daftar kandidat pada modul RAB, mis.
 * saat user harus memilih ruang kerja aktif di antara beberapa pilihan sebelum melanjutkan aksi.
 */
public interface Pemilih {

	/**
	 * Menampilkan dialog pemilihan workspace kepada user.
	 *
	 * @param judul      judul dialog pemilihan
	 * @param workspaces daftar workspace kandidat yang dapat dipilih
	 */
	public void pilih(String judul, List<Workspace> workspaces);

}
