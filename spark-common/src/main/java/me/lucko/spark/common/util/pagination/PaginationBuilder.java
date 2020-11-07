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
import net.kyori.adventure.text.format.Style;

import java.util.function.Consumer;

final class PaginationBuilder implements Pagination.Builder {
    private int width = Pagination.WIDTH;
    private int resultsPerPage = Pagination.RESULTS_PER_PAGE;

    private char lineCharacter = Pagination.LINE_CHARACTER;
    private Style lineStyle = Pagination.LINE_STYLE;

    private Pagination.Renderer renderer = Pagination.DEFAULT_RENDERER;

    private char previousPageButtonCharacter = Pagination.PREVIOUS_PAGE_BUTTON_CHARACTER;
    private Style previousPageButtonStyle = Pagination.PREVIOUS_PAGE_BUTTON_STYLE;
    private char nextPageButtonCharacter = Pagination.NEXT_PAGE_BUTTON_CHARACTER;
    private Style nextPageButtonStyle = Pagination.NEXT_PAGE_BUTTON_STYLE;

    @Override
    public Pagination.Builder width(final int width) {
        this.width = width;
        return this;
    }

    @Override
    public Pagination.Builder resultsPerPage(final int resultsPerPage) {
        this.resultsPerPage = resultsPerPage;
        return this;
    }

    @Override
    public Pagination.Builder renderer(final Pagination.Renderer renderer) {
        this.renderer = renderer;
        return this;
    }

    @Override
    public Pagination.Builder line(final Consumer<CharacterAndStyle> line) {
        line.accept(new CharacterAndStyle() {
            @Override
            public CharacterAndStyle character(final char character) {
                PaginationBuilder.this.lineCharacter = character;
                return this;
            }

            @Override
            public CharacterAndStyle style(final Style style) {
                PaginationBuilder.this.lineStyle = style;
                return this;
            }
        });
        return this;
    }

    @Override
    public Pagination.Builder previousButton(final Consumer<CharacterAndStyle> previousButton) {
        previousButton.accept(new CharacterAndStyle() {
            @Override
            public CharacterAndStyle character(final char character) {
                PaginationBuilder.this.previousPageButtonCharacter = character;
                return this;
            }

            @Override
            public CharacterAndStyle style(final Style style) {
                PaginationBuilder.this.previousPageButtonStyle = style;
                return this;
            }
        });
        return this;
    }

    @Override
    public Pagination.Builder nextButton(final Consumer<CharacterAndStyle> nextButton) {
        nextButton.accept(new CharacterAndStyle() {
            @Override
            public CharacterAndStyle character(final char character) {
                PaginationBuilder.this.nextPageButtonCharacter = character;
                return this;
            }

            @Override
            public CharacterAndStyle style(final Style style) {
                PaginationBuilder.this.nextPageButtonStyle = style;
                return this;
            }
        });
        return this;
    }

    @Override
    public <T> Pagination<T> build(final Component title, final Pagination.Renderer.RowRenderer<T> rowRenderer, final Pagination.PageCommandFunction pageCommand) {
        return new PaginationImpl<>(
                this.width,
                this.resultsPerPage,
                this.renderer,
                this.lineCharacter,
                this.lineStyle,
                this.previousPageButtonCharacter,
                this.previousPageButtonStyle,
                this.nextPageButtonCharacter,
                this.nextPageButtonStyle,
                title,
                rowRenderer,
                pageCommand
        );
    }
}