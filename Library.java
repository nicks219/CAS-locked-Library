package com.accenture.atc.javacore.m10threads;

import java.util.concurrent.atomic.AtomicReference;

// Автор кода: Николай Медведев
// Благодарность за помощь в поиске решения: Виктору Лошманову (Java-разработчик) и Википедии
// JavaDoc я использую в своём коде не везде - простите мне эту мою маленькую слабость

/**
 * Потокобезопасная неблокирующая высоконагруженная библиотека.
 * Билиотека "принимает" книгу и "отдаёт" книгу.
 * Библиотека использует правильно реализованный AtomicReference.compareAndExchange.
 * Не падает под высокой нагрузкой.
 * Мониторы или synchronized не используются вообще.
 */

public class Library {

    private static final int Zero = 0;
    private final AtomicReference<Book>[] library;

    public Library(int count) {

        library = new AtomicReference[count];

        while (count-- > Zero) {
            library[count] = new AtomicReference<Book>(new Book(true, false, Zero, count));
        }
    }

    public Book tryTakeBook(int bookNumber, boolean toReadingRoom) {

        if (bookNumber < Zero || bookNumber >= library.length) {
            return null;
        }

        Book book = library[bookNumber].get();
        if (book == null) return null;

        if (toReadingRoom != book.isReadingRoomOnly) {
            return null;
        }

        /* Мы будем брать книгу под CAS-блокировкой */
        while (true) {
            book = library[bookNumber].get();

            if (book == null) {
                return null;
            }

            /* Тут происходит сама попытка взятие блокировки */
            if (book == library[bookNumber].compareAndExchange(book, null)) {
                return new Book(book.isReadingRoomOnly, book.isBusy, ++book.theftCounter, book.bookNumber);
            }
        }
    }

    public boolean tryReturnBook(Book book) {

        if (book == null) {
            return false;
        }

        int bookNumber = book.bookNumber;

        Book newBook = new Book(book.isReadingRoomOnly, book.isBusy, --book.theftCounter, book.bookNumber);

        if (library[bookNumber].get() != null) {
            return false;
        }

        /* Возвращать книгу можно не под блокировкой */
        library[bookNumber].set(newBook);
        return true;
    }

    public int busyBookCount() {

        int busyBookCount = Zero;
        for (var book : library) {
            if (book.get() == null) busyBookCount++;
        }
        return busyBookCount;
    }

    public int booksZeroCounterSum() {

        int zeroCounter = Zero;
        for (var book : library) {
            if (book.get() == null) continue;
            zeroCounter += book.get().theftCounter;
        }
        return zeroCounter;
    }

    /* Immutable */
    public static class Book {

        final int bookNumber;
        final boolean isReadingRoomOnly;
        final boolean isBusy;
        /* Счетчик counter используется как дополнительная проверка */
        int theftCounter;

        public Book(boolean readingRoomOnly, boolean busy, int counter, int bookNumber) {

            this.isReadingRoomOnly = readingRoomOnly;
            this.isBusy = busy;
            this.theftCounter = counter;
            this.bookNumber = bookNumber;
        }
    }
}