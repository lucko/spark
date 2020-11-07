/*
 * This file is part of text, licensed under the MIT License.
 *
 * Copyright (c) 2017-2020 KyoriPowered
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package me.lucko.spark.common.util.pagination;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;
import java.util.function.ObjIntConsumer;

interface Paginator {
    @SuppressWarnings("unchecked")
    static <T> void forEachPageEntry(final Collection<? extends T> content, final int pageSize, final int page, final ObjIntConsumer<? super T> consumer) {
        final int size = content.size();
        final int start = pageSize * (page - 1);
        final int end = pageSize * page;
        if(content instanceof List<?> && content instanceof RandomAccess) {
            final List<? extends T> list = (List<? extends T>) content;
            for(int i = start; i < end && i < size; i++) {
                consumer.accept(list.get(i), i);
            }
        } else {
            final Iterator<? extends T> it = content.iterator();

            // skip entries on previous pages
            for(int i = 0; i < start; i++) {
                it.next();
            }

            for(int i = start; i < end && i < size; i++) {
                consumer.accept(it.next(), i);
            }
        }
    }
}