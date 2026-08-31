package ais.action.master.rab.util;

import ais.action.master.rab.helper.AmbilDataWorkspaceBanbox;

/**
 * Kontrak penyedia sumber data {@link AmbilDataWorkspaceBanbox} (banbox lookup workspace) pada
 * modul RAB. Memisahkan titik pemanggilan dari cara sumber data tersebut dikonstruksi/dikonfigurasi.
 */
public interface WorkspaceSelecter {

	/** @return instans banbox lookup workspace yang siap dipakai */
	public AmbilDataWorkspaceBanbox select();

}
