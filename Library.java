package com.accenture.atc.javacore.m10threads;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

// Автор кода: Николай Медведев
// Благодарность за помощь в поиске решения: Виктору Лошманову (Java-разработчик) и Википедии
// JavaDoc я использую в своём коде не везде - простите мне эту мою маленькую слабость

/**
 * Потокобезопасная высоконагруженная библиотека.
 * Билиотека "принимает" книгу и "отдаёт" книгу.
 * Библиотека использует команду Compare-And-Swap для блокировки (с AtomicReference.compareAndExchange).
 * Не падает под высокой нагрузкой.
 * Джавовские мониторы или synchronized не используются.
 */

public class Library {

    private final AtomicReference<Book>[] library;

    public Library(int count) {

        library = new AtomicReference[count];

        while (count-- > 0) {
            library[count] = new AtomicReference<Book>(new Book(true, false, 0, count));
        }
    }

    public Book tryTakeBook(int bookId, boolean toReadingRoom) {

        if (bookId < 0 || bookId >= library.length) {
            return null;
        }

        AtomicReference<Book> atomic = library[bookId];
        Book book = atomic.get();
        if (book == null) return null;

        if (toReadingRoom != book.isReadingRoomOnly) {
            return null;
        }

        /* Мы будем брать книгу под CAS-блокировкой */
        do {
            book = atomic.get();

            if (book == null) {
                return null;
            }
        }
        /* Тут происходит сама попытка взятие блокировки */
        while (book != atomic.compareAndExchange(book, null));

        return new Book(book.isReadingRoomOnly, book.isBusy, ++book.theftCounter, book.bookId);
    }


    public boolean tryReturnBook(Book book) {

        if (book == null) {
            return false;
        }

        int bookId = book.bookId;

        Book newBook = new Book(book.isReadingRoomOnly, book.isBusy, --book.theftCounter, book.bookId);
        AtomicReference<Book> atomic = library[bookId];

        if (atomic.get() != null) {
            return false;
        }

        /* Возвращать книгу можно не под блокировкой */
        atomic.set(newBook);
        return true;
    }

    public int busyBookCount() {

        int busyBookCount = 0;
        busyBookCount += Arrays.stream(library)
                .filter(book -> book.get() == null)
                .count();
        return busyBookCount;
    }

    public int booksTheftCounterSum() {

        Integer theftCounter = 0;
        theftCounter += Arrays.stream(library)
                .filter(book -> book.get() != null)
                .map(book -> book.get().theftCounter)
                .reduce(Integer::sum)
                .get();
        return theftCounter;
    }

    public int getBookNumber() {

        return library.length;
    }

    public static class Book {

        final int bookId;
        final boolean isReadingRoomOnly;
        final boolean isBusy;
        /* Счетчик counter используется как дополнительная проверка */
        int theftCounter;

        public Book(boolean readingRoomOnly, boolean busy, int counter, int bookNumber) {

            this.isReadingRoomOnly = readingRoomOnly;
            this.isBusy = busy;
            this.theftCounter = counter;
            this.bookId = bookNumber;
        }
    }

    public static class LibraryExceptions extends Exception {

        public LibraryExceptions(String message) {
            super(message);
        }
    }
}