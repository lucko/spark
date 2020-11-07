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
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Pagination.
 *
 * @param <T> the content type
 */
@FunctionalInterface
public interface Pagination<T> {
    /**
     * The default interface width.
     */
    int WIDTH = 55;
    /**
     * The default number of results per page.
     */
    int RESULTS_PER_PAGE = 6;
    /**
     * The default line character.
     */
    char LINE_CHARACTER = '-';
    /**
     * The default line style.
     */
    Style LINE_STYLE = Style.style(NamedTextColor.DARK_GRAY);
    /**
     * The default character for the previous page button.
     */
    char PREVIOUS_PAGE_BUTTON_CHARACTER = '\u00AB'; // «
    /**
     * The default style for the next page button.
     */
    Style PREVIOUS_PAGE_BUTTON_STYLE = Style.style()
            .color(NamedTextColor.RED)
            .hoverEvent(HoverEvent.showText(Component.text("Previous Page", NamedTextColor.RED)))
            .build();
    /**
     * The default character for the next page button.
     */
    char NEXT_PAGE_BUTTON_CHARACTER = '\u00BB'; // »
    /**
     * The default style for the next page button.
     */
    Style NEXT_PAGE_BUTTON_STYLE = Style.style()
            .color(NamedTextColor.GREEN)
            .hoverEvent(HoverEvent.showText(Component.text("Next Page", NamedTextColor.GREEN)))
            .build();

    /**
     * The default interface renderer.
     */
    Renderer DEFAULT_RENDERER = new Renderer() {
        @Override
        public String toString() {
            return "Pagination.DEFAULT_RENDERER";
        }
    };

    /**
     * Creates a pagination builder.
     *
     * @return a builder
     */
    static Builder builder() {
        return new PaginationBuilder();
    }

    /**
     * Renders.
     *
     * @param content the content to render
     * @param page the page number
     * @return the rendered results
     */
    List<Component> render(final Collection<? extends T> content, final int page);

    /**
     * A pagination renderer.
     */
    interface Renderer {
        Component GRAY_LEFT_ROUND_BRACKET = Component.text("(", NamedTextColor.GRAY);
        Component GRAY_LEFT_SQUARE_BRACKET = Component.text("[", NamedTextColor.GRAY);
        Component GRAY_RIGHT_ROUND_BRACKET = Component.text(")", NamedTextColor.GRAY);
        Component GRAY_RIGHT_SQUARE_BRACKET = Component.text("]", NamedTextColor.GRAY);
        Component GRAY_FORWARD_SLASH = Component.text("/", NamedTextColor.GRAY);

        /**
         * Renders an empty result.
         *
         * <p>No header or footer are rendered.</p>
         *
         * @return the rendered component
         */
        default Component renderEmpty() {
            return Component.text("No results match.", NamedTextColor.GRAY);
        }

        /**
         * Renders an unknown page.
         *
         * <p>No header or footer are rendered.</p>
         *
         * @param page the unknown page
         * @param pages the total number of pages
         * @return the rendered component
         */
        default Component renderUnknownPage(final int page, final int pages) {
            return Component.text("Unknown page selected. " + pages + " total pages.", NamedTextColor.GRAY);
        }

        /**
         * Renders a header.
         *
         * @param title the title
         * @param page the page
         * @param pages the total number of pages
         * @return the rendered component
         */
        default Component renderHeader(final Component title, final int page, final int pages) {
            return Component.text()
                    .append(Component.space())
                    .append(title)
                    .append(Component.space())
                    .append(GRAY_LEFT_ROUND_BRACKET)
                    .append(Component.text(page, NamedTextColor.WHITE))
                    .append(GRAY_FORWARD_SLASH)
                    .append(Component.text(pages, NamedTextColor.WHITE))
                    .append(GRAY_RIGHT_ROUND_BRACKET)
                    .append(Component.space())
                    .build();
        }

        /**
         * Renders a previous page button.
         *
         * @param character the button character
         * @param style the button style
         * @param clickEvent the click event for the button
         * @return the rendered component
         */
        default Component renderPreviousPageButton(final char character, final Style style, final ClickEvent clickEvent) {
            return Component.text()
                    .append(Component.space())
                    .append(GRAY_LEFT_SQUARE_BRACKET)
                    .append(Component.text(character, style.clickEvent(clickEvent)))
                    .append(GRAY_RIGHT_SQUARE_BRACKET)
                    .append(Component.space())
                    .build();
        }

        /**
         * Renders a next page button.
         *
         * @param character the button character
         * @param style the button style
         * @param clickEvent the click event for the button
         * @return the rendered component
         */
        default Component renderNextPageButton(final char character, final Style style, final ClickEvent clickEvent) {
            return Component.text()
                    .append(Component.space())
                    .append(GRAY_LEFT_SQUARE_BRACKET)
                    .append(Component.text(character, style.clickEvent(clickEvent)))
                    .append(GRAY_RIGHT_SQUARE_BRACKET)
                    .append(Component.space())
                    .build();
        }

        /**
         * A row renderer.
         *
         * @param <T> the content type
         */
        @FunctionalInterface
        interface RowRenderer<T> {
            /**
             * Renders a row.
             *
             * @param value the value
             * @param index the index
             * @return the rendered row
             */
            Collection<Component> renderRow(final T value, final int index);
        }
    }

    /**
     * A page command function.
     */
    @FunctionalInterface
    interface PageCommandFunction {
        /**
         * Gets the command to display the page.
         *
         * @param page the page
         * @return the command
         */
        String pageCommand(final int page);
    }

    /**
     * A pagination builder.
     */
    interface Builder {
        /**
         * Sets the width.
         *
         * @param width the width
         * @return this builder
         */
        Builder width(final int width);

        /**
         * Sets the number of results per page.
         *
         * @param resultsPerPage the number of results per page
         * @return this builder
         */
        Builder resultsPerPage(final int resultsPerPage);

        /**
         * Sets the renderer.
         *
         * @param renderer the renderer
         * @return this builder
         */
        Builder renderer(final Renderer renderer);

        /**
         * Sets the line character and style.
         *
         * @param line the line consumer
         * @return this builder
         */
        Builder line(final Consumer<CharacterAndStyle> line);

        /**
         * Sets the previous button.
         *
         * @param previousButton the button consumer
         * @return this builder
         */
        Builder previousButton(final Consumer<CharacterAndStyle> previousButton);

        /**
         * Sets the next button.
         *
         * @param nextButton the button consumer
         * @return this builder
         */
        Builder nextButton(final Consumer<CharacterAndStyle> nextButton);

        /**
         * Builds.
         *
         * @param title the title
         * @param rowRenderer the row renderer
         * @param pageCommand the page command
         * @param <T> the content type
         * @return pagination
         * @throws IllegalStateException if the title has not been set
         * @throws IllegalStateException if the row renderer has not been set
         */
        <T> Pagination<T> build(final Component title, final Renderer.RowRenderer<T> rowRenderer, final PageCommandFunction pageCommand);

        /**
         * A builder for a character and style pair.
         */
        interface CharacterAndStyle {
            /**
             * Sets the character.
             *
             * @param character the character
             * @return this builder
             */
            CharacterAndStyle character(final char character);

            /**
             * Sets the style.
             *
             * @param style the style
             * @return this builder
             */
            CharacterAndStyle style(final Style style);
        }
    }
}