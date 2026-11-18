package com.quincaisoft.operations;

import com.quincaisoft.operations.application.Csv;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

/** Untrusted import input must obey the same limits in quoted and unquoted form. */
class CsvInputBoundaryTest {
    @Test void rejectsUnquotedNul() {
        assertThrows(IllegalArgumentException.class, () -> Csv.parse("name\nA\0B", ","));
    }

    @Test void rejectsQuotedNul() {
        assertThrows(IllegalArgumentException.class, () -> Csv.parse("name\n\"A\0B\"", ","));
    }

    @Test void acceptsFortyColumns() {
        String row = String.join(",", Collections.nCopies(40, "x"));
        assertEquals(40, Csv.parse(row + "\n" + row, ",").get(1).size());
    }

    @Test void rejectsFortyOneColumnsAtEndOfInput() {
        String row = String.join(",", Collections.nCopies(41, "x"));
        assertThrows(IllegalArgumentException.class, () -> Csv.parse(row + "\n" + row, ","));
    }

    @Test void rejectsFortyOneColumnsWithLineTerminator() {
        String row = String.join(";", Collections.nCopies(41, "\"x\""));
        assertThrows(IllegalArgumentException.class, () -> Csv.parse(row + "\r\n" + row + "\r\n", ";"));
    }

    @Test void trailingEmptyFieldCountsTowardTheLimit() {
        String forty = String.join(",", Collections.nCopies(40, "x"));
        for (String end : new String[]{"", "\n", "\r", "\r\n"}) {
            assertThrows(IllegalArgumentException.class, () -> Csv.parse("name\n" + forty + "," + end, ","));
        }
        assertEquals(40, Csv.parse("name\n" + String.join(",", Collections.nCopies(39, "x")) + ",", ",").get(1).size());
    }

    @Test void quotedNulIsRejectedAfterEscapedQuoteAndAcrossLines() {
        assertThrows(IllegalArgumentException.class, () -> Csv.parse("name\n\"A\"\"\0B\"", ","));
        assertThrows(IllegalArgumentException.class, () -> Csv.parse("name\n\"A\n\0B\"", ","));
    }

    @Test void preservesBomAndEmptyQuotedCells() {
        assertEquals("", Csv.parse("\ufeffname\r\n\"\"\r\n", ",").get(1).getFirst());
    }

    @Test void enforcesUtf8ByteLimitRatherThanCharacterCount() {
        String row = "é".repeat(1000) + "\n";
        assertThrows(IllegalArgumentException.class, () -> Csv.parse("name\n" + row.repeat(260), ","));
    }

    @Test void rejectsMalformedQuotesWithoutDiscardingTrailingText() {
        assertThrows(IllegalArgumentException.class, () -> Csv.parse("name\n\"value", ","));
        assertThrows(IllegalArgumentException.class, () -> Csv.parse("name\n\"value\"suffix", ","));
    }

    @Test void acceptsFiveHundredDataRowsAndRejectsNext() {
        assertEquals(501, Csv.parse("name\n" + "x\n".repeat(500), ",").size());
        assertThrows(IllegalArgumentException.class, () -> Csv.parse("name\n" + "x\n".repeat(501), ","));
    }

    @Test void fieldLimitAppliesInsideQuotes() {
        assertEquals(10000, Csv.parse("name\n\"" + "x".repeat(10000) + "\"", ",").get(1).getFirst().length());
        assertThrows(IllegalArgumentException.class, () -> Csv.parse("name\n\"" + "x".repeat(10001) + "\"", ","));
    }

    @Test void preservesFrenchQuotedNewlineAndEscapedQuotes() {
        assertEquals("Écrou, \"acier\"\nM8", Csv.parse("name\r\n\"Écrou, \"\"acier\"\"\nM8\"\r\n", ",").get(1).getFirst());
    }
}
