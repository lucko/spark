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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.Style;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

final class PaginationImpl<T> implements Pagination<T> {
    private static final int LINE_CHARACTER_LENGTH = 1;

    private final int width;
    private final int resultsPerPage;

    private final Renderer renderer;

    private final char lineCharacter;
    private final Style lineStyle;

    private final char previousPageButtonCharacter;
    private final Style previousPageButtonStyle;
    private final char nextPageButtonCharacter;
    private final Style nextPageButtonStyle;

    private final Component title;
    private final Renderer.RowRenderer<T> rowRenderer;
    private final PageCommandFunction pageCommand;

    PaginationImpl(final int width, final int resultsPerPage, final Renderer renderer, final char lineCharacter, final Style lineStyle, final char previousPageButtonCharacter, final Style previousPageButtonStyle, final char nextPageButtonCharacter, final Style nextPageButtonStyle, final Component title, final Renderer.RowRenderer<T> rowRenderer, final PageCommandFunction pageCommand) {
        this.width = width;
        this.resultsPerPage = resultsPerPage;
        this.renderer = renderer;
        this.lineCharacter = lineCharacter;
        this.lineStyle = lineStyle;
        this.previousPageButtonCharacter = previousPageButtonCharacter;
        this.previousPageButtonStyle = previousPageButtonStyle;
        this.nextPageButtonCharacter = nextPageButtonCharacter;
        this.nextPageButtonStyle = nextPageButtonStyle;
        this.title = title;
        this.rowRenderer = rowRenderer;
        this.pageCommand = pageCommand;
    }

    @Override
    public List<Component> render(final Collection<? extends T> content, final int page) {
        if(content.isEmpty()) {
            return Collections.singletonList(this.renderer.renderEmpty());
        }

        final int pages = pages(this.resultsPerPage, content.size());

        if(!pageInRange(page, pages)) {
            return Collections.singletonList(this.renderer.renderUnknownPage(page, pages));
        }

        final List<Component> components = new ArrayList<>();
        components.add(this.renderHeader(page, pages));
        Paginator.forEachPageEntry(content, this.resultsPerPage, page, (value, index) -> {
            components.addAll(this.rowRenderer.renderRow(value, index));
        });
        components.add(this.renderFooter(page, pages));
        return Collections.unmodifiableList(components);
    }

    private Component renderHeader(final int page, final int pages) {
        final Component header = this.renderer.renderHeader(this.title, page, pages);
        final Component dashes = this.line(header);

        return Component.text()
                .append(dashes)
                .append(header)
                .append(dashes)
                .build();
    }

    private Component renderFooter(final int page, final int pages) {
        if(page == 1 && page == pages) {
            return this.line(this.width);
        }

        final Component buttons = this.renderFooterButtons(page, pages);
        final Component dashes = this.line(buttons);

        return Component.text()
                .append(dashes)
                .append(buttons)
                .append(dashes)
                .build();
    }

    private Component renderFooterButtons(final int page, final int pages) {
        final boolean hasPreviousPage = page > 1 && pages > 1;
        final boolean hasNextPage = (page < pages && page == 1) || ((hasPreviousPage && page > 1) && page != pages);

        final TextComponent.Builder buttons = Component.text();
        if(hasPreviousPage) {
            buttons.append(this.renderer.renderPreviousPageButton(this.previousPageButtonCharacter, this.previousPageButtonStyle, ClickEvent.runCommand(this.pageCommand.pageCommand(page - 1))));

            if(hasNextPage) {
                buttons.append(this.line(8));
            }
        }

        if(hasNextPage) {
            buttons.append(this.renderer.renderNextPageButton(this.nextPageButtonCharacter, this.nextPageButtonStyle, ClickEvent.runCommand(this.pageCommand.pageCommand(page + 1))));
        }

        return buttons.build();
    }

    private Component line(final Component component) {
        return this.line((this.width - length(component)) / (LINE_CHARACTER_LENGTH * 2));
    }

    private Component line(final int characters) {
        return Component.text(repeat(String.valueOf(this.lineCharacter), characters), this.lineStyle);
    }

    static int length(final Component component) {
        int length = 0;
        if(component instanceof TextComponent) {
            length += ((TextComponent) component).content().length();
        }
        for(final Component child : component.children()) {
            length += length(child);
        }
        return length;
    }

    static String repeat(final String character, final int count) {
        return String.join("", Collections.nCopies(count, character));
    }

    static int pages(final int pageSize, final int count) {
        final int pages = count / pageSize + 1;
        if(count % pageSize == 0) {
            return pages - 1;
        }
        return pages;
    }

    static boolean pageInRange(final int page, final int pages) {
        return page > 0 && page <= pages;
    }

    @Override
    public String toString() {
        return "PaginationImpl{" +
                "width=" + this.width +
                ", resultsPerPage=" + this.resultsPerPage +
                ", renderer=" + this.renderer +
                ", lineCharacter=" + this.lineCharacter +
                ", lineStyle=" + this.lineStyle +
                ", previousPageButtonCharacter=" + this.previousPageButtonCharacter +
                ", previousPageButtonStyle=" + this.previousPageButtonStyle +
                ", nextPageButtonCharacter=" + this.nextPageButtonCharacter +
                ", nextPageButtonStyle=" + this.nextPageButtonStyle +
                ", title=" + this.title +
                ", rowRenderer=" + this.rowRenderer +
                ", pageCommand=" + this.pageCommand +
                '}';
    }

    @Override
    public boolean equals(final Object other) {
        if(this == other) return true;
        if(other == null || this.getClass() != other.getClass()) return false;
        final PaginationImpl<?> that = (PaginationImpl<?>) other;
        if(this.width != that.width) return false;
        if(this.resultsPerPage != that.resultsPerPage) return false;
        if(this.lineCharacter != that.lineCharacter) return false;
        if(this.previousPageButtonCharacter != that.previousPageButtonCharacter) return false;
        if(this.nextPageButtonCharacter != that.nextPageButtonCharacter) return false;
        if(!this.renderer.equals(that.renderer)) return false;
        if(!this.lineStyle.equals(that.lineStyle)) return false;
        if(!this.previousPageButtonStyle.equals(that.previousPageButtonStyle)) return false;
        if(!this.nextPageButtonStyle.equals(that.nextPageButtonStyle)) return false;
        if(!this.title.equals(that.title)) return false;
        if(!this.rowRenderer.equals(that.rowRenderer)) return false;
        return this.pageCommand.equals(that.pageCommand);
    }

    @Override
    public int hashCode() {
        int result = this.width;
        result = (31 * result) + this.resultsPerPage;
        result = (31 * result) + this.renderer.hashCode();
        result = (31 * result) + (int) this.lineCharacter;
        result = (31 * result) + this.lineStyle.hashCode();
        result = (31 * result) + (int) this.previousPageButtonCharacter;
        result = (31 * result) + this.previousPageButtonStyle.hashCode();
        result = (31 * result) + (int) this.nextPageButtonCharacter;
        result = (31 * result) + this.nextPageButtonStyle.hashCode();
        result = (31 * result) + this.title.hashCode();
        result = (31 * result) + this.rowRenderer.hashCode();
        result = (31 * result) + this.pageCommand.hashCode();
        return result;
    }
}