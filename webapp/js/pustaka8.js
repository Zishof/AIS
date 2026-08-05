document.addEventListener('DOMContentLoaded', function () {
    // Sample book data (replace with your actual data)
    /*
	const books = [
        {
            id: 1,
            title: "Harry Potter and the Sorcerer's Stone",
            author: "J.K. Rowling",
            publisher: "Scholastic",
            year: 1997,
            description: "The first book in the Harry Potter series.",
            classification: "Fiction",
            cover: "https://m.media-amazon.com/images/I/51HSkTKlauL._SL160_.jpg",
            downloadLink: "#",
            readLink: "#",
            borrowHistory: [
                { date: "2024-01-01", returnDate: "2024-01-15", barcode: "BK001" },
                { date: "2024-02-01", returnDate: "2024-02-15", barcode: "BK001" }
            ]
        },
        {
            id: 2,
            title: "The Lord of the Rings",
            author: "J.R.R. Tolkien",
            publisher: "Allen & Unwin",
            year: 1954,
            description: "An epic high-fantasy novel.",
            classification: "Fiction",
            cover: "https://m.media-amazon.com/images/I/51ErwAKjQ0L._SL160_.jpg",
            downloadLink: "#",
            readLink: "#",
            borrowHistory: [
                { date: "2024-03-01", returnDate: "2024-03-15", barcode: "BK002" },
                { date: "2024-04-01", returnDate: "2024-04-15", barcode: "BK002" }
            ]
        },
        {
            id: 3,
            title: "Pride and Prejudice",
            author: "Jane Austen",
            publisher: "T. Egerton",
            year: 1813,
            description: "A romantic novel of manners.",
            classification: "Fiction",
            cover: "https://m.media-amazon.com/images/I/51w7SjjekPL._SL160_.jpg",
            downloadLink: "#",
            readLink: "#",
            borrowHistory: [
                { date: "2024-05-01", returnDate: "2024-05-15", barcode: "BK003" },
                { date: "2024-06-01", returnDate: "2024-06-15", barcode: "BK003" }
            ]
        },
        {
            id: 4,
            title: "The Hobbit",
            author: "J.R.R. Tolkien",
            publisher: "George Allen & Unwin",
            year: 1937,
            description: "A children's fantasy novel.",
            classification: "Fiction",
            cover: "https://m.media-amazon.com/images/I/51Zymoq7kJL._SL160_.jpg",
            downloadLink: "#",
            readLink: "#",
            borrowHistory: [
                { date: "2024-07-01", returnDate: "2024-07-15", barcode: "BK004" },
                { date: "2024-08-01", returnDate: "2024-08-15", barcode: "BK004" }
            ]
        },
        {
            id: 5,
            title: "To Kill a Mockingbird",
            author: "Harper Lee",
            publisher: "J. B. Lippincott & Co.",
            year: 1960,
            description: "A novel set in the American South.",
            classification: "Fiction",
            cover: "https://m.media-amazon.com/images/I/51J6mJ4VqRL._SL160_.jpg",
            downloadLink: "#",
            readLink: "#",
            borrowHistory: [
                { date: "2024-09-01", returnDate: "2024-09-15", barcode: "BK005" },
                { date: "2024-10-01", returnDate: "2024-10-15", barcode: "BK005" }
            ]
        },
        {
            id: 6,
            title: "1984",
            author: "George Orwell",
            publisher: "Secker & Warburg",
            year: 1949,
            description: "A dystopian novel.",
            classification: "Fiction",
            cover: "https://m.media-amazon.com/images/I/51kwK693HqL._SL160_.jpg",
            downloadLink: "#",
            readLink: "#",
            borrowHistory: [
                { date: "2024-11-01", returnDate: "2024-11-15", barcode: "BK006" },
                { date: "2024-12-01", returnDate: "2024-12-15", barcode: "BK006" }
            ]
        },
        {
            id: 7,
            title: "The Great Gatsby",
            author: "F. Scott Fitzgerald",
            publisher: "Charles Scribner's Sons",
            year: 1925,
            description: "A novel about the Roaring Twenties.",
            classification: "Fiction",
            cover: "https://m.media-amazon.com/images/I/51VV53KLASL._SL160_.jpg",
            downloadLink: "#",
            readLink: "#",
            borrowHistory: [
                { date: "2025-01-01", returnDate: "2025-01-15", barcode: "BK007" },
                { date: "2025-02-01", returnDate: "2025-02-15", barcode: "BK007" }
            ]
        },
        {
            id: 8,
            title: "One Hundred Years of Solitude",
            author: "Gabriel García Márquez",
            publisher: "Harper & Row",
            year: 1967,
            description: "A multi-generational story of the Buendía family.",
            classification: "Fiction",
            cover: "https://m.media-amazon.com/images/I/51wK9yWXREL._SL160_.jpg",
            downloadLink: "#",
            readLink: "#",
            borrowHistory: [
                { date: "2025-03-01", returnDate: "2025-03-15", barcode: "BK008" },
                { date: "2025-04-01", returnDate: "2025-04-15", barcode: "BK008" }
            ]
        },
        {
            id: 9,
            title: "Moby Dick",
            author: "Herman Melville",
            publisher: "Richard Bentley",
            year: 1851,
            description: "The story of Captain Ahab's obsessive quest to hunt the white whale.",
            classification: "Fiction",
            cover: "https://m.media-amazon.com/images/I/419PTpwqtjL._SL160_.jpg",
            downloadLink: "#",
            readLink: "#",
            borrowHistory: [
                { date: "2025-05-01", returnDate: "2025-05-15", barcode: "BK009" },
                { date: "2025-06-01", returnDate: "2025-06-15", barcode: "BK009" }
            ]
        },
        {
            id: 10,
            title: "War and Peace",
            author: "Leo Tolstoy",
            publisher: "The Russian Messenger",
            year: 1869,
            description: "An epic novel about the impact of the Napoleonic era on Tsarist society.",
            classification: "Fiction",
            cover: "https://m.media-amazon.com/images/I/41K5Vj5Y-ZL._SL160_.jpg",
            downloadLink: "#",
            readLink: "#",
            borrowHistory: [
                { date: "2025-07-01", returnDate: "2025-07-15", barcode: "BK010" },
                { date: "2025-08-01", returnDate: "2025-08-15", barcode: "BK010" }
            ]
        },
        {
            id: 11,
            title: "The Odyssey",
            author: "Homer",
            publisher: "Unknown",
            year: "8th century BC",
            description: "One of the oldest works of literature still widely read by modern audiences.",
            classification: "Poetry",
            cover: "https://m.media-amazon.com/images/I/41WMc8EnesL._SL160_.jpg",
            downloadLink: "#",
            readLink: "#",
            borrowHistory: [
                { date: "2025-09-01", returnDate: "2025-09-15", barcode: "BK011" },
                { date: "2025-10-01", returnDate: "2025-10-15", barcode: "BK011" }
            ]
        },
        {
            id: 12,
            title: "Hamlet",
            author: "William Shakespeare",
            publisher: "Unknown",
            year: 1603,
            description: "A tragedy by William Shakespeare.",
            classification: "Drama",
            cover: "https://m.media-amazon.com/images/I/41M9-2VZTlL._SL160_.jpg",
            downloadLink: "#",
            readLink: "#",
            borrowHistory: [
                { date: "2025-11-01", returnDate: "2025-11-15", barcode: "BK012" },
                { date: "2025-12-01", returnDate: "2025-12-15", barcode: "BK012" }
            ]
        },
        {
            id: 13,
            title: "The Divine Comedy",
            author: "Dante Alighieri",
            publisher: "Unknown",
            year: 1320,
            description: "An epic poem by Dante Alighieri.",
            classification: "Poetry",
            cover: "https://m.media-amazon.com/images/I/51RQg5H76qL._SL160_.jpg",
            downloadLink: "#",
            readLink: "#",
            borrowHistory: [
                { date: "2026-01-01", returnDate: "2026-01-15", barcode: "BK013" },
                { date: "2026-02-01", returnDate: "2026-02-15", barcode: "BK013" }
            ]
        },
        {
            id: 14,
            title: "Don Quixote",
            author: "Miguel de Cervantes",
            publisher: "Francisco de Robles",
            year: 1605,
            description: "A Spanish novel by Miguel de Cervantes.",
            classification: "Fiction",
            cover: "https://m.media-amazon.com/images/I/51eqmUuTqKL._SL160_.jpg",
            downloadLink: "#",
            readLink: "#",
            borrowHistory: [
                { date: "2026-03-01", returnDate: "2026-03-15", barcode: "BK014" },
                { date: "2026-04-01", returnDate: "2026-04-15", barcode: "BK014" }
            ]
        },
        {
            id: 15,
            title: "The Catcher in the Rye",
            author: "J. D. Salinger",
            publisher: "Little, Brown and Company",
            year: 1951,
            description: "A novel about teenage angst and alienation.",
            classification: "Fiction",
            cover: "https://m.media-amazon.com/images/I/51wWw9jY-OL._SL160_.jpg",
            downloadLink: "#",
            readLink: "#",
            borrowHistory: [
                { date: "2026-05-01", returnDate: "2026-05-15", barcode: "BK015" },
                { date: "2026-06-01", returnDate: "2026-06-15", barcode: "BK015" }
            ]
        },
        {
            id: 16,
            title: "The Little Prince",
            author: "Antoine de Saint-Exupéry",
            publisher: "Reynal & Hitchcock",
            year: 1943,
            description: "A novella and the most famous work of French aristocrat.",
            classification: "Fiction",
            cover: "https://m.media-amazon.com/images/I/71EAdQ9wJdL._SL160_.jpg",
            downloadLink: "#",
            readLink: "#",
            borrowHistory: [
                { date: "2026-07-01", returnDate: "2026-07-15", barcode: "BK016" },
                { date: "2026-08-01", returnDate: "2026-08-15", barcode: "BK016" }
            ]
        },
        {
            id: 17,
            title: "And Then There Were None",
            author: "Agatha Christie",
            publisher: "Collins Crime Club",
            year: 1939,
            description: "A mystery novel by Agatha Christie.",
            classification: "Mystery",
            cover: "https://m.media-amazon.com/images/I/417zw1F5j6L._SL160_.jpg",
            downloadLink: "#",
            readLink: "#",
            borrowHistory: [
                { date: "2026-09-01", returnDate: "2026-09-15", barcode: "BK017" },
                { date: "2026-10-01", returnDate: "2026-10-15", barcode: "BK017" }
            ]
        },
        {
            id: 18,
            title: "Dream of the Red Chamber",
            author: "Cao Xueqin",
            publisher: "Unknown",
            year: 1791,
            description: "Considered to be one of China's Four Great Classical Novels.",
            classification: "Fiction",
            cover: "https://m.media-amazon.com/images/I/51u1ex1jXJL._SL160_.jpg",
            downloadLink: "#",
            readLink: "#",
            borrowHistory: [
                { date: "2026-11-01", returnDate: "2026-11-15", barcode: "BK018" },
                { date: "2026-12-01", returnDate: "2026-12-15", barcode: "BK018" }
            ]
        },
        {
            id: 19,
            title: "The Lion, the Witch and the Wardrobe",
            author: "C. S. Lewis",
            publisher: "Geoffrey Bles",
            year: 1950,
            description: "A fantasy novel for children.",
            classification: "Fiction",
            cover: "https://m.media-amazon.com/images/I/51j4P9uJVEL._SL160_.jpg",
            downloadLink: "#",
            readLink: "#",
            borrowHistory: [
                { date: "2027-01-01", returnDate: "2027-01-15", barcode: "BK019" },
                { date: "2027-02-01", returnDate: "2027-02-15", barcode: "BK019" }
            ]
        },
        {
            id: 20,
            title: "The Da Vinci Code",
            author: "Dan Brown",
            publisher: "Doubleday",
            year: 2003,
            description: "A mystery thriller novel.",
            classification: "Mystery",
            cover: "https://m.media-amazon.com/images/I/51j57FpMDqL._SL160_.jpg",
            downloadLink: "#",
            readLink: "#",
            borrowHistory: [
                { date: "2027-03-01", returnDate: "2027-03-15", barcode: "BK020" },
                { date: "2027-04-01", returnDate: "2027-04-15", barcode: "BK020" }
            ]
        }
    ];
	*/
    const bookList = document.getElementById('bookList');
    const searchInput = document.getElementById('searchInput');
    const searchButton = document.getElementById('searchButton');
    const bookDetailContent = document.getElementById('bookDetailContent');
    const pagination = document.getElementById('pagination');
    const booksPerPage = 8;
    let currentPage = 1;
    let filteredBooks = [...books]; // Start with all books

    // Function to display books for the current page
    function displayBooks(booksToDisplay, page) {
        bookList.innerHTML = ''; // Clear existing book list
        const startIndex = (page - 1) * booksPerPage;
        const endIndex = startIndex + booksPerPage;
        const booksForPage = booksToDisplay.slice(startIndex, endIndex);

        booksForPage.forEach(book => {
            const bookItem = document.createElement('div');
            bookItem.classList.add('col-md-6', 'col-lg-3', 'book-item');
            bookItem.innerHTML = `
                <div class="d-flex align-items-center">
                    <img src="${book.cover}" alt="${book.title}" class="book-cover">
                    <div class="book-info">
                        <h5>${book.title}</h5>
						<p><i class="fas fa-barcode"></i> ${book.isbn}</p>
                        <p><i class="fas fa-user"></i> ${book.author_simple}</p>
                        <p><i class="fas fa-building"></i> ${book.publisher}</p>
                        <button class="btn btn-sm btn-primary detail-button" data-book-id="${book.id}">
                            <i class="fas fa-info-circle"></i> Detail
                        </button>
                    </div>
                </div>
            `;
            bookList.appendChild(bookItem);
        });

        // Add event listeners to detail buttons
        const detailButtons = document.querySelectorAll('.detail-button');
        detailButtons.forEach(button => {
            button.addEventListener('click', function () {
                const bookId = parseInt(this.dataset.bookId);
                showBookDetail(bookId);
            });
        });
    }

    // Function to display pagination links
    function displayPagination(booksToPaginate) {
        pagination.innerHTML = ''; // Clear existing pagination
        const totalPages = Math.ceil(booksToPaginate.length / booksPerPage);

        for (let i = 1; i <= totalPages; i++) {
            const pageItem = document.createElement('li');
            pageItem.classList.add('page-item');
            if (i === currentPage) {
                pageItem.classList.add('active');
            }
            pageItem.innerHTML = `<button class="page-link" data-page="${i}">${i}</button>`;
            pagination.appendChild(pageItem);
        }

        // Add event listeners to page links
        const pageLinks = document.querySelectorAll('.page-link');
        pageLinks.forEach(link => {
            link.addEventListener('click', function () {
                currentPage = parseInt(this.dataset.page);
                displayBooks(filteredBooks, currentPage);
                updateActivePage();
            });
        });
    }

    // Function to update active page in pagination
    function updateActivePage() {
        const pageLinks = document.querySelectorAll('.page-item');
        pageLinks.forEach(link => link.classList.remove('active'));
        const activePage = document.querySelector(`.page-item:nth-child(${currentPage})`);
        if (activePage) {
            activePage.classList.add('active');
        }
    }

    // Function to show book detail in modal
    function showBookDetail(bookId) {
        const book = books.find(b => b.id === bookId);
        if (book) {
            bookDetailContent.innerHTML = `
                <div class="row">
                    <div class="col-md-4">
                        <img src="${book.cover}" alt="${book.title}" class="img-fluid">
                    </div>
                    <div class="col-md-8">
                        <h3>${book.title}</h3>
						<p><i class="fas fa-barcode"></i> <strong>ISBN:</strong> ${book.isbn}</p>
                        <p><i class="fas fa-user"></i> <strong>Pengarang:</strong> ${book.author}</p>
                        <p><i class="fas fa-building"></i> <strong>Penerbit:</strong> ${book.publisher}</p>
                        <p><i class="fas fa-calendar-alt"></i> <strong>Tahun:</strong> ${book.year}</p>
                        <p><i class="fas fa-book"></i> <strong>Klasifikasi:</strong> ${book.classification}</p>
                        <p><i class="fas fa-file-alt"></i> <strong>Deskripsi:</strong> ${book.description}</p>
                        ${book.downloadLink ? `<p><i class="fas fa-download"></i> <a href="${book.downloadLink}">Download Ebook</a></p>` : ''}
                        ${book.readLink ? `<p><i class="fas fa-book-open"></i> <a href="${book.readLink}">Baca Buku</a></p>` : ''}
                        <h4>Riwayat Peminjaman</h4>
                        <div class="table-responsive">
                            <table class="table table-bordered">
                                <thead>
                                    <tr>
                                        <th>Tanggal Pinjam</th>
                                        <th>Tanggal Kembali</th>
                                        <th>Kode Barcode</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${book.borrowHistory.map(item => `
                                        <tr>
                                            <td>${item.date}</td>
                                            <td>${item.returnDate}</td>
                                            <td>${item.barcode}</td>
                                        </tr>
                                    `).join('')}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            `;
            const bookDetailModal = new bootstrap.Modal(document.getElementById('bookDetailModal'));
            bookDetailModal.show();
        }
    }

    // Function to filter books based on search input
    function filterBooks(searchTerm) {
        searchTerm = searchTerm.toLowerCase();
        filteredBooks = books.filter(book => {
            return (
                book.title.toLowerCase().includes(searchTerm) ||
                book.author.toLowerCase().includes(searchTerm) ||
                book.publisher.toLowerCase().includes(searchTerm) ||
                book.classification.toLowerCase().includes(searchTerm)
            );
        });
        currentPage = 1; // Reset to first page after filtering
        displayBooks(filteredBooks, currentPage);
        displayPagination(filteredBooks);
    }

    // Event listener for search button
    searchButton.addEventListener('click', function () {
        const searchTerm = searchInput.value;
        filterBooks(searchTerm);
    });

    // Event listener for search input (on enter key)
    searchInput.addEventListener('keyup', function (event) {
        if (event.key === 'Enter') {
            const searchTerm = searchInput.value;
            filterBooks(searchTerm);
        }
    });

    // Initial display
    displayBooks(filteredBooks, currentPage);
    displayPagination(filteredBooks);
    updateActivePage();

    // Automatic page transition (every 5 seconds)
    let pageTransitionInterval = setInterval(function () {
        if (currentPage < Math.ceil(filteredBooks.length / booksPerPage)) {
            currentPage++;
        } else {
            currentPage = 1; // Back to first page
        }
        displayBooks(filteredBooks, currentPage);
        updateActivePage();
    }, 5000);

    // Pause automatic transition on mouse hover
    document.addEventListener('mousemove', function () {
        clearInterval(pageTransitionInterval);
        pageTransitionInterval = setInterval(function () {
            if (currentPage < Math.ceil(filteredBooks.length / booksPerPage)) {
                currentPage++;
            } else {
                currentPage = 1; // Back to first page
            }
            displayBooks(filteredBooks, currentPage);
            updateActivePage();
        }, 5000);
    });
});