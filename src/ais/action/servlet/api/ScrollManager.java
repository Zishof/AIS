package ais.action.servlet.api;

import java.util.ArrayList;
import java.util.List;

public class ScrollManager<T> {

	private List<T> fullList;
	private int currentStartIndex; // Posisi paling atas saat ini
	private int viewportSize; // Berapa banyak item yang tampil di layar

	public ScrollManager(List<T> fullList, int viewportSize) {
		this.fullList = fullList;
		this.viewportSize = viewportSize;
		this.currentStartIndex = 0; // Mulai dari paling atas
	}

	/**
	 * Method untuk melakukan scroll dan mendapatkan data baru. Menggunakan Java 1.7
	 * Syntax.
	 */
	public List<T> onScroll(int scrollUp, int scrollDown) {
		List<T> newItems = new ArrayList<T>();

		// Validasi data kosong
		if (fullList == null || fullList.isEmpty()) {
			return newItems;
		}

		int totalData = fullList.size();

		// --- LOGIKA SCROLL DOWN ---
		if (scrollDown > 0) {
			// Hitung index baru
			int oldEndIndex = currentStartIndex + viewportSize;
			int potentialNewStart = currentStartIndex + scrollDown;

			// Batasi agar tidak melebihi jumlah data (Max index yang mungkin)
			int maxStart = totalData - viewportSize;
			if (maxStart < 0)
				maxStart = 0; // Handle jika data lebih sedikit dari viewport

			int actualNewStart = Math.min(potentialNewStart, maxStart);

			// Jika posisi tidak berubah (sudah mentok bawah), return list kosong
			if (actualNewStart == currentStartIndex) {
				return newItems;
			}

			// Hitung range data BARU yang muncul di bawah
			// Data baru dimulai dari posisi 'oldEndIndex' sampai batas bawah view baru
			int newEndIndex = actualNewStart + viewportSize;

			// Safety check untuk subList
			int safeStart = Math.min(oldEndIndex, totalData);
			int safeEnd = Math.min(newEndIndex, totalData);

			if (safeStart < safeEnd) {
				newItems.addAll(fullList.subList(safeStart, safeEnd));
			}

			// Update posisi saat ini
			currentStartIndex = actualNewStart;
		}

		// --- LOGIKA SCROLL UP ---
		else if (scrollUp > 0) {
			// Hitung index baru
			int potentialNewStart = currentStartIndex - scrollUp;

			// Batasi agar tidak kurang dari 0
			int actualNewStart = Math.max(0, potentialNewStart);

			// Jika posisi tidak berubah (sudah mentok atas), return list kosong
			if (actualNewStart == currentStartIndex) {
				return newItems;
			}

			// Hitung range data BARU yang muncul di atas
			// Data baru adalah dari 'actualNewStart' sampai sebelum 'currentStartIndex'
			// lama
			int safeStart = actualNewStart;
			int safeEnd = currentStartIndex;

			if (safeStart < safeEnd) {
				newItems.addAll(fullList.subList(safeStart, safeEnd));
			}

			// Update posisi saat ini
			currentStartIndex = actualNewStart;
		}

		return newItems;
	}

	// Helper untuk melihat posisi saat ini (untuk debug)
	public int getCurrentStartIndex() {
		return currentStartIndex;
	}
}
