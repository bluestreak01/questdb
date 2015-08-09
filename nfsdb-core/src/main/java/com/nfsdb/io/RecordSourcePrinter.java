/*
 *  _  _ ___ ___     _ _
 * | \| | __/ __| __| | |__
 * | .` | _|\__ \/ _` | '_ \
 * |_|\_|_| |___/\__,_|_.__/
 *
 * Copyright (c) 2014-2015. The NFSdb project and its contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nfsdb.io;

import com.nfsdb.exceptions.JournalException;
import com.nfsdb.exceptions.JournalRuntimeException;
import com.nfsdb.factory.JournalReaderFactory;
import com.nfsdb.io.sink.CharSink;
import com.nfsdb.ql.Record;
import com.nfsdb.ql.RecordCursor;
import com.nfsdb.ql.RecordMetadata;
import com.nfsdb.ql.RecordSource;
import com.nfsdb.utils.Dates;
import com.nfsdb.utils.Numbers;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings({"SF_SWITCH_NO_DEFAULT"})
public class RecordSourcePrinter {
    private final CharSink sink;
    private final char delimiter;

    public RecordSourcePrinter(CharSink sink) {
        this.sink = sink;
        this.delimiter = '\t';
    }

    public RecordSourcePrinter(CharSink sink, char delimiter) {
        this.sink = sink;
        this.delimiter = delimiter;
    }

    public void print(Record r, RecordMetadata m) {
        for (int i = 0, sz = m.getColumnCount(); i < sz; i++) {
            if (i > 0) {
                sink.put(delimiter);
            }
            printRecord(r, m, i);
        }
        sink.put("\n");
        sink.flush();
    }

    @SuppressFBWarnings({"EXS_EXCEPTION_SOFTENING_NO_CONSTRAINTS"})
    public void print(RecordSource<? extends Record> src) {
        try {
            printCursor(src.prepareCursor(null));
        } catch (JournalException e) {
            throw new JournalRuntimeException(e);
        }
    }


    @SuppressFBWarnings({"EXS_EXCEPTION_SOFTENING_NO_CONSTRAINTS"})
    public void print(RecordSource<? extends Record> src, JournalReaderFactory factory) {
        try {
            printCursor(src.prepareCursor(factory));
        } catch (JournalException e) {
            throw new JournalRuntimeException(e);
        }
    }

    public void printCursor(RecordCursor<? extends Record> src) {
        printCursor(src, false);
    }

    public void printCursor(RecordCursor<? extends Record> src, boolean header) {
        RecordMetadata metadata = src.getMetadata();
        if (header) {
            printHeader(metadata);
        }

        while (src.hasNext()) {
            print(src.next(), metadata);
        }
    }

    public void printHeader(RecordMetadata metadata) {
        for (int i = 0, n = metadata.getColumnCount(); i < n; i++) {
            if (i > 0) {
                sink.put(delimiter);
            }
            sink.put(metadata.getColumn(i).getName());
        }
        sink.put('\n');
    }

    private void printRecord(Record r, RecordMetadata m, int i) {
        switch (m.getColumn(i).getType()) {
            case DATE:
                Dates.appendDateTime(sink, r.getLong(i));
                break;
            case DOUBLE:
                Numbers.append(sink, r.getDouble(i), 12);
                break;
            case FLOAT:
                Numbers.append(sink, r.getFloat(i), 4);
                break;
            case INT:
                Numbers.append(sink, r.getInt(i));
                break;
            case STRING:
                r.getStr(i, sink);
                break;
            case SYMBOL:
                sink.put(r.getSym(i));
                break;
            case SHORT:
                Numbers.append(sink, r.getShort(i));
                break;
            case LONG:
                Numbers.append(sink, r.getLong(i));
                break;
            case BYTE:
                Numbers.append(sink, r.get(i));
                break;
            case BOOLEAN:
                sink.put(r.getBool(i) ? "true" : "false");
                break;
//                default:
//                    throw new JournalRuntimeException("Unsupported type: " + r.getColumnType(i));
        }
    }
}
